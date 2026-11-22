//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.vaulthunters;


import java.util.List;
import java.util.Objects;

import iskallia.vault.core.card.modifier.deck.DeckModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;


/**
 * Writes a deck modifier's text for {@code MixinCardDeck}. The mod sets the parser's writer when it is constructed, which
 * falls back to a description when a modifier's own text needs the client; until then the modifier writes its own text
 * (looked up only when a deck's text is built, so a modifier method missing on a server cannot fail the mod's start).
 */
public final class DeckModifierTextWriter
{
    private static volatile Writer writer;


    private DeckModifierTextWriter()
    {
    }


    /**
     * Writes one deck modifier's text.
     */
    @FunctionalInterface
    public interface Writer
    {
        /**
         * Appends a modifier's text.
         *
         * @param modifier the modifier.
         * @param tooltip the tooltip to append to.
         * @param minIndex the game's tooltip index argument.
         * @param flag the tooltip flag.
         * @param time the animation time.
         */
        void write(DeckModifier<?> modifier, List<Component> tooltip, int minIndex, TooltipFlag flag, float time);
    }


    /**
     * Sets the writer every deck modifier's text goes through.
     *
     * @param newWriter the writer.
     */
    public static void use(Writer newWriter)
    {
        writer = Objects.requireNonNull(newWriter, "newWriter");
    }


    /**
     * Appends a modifier's text through the current writer.
     *
     * @param modifier the modifier.
     * @param tooltip the tooltip to append to.
     * @param minIndex the game's tooltip index argument.
     * @param flag the tooltip flag.
     * @param time the animation time.
     */
    public static void write(DeckModifier<?> modifier, List<Component> tooltip, int minIndex, TooltipFlag flag, float time)
    {
        Writer current = writer;

        if (current == null)
        {
            modifier.addText(tooltip, minIndex, flag, time);
        }
        else
        {
            current.write(modifier, tooltip, minIndex, flag, time);
        }
    }
}
