//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.masuchatlink;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
     * @param hints the modifier runs and value colours noted while the markdown was written.
     * @param extraLines lines to add after the description, as they are.
     * @param modLine the mod name line MasuChatLink added, or null.
     * @return one list of segments per tooltip line.
     */
    public static List<List<TooltipSegment>> convert(String title,
        String titleColour,
        String markdown,
        TooltipColorHints.Hints hints,
        List<List<TooltipSegment>> extraLines,
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
                lines.add(MarkdownTooltipLines.lineOf(raw, hints));
            }
        }

        lines.addAll(extraLines);

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
     * @param hints the modifier runs and value colours noted while the markdown was written.
     * @return the line's segments, empty for a blank line.
     */
    private static List<TooltipSegment> lineOf(String raw, TooltipColorHints.Hints hints)
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
                Integer hint = hints.values().get(label);
                segments.add(new TooltipSegment(value, hint == null ? WHITE : MarkdownTooltipLines.hex(hint), false, false));
            }

            return segments;
        }

        Matcher tier = TIER.matcher(line);
        String body = tier.matches() ? tier.group(1) : line;
        String tierText = tier.matches() ? MarkdownTooltipLines.plain(tier.group(2)) : null;
        List<TooltipSegment> styled = MarkdownTooltipLines.styledModifier(MarkdownTooltipLines.plain(body), hints.lines());
        List<TooltipSegment> segments = new ArrayList<>();

        Matcher wholeBold = WHOLE_BOLD.matcher(body);

        if (styled != null)
        {
            segments.addAll(styled);
        }
        else if (wholeBold.matches())
        {
            segments.add(new TooltipSegment(wholeBold.group(1), WHITE, false, false));

            if (!wholeBold.group(2).isBlank())
            {
                segments.add(new TooltipSegment(MarkdownTooltipLines.plain(wholeBold.group(2)), GRAY, false, false));
            }
        }
        else
        {
            segments.add(new TooltipSegment(MarkdownTooltipLines.plain(body), WHITE, false, false));
        }

        if (tierText != null)
        {
            segments.add(new TooltipSegment(tierText, GRAY, false, false));
        }

        return segments;
    }


    /**
     * Draws a modifier line with the runs the game styled it with (category symbols in their own colours): an exact
     * match first, else the longest noted line it starts with, the rest in the last run's style.
     *
     * @param body the line without its tier details.
     * @param noted plain modifier lines to their styled runs.
     * @return the segments, or null when the line was not noted.
     */
    private static List<TooltipSegment> styledModifier(String body, Map<String, List<TooltipColorHints.Run>> noted)
    {
        String best = noted.containsKey(body) ? body : noted.keySet().stream().
            filter(body::startsWith).
            max(Comparator.comparingInt(String::length)).
            orElse(null);

        if (best == null || noted.get(best).isEmpty())
        {
            return null;
        }

        List<TooltipSegment> segments = new ArrayList<>();
        TooltipColorHints.Run last = null;

        for (TooltipColorHints.Run run : noted.get(best))
        {
            MarkdownTooltipLines.appendMerged(segments, MarkdownTooltipLines.segmentOf(run.text(), run));
            last = run;
        }

        if (body.length() > best.length())
        {
            segments.add(MarkdownTooltipLines.segmentOf(MarkdownTooltipLines.plain(body.substring(best.length())), last));
        }

        return segments;
    }


    /**
     * Appends a segment, joining it to the previous one when both look the same, so a line stays a few segments long.
     *
     * @param segments the line so far.
     * @param segment the segment to add.
     */
    private static void appendMerged(List<TooltipSegment> segments, TooltipSegment segment)
    {
        TooltipSegment previous = segments.isEmpty() ? null : segments.get(segments.size() - 1);

        if (previous != null &&
            Objects.equals(previous.color(), segment.color()) &&
            previous.bold() == segment.bold() &&
            previous.italic() == segment.italic())
        {
            segments.set(segments.size() - 1,
                new TooltipSegment(previous.text() + segment.text(), previous.color(), previous.bold(), previous.italic()));
        }
        else
        {
            segments.add(segment);
        }
    }


    private static TooltipSegment segmentOf(String text, TooltipColorHints.Run style)
    {
        String colour = style.colour() == null ? WHITE : MarkdownTooltipLines.hex(style.colour());
        return new TooltipSegment(text, colour, style.bold(), style.italic());
    }


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


    /**
     * Drops the bold markers Discord markdown uses.
     *
     * @param text the markdown text.
     * @return the text without {@code **}.
     */
    private static String plain(String text)
    {
        return text.replace("**", "");
    }
}
