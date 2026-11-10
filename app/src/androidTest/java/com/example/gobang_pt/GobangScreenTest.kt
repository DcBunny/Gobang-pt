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
    fun aiModeWaitsOneSecondBeforeResponding() {
        composeRule.onNodeWithTag("ai_mode").performClick()
        composeRule.mainClock.autoAdvance = false
        try {
            tapIntersection(7, 7)
            composeRule.mainClock.advanceTimeByFrame()
            val thinkingStartedAt = composeRule.mainClock.currentTime
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithText("电脑正在思考…").assertExists()
            composeRule.onNodeWithText("已落子 1 手").assertExists()

            val elapsedThinkingTime = composeRule.mainClock.currentTime - thinkingStartedAt
            composeRule.mainClock.advanceTimeBy(999 - elapsedThinkingTime)
            composeRule.onNodeWithText("已落子 1 手").assertExists()

            composeRule.mainClock.advanceTimeBy(1)
            composeRule.mainClock.autoAdvance = true
            waitForMoveCount(2)
            composeRule.onNodeWithText("轮到你落子").assertExists()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
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

    @Test
    fun classicGameReviewIsReadOnlyAndRestoresCurrentGame() {
        tapIntersection(7, 7)
        tapIntersection(6, 7)
        composeRule.onNodeWithTag("learning_home_button").performClick()
        composeRule.onNodeWithTag("learning_home_title").assertExists()

        composeRule.onNodeWithTag("classic_game_meritee_nakamura_1999_1").performClick()
        composeRule.onNodeWithText("复盘进度 0/44").assertExists()
        composeRule.onNodeWithTag("previous_move_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("next_move_button").assertIsEnabled()

        tapIntersection(10, 10)
        composeRule.onNodeWithText("复盘进度 0/44").assertExists()
        composeRule.onNodeWithText("已落子 0 手").assertExists()
        composeRule.onNodeWithTag("next_move_button").performClick()
        composeRule.onNodeWithText("复盘进度 1/44").assertExists()
        composeRule.onNodeWithTag("previous_move_button").performClick()
        composeRule.onNodeWithText("复盘进度 0/44").assertExists()

        composeRule.onNodeWithTag("classic_review_back_to_game_button").performClick()
        composeRule.onNodeWithText("已落子 2 手").assertExists()
        composeRule.onNodeWithText("轮到黑方落子").assertExists()
        tapIntersection(7, 7)
        composeRule.onNodeWithText("已落子 2 手").assertExists()
    }

    @Test
    fun classicReviewSurvivesOrientationChangesAndCanReturnToList() {
        composeRule.onNodeWithTag("learning_home_button").performClick()
        composeRule.onNodeWithTag("classic_game_meritee_nakamura_1999_2").performClick()
        composeRule.onNodeWithTag("next_move_button").performClick()

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()
        composeRule.onNodeWithText("复盘进度 1/43").assertExists()
        composeRule.onNodeWithTag("next_move_button").performClick()

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        composeRule.waitForIdle()
        composeRule.onNodeWithText("复盘进度 2/43").assertExists()
        composeRule.onNodeWithTag("back_to_classic_games_button").performClick()
        composeRule.onNodeWithTag("learning_home_title").assertExists()

        composeRule.onNodeWithTag("classic_game_meritee_nakamura_1999_2").performClick()
        composeRule.onNodeWithText("复盘进度 0/43").assertExists()
        composeRule.onNodeWithTag("back_to_classic_games_button").performClick()
        composeRule.onNodeWithTag("learning_back_button").performClick()
        composeRule.onNodeWithText("已落子 0 手").assertExists()
    }

    @Test
    fun classicReviewReachesPublishedResultAndDisablesNextAtEnd() {
        composeRule.onNodeWithTag("learning_home_button").performClick()
        composeRule.onNodeWithTag("classic_game_meritee_nakamura_1999_1").performClick()
        repeat(44) {
            composeRule.onNodeWithTag("next_move_button").performClick()
        }

        composeRule.onNodeWithText("复盘进度 44/44").assertExists()
        composeRule.onNodeWithText("已落子 44 手").assertExists()
        composeRule.onNodeWithText("原谱结果：白方胜").assertExists()
        composeRule.onNodeWithTag("next_move_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("previous_move_button").assertIsEnabled()
        composeRule.onNodeWithTag("previous_move_button").performClick()
        composeRule.onNodeWithText("复盘进度 43/44").assertExists()
    }

    @Test
    fun patternPracticeHandlesWrongAndMultipleAnswersAndSurvivesRotation() {
        tapIntersection(7, 7)
        composeRule.onNodeWithTag("learning_home_button").performClick()
        composeRule.onNodeWithTag("learning_patterns_category").performClick()
        composeRule.onNodeWithTag("pattern_lesson_open_four").performClick()
        composeRule.onNodeWithTag("pattern_start_practice_button").performClick()
        composeRule.onNodeWithText("已找到 0/2 个要点").assertExists()

        tapIntersection(0, 0)
        composeRule.onNodeWithTag("pattern_wrong_answer").assertExists()
        composeRule.onNodeWithText("已找到 0/2 个要点").assertExists()
        tapIntersection(4, 4)
        composeRule.onNodeWithText("已找到 1/2 个要点").assertExists()
        composeRule.onNodeWithTag("pattern_practice_complete").assertDoesNotExist()

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()
        composeRule.onNodeWithText("已找到 1/2 个要点").assertExists()
        tapIntersection(9, 9)
        composeRule.onNodeWithText("已找到 2/2 个要点").assertExists()
        composeRule.onNodeWithTag("pattern_practice_complete").assertExists()
        composeRule.onNodeWithTag("pattern_next_lesson_button").assertIsEnabled()

        composeRule.onNodeWithTag("pattern_reset_button").performClick()
        composeRule.onNodeWithText("已找到 0/2 个要点").assertExists()
        composeRule.onNodeWithTag("pattern_practice_complete").assertDoesNotExist()
        composeRule.onNodeWithTag("pattern_next_lesson_button").assertIsNotEnabled()
        tapIntersection(4, 4)
        tapIntersection(9, 9)
        composeRule.onNodeWithTag("pattern_next_lesson_button").performClick()
        composeRule.onNodeWithText("冲四").assertExists()
        composeRule.onNodeWithTag("pattern_start_practice_button").assertExists()

        composeRule.onNodeWithTag("pattern_back_to_list_button").performClick()
        composeRule.onNodeWithTag("learning_patterns_category").assertIsSelected()
        composeRule.onNodeWithTag("learning_back_button").performClick()
        composeRule.onNodeWithText("已落子 1 手").assertExists()
        composeRule.onNodeWithText("轮到白方落子").assertExists()
    }

    @Test
    fun singleAnswerPracticeCanRevisitExampleWithoutLosingCompletion() {
        composeRule.onNodeWithTag("learning_home_button").performClick()
        composeRule.onNodeWithTag("learning_patterns_category").performClick()
        composeRule.onNodeWithTag("pattern_lesson_closed_four").performClick()
        composeRule.onNodeWithTag("pattern_start_practice_button").performClick()

        tapIntersection(7, 8)
        composeRule.onNodeWithText("已找到 1/1 个要点").assertExists()
        composeRule.onNodeWithTag("pattern_practice_complete").assertExists()
        composeRule.onNodeWithTag("pattern_show_example_button").performClick()
        composeRule.onNodeWithTag("pattern_start_practice_button").assertExists()
        composeRule.onNodeWithTag("pattern_start_practice_button").performClick()
        composeRule.onNodeWithTag("pattern_practice_complete").assertExists()

        composeRule.onNodeWithTag("pattern_back_to_list_button").performClick()
        composeRule.onNodeWithTag("learning_back_button").performClick()
        composeRule.onNodeWithText("已落子 0 手").assertExists()
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
