//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import iskallia.vault.core.card.Card;
import iskallia.vault.core.card.CardCondition;
import iskallia.vault.core.card.CardEntry;
import iskallia.vault.core.card.CardPos;
import iskallia.vault.core.card.CardProperty;
import iskallia.vault.core.card.modifier.card.GearCardModifier;
import iskallia.vault.core.card.modifier.deck.BountyDeckModifier;
import iskallia.vault.core.card.modifier.deck.DeckModifier;
import iskallia.vault.core.card.modifier.deck.GlobalDeckModifier;
import iskallia.vault.core.card.modifier.deck.NonFoilEfficiencyDeckModifier;
import iskallia.vault.core.card.modifier.deck.ResourceDoubleDeckModifier;
import iskallia.vault.core.card.modifier.deck.ResourceRequirementDeckModifier;
import iskallia.vault.core.card.modifier.deck.SlotDeckModifier;
import iskallia.vault.core.card.modifier.deck.StatEfficiencyDeckModifier;
import iskallia.vault.gear.attribute.VaultGearAttributeInstance;
import iskallia.vault.gear.attribute.VaultGearModifier;
import iskallia.vault.gear.attribute.config.ConfigurableAttributeGenerator;
import iskallia.vault.gear.reader.VaultGearModifierReader;
import iskallia.vault.item.CardDeckItem;
import iskallia.vault.item.CardItem;
import iskallia.vault.item.DeckSocketItem;
import lv.id.bonne.vhdiscord.vaulthunters.mixin.CardEntryAccessor;
import lv.id.bonne.vhdiscord.vaulthunters.mixin.GearCardModifierAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;


/**
 * Tooltips of cards, card decks and deck sockets, and what deck modifiers do.
 */
final class CardTooltips
{
    /**
     * The colour the game's card tooltip gives types and groups.
     */
    private static final int CARD_GROUP_COLOUR = 0xCCCCCC;

    /**
     * How the game's deck tooltip starts its sockets line.
     */
    private static final String SOCKETS_PREFIX = "Sockets:";


    private CardTooltips()
    {
    }


    static void handleDeckSocketTooltip(StringBuilder builder, ItemStack itemStack)
    {
        DeckSocketItem.getDeckModifier(itemStack).
            map(DeckTooltips::modifierLines).
            ifPresent(lines -> lines.forEach(line -> TooltipLines.appendStyledLine(builder, line.getString(), line)));
    }


    /**
     * This method describes what a deck core does, as its tooltip says it.
     * @param modifier Deck modifier of the core.
     * @return The description, or null for a modifier without one.
     */
    static String describeDeckModifier(DeckModifier<?> modifier)
    {
        float value = modifier.getModifierValue();

        if (modifier instanceof StatEfficiencyDeckModifier)
        {
            return String.format("+%.1f%% Stat card efficiency per unique deck color", value * 100f);
        }
        else if (modifier instanceof NonFoilEfficiencyDeckModifier)
        {
            return String.format("+%.1f%% Efficiency to all Stat and Evolution cards per non-foil card in the deck", value * 100f);
        }
        else if (modifier instanceof ResourceDoubleDeckModifier)
        {
            return String.format("+%.0f%% chance for Resource cards to double rewards", value * 100f);
        }
        else if (modifier instanceof ResourceRequirementDeckModifier)
        {
            return String.format("-%.0f%% Resource card requirements", value * 100f);
        }
        else if (modifier instanceof GlobalDeckModifier globalModifier)
        {
            List<String> affectedCards = new ArrayList<>();
            affectedCards.addAll(globalModifier.getConfig().requiredColors.stream()
                .map(Enum::name)
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .toList());
            affectedCards.addAll(globalModifier.getConfig().requiredGroups.stream()
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .toList());

            String target = affectedCards.isEmpty() ? "all" : String.join(", ", affectedCards);
            return String.format("+%.0f%% efficiency to %s cards", value * 100f, target);
        }
        else if (modifier instanceof BountyDeckModifier bountyModifier)
        {
            int tierIncrease = (int) Math.ceil(bountyModifier.getTierIncrease());
            CompoundTag nbt = modifier.writeNbt().orElse(new CompoundTag());
            int resourcesNeeded = nbt.getInt("resourcesNeeded");
            return String.format("+%d Crate %s Completing a vault with at least %d Resource %s",
                tierIncrease, tierIncrease == 1 ? "Tier" : "Tiers",
                resourcesNeeded, resourcesNeeded == 1 ? "Card" : "Cards");
        }
        else if (modifier instanceof SlotDeckModifier slotModifier)
        {
            CompoundTag nbt = modifier.writeNbt().orElse(new CompoundTag());
            int slotRoll = nbt.getInt("slotRoll");

            List<String> affectedCards = new ArrayList<>();
            affectedCards.addAll(slotModifier.getConfig()
                .getRequiredColors(slotModifier.getConfig().getSelectedRollId()).stream()
                .map(Enum::name)
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .toList());
            affectedCards.addAll(slotModifier.getConfig()
                .getRequiredGroups(slotModifier.getConfig().getSelectedRollId()).stream()
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .toList());

            String target = affectedCards.isEmpty() ? "all cards" : String.join(", ", affectedCards);
            return String.format("+%.0f%% efficiency for %d %s %s",
                value * 100f, slotRoll, target, slotRoll == 1 ? "card" : "cards");
        }

        return null;
    }


