package com.example.gobang_pt

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.gobang_pt.ui.theme.GobangptTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PracticeGameScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun whiteVictoryShowsIncompleteChallengeAndAllowsRestart() {
        val scenario = practiceScenarios.first()
        var game = scenario.newGame()
        listOf("a1", "d7", "c1", "e7", "e1", "f7", "g1", "g7", "i1", "h7").forEach {
            game = game.play(patternPoint(it))
        }
        var restarted = false

        composeRule.setContent {
            GobangptTheme {
                PracticeGameScreen(
                    scenario = scenario,
                    game = game,
                    analysis = PracticeCoach.analyze(scenario, game),
                    isAiThinking = false,
                    showHint = false,
                    recommendation = null,
                    onPositionClick = {},
                    onToggleHint = {},
                    onUndo = {},
                    onRestart = { restarted = true },
                    onNextScenario = null,
                    onBackToLayout = {},
                )
            }
        }

        composeRule.onNodeWithText("白方获胜，本次挑战未完成").assertExists()
        composeRule.onNodeWithTag("practice_hint_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("practice_restart_button").performClick()
        assertTrue(restarted)
    }
}
