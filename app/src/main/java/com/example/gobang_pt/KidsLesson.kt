package com.example.gobang_pt

import androidx.annotation.StringRes

enum class KidsStage(@param:StringRes val titleRes: Int) {
    CONNECT(R.string.kids_stage_connect),
    GROW(R.string.kids_stage_grow),
    COMBINE(R.string.kids_stage_combine),
    OPENING(R.string.kids_stage_opening),
}

enum class KidsTarget(@param:StringRes val labelRes: Int) {
    FIVE(R.string.kids_target_five),
    BLOCK(R.string.kids_target_block),
    OPEN_TWO(R.string.kids_target_open_two),
    OPEN_THREE(R.string.kids_target_open_three),
    OPEN_FOUR(R.string.kids_target_open_four),
    DOUBLE_THREE(R.string.kids_target_double_three),
    FOUR_THREE(R.string.kids_target_four_three),
    CENTER(R.string.kids_target_center),
}

data class LessonPosition(
    val black: Set<Position> = emptySet(),
    val white: Set<Position> = emptySet(),
) {
    init {
        require(black.all(Position::isValid) && white.all(Position::isValid)) {
            "Lesson stones must be on the board"
        }
        require((black intersect white).isEmpty()) { "Lesson stones cannot overlap" }
    }

    fun stoneAt(position: Position): Stone? = when (position) {
        in black -> Stone.BLACK
        in white -> Stone.WHITE
        else -> null
    }

    fun withBlack(position: Position): LessonPosition {
        require(position.isValid && stoneAt(position) == null) { "Answer must be an empty board point" }
        return copy(black = black + position)
    }

    fun withWhite(position: Position): LessonPosition {
        require(position.isValid && stoneAt(position) == null) { "Response must be an empty board point" }
        return copy(white = white + position)
    }

    fun toGobangGame(extraMoves: List<Move> = emptyList()): GobangGame {
        val moves = buildList {
            black.forEach { add(Move(it, Stone.BLACK)) }
            white.forEach { add(Move(it, Stone.WHITE)) }
            addAll(extraMoves)
        }
        val board = List<Stone?>(BOARD_SIZE * BOARD_SIZE) { null }.toMutableList()
        moves.forEach { move ->
            board[move.position.row * BOARD_SIZE + move.position.column] = move.stone
        }
        return GobangGame(board = board, moves = moves)
    }
}

data class KidsStep(
    val target: KidsTarget,
    val prompt: String,
    val hint: String,
    val answers: Set<Position>,
    val explanation: String,
    val wrongFeedback: String,
    val whiteReply: Position? = null,
    val whiteReplyText: String = "",
) {
    init {
        require(prompt.isNotBlank() && hint.isNotBlank()) { "A lesson step needs a prompt and a hint" }
        require(explanation.isNotBlank() && wrongFeedback.isNotBlank()) {
            "A lesson step needs explanations"
        }
        require(answers.isNotEmpty()) { "A lesson step needs at least one answer" }
    }
}

data class KidsLesson(
    val id: String,
    val order: Int,
    val stage: KidsStage,
    val title: String,
    val patternName: String,
    val intro: String,
    val rule: String,
    val thinkingTip: String,
    val initial: LessonPosition,
    val steps: List<KidsStep>,
    val completion: String,
) {
    init {
        require(id.isNotBlank()) { "A lesson needs an id" }
        require(order > 0) { "A lesson needs a positive order" }
        require(steps.isNotEmpty()) { "A lesson needs at least one step" }
        require(steps.dropLast(1).all { it.whiteReply != null }) {
            "Every non-final step needs a white response"
        }
        require(steps.last().whiteReply == null) { "The final step must not have a white response" }
        require(steps.dropLast(1).all { it.answers.size == 1 }) {
            "Guided steps must have exactly one canonical move"
        }
    }
}

