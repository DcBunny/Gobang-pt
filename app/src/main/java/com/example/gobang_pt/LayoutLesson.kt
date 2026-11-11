package com.example.gobang_pt

data class LayoutStep(
    val move: Position,
    val prompt: String,
    val hint: String,
    val explanation: String,
    val whiteResponse: Position? = null,
    val whiteExplanation: String = "",
)

data class LayoutLesson(
    val id: String,
    val title: String,
    val summary: String,
    val target: String,
    val initialPosition: PatternPosition,
    val steps: List<LayoutStep>,
    val winningMoves: Set<Position>,
    val finalExplanation: String,
    val completion: String,
) {
    init {
        require(steps.isNotEmpty()) { "A layout lesson needs guided steps" }
        require(steps.dropLast(1).all { it.whiteResponse != null }) {
            "Every non-final step needs a white response"
        }
        require(steps.last().whiteResponse == null) { "The final step must reach the winning pattern" }
        require(winningMoves.isNotEmpty()) { "A layout lesson needs winning moves" }
    }

    fun positionBeforeStep(stepIndex: Int): PatternPosition {
        require(stepIndex in 0..steps.size) { "Step index must be within the lesson" }
        var position = initialPosition
        steps.take(stepIndex).forEach { step ->
            position = position.withBlack(step.move)
            step.whiteResponse?.let { response -> position = position.withWhite(response) }
        }
        return position
    }

    fun finalPosition(): PatternPosition = positionBeforeStep(steps.size)
}

