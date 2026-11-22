//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.vaulthunters;


/**
 * The greed tier the deck text being built on this thread is worked out for; the game reads the client's own tier,
 * which a server does not have. The deck tooltip sets it around the deck's text and {@code MixinCardDeck} reads it in
 * place of the client's tier.
 */
public final class DeckTextGreedTier
{
    private static final ThreadLocal<Integer> GREED_TIER_OF_DECK_TEXT = ThreadLocal.withInitial(() -> 0);


    private DeckTextGreedTier()
    {
    }


    /**
     * The greed tier the deck text being built on this thread is worked out for.
     *
     * @return the greed tier, 0 outside a deck text.
     */
    public static int get()
    {
        return GREED_TIER_OF_DECK_TEXT.get();
    }


    /**
     * Sets the greed tier the deck text built next on this thread is worked out for.
     *
     * @param greedTier the greed tier.
     */
    public static void set(int greedTier)
    {
        GREED_TIER_OF_DECK_TEXT.set(greedTier);
    }
}