private fun kidsStep(
    target: KidsTarget,
    prompt: String,
    hint: String,
    answers: String,
    explanation: String,
    wrongFeedback: String,
    whiteReply: String? = null,
    whiteReplyText: String = "",
): KidsStep = KidsStep(
    target = target,
    prompt = prompt,
    hint = hint,
    answers = lessonPoints(answers),
    explanation = explanation,
    wrongFeedback = wrongFeedback,
    whiteReply = whiteReply?.let(::lessonPoint),
    whiteReplyText = whiteReplyText,
)

val kidsLessons: List<KidsLesson> = listOf(
    KidsLesson(
        id = "five_in_a_row",
        order = 1,
        stage = KidsStage.CONNECT,
        title = "五连在哪里",
        patternName = "成五",
        intro = "黑棋已经有四个棋子连在一起，只差最后一手。",
        rule = "五连：五个同色棋子连成一条直线，就赢了。",
        thinkingTip = "先看自己：有没有四个连在一起？能不能一手成五？",
        initial = lessonPosition(black = "f8 g8 h8 i8", white = "e8"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.FIVE,
                prompt = "点一下能让黑棋连成五子的交叉点。",
                hint = "黑棋的四个棋子在第八行，从 F8 一直排到 I8，右边还空着。",
                answers = "j8",
                explanation = "J8 让 F8 到 J8 五个黑子连成一条线，黑棋获胜！",
                wrongFeedback = "再看看四个黑子的两头，哪一头还空着？",
            ),
        ),
        completion = "你会找成五点了。记住：先看自己有没有四，能不能一手成五。",
    ),
    KidsLesson(
        id = "block_five",
        order = 2,
        stage = KidsStage.CONNECT,
        title = "挡住对手",
        patternName = "挡五",
        intro = "白棋已经有四个棋子连在一起，下一步就要赢了！",
        rule = "挡五：对手有四个棋子时，先占住它能连成五子的点。",
        thinkingTip = "先看对手：对手有没有四个？有就马上挡住。",
        initial = lessonPosition(black = "c8", white = "d8 e8 f8 g8"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.BLOCK,
                prompt = "白棋下一步就能连成五子，快点住那个点。",
                hint = "白棋的四个棋子在第八行，从 D8 排到 G8；左边被黑棋 C8 挡住了，只剩右边。",
                answers = "h8",
                explanation = "H8 把白棋的四子两头都堵住了，白棋暂时不能连成五子。防守和进攻一样重要！",
                wrongFeedback = "再找找白棋四个棋子的开口在哪里，别让它连成五个。",
            ),
        ),
        completion = "你学会了先看对手的威胁。下棋前先问：对手有没有四？",
    ),
    KidsLesson(
        id = "make_open_three",
        order = 3,
        stage = KidsStage.GROW,
        title = "两个变三个",
        patternName = "活三",
        intro = "两个黑子手拉手，再添一个，就能变成三个。",
        rule = "活三：三个同色棋子连在一起，两端都空着。",
        thinkingTip = "活三的秘密：三个连在一起，两头都要空着。",
        initial = lessonPosition(black = "h8 i8", white = "d4 e5"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.OPEN_THREE,
                prompt = "在 H8 或 I8 旁边落子，让三个黑子连成一条线，并且两端都空着。",
                hint = "两个黑子是 H8 和 I8，可以往左接 G8，也可以往右接 J8。",
                answers = "g8 j8",
                explanation = "G8 或 J8 都能做出活三。三个黑子两端都空着，白棋只能挡住一头。",
                wrongFeedback = "要连成三个，而且要两头都空着。试试 H8 的左边或右边。",
            ),
        ),
        completion = "你学会了做活三。活三就像小树苗，两头都有地方长大。",
    ),
    KidsLesson(
        id = "make_open_four",
        order = 4,
        stage = KidsStage.GROW,
        title = "三个变四个",
        patternName = "活四",
        intro = "三个黑子已经连在一起，两端都空着。再长一个，就变成活四！",
        rule = "活四：四个同色棋子连在一起，两端都空着，有两个成五点。",
        thinkingTip = "活四有两个胜点，白棋只能挡住一个。",
        initial = lessonPosition(black = "g8 h8 i8", white = "d4 e5"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.OPEN_FOUR,
                prompt = "在三个黑子的任意一头落子，做出活四。",
                hint = "G8、H8、I8 的两头是 F8 和 J8。",
                answers = "f8 j8",
                explanation = "F8 或 J8 都能做出活四。活四有两个成五点，白棋挡一个，黑棋就从另一个赢。",
                wrongFeedback = "把三个黑子往两头延长，做出四个，并让两头都空着。",
            ),
        ),
        completion = "你学会了活四。活四有两个胜点，白棋只能挡住一个。",
    ),
    KidsLesson(
        id = "choose_open_four",
        order = 5,
        stage = KidsStage.COMBINE,
        title = "冲四还是活四",
        patternName = "活四",
        intro = "黑棋有两个进攻方向：一边能做出活四，一边只能做出冲四。",
        rule = "活四有两个成五点；冲四只有一个，白棋挡住就安全了。",
        thinkingTip = "先数胜点：两个胜点是活四，一个胜点是冲四。",
        initial = lessonPosition(black = "g8 h8 i8 f3 g4 h5", white = "e2"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.OPEN_FOUR,
                prompt = "找一找：哪一步能做出活四，让白棋挡不住？",
                hint = "先数一数胜点：活四有两个，冲四只有一个。看看三个连着的 G8、H8、I8。",
                answers = "f8 j8",
                explanation = "F8 或 J8 把 G8、H8、I8 变成活四，白棋只能挡一头。I6 只能做出冲四，白棋在 J7 一挡就没事了。",
                wrongFeedback = "冲四只有一个胜点，白棋挡住就安全了。找找能做出两个胜点的活四。",
            ),
        ),
        completion = "你学会了比较威胁：能做出活四，就不要只做冲四。",
    ),
    KidsLesson(
        id = "double_three",
        order = 6,
        stage = KidsStage.COMBINE,
        title = "一个点两条线",
        patternName = "双三",
        intro = "一个落点，能不能同时做出两个活三？",
        rule = "双三：一手棋同时做出两个活三，白棋挡不住两条线。",
        thinkingTip = "找交叉点：两条线都缺的那个点，就是双三。",
        initial = lessonPosition(black = "f6 g7 f10 g9", white = "c14 d13"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.DOUBLE_THREE,
                prompt = "找到两条斜线共同缺少的那个点，一手做出两个活三。",
                hint = "两条斜线分别经过 F6、G7 和 F10、G9，它们都想在 H8 会合。",
                answers = "h8",
                explanation = "H8 同时补齐两条斜线，做出双三！白棋只能挡住其中一条。",
                wrongFeedback = "找两条斜线都缺的那个点。看看 F6、G7 和 F10、G9。",
                whiteReply = "i9",
                whiteReplyText = "白棋在 I9 挡住了其中一条斜线。",
            ),
            kidsStep(
                target = KidsTarget.OPEN_FOUR,
                prompt = "另一条斜线还没被挡住。在 E11 或 I7 落子，把它变成活四。",
                hint = "F10、G9、H8 这条斜线，两头是 E11 和 I7。",
                answers = "i7 e11",
                explanation = "E11 或 I7 都能把另一条斜线变成活四。双三的厉害之处，就是让对手顾此失彼。",
                wrongFeedback = "这条斜线已经被白棋挡住一头了，换另一条斜线看看。",
            ),
        ),
        completion = "你学会了双三：一手棋，两个威胁，让对手顾此失彼。",
    ),
    KidsLesson(
        id = "four_three",
        order = 7,
        stage = KidsStage.COMBINE,
        title = "先冲四再活三",
        patternName = "四三",
        intro = "先做一个冲四，逼白棋去挡；再把自己另一条线变成活四。",
        rule = "四三：一手棋同时做出冲四和活三，白棋必须先挡冲四。",
        thinkingTip = "先做冲四逼对手，再把另一条线变成活四。",
        initial = lessonPosition(black = "f10 g9 i7 h10 h9", white = "e11"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.FOUR_THREE,
                prompt = "找到一个点，让斜线变成冲四，同时让竖线变成活三。",
                hint = "斜线经过 F10、G9、I7；竖线经过 H10、H9。它们都缺 H8。",
                answers = "h8",
                explanation = "H8 让斜线形成冲四（白棋必须挡 J6），同时让竖线 H10、H9、H8 变成活三。",
                wrongFeedback = "找斜线和竖线都缺的那个点。斜线是 F10、G9、I7，竖线是 H10、H9。",
                whiteReply = "j6",
                whiteReplyText = "白棋只能先去挡斜线的成五点 J6。",
            ),
            kidsStep(
                target = KidsTarget.OPEN_FOUR,
                prompt = "白棋去挡斜线了。在 H7 或 H11 落子，把竖线的三个黑子变成活四。",
                hint = "竖线现在是 H8、H9、H10，两头是 H7 和 H11。",
                answers = "h7 h11",
                explanation = "H7 或 H11 都能做出活四。白棋挡住冲四后，另一条线已经挡不住了。",
                wrongFeedback = "把竖线上的三个黑子往两头延长，做出活四。",
            ),
        ),
        completion = "你学会了四三：先用冲四逼对手，再把活三变成活四。",
    ),
    KidsLesson(
        id = "opening_center",
        order = 8,
        stage = KidsStage.OPENING,
        title = "先占中间",
        patternName = "活二",
        intro = "棋盘空空的，第一颗棋子下在哪里最好？",
        rule = "活二：两个同色棋子连在一起，两端都空着。开局先占中间。",
        thinkingTip = "开局先占中间，再让棋子连成两个、三个。",
        initial = lessonPosition(),
        steps = listOf(
            kidsStep(
                target = KidsTarget.CENTER,
                prompt = "点一下棋盘正中间的点。",
                hint = "棋盘是 15 路，正中间是 H8。",
                answers = "h8",
                explanation = "H8 在棋盘正中间，横、竖、两条斜线都能继续发展。开局先占中间，棋子才容易连成多条线。",
                wrongFeedback = "开局先占中间，四个方向都能发展。试试 H8。",
                whiteReply = "g7",
                whiteReplyText = "白棋在 G7 落子，靠近中心。",
            ),
            kidsStep(
                target = KidsTarget.OPEN_TWO,
                prompt = "在 H8 旁边再下一个黑子，做出一个两头都空着的活二。",
                hint = "可以上下左右连，也可以斜着连；注意别让白棋挡住一头。",
                answers = "g8 i8 h7 h9 g9 i7",
                explanation = "你连成了两个黑子，而且两头都空着，这就是活二。活二可以长成活三、活四，最后连成五子。",
                wrongFeedback = "要和 H8 连成两个，而且两头都空着。换一个方向试试。",
            ),
        ),
        completion = "你学会了开局：先占中间，再让棋子手拉手长大。",
    ),
)

internal fun lessonPoint(coordinate: String): Position {
    require(coordinate.length in 2..3) { "Invalid coordinate: $coordinate" }
    val column = coordinate.first().lowercaseChar() - 'a'
    val row = coordinate.drop(1).toIntOrNull()?.minus(1) ?: -1
    return Position(row, column).also { position ->
        require(position.isValid) { "Invalid coordinate: $coordinate" }
    }
}

internal fun lessonPoints(notation: String): Set<Position> = notation.split(' ')
    .filter { it.isNotBlank() }
    .map(::lessonPoint)
    .toSet()

internal fun lessonPosition(black: String = "", white: String = ""): LessonPosition = LessonPosition(
    black = if (black.isBlank()) emptySet() else lessonPoints(black),
    white = if (white.isBlank()) emptySet() else lessonPoints(white),
)
