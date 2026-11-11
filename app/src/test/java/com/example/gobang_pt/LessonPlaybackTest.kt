package com.example.gobang_pt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonPlaybackTest {
    @Test
    fun patternLessonsPlayFromExampleToOneLegalAnswer() {
        patternLessons.forEach { lesson ->
            val playback = lesson.toPlayback()

            assertEquals(lesson.id, 3, playback.frames.size)
            assertEquals(
                lesson.id,
                lesson.example.position.toGobangGame().board,
                playback.frames[0].game.board,
            )
            assertEquals(
                lesson.id,
                lesson.exercise.position.toGobangGame().board,
                playback.frames[1].game.board,
            )
            assertEquals(
                lesson.id,
                1,
                lesson.exercise.answers.count { playback.frames.last().game.stoneAt(it) == Stone.BLACK },
            )
            assertTrue(lesson.id, playback.frames.last().explanation.contains(lesson.defenseTip))
        }
    }

    @Test
    fun layoutLessonsPlayEveryGuidedMoveAndFinishWithFive() {
        layoutLessons.forEach { lesson ->
            val playback = lesson.toPlayback()
            val expectedFrames = lesson.steps.size + lesson.steps.count { it.whiteResponse != null } + 2

            assertEquals(lesson.id, expectedFrames, playback.frames.size)
            assertEquals(
                lesson.id,
                lesson.initialPosition.toGobangGame().board,
                playback.frames.first().game.board,
            )
            playback.frames.dropLast(1).forEach { frame ->
                assertTrue(lesson.id, !frame.game.isOver)
            }
            assertEquals(lesson.id, Stone.BLACK, playback.frames.last().game.winner)
            assertTrue(lesson.id, playback.frames.last().game.winningLine.size >= 5)
            assertTrue(
                lesson.id,
                lesson.winningMoves.any { playback.frames.last().game.stoneAt(it) == Stone.BLACK },
            )
        }
    }
}
