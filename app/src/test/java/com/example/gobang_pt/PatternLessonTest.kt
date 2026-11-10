package com.example.gobang_pt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternLessonTest {
    @Test
    fun catalogHasEightUniqueLessonsWithValidContent() {
        assertEquals(8, patternLessons.size)
        assertEquals(patternLessons.size, patternLessons.map(PatternLesson::id).distinct().size)
        patternLessons.forEach { lesson ->
            assertTrue(lesson.title.isNotBlank())
            assertTrue(lesson.summary.isNotBlank())
            assertTrue(lesson.explanation.isNotBlank())
            assertTrue(lesson.defenseTip.isNotBlank())
            assertTrue(lesson.example.position.black.isNotEmpty())
            assertTrue(lesson.example.highlights.all(Position::isValid))
            assertTrue(lesson.exercise.task.isNotBlank())
            assertTrue(lesson.exercise.hint.isNotBlank())
            assertTrue(lesson.exercise.successExplanation.isNotBlank())
            assertTrue(lesson.exercise.answers.all { lesson.exercise.position.stoneAt(it) == null })
        }
    }

    @Test
    fun exerciseAnswersContainEveryMoveThatFormsTheTargetShape() {
        patternLessons.forEach { lesson ->
            val actualAnswers = allPositions().filter { candidate ->
                lesson.exercise.position.stoneAt(candidate) == null &&
                    matchesExercise(lesson.id, lesson.exercise.position.withBlack(candidate), candidate)
            }.toSet()

            assertEquals(lesson.id, lesson.exercise.answers, actualAnswers)
        }
    }

    @Test
    fun everyExampleShowsTheShapeNamedByTheLesson() {
        patternLessons.forEach { lesson ->
            val example = lesson.example.position
            val matches = example.black.any { origin ->
                when (lesson.id) {
                    "open_four" -> example.hasLine(origin, 4, 2)
                    "closed_four" -> example.hasLine(origin, 4, 1)
                    "open_three" -> example.hasLine(origin, 3, 2)
                    "closed_three" -> example.hasLine(origin, 3, 1)
                    "open_two" -> example.hasLine(origin, 2, 2)
                    "closed_two" -> example.hasLine(origin, 2, 1)
                    "double_three" -> example.lineCount(origin, 3, 2) >= 2
                    "four_three" -> example.hasLine(origin, 4, 1) && example.hasLine(origin, 3, 2)
                    else -> false
                }
            }
            assertTrue(lesson.id, matches)
        }
    }

    @Test
    fun arbitraryPositionBuildsSnapshotAndAddsBlackAnswers() {
        val exercise = patternLessons.first().exercise
        val answer = exercise.answers.first()
        val updated = exercise.position.withBlack(answer)
        val game = updated.toGobangGame()

        assertEquals(Stone.BLACK, updated.stoneAt(answer))
        assertEquals(updated.black.size + updated.white.size, game.moves.size)
        updated.black.forEach { position -> assertEquals(Stone.BLACK, game.stoneAt(position)) }
        updated.white.forEach { position -> assertEquals(Stone.WHITE, game.stoneAt(position)) }
    }

    @Test
    fun multipleAnswersPreviewOnlyTheLatestAlternative() {
        val exercise = patternLessons.first().exercise
        val first = patternPoint("e5")
        val second = patternPoint("j10")
        val preview = exercise.previewPosition(listOf(first, second))

        assertEquals(exercise.position.black.size + 1, preview.black.size)
        assertEquals(Stone.BLACK, preview.stoneAt(second))
        assertTrue(preview.stoneAt(first) == null)
    }

    private fun matchesExercise(id: String, position: PatternPosition, origin: Position): Boolean = when (id) {
        "open_four", "closed_four" -> position.linesAt(origin).any { it.count >= 5 }
        "open_three" -> position.hasLine(origin, 4, 2)
        "closed_three" -> position.hasLine(origin, 4, 1)
        "open_two" -> position.hasLine(origin, 3, 2)
        "closed_two" -> position.hasLine(origin, 3, 1)
        "double_three" -> position.lineCount(origin, 3, 2) >= 2
        "four_three" -> position.hasLine(origin, 4, 1) && position.hasLine(origin, 3, 2)
        else -> false
    }

    private fun PatternPosition.hasLine(origin: Position, count: Int, openEnds: Int): Boolean =
        linesAt(origin).any { it.count == count && it.openEnds == openEnds }

    private fun PatternPosition.lineCount(origin: Position, count: Int, openEnds: Int): Int =
        linesAt(origin).count { it.count == count && it.openEnds == openEnds }

    private fun PatternPosition.linesAt(origin: Position): List<LineStats> {
        require(stoneAt(origin) == Stone.BLACK)
        return listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1).map { (rowDelta, columnDelta) ->
            var count = 1
            var openEnds = 0
            listOf(-1, 1).forEach { sign ->
                var distance = 1
                while (true) {
                    val position = Position(
                        origin.row + sign * rowDelta * distance,
                        origin.column + sign * columnDelta * distance,
                    )
                    if (!position.isValid || stoneAt(position) != Stone.BLACK) break
                    count++
                    distance++
                }
                val end = Position(
                    origin.row + sign * rowDelta * distance,
                    origin.column + sign * columnDelta * distance,
                )
                if (end.isValid && stoneAt(end) == null) openEnds++
            }
            LineStats(count, openEnds)
        }
    }

    private fun allPositions(): List<Position> = List(BOARD_SIZE * BOARD_SIZE) { index ->
        Position(index / BOARD_SIZE, index % BOARD_SIZE)
    }

    private data class LineStats(val count: Int, val openEnds: Int)
}
