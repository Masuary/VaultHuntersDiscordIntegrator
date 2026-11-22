//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


/**
 * A row of hearts as Vault Hunters' {@code TooltipHearts} draws it (a potion's Heals line): one heart per two hit points,
 * a half heart for an odd one, at most ten hearts and a "+" after them for more. The picture writes a full heart as U+2665
 * and a half heart as U+2661; the site draws a run made only of those as the game's heart sprites.
 */
final class HeartRow
{
    private static final String FULL_HEART = "♥";

    private static final String HALF_HEART = "♡";

    private static final int MAXIMUM_HEARTS_SHOWN = 10;

    private static final int HIT_POINTS_PER_HEART = 2;


    private HeartRow()
    {
    }


    /**
     * The hearts shown for an amount of hit points.
     *
     * @param hitPoints the hit points.
     * @return the hearts, empty for none.
     */
    static String hearts(float hitPoints)
    {
        int halfHearts = HeartRow.halfHearts(hitPoints);
        int shown = (int) Math.min(MAXIMUM_HEARTS_SHOWN, (halfHearts + 1L) / HIT_POINTS_PER_HEART);
        StringBuilder hearts = new StringBuilder();

        for (int index = 0; index < shown; index++)
        {
            hearts.append(index * HIT_POINTS_PER_HEART + 1 == halfHearts ? HALF_HEART : FULL_HEART);
        }

        return hearts.toString();
    }


    /**
     * Whether the hit points are more than the hearts shown.
     *
     * @param hitPoints the hit points.
     * @return true when the game adds a "+".
     */
    static boolean hasMoreThanShown(float hitPoints)
    {
        return HeartRow.halfHearts(hitPoints) > MAXIMUM_HEARTS_SHOWN * HIT_POINTS_PER_HEART;
    }


    private static int halfHearts(float hitPoints)
    {
        return Math.max(0, Math.round(hitPoints));
    }
}
