package com.example.gobang_pt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutLessonTest {
    @Test
    fun catalogHasTenUniqueLessonsWithLegalScripts() {
        assertEquals(10, layoutLessons.size)
        assertEquals(layoutLessons.size, layoutLessons.map(LayoutLesson::id).distinct().size)
        layoutLessons.forEach { lesson ->
            assertTrue(lesson.title.isNotBlank())
            assertTrue(lesson.summary.isNotBlank())
            assertTrue(lesson.target.isNotBlank())
            assertTrue(lesson.finalExplanation.isNotBlank())
            assertTrue(lesson.completion.isNotBlank())

            var position = lesson.initialPosition
            lesson.steps.forEachIndexed { index, step ->
                assertTrue(step.prompt.isNotBlank())
                assertTrue(step.hint.isNotBlank())
                assertTrue(step.explanation.isNotBlank())
                assertNull(position.stoneAt(step.move))
                position = position.withBlack(step.move)
                assertFalse(lesson.id, position.hasFive(Stone.BLACK))

                if (index < lesson.steps.lastIndex) {
                    val response = requireNotNull(step.whiteResponse)
                    assertTrue(step.whiteExplanation.isNotBlank())
                    assertNull(position.stoneAt(response))
                    position = position.withWhite(response)
                    assertFalse(lesson.id, position.hasFive(Stone.WHITE))
                }
            }
            assertEquals(lesson.id, position, lesson.finalPosition())
        }
    }

    @Test
    fun guidedMovesBuildTheShapesExplainedByEachStep() {
        val expectations = listOf(
            Expectation("center_development", 1, 2, 2),
            Expectation("center_development", 2, 3, 2),
            Expectation("center_development", 3, 4, 2),
            Expectation("connected_diagonals", 0, 3, 2, requiredLines = 2),
            Expectation("connected_diagonals", 1, 4, 2),
            Expectation("force_then_switch", 0, 4, 1),
            Expectation("force_then_switch", 1, 4, 2),
            Expectation("double_four", 1, 4, 1, requiredLines = 2),
            Expectation("turtle_formation", 0, 3, 2),
            Expectation("turtle_formation", 1, 4, 2),
            Expectation("lightning_formation", 0, 4, 1),
            Expectation("lightning_formation", 1, 4, 1),
            Expectation("lightning_formation", 2, 4, 2),
            Expectation("lobster_formation", 0, 3, 2, requiredLines = 2),
            Expectation("lobster_formation", 1, 4, 2),
            Expectation("moon_formation", 0, 4, 1),
            Expectation("moon_formation", 1, 4, 2),
            Expectation("sword_formation", 0, 4, 1),
            Expectation("sword_formation", 1, 4, 2),
            Expectation("sickle_formation", 0, 4, 1, requiredLines = 2),
        )

        expectations.forEach { expectation ->
            val lesson = layoutLessons.first { it.id == expectation.lessonId }
            val step = lesson.steps[expectation.stepIndex]
            val position = lesson.positionBeforeStep(expectation.stepIndex).withBlack(step.move)
            val matchingLines = position.linesAt(step.move, Stone.BLACK).count { line ->
                line.count == expectation.count && line.openEnds == expectation.openEnds
            }
            assertEquals(expectation.toString(), expectation.requiredLines, matchingLines)
        }
    }

    @Test
    fun moonFormationCombinesAClosedFourWithAJumpOpenThree() {
        val lesson = layoutLessons.first { it.id == "moon_formation" }
        val step = lesson.steps.first()
        val position = lesson.positionBeforeStep(0).withBlack(step.move)
        val patterns = GobangShapes.analyzeMove(position.toGobangGame().board, step.move, Stone.BLACK)

        assertTrue(patterns.any { it.count == 4 && it.winningPoints == 1 })
        assertTrue(patterns.any { it.hasOpenThree })
    }

    @Test
    fun finalPositionsGiveBlackAnImmediateWinAfterEveryLegalWhiteReply() {
        layoutLessons.forEach { lesson ->
            val finalPosition = lesson.finalPosition()
            assertFalse(lesson.id, finalPosition.hasFive(Stone.BLACK))
            assertFalse(lesson.id, finalPosition.hasFive(Stone.WHITE))
            val baseGame = finalPosition.toGobangGame()
            val emptyPositions = allPositions().filter { finalPosition.stoneAt(it) == null }
            val actualWinningMoves = emptyPositions.filter { move ->
                baseGame.play(move).winner == Stone.BLACK
            }.toSet()
            assertEquals(lesson.id, lesson.winningMoves, actualWinningMoves)

            emptyPositions.forEach { whiteMove ->
                val afterWhite = baseGame.copy(currentPlayer = Stone.WHITE).play(whiteMove)
                assertNull("${lesson.id}: white move $whiteMove", afterWhite.winner)
                val blackCanWin = allPositions().any { blackMove ->
                    afterWhite.stoneAt(blackMove) == null &&
                        afterWhite.play(blackMove).winner == Stone.BLACK
                }
                assertTrue("${lesson.id}: white move $whiteMove", blackCanWin)
            }

            lesson.winningMoves.forEach { winningMove ->
                val finished = baseGame.play(winningMove)
                assertEquals(lesson.id, Stone.BLACK, finished.winner)
                assertTrue(lesson.id, finished.winningLine.size >= 5)
            }
        }
    }

    private fun PatternPosition.hasFive(stone: Stone): Boolean =
        (if (stone == Stone.BLACK) black else white).any { origin ->
            linesAt(origin, stone).any { line -> line.count >= 5 }
        }

    private fun PatternPosition.linesAt(origin: Position, stone: Stone): List<LineStats> {
        require(stoneAt(origin) == stone)
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
                    if (!position.isValid || stoneAt(position) != stone) break
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

    private data class Expectation(
        val lessonId: String,
        val stepIndex: Int,
        val count: Int,
        val openEnds: Int,
        val requiredLines: Int = 1,
    )
}
