package com.example.gobang_pt

import android.content.pm.ActivityInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.example.gobang_pt.ui.theme.GobangptTheme
import org.junit.Rule
import org.junit.Test

class KidsLessonsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsStagesLessonsAndProgress() {
        setKidsContent()

        composeRule.onNodeWithText("第一站 · 连五和防守").assertExists()
        composeRule.onNodeWithText("第二站 · 让棋子长大").assertExists()
        composeRule.onNodeWithText("第三站 · 学会组合进攻").assertExists()
        composeRule.onNodeWithText("第四站 · 开局布局").assertExists()
        composeRule.onNodeWithText("第五站 · 棋型进阶").assertExists()
        composeRule.onNodeWithText("第六站 · 连续与防守").assertExists()
        composeRule.onNodeWithText("第七站 · 防守基本功").assertExists()
        composeRule.onNodeWithText("第八站 · 棋型与判断").assertExists()
        composeRule.onNodeWithText("已通关 0 / 20 关", substring = true).assertExists()

        kidsLessons.forEach { lesson ->
            composeRule.onNodeWithTag("kids_lesson_${lesson.id}").performScrollTo().assertExists()
        }
    }

    @Test
    fun wrongMoveShowsFeedbackThenHintAndCompletion() {
        setKidsContent()
        openLesson("make_open_four")

        tapBoard(0, 0)
        composeRule.onNodeWithTag("kids_lesson_wrong_feedback").assertExists()
        composeRule.onNodeWithText("把三个黑子往两头延长", substring = true).assertExists()

        composeRule.onNodeWithTag("kids_lesson_hint_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("kids_lesson_hint_text").assertExists()
        composeRule.onNodeWithText("G8、H8、I8 的两头是 F8 和 J8。", substring = true).assertExists()

        tapBoard(7, 5)
        composeRule.onNodeWithTag("kids_completion_card").assertExists()
        composeRule.onNodeWithText("你学会了：活四").assertExists()

        composeRule.onNodeWithTag("kids_completion_back_button").performScrollTo().performClick()
        composeRule.onNodeWithText("已通关 1 / 20 关", substring = true).assertExists()
        composeRule.onNodeWithTag("kids_lesson_make_open_four").performScrollTo().assertExists()
        composeRule.onNodeWithText("⭐ 已通关 · 再玩一次").assertExists()
    }

    @Test
    fun twoStepLessonPlaysWhiteReplyThenAdvancesToNextLesson() {
        setKidsContent()
        openLesson("double_three")

        tapBoard(7, 7)
        composeRule.onNodeWithTag("kids_lesson_correct_feedback").assertExists()
        composeRule.onNodeWithText("白棋在 I9 挡住了其中一条斜线。", substring = true).assertExists()
        composeRule.onNodeWithTag("kids_lesson_continue_button").performScrollTo().performClick()

        composeRule.onNodeWithText("第 2 步 / 共 2 步").assertExists()
        tapBoard(6, 8)
        composeRule.onNodeWithTag("kids_completion_card").assertExists()
        composeRule.onNodeWithText("你学会了：双三").assertExists()

        composeRule.onNodeWithTag("kids_lesson_next_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("kids_lesson_title").assertExists()
        composeRule.onNodeWithText("先冲四再活三").assertExists()
    }

    @Test
    fun advancedVcfLessonPlaysForcedSequence() {
        setKidsContent()
        openLesson("vcf_continuous_four")

        tapBoard(7, 7)
        composeRule.onNodeWithTag("kids_lesson_correct_feedback").assertExists()
        composeRule.onNodeWithText("白棋只能去挡 J8。", substring = true).assertExists()
        composeRule.onNodeWithTag("kids_lesson_continue_button").performScrollTo().performClick()

        tapBoard(8, 8)
        composeRule.onNodeWithTag("kids_lesson_correct_feedback").assertExists()
        composeRule.onNodeWithText("白棋只能去挡 J10。", substring = true).assertExists()
        composeRule.onNodeWithTag("kids_lesson_continue_button").performScrollTo().performClick()

        tapBoard(8, 7)
        composeRule.onNodeWithTag("kids_completion_card").assertExists()
        composeRule.onNodeWithText("你学会了：连续冲四").assertExists()
    }

    @Test
    fun advancedDefenseLessonBlocksDoubleThree() {
        setKidsContent()
        openLesson("block_double_three")

        tapBoard(7, 7)
        composeRule.onNodeWithTag("kids_completion_card").assertExists()
        composeRule.onNodeWithText("你学会了：拆双三").assertExists()
        composeRule.onNodeWithText("🎉 通关啦！").assertExists()
        composeRule.onNodeWithText("全部通关", substring = true).assertDoesNotExist()
    }

    @Test
    fun defendOpenThreeLessonCompletes() {
        setKidsContent()
        openLesson("defend_open_three")

        tapBoard(7, 5)
        composeRule.onNodeWithTag("kids_completion_card").assertExists()
        composeRule.onNodeWithText("你学会了：防活三").assertExists()
    }

    @Test
    fun winBeforeBlockLessonCompletes() {
        setKidsContent()
        openLesson("win_before_block")

        tapBoard(7, 9)
        composeRule.onNodeWithTag("kids_completion_card").assertExists()
        composeRule.onNodeWithText("你学会了：先赢").assertExists()
    }

    @Test
    fun lastLessonShowsAllCompleteOnlyWhenEveryLessonIsDone() {
        val previousLessons = kidsLessons.dropLast(1).map { it.id }.toSet()
        setKidsContent(progress = InMemoryKidsProgress(previousLessons))
        openLesson("block_four_three")

        tapBoard(7, 7)
        composeRule.onNodeWithTag("kids_completion_card").assertExists()
        composeRule.onNodeWithText("全部通关", substring = true).assertExists()
    }

    @Test
    fun replayResetsTheLessonAndKeepsTheStar() {
        setKidsContent()
        openLesson("make_open_four")

        tapBoard(7, 5)
        composeRule.onNodeWithTag("kids_completion_card").assertExists()
        composeRule.onNodeWithTag("kids_lesson_replay_button").performScrollTo().performClick()

        composeRule.onNodeWithTag("kids_completion_card").assertDoesNotExist()
        composeRule.onNodeWithTag("kids_lesson_prompt").assertExists()
        composeRule.onNodeWithTag("kids_lesson_board").assertExists()

        composeRule.onNodeWithTag("kids_lesson_back_to_list_button").performScrollTo().performClick()
        composeRule.onNodeWithText("已通关 1 / 20 关", substring = true).assertExists()
    }

    private fun setKidsContent(progress: KidsProgressStore = InMemoryKidsProgress()) {
        composeRule.setContent {
            var selectedLessonId by remember { mutableStateOf<String?>(null) }
            GobangptTheme {
                KidsLessonsScreen(
                    lessons = kidsLessons,
                    selectedLessonId = selectedLessonId,
                    progress = progress,
                    onSelectLesson = { selectedLessonId = it.id },
                    onBackToGame = {},
                    onBackToList = { selectedLessonId = null },
                    onNextLesson = { selectedLessonId = it.id },
                )
            }
        }
    }

    private fun openLesson(id: String) {
        composeRule.onNodeWithTag("kids_lesson_$id").performScrollTo().performClick()
    }

    private fun tapBoard(row: Int, column: Int) {
        val board = composeRule.onNodeWithTag("kids_lesson_board")
        // 手机竖屏的棋盘在滚动列里，平板横屏的棋盘在固定左栏里。
        runCatching { board.performScrollTo() }
        board.performTouchInput {
            val step = width / (BOARD_SIZE + 1f)
            click(Offset(step * (column + 1), step * (row + 1)))
        }
    }
}

class KidsLessonsRotationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun lessonProgressSurvivesRotation() {
        composeRule.onNodeWithTag("kids_lessons_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("kids_lesson_double_three").performScrollTo().performClick()

        tapBoard(7, 7)
        composeRule.onNodeWithTag("kids_lesson_continue_button").performScrollTo().performClick()
        composeRule.onNodeWithText("第 2 步 / 共 2 步").assertExists()

        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()
        composeRule.onNodeWithText("第 2 步 / 共 2 步").assertExists()
        composeRule.onNodeWithTag("kids_lesson_prompt").assertExists()
    }

    private fun tapBoard(row: Int, column: Int) {
        val board = composeRule.onNodeWithTag("kids_lesson_board")
        // 手机竖屏的棋盘在滚动列里，平板横屏的棋盘在固定左栏里。
        runCatching { board.performScrollTo() }
        board.performTouchInput {
            val step = width / (BOARD_SIZE + 1f)
            click(Offset(step * (column + 1), step * (row + 1)))
        }
    }
}
