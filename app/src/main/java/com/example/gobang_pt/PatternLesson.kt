package com.example.gobang_pt

enum class PatternLevel {
    BASIC,
    COMBINATION,
}

data class PatternPosition(
    val black: Set<Position>,
    val white: Set<Position> = emptySet(),
) {
    init {
        require(black.all(Position::isValid) && white.all(Position::isValid)) {
            "Pattern stones must be on the board"
        }
        require((black intersect white).isEmpty()) { "Pattern stones cannot overlap" }
    }

    fun stoneAt(position: Position): Stone? = when (position) {
        in black -> Stone.BLACK
        in white -> Stone.WHITE
        else -> null
    }

    fun withBlack(position: Position): PatternPosition {
        require(position.isValid && stoneAt(position) == null) { "Answer must be an empty board point" }
        return copy(black = black + position)
    }

    fun toGobangGame(): GobangGame {
        val moves = black.map { Move(it, Stone.BLACK) } + white.map { Move(it, Stone.WHITE) }
        val board = List<Stone?>(BOARD_SIZE * BOARD_SIZE) { null }.toMutableList()
        moves.forEach { move ->
            board[move.position.row * BOARD_SIZE + move.position.column] = move.stone
        }
        return GobangGame(board = board, moves = moves)
    }
}

data class PatternExample(
    val position: PatternPosition,
    val highlights: Set<Position>,
)

data class PatternExercise(
    val position: PatternPosition,
    val task: String,
    val answers: Set<Position>,
    val hint: String,
    val successExplanation: String,
) {
    fun previewPosition(foundAnswers: List<Position>): PatternPosition {
        require(foundAnswers.all { it in answers } && foundAnswers.distinct().size == foundAnswers.size) {
            "Preview answers must be distinct exercise answers"
        }
        return foundAnswers.lastOrNull()?.let(position::withBlack) ?: position
    }

    init {
        require(answers.isNotEmpty()) { "An exercise needs at least one answer" }
        require(answers.all { it.isValid && position.stoneAt(it) == null }) {
            "Exercise answers must be empty board points"
        }
    }
}

data class PatternLesson(
    val id: String,
    val title: String,
    val level: PatternLevel,
    val summary: String,
    val explanation: String,
    val defenseTip: String,
    val example: PatternExample,
    val exercise: PatternExercise,
)

