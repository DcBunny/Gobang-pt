package com.example.gobang_pt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeCoachTest {
    @Test
    fun scenariosMatchLayoutLessonsAndStartWithEmptyActualHistory() {
        assertEquals(layoutLessons.map(LayoutLesson::id), practiceScenarios.map(PracticeScenario::id))
        practiceScenarios.forEach { scenario ->
            val game = scenario.newGame()
            assertTrue(game.moves.isEmpty())
            assertEquals(Stone.BLACK, game.currentPlayer)
            assertEquals(scenario.layoutLesson.initialPosition.toGobangGame().board, game.board)
            assertEquals(PracticeGoal.FIVE, scenario.goals.last())
            assertEquals(scenario.goals.size, scenario.goals.distinct().size)
        }
    }

    @Test
    fun replayAndUndoPreserveTheInitialLayout() {
        val scenario = practiceScenarios.first { it.id == "connected_diagonals" }
        val initial = scenario.newGame()
        val afterBlack = initial.play(patternPoint("h8"))
        val afterWhite = afterBlack.play(patternPoint("i9"))

        assertEquals(afterWhite, scenario.replay(scenario.encode(afterWhite)))
        assertEquals(initial, scenario.undo(afterWhite))
        assertEquals(initial, scenario.undo(afterBlack))
        assertEquals(initial, scenario.replay(""))
    }

    @Test
    fun coachPrioritizesImmediateWhiteThreat() {
        val scenario = practiceScenarios.first()
        var game = scenario.newGame()
        listOf("a1", "d7", "c1", "e7", "e1", "f7", "g1", "g7").forEach {
            game = game.play(patternPoint(it))
        }

        val recommendation = PracticeCoach.recommend(scenario, game)
        assertNotNull(recommendation)
        assertEquals(Position(6, 2), recommendation!!.move)
        assertTrue(recommendation.text.contains("防守"))
    }

    @Test
    fun scriptedRoutesAdvanceEveryGoalAndFinishOnlyWithFive() {
        val scripts = mapOf(
            "center_development" to "h8 g7 i8 g9 g8 i6 j8 k8 f8",
            "connected_diagonals" to "h8 i9 i7 e11 j6",
            "force_then_switch" to "i8 k8 i9 j10 e5",
            "double_four" to "i9 h7 h8 j10 j8",
            "turtle_formation" to "h7 h9 i8 e4 j9",
            "lightning_formation" to "h8 j8 i9 j10 h9 h5 h10",
            "lobster_formation" to "j10 k9 k11 g7 l12",
            "moon_formation" to "h8 j6 h10 h7 h12",
            "sword_formation" to "h7 h4 i6 e10 j5",
            "sickle_formation" to "h8 h9 j6",
        )
        scripts.forEach { (id, script) ->
            val scenario = practiceScenarios.first { it.id == id }
            var game = scenario.newGame()
            script.split(' ').forEach { coordinate -> game = game.play(patternPoint(coordinate)) }

            val analysis = PracticeCoach.analyze(scenario, game)
            assertEquals(id, Stone.BLACK, game.winner)
            assertEquals(id, scenario.goals.toSet(), analysis.achievedGoals)
            assertNull(id, analysis.currentGoal)
            assertEquals(id, PracticeGoal.FIVE, analysis.latestAchievedGoal)
            assertNull(PracticeCoach.recommend(scenario, game))
        }
    }

    @Test
    fun coachRecommendationsCanBeatThePracticeOpponentInEveryScenario() {
        practiceScenarios.forEach { scenario ->
            var game = scenario.newGame()
            var rounds = 0
            while (!game.isOver && rounds < 80) {
                if (game.currentPlayer == Stone.BLACK) {
                    val recommendation = PracticeCoach.recommend(scenario, game)
                    assertNotNull("${scenario.id}: no coach move", recommendation)
                    val move = recommendation!!.move
                    assertNull("${scenario.id}: occupied $move", game.stoneAt(move))
                    game = game.play(move)
                } else {
                    val move = scenario.chooseWhiteMove(game)
                    assertNotNull("${scenario.id}: no AI move", move)
                    assertNull("${scenario.id}: occupied $move", game.stoneAt(move!!))
                    game = game.play(move)
                }
                rounds++
            }

            assertEquals("${scenario.id}: moves=${scenario.encode(game)}", Stone.BLACK, game.winner)
            assertEquals(
                "${scenario.id}: moves=${scenario.encode(game)}",
                scenario.goals.toSet(),
                PracticeCoach.analyze(scenario, game).achievedGoals,
            )
            assertNull(scenario.chooseWhiteMove(game))
        }
    }
}
