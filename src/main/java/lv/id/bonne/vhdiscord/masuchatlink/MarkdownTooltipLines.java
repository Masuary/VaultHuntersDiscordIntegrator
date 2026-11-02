//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.masuchatlink;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.masuary.masuchatlink.api.TooltipSegment;
import lv.id.bonne.vhdiscord.parser.TooltipColorHints;


/**
 * Turns the Discord markdown {@code VaultItemsHandler} writes into tooltip lines for MasuChatLink's in-game picture:
 * {@code **Label:** value} lines read as a grey label and a white value, a whole {@code **bold**} line stays bold, gear
 * modifier lines take the colour noted while they were built, and tier details in brackets turn grey.
 */
public final class MarkdownTooltipLines
{
    private static final String GRAY = "#AAAAAA";

    private static final String WHITE = "#FFFFFF";

    private static final String YELLOW = "#FFFF55";

    private static final char FULL_CIRCLE = '\u25CF';

    private static final char EMPTY_CIRCLE = '\u25CB';

    /**
     * The game draws every repair as a filled dot and tells used from remaining by colour.
     */
    private static final char DOT = '\u25CF';

    private static final Pattern LABELLED = Pattern.compile("^\\*\\*([^*]+?):\\*\\*\\s*(.*)$");

    /**
     * A whole line in single asterisks: a unique item's lore, drawn gray and italic like the game, wrapped by words.
     */
    private static final Pattern LORE = Pattern.compile("^\\*([^*].*[^*])\\*$");

    private static final int LORE_WIDTH = 48;

    private static final Pattern WHOLE_BOLD = Pattern.compile("^\\*\\*([^*]+)\\*\\*(.*)$");

    private static final Pattern TIER = Pattern.compile("^(.*?)( \\((?:\\*\\*Legendary\\*\\* )?T\\d+.*\\))$");


    private MarkdownTooltipLines()
    {
    }


    /**
     * Builds the tooltip lines for one item.
     *
     * @param title the item's name.
     * @param titleColour the name's colour as #RRGGBB, or null for white.
     * @param markdown the description VaultItemsHandler wrote.
     * @param colourHints plain modifier lines to their colour.
     * @param modLine the mod name line MasuChatLink added, or null.
     * @return one list of segments per tooltip line.
     */
    public static List<List<TooltipSegment>> convert(String title,
        String titleColour,
        String markdown,
        Map<String, Integer> colourHints,
        List<TooltipSegment> modLine)
    {
        List<List<TooltipSegment>> lines = new ArrayList<>();
        lines.add(List.of(new TooltipSegment(title, titleColour, false, false)));

        for (String raw : markdown.strip().split("\n", -1))
        {
            Matcher lore = LORE.matcher(raw.strip());

            if (lore.matches())
            {
                MarkdownTooltipLines.wrap(lore.group(1), LORE_WIDTH).forEach(part ->
                    lines.add(List.of(new TooltipSegment(part, GRAY, false, true))));
            }
            else
            {
                lines.add(MarkdownTooltipLines.lineOf(raw, colourHints));
            }
        }

        if (modLine != null)
        {
            lines.add(modLine);
        }

        return lines;
    }


    /**
     * Converts one markdown line.
     *
     * @param raw the markdown line.
     * @param colourHints plain modifier lines to their colour.
     * @return the line's segments, empty for a blank line.
     */
    private static List<TooltipSegment> lineOf(String raw, Map<String, Integer> colourHints)
    {
        String line = raw.stripTrailing();

        if (line.isBlank())
        {
            return List.of();
        }

        Matcher labelled = LABELLED.matcher(line);

        if (labelled.matches())
        {
            String label = labelled.group(1);
            String value = MarkdownTooltipLines.plain(labelled.group(2)).strip();
            List<TooltipSegment> segments = new ArrayList<>();
            segments.add(new TooltipSegment(label + ": ", value.isEmpty() ? GRAY : WHITE, false, false));

            if (label.equals("Repairs"))
            {
                segments.addAll(MarkdownTooltipLines.repairDots(value));
            }
            else if (!value.isEmpty())
            {
                Integer hint = colourHints.get(TooltipColorHints.VALUE_PREFIX + label);
                segments.add(new TooltipSegment(value, hint == null ? WHITE : MarkdownTooltipLines.hex(hint), false, false));
            }

            return segments;
        }

        Matcher tier = TIER.matcher(line);
        String body = tier.matches() ? tier.group(1) : line;
        String tierText = tier.matches() ? MarkdownTooltipLines.plain(tier.group(2)) : null;
        Integer hint = MarkdownTooltipLines.hintFor(body, colourHints);
        List<TooltipSegment> segments = new ArrayList<>();

        Matcher wholeBold = WHOLE_BOLD.matcher(body);

        if (hint == null && wholeBold.matches())
        {
            segments.add(new TooltipSegment(wholeBold.group(1), WHITE, false, false));

            if (!wholeBold.group(2).isBlank())
            {
                segments.add(new TooltipSegment(MarkdownTooltipLines.plain(wholeBold.group(2)), GRAY, false, false));
            }
        }
        else
        {
            String colour = hint == null ? WHITE : MarkdownTooltipLines.hex(hint);
            segments.add(new TooltipSegment(MarkdownTooltipLines.plain(body), colour, false, false));
        }

        if (tierText != null)
        {
            segments.add(new TooltipSegment(tierText, GRAY, false, false));
        }

        return segments;
    }


    /**
     * Finds the colour noted for a modifier line: an exact match first, else the longest noted line it starts with.
     *
     * @param body the line without its tier details.
     * @param colourHints plain modifier lines to their colour.
     * @return the colour, or null.
     */
    private static Integer hintFor(String body, Map<String, Integer> colourHints)
    {
        Integer exact = colourHints.get(body);

        if (exact != null)
        {
            return exact;
        }

        String best = null;

        for (String noted : colourHints.keySet())
        {
            if (body.startsWith(noted) && (best == null || noted.length() > best.length()))
            {
                best = noted;
            }
        }

        return best == null ? null : colourHints.get(best);
    }


    /**
     * Drops the bold markers Discord markdown uses.
     *
     * @param text the markdown text.
     * @return the text without {@code **}.
     */
    /**
     * The repair dots as the game draws them: used repairs yellow, the remaining ones gray.
     *
     * @param dots the dots VaultItemsHandler wrote, full circles for used repairs and empty ones for the rest.
     * @return one segment per colour run.
     */
    private static List<TooltipSegment> repairDots(String dots)
    {
        long used = dots.chars().filter(character -> character == FULL_CIRCLE).count();
        long remaining = dots.chars().filter(character -> character == EMPTY_CIRCLE).count();
        List<TooltipSegment> segments = new ArrayList<>();

        if (used > 0)
        {
            segments.add(new TooltipSegment((DOT + " ").repeat((int) used), YELLOW, false, false));
        }

        if (remaining > 0)
        {
            segments.add(new TooltipSegment((DOT + " ").repeat((int) remaining), GRAY, false, false));
        }

        return segments;
    }


    private static List<String> wrap(String text, int width)
    {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : text.split("\\s+"))
        {
            if (current.length() > 0 && current.length() + 1 + word.length() > width)
            {
                parts.add(current.toString());
                current.setLength(0);
            }

            if (current.length() > 0)
            {
                current.append(' ');
            }

            current.append(word);
        }

        if (current.length() > 0)
        {
            parts.add(current.toString());
        }

        return parts;
    }


    private static String hex(int colour)
    {
        return String.format("#%06X", colour & 0xFFFFFF);
    }


    private static String plain(String text)
    {
        return text.replace("**", "");
    }
}
