package com.example.gobang_pt

import kotlin.math.abs
import kotlin.random.Random

enum class PracticeGoal(val label: String, val guidance: String) {
    OPEN_TWO("活二", "先连接两个黑子，并保持两端开放。"),
    OPEN_THREE("活三", "扩展成活三，确保下一手能形成活四。"),
    CLOSED_FOUR("冲四", "制造一个白方必须立即回应的冲四。"),
    DOUBLE_THREE("双三", "寻找两条活三线路的交叉点。"),
    OPEN_FOUR("活四", "形成活四，保留两个成五点。"),
    DOUBLE_FOUR("双四", "寻找能同时形成两个四的交叉点。"),
    FIVE("五连", "选择任意成五点，完成五连。"),
}

data class PracticeScenario(
    val layoutLesson: LayoutLesson,
    val goals: List<PracticeGoal>,
) {
    val id: String
        get() = layoutLesson.id

    fun newGame(): GobangGame = layoutLesson.initialPosition.toGobangGame().copy(
        moves = emptyList(),
        currentPlayer = Stone.BLACK,
        winner = null,
        winningLine = emptyList(),
        isDraw = false,
    )

    fun replay(moves: List<Position>): GobangGame = moves.fold(newGame()) { game, position ->
        game.play(position)
    }

    fun replay(encodedMoves: String): GobangGame = replay(
        encodedMoves.split(';').filter { it.isNotBlank() }.map { move ->
            val (row, column) = move.split(',').map(String::toInt)
            Position(row, column)
        },
    )

    fun encode(game: GobangGame): String = game.moves.joinToString(";") { move ->
        "${move.position.row},${move.position.column}"
    }

    fun undo(game: GobangGame): GobangGame {
        val movesToUndo = when (game.moves.lastOrNull()?.stone) {
            Stone.BLACK -> 1
            Stone.WHITE -> 2
            null -> 0
        }
        val remainingMoves = game.moves.dropLast(movesToUndo).map(Move::position)
        return replay(remainingMoves)
    }

    fun chooseWhiteMove(game: GobangGame): Position? = GobangAi.chooseMove(
        game = game,
        difficulty = AiDifficulty.MEDIUM,
        random = Random(id.hashCode() + game.moves.size),
    )
}

val practiceScenarios = listOf(
    PracticeScenario(
        layoutLesson = layoutLessons.first { it.id == "center_development" },
        goals = listOf(
            PracticeGoal.OPEN_TWO,
            PracticeGoal.OPEN_THREE,
            PracticeGoal.OPEN_FOUR,
            PracticeGoal.FIVE,
        ),
    ),
    PracticeScenario(
        layoutLesson = layoutLessons.first { it.id == "connected_diagonals" },
        goals = listOf(PracticeGoal.DOUBLE_THREE, PracticeGoal.OPEN_FOUR, PracticeGoal.FIVE),
    ),
    PracticeScenario(
        layoutLesson = layoutLessons.first { it.id == "force_then_switch" },
        goals = listOf(PracticeGoal.CLOSED_FOUR, PracticeGoal.OPEN_FOUR, PracticeGoal.FIVE),
    ),
    PracticeScenario(
        layoutLesson = layoutLessons.first { it.id == "double_four" },
        goals = listOf(PracticeGoal.DOUBLE_FOUR, PracticeGoal.FIVE),
    ),
    PracticeScenario(
        layoutLesson = layoutLessons.first { it.id == "turtle_formation" },
        goals = listOf(PracticeGoal.OPEN_THREE, PracticeGoal.OPEN_FOUR, PracticeGoal.FIVE),
    ),
    PracticeScenario(
        layoutLesson = layoutLessons.first { it.id == "lightning_formation" },
        goals = listOf(PracticeGoal.CLOSED_FOUR, PracticeGoal.OPEN_FOUR, PracticeGoal.FIVE),
    ),
    PracticeScenario(
        layoutLesson = layoutLessons.first { it.id == "lobster_formation" },
        goals = listOf(PracticeGoal.DOUBLE_THREE, PracticeGoal.OPEN_FOUR, PracticeGoal.FIVE),
    ),
    PracticeScenario(
        layoutLesson = layoutLessons.first { it.id == "moon_formation" },
        goals = listOf(
            PracticeGoal.CLOSED_FOUR,
            PracticeGoal.OPEN_THREE,
            PracticeGoal.OPEN_FOUR,
            PracticeGoal.FIVE,
        ),
    ),
    PracticeScenario(
        layoutLesson = layoutLessons.first { it.id == "sword_formation" },
        goals = listOf(PracticeGoal.CLOSED_FOUR, PracticeGoal.OPEN_FOUR, PracticeGoal.FIVE),
    ),
    PracticeScenario(
        layoutLesson = layoutLessons.first { it.id == "sickle_formation" },
        goals = listOf(PracticeGoal.DOUBLE_FOUR, PracticeGoal.FIVE),
    ),
)