    static void handleCardTooltip(StringBuilder builder, ItemStack itemStack)
    {
        Card card = CardItem.getCard(itemStack);

        TooltipLines.appendGameLine(builder, new TextComponent("Tier: ").
            append(new TextComponent(String.valueOf(card.getTier())).withStyle(Style.EMPTY.withColor(TooltipLines.LEVEL_COLOUR))));

        List<CardEntry.Color> colors = new ArrayList<>(card.getColors());

        if (!colors.isEmpty())
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Color: ").
                append(CardTooltips.joinedCardList(colors.stream().map(CardEntry.Color::getColoredText).toList())));
        }

        List<String> groups = new ArrayList<>(card.getGroups());
        List<String> types = new ArrayList<>();

        for (String type : Card.TYPES)
        {
            if (groups.remove(type))
            {
                types.add(type);
            }
        }

        if (!types.isEmpty())
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Type: ").
                append(CardTooltips.joinedCardList(CardTooltips.cardGroupTexts(types))));
        }

        if (!groups.isEmpty())
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Groups: ").
                append(CardTooltips.joinedCardList(CardTooltips.cardGroupTexts(groups))));
        }

        for (CardEntry entry : card.getEntries())
        {
            addCardProperty(builder, entry.getModifier(), card.getTier());

            if (entry.getScaler() != null)
            {
                addCardProperty(builder, entry.getScaler(), card.getTier());
            }

            CardCondition condition = ((CardEntryAccessor) entry).getCondition();

            if (condition != null)
            {
                addCardProperty(builder, condition, card.getTier());
            }
        }
    }


    /**
     * Card types and groups as the game's card tooltip draws them.
     *
     * @param names the type or group names.
     * @return one component per name.
     */
    private static List<Component> cardGroupTexts(List<String> names)
    {
        return names.stream().
            map(name -> (Component) new TextComponent(name).withStyle(Style.EMPTY.withColor(CARD_GROUP_COLOUR))).
            toList();
    }


    /**
     * Joins card tooltip values with the game's grey commas.
     *
     * @param parts the values.
     * @return the joined component.
     */
    private static Component joinedCardList(List<Component> parts)
    {
        TextComponent joined = new TextComponent("");

        for (int i = 0; i < parts.size(); ++i)
        {
            joined.append(parts.get(i));

            if (i != parts.size() - 1)
            {
                joined.append(new TextComponent(", ").withStyle(ChatFormatting.GRAY));
            }
        }

        return joined;
    }


    /**
     * Writes a card deck's cards, attributes, sockets and modifiers. Since Vault Hunters 7209 the attributes include the
     * greed bonus of the linking player's greed tier.
     *
     * @param builder Embed Builder.
     * @param itemStack the deck.
     * @param linkingPlayer the player who linked the deck, or null when unknown (greed tier 0).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void handleCardDeckTooltip(StringBuilder builder, ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        CardDeckItem.getCardDeck(itemStack).filter(DeckTooltips::isDescribable).ifPresent(deck ->
        {
            int greedTier = DeckTooltips.greedTierOf(linkingPlayer);
            Map<CardPos, Card> cards = deck.getCards();
            long filledSlots = cards.values().stream().
                filter(card -> card != null && !card.getEntries().isEmpty()).count();

            if (filledSlots > 0)
            {
                builder.append("**Cards:** (").append(filledSlots).append("/").append(cards.size()).append(")\n");

                cards.entrySet().stream().
                    filter(entry -> entry.getValue() != null && !entry.getValue().getEntries().isEmpty()).
                    sorted(Comparator.comparing(
                        entry -> Optional.ofNullable(entry.getValue().getFirstName()).
                            map(Component::getString).orElse(""),
                        String.CASE_INSENSITIVE_ORDER)).
                    forEachOrdered(entry ->
                    {
                        Card card = entry.getValue();
                        String cardName = Optional.ofNullable(card.getFirstName()).
                            map(Component::getString).orElse("Unknown");

                        builder.append(" ").append(TooltipLines.DOT).append(" ").
                            append(cardName).
                            append(" (T").append(card.getTier()).append(")").
                            append("\n");
                    });

                builder.append("\n");
            }

            List<VaultGearAttributeInstance<?>> attributes = LegacyVaultItemApi.hasTierlessDeckAttributes() ?
                LegacyVaultItemApi.deckSnapshotAttributes(deck) :
                deck.getSnapshotAttributes(greedTier);

            if (!attributes.isEmpty())
            {
                builder.append("**Attributes:**\n");

                for (VaultGearAttributeInstance instance : attributes)
                {
                    MutableComponent text = instance.getAttribute().getReader().
                        getDisplay(instance, VaultGearModifier.AffixType.PREFIX);

                    if (text != null)
                    {
                        builder.append(" ").append(TooltipLines.DOT).append(" ").
                            append(text.getString()).append("\n");
                    }
                }

                builder.append("\n");
            }

            CardTooltips.appendDeckSockets(builder, DeckTooltips.deckLines(deck, greedTier));
        });
    }


    /**
     * This method appends the sockets and what the deck's modifiers do, from the game's own deck tooltip: it ends with
     * the sockets line (which counts only the sockets cores fill, as the pack decides) and one line per modifier effect.
     * @param builder Builder that contains the description.
     * @param deckLines The deck's tooltip lines.
     */
    private static void appendDeckSockets(StringBuilder builder, List<Component> deckLines)
    {
        List<String> lines = deckLines.stream().map(Component::getString).map(String::strip).toList();
        int socketsLine = -1;

        for (int index = 0; index < lines.size(); index++)
        {
            if (lines.get(index).startsWith(SOCKETS_PREFIX))
            {
                socketsLine = index;
            }
        }

        if (socketsLine < 0)
        {
            return;
        }

        builder.append("**Sockets:** ").
            append(lines.get(socketsLine).substring(SOCKETS_PREFIX.length()).replace(" ", "")).
            append("\n");

        List<String> effects = lines.subList(socketsLine + 1, lines.size()).stream().
            filter(line -> !line.isEmpty()).
            toList();

        if (!effects.isEmpty())
        {
            builder.append("**Modifiers:**\n");
            effects.forEach(effect -> builder.append(" ").append(TooltipLines.DOT).append(" ").append(effect).append("\n"));
        }
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addCardProperty(StringBuilder builder, CardProperty<?> property, int tier)
    {
        if (property instanceof GearCardModifier<?> modifier)
        {
            GearCardModifierAccessor<?> accessor = (GearCardModifierAccessor<?>) modifier;

            CardEntry.getForTier(accessor.getValues(), tier).ifPresent((value) ->
            {
                VaultGearAttributeInstance instance = new VaultGearAttributeInstance(modifier.getAttribute(), value);
                MutableComponent name =
                    modifier.getAttribute().getReader().getDisplay(instance, VaultGearModifier.AffixType.PREFIX);
                MutableComponent ranges = new TextComponent("");

                List<Object> configs = modifier.getConfig().getPool().keySet().stream().
                    map(i -> modifier.getConfig().getConfig(i)).
                    toList();

                MutableComponent minMaxRangeCmp = uncheckedGetConfigRangeDisplay(
                    modifier.getAttribute().getGenerator(),
                    modifier.getAttribute().getReader(),
                    configs.get(0),
                    configs.get(configs.size() - 1));


                if (minMaxRangeCmp != null)
                {
                    ranges.append(minMaxRangeCmp).append(", ");
                }

                MutableComponent rangeCmp = uncheckedGetConfigRangeDisplay(
                    modifier.getAttribute().getGenerator(),
                    modifier.getAttribute().getReader(),
                    modifier.getConfig().getConfig(tier));

                CardEntry.getTier(accessor.getValues(), tier).ifPresent((t) ->
                {
                    if (rangeCmp != null)
                    {
                        ranges.append("T%s: ".formatted(t)).append(rangeCmp);
                    }
                });

                MutableComponent line = name.copy().
                    append(new TextComponent(" (").withStyle(ChatFormatting.GRAY).append(ranges).append(")"));
                TooltipLines.appendStyledLine(builder, line.getString(), line);
            });
        }
        else
        {
            List<Component> components = new ArrayList<>();
            property.addText(components, 0, TooltipFlag.Default.ADVANCED, 0f, tier);

            components.forEach(component -> TooltipLines.appendStyledLine(builder, component.getString(), component));
        }
    }


    @SuppressWarnings("unchecked")
    private static <C, T> MutableComponent uncheckedGetConfigRangeDisplay(
        ConfigurableAttributeGenerator<T, C> generator,
        VaultGearModifierReader<?> reader,
        Object tier)
    {
        VaultGearModifierReader<T> typedReader = (VaultGearModifierReader<T>) reader;

        // Cast tier to C
        C castedTier = (C) tier;

        // Pass them to the method
        return generator.getConfigRangeDisplay(typedReader, castedTier);
    }


    @SuppressWarnings("unchecked")
    private static <C, T> MutableComponent uncheckedGetConfigRangeDisplay(
        ConfigurableAttributeGenerator<T, C> generator,
        VaultGearModifierReader<?> reader,
        Object min,
        Object max)
    {
        VaultGearModifierReader<T> typedReader = (VaultGearModifierReader<T>) reader;

        // Cast min and max to C
        C castedMin = (C) min;
        C castedMax = (C) max;

        // Pass them to the method
        return generator.getConfigRangeDisplay(typedReader, castedMin, castedMax);
    }
}
