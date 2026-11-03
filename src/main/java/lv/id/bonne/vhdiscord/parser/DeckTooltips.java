//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import iskallia.vault.core.card.CardDeck;
import iskallia.vault.core.card.modifier.deck.DeckModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.TooltipFlag;


/**
 * A card deck's tooltip as the game writes it, built on the server. VH's deck and deck modifier text is client-side code;
 * the server-only mixins in {@code vaulthunters.mixin} restore what Forge strips from it on a dedicated server and stand in
 * for the client's Shift key (not held), so the deck's own code, with every pack's modifiers and their socket rules, writes
 * the text.
 */
public final class DeckTooltips
{
    /**
     * The logger: a modifier whose text cannot be built on the server is reported once per modifier class.
     */
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Modifier classes whose text failed, so each is logged once.
     */
    private static final Set<Class<?>> FAILED_MODIFIERS = ConcurrentHashMap.newKeySet();


    private DeckTooltips()
    {
    }


    /**
     * The deck's tooltip lines: attributes, resource card rewards, sockets and what each modifier does.
     *
     * @param deck the deck.
     * @return the lines, blank lines included.
     */
    public static List<Component> deckLines(CardDeck deck)
    {
        List<Component> lines = new ArrayList<>();
        deck.addText(lines, 0, TooltipFlag.Default.NORMAL, 0f);
        return lines;
    }


    /**
     * What a deck modifier (a deck core) does, as its tooltip says it.
     *
     * @param modifier the modifier.
     * @return the lines.
     */
    public static List<Component> modifierLines(DeckModifier<?> modifier)
    {
        List<Component> lines = new ArrayList<>();
        DeckTooltips.appendModifierText(modifier, lines, 0, TooltipFlag.Default.NORMAL, 0f);
        return lines;
    }


    /**
     * Appends a modifier's own text. A modifier whose text needs more of the client than the mixins provide (one added
     * after them) is written as this mod describes it, or by its name, instead of failing the whole deck.
     *
     * @param modifier the modifier.
     * @param tooltip the tooltip to append to.
     * @param minIndex the game's tooltip index argument.
     * @param flag the tooltip flag.
     * @param time the animation time.
     */
    public static void appendModifierText(DeckModifier<?> modifier, List<Component> tooltip, int minIndex, TooltipFlag flag, float time)
    {
        List<Component> text = new ArrayList<>();

        try
        {
            modifier.addText(text, minIndex, flag, time);
            tooltip.addAll(text);
        }
        catch (RuntimeException | LinkageError clientOnly)
        {
            if (FAILED_MODIFIERS.add(modifier.getClass()))
            {
                LOGGER.warn("The tooltip of deck modifier {} cannot be built on the server; it is described by name", modifier.getClass().getName(), clientOnly);
            }

            String description = VaultItemsHandler.describeDeckModifier(modifier);
            String fallback = description != null ? description : modifier.getName();

            if (fallback != null)
            {
                tooltip.add(new TextComponent(fallback));
            }
        }
    }
}
