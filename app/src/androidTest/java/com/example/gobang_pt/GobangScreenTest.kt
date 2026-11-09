package com.example.gobang_pt

import android.content.pm.ActivityInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Rule
import org.junit.Test

class GobangScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun tappingBoardAlternatesPlayersAndRejectsOccupiedPoint() {
        tapIntersection(7, 7)
        composeRule.onNodeWithText("轮到白方落子").assertExists()
        tapIntersection(7, 7)
        composeRule.onNodeWithText("轮到白方落子").assertExists()
        tapIntersection(0, 0)
        composeRule.onNodeWithText("轮到黑方落子").assertExists()
        composeRule.onNodeWithText("已落子 2 手").assertExists()
    }

    @Test
    fun completeGameCanBeWonUndoneAndRestarted() {
        (3..6).forEach { column ->
            tapIntersection(7, column)
            tapIntersection(0, column - 3)
        }
        tapIntersection(7, 7)

        composeRule.onNodeWithText("黑方获胜！").assertExists()
        tapIntersection(10, 10)
        composeRule.onNodeWithText("已落子 9 手").assertExists()

        composeRule.onNodeWithTag("undo_button").performClick()
        composeRule.onNodeWithText("轮到黑方落子").assertExists()
        composeRule.onNodeWithText("已落子 8 手").assertExists()

        composeRule.onNodeWithTag("restart_button").performClick()
        composeRule.onNodeWithText("当前对局尚未结束，确定要清空棋盘吗？").assertExists()
        composeRule.onNodeWithTag("confirm_restart_button").performClick()
        composeRule.onNodeWithText("轮到黑方落子").assertExists()
        composeRule.onNodeWithText("已落子 0 手").assertExists()
        composeRule.onNodeWithTag("undo_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("restart_button").assertIsEnabled()
    }

    @Test
    fun gameSurvivesOrientationChanges() {
        tapIntersection(7, 7)
        tapIntersection(6, 7)

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()
        composeRule.onNodeWithText("轮到黑方落子").assertExists()
        composeRule.onNodeWithText("已落子 2 手").assertExists()

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        composeRule.waitForIdle()
        composeRule.onNodeWithText("轮到黑方落子").assertExists()
        composeRule.onNodeWithText("已落子 2 手").assertExists()
    }

    @Test
    fun reviewStepsThroughReadOnlyBoardAndRestoresGameOnExit() {
        tapIntersection(7, 7)
        tapIntersection(6, 7)
        tapIntersection(8, 8)

        composeRule.onNodeWithTag("review_button").performClick()
        composeRule.onNodeWithText("复盘进度 0/3").assertExists()
        composeRule.onNodeWithText("已落子 0 手").assertExists()
        composeRule.onNodeWithTag("previous_move_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("next_move_button").assertIsEnabled()

        tapIntersection(10, 10)
        composeRule.onNodeWithText("复盘进度 0/3").assertExists()
        composeRule.onNodeWithText("已落子 0 手").assertExists()

        composeRule.onNodeWithTag("next_move_button").performClick()
        composeRule.onNodeWithText("复盘进度 1/3").assertExists()
        composeRule.onNodeWithText("第 1 手 · 黑方落子：第 8 行，第 8 列").assertExists()
        composeRule.onNodeWithTag("next_move_button").performClick()
        composeRule.onNodeWithText("复盘进度 2/3").assertExists()
        composeRule.onNodeWithTag("previous_move_button").performClick()
        composeRule.onNodeWithText("复盘进度 1/3").assertExists()

        composeRule.onNodeWithTag("next_move_button").performClick()
        composeRule.onNodeWithTag("next_move_button").performClick()
        composeRule.onNodeWithText("复盘进度 3/3").assertExists()
        composeRule.onNodeWithText("已落子 3 手").assertExists()
        composeRule.onNodeWithTag("next_move_button").assertIsNotEnabled()

        composeRule.onNodeWithTag("exit_review_button").performClick()
        composeRule.onNodeWithText("轮到白方落子").assertExists()
        composeRule.onNodeWithText("已落子 3 手").assertExists()
        composeRule.onNodeWithTag("review_button").assertExists()
    }

    @Test
    fun reviewProgressSurvivesOrientationChanges() {
        tapIntersection(7, 7)
        tapIntersection(6, 7)
        composeRule.onNodeWithTag("review_button").performClick()
        composeRule.onNodeWithTag("next_move_button").performClick()

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()
        composeRule.onNodeWithText("复盘进度 1/2").assertExists()
        composeRule.onNodeWithTag("next_move_button").performClick()

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        composeRule.waitForIdle()
        composeRule.onNodeWithText("复盘进度 2/2").assertExists()
        composeRule.onNodeWithTag("exit_review_button").performClick()
        composeRule.onNodeWithText("已落子 2 手").assertExists()
        composeRule.onNodeWithText("轮到黑方落子").assertExists()
    }

    @Test
    fun aiModeRespondsAndUndoRemovesWholeRound() {
        composeRule.onNodeWithTag("ai_mode").performClick()
        composeRule.onNodeWithTag("difficulty_easy").performClick()
        composeRule.onNodeWithTag("difficulty_easy").assertIsSelected()

        tapIntersection(7, 7)
        waitForMoveCount(2)
        composeRule.onNodeWithText("轮到你落子").assertExists()

        composeRule.onNodeWithTag("undo_button").performClick()
        composeRule.onNodeWithText("已落子 0 手").assertExists()
        composeRule.onNodeWithText("轮到你落子").assertExists()
    }

    @Test
    fun switchingModeWithMovesStartsNewGameAfterConfirmation() {
        tapIntersection(7, 7)
        composeRule.onNodeWithTag("ai_mode").performClick()

        composeRule.onNodeWithText("切换模式或难度将开始新对局，确定继续吗？").assertExists()
        composeRule.onNodeWithTag("confirm_settings_button").performClick()
        composeRule.onNodeWithText("已落子 0 手").assertExists()
        composeRule.onNodeWithText("轮到你落子").assertExists()
        composeRule.onNodeWithTag("difficulty_easy").assertIsSelected()
    }

    @Test
    fun aiSettingsAndGameSurviveOrientationChanges() {
        composeRule.onNodeWithTag("ai_mode").performClick()
        composeRule.onNodeWithTag("difficulty_hard").performClick()
        tapIntersection(7, 7)
        waitForMoveCount(2)

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("difficulty_hard").assertIsSelected()
        composeRule.onNodeWithText("已落子 2 手").assertExists()
        composeRule.onNodeWithText("轮到你落子").assertExists()

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("difficulty_hard").assertIsSelected()
        composeRule.onNodeWithText("已落子 2 手").assertExists()
    }

    private fun waitForMoveCount(moveCount: Int) {
        val text = "已落子 $moveCount 手"
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tapIntersection(row: Int, column: Int) {
        composeRule.onNodeWithTag("gobang_board").performTouchInput {
            val step = width / (BOARD_SIZE + 1f)
            click(Offset(step * (column + 1), step * (row + 1)))
        }
    }
}
