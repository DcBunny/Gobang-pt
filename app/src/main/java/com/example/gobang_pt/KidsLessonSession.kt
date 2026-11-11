package com.example.gobang_pt

data class KidsLessonSession(
    val lesson: KidsLesson,
    val moves: List<Position> = emptyList(),
) {
    init {
        require(moves.size <= lesson.steps.size) { "Session cannot exceed the lesson steps" }
    }

    val stepIndex: Int
        get() = moves.size

    val isCompleted: Boolean
        get() = stepIndex >= lesson.steps.size

    val currentStep: KidsStep?
        get() = lesson.steps.getOrNull(stepIndex)

    val lastChildMove: Position?
        get() = moves.lastOrNull()

    val lastWhiteReply: Position?
        get() = if (moves.isEmpty()) null else lesson.steps[moves.lastIndex].whiteReply

    val position: LessonPosition
        get() {
            var result = lesson.initial
            lesson.steps.take(moves.size).forEachIndexed { index, step ->
                result = result.withBlack(moves[index])
                step.whiteReply?.let { reply -> result = result.withWhite(reply) }
            }
            return result
        }

    fun toGobangGame(): GobangGame = lesson.initial.toGobangGame(
        buildList {
            lesson.steps.take(moves.size).forEachIndexed { index, step ->
                add(Move(moves[index], Stone.BLACK))
                step.whiteReply?.let { reply -> add(Move(reply, Stone.WHITE)) }
            }
        },
    )

    fun submit(position: Position): KidsSubmitResult {
        val step = currentStep ?: return KidsSubmitResult.Completed
        if (position !in step.answers) return KidsSubmitResult.Wrong(step, position)
        return KidsSubmitResult.Correct(
            session = copy(moves = moves + position),
            step = step,
            stepIndex = stepIndex,
        )
    }

    fun encodeMoves(): String = moves.joinToString(";") { move -> "${move.row},${move.column}" }

    companion object {
        fun restore(lesson: KidsLesson, encoded: String): KidsLessonSession {
            var session = KidsLessonSession(lesson)
            decodedMoves(encoded).forEach { position ->
                when (val result = session.submit(position)) {
                    is KidsSubmitResult.Correct -> session = result.session
                    is KidsSubmitResult.Wrong, KidsSubmitResult.Completed -> return session
                }
            }
            return session
        }

        private fun decodedMoves(encoded: String): List<Position> = encoded
            .split(';')
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split(',')
                val row = parts.getOrNull(0)?.toIntOrNull()
                val column = parts.getOrNull(1)?.toIntOrNull()
                if (row == null || column == null) {
                    null
                } else {
                    Position(row, column).takeIf(Position::isValid)
                }
            }
    }
}

sealed interface KidsSubmitResult {
    data class Correct(
        val session: KidsLessonSession,
        val step: KidsStep,
        val stepIndex: Int,
    ) : KidsSubmitResult

    data class Wrong(val step: KidsStep, val attempt: Position) : KidsSubmitResult

    data object Completed : KidsSubmitResult
}
