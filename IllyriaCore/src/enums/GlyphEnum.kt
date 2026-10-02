package org.xodium.illyriacore.enums

/** Custom font glyphs available via the resource pack. */
internal enum class GlyphEnum(
    val glyph: Char,
) {
    SPRING('\uE901'),
    SUMMER('\uE902'),
    AUTUMN('\uE903'),
    WINTER('\uE904'),
    ;

    override fun toString(): String = glyph.toString()

    companion object {
        /** Returns the glyph for the given [SeasonStateEnum]. */
        fun of(season: SeasonStateEnum): GlyphEnum = entries[season.ordinal]
    }
}

/** Large-scale HUD font glyphs rendered via title packets. */
internal enum class HudGlyphEnum(
    val glyph: Char,
) {
    SPRING('\uE9A0'),
    SUMMER('\uE9A1'),
    AUTUMN('\uE9A2'),
    WINTER('\uE9A3'),
    ;

    override fun toString(): String = glyph.toString()

    companion object {
        /** Returns the HUD glyph for the given [SeasonStateEnum]. */
        fun of(season: SeasonStateEnum): HudGlyphEnum = entries[season.ordinal]
    }
}