val patternLessons = listOf(
    PatternLesson(
        id = "open_four",
        title = "活四",
        level = PatternLevel.BASIC,
        summary = "四子相连且两端开放，两端都能立即成五。",
        explanation = "连续四个同色棋子，两端都没有被棋子或边界封住，就是活四。黑方在任一端落子都能连成五子。",
        defenseTip = "活四的两个成五点无法同时封住，应在对手形成活四之前提前防守。",
        example = PatternExample(
            position = patternPosition(black = "f8 g8 h8 i8"),
            highlights = patternPoints("f8 g8 h8 i8 e8 j8"),
        ),
        exercise = PatternExercise(
            position = patternPosition(black = "f6 g7 h8 i9"),
            task = "斜线上已经有活四，找出两个能立即连成五子的落点。",
            answers = patternPoints("e5 j10"),
            hint = "沿着四个黑子的斜线观察两端，两个端点都没有被挡住。",
            successExplanation = "E5 和 J10 都能补上五子连线。活四拥有两个获胜点，是最直接的必胜威胁。",
        ),
    ),
    PatternLesson(
        id = "closed_four",
        title = "冲四",
        level = PatternLevel.BASIC,
        summary = "只有一个点能立即成五，对手通常必须马上防守。",
        explanation = "四个同色棋子已经具备一手成五的机会，但另一端被挡住或中间尚有空隙，只剩一个有效成五点，就是冲四。",
        defenseTip = "面对冲四，应立即占据唯一的成五点，不能忽略这个直接威胁。",
        example = PatternExample(
            position = patternPosition(black = "f8 g8 h8 i8", white = "e8"),
            highlights = patternPoints("f8 g8 h8 i8 j8"),
        ),
        exercise = PatternExercise(
            position = patternPosition(black = "f8 g8 h8 j8", white = "e8"),
            task = "黑棋只差一手就能连成五子，找出唯一的成五点。",
            answers = patternPoints("i8"),
            hint = "四个黑子之间还有一个空隙，不需要只看横线两端。",
            successExplanation = "I8 补上后，F8 至 J8 连成五子。即使四子不完全连续，也可能形成冲四。",
        ),
    ),
    PatternLesson(
        id = "open_three",
        title = "活三",
        level = PatternLevel.BASIC,
        summary = "下一手可以形成两端都开放的活四。",
        explanation = "三个同色棋子保持足够开放的空间，下一手能形成两端都开放的活四，就是活三。它比冲四慢一手，但后续威胁很强。",
        defenseTip = "尽量在活三扩展成活四之前处理，同时观察两端以及可能形成的跳活三。",
        example = PatternExample(
            position = patternPosition(black = "g8 h8 i8"),
            highlights = patternPoints("g8 h8 i8 f8 j8"),
        ),
        exercise = PatternExercise(
            position = patternPosition(black = "g6 h7 i8"),
            task = "找出两个能让黑棋形成活四的落点。",
            answers = patternPoints("f5 j9"),
            hint = "分别向斜线两端延伸，并确认形成的四子两端仍有余地。",
            successExplanation = "F5 或 J9 都能把三个黑子扩展为活四，下一手将拥有两个成五点。",
        ),
    ),
    PatternLesson(
        id = "closed_three",
        title = "眠三",
        level = PatternLevel.BASIC,
        summary = "一侧受到限制，继续扩展只能形成冲四。",
        explanation = "三个同色棋子的一侧被对手或边界限制，无法形成活四，只能向开放的一端发展成冲四，就是眠三。",
        defenseTip = "封住仍可发展的开口即可，但如果有冲四等更紧迫的威胁，应先处理直接危险。",
        example = PatternExample(
            position = patternPosition(black = "g8 h8 i8", white = "f8"),
            highlights = patternPoints("g8 h8 i8 j8"),
        ),
        exercise = PatternExercise(
            position = patternPosition(black = "g6 h7 i8", white = "f5"),
            task = "白棋已经封住一端，找出黑棋能形成冲四的落点。",
            answers = patternPoints("j9"),
            hint = "沿三个黑子的方向，看向没有白棋阻挡的另一端。",
            successExplanation = "J9 能形成四子连线，但 F5 已被白棋占据，所以只形成冲四而不是活四。",
        ),
    ),
    PatternLesson(
        id = "open_two",
        title = "活二",
        level = PatternLevel.BASIC,
        summary = "两个棋子保留开放空间，可以继续发展成活三。",
        explanation = "两个同色棋子周围有足够空间，下一手能形成活三，就是活二。它是常见的布局起点，需要结合位置和后续连接判断价值。",
        defenseTip = "活二不会立即成五，不必像冲四一样急着封堵，但应留意它与周围棋子的连接。",
        example = PatternExample(
            position = patternPosition(black = "h8 i8"),
            highlights = patternPoints("h8 i8 g8 j8"),
        ),
        exercise = PatternExercise(
            position = patternPosition(black = "h7 i8"),
            task = "找出两个能让黑棋形成活三的落点。",
            answers = patternPoints("g6 j9"),
            hint = "沿两个黑子的斜线向两端扩展，并检查新三子的两端是否开放。",
            successExplanation = "G6 或 J9 都能形成活三。活二的价值在于保留向多条线路继续发展的空间。",
        ),
    ),
    PatternLesson(
        id = "closed_two",
        title = "眠二",
        level = PatternLevel.BASIC,
        summary = "两个棋子的一侧受限，只能向另一端发展。",
        explanation = "两个同色棋子的一侧被挡住，继续扩展只能形成眠三，就是眠二。它的威胁较弱，但仍可与其他线路配合。",
        defenseTip = "可以优先处理更活跃的形状，同时留意眠二的开放端以及它与其他棋子的呼应。",
        example = PatternExample(
            position = patternPosition(black = "h8 i8", white = "g8"),
            highlights = patternPoints("h8 i8 j8"),
        ),
        exercise = PatternExercise(
            position = patternPosition(black = "h7 i8", white = "g6"),
            task = "白棋封住了一端，找出黑棋能形成眠三的落点。",
            answers = patternPoints("j9"),
            hint = "从两个黑子的开放端继续延伸，不要走向白棋占据的位置。",
            successExplanation = "J9 能形成三个连续黑子，但 G6 被白棋封住，因此形成的是眠三。",
        ),
    ),
    PatternLesson(
        id = "double_three",
        title = "双三",
        level = PatternLevel.COMBINATION,
        summary = "一手棋同时形成两个活三，制造两条进攻线。",
        explanation = "落子后同时形成两个活三，就是双三。对手难以同时照顾两条线路。本应用采用无禁手规则，黑棋可以正常使用双三。",
        defenseTip = "双三的关键是两条线路的交叉点，应在交叉点形成前拆解其中一条线路。",
        example = PatternExample(
            position = patternPosition(black = "g6 g7 g8 h8 i8"),
            highlights = patternPoints("g6 g7 g8 h8 i8 g5 g9 f8 j8"),
        ),
        exercise = PatternExercise(
            position = patternPosition(black = "f6 g7 f10 g9"),
            task = "找出一个能同时形成两个活三的交叉落点。",
            answers = patternPoints("h8"),
            hint = "分别观察两条斜线，找到它们共同缺少的中心点。",
            successExplanation = "H8 同时补齐两条斜线，形成两个活三。当前无禁手规则允许这一进攻。",
        ),
    ),
    PatternLesson(
        id = "four_three",
        title = "四三",
        level = PatternLevel.COMBINATION,
        summary = "一手同时形成冲四和活三，把直接威胁与后续攻势结合。",
        explanation = "一手棋同时形成一个四和一个活三，就是四三。对手必须处理四的成五威胁，而活三仍能继续发展，因此四三常能形成连续攻势。",
        defenseTip = "不要只看单独的一条线，应提前识别四与三共用的关键点，并在组合形成前防守。",
        example = PatternExample(
            position = patternPosition(black = "f6 g7 h8 f8 g8 i8", white = "e8"),
            highlights = patternPoints("f6 g7 h8 f8 g8 i8 j8 e5 i9"),
        ),
        exercise = PatternExercise(
            position = patternPosition(black = "f10 g9 i7 h10 h9", white = "e11"),
            task = "找出一手能同时形成冲四和活三的落点。",
            answers = patternPoints("h8"),
            hint = "一个点要同时补齐斜线上的四和竖线上的三。",
            successExplanation = "H8 让斜线形成冲四，同时让竖线形成活三。白方挡住冲四后，黑方仍能利用活三继续进攻。",
        ),
    ),
)

internal fun patternPoint(coordinate: String): Position {
    require(coordinate.length in 2..3) { "Invalid coordinate: $coordinate" }
    val column = coordinate.first().lowercaseChar() - 'a'
    val row = coordinate.drop(1).toIntOrNull()?.minus(1) ?: -1
    return Position(row, column).also { position ->
        require(position.isValid) { "Invalid coordinate: $coordinate" }
    }
}

private fun patternPoints(notation: String): Set<Position> = notation.split(' ')
    .map(::patternPoint)
    .toSet()

private fun patternPosition(black: String, white: String = "") = PatternPosition(
    black = patternPoints(black),
    white = if (white.isEmpty()) emptySet() else patternPoints(white),
)