data class PracticeAnalysis(
    val achievedGoals: Set<PracticeGoal>,
    val currentGoal: PracticeGoal?,
    val latestAchievedGoal: PracticeGoal?,
)

data class PracticeRecommendation(
    val move: Position,
    val text: String,
)

object PracticeCoach {
    fun analyze(scenario: PracticeScenario, game: GobangGame): PracticeAnalysis {
        val achieved = linkedSetOf<PracticeGoal>()
        var latestAchieved: PracticeGoal? = null
        var replay = scenario.newGame()
        game.moves.forEach { move ->
            replay = replay.play(move.position)
            if (move.stone == Stone.BLACK) {
                val patterns = GobangShapes.analyzeMove(replay.board, move.position, Stone.BLACK)
                scenario.goals.forEachIndexed { index, goal ->
                    if (goal.isFormed(patterns)) {
                        scenario.goals.take(index + 1).forEach { achieved.add(it) }
                        latestAchieved = goal
                    }
                }
            }
        }
        return PracticeAnalysis(
            achievedGoals = achieved,
            currentGoal = scenario.goals.firstOrNull { it !in achieved },
            latestAchievedGoal = latestAchieved,
        )
    }

    fun recommend(scenario: PracticeScenario, game: GobangGame): PracticeRecommendation? {
        val analysis = analyze(scenario, game)
        val goal = analysis.currentGoal ?: return null
        if (game.isOver || game.currentPlayer != Stone.BLACK) return null

        val blackWin = GobangShapes.immediateWinningMoves(game.board, Stone.BLACK).firstOrNull()
        if (blackWin != null) {
            return PracticeRecommendation(blackWin, "在 ${blackWin.notation()} 落子即可连成五子。")
        }

        val whiteWin = GobangShapes.immediateWinningMoves(game.board, Stone.WHITE).firstOrNull()
        if (whiteWin != null) {
            return PracticeRecommendation(whiteWin, "白方已有直接成五威胁，先在 ${whiteWin.notation()} 防守。")
        }

        val targetMoves = allEmptyPositions(game).filter { move ->
            val updated = game.play(move)
            goal.isFormed(GobangShapes.analyzeMove(updated.board, move, Stone.BLACK))
        }
        val targetMove = scenario.layoutLesson.steps.firstOrNull { it.move in targetMoves }?.move
            ?: targetMoves.minWithOrNull(compareBy({ centerDistance(it) }, { it.row }, { it.column }))
        if (targetMove != null) {
            return PracticeRecommendation(
                targetMove,
                "在 ${targetMove.notation()} 落子可以形成${goal.label}。",
            )
        }

        val fallback = GobangAi.chooseMove(
            game = game,
            difficulty = AiDifficulty.HARD,
            random = Random(scenario.id.hashCode() + game.moves.size),
        ) ?: return null
        return PracticeRecommendation(
            fallback,
            "建议 ${fallback.notation()}，先保持棋子连接并保留开放端，再寻找${goal.label}。",
        )
    }

    private fun PracticeGoal.isFormed(patterns: List<DirectionPattern>): Boolean = when (this) {
        PracticeGoal.OPEN_TWO -> patterns.any { it.count == 2 && it.openEnds == 2 }
        PracticeGoal.OPEN_THREE -> patterns.any { it.hasOpenThree }
        PracticeGoal.CLOSED_FOUR -> patterns.any { it.winningPoints == 1 }
        PracticeGoal.DOUBLE_THREE -> patterns.count { it.hasOpenThree } >= 2
        PracticeGoal.OPEN_FOUR -> patterns.any { it.winningPoints >= 2 }
        PracticeGoal.DOUBLE_FOUR -> patterns.count { it.winningPoints > 0 } >= 2
        PracticeGoal.FIVE -> patterns.any { it.count >= 5 }
    }

    private fun allEmptyPositions(game: GobangGame): List<Position> =
        List(BOARD_SIZE * BOARD_SIZE) { index ->
            Position(index / BOARD_SIZE, index % BOARD_SIZE)
        }.filter { game.stoneAt(it) == null }

    private fun centerDistance(position: Position): Int =
        abs(position.row - BOARD_SIZE / 2) + abs(position.column - BOARD_SIZE / 2)
}

internal fun Position.notation(): String = "${'A' + column}${row + 1}"
