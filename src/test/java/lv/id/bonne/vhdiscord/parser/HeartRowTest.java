//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class HeartRowTest
{
    @Test
    void evenHitPointsAreFullHearts()
    {
        assertEquals("♥♥♥", HeartRow.hearts(6.0F));
        assertFalse(HeartRow.hasMoreThanShown(6.0F));
    }


    @Test
    void oddHitPointsEndWithAHalfHeart()
    {
        assertEquals("♥♥♥♡", HeartRow.hearts(7.0F));
        assertEquals("♡", HeartRow.hearts(1.0F));
    }


    @Test
    void hitPointsRoundLikeTheGame()
    {
        assertEquals("♥♥♥♡", HeartRow.hearts(6.6F));
        assertEquals("♥♥♥", HeartRow.hearts(6.4F));
    }


    @Test
    void noHealingIsNoHearts()
    {
        assertEquals("", HeartRow.hearts(0.0F));
        assertEquals("", HeartRow.hearts(-3.0F));
    }


    @Test
    void atMostTenHeartsWithAMarkForMore()
    {
        assertEquals("♥".repeat(10), HeartRow.hearts(20.0F));
        assertFalse(HeartRow.hasMoreThanShown(20.0F));
        assertEquals("♥".repeat(10), HeartRow.hearts(25.0F));
        assertTrue(HeartRow.hasMoreThanShown(25.0F));
        assertEquals("♥".repeat(10), HeartRow.hearts(Float.MAX_VALUE));
    }
}
