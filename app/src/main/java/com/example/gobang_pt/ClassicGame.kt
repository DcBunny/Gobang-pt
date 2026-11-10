package com.example.gobang_pt

enum class ClassicGameResult(val winner: Stone?) {
    BLACK_WIN(Stone.BLACK),
    WHITE_WIN(Stone.WHITE),
    DRAW(null),
}

data class ClassicGame(
    val id: String,
    val title: String,
    val event: String,
    val blackPlayer: String,
    val whitePlayer: String,
    val description: String,
    val result: ClassicGameResult,
    val source: String,
    val moves: List<Position>,
) {
    fun toGobangGame(): GobangGame = moves.fold(GobangGame()) { game, position ->
        game.play(position)
    }
}

val classicGames = listOf(
    ClassicGame(
        id = "meritee_nakamura_1999_1",
        title = "世纪之战 · 第一局",
        event = "1999 年京都 · 中村茂—安度六局对抗赛",
        blackPlayer = "安度·梅里特",
        whitePlayer = "中村茂",
        description = "中村茂执白迎接安度的攻势，以冷静防守和有力反击拿下首局。原谱共 44 手，以黑方认输结束。",
        result = ClassicGameResult.WHITE_WIN,
        source = "RenjuNet · Match Meritee–Nakamura · Game 342",
        moves = classicMoves(
            "h8 h9 j10 i9 i8 g9 j9 j8 f9 k10 h10 k9 j11 l10 m11 k11 " +
                "j12 j13 k8 l7 g10 e8 k12 i10 h6 l6 l12 m12 n10 k13 l8 j6 " +
                "k7 k5 j7 l5 i7 i6 h7 g7 i12 h12 m5 j4",
        ),
    ),
    ClassicGame(
        id = "meritee_nakamura_1999_2",
        title = "世纪之战 · 第二局",
        event = "1999 年京都 · 中村茂—安度六局对抗赛",
        blackPlayer = "安度·梅里特",
        whitePlayer = "中村茂",
        description = "安度执黑持续施加位置压力，抓住白方第 36 手后的机会扳回一局。原谱共 43 手，以白方认输结束。",
        result = ClassicGameResult.BLACK_WIN,
        source = "RenjuNet · Match Meritee–Nakamura · Game 343",
        moves = classicMoves(
            "h8 h9 h7 h6 g6 i8 j7 g7 j5 f8 e9 f7 g8 i6 j6 j4 i5 " +
                "i7 i9 k7 g5 f5 f6 d8 h4 i3 k5 h5 l7 m8 k9 j10 d6 g3 " +
                "e10 e11 l8 l6 f9 d11 d9 c9 h11",
        ),
    ),
    ClassicGame(
        id = "meritee_nakamura_1999_3",
        title = "世纪之战 · 第三局",
        event = "1999 年京都 · 中村茂—安度六局对抗赛",
        blackPlayer = "安度·梅里特",
        whitePlayer = "中村茂",
        description = "安度在中盘逐步连接上下两翼，以连续的位置进攻扩大优势。原谱共 45 手，中村茂在第 45 手后认输。",
        result = ClassicGameResult.BLACK_WIN,
        source = "RenjuNet · Match Meritee–Nakamura · Game 344",
        moves = classicMoves(
            "h8 h9 j10 i9 j9 j8 k7 i10 j11 i7 i8 f7 h7 k10 k9 l8 " +
                "f9 g6 e8 g8 e6 d7 e7 e9 g5 h4 g11 i11 f12 e11 g12 g13 " +
                "d6 e5 c8 f8 c9 f6 c11 c10 j12 j13 h12 i12 i13",
        ),
    ),
)

private fun classicMoves(notation: String): List<Position> = notation.split(' ').map { coordinate ->
    require(coordinate.length in 2..3) { "Invalid coordinate: $coordinate" }
    val column = coordinate.first().lowercaseChar() - 'a'
    val row = coordinate.drop(1).toIntOrNull()?.minus(1) ?: -1
    return@map Position(row, column).also { position ->
        require(position.isValid) { "Invalid coordinate: $coordinate" }
    }
}
