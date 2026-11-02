//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;


/**
 * Remembers how the game styles each gear modifier line (its category symbols and their colours included) and the
 * colour of each labelled value while {@link VaultItemsHandler} flattens the tooltip to plain text, so the MasuChatLink
 * integration can draw the tooltip as the game does. Only collects between {@link #start()} and {@link #take()} on the
 * same thread; other callers (mc2discord, DCIntegration) never start it and pay nothing.
 */
public final class TooltipColorHints
{
    /**
     * The hints of the tooltip being built on this thread, or null when nobody asked for them.
     */
    private static final ThreadLocal<Hints> HINTS = new ThreadLocal<>();


    private TooltipColorHints()
    {
    }


    /**
     * Starts collecting hints for the tooltip about to be built on this thread.
     */
    public static void start()
    {
        HINTS.set(new Hints(new LinkedHashMap<>(), new LinkedHashMap<>()));
    }


    /**
     * Ends collecting and returns the hints.
     *
     * @return the collected hints, empty when nothing was started.
     */
    public static Hints take()
    {
        Hints hints = HINTS.get();
        HINTS.remove();
        return hints == null ? Hints.EMPTY : hints;
    }


    /**
     * Records the styled runs of a modifier line.
     *
     * @param line the plain text the line is written as.
     * @param component the modifier's display component, category formatting applied.
     */
    static void record(String line, Component component)
    {
        Hints hints = HINTS.get();

        if (hints != null)
        {
            hints.lines().put(line, TooltipColorHints.runsOf(component));
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
        Hints hints = HINTS.get();

        if (hints != null)
        {
            hints.values().put(label, colour);
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
        TooltipColorHints.runsOf(component).stream().
            map(Run::colour).
            filter(Objects::nonNull).
            findFirst().
            ifPresent(colour -> TooltipColorHints.recordValue(label, colour));
    }


    /**
     * The non-empty text runs of a component with their effective style.
     *
     * @param component the component.
     * @return the runs in order.
     */
    private static List<Run> runsOf(Component component)
    {
        List<Run> runs = new ArrayList<>();
        component.visit((style, text) ->
        {
            if (!text.isEmpty())
            {
                TextColor colour = style.getColor();
                runs.add(new Run(text, colour == null ? null : colour.getValue(), style.isBold(), style.isItalic()));
            }

            return Optional.empty();
        }, Style.EMPTY);

        return runs;
    }


    /**
     * One styled run of a modifier line.
     *
     * @param text the run's text.
     * @param colour its RGB colour, or null for the default.
     * @param bold whether it is bold.
     * @param italic whether it is italic.
     */
    public record Run(String text, Integer colour, boolean bold, boolean italic)
    {
    }


    /**
     * What was noted while one tooltip was built.
     *
     * @param lines plain modifier lines to their styled runs.
     * @param values labels (without colon) to their value's RGB colour.
     */
    public record Hints(Map<String, List<Run>> lines, Map<String, Integer> values)
    {
        /**
         * No hints: nothing was collected.
         */
        public static final Hints EMPTY = new Hints(Map.of(), Map.of());
    }
}
