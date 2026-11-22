//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.parser;


import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import iskallia.vault.gear.tooltip.VaultGearTooltipItem;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.item.AntiqueItem;
import iskallia.vault.item.AugmentItem;
import iskallia.vault.item.CardDeckItem;
import iskallia.vault.item.CardItem;
import iskallia.vault.item.CoinPouchItem;
import iskallia.vault.item.CompanionRelicItem;
import iskallia.vault.item.DeckSocketItem;
import iskallia.vault.item.InfusedCatalystItem;
import iskallia.vault.item.InscriptionItem;
import iskallia.vault.item.ItemRespecFlask;
import iskallia.vault.item.ItemShardPouch;
import iskallia.vault.item.JewelPouchItem;
import iskallia.vault.item.ModifierScrollItem;
import iskallia.vault.item.RelicFragmentItem;
import iskallia.vault.item.SigilItem;
import iskallia.vault.item.SoulFlameItem;
import iskallia.vault.item.TitleScrollItem;
import iskallia.vault.item.VaultCatalystItem;
import iskallia.vault.item.VaultDollItem;
import iskallia.vault.item.bottle.BottleItem;
import iskallia.vault.item.crystal.CrystalData;
import iskallia.vault.item.crystal.VaultCrystalItem;
import iskallia.vault.item.gear.CharmItem;
import iskallia.vault.item.gear.EtchingItem;
import iskallia.vault.item.gear.TrinketItem;
import iskallia.vault.item.modification.GearModificationItem;
import iskallia.vault.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;


/**
 * This class allows to parse VaultHunters item tooltips to discord chat. It adds the lines client tooltip events add and
 * hands each item to the class of its family (gear, crystals, cards, bottles, companions, the smaller items) for its own
 * lines.
 */
public class VaultItemsHandler
{
    /**
     * This method generates string with tooltip of given item stack
     * @param itemJson Item Json object.
     * @param itemStack Item Stack object.
     * @param itemTag Item Compound Tag
     * @return String with a tooltip
     */
    public static String generateVaultHuntersItemTooltips(JsonObject itemJson,
        ItemStack itemStack,
        CompoundTag itemTag)
    {
        return VaultItemsHandler.generateVaultHuntersItemTooltips(itemJson, itemStack, itemTag, null);
    }


    /**
     * This method generates string with tooltip of given item stack, with the values the game works out for a player
     * (potion charges, healing and effects, a deck's greed bonus) read for the player who linked the item.
     * @param itemJson Item Json object.
     * @param itemStack Item Stack object.
     * @param itemTag Item Compound Tag
     * @param linkingPlayer the player who linked the item, or null when unknown.
     * @return String with a tooltip
     */
    public static String generateVaultHuntersItemTooltips(JsonObject itemJson,
        ItemStack itemStack,
        CompoundTag itemTag,
        ServerPlayer linkingPlayer)
    {
        String description = VaultItemsHandler.describeItem(itemJson, itemStack, itemTag, linkingPlayer);
        return description == null ? null : VaultItemsHandler.withClientEventLinesOrOwn(description, itemStack);
    }


    /**
     * The name the game shows the linking player where it differs from the item's server name (a Vault Hunters 7209
     * potion is named after the player's potion tier).
     *
     * @param itemStack the item.
     * @param linkingPlayer the player who linked it, or null when unknown.
     * @return the name, or empty when the server name is the one the game shows.
     */
    public static Optional<Component> playerTitleOf(ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        return BottleTooltips.potionTitle(itemStack, linkingPlayer);
    }


    /**
     * Only the lines client tooltip events add (Curios slot, Soul Value, tooltip config description), for an item whose
     * own lines come from elsewhere.
     *
     * @param itemStack the item.
     * @return the lines as markdown, empty when the game adds none.
     */
    public static String clientEventLines(ItemStack itemStack)
    {
        return VaultItemsHandler.withClientEventLinesOrOwn("", itemStack);
    }


    /**
     * The item's own lines with the client tooltip event lines added, or only its own lines when those cannot be built
     * (logged once per item class).
     *
     * @param description the item's own lines.
     * @param itemStack the item.
     * @return the lines as markdown.
     */
    private static String withClientEventLinesOrOwn(String description, ItemStack itemStack)
    {
        try
        {
            return VaultItemsHandler.withClientEventLines(description, itemStack);
        }
        catch (RuntimeException | LinkageError failure)
        {
            if (GAME_TEXT_FAILURES.add(itemStack.getItem().getClass().getName()))
            {
                LOGGER.warn("{} cannot add the Curios, Soul Value and tooltip config lines on the server; its picture has only its own lines",
                    itemStack.getItem().getClass().getName(), failure);
            }

            return description;
        }
    }


