package com.example.gobang_pt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicGameTest {
    @Test
    fun catalogContainsSeveralGamesWithUniqueIds() {
        assertTrue(classicGames.size >= 3)
        assertEquals(classicGames.size, classicGames.map(ClassicGame::id).distinct().size)
    }

    @Test
    fun everyPublishedGameReplaysCompletelyWithLegalAlternatingMoves() {
        classicGames.forEach { classicGame ->
            assertTrue(classicGame.title.isNotBlank())
            assertTrue(classicGame.event.isNotBlank())
            assertTrue(classicGame.blackPlayer.isNotBlank())
            assertTrue(classicGame.whitePlayer.isNotBlank())
            assertTrue(classicGame.description.isNotBlank())
            assertTrue(classicGame.source.isNotBlank())
            assertTrue(classicGame.moves.isNotEmpty())
            assertTrue(classicGame.moves.all(Position::isValid))
            assertEquals(classicGame.moves.size, classicGame.moves.distinct().size)

            var game = GobangGame()
            classicGame.moves.forEachIndexed { index, position ->
                game = game.play(position)
                assertEquals(classicGame.id, index + 1, game.moves.size)
                assertEquals(
                    classicGame.id,
                    if (index % 2 == 0) Stone.BLACK else Stone.WHITE,
                    game.moves.last().stone,
                )
            }

            assertEquals(classicGame.id, game, classicGame.toGobangGame())
            assertEquals(classicGame.id, classicGame.moves, game.moves.map(Move::position))
            if (game.winner != null) {
                assertEquals(classicGame.id, classicGame.result.winner, game.winner)
            }
            if (classicGame.result == ClassicGameResult.DRAW) {
                assertNull(classicGame.id, game.winner)
            }
        }
    }

    @Test
    fun publishedLengthsAndResultsMatchTheCatalog() {
        assertEquals(listOf(44, 43, 45), classicGames.map { it.moves.size })
        assertEquals(
            listOf(
                ClassicGameResult.WHITE_WIN,
                ClassicGameResult.BLACK_WIN,
                ClassicGameResult.BLACK_WIN,
            ),
            classicGames.map(ClassicGame::result),
        )
    }
}
