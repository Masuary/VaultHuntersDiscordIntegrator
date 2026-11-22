package lv.id.bonne.vhdiscord.parser;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class TooltipLimitsTest
{
    private static final String HEART = "❤";


    @Test
    void repeatsSmallCountsExactly()
    {
        assertEquals(HEART.repeat(3), TooltipLimits.repeat(HEART, 3));
        assertEquals("● ● ", TooltipLimits.repeat("● ", 2));
    }


    @Test
    void negativeCountIsEmpty()
    {
        assertEquals("", TooltipLimits.repeat(HEART, -5));
        assertEquals("", TooltipLimits.repeat(HEART, Integer.MIN_VALUE));
    }


    @Test
    void forgedCountIsCapped()
    {
        assertEquals(TooltipLimits.MAXIMUM_REPEATED_SYMBOLS, TooltipLimits.repeat(HEART, 900_000_000).length());
        assertEquals(TooltipLimits.MAXIMUM_REPEATED_SYMBOLS * 2, TooltipLimits.repeat("○ ", Integer.MAX_VALUE).length());
    }


    @Test
    void gameDecksAreDescribable()
    {
        assertTrue(TooltipLimits.isDescribableDeck(-1, 0));
        assertTrue(TooltipLimits.isDescribableDeck(3, 50));
        assertTrue(TooltipLimits.isDescribableDeck(TooltipLimits.MAXIMUM_DECK_SOCKETS, TooltipLimits.MAXIMUM_DECK_SLOTS));
    }


    @Test
    void forgedDecksAreNotDescribable()
    {
        assertFalse(TooltipLimits.isDescribableDeck(900_000_000, 10));
        assertFalse(TooltipLimits.isDescribableDeck(TooltipLimits.MAXIMUM_DECK_SOCKETS + 1, 10));
        assertFalse(TooltipLimits.isDescribableDeck(3, TooltipLimits.MAXIMUM_DECK_SLOTS + 1));
    }


    @Test
    void oversizedDescriptionIsNotAttachable()
    {
        assertTrue(TooltipLimits.isAttachableDescription("**Level:** 80"));
        assertTrue(TooltipLimits.isAttachableDescription("x".repeat(TooltipLimits.MAXIMUM_DESCRIPTION_LENGTH)));
        assertFalse(TooltipLimits.isAttachableDescription("x".repeat(TooltipLimits.MAXIMUM_DESCRIPTION_LENGTH + 1)));
    }
}
