package com.example.gobang_pt

import androidx.annotation.StringRes

enum class KidsStage(@param:StringRes val titleRes: Int) {
    CONNECT(R.string.kids_stage_connect),
    GROW(R.string.kids_stage_grow),
    COMBINE(R.string.kids_stage_combine),
    OPENING(R.string.kids_stage_opening),
    ADVANCED_PATTERNS(R.string.kids_stage_advanced_patterns),
    ADVANCED_TACTICS(R.string.kids_stage_advanced_tactics),
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
    CLOSED_FOUR(R.string.kids_target_closed_four),
    DOUBLE_FOUR(R.string.kids_target_double_four),
    BLOCK_AND_ATTACK(R.string.kids_target_block_and_attack),
    BLOCK_DOUBLE_THREE(R.string.kids_target_block_double_three),
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
    KidsLesson(
        id = "jump_three",
        order = 9,
        stage = KidsStage.ADVANCED_PATTERNS,
        title = "跳三补中间",
        patternName = "跳三",
        intro = "三个黑子中间隔着一个空。把空填上，也能变成活四！",
        rule = "跳三：三个棋子中间有空隙，补上空隙就能变成活四。",
        thinkingTip = "先看中间：棋型有缺口时，胜点可能藏在里面。",
        initial = lessonPosition(black = "f8 g8 i8", white = "d4 e5"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.OPEN_FOUR,
                prompt = "三个黑子中间隔着一个空。点一下补上空隙，做出活四。",
                hint = "F8、G8、I8 中间缺的是 H8。",
                answers = "h8",
                explanation = "H8 把三个黑子连成 F8 到 I8 的活四，两头 E8 和 J8 都能成五。",
                wrongFeedback = "别只看两头，三个黑子中间的空隙才是关键。",
                whiteReply = "e8",
                whiteReplyText = "白棋在 E8 挡住了活四的一头。",
            ),
            kidsStep(
                target = KidsTarget.FIVE,
                prompt = "白棋挡住了左边。在右边落子，连成五子。",
                hint = "F8 到 I8 的活四，另一头是 J8。",
                answers = "j8",
                explanation = "J8 让 F8 到 J8 五个黑子连成一条线，黑棋获胜！",
                wrongFeedback = "活四有两头，白棋挡住一边，就从另一边赢。",
            ),
        ),
        completion = "你学会了跳三：中间有空隙的棋型，补上缺口就能变成活四。",
    ),
    KidsLesson(
        id = "jump_four",
        order = 10,
        stage = KidsStage.ADVANCED_PATTERNS,
        title = "跳四找暗点",
        patternName = "跳四",
        intro = "四个黑子中间隔着一个空，胜点藏在中间。",
        rule = "跳四：四个棋子中间有空隙，补上空隙就能连成五子。",
        thinkingTip = "先看中间：不连续的四个棋子，胜点往往在缺口。",
        initial = lessonPosition(black = "f8 g8 h8 j8", white = "e8"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.FIVE,
                prompt = "黑棋的四个棋子中间有一个空。点一下补上它，连成五子。",
                hint = "F8、G8、H8、J8 中间缺的是 I8。",
                answers = "i8",
                explanation = "I8 补上后，F8 到 J8 五个黑子连成一条线，黑棋获胜！",
                wrongFeedback = "别只看两头，四个黑子中间的空隙补上就能赢。",
            ),
        ),
        completion = "你学会了跳四：不连续的四个棋子，缺口就是胜点。",
    ),
    KidsLesson(
        id = "double_four",
        order = 11,
        stage = KidsStage.ADVANCED_PATTERNS,
        title = "双四收杀",
        patternName = "双四",
        intro = "竖线和斜线都只差一个点。一手同时做出两个冲四，会怎样？",
        rule = "双四：一手棋同时做出两个冲四，白棋只能挡住一边。",
        thinkingTip = "先数胜点：两个冲四，就有两个不同的胜点。",
        initial = lessonPosition(black = "h5 h6 h7 f10 g9 i7", white = "h4 e11"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.DOUBLE_FOUR,
                prompt = "找到一个点，让竖线和斜线同时变成冲四。",
                hint = "竖线 H5、H6、H7 缺 H8；斜线 F10、G9、I7 也缺 H8。",
                answers = "h8",
                explanation = "H8 同时做出竖线和斜线的两个冲四。白棋只能挡住其中一个胜点。",
                wrongFeedback = "找两条线共同缺少的那个点。竖线缺 H8，斜线也缺 H8。",
                whiteReply = "h9",
                whiteReplyText = "白棋在 H9 挡住了竖线的成五点。",
            ),
            kidsStep(
                target = KidsTarget.FIVE,
                prompt = "白棋去挡竖线了。斜线的成五点还空着，点它连成五子。",
                hint = "F10、G9、H8、I7 这条斜线，下一颗在 J6。",
                answers = "j6",
                explanation = "J6 让 F10 到 J6 五个黑子连成一条线，黑棋获胜！",
                wrongFeedback = "竖线被挡住了，看看斜线的成五点在哪里。",
            ),
        ),
        completion = "你学会了双四：一手两个冲四，对手只能挡住一边。",
    ),
    KidsLesson(
        id = "vcf_continuous_four",
        order = 12,
        stage = KidsStage.ADVANCED_TACTICS,
        title = "连续冲四",
        patternName = "连续冲四",
        intro = "冲四不只是威胁，还能逼对手跟着你走。连续两次冲四，会得到什么？",
        rule = "连续冲四：每一步都做冲四，白棋必须挡，你就能一直保持先手。",
        thinkingTip = "先手意识：对手必须应一手，你就能安排下一步。",
        initial = lessonPosition(black = "f8 g8 i8 f6 g7 h6 h7", white = "e8 e5"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.CLOSED_FOUR,
                prompt = "先看横线：F8、G8、I8 缺一个点。补上它，做出冲四。",
                hint = "横线 F8 到 I8 缺 H8；E8 被白棋挡住，只剩 J8 能成五。",
                answers = "h8",
                explanation = "H8 让横线形成冲四，白棋必须去挡唯一的成五点 J8。",
                wrongFeedback = "横线缺的是 H8。做出冲四，逼白棋回应。",
                whiteReply = "j8",
                whiteReplyText = "白棋只能去挡 J8。",
            ),
            kidsStep(
                target = KidsTarget.CLOSED_FOUR,
                prompt = "不要停，转向斜线：F6、G7、H8 还缺一个点，补成冲四。",
                hint = "斜线 F6、G7、H8 缺 I9；E5 被白棋挡住，只剩 J10。",
                answers = "i9",
                explanation = "I9 让斜线形成第二个冲四，白棋又必须去挡 J10。",
                wrongFeedback = "斜线 F6、G7、H8 缺 I9，补上它继续逼白棋。",
                whiteReply = "j10",
                whiteReplyText = "白棋只能去挡 J10。",
            ),
            kidsStep(
                target = KidsTarget.OPEN_FOUR,
                prompt = "白棋被连续逼了两手。现在看竖线 H6、H7、H8，把它变成活四。",
                hint = "竖线 H6、H7、H8 两头是 H5 和 H9，都可以落子。",
                answers = "h5 h9",
                explanation = "H5 或 H9 都能做出活四。连续冲四让你一直先手，最后把竖线变成活四。",
                wrongFeedback = "竖线 H6、H7、H8 两头都空着，在 H5 或 H9 落子。",
            ),
        ),
        completion = "你学会了连续冲四：每一次都逼白棋防守，最后把另一条线变成活四。",
    ),
    KidsLesson(
        id = "defend_and_attack",
        order = 13,
        stage = KidsStage.ADVANCED_TACTICS,
        title = "防中带攻",
        patternName = "防中带攻",
        intro = "白棋下一步就能连成五子。有没有一步棋，既挡住白棋，又能给自己做出威胁？",
        rule = "防中带攻：最好的防守落点，也能同时连接自己的棋子。",
        thinkingTip = "先看对手的成五点，再看自己能不能一子两用。",
        initial = lessonPosition(black = "c7 f5 g6", white = "d7 e7 f7 g7"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.BLOCK_AND_ATTACK,
                prompt = "白棋的四个棋子在第七行，下一步就能成五。找到既能挡住、又能连接黑棋的点。",
                hint = "白棋的成五点是 H7；H7 还能和 F5、G6 连成斜线。",
                answers = "h7",
                explanation = "H7 挡住白棋的五连，同时让 F5、G6、H7 形成活三。防守也能变成进攻！",
                wrongFeedback = "先找白棋的成五点，再看这个点能不能连接自己的棋子。",
                whiteReply = "h9",
                whiteReplyText = "白棋转向 H9 落子。",
            ),
            kidsStep(
                target = KidsTarget.OPEN_FOUR,
                prompt = "白棋去 H9 了。黑棋的斜线 F5、G6、H7 还差一个点，把它变成活四。",
                hint = "F5、G6、H7 的两头是 E4 和 I8，都可以落子。",
                answers = "e4 i8",
                explanation = "E4 或 I8 都能做出活四。白棋挡住一个，黑棋就从另一个赢。",
                wrongFeedback = "把 F5、G6、H7 往两头延长，做出活四。",
            ),
        ),
        completion = "你学会了防中带攻：先挡住对手的成五点，再找能同时连接自己棋子的落点。",
    ),
    KidsLesson(
        id = "block_double_three",
        order = 14,
        stage = KidsStage.ADVANCED_TACTICS,
        title = "拆掉双三",
        patternName = "拆双三",
        intro = "白棋四个子围住一个点，下一步会同时做出两个活三。你要先占住哪里？",
        rule = "拆双三：两条活三的交叉点，就是必须马上占住的点。",
        thinkingTip = "看到对手两条线都缺同一个点，先占住它。",
        initial = lessonPosition(black = "d4 e5", white = "g8 i8 h7 h9"),
        steps = listOf(
            kidsStep(
                target = KidsTarget.BLOCK_DOUBLE_THREE,
                prompt = "白棋在 H8 落子会同时做出两个活三。先占住 H8，拆掉这个双三。",
                hint = "横线 G8、I8 和竖线 H7、H9 都缺 H8。",
                answers = "h8",
                explanation = "H8 被黑棋占住，白棋就不能在 H8 同时做出两个活三。看到交叉点，要提前防守。",
                wrongFeedback = "找白棋两条线共同缺少的那个点。横线缺 H8，竖线也缺 H8。",
            ),
        ),
        completion = "你学会了拆双三：对手两条线都缺的交叉点，必须先占住。",
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
