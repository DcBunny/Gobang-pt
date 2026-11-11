package com.example.gobang_pt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KidsLessonTest {
    @Test
    fun catalogHasOrderedUniqueLessonsAcrossEveryStage() {
        assertEquals(8, kidsLessons.size)
        assertEquals((1..8).toList(), kidsLessons.map(KidsLesson::order))
        assertEquals(kidsLessons.size, kidsLessons.map(KidsLesson::id).distinct().size)

        KidsStage.entries.forEach { stage ->
            assertTrue("Stage $stage has no lesson", kidsLessons.any { it.stage == stage })
        }

        kidsLessons.forEach { lesson ->
            assertTrue(lesson.id, lesson.title.isNotBlank())
            assertTrue(lesson.id, lesson.patternName.isNotBlank())
            assertTrue(lesson.id, lesson.intro.isNotBlank())
            assertTrue(lesson.id, lesson.rule.isNotBlank())
            assertTrue(lesson.id, lesson.thinkingTip.isNotBlank())
            assertTrue(lesson.id, lesson.completion.isNotBlank())
            assertTrue(lesson.id, lesson.steps.size in 1..3)
            assertFalse(lesson.id, lesson.initial.hasFive(Stone.BLACK))
            assertFalse(lesson.id, lesson.initial.hasFive(Stone.WHITE))
        }
    }

    @Test
    fun everyAnswerFormsTheTaughtTarget() {
        kidsLessons.forEach { lesson ->
            lesson.steps.forEachIndexed { stepIndex, step ->
                val before = sessionAt(lesson, stepIndex).position
                assertTrue(
                    "${lesson.id} step $stepIndex has an occupied answer",
                    step.answers.all { before.stoneAt(it) == null },
                )
                step.answers.forEach { answer ->
                    val result = sessionAt(lesson, stepIndex).submit(answer)
                    assertTrue("${lesson.id} step $stepIndex answer $answer", result is KidsSubmitResult.Correct)
                    val board = before.toGobangGame().board

                    when (step.target) {
                        KidsTarget.FIVE -> {
                            assertEquals(
                                "${lesson.id} step $stepIndex answer $answer",
                                Stone.BLACK,
                                before.toGobangGame().play(answer).winner,
                            )
                        }
                        KidsTarget.BLOCK -> {
                            val after = before.withBlack(answer).toGobangGame().board
                            assertTrue(
                                "${lesson.id} step $stepIndex answer $answer",
                                GobangShapes.immediateWinningMoves(after, Stone.WHITE).isEmpty(),
                            )
                        }
                        KidsTarget.OPEN_TWO -> assertTrue(
                            "${lesson.id} step $stepIndex answer $answer",
                            GobangShapes.analyzeMove(board, answer, Stone.BLACK)
                                .any { it.count == 2 && it.openEnds == 2 },
                        )
                        KidsTarget.OPEN_THREE -> assertTrue(
                            "${lesson.id} step $stepIndex answer $answer",
                            GobangShapes.analyzeMove(board, answer, Stone.BLACK).any { it.hasOpenThree },
                        )
                        KidsTarget.OPEN_FOUR -> assertTrue(
                            "${lesson.id} step $stepIndex answer $answer",
                            GobangShapes.analyzeMove(board, answer, Stone.BLACK)
                                .any { it.winningPoints >= 2 },
                        )
                        KidsTarget.DOUBLE_THREE -> assertTrue(
                            "${lesson.id} step $stepIndex answer $answer",
                            GobangShapes.analyzeMove(board, answer, Stone.BLACK)
                                .count { it.hasOpenThree } >= 2,
                        )
                        KidsTarget.FOUR_THREE -> {
                            val patterns = GobangShapes.analyzeMove(board, answer, Stone.BLACK)
                            assertTrue(
                                "${lesson.id} step $stepIndex answer $answer needs a four",
                                patterns.any { it.winningPoints == 1 },
                            )
                            assertTrue(
                                "${lesson.id} step $stepIndex answer $answer needs an open three",
                                patterns.any { it.hasOpenThree },
                            )
                        }
                        KidsTarget.CENTER -> assertEquals(Position(7, 7), answer)
                    }
                }
            }
        }
    }

    @Test
    fun everyScriptedWhiteReplyIsLegalAndLeadsToTheNextStep() {
        kidsLessons.forEach { lesson ->
            lesson.steps.forEachIndexed { stepIndex, step ->
                val reply = step.whiteReply ?: return@forEachIndexed
                assertTrue(lesson.id, step.whiteReplyText.isNotBlank())

                val before = sessionAt(lesson, stepIndex).position
                val afterBlack = before.withBlack(canonical(step))
                assertNull(lesson.id, afterBlack.stoneAt(reply))

                val afterReply = afterBlack.withWhite(reply)
                assertFalse("${lesson.id} white reply wins", afterReply.hasFive(Stone.WHITE))
                assertEquals(lesson.id, afterReply, sessionAt(lesson, stepIndex + 1).position)
            }
        }
    }

    @Test
    fun blockLessonHasOneWhiteWinningPointAndTheAnswerRemovesIt() {
        val lesson = kidsLessons.first { it.id == "block_five" }
        val before = lesson.initial.toGobangGame()
        val whiteWinningPoints = GobangShapes.immediateWinningMoves(before.board, Stone.WHITE)

        assertEquals(setOf(lessonPoint("h8")), whiteWinningPoints.toSet())
    }

    @Test
    fun fourThreeLessonForcesTheOnlyWhiteBlock() {
        val lesson = kidsLessons.first { it.id == "four_three" }
        val step = lesson.steps.first()
        val answer = canonical(step)
        val afterBlack = sessionAt(lesson, 0).position.withBlack(answer)

        assertEquals(
            setOf(step.whiteReply),
            GobangShapes.immediateWinningMoves(afterBlack.toGobangGame().board, Stone.BLACK).toSet(),
        )
    }

    @Test
    fun comparisonLessonSeparatesOpenFourFromClosedFour() {
        val lesson = kidsLessons.first { it.id == "choose_open_four" }
        val step = lesson.steps.first()
        val trap = lessonPoint("i6")

        assertFalse(trap in step.answers)
        val trapPatterns = GobangShapes.analyzeMove(
            lesson.initial.toGobangGame().board,
            trap,
            Stone.BLACK,
        )
        assertTrue(trapPatterns.any { it.winningPoints == 1 })
        assertFalse(trapPatterns.any { it.winningPoints >= 2 })
    }

    @Test
    fun openingLessonRejectsAClosedTwo() {
        val lesson = kidsLessons.first { it.id == "opening_center" }
        val step = lesson.steps[1]
        val closedTwo = lessonPoint("i9")

        assertFalse(closedTwo in step.answers)
        val board = sessionAt(lesson, 1).position.toGobangGame().board
        assertFalse(
            GobangShapes.analyzeMove(board, closedTwo, Stone.BLACK)
                .any { it.count == 2 && it.openEnds == 2 },
        )
    }

    @Test
    fun sessionRejectsWrongMovesAndCompletesOnTheLastAnswer() {
        val lesson = kidsLessons.first { it.id == "make_open_four" }
        val session = KidsLessonSession(lesson)

        val wrong = session.submit(Position(0, 0))
        assertTrue(wrong is KidsSubmitResult.Wrong)
        assertEquals(0, session.moves.size)
        assertEquals(0, session.stepIndex)

        val answer = canonical(lesson.steps.first())
        val correct = session.submit(answer)
        assertTrue(correct is KidsSubmitResult.Correct)
        val completed = (correct as KidsSubmitResult.Correct).session
        assertTrue(completed.isCompleted)
        assertNull(completed.currentStep)
        assertEquals(1, completed.moves.size)
        assertEquals(KidsSubmitResult.Completed, completed.submit(Position(1, 1)))
    }

    @Test
    fun sessionAppliesWhiteReplyAndRestoresFromSavedMoves() {
        val lesson = kidsLessons.first { it.id == "double_three" }
        val firstStep = lesson.steps.first()
        val firstAnswer = canonical(firstStep)
        val firstResult = KidsLessonSession(lesson).submit(firstAnswer)
        assertTrue(firstResult is KidsSubmitResult.Correct)

        val afterFirst = (firstResult as KidsSubmitResult.Correct).session
        val whiteReply = requireNotNull(firstStep.whiteReply)
        assertEquals(whiteReply, afterFirst.lastWhiteReply)
        assertEquals(Stone.WHITE, afterFirst.position.stoneAt(whiteReply))
        assertEquals(1, afterFirst.stepIndex)

        val secondAnswer = canonical(lesson.steps[1])
        val secondResult = afterFirst.submit(secondAnswer)
        assertTrue(secondResult is KidsSubmitResult.Correct)
        val complete = (secondResult as KidsSubmitResult.Correct).session
        assertTrue(complete.isCompleted)
        assertEquals(complete, KidsLessonSession.restore(lesson, complete.encodeMoves()))
    }

    @Test
    fun corruptedSavedMovesStopAtTheFirstInvalidMove() {
        val lesson = kidsLessons.first { it.id == "double_three" }
        val firstAnswer = canonical(lesson.steps.first())
        val encoded = "${firstAnswer.row},${firstAnswer.column};0,0"

        val restored = KidsLessonSession.restore(lesson, encoded)
        assertEquals(1, restored.stepIndex)
        assertEquals(listOf(firstAnswer), restored.moves)
        assertEquals(0, KidsLessonSession.restore(lesson, "bad;99,99").stepIndex)
    }

    @Test
    fun inMemoryProgressIsIdempotent() {
        val progress = InMemoryKidsProgress()
        assertTrue(progress.completedLessonIds().isEmpty())

        progress.markCompleted("five_in_a_row")
        progress.markCompleted("five_in_a_row")
        progress.markCompleted("block_five")

        assertEquals(setOf("five_in_a_row", "block_five"), progress.completedLessonIds())
    }

    private fun sessionAt(lesson: KidsLesson, stepIndex: Int): KidsLessonSession {
        var session = KidsLessonSession(lesson)
        repeat(stepIndex) { index ->
            val result = session.submit(canonical(lesson.steps[index]))
            assertTrue("${lesson.id} step $index", result is KidsSubmitResult.Correct)
            session = (result as KidsSubmitResult.Correct).session
        }
        return session
    }

    private fun canonical(step: KidsStep): Position =
        step.answers.minWithOrNull(compareBy({ it.row }, { it.column }))!!

    private fun LessonPosition.hasFive(stone: Stone): Boolean {
        val game = toGobangGame()
        val stones = if (stone == Stone.BLACK) black else white
        return stones.any { origin ->
            GobangShapes.analyzeMove(game.board, origin, stone).any { it.count >= 5 }
        }
    }
}
