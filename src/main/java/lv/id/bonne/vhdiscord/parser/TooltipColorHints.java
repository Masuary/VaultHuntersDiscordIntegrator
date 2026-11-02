//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;


/**
 * Remembers the colour of each gear modifier line and labelled value while {@link VaultItemsHandler} flattens it to
 * plain text, so the MasuChatLink integration can draw the tooltip in the game's colours. Only collects between {@link #start()} and
 * {@link #take()} on the same thread; other callers (mc2discord, DCIntegration) never start it and pay nothing.
 */
public final class TooltipColorHints
{
    /**
     * Key prefix of a hint that colours only the value of a labelled line ({@code **Rarity:** Common}), keyed by its label.
     */
    public static final String VALUE_PREFIX = "value:";

    /**
     * The hints of the tooltip being built on this thread, or null when nobody asked for them.
     */
    private static final ThreadLocal<Map<String, Integer>> HINTS = new ThreadLocal<>();


    private TooltipColorHints()
    {
    }


    /**
     * Starts collecting hints for the tooltip about to be built on this thread.
     */
    public static void start()
    {
        HINTS.set(new LinkedHashMap<>());
    }


    /**
     * Ends collecting and returns the hints: plain line text to its RGB colour.
     *
     * @return the collected hints, empty when nothing was started.
     */
    public static Map<String, Integer> take()
    {
        Map<String, Integer> hints = HINTS.get();
        HINTS.remove();
        return hints == null ? Map.of() : hints;
    }


    /**
     * Records the colour of a modifier line, read from the first coloured part of its component.
     *
     * @param line the plain text the line is written as.
     * @param component the modifier's display component.
     */
    static void record(String line, Component component)
    {
        Map<String, Integer> hints = HINTS.get();
        Integer colour = TooltipColorHints.colourOf(component);

        if (hints != null && colour != null)
        {
            hints.put(line, colour);
        }
    }


    /**
     * Records the colour the game gives the value of a labelled line.
     *
     * @param label the label without its colon, e.g. Rarity.
     * @param colour the value's RGB colour.
     */
    static void recordValue(String label, int colour)
    {
        Map<String, Integer> hints = HINTS.get();

        if (hints != null)
        {
            hints.put(VALUE_PREFIX + label, colour);
        }
    }


    /**
     * Records the colour of a labelled line's value, read from the first coloured part of its component.
     *
     * @param label the label without its colon, e.g. Rarity.
     * @param component the value's component.
     */
    static void recordValue(String label, Component component)
    {
        Integer colour = TooltipColorHints.colourOf(component);

        if (colour != null)
        {
            TooltipColorHints.recordValue(label, colour);
        }
    }


    /**
     * The colour of the first coloured, non-blank part of a component.
     *
     * @param component the component.
     * @return its RGB colour, or null when no part is coloured.
     */
    private static Integer colourOf(Component component)
    {
        Integer[] colour = { null };
        component.visit((style, text) ->
        {
            TextColor textColor = style.getColor();

            if (colour[0] == null && textColor != null && !text.isBlank())
            {
                colour[0] = textColor.getValue();
            }

            return Optional.empty();
        }, Style.EMPTY);

        return colour[0];
    }
}
