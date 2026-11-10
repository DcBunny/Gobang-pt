package com.example.gobang_pt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GobangAiTest {
    @Test
    fun everyDifficultyStartsAtCenterOnEmptyBoard() {
        AiDifficulty.entries.forEach { difficulty ->
            val move = GobangAi.chooseMove(GobangGame(), difficulty)
            assertEquals(Position(7, 7), move)
        }
    }

    @Test
    fun everyDifficultyReturnsLegalEmptyPosition() {
        val game = alternatingGame(
            blackPositions = listOf(Position(7, 7), Position(8, 8)),
            whitePositions = listOf(Position(7, 8)),
        )

        AiDifficulty.entries.forEach { difficulty ->
            val move = GobangAi.chooseMove(game, difficulty)
            assertNotNull(move)
            assertTrue(move!!.isValid)
            assertNull(game.stoneAt(move))
        }
    }

    @Test
    fun everyDifficultyTakesImmediateWin() {
        val game = alternatingGame(
            blackPositions = listOf(0, 2, 4, 6, 8).map { Position(0, it) },
            whitePositions = (3..6).map { Position(7, it) },
        )

        AiDifficulty.entries.forEach { difficulty ->
            (0..2).forEach { seed ->
                val move = GobangAi.chooseMove(game, difficulty, Random(seed))
                assertNotNull(move)
                assertEquals(Stone.WHITE, game.play(move!!).winner)
            }
        }
    }

    @Test
    fun everyDifficultyBlocksImmediateOpponentWin() {
        val game = alternatingGame(
            blackPositions = (3..6).map { Position(7, it) },
            whitePositions = listOf(0, 2, 4).map { Position(0, it) },
        )

        AiDifficulty.entries.forEach { difficulty ->
            (0..2).forEach { seed ->
                val move = GobangAi.chooseMove(game, difficulty, Random(seed))
                assertTrue(move in listOf(Position(7, 2), Position(7, 7)))
            }
        }
    }

    @Test
    fun easyRecognizesBrokenFourAsAnEqualForcingMove() {
        val game = alternatingGame(
            blackPositions = listOf(Position(7, 4), Position(0, 0), Position(0, 2), Position(0, 4)),
            whitePositions = (5..7).map { Position(7, it) },
        )
        val forcingMoves = setOf(Position(7, 8), Position(7, 9))

        val moves = (0..5).map { seed ->
            GobangAi.chooseMove(game, AiDifficulty.EASY, Random(seed))
        }

        assertTrue(moves.all { it in forcingMoves })
        assertEquals(forcingMoves, moves.toSet())
    }

    @Test
    fun easyRecognizesBrokenThreeAlongsideContiguousThrees() {
        val game = alternatingGame(
            blackPositions = listOf(Position(0, 0), Position(0, 14), Position(14, 0), Position(14, 4)),
            whitePositions = listOf(Position(7, 6), Position(7, 7), Position(12, 12)),
        )
        val openThreeMoves = setOf(Position(7, 4), Position(7, 5), Position(7, 8), Position(7, 9))

        val moves = (0..7).map { seed ->
            GobangAi.chooseMove(game, AiDifficulty.EASY, Random(seed))
        }

        assertTrue("Unexpected moves: $moves", moves.all { it in openThreeMoves })
        assertTrue("Broken three was never selected: $moves", Position(7, 9) in moves)
    }

    @Test
    fun everyDifficultyBlocksDoubleThree() {
        val game = alternatingGame(
            blackPositions = listOf(
                Position(7, 6),
                Position(6, 7),
                Position(8, 7),
                Position(7, 8),
                Position(2, 13),
            ),
            whitePositions = listOf(Position(0, 0), Position(0, 14), Position(14, 0), Position(14, 4)),
        )

        AiDifficulty.entries.forEach { difficulty ->
            val move = GobangAi.chooseMove(game, difficulty, Random(0))
            assertEquals("Difficulty: $difficulty", Position(7, 7), move)
        }
    }

    @Test
    fun searchDifficultiesCreateDoubleThree() {
        val game = alternatingGame(
            blackPositions = listOf(Position(0, 0), Position(0, 14), Position(14, 0), Position(14, 4), Position(2, 13)),
            whitePositions = listOf(Position(7, 6), Position(6, 7), Position(8, 7), Position(7, 8)),
        )

        listOf(AiDifficulty.MEDIUM, AiDifficulty.HARD).forEach { difficulty ->
            val move = GobangAi.chooseMove(game, difficulty, Random(0))
            assertEquals("Difficulty: $difficulty", Position(7, 7), move)
        }
    }

    @Test
    fun searchDifficultiesPrioritizeFourThree() {
        val game = alternatingGame(
            blackPositions = listOf(
                Position(0, 0),
                Position(0, 14),
                Position(14, 0),
                Position(14, 4),
                Position(2, 13),
                Position(12, 1),
            ),
            whitePositions = listOf(
                Position(7, 5),
                Position(7, 6),
                Position(7, 8),
                Position(6, 7),
                Position(8, 7),
            ),
        )

        listOf(AiDifficulty.MEDIUM, AiDifficulty.HARD).forEach { difficulty ->
            val move = GobangAi.chooseMove(game, difficulty, Random(0))
            assertEquals("Difficulty: $difficulty", Position(7, 7), move)
        }
    }

    @Test
    fun searchDifficultiesVarySymmetricBestMoves() {
        val game = GobangGame().play(Position(7, 7))
        val symmetricMoves = setOf(Position(6, 6), Position(6, 8), Position(8, 6), Position(8, 8))

        listOf(AiDifficulty.MEDIUM, AiDifficulty.HARD).forEach { difficulty ->
            val moves = (0..3).map { seed ->
                GobangAi.chooseMove(game, difficulty, Random(seed))
            }
            assertTrue("Difficulty $difficulty produced $moves", moves.all { it in symmetricMoves })
            assertTrue("Difficulty $difficulty produced $moves", moves.toSet().size > 1)
        }
    }

    @Test
    fun seededAiGamesKeepEveryMoveLegal() {
        (0..1).forEach { seed ->
            var game = GobangGame()
            val random = Random(seed)
            while (game.moves.size < 16 && !game.isOver) {
                val difficulty = if (game.currentPlayer == Stone.BLACK) AiDifficulty.EASY else AiDifficulty.HARD
                val move = GobangAi.chooseMove(game, difficulty, random)
                assertNotNull(move)
                assertTrue(move!!.isValid)
                assertNull(game.stoneAt(move))
                val updatedGame = game.play(move)
                assertEquals(game.moves.size + 1, updatedGame.moves.size)
                game = updatedGame
            }
            assertTrue(game.moves.isNotEmpty())
        }
    }

    @Test
    fun finishedGameHasNoAiMove() {
        val game = alternatingGame(
            blackPositions = (3..7).map { Position(7, it) },
            whitePositions = listOf(0, 2, 4, 6).map { Position(0, it) },
        )

        AiDifficulty.entries.forEach { difficulty ->
            assertNull(GobangAi.chooseMove(game, difficulty))
        }
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