val layoutLessons = listOf(
    LayoutLesson(
        id = "center_development",
        title = "中心展开：从活二到活四",
        summary = "从中心占点开始，保持横线两端开放，逐步建立直接威胁。",
        target = "活四",
        initialPosition = PatternPosition(black = emptySet()),
        steps = listOf(
            layoutStep(
                move = "h8",
                prompt = "第一步在中心附近落子，为四个方向保留发展空间。",
                hint = "选择棋盘中心的 H8。",
                explanation = "H8 让横、竖和两条斜线都能继续连接，是这个简化布局的起点。",
                whiteResponse = "g7",
                whiteExplanation = "白方在 G7 接近黑棋，但没有影响黑棋向横线两端发展。",
            ),
            layoutStep(
                move = "i8",
                prompt = "沿同一方向再添一子，先形成活二。",
                hint = "在 H8 右侧相邻的 I8 落子。",
                explanation = "H8 与 I8 形成活二，横线两端仍然开放。",
                whiteResponse = "g9",
                whiteExplanation = "白方在 G9 布子，黑棋继续保留横线的连接空间。",
            ),
            layoutStep(
                move = "g8",
                prompt = "向另一端延伸，把活二扩展为活三。",
                hint = "选择 H8 左侧的 G8。",
                explanation = "G8、H8、I8 形成活三。布局不只是堆积棋子，还要确保下一手能形成活四。",
                whiteResponse = "i6",
                whiteExplanation = "白方转向 I6 发展自己的线路，横线的 J8 仍可扩展。",
            ),
            layoutStep(
                move = "j8",
                prompt = "向 J8 延伸，形成两端开放的活四。",
                hint = "在 I8 右侧的 J8 落子。",
                explanation = "G8 至 J8 形成活四，F8 与 K8 都是立即成五点。",
            ),
        ),
        winningMoves = patternPoints("f8 k8"),
        finalExplanation = "白方只能封住 F8 或 K8 中的一端，黑棋下一手在另一端即可获胜。",
        completion = "你已依次完成中心占位、活二、活三和活四，并亲手连成五子。",
    ),
    LayoutLesson(
        id = "connected_diagonals",
        title = "双线连接：双三转为活四",
        summary = "利用两条斜线的交叉点形成双三，再把未被封锁的一路扩展为活四。",
        target = "双三 → 活四",
        initialPosition = patternPosition(black = "f6 g7 f10 g9"),
        steps = listOf(
            layoutStep(
                move = "h8",
                prompt = "找到两条斜线共同缺少的中心点，一手形成双三。",
                hint = "F6、G7 与 F10、G9 都围绕 H8 展开。",
                explanation = "H8 同时补齐两条斜线，形成两个活三。双三只是进攻起点，还要根据白方防守继续发展。",
                whiteResponse = "i9",
                whiteExplanation = "白方在 I9 封住其中一条斜线，另一条从 F10 到 H8 的活三仍可发展。",
            ),
            layoutStep(
                move = "i7",
                prompt = "转向未被封锁的活三，向 I7 扩展成活四。",
                hint = "从 H8 沿右上方向连接到 I7。",
                explanation = "F10、G9、H8、I7 形成活四，E11 和 J6 都是立即成五点。",
            ),
        ),
        winningMoves = patternPoints("e11 j6"),
        finalExplanation = "白方封住一条斜线后，另一条已经成长为活四，E11 与 J6 无法同时防守。",
        completion = "你已学会在交叉点组织两条线路，并根据对方防守切换到仍有开放空间的一路。",
    ),
    LayoutLesson(
        id = "force_then_switch",
        title = "冲四逼防：转移主线形成活四",
        summary = "先用冲四迫使白方防守，再利用预先准备的斜线形成活四。",
        target = "冲四 → 活四",
        initialPosition = patternPosition(black = "f6 g7 g8 h8 j8", white = "f8"),
        steps = listOf(
            layoutStep(
                move = "i8",
                prompt = "先在横线补成冲四，迫使白方立即回应。",
                hint = "横线 G8、H8、J8 之间缺少 I8。",
                explanation = "I8 让 G8 至 J8 形成冲四，白方必须挡住唯一的 K8 成五点。",
                whiteResponse = "k8",
                whiteExplanation = "白方在 K8 防守冲四。黑棋获得先手，可以转向预先布置的斜线。",
            ),
            layoutStep(
                move = "i9",
                prompt = "利用斜线上已有的棋子，转向 I9 形成活四。",
                hint = "F6、G7、H8 的斜线下一颗在 I9。",
                explanation = "F6、G7、H8、I9 形成活四，E5 与 J10 都能立即成五。",
            ),
        ),
        winningMoves = patternPoints("e5 j10"),
        finalExplanation = "冲四迫使白方在 K8 防守后，黑棋已在另一方向形成活四，两个成五点无法同时封住。",
        completion = "你已用冲四争取到先手，再把攻势转移到开放线路，完成活四与五连。",
    ),
    LayoutLesson(
        id = "double_four",
        title = "一子双四：组合两条攻势",
        summary = "提前围绕交叉点布子，一手同时形成两个冲四，让白方无法兼顾。",
        target = "双四",
        initialPosition = patternPosition(black = "f8 g8 i8 f6 g7", white = "e8 e5"),
        steps = listOf(
            layoutStep(
                move = "i9",
                prompt = "先在斜线另一端准备棋子，为交叉攻击保留 H8。",
                hint = "选择 I9，与 F6、G7 共同围绕 H8 布局。",
                explanation = "I9 没有立即连成三子，却让 H8 一旦落子就能连接 F6、G7、I9。布局需要提前准备交叉点。",
                whiteResponse = "h7",
                whiteExplanation = "白方在 H7 接近中心，但没有占据双四的关键交叉点 H8。",
            ),
            layoutStep(
                move = "h8",
                prompt = "在 H8 落子，同时完成横线与斜线的两个冲四。",
                hint = "连接 F8、G8、I8，同时连接 F6、G7、I9。",
                explanation = "H8 同时形成两个冲四，J8 和 J10 都是立即成五点。",
            ),
        ),
        winningMoves = patternPoints("j8 j10"),
        finalExplanation = "白方占据 J8 或 J10 只能防住一条线，黑棋可从另一条线立即成五。",
        completion = "你已提前准备两条线路，并用一个交叉落点形成双四，完成无法同时防守的攻击。",
    ),
    LayoutLesson(
        id = "turtle_formation",
        title = "乌龟阵：稳守反击",
        summary = "先化解白方的直接成五威胁，再用同一个落点建立自己的活三。",
        target = "防守 → 活三 → 活四",
        initialPosition = patternPosition(black = "c7 f5 g6", white = "d7 e7 f7 g7"),
        steps = listOf(
            layoutStep(
                move = "h7",
                prompt = "白方下一手能在 H7 成五。找到既能防守、又能连接黑棋的落点。",
                hint = "H7 不仅封住白方横线，还能与 F5、G6 连成斜线。",
                explanation = "H7 化解了白方的直接威胁，同时让 F5、G6、H7 形成活三。防守落点也可以发展自己的棋形。",
                whiteResponse = "h9",
                whiteExplanation = "白方转向 H9 布子，黑方斜线的 I8 仍然开放。",
            ),
            layoutStep(
                move = "i8",
                prompt = "沿未被限制的斜线继续扩展，把活三发展成活四。",
                hint = "在 H7 右下方的 I8 落子。",
                explanation = "F5、G6、H7、I8 形成活四，E4 与 J9 都是立即成五点。",
            ),
        ),
        winningMoves = patternPoints("e4 j9"),
        finalExplanation = "黑方先在 H7 完成防守与连接，再形成活四；白方只能封住 E4 或 J9 中的一端。",
        completion = "你已学会先检查直接危险，并寻找兼顾防守与进攻的一子双用落点。",
    ),
    LayoutLesson(
        id = "lightning_formation",
        title = "闪电阵：连续冲四",
        summary = "连续两次冲四迫使白方跟随防守，最后转向竖线形成活四。",
        target = "连续冲四 → 活四",
        initialPosition = patternPosition(
            black = "f8 g8 i8 f6 g7 h6 h7",
            white = "e8 e5",
        ),
        steps = listOf(
            layoutStep(
                move = "h8",
                prompt = "先补齐横线，制造白方必须立即回应的冲四。",
                hint = "H8 可以连接 F8、G8 与 I8。",
                explanation = "F8 至 I8 形成冲四。E8 已被白方封住，白方只能占据 J8 阻止黑棋成五。",
                whiteResponse = "j8",
                whiteExplanation = "白方在 J8 封住横线唯一的成五点，黑方继续保持先手。",
            ),
            layoutStep(
                move = "i9",
                prompt = "不要停在横线，转向预先准备的斜线再次冲四。",
                hint = "I9 能连接 F6、G7 与 H8。",
                explanation = "F6、G7、H8、I9 形成第二个冲四。E5 已被封住，白方又必须防守 J10。",
                whiteResponse = "j10",
                whiteExplanation = "白方在 J10 防守斜线。黑方两次逼迫回应后，竖线已经准备完成。",
            ),
            layoutStep(
                move = "h9",
                prompt = "利用连续进攻争取的时间，在竖线形成活四。",
                hint = "连接 H6、H7、H8，在 H9 落子。",
                explanation = "H6 至 H9 形成活四，H5 与 H10 都是立即成五点。",
            ),
        ),
        winningMoves = patternPoints("h5 h10"),
        finalExplanation = "连续冲四让白方只能在 J8、J10 跟随防守，最终竖线活四留下 H5 与 H10 两个胜点。",
        completion = "你已演示连续冲四保持先手；实战计算时，要预先看清每次最强防守和下一手进攻。",
    ),
    LayoutLesson(
        id = "lobster_formation",
        title = "龙虾阵：双钳交叉",
        summary = "两条斜线像双钳一样指向 J10，一手形成双三，再发展未被封住的一路。",
        target = "双三 → 活四",
        initialPosition = patternPosition(black = "h8 i9 h12 i11"),
        steps = listOf(
            layoutStep(
                move = "j10",
                prompt = "找到两条斜线共同缺少的交叉点，一手形成两个活三。",
                hint = "H8、I9 与 H12、I11 都围绕 J10 展开。",
                explanation = "J10 同时补齐两条斜线，形成双三。两条线路都需要白方处理，但白方一手只能封住其中一路。",
                whiteResponse = "k9",
                whiteExplanation = "白方在 K9 封住 H12、I11、J10 一路，另一路仍能向 K11 发展。",
            ),
            layoutStep(
                move = "k11",
                prompt = "转向仍然开放的活三，把它扩展为活四。",
                hint = "沿 H8、I9、J10 的方向，在 K11 落子。",
                explanation = "H8、I9、J10、K11 形成活四，G7 与 L12 都是立即成五点。",
            ),
        ),
        winningMoves = patternPoints("g7 l12"),
        finalExplanation = "白方封住双三的一路后，另一路已成长为活四，G7 与 L12 无法同时防守。",
        completion = "你已学会围绕交叉点组织两条攻势，并根据白方防守切换到仍有空间的一路。",
    ),
    LayoutLesson(
        id = "moon_formation",
        title = "月亮阵：跳三成月",
        summary = "利用带空隙的跳活三配合斜线冲四，形成四三后继续进攻。",
        target = "四三 → 活四",
        initialPosition = patternPosition(black = "f10 g9 i7 h11 h9", white = "e11"),
        steps = listOf(
            layoutStep(
                move = "h8",
                prompt = "寻找同时补齐斜线四子和竖线跳活三的落点。",
                hint = "H8 能连接斜线 F10、G9、I7，也能与 H9、H11 呼应。",
                explanation = "H8 让斜线形成冲四，同时与 H9、H11 形成带空隙的跳活三。活三不要求三个棋子全部相邻。",
                whiteResponse = "j6",
                whiteExplanation = "白方必须在 J6 防守斜线的成五点，竖线的跳活三仍然存在。",
            ),
            layoutStep(
                move = "h10",
                prompt = "补上跳活三中间的空隙，把竖线发展成活四。",
                hint = "H10 位于 H9 与 H11 之间。",
                explanation = "H8 至 H11 形成连续活四，H7 与 H12 都是立即成五点。",
            ),
        ),
        winningMoves = patternPoints("h7 h12"),
        finalExplanation = "四三迫使白方先处理斜线冲四，随后黑方补全跳活三，留下 H7 与 H12 两个胜点。",
        completion = "你已学会识别带空隙的跳活三，并把冲四的直接威胁与活三的后续攻势组合起来。",
    ),
    LayoutLesson(
        id = "sword_formation",
        title = "宝剑阵：直刺转折",
        summary = "沿竖线冲四逼防，再沿准备好的反斜线转移攻势，形成活四。",
        target = "冲四 → 转移 → 活四",
        initialPosition = patternPosition(black = "h5 h6 h8 f9 g8", white = "h9"),
        steps = listOf(
            layoutStep(
                move = "h7",
                prompt = "先沿竖线直刺，补成白方必须防守的冲四。",
                hint = "H7 能连接 H5、H6 与 H8。",
                explanation = "H5 至 H8 形成冲四。H9 已被白方封住，白方只能占据 H4 阻止黑棋成五。",
                whiteResponse = "h4",
                whiteExplanation = "白方在 H4 封住竖线，黑方获得转向另一线路的先手。",
            ),
            layoutStep(
                move = "i6",
                prompt = "转向预先准备的反斜线，把攻势发展成活四。",
                hint = "I6 能连接 F9、G8 与 H7。",
                explanation = "F9、G8、H7、I6 形成活四，E10 与 J5 都是立即成五点。",
            ),
        ),
        winningMoves = patternPoints("e10 j5"),
        finalExplanation = "竖线冲四把白方引到 H4 后，黑方在另一方向形成活四，E10 与 J5 只能被封住一个。",
        completion = "你已学会用冲四争取先手，再把主线转移到开放线路，而不是在受阻方向继续堆子。",
    ),
    LayoutLesson(
        id = "sickle_formation",
        title = "镰刀阵：双线收杀",
        summary = "提前准备竖线与反斜线，最后一子同时形成两个不同方向的冲四。",
        target = "双四",
        initialPosition = patternPosition(
            black = "h5 h6 h7 f10 g9 i7",
            white = "h4 e11",
        ),
        steps = listOf(
            layoutStep(
                move = "h8",
                prompt = "找到两条受限线路共同的交叉点，一手完成双四。",
                hint = "H8 既能连接 H5、H6、H7，也能连接 F10、G9、I7。",
                explanation = "H8 同时形成竖线与反斜线的两个冲四，H9 和 J6 是不同方向的两个立即成五点。",
            ),
        ),
        winningMoves = patternPoints("h9 j6"),
        finalExplanation = "白方占据 H9 或 J6 只能防守一条线路，黑方下一手在另一个落点即可成五。",
        completion = "你已学会提前准备两条线路，并用一个交叉落点形成白方无法同时化解的双四。",
    ),
)

private fun layoutStep(
    move: String,
    prompt: String,
    hint: String,
    explanation: String,
    whiteResponse: String? = null,
    whiteExplanation: String = "",
) = LayoutStep(
    move = patternPoint(move),
    prompt = prompt,
    hint = hint,
    explanation = explanation,
    whiteResponse = whiteResponse?.let(::patternPoint),
    whiteExplanation = whiteExplanation,
)