    /**
     * Adds the lines the game's client tooltip events put around an item's own lines, from data the server has: Vault
     * Hunters' tooltip config description and blank line, then its Soul Value line, then Curios' slot line, each
     * inserted under the name as the game does (a doll's own lines come from an event that runs between them, so its
     * description follows them).
     *
     * @param description the item's own lines.
     * @param itemStack the item.
     * @return the lines as the game shows them without Shift.
     */
    private static String withClientEventLines(String description, ItemStack itemStack)
    {
        StringBuilder configLines = new StringBuilder();
        VaultItemsHandler.appendTooltipConfigLines(configLines, itemStack);

        StringBuilder top = new StringBuilder();
        VaultItemsHandler.appendCuriosSlotLines(top, itemStack);
        VaultItemsHandler.appendSoulValueLine(top, itemStack);

        if (itemStack.getItem() instanceof VaultDollItem)
        {
            String ownLines = description.isEmpty() || description.endsWith("\n") ? description : description + "\n";
            return top + ownLines + configLines;
        }

        return top.toString() + configLines + description;
    }


    /**
     * Vault Hunters' tooltip config description of the item in grey (colour tags applied), followed by a blank line.
     *
     * @param builder Embed Builder.
     * @param itemStack the item.
     */
    private static void appendTooltipConfigLines(StringBuilder builder, ItemStack itemStack)
    {
        if (ModConfigs.TOOLTIP == null)
        {
            return;
        }

        ModConfigs.TOOLTIP.getTooltipString(itemStack.getItem()).ifPresent(text ->
        {
            for (String line : text.split("\n"))
            {
                TooltipLines.appendTextLine(builder, TextUtil.applyColorTags(line).withStyle(ChatFormatting.GRAY));
            }

            builder.append("\n");
        });
    }


