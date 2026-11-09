package com.example.gobang_pt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GobangGameTest {
    @Test
    fun blackMovesFirstAndPlayersAlternate() {
        val firstMove = GobangGame().play(Position(7, 7))

        assertEquals(Stone.BLACK, firstMove.stoneAt(Position(7, 7)))
        assertEquals(Stone.WHITE, firstMove.currentPlayer)
        val secondMove = firstMove.play(Position(0, 0))
        assertEquals(Stone.WHITE, secondMove.stoneAt(Position(0, 0)))
        assertEquals(Stone.BLACK, secondMove.currentPlayer)
    }

    @Test
    fun occupiedAndOutsidePositionsDoNotChangeGame() {
        val game = GobangGame().play(Position(7, 7))

        assertSame(game, game.play(Position(7, 7)))
        assertSame(game, game.play(Position(-1, 0)))
        assertSame(game, game.play(Position(0, BOARD_SIZE)))
    }

    @Test
    fun horizontalFiveWins() {
        assertBlackWins(listOf(3, 4, 5, 6, 7).map { Position(7, it) })
    }

    @Test
    fun verticalFiveWins() {
        assertBlackWins(listOf(3, 4, 5, 6, 7).map { Position(it, 7) })
    }

    @Test
    fun diagonalFiveWins() {
        assertBlackWins((3..7).map { Position(it, it) })
    }

    @Test
    fun antiDiagonalFiveWins() {
        assertBlackWins((3..7).map { Position(it, 14 - it) })
    }

    @Test
    fun sixConnectedStonesAlsoWins() {
        val initialMoves = (2..6).map { Move(Position(7, it), Stone.BLACK) } +
            listOf(0, 2, 4, 6).map { Move(Position(0, it), Stone.WHITE) }
        val initialBoard = List<Stone?>(BOARD_SIZE * BOARD_SIZE) { null }.toMutableList()
        initialMoves.forEach { move ->
            initialBoard[move.position.row * BOARD_SIZE + move.position.column] = move.stone
        }
        val game = GobangGame(
            board = initialBoard,
            moves = initialMoves,
            currentPlayer = Stone.BLACK,
        ).play(Position(7, 7))

        assertEquals(Stone.BLACK, game.winner)
        assertEquals(6, game.winningLine.size)
        assertEquals(Position(7, 2), game.winningLine.first())
        assertEquals(Position(7, 7), game.winningLine.last())
    }

    @Test
    fun fourConnectedStonesDoesNotWin() {
        val game = alternatingGame(
            blackPositions = (3..6).map { Position(7, it) },
            whitePositions = (0..3).map { Position(0, it) },
        )

        assertNull(game.winner)
        assertFalse(game.isOver)
        assertEquals(Stone.BLACK, game.currentPlayer)
    }

    @Test
    fun whiteCanWin() {
        val game = alternatingGame(
            blackPositions = listOf(0, 2, 4, 6, 8).map { Position(0, it) },
            whitePositions = (3..7).map { Position(7, it) },
        )

        assertEquals(Stone.WHITE, game.winner)
        assertTrue(game.isOver)
    }

    @Test
    fun noMovesAllowedAfterWin() {
        val game = assertBlackWins((3..7).map { Position(7, it) })
        val moveCount = game.moves.size

        assertSame(game, game.play(Position(10, 10)))
        assertEquals(moveCount, game.moves.size)
    }

    @Test
    fun undoRestoresPreviousTurnAndCanResumeFinishedGame() {
        val game = assertBlackWins((3..7).map { Position(7, it) })
        val undone = game.undo()

        assertNull(undone.winner)
        assertFalse(undone.isOver)
        assertEquals(Stone.BLACK, undone.currentPlayer)
        assertEquals(game.moves.size - 1, undone.moves.size)
        assertNull(undone.stoneAt(Position(7, 7)))
        assertEquals(Stone.BLACK, undone.play(Position(7, 7)).winner)
    }

    @Test
    fun fullBoardWithoutFiveIsDraw() {
        val lastPosition = Position(0, 0)
        val board = List<Stone?>(BOARD_SIZE * BOARD_SIZE) { index ->
            val row = index / BOARD_SIZE
            val column = index % BOARD_SIZE
            if ((row + 2 * column) % 4 < 2) Stone.BLACK else Stone.WHITE
        }.toMutableList()
        val lastStone = board[0]
        board[0] = null
        val game = GobangGame(
            board = board,
            moves = List(BOARD_SIZE * BOARD_SIZE - 1) { Move(lastPosition, Stone.BLACK) },
            currentPlayer = lastStone!!,
        ).play(lastPosition)

        assertTrue(game.isDraw)
        assertTrue(game.isOver)
        assertNull(game.winner)
    }

    @Test
    fun freshGameCanBeCreatedAfterMoves() {
        val game = GobangGame().play(Position(7, 7)).undo()

        assertTrue(game.moves.isEmpty())
        assertTrue(game.board.all { it == null })
        assertEquals(Stone.BLACK, game.currentPlayer)
        assertFalse(game.isOver)
    }

    @Test
    fun replayToRebuildsSelectedPrefixWithoutChangingOriginal() {
        val game = alternatingGame(
            blackPositions = listOf(Position(7, 7), Position(8, 8), Position(9, 9)),
            whitePositions = listOf(Position(0, 0), Position(0, 2)),
        )

        val initial = game.replayTo(0)
        val replay = game.replayTo(3)

        assertTrue(initial.moves.isEmpty())
        assertTrue(initial.board.all { it == null })
        assertEquals(3, replay.moves.size)
        assertEquals(Stone.BLACK, replay.stoneAt(Position(7, 7)))
        assertEquals(Stone.WHITE, replay.stoneAt(Position(0, 0)))
        assertEquals(Stone.BLACK, replay.stoneAt(Position(8, 8)))
        assertNull(replay.stoneAt(Position(0, 2)))
        assertEquals(Stone.WHITE, replay.currentPlayer)
        assertEquals(5, game.moves.size)
        assertEquals(Stone.WHITE, game.stoneAt(Position(0, 2)))
    }

    @Test
    fun replayToFinalMoveRestoresResultAndWinningLine() {
        val game = assertBlackWins((3..7).map { Position(7, it) })

        assertEquals(game, game.replayTo(game.moves.size))
    }

    @Test
    fun replayToRejectsMoveCountsOutsideHistory() {
        val game = GobangGame().play(Position(7, 7))

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { game.replayTo(-1) }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { game.replayTo(2) }
    }

    private fun assertBlackWins(blackPositions: List<Position>): GobangGame {
        val whitePositions = (0 until blackPositions.size - 1).map { Position(0, it * 2) }
        val game = alternatingGame(blackPositions, whitePositions)
        assertEquals(Stone.BLACK, game.winner)
        assertTrue(game.winningLine.size >= 5)
        return game
    }

    private fun alternatingGame(
        blackPositions: List<Position>,
        whitePositions: List<Position>,
    ): GobangGame {
        var game = GobangGame()
        blackPositions.forEachIndexed { index, position ->
            game = game.play(position)
            if (index < whitePositions.size) game = game.play(whitePositions[index])
        }
        return game
    }
}
