package com.example.gobang_pt

internal data class LessonPlaybackFrame(
    val game: GobangGame,
    val title: String,
    val explanation: String,
    val highlights: Set<Position> = emptySet(),
)

internal data class LessonPlayback(
    val title: String,
    val subtitle: String,
    val summary: String,
    val frames: List<LessonPlaybackFrame>,
    val backLabelResource: Int,
) {
    init {
        require(frames.isNotEmpty()) { "A lesson playback needs at least one frame" }
    }
}

internal fun PatternLesson.toPlayback(): LessonPlayback {
    val answer = exercise.answers.minWithOrNull(compareBy(Position::row, Position::column))!!
    return LessonPlayback(
        title = title,
        subtitle = when (level) {
            PatternLevel.BASIC -> "基础棋型"
            PatternLevel.COMBINATION -> "组合棋型"
        },
        summary = summary,
        frames = listOf(
            LessonPlaybackFrame(
                game = example.position.toGobangGame(),
                title = "棋型示例",
                explanation = explanation,
                highlights = example.highlights,
            ),
            LessonPlaybackFrame(
                game = exercise.position.toGobangGame(),
                title = "应用局面",
                explanation = exercise.task,
                highlights = exercise.answers,
            ),
            LessonPlaybackFrame(
                game = exercise.position.withBlack(answer).toGobangGame(),
                title = "标准答案 · ${answer.notation()}",
                explanation = "${exercise.successExplanation}\n\n防守提示：$defenseTip",
                highlights = setOf(answer),
            ),
        ),
        backLabelResource = R.string.back_to_pattern_lessons,
    )
}

internal fun LayoutLesson.toPlayback(): LessonPlayback {
    val frames = mutableListOf(
        LessonPlaybackFrame(
            game = initialPosition.toGobangGame(),
            title = "初始局面",
            explanation = summary,
        ),
    )
    var position = initialPosition
    steps.forEachIndexed { index, step ->
        position = position.withBlack(step.move)
        frames += LessonPlaybackFrame(
            game = position.toGobangGame(),
            title = "黑方 · ${step.move.notation()}",
            explanation = "${step.prompt}\n\n${step.explanation}",
            highlights = buildSet {
                add(step.move)
                if (index == steps.lastIndex) addAll(winningMoves)
            },
        )
        step.whiteResponse?.let { response ->
            position = position.withWhite(response)
            frames += LessonPlaybackFrame(
                game = position.toGobangGame(),
                title = "白方 · ${response.notation()}",
                explanation = step.whiteExplanation,
                highlights = setOf(response),
            )
        }
    }

    val winningMove = winningMoves.minWithOrNull(compareBy(Position::row, Position::column))!!
    frames += LessonPlaybackFrame(
        game = position.toGobangGame().play(winningMove),
        title = "黑方 · ${winningMove.notation()} · 五连",
        explanation = "$finalExplanation\n\n$completion",
        highlights = setOf(winningMove),
    )
    return LessonPlayback(
        title = title,
        subtitle = "目标：$target",
        summary = summary,
        frames = frames,
        backLabelResource = R.string.back_to_layout_lessons,
    )
}