    /**
     * The grey Soul Value line with the dark purple value the game shows for items a Vault Diffuser takes.
     *
     * @param builder Embed Builder.
     * @param itemStack the item.
     */
    private static void appendSoulValueLine(StringBuilder builder, ItemStack itemStack)
    {
        if (ModConfigs.VAULT_DIFFUSER == null || !ModConfigs.VAULT_DIFFUSER.contains(itemStack))
        {
            return;
        }

        Integer value = ModConfigs.VAULT_DIFFUSER.getDiffuserOutputMap().get(itemStack.getItem().getRegistryName());

        if (value != null && value > 0)
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Soul Value: ").withStyle(ChatFormatting.GRAY).
                append(new TextComponent(String.valueOf(value)).withStyle(ChatFormatting.DARK_PURPLE)));
        }
    }


    /**
     * Curios' slot line for an item that fits curio slots: the gold label and the yellow slot names, or the lines the
     * item's curio gives instead (Vault Hunters' trinkets give none and write their own).
     *
     * @param builder Embed Builder.
     * @param itemStack the item.
     */
    private static void appendCuriosSlotLines(StringBuilder builder, ItemStack itemStack)
    {
        List<String> slots = new ArrayList<>(CuriosApi.getCuriosHelper().getCurioTags(itemStack.getItem()));

        if (slots.isEmpty())
        {
            return;
        }

        MutableComponent slotsLine = new TranslatableComponent("curios.slot").append(": ").withStyle(ChatFormatting.GOLD);

        for (int index = 0; index < slots.size(); index++)
        {
            MutableComponent slot = new TranslatableComponent("curios.identifier." + slots.get(index));

            if (index < slots.size() - 1)
            {
                slot = slot.append(", ");
            }

            slotsLine.append(slot.withStyle(ChatFormatting.YELLOW));
        }

        List<Component> tagLines = List.of(slotsLine);
        List<Component> lines = CuriosApi.getCuriosHelper().getCurio(itemStack).
            map(curio -> curio.getSlotsTooltip(tagLines)).
            orElse(tagLines);

        lines.forEach(line -> TooltipLines.appendGameLine(builder, line));
    }


    /**
     * The tooltip lines the item itself writes, without the lines client tooltip events add.
     *
     * @param itemJson Item Json object.
     * @param itemStack Item Stack object.
     * @param itemTag Item Compound Tag
     * @param linkingPlayer the player who linked the item, or null when unknown.
     * @return the item's lines as markdown, or null when this handler does not describe the item.
     */
    @SuppressWarnings("deprecation")
    private static String describeItem(JsonObject itemJson,
        ItemStack itemStack,
        CompoundTag itemTag,
        ServerPlayer linkingPlayer)
    {
        try
        {
            StringBuilder builder = new StringBuilder();
            String createString;

            if (ModList.get().isLoaded("create") && (createString = CreateItemsHandler.generateCrateItemTooltips(itemStack)) != null)
            {
                return createString;
            }
            else if (itemStack.getItem() instanceof BottleItem)
            {
                BottleTooltips.handleBottleTooltip(builder, itemStack, linkingPlayer);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof EtchingItem)
            {
                GearTooltips.handleEtchingTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultGearTooltipItem)
            {
                GearTooltips.handleGearTooltip(builder, itemStack, linkingPlayer);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultDollItem)
            {
                MiscItemTooltips.handleDollTooltip(builder, itemTag);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof InscriptionItem)
            {
                GearTooltips.handleInscriptionTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultCrystalItem)
            {
                CrystalTooltips.handleVaultCrystalTooltip(builder, CrystalData.read(itemStack));
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof TrinketItem)
            {
                MiscItemTooltips.handleTrinketTooltip(builder, itemStack, itemTag);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof RelicFragmentItem relic)
            {
                CompanionTooltips.handleRelicFragmentTooltip(builder, itemStack, relic);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof AugmentItem)
            {
                CrystalTooltips.handleAugmentTooltip(builder, itemStack, linkingPlayer);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof CharmItem)
            {
                MiscItemTooltips.handleCharmTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof ItemRespecFlask)
            {
                MiscItemTooltips.handleFlaskTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof GearModificationItem)
            {
                MiscItemTooltips.handleFocusTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof TitleScrollItem)
            {
                MiscItemTooltips.handleTitleScrollTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof SoulFlameItem)
            {
                MiscItemTooltips.handleSoulFlameTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof AntiqueItem)
            {
                MiscItemTooltips.handleAntiqueTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof JewelPouchItem)
            {
                MiscItemTooltips.handleJewelPouchTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof CoinPouchItem)
            {
                MiscItemTooltips.handleCoinPouchTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof ItemShardPouch)
            {
                MiscItemTooltips.handleShardPouchTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof CardItem)
            {
                CardTooltips.handleCardTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof CardDeckItem)
            {
                CardTooltips.handleCardDeckTooltip(builder, itemStack, linkingPlayer);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof ModifierScrollItem)
            {
                MiscItemTooltips.handleModifierScrollTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof InfusedCatalystItem)
            {
                CrystalTooltips.handleInfusedCatalystTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultCatalystItem)
            {
                CrystalTooltips.handleBaseCatalystTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof CompanionRelicItem)
            {
                CompanionTooltips.handleCompanionRelicTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof SigilItem)
            {
                CrystalTooltips.handleSigilTooltip(builder, itemStack, linkingPlayer);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof DeckSocketItem)
            {
                CardTooltips.handleDeckSocketTooltip(builder, itemStack);
                return builder.toString();
            }
            else
            {
                String itemId = itemJson.has("id") ? itemJson.get("id").getAsString() : "";

                switch (itemId)
                {
                    case "the_vault:greedy_meal" ->
                        MiscItemTooltips.handleGreedyMealTooltip(builder, itemStack, linkingPlayer);
                    case "the_vault:greedy_charm" ->
                    {
                        if (!MiscItemTooltips.handleGreedyCharmTooltip(builder, itemStack, linkingPlayer))
                        {
                            return null;
                        }
                    }
                    case "the_vault:temporal_shard" ->
                    {
                        if (!MiscItemTooltips.handleTemporalShardTooltip(builder, itemStack, linkingPlayer))
                        {
                            return null;
                        }
                    }
                    case "the_vault:herald_trophy" ->
                        CompanionTooltips.handleHeraldTrophyTooltip(builder, itemStack);
                    case "the_vault:companion" ->
                        CompanionTooltips.handleCompanionTooltip(builder, itemStack);
                    case "create:attribute_filter" ->
                        MiscItemTooltips.handleAttributeFilterTooltip(builder, itemStack);
                    case "the_vault:treasure_keyring" ->
                        MiscItemTooltips.handleKeyringTooltip(builder, itemStack);
                    default ->
                    {
                        return null;
                    }
                }

                return builder.toString();
            }
        }
        catch (Exception failure)
        {
            if (GAME_TEXT_FAILURES.add(itemStack.getItem().getClass().getName()))
            {
                LOGGER.warn("Could not describe {}; its link is posted with the plain tooltip", itemStack.getItem().getClass().getName(), failure);
            }

            return null;
        }
    }


    /**
     * The logger the item family classes share, so their warnings keep this class's name.
     */
    static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Crystal part and item classes whose tooltip text failed on the server, so each is logged once.
     */
    static final Set<String> GAME_TEXT_FAILURES = ConcurrentHashMap.newKeySet();
}
