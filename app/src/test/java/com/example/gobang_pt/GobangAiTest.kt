package com.example.gobang_pt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
            val move = GobangAi.chooseMove(game, difficulty)
            assertNotNull(move)
            assertEquals(Stone.WHITE, game.play(move!!).winner)
        }
    }

    @Test
    fun everyDifficultyBlocksImmediateOpponentWin() {
        val game = alternatingGame(
            blackPositions = (3..6).map { Position(7, it) },
            whitePositions = listOf(0, 2, 4).map { Position(0, it) },
        )

        AiDifficulty.entries.forEach { difficulty ->
            val move = GobangAi.chooseMove(game, difficulty)
            assertTrue(move in listOf(Position(7, 2), Position(7, 7)))
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
