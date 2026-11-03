//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.masuchatlink;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.masuary.masuchatlink.api.TooltipDeck;
import com.masuary.masuchatlink.api.TooltipSegment;

import iskallia.vault.core.card.Card;
import iskallia.vault.core.card.CardDeck;
import iskallia.vault.core.card.CardEntry;
import iskallia.vault.core.card.CardPos;
import iskallia.vault.core.card.CardTooltipContext;
import iskallia.vault.core.card.modifier.card.TaskLootCardModifier;
import iskallia.vault.core.card.modifier.deck.DeckModifier;
import iskallia.vault.gear.attribute.VaultGearAttributeInstance;
import iskallia.vault.gear.attribute.VaultGearModifier;
import iskallia.vault.item.CardDeckItem;
import lv.id.bonne.vhdiscord.parser.VaultItemsHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;


/**
 * Builds a card deck's picture for MasuChatLink: the deck screen (each slot of its layout with the card's face and icon,
 * named as VH's own card models) and the stats under it (attributes, sockets and what the cores do). Uses only the deck's
 * data; VH's deck tooltip and card renderer are client-only.
 */
public final class DeckPicture
{
    private static final String WHITE = "#FFFFFF";

    private static final String YELLOW = "#FFFF55";

    private static final String GRAY = "#AAAAAA";

    private static final String ITEM_MODEL_PREFIX = "the_vault:";

    private static final String ICON_MODEL_PREFIX = "the_vault:card/icon/";


    private DeckPicture()
    {
    }


    /**
     * The picture of a linked deck.
     *
     * @param deck the deck screen.
     * @param lines the stats lines under it, ending with the mod line.
     */
    public record Picture(TooltipDeck deck, List<List<TooltipSegment>> lines)
    {
    }


    /**
     * Builds the picture of a deck item.
     *
     * @param itemStack the item.
     * @param modLine the mod name line MasuChatLink added, or null.
     * @return the picture, or empty when the item holds no deck.
     */
    public static Optional<Picture> of(ItemStack itemStack, List<TooltipSegment> modLine)
    {
        if (!(itemStack.getItem() instanceof CardDeckItem))
        {
            return Optional.empty();
        }

        return CardDeckItem.getCardDeck(itemStack).map(deck ->
        {
            List<List<TooltipSegment>> lines = DeckPicture.statsLines(deck);

            if (modLine != null)
            {
                lines.add(modLine);
            }

            return new Picture(DeckPicture.screen(deck, DeckPicture.titleOf(itemStack)), lines);
        });
    }


    /**
     * The deck's name as its screen shows it, without the socket marks the item name ends with.
     *
     * @param itemStack the deck item.
     * @return the title.
     */
    private static String titleOf(ItemStack itemStack)
    {
        return itemStack.getHoverName().getString().replaceAll("\\s*\\([^)]*\\)\\s*$", "").strip();
    }


    /**
     * Every slot of the deck's layout, with the card in it where there is one.
     *
     * @param deck the deck.
     * @param title the deck's title.
     * @return the deck screen.
     */
    private static TooltipDeck screen(CardDeck deck, String title)
    {
        Set<CardPos> positions = deck.getSlots();
        int minimumX = positions.stream().mapToInt(position -> position.x).min().orElse(0);
        int minimumY = positions.stream().mapToInt(position -> position.y).min().orElse(0);
        int columns = positions.stream().mapToInt(position -> position.x).max().orElse(0) - minimumX + 1;
        int rows = positions.stream().mapToInt(position -> position.y).max().orElse(0) - minimumY + 1;
        List<TooltipDeck.Slot> slots = new ArrayList<>();

        for (CardPos position : positions)
        {
            Optional<Card> card = deck.getCard(position).filter(content -> !content.getEntries().isEmpty());
            String face = card.map(DeckPicture::faceOf).orElse(null);
            String icon = face == null ? null : card.map(DeckPicture::iconOf).orElse(null);
            double iconTop = card.map(DeckPicture::iconTopOf).orElse(0.0);
            boolean arcane = position.allowedGroups != null && position.allowedGroups.contains("Arcane");
            slots.add(new TooltipDeck.Slot(position.x - minimumX, position.y - minimumY, face, icon, iconTop, arcane));
        }

        return new TooltipDeck(title, columns, rows, slots);
    }


    /**
     * The card's face model, by VH's card renderer: wild cards by all their colours, other kinds by colour (and tier).
     *
     * @param card the card.
     * @return the model name under {@code the_vault:card/}, or null for a card without a colour (the game cycles those).
     */
    private static String faceOf(Card card)
    {
        CardEntry.Color colour = card.getFirstColor();

        if (colour == null)
        {
            return null;
        }

        String name = colour.name().toLowerCase();

        if (card.hasGroup("Wild"))
        {
            StringBuilder colours = new StringBuilder();

            for (CardEntry.Color value : CardEntry.Color.values())
            {
                if (card.getColors().contains(value))
                {
                    colours.append(value.name().toLowerCase()).append('_');
                }
            }

            return colours + "wild";
        }

        if (card.hasGroup("Arcane"))
        {
            return name + "_arcane";
        }

        if (card.hasGroup("Temporal"))
        {
            return name + "_temporal_" + card.getTier();
        }

        if (card.hasGroup("Resource"))
        {
            return name + "_resource";
        }

        if (card.hasGroup("Evolution"))
        {
            return name + "_evolution_" + card.getTier();
        }

        if (card.hasGroup("Greed"))
        {
            return "greed";
        }

        return name + "_" + card.getTier();
    }


