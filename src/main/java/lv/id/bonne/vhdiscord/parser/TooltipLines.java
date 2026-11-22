//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;


/**
 * Writes game tooltip lines as Discord markdown and notes their styled runs for the in-game picture; the line writers,
 * symbols and colours several item families share.
 */
final class TooltipLines
{
    /**
     * Symbol for text fields.
     */
    static final String DOT = "\u2022";

    /**
     * The colour the game's tooltips give gear levels, greed tiers, card tiers and companion levels.
     */
    static final int LEVEL_COLOUR = 0xB0C0FA;

    /**
     * The colour the game's gear and trinket tooltips give the crafter's name.
     */
    static final int CRAFTER_COLOUR = 0xFFE400;

    static final String WOLDS_VAULTS_MOD_ID = "woldsvaults";


    private TooltipLines()
    {
    }


    /**
     * Writes one game tooltip line as markdown ({@code **Label:** value}, or the whole line bold when it has no label)
     * and notes its styled runs, so the in-game picture draws it in the game's colours; a line holding line breaks is
     * written as several lines.
     *
     * @param builder Embed Builder.
     * @param line the game's line.
     */
    static void appendGameLine(StringBuilder builder, Component line)
    {
        String raw = line.getString();
        String text = raw.strip();
        int separator = text.indexOf(": ");

        if (raw.indexOf('\n') >= 0)
        {
            TooltipLines.splitAtLineBreaks(line).forEach(part -> TooltipLines.appendGameLine(builder, part));
        }
        else if (separator > 0)
        {
            TooltipLines.appendStyledLine(builder,
                "**" + text.substring(0, separator) + ":** " + text.substring(separator + 2),
                line);
        }
        else if (text.isEmpty())
        {
            builder.append("\n");
        }
        else
        {
            TooltipLines.appendStyledLine(builder, "**" + text + "**", line);
        }
    }


    /**
     * Splits a component at its line breaks, each part keeping its styles; config texts such as augment lore hold several
     * tooltip lines in one component.
     *
     * @param component the component.
     * @return one component per line.
     */
    private static List<MutableComponent> splitAtLineBreaks(Component component)
    {
        List<MutableComponent> lines = new ArrayList<>(List.of(new TextComponent("")));

        component.visit((style, text) ->
        {
            String[] parts = text.split("\n", -1);

            for (int index = 0; index < parts.length; index++)
            {
                if (index > 0)
                {
                    lines.add(new TextComponent(""));
                }

                if (!parts[index].isEmpty())
                {
                    lines.get(lines.size() - 1).append(new TextComponent(parts[index]).withStyle(style));
                }
            }

            return Optional.empty();
        }, Style.EMPTY);

        return lines;
    }


    /**
     * Writes a markdown line and notes the styled runs of the game's line it stands for, so the in-game picture draws
     * that line in the game's colours instead of reading the markdown.
     *
     * @param builder Embed Builder.
     * @param markdown the line as markdown.
     * @param line the game's line.
     */
    static void appendStyledLine(StringBuilder builder, String markdown, Component line)
    {
        builder.append(markdown).append("\n");
        TooltipColorHints.record(markdown.replace("**", "").strip(), line);
    }


    /**
     * Writes one game tooltip line as plain text, for description lines the game draws without a label, and notes its
     * styled runs; an empty line stays blank.
     *
     * @param builder Embed Builder.
     * @param line the game's line.
     */
    static void appendTextLine(StringBuilder builder, Component line)
    {
        String text = line.getString().strip();

        if (text.isEmpty())
        {
            builder.append("\n");
        }
        else
        {
            TooltipLines.appendStyledLine(builder, text, line);
        }
    }


    /**
     * Writes the lines an item's own tooltip code writes, each as {@link #appendGameLine} does; for items whose tooltip
     * Vault Hunters builds without client classes, so it runs on a dedicated server too.
     *
     * @param builder Embed Builder.
     * @param itemStack the item.
     */
    static void appendItemGameLines(StringBuilder builder, ItemStack itemStack)
    {
        List<Component> lines = new ArrayList<>();

        try
        {
            itemStack.getItem().appendHoverText(itemStack, null, lines, TooltipFlag.Default.NORMAL);
        }
        catch (RuntimeException | LinkageError failure)
        {
            if (VaultItemsHandler.GAME_TEXT_FAILURES.add(itemStack.getItem().getClass().getName()))
            {
                VaultItemsHandler.LOGGER.warn("{} cannot write its tooltip on the server; its picture has no description",
                    itemStack.getItem().getClass().getName(), failure);
            }

            return;
        }

        lines.forEach(line -> TooltipLines.appendGameLine(builder, line));
    }


    /**
     * A modifier line as infused catalysts and companion relics draw it: a bullet and the modifier's name, all in the
     * modifier's colour.
     *
     * @param builder Embed Builder.
     * @param vaultModifier the modifier.
     */
    static void appendBulletModifierLine(StringBuilder builder, VaultModifier<?> vaultModifier)
    {
        TooltipLines.appendTextLine(builder, new TextComponent(" " + DOT + " " + vaultModifier.getDisplayName()).
            withStyle(Style.EMPTY.withColor(vaultModifier.getDisplayTextColor())));
    }


    /**
     * Time parser
     * @param ticks how many ticks remaining
     * @return remaining ticks parsed as string.
     */
    static String formatTimeString(long ticks)
    {
        long seconds = ticks / 20L % 60L;
        long minutes = ticks / 20L / 60L % 60L;
        long hours = ticks / 20L / 60L / 60L;

        return hours > 0L
            ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
            : String.format("%02d:%02d", minutes, seconds);
    }
}