    /**
     * The card's icon: a card icon model ({@code the_vault:card/icon/thorns} is {@code thorns}) or, on resource cards, an
     * item model ({@code the_vault:vault_silver} is {@code item_vault_silver}).
     *
     * @param card the card.
     * @return the icon name, or null when the card has no icon the picture knows.
     */
    private static String iconOf(Card card)
    {
        String model = card.getFirstModel();

        if (model == null)
        {
            return null;
        }

        int variant = model.indexOf('#');
        String location = model.substring(0, variant < 0 ? model.length() : variant);

        if (location.startsWith(ICON_MODEL_PREFIX))
        {
            return location.substring(ICON_MODEL_PREFIX.length());
        }

        if (location.startsWith(ITEM_MODEL_PREFIX) && location.indexOf('/') < 0)
        {
            return "item_" + location.substring(ITEM_MODEL_PREFIX.length());
        }

        return null;
    }


    /**
     * Where VH's card renderer puts the half-size icon: its top as a fraction of the card's height.
     *
     * @param card the card.
     * @return the fraction.
     */
    private static double iconTopOf(Card card)
    {
        if (card.hasGroup("Wild") || card.hasGroup("Arcane") || card.hasGroup("Temporal"))
        {
            return 0.235;
        }

        if (card.hasGroup("Resource"))
        {
            return 0.25;
        }

        return 0.125;
    }


    /**
     * The deck's stats in the game's colours: attributes, sockets and what each core does.
     *
     * @param deck the deck.
     * @return the lines.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<List<TooltipSegment>> statsLines(CardDeck deck)
    {
        List<List<TooltipSegment>> lines = new ArrayList<>();

        for (VaultGearAttributeInstance instance : deck.getSnapshotAttributes())
        {
            MutableComponent text = instance.getAttribute().getReader().getDisplay(instance, VaultGearModifier.AffixType.PREFIX);

            if (text != null)
            {
                lines.add(DeckPicture.segmentsOf(text));
            }
        }

        for (Component text : DeckPicture.resourceLines(deck))
        {
            lines.add(DeckPicture.segmentsOf(text));
        }

        int socketCount = deck.getSocketCount();

        if (socketCount > 0)
        {
            int filled = deck.getModifiers().size();
            List<TooltipSegment> sockets = new ArrayList<>();
            sockets.add(new TooltipSegment("Sockets: ", WHITE, false, false));
            sockets.add(new TooltipSegment("● ".repeat(filled).strip(), YELLOW, false, false));

            if (socketCount > filled)
            {
                sockets.add(new TooltipSegment((filled > 0 ? " " : "") + "○ ".repeat(socketCount - filled).strip(), GRAY, false, false));
            }

            lines.add(List.of());
            lines.add(sockets);
        }

        List<List<TooltipSegment>> cores = new ArrayList<>();

        for (DeckModifier<?> modifier : deck.getModifiers())
        {
            String description = VaultItemsHandler.describeDeckModifier(modifier);

            if (description != null)
            {
                cores.add(List.of(new TooltipSegment(description, WHITE, false, false)));
            }
        }

        if (!cores.isEmpty())
        {
            lines.add(List.of());
            lines.addAll(cores);
        }

        return lines;
    }


    /**
     * What the deck's resource cards earn, written by VH's own card modifier as the game's deck tooltip does (cards by name,
     * each inside its deck context so requirement modifiers apply).
     *
     * @param deck the deck.
     * @return the lines.
     */
    private static List<Component> resourceLines(CardDeck deck)
    {
        List<Component> lines = new ArrayList<>();
        deck.getCards().entrySet().stream().
            filter(entry -> entry.getValue() != null).
            sorted(Comparator.comparing(
                entry -> Optional.ofNullable(entry.getValue().getFirstName()).map(Component::getString).orElse(""),
                String.CASE_INSENSITIVE_ORDER)).
            forEachOrdered(entry ->
            {
                Card card = entry.getValue();

                for (CardEntry cardEntry : card.getEntries())
                {
                    if (cardEntry.getModifier() instanceof TaskLootCardModifier modifier)
                    {
                        CardTooltipContext.with(deck, entry.getKey(), card, () ->
                        {
                            modifier.addText(lines, 0, TooltipFlag.Default.NORMAL, 0f, card.getTier());
                            return null;
                        });
                    }
                }
            });

        return lines;
    }


    /**
     * A component's text runs with their colours, neighbours of the same look joined.
     *
     * @param component the component.
     * @return the segments.
     */
    private static List<TooltipSegment> segmentsOf(Component component)
    {
        List<TooltipSegment> segments = new ArrayList<>();
        component.visit((style, text) ->
        {
            if (!text.isEmpty())
            {
                TextColor colour = style.getColor();
                String hex = colour == null ? WHITE : String.format("#%06X", colour.getValue() & 0xFFFFFF);
                int last = segments.size() - 1;

                if (last >= 0 && segments.get(last).color().equals(hex) && segments.get(last).bold() == style.isBold() &&
                    segments.get(last).italic() == style.isItalic())
                {
                    TooltipSegment previous = segments.get(last);
                    segments.set(last, new TooltipSegment(previous.text() + text, hex, style.isBold(), style.isItalic()));
                }
                else
                {
                    segments.add(new TooltipSegment(text, hex, style.isBold(), style.isItalic()));
                }
            }

            return Optional.empty();
        }, Style.EMPTY);

        return segments;
    }
}
