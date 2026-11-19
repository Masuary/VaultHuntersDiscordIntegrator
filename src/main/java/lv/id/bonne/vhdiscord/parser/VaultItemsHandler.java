//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.parser;


import com.google.gson.JsonObject;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import iskallia.vault.config.PlayerTitlesConfig;
import iskallia.vault.config.TrinketConfig;
import iskallia.vault.config.UniqueGearConfig;
import iskallia.vault.config.VaultItemsConfig;
import iskallia.vault.config.entry.DescriptionData;
import iskallia.vault.config.gear.VaultGearTagConfig;
import iskallia.vault.config.gear.VaultGearTierConfig;
import iskallia.vault.config.sigil.SigilConfig;
import iskallia.vault.core.card.*;
import iskallia.vault.core.card.modifier.card.GearCardModifier;
import iskallia.vault.core.card.modifier.deck.*;
import iskallia.vault.core.data.key.ThemeKey;
import iskallia.vault.core.vault.VaultRegistry;
import iskallia.vault.core.vault.influence.VaultGod;
import iskallia.vault.core.vault.modifier.VaultModifierStack;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.core.vault.objective.ParadoxObjective;
import iskallia.vault.core.world.generator.layout.ArchitectRoomEntry;
import iskallia.vault.core.world.generator.layout.DIYRoomEntry;
import iskallia.vault.dynamodel.DynamicModel;
import iskallia.vault.dynamodel.model.armor.ArmorPieceModel;
import iskallia.vault.dynamodel.model.item.PlainItemModel;
import iskallia.vault.dynamodel.registry.DynamicModelRegistry;
import iskallia.vault.gear.VaultGearState;
import iskallia.vault.gear.attribute.VaultGearAttribute;
import iskallia.vault.gear.attribute.VaultGearAttributeInstance;
import iskallia.vault.gear.attribute.VaultGearModifier;
import iskallia.vault.gear.attribute.config.ConfigurableAttributeGenerator;
import iskallia.vault.gear.charm.CharmEffect;
import iskallia.vault.gear.data.AttributeGearData;
import iskallia.vault.gear.data.VaultGearData;
import iskallia.vault.gear.item.VaultGearItem;
import iskallia.vault.gear.reader.DecimalModifierReader;
import iskallia.vault.gear.reader.VaultGearModifierReader;
import iskallia.vault.gear.tooltip.VaultGearTooltipItem;
import iskallia.vault.gear.trinket.TrinketEffect;
import iskallia.vault.init.*;
import iskallia.vault.item.*;
import iskallia.vault.item.bottle.BottleItem;
import iskallia.vault.item.crystal.CrystalData;
import iskallia.vault.item.crystal.CrystalEntry;
import iskallia.vault.item.crystal.VaultCrystalItem;
import iskallia.vault.item.crystal.layout.*;
import iskallia.vault.item.crystal.layout.preset.ParadoxTemplatePreset;
import iskallia.vault.item.crystal.modifiers.CrystalModifiers;
import iskallia.vault.item.crystal.modifiers.ParadoxCrystalModifiers;
import iskallia.vault.item.crystal.modifiers.PersonalCrystalModifiers;
import iskallia.vault.item.crystal.objective.*;
import iskallia.vault.item.crystal.properties.CapacityCrystalProperties;
import iskallia.vault.item.crystal.properties.CrystalProperties;
import iskallia.vault.item.crystal.properties.InstabilityCrystalProperties;
import iskallia.vault.item.crystal.theme.CrystalTheme;
import iskallia.vault.item.crystal.theme.NullCrystalTheme;
import iskallia.vault.item.crystal.theme.PoolCrystalTheme;
import iskallia.vault.item.crystal.theme.ValueCrystalTheme;
import iskallia.vault.item.crystal.time.CrystalTime;
import iskallia.vault.item.crystal.time.NullCrystalTime;
import iskallia.vault.item.crystal.time.PoolCrystalTime;
import iskallia.vault.item.crystal.time.ValueCrystalTime;
import iskallia.vault.item.data.InscriptionData;
import iskallia.vault.item.gear.CharmItem;
import iskallia.vault.config.gear.VaultEtchingConfig;
import iskallia.vault.core.vault.modifier.registry.VaultModifierRegistry;
import iskallia.vault.gear.etching.EtchingHelper;
import iskallia.vault.item.CoinPouchItem;
import iskallia.vault.item.InfusedCatalystItem;
import iskallia.vault.item.ItemShardPouch;
import iskallia.vault.item.VaultCatalystItem;
import iskallia.vault.item.gear.EtchingItem;
import iskallia.vault.item.gear.GreedTrinketItem;
import iskallia.vault.item.gear.IVaultUsesItem;
import iskallia.vault.item.gear.TrinketItem;
import iskallia.vault.item.gear.VaultCharmItem;
import iskallia.vault.item.gear.VaultNecklaceItem;
import iskallia.vault.item.gear.VaultUsesHelper;
import iskallia.vault.item.gear.VoidStoneItem;
import iskallia.vault.item.modification.GearModificationItem;
import iskallia.vault.item.modification.ReforgeTagModificationFocus;
import iskallia.vault.item.CompanionRelicItem;
import iskallia.vault.item.DeckSocketItem;
import iskallia.vault.item.SigilItem;
import iskallia.vault.item.tool.JewelItem;
import iskallia.vault.item.tool.PaxelItem;
import iskallia.vault.util.MiscUtils;
import iskallia.vault.util.TextUtil;
import iskallia.vault.util.Throuple;
import iskallia.vault.world.VaultDifficulty;
import lv.id.bonne.vhdiscord.vaulthunters.mixin.CardEntryAccessor;
import lv.id.bonne.vhdiscord.vaulthunters.mixin.GearCardModifierAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;


/**
 * This class allows to parse VaultHunters item tooltips to discord chat.
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
        String description = VaultItemsHandler.describeItem(itemJson, itemStack, itemTag);
        return description == null ? null : VaultItemsHandler.withClientEventLinesOrOwn(description, itemStack);
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
                VaultItemsHandler.appendTextLine(builder, TextUtil.applyColorTags(line).withStyle(ChatFormatting.GRAY));
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
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Soul Value: ").withStyle(ChatFormatting.GRAY).
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

        lines.forEach(line -> VaultItemsHandler.appendGameLine(builder, line));
    }


    /**
     * The tooltip lines the item itself writes, without the lines client tooltip events add.
     *
     * @param itemJson Item Json object.
     * @param itemStack Item Stack object.
     * @param itemTag Item Compound Tag
     * @return the item's lines as markdown, or null when this handler does not describe the item.
     */
    @SuppressWarnings("deprecation")
    private static String describeItem(JsonObject itemJson,
        ItemStack itemStack,
        CompoundTag itemTag)
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
                VaultItemsHandler.handleBottleTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof EtchingItem)
            {
                VaultItemsHandler.handleEtchingTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultGearTooltipItem)
            {
                VaultItemsHandler.handleGearTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultDollItem)
            {
                VaultItemsHandler.handleDollTooltip(builder, itemTag);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof InscriptionItem)
            {
                VaultItemsHandler.handleInscriptionTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultCrystalItem)
            {
                VaultItemsHandler.handleVaultCrystalTooltip(builder, CrystalData.read(itemStack));
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof TrinketItem)
            {
                VaultItemsHandler.handleTrinketTooltip(builder, itemStack, itemTag);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof RelicFragmentItem relic)
            {
                VaultItemsHandler.handleRelicFragmentTooltip(builder, itemStack, relic);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof AugmentItem)
            {
                VaultItemsHandler.handleAugmentTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof CharmItem)
            {
                VaultItemsHandler.handleCharmTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof ItemRespecFlask)
            {
                VaultItemsHandler.handleFlaskTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof GearModificationItem)
            {
                VaultItemsHandler.handleFocusTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof TitleScrollItem)
            {
                VaultItemsHandler.handleTitleScrollTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof SoulFlameItem)
            {
                VaultItemsHandler.handleSoulFlameTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof AntiqueItem)
            {
                VaultItemsHandler.handleAntiqueTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof JewelPouchItem)
            {
                VaultItemsHandler.handleJewelPouchTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof CoinPouchItem)
            {
                VaultItemsHandler.handleCoinPouchTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof ItemShardPouch)
            {
                VaultItemsHandler.handleShardPouchTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof CardItem)
            {
                VaultItemsHandler.handleCardTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof CardDeckItem)
            {
                VaultItemsHandler.handleCardDeckTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof ModifierScrollItem)
            {
                VaultItemsHandler.handleModifierScrollTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof InfusedCatalystItem)
            {
                VaultItemsHandler.handleInfusedCatalystTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultCatalystItem)
            {
                VaultItemsHandler.handleBaseCatalystTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof CompanionRelicItem)
            {
                VaultItemsHandler.handleCompanionRelicTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof SigilItem)
            {
                VaultItemsHandler.handleSigilTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof DeckSocketItem)
            {
                VaultItemsHandler.handleDeckSocketTooltip(builder, itemStack);
                return builder.toString();
            }
            else
            {
                String itemId = itemJson.has("id") ? itemJson.get("id").getAsString() : "";

                switch (itemId)
                {
                    case "the_vault:greedy_meal" ->
                        VaultItemsHandler.handleGreedyMealTooltip(builder, itemStack);
                    case "the_vault:herald_trophy" ->
                        VaultItemsHandler.handleHeraldTrophyTooltip(builder, itemStack);
                    case "the_vault:companion" ->
                        VaultItemsHandler.handleCompanionTooltip(builder, itemStack);
                    case "create:attribute_filter" ->
                        VaultItemsHandler.handleAttributeFilterTooltip(builder, itemStack);
                    case "the_vault:treasure_keyring" ->
                        VaultItemsHandler.handleKeyringTooltip(builder, itemStack);
                    default ->
                    {
                        return null;
                    }
                }

                return builder.toString();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();

            // If I fail, then return nothing.
            return null;
        }
    }


    /**
     * This method parses gear tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Gear ItemStack.
     */
    public static void handleGearTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultGearData data = VaultGearData.read(itemStack);
        VaultGearState state = data.getState();
        boolean jewel = itemStack.getItem() instanceof JewelItem;

        if (itemStack.getItem() instanceof VaultCharmItem)
        {
            VaultItemsHandler.handleGodCharmTooltip(builder, itemStack, data, state);
            return;
        }

        if (itemStack.getItem() instanceof VoidStoneItem && ModList.get().isLoaded(WOLDS_VAULTS_MOD_ID))
        {
            return;
        }

        VaultItemsHandler.appendGearUsesLines(builder, itemStack, state);

        // Add unique item lore
        data.getFirstValue(ModGearAttributes.UNIQUE_ITEM_KEY).
            flatMap(ModConfigs.UNIQUE_GEAR::getEntry).
            map(UniqueGearConfig.Entry::getLore).
            filter(lore -> !lore.isBlank()).
            ifPresent(lore -> builder.append("*").append(TextUtil.applyColorTags(lore).getString().strip()).append("*\n"));

        // Add gear Level
        builder.append("**Level:** ").append(data.getItemLevel()).append("\n");
        TooltipColorHints.recordValue("Level", LEVEL_COLOUR);

        // Add crafter name
        data.getFirstValue(ModGearAttributes.CRAFTED_BY).ifPresent(crafter ->
        {
            builder.append("**Crafted by:** ").append(crafter).append("\n");
            TooltipColorHints.recordValue("Crafted by", CRAFTER_COLOUR);
        });

        // Add Rarity
        switch (state)
        {
            case UNIDENTIFIED ->
            {
                Objects.requireNonNull(ModConfigs.VAULT_GEAR_TYPE_CONFIG);
                data.getFirstValue(ModGearAttributes.GEAR_ROLL_TYPE).
                    flatMap(ModConfigs.VAULT_GEAR_TYPE_CONFIG::getRollPool).
                    ifPresent(pool ->
                    {
                        builder.append("**Roll:** ").append(pool.getName()).append("\n");
                        TooltipColorHints.recordValue("Roll", pool.getColor());
                    });
            }
            case IDENTIFIED ->
            {
                if (jewel)
                {
                    break;
                }

                builder.append("**Rarity:** ").append(data.getRarity().getDisplayName().getString()).append("\n");
                TooltipColorHints.recordValue("Rarity", data.getRarity().getDisplayName());
            }
        }

        if (state == VaultGearState.IDENTIFIED)
        {
            // Add Slot name
            if (itemStack.getItem() instanceof VaultGearItem gearItem)
            {
                EquipmentSlot slot = gearItem.getGearType(itemStack).getEquipmentSlot();

                if (slot != null)
                {
                    builder.append("**Slot:** ").
                        append(new TranslatableComponent("the_vault.equipment." + slot.getName()).getString()).
                        append("\n");
                    TooltipColorHints.recordValue("Slot", SLOT_COLOUR);
                }
            }

            // Add Repair text.
            int usedRepairs = data.getUsedRepairSlots();
            int totalRepairs = data.getRepairSlots();

            if (totalRepairs > 0 && !jewel)
            {
                builder.append(VaultItemsHandler.createRepairText(usedRepairs, totalRepairs)).append("\n");
            }

            // Add Durability
            if (!jewel && VaultGearItem.of(itemStack).isBroken(itemStack))
            {
                builder.append("**Durability:** ").append("BROKEN").append("\n");
                TooltipColorHints.recordValue("Durability", BROKEN_COLOUR);
            }
            else if (!jewel && itemStack.isDamageableItem() && itemStack.getMaxDamage() > 0)
            {
                builder.append("**Durability:** ").append("%d/%d".formatted(
                    itemStack.getMaxDamage() - itemStack.getDamageValue(),
                        itemStack.getMaxDamage())).
                    append("\n");
                TooltipColorHints.recordValue("Durability", DURABILITY_COLOUR);
            }

            // Add Model
            data.getFirstValue(ModGearAttributes.GEAR_MODEL).
                flatMap(modelId -> ModDynamicModels.REGISTRIES.getModel(itemStack.getItem(), modelId)).
                ifPresent(gearModel -> {
                    Item pattern = itemStack.getItem();
                    if (pattern instanceof VaultGearItem)
                    {
                        String name = gearModel.getDisplayName();

                        if (gearModel instanceof ArmorPieceModel modelPiece)
                        {
                            name = modelPiece.getArmorModel().getDisplayName();
                        }

                        builder.append("**Model:** ").append(name).append("\n");
                        TooltipColorHints.recordValue("Model",
                            ModConfigs.GEAR_MODEL_ROLL_RARITIES.getRarityOf(itemStack, gearModel.getId()).getColor().getValue());
                    }
                });

            // Add Etching info if present
            data.getFirstValue(ModGearAttributes.ETCHING).ifPresent(etchingId -> {
                VaultEtchingConfig.EtchingEntry etchingEntry = ModConfigs.ETCHINGS.getEtchingConfig(etchingId);
                if (etchingEntry != null)
                {
                    String etchingDesc = VaultItemsHandler.etchingDescription(etchingEntry.getDescription(), itemStack);
                    etchingDesc = etchingDesc.replaceAll("<[^>]+>", "");
                    builder.append("**Etching:** ").append(etchingEntry.getName()).append("\n");
                    builder.append(etchingDesc).append("\n");
                }
            });

            // Add Implicits
            List<VaultGearModifier<?>> implicits = data.getModifiers(VaultGearModifier.AffixType.IMPLICIT);

            if (!implicits.isEmpty())
            {
                VaultItemsHandler.addAffixList(builder, data, VaultGearModifier.AffixType.IMPLICIT, itemStack);
                builder.append("\n");
            }

            int maxPrefixes = data.getFirstValue(ModGearAttributes.PREFIXES).orElse(0);
            List<VaultGearModifier<?>> prefixes = data.getModifiers(VaultGearModifier.AffixType.PREFIX);

            if (maxPrefixes > 0 || !prefixes.isEmpty())
            {
                VaultItemsHandler.addAffixList(builder, data, VaultGearModifier.AffixType.PREFIX, itemStack);
                builder.append("\n");
            }

            int maxSuffixes = data.getFirstValue(ModGearAttributes.SUFFIXES).orElse(0);
            List<VaultGearModifier<?>> suffixes = data.getModifiers(VaultGearModifier.AffixType.SUFFIX);

            if (maxSuffixes > 0 || !suffixes.isEmpty())
            {
                VaultItemsHandler.addAffixList(builder, data, VaultGearModifier.AffixType.SUFFIX, itemStack);
            }
        }

        if (data.isImbued())
        {
            builder.append("\n").append("**Imbued**");
            TooltipColorHints.record("Imbued", new TextComponent("Imbued").withStyle(Style.EMPTY.withColor(IMBUED_COLOUR)));
        }

        if (data.isSealed())
        {
            builder.append("\n").append("**Sealed**");
            TooltipColorHints.record("Sealed", new TextComponent("Sealed").withStyle(Style.EMPTY.withColor(SEALED_COLOUR)));
        }

        if (data.isCorrupted())
        {
            builder.append("\n").append("**Corrupted**").append(" (Unmodifiable Item)");
            TooltipColorHints.record("Corrupted (Unmodifiable Item)",
                new TextComponent("Corrupted").withStyle(Style.EMPTY.withColor(CORRUPTED_COLOUR)).
                    append(new TextComponent(" (Unmodifiable Item)").withStyle(ChatFormatting.GRAY)));
        }
    }


    /**
     * Writes a god charm's tooltip as the game draws it without Shift: the uses left, its reputation and god, then the
     * roll of an unidentified charm or the modifiers of an identified one (on Wold's Vaults the implicits first, without
     * a header). The game hides the uses from players with the matching prestige power, which is not known here.
     *
     * @param builder Embed Builder.
     * @param itemStack the god charm.
     * @param data the charm's gear data.
     * @param state the charm's identification state.
     */
    private static void handleGodCharmTooltip(StringBuilder builder, ItemStack itemStack, VaultGearData data, VaultGearState state)
    {
        if (state == VaultGearState.IDENTIFIED)
        {
            int remaining = Math.max(VaultUsesHelper.getUses(itemStack) - VaultUsesHelper.getUsedVaults(itemStack).size(), 0);
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Uses: ").withStyle(ChatFormatting.GRAY).
                append(new TextComponent(String.valueOf(remaining)).withStyle(ChatFormatting.WHITE)));
        }

        VaultItemsHandler.appendGameLine(builder, new TextComponent("Reputation: ").withStyle(ChatFormatting.GRAY).
            append(new TextComponent(String.valueOf(VaultCharmItem.getGodReputation(itemStack))).withStyle(ChatFormatting.WHITE)));
        VaultCharmItem.getGod(itemStack).ifPresent(god ->
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Vault God: ").
                append(new TextComponent(god.getName()).withStyle(god.getChatColor()))));

        if (state == VaultGearState.UNIDENTIFIED)
        {
            data.getFirstValue(ModGearAttributes.GEAR_ROLL_TYPE).
                flatMap(ModConfigs.VAULT_GEAR_TYPE_CONFIG::getRollPool).
                ifPresent(pool ->
                {
                    builder.append("**Roll:** ").append(pool.getName()).append("\n");
                    TooltipColorHints.recordValue("Roll", pool.getColor());
                });
            return;
        }

        if (ModList.get().isLoaded(WOLDS_VAULTS_MOD_ID))
        {
            data.getModifiers(VaultGearModifier.AffixType.IMPLICIT).forEach(modifier ->
                VaultItemsHandler.addAffix(builder, modifier, data, VaultGearModifier.AffixType.IMPLICIT, itemStack));
        }

        data.getModifiers(VaultGearModifier.AffixType.PREFIX).forEach(modifier ->
            VaultItemsHandler.addAffix(builder, modifier, data, VaultGearModifier.AffixType.PREFIX, itemStack));
    }


    /**
     * The lines necklaces and void stones show above their gear tooltip without Shift: the uses left and a necklace's
     * identify hint.
     *
     * @param builder Embed Builder.
     * @param itemStack the gear item.
     * @param state the item's identification state.
     */
    private static void appendGearUsesLines(StringBuilder builder, ItemStack itemStack, VaultGearState state)
    {
        boolean identified = state == VaultGearState.IDENTIFIED;

        if (itemStack.getItem() instanceof VaultNecklaceItem necklace)
        {
            if (identified)
            {
                VaultItemsHandler.appendGameLine(builder, VaultItemsHandler.usesLine(necklace, itemStack));
            }
            else
            {
                VaultItemsHandler.appendStyledLine(builder,
                    "Right click to identify",
                    new TextComponent("Right click to identify").withStyle(ChatFormatting.GRAY));
            }
        }
        else if (itemStack.getItem() instanceof VoidStoneItem voidStone && identified)
        {
            VaultItemsHandler.appendGameLine(builder, VaultItemsHandler.usesLine(voidStone, itemStack));
        }
    }


    /**
     * The plain uses line of necklaces and void stones.
     *
     * @param usesItem the item type.
     * @param itemStack the item.
     * @return the line.
     */
    private static Component usesLine(IVaultUsesItem usesItem, ItemStack itemStack)
    {
        int remaining = Math.max(usesItem.getUses(itemStack) - usesItem.getUsedVaults(itemStack).size(), 0);
        return new TextComponent("Uses: ").append(new TextComponent(String.valueOf(remaining)));
    }


    /**
     * Writes a potion bottle's tooltip as the game draws it, without the recharge progress line the game counts for the
     * viewer.
     * @param builder Embed Builder.
     * @param itemStack Bottle Item.
     */
    public static void handleBottleTooltip(StringBuilder builder, ItemStack itemStack)
    {
        if (ModConfigs.POTION == null)
        {
            // Potions are not defined.
            return;
        }

        BottleItem.getType(itemStack).ifPresent(type ->
        {
            BottleItem.getRecharge(itemStack).ifPresent(recharge ->
            {
                switch (recharge)
                {
                    case TIME -> VaultItemsHandler.appendTextLine(builder,
                        new TextComponent("Passively restores charges while inside a vault").withStyle(ChatFormatting.GRAY));
                    case MOBS -> VaultItemsHandler.appendTextLine(builder,
                        new TextComponent("Kill vault mobs to restore charges").withStyle(ChatFormatting.GRAY));
                    case CHESTS -> VaultItemsHandler.appendTextLine(builder,
                        new TextComponent("Loot vault chests to restore charges").withStyle(ChatFormatting.GRAY));
                }
            });

            VaultItemsHandler.appendTextLine(builder, new TextComponent("Consume a ").
                append(new TextComponent("charge").withStyle(ChatFormatting.WHITE)).
                append(new TextComponent(" to")).
                withStyle(Style.EMPTY.withColor(BOTTLE_TEXT_COLOUR)));
            VaultItemsHandler.appendTextLine(builder, new TextComponent(" " + DOT + " Heal ").
                append(new TextComponent(String.valueOf(ModConfigs.POTION.getPotion(type).getHealing()))).
                append(new TextComponent(" Hit Points")).
                withStyle(ChatFormatting.GREEN));

            BottleItem.getEffect(itemStack).ifPresent(effect ->
                VaultItemsHandler.appendTextLine(builder, new TextComponent(" " + DOT + " ").append(effect.getTooltip())));
        });
    }


    /**
     * Writes a vault doll's tooltip as the game draws it for a player outside the doll's vault.
     * @param builder Embed Builder.
     * @param itemTag Vault Doll Item Tag.
     */
    public static void handleDollTooltip(StringBuilder builder, CompoundTag itemTag)
    {
        VaultDollItem.getPlayerGameProfile(itemTag).ifPresent(profile ->
        {
            VaultItemsConfig.VaultDoll config = ModConfigs.VAULT_ITEMS.VAULT_DOLL;

            VaultItemsHandler.appendGameLine(builder, new TranslatableComponent("tooltip.the_vault.doll_owner",
                new TextComponent(profile.getName()).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
            VaultItemsHandler.appendGameLine(builder, VaultItemsHandler.dollEfficiencyLine("tooltip.the_vault.doll_loot_efficiency",
                itemTag.getFloat("lootPercent"),
                config.lootPercentageMin,
                config.lootPercentageMax));
            VaultItemsHandler.appendGameLine(builder, VaultItemsHandler.dollEfficiencyLine("tooltip.the_vault.doll_experience_efficiency",
                itemTag.getFloat("xpPercent"),
                config.xpPercentageMin,
                config.xpPercentageMax));

            if (itemTag.contains("vaultUUID"))
            {
                VaultItemsHandler.appendGameLine(builder, new TranslatableComponent("tooltip.the_vault.doll_status.completed_by",
                    itemTag.getString("completedBy"),
                    itemTag.getInt("vaultLevel")).withStyle(ChatFormatting.GRAY));
                VaultItemsHandler.appendGameLine(builder,
                    new TranslatableComponent("tooltip.the_vault.doll_status.completed").withStyle(ChatFormatting.GREEN));
            }
            else
            {
                VaultItemsHandler.appendGameLine(builder,
                    new TranslatableComponent("tooltip.the_vault.doll_status.ready").withStyle(ChatFormatting.WHITE));
            }
        });
    }


    /**
     * A doll's loot or experience efficiency line: the percentage red in the lowest third of its configured range, gold
     * in the middle third and green in the top third.
     *
     * @param translationKey the line's translation key.
     * @param percent the efficiency, 1 being 100%.
     * @param minimum the lowest efficiency a doll can roll.
     * @param maximum the highest efficiency a doll can roll.
     * @return the line.
     */
    private static Component dollEfficiencyLine(String translationKey, float percent, float minimum, float maximum)
    {
        float thirdOfRange = (maximum - minimum) / 3.0F;
        ChatFormatting colour = percent < minimum + thirdOfRange ? ChatFormatting.RED :
            percent < maximum - thirdOfRange ? ChatFormatting.GOLD : ChatFormatting.GREEN;

        return new TranslatableComponent(translationKey,
            new TextComponent(String.format("%d", (int) (percent * 100.0F)) + "%").withStyle(colour)).
            withStyle(ChatFormatting.GRAY);
    }


    /**
     * This method parses paxel item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Paxel Item Stack.
     * @param paxelItem Vault Paxel Item.
     */
    public static void handlePaxelTooltip(StringBuilder builder, ItemStack itemStack, PaxelItem paxelItem)
    {
        int durability = paxelItem.getMaxDamage(itemStack);
        float miningSpeed = PaxelItem.getUsableStat(itemStack, PaxelItem.Stat.MINING_SPEED);
        float reach = PaxelItem.getUsableStat(itemStack, PaxelItem.Stat.REACH);
        float copiously = PaxelItem.getUsableStat(itemStack, PaxelItem.Stat.COPIOUSLY);

        // Generic information.

        builder.append("**D:** ").append(FORMAT.format(durability));

        if (reach > 0)
        {
            builder.append(" **R:** ").append(FORMAT.format(reach));
        }

        builder.append(" **S:** ").append(FORMAT.format(miningSpeed));

        if (copiously > 0)
        {
            builder.append(" **C:** ").append(FORMAT.format(copiously)).append("%");
        }

        builder.append("\n");

        // Perks

        List<PaxelItem.Perk> perks = PaxelItem.getPerks(itemStack);

        if (perks.size() > 0)
        {
            builder.append("**Perks:**");
            perks.forEach(perk -> {
                builder.append("\n");
                builder.append("  ").append(perk.getSerializedName());
            });

            builder.append("\n");
        }

        // Main information

        int level = PaxelItem.getPaxelLevel(itemStack);
        builder.append("**Level:** ").append(level).append("\n");

        builder.append(VaultItemsHandler.createRepairText(
            PaxelItem.getUsedRepairSlots(itemStack),
            PaxelItem.getMaxRepairSlots(itemStack))).
            append("\n");

        int sockets = PaxelItem.getSockets(itemStack);

        if (sockets != 0)
        {
            builder.append("**Sockets:** ").
                append(VaultItemsHandler.createDots(sockets, EMPTY_CIRCLE)).
                append("\n");
        }

        builder.append("\n");

        PaxelItem.Stat[] stats = PaxelItem.Stat.values();

        for (int index = 0; index < stats.length; ++index)
        {
            PaxelItem.Stat stat = stats[index];

            float value = PaxelItem.getStatUpgrade(itemStack, stat);

            if (value != 0.0F)
            {
                builder.append("**").append(stat.getReadableName()).append("**");
                builder.append(value > 0.0F ? " +" : " ");
                builder.append(ModConfigs.PAXEL_CONFIGS.getUpgrade(stat).formatValue(value));
                builder.append("\n");
            }
        }
    }


    /**
     * This method parses Etching item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Etching Item Stack.
     */
    public static void handleEtchingTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultGearData data = VaultGearData.read(itemStack);

        if (data.getState() != VaultGearState.IDENTIFIED)
        {
            return;
        }

        ResourceLocation etchingId = data.getAttributes(ModGearAttributes.ETCHING)
            .findFirst()
            .map(VaultGearAttributeInstance::getValue)
            .orElse(null);

        if (etchingId == null)
        {
            VaultItemsHandler.appendStyledLine(builder,
                "No Etching Applied",
                new TextComponent("No Etching Applied").withStyle(ChatFormatting.GRAY));
            return;
        }

        VaultEtchingConfig.EtchingEntry entry = ModConfigs.ETCHINGS.getEtchingConfig(etchingId);

        if (entry == null)
        {
            VaultItemsHandler.appendStyledLine(builder,
                "Unknown Etching",
                new TextComponent("Unknown Etching").withStyle(ChatFormatting.RED));
            return;
        }

        VaultItemsHandler.greedTierOf(data).ifPresent(greedTier ->
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Greed Tier: ").
                append(new TextComponent(String.valueOf(greedTier)).withStyle(Style.EMPTY.withColor(LEVEL_COLOUR)))));

        Component description = TextUtil.applyColorTags(VaultItemsHandler.etchingDescription(entry.getDescription(), itemStack));
        VaultItemsHandler.appendStyledLine(builder, description.getString(), description);
        builder.append("\n");

        VaultItemsHandler.appendGameLine(builder, new TextComponent("Type: ").withStyle(ChatFormatting.GRAY).
            append(new TextComponent(String.join(", ", entry.getTypeGroups())).withStyle(ChatFormatting.WHITE)));
    }


    /**
     * An etching's description with its values filled in by Vault Hunters' own formatter. Effect names are filled in
     * first: the game translates them with the client-only I18n, which cannot load on a server.
     *
     * @param description the etching's description template.
     * @param itemStack the etching or the gear carrying it.
     * @return the description.
     */
    private static String etchingDescription(String description, ItemStack itemStack)
    {
        Optional<?> value = EtchingHelper.getEtching(itemStack);
        Map<String, Object> values = new HashMap<>();
        value.ifPresent(present ->
        {
            if (present instanceof Pair<?, ?> pair)
            {
                values.put("valueA", pair.getFirst());
                values.put("valueB", pair.getSecond());
            }
            else if (present instanceof Throuple<?, ?, ?> throuple)
            {
                values.put("valueA", throuple.getFirst());
                values.put("valueB", throuple.getSecond());
                values.put("valueC", throuple.getThird());
            }
            else
            {
                values.put("value", present);
            }
        });

        Matcher placeholder = ETCHING_EFFECT_PLACEHOLDER.matcher(description);
        StringBuilder prepared = new StringBuilder();

        while (placeholder.find())
        {
            Object effectId = values.get(placeholder.group(1));
            MobEffect effect = effectId == null ? null :
                ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(effectId.toString()));
            String replacement = effect == null ? placeholder.group() : new TranslatableComponent(effect.getDescriptionId()).getString();
            placeholder.appendReplacement(prepared, Matcher.quoteReplacement(replacement));
        }

        placeholder.appendTail(prepared);
        return EtchingHelper.formatDescription(prepared.toString(), value);
    }


    /**
     * The greed tier an etching needs, read by its registry name: the attribute is newer than the Vault Hunters
     * version this mod compiles against.
     *
     * @param data the etching's gear data.
     * @return the tier, or empty when the etching has none.
     */
    private static Optional<Integer> greedTierOf(VaultGearData data)
    {
        return data.getAttributes().stream().
            filter(instance -> GREED_TIER_ATTRIBUTE.equals(instance.getAttribute().getRegistryName())).
            map(VaultGearAttributeInstance::getValue).
            filter(Integer.class::isInstance).
            map(Integer.class::cast).
            findFirst();
    }


    /**
     * This method parses Vault Inscription item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Rune Item Stack.
     */
    public static void handleInscriptionTooltip(StringBuilder builder, ItemStack itemStack)
    {
        InscriptionData data = InscriptionData.from(itemStack);
        CompoundTag compoundTag = data.serializeNBT();

        if (compoundTag.contains("completion"))
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Completion: ").
                append(new TextComponent(Math.round(compoundTag.getFloat("completion") * 100.0F) + "%").
                    withStyle(Style.EMPTY.withColor(INSCRIPTION_COMPLETION_COLOUR))));
        }

        if (data.getTime() != null)
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Time: ").
                append(new TextComponent(VaultItemsHandler.formatTimeString(data.getTime())).withStyle(ChatFormatting.GRAY)));
        }

        if (compoundTag.contains("instability"))
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Instability: ").
                append(new TextComponent("%.1f%%".formatted(compoundTag.getFloat("instability") * 100.0F)).
                    withStyle(ChatFormatting.RED)));
        }

        if (!data.isSuper())
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Size: ").
                append(new TextComponent(String.valueOf(data.getSize())).withStyle(ChatFormatting.RED)));
        }

        for (InscriptionData.Entry entry : data.getEntries())
        {
            String roomStr = entry.count > 1 ? "Rooms" : "Room";
            Component line = new TextComponent(" " + DOT + " ").withStyle(ChatFormatting.GRAY).
                append(new TextComponent(String.valueOf(entry.count)).withStyle(ChatFormatting.GRAY)).
                append(" ").
                append(entry.toRoomEntry().getName()).
                append(new TextComponent(" " + roomStr).withStyle(ChatFormatting.GRAY));
            VaultItemsHandler.appendStyledLine(builder, line.getString(), line);
        }

        for (ResourceLocation modifierId : data.getModifiers())
        {
            VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
            {
                TextColor textColor = vaultModifier.getDisplayTextColor();
                Component line = new TextComponent(" " + DOT + " " + vaultModifier.getDisplayName()).
                    withStyle(textColor == null ? Style.EMPTY : Style.EMPTY.withColor(textColor));
                VaultItemsHandler.appendStyledLine(builder, line.getString(), line);
            });
        }

        if (data.getEchoCost() != null)
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Echo Cost: ").
                append(new TextComponent(String.valueOf(data.getEchoCost())).withStyle(ChatFormatting.DARK_PURPLE)));
        }
    }


    /**
     * This method parses Vault Crystal item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param crystalData Vault Crystal Data.
     */
    public static void handleVaultCrystalTooltip(StringBuilder builder, CrystalData crystalData)
    {
        List<Component> gameLines = VaultItemsHandler.crystalGameLines(crystalData);

        if (gameLines == null)
        {
            VaultItemsHandler.appendCrystalSummary(builder, crystalData);
        }
        else
        {
            gameLines.forEach(line -> VaultItemsHandler.appendGameLine(builder, line));
        }

        // Modifiers
        VaultItemsHandler.parseModifiers(builder, crystalData.getModifiers());
    }


    /**
     * The crystal's objective, theme, layout, time and properties lines exactly as Vault Hunters writes them in game, in
     * the game's order (level and capacity first). These parts build their text without client classes, unlike the
     * crystal's modifiers, hazards and sigil, so they run on a dedicated server too.
     *
     * @param crystalData Vault Crystal Data.
     * @return the lines, or null when a part could not build its text on the server.
     */
    private static List<Component> crystalGameLines(CrystalData crystalData)
    {
        int level = crystalData.getProperties().getLevel().orElse(0);
        List<Component> lines = new ArrayList<>();
        List<CrystalEntry> parts = List.of(crystalData.getObjective(),
            crystalData.getTheme(),
            crystalData.getLayout(),
            crystalData.getTime(),
            crystalData.getProperties());

        for (CrystalEntry part : parts)
        {
            try
            {
                part.addText(lines, 0, TooltipFlag.Default.NORMAL, 0.0F, level);
            }
            catch (RuntimeException | LinkageError failure)
            {
                if (GAME_TEXT_FAILURES.add(part.getClass().getName()))
                {
                    LOGGER.warn("{} cannot write its crystal tooltip on the server; crystals with it use the summary text",
                        part.getClass().getName(), failure);
                }

                return null;
            }
        }

        return lines;
    }


    /**
     * Writes one game tooltip line as markdown ({@code **Label:** value}, or the whole line bold when it has no label)
     * and notes its styled runs, so the in-game picture draws it in the game's colours; a line holding line breaks is
     * written as several lines.
     *
     * @param builder Embed Builder.
     * @param line the game's line.
     */
    private static void appendGameLine(StringBuilder builder, Component line)
    {
        String raw = line.getString();
        String text = raw.strip();
        int separator = text.indexOf(": ");

        if (raw.indexOf('\n') >= 0)
        {
            VaultItemsHandler.splitAtLineBreaks(line).forEach(part -> VaultItemsHandler.appendGameLine(builder, part));
        }
        else if (separator > 0)
        {
            VaultItemsHandler.appendStyledLine(builder,
                "**" + text.substring(0, separator) + ":** " + text.substring(separator + 2),
                line);
        }
        else if (text.isEmpty())
        {
            builder.append("\n");
        }
        else
        {
            VaultItemsHandler.appendStyledLine(builder, "**" + text + "**", line);
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
    private static void appendStyledLine(StringBuilder builder, String markdown, Component line)
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
    private static void appendTextLine(StringBuilder builder, Component line)
    {
        String text = line.getString().strip();

        if (text.isEmpty())
        {
            builder.append("\n");
        }
        else
        {
            VaultItemsHandler.appendStyledLine(builder, text, line);
        }
    }


    /**
     * Writes the lines an item's own tooltip code writes, each as {@link #appendGameLine} does; for items whose tooltip
     * Vault Hunters builds without client classes, so it runs on a dedicated server too.
     *
     * @param builder Embed Builder.
     * @param itemStack the item.
     */
    private static void appendItemGameLines(StringBuilder builder, ItemStack itemStack)
    {
        List<Component> lines = new ArrayList<>();

        try
        {
            itemStack.getItem().appendHoverText(itemStack, null, lines, TooltipFlag.Default.NORMAL);
        }
        catch (RuntimeException | LinkageError failure)
        {
            if (GAME_TEXT_FAILURES.add(itemStack.getItem().getClass().getName()))
            {
                LOGGER.warn("{} cannot write its tooltip on the server; its picture has no description",
                    itemStack.getItem().getClass().getName(), failure);
            }

            return;
        }

        lines.forEach(line -> VaultItemsHandler.appendGameLine(builder, line));
    }


    /**
     * The crystal summary this handler wrote before it used the game's own lines; kept for crystal parts that cannot
     * build their text on a server.
     *
     * @param builder Embed Builder.
     * @param crystalData Vault Crystal Data.
     */
    private static void appendCrystalSummary(StringBuilder builder, CrystalData crystalData)
    {
        // Objective
        addObjectiveName(builder, crystalData.getObjective());

        // Vault Theme
        builder.append("**Theme:** ").
            append(parseThemeName(crystalData.getTheme()));
        builder.append("\n");

        // Vault Layout
        addLayoutName(builder, crystalData.getLayout());

        // Vault Time
        String time = parseTime(crystalData.getTime());

        // Vault properties
        CrystalProperties properties = crystalData.getProperties();
        Integer level = properties.getLevel().orElse(null);
        builder.append("**Level:** ").append(level == null ? "???" : level).append("\n");

        if (crystalData.getProperties() instanceof CapacityCrystalProperties capacity)
        {
            Optional<Integer> volume = capacity.getVolume();

            if (volume.isEmpty())
            {
                builder.append("**Capacity:** ???").append("\n");
            }
            else if (volume.get() > 0)
            {
                int value = volume.get();
                int size = capacity.getSize();

                builder.append("**Capacity:** ").
                    append(Math.max(value - size, 0)).
                    append("/").
                    append(value).
                    append("\n");
            }
        }
        else if (crystalData.getProperties() instanceof InstabilityCrystalProperties instability)
        {
            if (instability.getInstability() > 0.0F)
            {
                builder.append("**Instability:** ").
                    append("%.1f%%".formatted(instability.getInstability() * 100.0F)).
                    append("\n");
            }
        }

        // Unmodifiable
        if (crystalData.getProperties().isUnmodifiable())
        {
            builder.append("**Unmodifiable**").append("\n");
        }
    }


    /**
     * This method parses VaultTrinket item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Trinket Item Stack.
     * @param itemTag The item tag data.
     */
    public static void handleTrinketTooltip(StringBuilder builder, ItemStack itemStack, CompoundTag itemTag)
    {
        if (!TrinketItem.isIdentified(itemStack))
        {
            VaultItemsHandler.appendStyledLine(builder,
                "Right click to identify",
                new TextComponent("Right click to identify").withStyle(ChatFormatting.GRAY));
            return;
        }

        int totalUses = itemTag.getInt("vaultUses");
        int used = itemTag.getList("usedVaults", 10).size();
        int remaining = Math.max(totalUses - used, 0);
        VaultItemsHandler.appendGameLine(builder, new TextComponent("Uses: ").append(String.valueOf(remaining)));

        AttributeGearData data = AttributeGearData.read(itemStack);
        data.getFirstValue(ModGearAttributes.CRAFTED_BY).ifPresent(crafter ->
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Crafted by: ").
                append(new TextComponent(crafter).withStyle(Style.EMPTY.withColor(CRAFTER_COLOUR)))));

        Optional<List<TrinketEffect<?>>> combined = VaultItemsHandler.combinedTrinketEffects(itemStack);
        List<TrinketEffect<?>> effects = combined.orElseGet(() -> itemStack.getItem() instanceof GreedTrinketItem ?
            GreedTrinketItem.getAllTrinkets(itemStack) :
            TrinketItem.getTrinket(itemStack).<List<TrinketEffect<?>>>map(List::of).orElse(List.of()));

        for (TrinketEffect<?> effect : effects)
        {
            TrinketConfig.Trinket trinket = effect.getTrinketConfig();

            if (trinket == null)
            {
                continue;
            }

            if (combined.isPresent())
            {
                VaultItemsHandler.appendStyledLine(builder,
                    "**" + trinket.getName() + "**",
                    new TextComponent(trinket.getName()).withStyle(ChatFormatting.GOLD));
            }

            for (TextComponent line : MiscUtils.splitDescriptionText(trinket.getEffectText()))
            {
                VaultItemsHandler.appendStyledLine(builder, line.getString(), line.withStyle(ChatFormatting.GRAY));
            }
        }

        Optional<String> slot = itemStack.getItem() instanceof GreedTrinketItem ?
            Optional.of("red_trinket") :
            TrinketItem.getSlotIdentifier(itemStack);

        slot.ifPresent(identifier ->
        {
            builder.append("\n");
            VaultItemsHandler.appendGameLine(builder, VaultItemsHandler.curiosSlotLine(identifier));
        });
    }


    /**
     * The effects of a Wold's Vaults Combined Trinket, which holds several; read through its own static accessor
     * because this mod does not depend on Wold's Vaults.
     *
     * @param itemStack the trinket.
     * @return the effects, or empty when the item is not a Combined Trinket.
     */
    @SuppressWarnings("unchecked")
    private static Optional<List<TrinketEffect<?>>> combinedTrinketEffects(ItemStack itemStack)
    {
        Class<?> itemClass = itemStack.getItem().getClass();

        if (!itemClass.getName().equals(COMBINED_TRINKET_CLASS))
        {
            return Optional.empty();
        }

        try
        {
            return Optional.of((List<TrinketEffect<?>>) itemClass.getMethod("getTrinkets", ItemStack.class).invoke(null, itemStack));
        }
        catch (ReflectiveOperationException failure)
        {
            throw new IllegalStateException("Wold's Vaults' CombinedTrinketItem#getTrinkets(ItemStack) is not callable", failure);
        }
    }


    /**
     * The game's Curios slot line: the label in gold, the slot name in yellow.
     *
     * @param identifier the Curios slot identifier, e.g. charm.
     * @return the line.
     */
    private static Component curiosSlotLine(String identifier)
    {
        return new TranslatableComponent("curios.slot").append(": ").withStyle(ChatFormatting.GOLD).
            append(new TranslatableComponent("curios.identifier." + identifier).withStyle(ChatFormatting.YELLOW));
    }


    /**
     * This method parses Vault Relic Fragment item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Trinket Item Stack.
     * @param relic The item.
     */
    public static void handleRelicFragmentTooltip(StringBuilder builder, ItemStack itemStack, RelicFragmentItem relic)
    {
        Optional<ResourceLocation> resourceLocation = relic.getDynamicModelId(itemStack);
        DynamicModelRegistry<PlainItemModel> fragmentRegistry = ModDynamicModels.Relics.FRAGMENT_REGISTRY;

        resourceLocation = resourceLocation.
            flatMap(fragmentRegistry::get).
            map(DynamicModel::getId).
            flatMap(ModRelics::getRelicOfFragment).
            map(ModRelics.RelicRecipe::getResultingRelic);

        fragmentRegistry = ModDynamicModels.Relics.RELIC_REGISTRY;

        resourceLocation.flatMap(fragmentRegistry::get).ifPresent(relicModel ->
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Assembles: ").
                append(new TextComponent(relicModel.getDisplayName()).withStyle(Style.EMPTY.withColor(RELIC_COLOUR))));
            builder.append("\n");
            VaultItemsHandler.appendTextLine(builder,
                new TextComponent("Craft a Relic Pedestal to assemble").withStyle(ChatFormatting.GRAY));
            VaultItemsHandler.appendTextLine(builder,
                new TextComponent("this fragment into a Relic").withStyle(ChatFormatting.GRAY));
        });
    }


    /**
     * This method parses Infused Catalyst item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Infused Catalyst Item Stack.
     */
    public static void handleInfusedCatalystTooltip(StringBuilder builder, ItemStack itemStack)
    {
        if (InfusedCatalystItem.isSuper(itemStack))
        {
            VaultItemsHandler.appendTextLine(builder,
                new TextComponent("Used to modify your Personal Vault").withStyle(ChatFormatting.GRAY));
        }
        else
        {
            InfusedCatalystItem.getSize(itemStack).ifPresentOrElse(
                size -> VaultItemsHandler.appendGameLine(builder, new TextComponent("Size: ").
                    append(new TextComponent(String.valueOf(size)).withStyle(Style.EMPTY.withColor(CATALYST_SIZE_COLOUR)))),
                () -> VaultItemsHandler.appendGameLine(builder, new TextComponent("Size: ???").withStyle(ChatFormatting.GRAY)));
        }

        for (ResourceLocation modifierId : InfusedCatalystItem.getModifiers(itemStack))
        {
            VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
                VaultItemsHandler.appendBulletModifierLine(builder, vaultModifier));
        }
    }


    /**
     * This method parses base Catalyst item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Catalyst Item Stack.
     */
    public static void handleBaseCatalystTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultItemsHandler.appendItemGameLines(builder, itemStack);
    }


    /**
     * Writes a companion relic's modifiers as the game lists them.
     *
     * @param builder Embed Builder.
     * @param itemStack the companion relic.
     */
    public static void handleCompanionRelicTooltip(StringBuilder builder, ItemStack itemStack)
    {
        for (ResourceLocation modifierId : CompanionRelicItem.getModifiers(itemStack))
        {
            VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
                VaultItemsHandler.appendBulletModifierLine(builder, vaultModifier));
        }
    }


    /**
     * A modifier line as infused catalysts and companion relics draw it: a bullet and the modifier's name, all in the
     * modifier's colour.
     *
     * @param builder Embed Builder.
     * @param vaultModifier the modifier.
     */
    private static void appendBulletModifierLine(StringBuilder builder, VaultModifier<?> vaultModifier)
    {
        VaultItemsHandler.appendTextLine(builder, new TextComponent(" " + DOT + " " + vaultModifier.getDisplayName()).
            withStyle(Style.EMPTY.withColor(vaultModifier.getDisplayTextColor())));
    }


    /**
     * Writes a sigil's tooltip as the game draws it. The game counts the crate tiers for the viewer's vault level; with
     * no viewer here, the sigil's highest level is used.
     *
     * @param builder Embed Builder.
     * @param itemStack the sigil.
     */
    public static void handleSigilTooltip(StringBuilder builder, ItemStack itemStack)
    {
        SigilItem.readSigil(itemStack).ifPresent(sigil ->
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Sigil: ").withStyle(ChatFormatting.GRAY).
                append(new TextComponent(sigil.getDisplayName()).withStyle(Style.EMPTY.withColor(sigil.getColor()))));
            VaultItemsHandler.appendTextLine(builder, new TextComponent("+").withStyle(ChatFormatting.GRAY).
                append(new TextComponent(PERCENT_FORMAT.format(sigil.getDifficulty())).withStyle(ChatFormatting.WHITE)).
                append(" Difficulty"));

            SigilConfig sigilConfig = ModConfigs.SIGIL.get(sigil.getId());

            if (sigilConfig != null)
            {
                int crateTiers = sigilConfig.getLevel(Integer.MAX_VALUE).getExtraCrateTiers();
                VaultItemsHandler.appendTextLine(builder, new TextComponent("+").withStyle(ChatFormatting.GRAY).
                    append(new TextComponent(String.valueOf(crateTiers)).withStyle(ChatFormatting.WHITE)).
                    append(" Crate Tiers"));
            }
        });
    }


    public static void handleDeckSocketTooltip(StringBuilder builder, ItemStack itemStack)
    {
        DeckSocketItem.getDeckModifier(itemStack).
            map(DeckTooltips::modifierLines).
            ifPresent(lines -> lines.forEach(line -> VaultItemsHandler.appendStyledLine(builder, line.getString(), line)));
    }


    /**
     * This method describes what a deck core does, as its tooltip says it.
     * @param modifier Deck modifier of the core.
     * @return The description, or null for a modifier without one.
     */
    public static String describeDeckModifier(DeckModifier<?> modifier)
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


    /**
     * This method adds Vault Artifact image to the discord chat.
     * @param builder Embed Builder.
     * @param itemTag Vault artifact tag.
     */
    public static void handleVaultArtifactTooltip(StringBuilder builder, CompoundTag itemTag)
    {
         int customModelData = Math.max(itemTag.getInt("CustomModelData"), 1);
         String name = "vault_artifact_" + customModelData + ".png";
         builder.append("https://bonne.id.lv/assets/img/").append(name);
    }


    /**
     * Writes an augment's tooltip as the game draws it. Wold's Vaults' Infused Augment draws its theme in a rainbow
     * built with client classes, so that line is rebuilt here; a Vault Hunters augment writes its own lines.
     *
     * @param builder Embed Builder.
     * @param itemStack the augment.
     */
    public static void handleAugmentTooltip(StringBuilder builder, ItemStack itemStack)
    {
        if (!itemStack.getItem().getClass().getName().equals(INFUSED_AUGMENT_CLASS))
        {
            VaultItemsHandler.appendItemGameLines(builder, itemStack);
            return;
        }

        AugmentItem.getTheme(itemStack).ifPresent(key ->
            ModConfigs.THEME_AUGMENT_LORE.getAugmentLore(key.getId()).ifPresentOrElse(lore ->
                {
                    VaultItemsHandler.appendGameLine(builder, new TextComponent("Theme: ").withStyle(ChatFormatting.GRAY).
                        append(VaultItemsHandler.rainbowText(lore.displayName + " | " + key.getName())));

                    for (DescriptionData description : lore.description)
                    {
                        VaultItemsHandler.appendGameLine(builder, description.getComponent());
                    }
                },
                () -> VaultItemsHandler.appendGameLine(builder, new TextComponent("Theme: ").withStyle(ChatFormatting.GRAY).
                    append(VaultItemsHandler.rainbowText(key.getName())))));
    }


    /**
     * Text with one colour per character, running once around the hue circle from a hue that moves with the clock, as
     * Wold's Vaults draws rainbow text.
     *
     * @param text the text.
     * @return the coloured text.
     */
    private static MutableComponent rainbowText(String text)
    {
        MutableComponent result = new TextComponent("");
        float baseHue = (System.currentTimeMillis() % RAINBOW_PERIOD_MILLISECONDS) / (float) RAINBOW_PERIOD_MILLISECONDS;

        for (int index = 0; index < text.length(); index++)
        {
            float hue = (baseHue + (float) index / text.length()) % 1.0F;
            int colour = Color.HSBtoRGB(hue, RAINBOW_SATURATION, RAINBOW_BRIGHTNESS) & 0xFFFFFF;
            result.append(new TextComponent(String.valueOf(text.charAt(index))).withStyle(Style.EMPTY.withColor(colour)));
        }

        return result;
    }



    /**
     * Writes a charm's tooltip as the game draws it. Wold's Vaults replaces Vault Hunters' charm tooltip with one that
     * leaves out the uses and the Curios slot.
     *
     * @param builder Embed Builder.
     * @param itemStack the charm.
     */
    @SuppressWarnings("deprecation")
    public static void handleCharmTooltip(StringBuilder builder, ItemStack itemStack)
    {
        boolean woldsLayout = ModList.get().isLoaded(WOLDS_VAULTS_MOD_ID);

        if (!woldsLayout)
        {
            int totalUses = CharmItem.getUses(itemStack);
            int used = CharmItem.getUsedVaults(itemStack).size();
            int remaining = Math.max(totalUses - used, 0);
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Uses: ").append(remaining + "/" + totalUses));
        }

        AttributeGearData data = AttributeGearData.read(itemStack);

        data.getFirstValue(ModGearAttributes.CHARM_EFFECT).ifPresent(charmEffect ->
        {
            DecimalModifierReader.Percentage<?> percentage = (DecimalModifierReader.Percentage<?>)
                ((CharmEffect.Config<?>) charmEffect.getCharmConfig().getConfig()).getAttribute().getReader();
            String effectText = Math.round(CharmItem.getValue(itemStack) * 100.0F) + "% " + percentage.getModifierName();

            VaultItemsHandler.appendStyledLine(builder,
                effectText,
                new TextComponent(effectText).withStyle(Style.EMPTY.withColor(percentage.getRgbColor())));
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Size: ").append(String.valueOf(10)));
            builder.append("\n");
        });

        if (!woldsLayout)
        {
            VaultItemsHandler.appendGameLine(builder, VaultItemsHandler.curiosSlotLine("charm"));
        }
    }


    /**
     * Writes a respec flask's tooltip as the game draws it.
     *
     * @param builder Embed Builder.
     * @param itemStack the flask.
     */
    public static void handleFlaskTooltip(StringBuilder builder, ItemStack itemStack)
    {
        String abilityId = ItemRespecFlask.getAbility(itemStack);

        if (abilityId != null)
        {
            ModConfigs.ABILITIES.getAbilityById(abilityId).ifPresent(ability ->
            {
                builder.append("\n");
                VaultItemsHandler.appendTextLine(builder, new TextComponent("Use to remove selected specialization"));
                VaultItemsHandler.appendTextLine(builder, new TextComponent("of ability ").
                    append(new TextComponent(ability.getName()).withStyle(ChatFormatting.GOLD)));
            });
        }
    }


    /**
     * Writes a focus's tooltip as the game draws it. A plain focus writes its own lines; the tagged reforge focus builds
     * them with client classes, so they are rebuilt here, without the modifier list the game shows only on Shift.
     *
     * @param builder Embed Builder.
     * @param itemStack the focus.
     */
    public static void handleFocusTooltip(StringBuilder builder, ItemStack itemStack)
    {
        if (!(itemStack.getItem() instanceof ReforgeTagModificationFocus))
        {
            VaultItemsHandler.appendItemGameLines(builder, itemStack);
            return;
        }

        if (!ModConfigs.isInitialized())
        {
            return;
        }

        VaultItemsHandler.appendGameLine(builder,
            new TranslatableComponent("the_vault.gear_modification.information").withStyle(ChatFormatting.GRAY));
        VaultGearTagConfig.ModTagGroup group = ReforgeTagModificationFocus.getModifierTag(itemStack);

        if (group != null)
        {
            if (group.getAffixType() != null)
            {
                VaultItemsHandler.appendGameLine(builder,
                    new TextComponent("Add a new " + group.getJoinedAffix() + ", guaranteeing ").withStyle(ChatFormatting.GOLD).
                        append(group.getDisplayComponent()));
            }

            VaultItemsHandler.appendGameLine(builder,
                new TextComponent("Hold Shift + ALT for more information").withStyle(ChatFormatting.GRAY));
        }
    }


    /**
     * Writes a title scroll's tooltip as the game draws it.
     *
     * @param builder Embed Builder.
     * @param itemStack the title scroll.
     */
    public static void handleTitleScrollTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultItemsHandler.appendGameLine(builder, VaultItemsHandler.ownerLine(TitleScrollItem.getOwnerUUID(itemStack).
            map(uuid -> TitleScrollItem.getOwnerName(itemStack).orElse("Unknown")).
            orElse(null)));

        PlayerTitlesConfig.Affix affix = TitleScrollItem.getAffix(itemStack).orElse(PlayerTitlesConfig.Affix.PREFIX);
        String tabText = TitleScrollItem.getTabText(itemStack).orElse(null);
        String color = TitleScrollItem.getColor(itemStack).orElse(null);

        if (tabText != null)
        {
            MutableComponent title = new TextComponent(tabText).withStyle(color == null ?
                Style.EMPTY.applyFormat(ChatFormatting.GRAY) :
                Style.EMPTY.withColor(TextColor.parseColor(color)));
            MutableComponent preview = new TextComponent("");

            if (affix == PlayerTitlesConfig.Affix.PREFIX)
            {
                preview.append(title).append(new TextComponent("<Player>"));
            }
            else
            {
                preview.append(new TextComponent("<Player>")).append(title);
            }

            VaultItemsHandler.appendGameLine(builder, new TextComponent("Preview: ").append(preview));
        }

        if (color == null)
        {
            builder.append("\n");
            VaultItemsHandler.appendTextLine(builder,
                new TextComponent("Combine with a dye in an anvil").withStyle(ChatFormatting.DARK_GRAY));
            VaultItemsHandler.appendTextLine(builder,
                new TextComponent("to set the title color.").withStyle(ChatFormatting.DARK_GRAY));
        }
        else
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Color: ").
                append(new TextComponent(color).withStyle(Style.EMPTY.withColor(TextColor.parseColor(color)))));
        }
    }


    /**
     * The owner line of soul flames and title scrolls: the owner's name in yellow, or grey question marks without one.
     *
     * @param ownerName the owner's name, or null when the item has no owner.
     * @return the line.
     */
    private static Component ownerLine(String ownerName)
    {
        MutableComponent name = ownerName == null ?
            new TextComponent("???").withStyle(ChatFormatting.GRAY) :
            new TextComponent(ownerName).withStyle(ChatFormatting.YELLOW);

        return new TextComponent("Player: ").append(name);
    }


    /**
     * Writes a soul flame's tooltip as the game draws it: owner, stacks and the crystal modifiers it carries.
     *
     * @param builder Embed Builder.
     * @param itemStack the soul flame.
     */
    public static void handleSoulFlameTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultItemsHandler.appendGameLine(builder, VaultItemsHandler.ownerLine(SoulFlameItem.getOwnerUUID(itemStack).
            map(uuid -> SoulFlameItem.getOwnerName(itemStack).orElse("Unknown")).
            orElse(null)));
        VaultItemsHandler.appendGameLine(builder, new TextComponent("Stacks: ").
            append(new TextComponent(String.valueOf(SoulFlameItem.getStacks(itemStack))).withStyle(ChatFormatting.AQUA)));

        SoulFlameItem.getModifiers(itemStack).ifPresent(modifiers ->
            VaultItemsHandler.parseModifiers(builder, modifiers));
    }


    public static void handleAntiqueTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultItemsHandler.appendItemGameLines(builder, itemStack);
    }


    public static void handleJewelPouchTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultItemsHandler.appendItemGameLines(builder, itemStack);
    }


    /**
     * Writes a coin pouch's own tooltip line; the coins themselves are drawn as slots in the picture.
     *
     * @param builder Embed Builder.
     * @param itemStack the coin pouch.
     */
    public static void handleCoinPouchTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultItemsHandler.appendItemGameLines(builder, itemStack);
    }


    public static void handleShardPouchTooltip(StringBuilder builder, ItemStack itemStack)
    {
        ItemStack shardStack = ItemShardPouch.getContainedStack(itemStack);
        String text = shardStack.isEmpty() ? "Empty" :
            shardStack.getCount() + " Shard" + (shardStack.getCount() > 1 ? "s" : "");

        VaultItemsHandler.appendGameLine(builder, new TextComponent(text).withStyle(ChatFormatting.GOLD));
    }


    public static void handleGreedyMealTooltip(StringBuilder builder, ItemStack itemStack)
    {
        int xpAmount = itemStack.getOrCreateTag().getInt("GreedXpBurger");

        if (xpAmount > 0)
        {
            VaultItemsHandler.appendGameLine(builder,
                new TextComponent("Grants " + String.format("%,d", xpAmount) + " Vault XP").withStyle(ChatFormatting.GREEN));
        }
    }


    /**
     * Writes a herald trophy's tooltip as the game draws it. The game shows the owner's current name when the viewer
     * knows the player; here it is the name stored on the trophy.
     *
     * @param builder Embed Builder.
     * @param itemStack the trophy.
     */
    public static void handleHeraldTrophyTooltip(StringBuilder builder, ItemStack itemStack)
    {
        CompoundTag blockEntityTag = itemStack.getOrCreateTagElement("BlockEntityTag");
        String ownerName = blockEntityTag.getString("OwnerName");

        VaultItemsHandler.appendGameLine(builder, new TextComponent("Awarded to: ").append(ownerName.isEmpty() ?
            new TextComponent("???").withStyle(ChatFormatting.GRAY) :
            new TextComponent(ownerName).withStyle(ChatFormatting.YELLOW)));
        VaultItemsHandler.appendGameLine(builder, new TextComponent("Time: ").
            append(new TextComponent(VaultItemsHandler.formatTimeString(blockEntityTag.getInt("Time"))).withStyle(ChatFormatting.GRAY)));

        VaultDifficulty difficulty = VaultDifficulty.byName(blockEntityTag.getString("Difficulty"));

        if (difficulty != null)
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Difficulty: ").
                append(difficulty.getDisplayName().copy().withStyle(ChatFormatting.GRAY)));
        }
    }


    /**
     * Writes a companion's tooltip as the game draws it. Vault Hunters builds the series, temporal modifier and relic
     * lines on the client only, so they are rebuilt here with the same text and styles.
     *
     * @param builder Embed Builder.
     * @param itemStack the companion.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void handleCompanionTooltip(StringBuilder builder, ItemStack itemStack)
    {
        CompoundTag tag = itemStack.getTag();

        if (tag == null || !tag.contains("PetName") || !tag.contains("PetSeries"))
        {
            return;
        }

        int level = tag.contains("level") ? tag.getInt("level") : 1;
        VaultItemsHandler.appendGameLine(builder, new TextComponent("Level: ").
            append(new TextComponent(String.valueOf(level)).withStyle(Style.EMPTY.withColor(LEVEL_COLOUR))));

        int hearts = tag.contains("hearts") ? tag.getInt("hearts") : 1;

        if (hearts > 0)
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Health: ").
                append(new TextComponent("\u2764".repeat(hearts)).withStyle(ChatFormatting.RED)));
            VaultItemsHandler.appendGameLine(builder, VaultItemsHandler.companionCooldownLine(tag));
        }
        else
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Health: ").
                append(new TextComponent("Retired").withStyle(ChatFormatting.GRAY)));
        }

        String seriesRaw = tag.getString("PetSeries");

        if (!seriesRaw.isEmpty())
        {
            String seriesDisplay = seriesRaw.substring(0, 1).toUpperCase() + seriesRaw.substring(1).toLowerCase();
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Series: ").
                append(new TextComponent(seriesDisplay).withStyle(Style.EMPTY.withColor(LEVEL_COLOUR))));
        }

        builder.append("\n");

        if (tag.contains("temporal"))
        {
            ResourceLocation modifierId = ResourceLocation.tryParse(tag.getString("temporal"));

            if (modifierId != null)
            {
                VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
                {
                    VaultItemsHandler.appendStyledLine(builder,
                        "**Temporal Modifier**",
                        new TextComponent("Temporal Modifier").withStyle(ChatFormatting.AQUA));
                    VaultItemsHandler.appendModifierLine(builder, vaultModifier, vaultModifier.getDisplayName(), 1);
                });
            }
        }

        CompoundTag relicsTag = tag.getCompound("relics");
        CompoundTag ancientRelicsTag = tag.getCompound("ancientRelics");

        if (!relicsTag.isEmpty() || ancientRelicsTag.contains("0"))
        {
            // The game counts in a HashMap, so its lines come in hash order; the same map gives the same order here.
            Map<ResourceLocation, Integer> modifierCounts = new HashMap<>();
            relicsTag.getAllKeys().stream().
                filter(key -> !key.isEmpty() && key.chars().allMatch(Character::isDigit)).
                forEach(key -> VaultItemsHandler.countRelicModifiers(relicsTag.getCompound(key), modifierCounts));

            if (ancientRelicsTag.contains("0"))
            {
                VaultItemsHandler.countRelicModifiers(ancientRelicsTag.getCompound("0"), modifierCounts);
            }

            VaultItemsHandler.appendStyledLine(builder,
                "**Relics**",
                new TextComponent("Relics").withStyle(ChatFormatting.GREEN));

            modifierCounts.forEach((id, count) ->
                VaultModifierRegistry.getOpt(id).ifPresent(vaultModifier ->
                    VaultItemsHandler.appendModifierLine(builder,
                        vaultModifier,
                        vaultModifier.getDisplayNameFormatted(count),
                        count)));
        }
    }


    /**
     * Counts each modifier of one companion relic.
     *
     * @param relic the relic's tag (model and modifier ids).
     * @param modifierCounts the counts so far.
     */
    private static void countRelicModifiers(CompoundTag relic, Map<ResourceLocation, Integer> modifierCounts)
    {
        ListTag modifiers = relic.getList("modifiers", Tag.TAG_STRING);

        for (int index = 0; index < modifiers.size(); index++)
        {
            ResourceLocation id = ResourceLocation.tryParse(modifiers.getString(index));

            if (id != null)
            {
                modifierCounts.merge(id, 1, Integer::sum);
            }
        }
    }


    /**
     * The companion's cooldown line: ready in green, else the time left in red.
     *
     * @param tag the companion's tag.
     * @return the line.
     */
    private static Component companionCooldownLine(CompoundTag tag)
    {
        int cooldownSeconds = 0;

        if (tag.contains("cooldownExpiry"))
        {
            long now = System.currentTimeMillis();
            long expiry = tag.getLong("cooldownExpiry");
            cooldownSeconds = (int) Math.max(0L, (expiry - now) / 1000L);
        }

        if (cooldownSeconds <= 0)
        {
            return new TextComponent("Cooldown: ").append(new TextComponent("Ready!").withStyle(ChatFormatting.GREEN));
        }

        String remaining = VaultItemsHandler.formatTimeString(cooldownSeconds * 20L);
        return new TextComponent("Cooldown: ").append(new TextComponent(remaining).withStyle(ChatFormatting.RED));
    }


    /**
     * One companion modifier line as the game draws it: indented, a grey count when stacked, the name in the
     * modifier's colour.
     *
     * @param builder Embed Builder.
     * @param vaultModifier the modifier.
     * @param displayName the name the game shows for it there.
     * @param count how many times the companion has it.
     */
    private static void appendModifierLine(StringBuilder builder, VaultModifier<?> vaultModifier, String displayName, int count)
    {
        MutableComponent name = new TextComponent(displayName).
            withStyle(Style.EMPTY.withColor(vaultModifier.getDisplayTextColor()));
        MutableComponent line = new TextComponent("  ");

        if (count > 1)
        {
            line.append(new TextComponent(count + "x ").withStyle(ChatFormatting.GRAY));
        }

        line.append(name);
        VaultItemsHandler.appendStyledLine(builder, DOT + " " + line.getString().strip(), line);
    }


    public static void handleAttributeFilterTooltip(StringBuilder builder, ItemStack itemStack)
    {
        CompoundTag tag = itemStack.getTag();

        if (tag == null)
        {
            return;
        }

        int whitelistMode = tag.getInt("WhitelistMode");
        String modeText = switch (whitelistMode)
        {
            case 1 -> "Allow-List (All)";
            case 2 -> "Deny-List";
            default -> "Allow-List (Any)";
        };

        builder.append("**").append(modeText).append("**\n");

        ListTag attributes = tag.getList("MatchedAttributes", 10);

        for (int i = 0; i < attributes.size(); i++)
        {
            if (i >= 5)
            {
                builder.append("...and ").append(attributes.size() - 5).append(" more\n");
                break;
            }

            CompoundTag attributeTag = attributes.getCompound(i);
            boolean inverted = attributeTag.getBoolean("Inverted");
            String description = VaultItemsHandler.decodeAttributeFilterEntry(attributeTag);

            builder.append(DOT).append(" ");

            if (inverted)
            {
                builder.append("NOT ");
            }

            builder.append(description).append("\n");
        }
    }


    private static String decodeAttributeFilterEntry(CompoundTag tag)
    {
        for (String key : tag.getAllKeys())
        {
            if ("Inverted".equals(key))
            {
                continue;
            }

            CompoundTag data = tag.getCompound(key);

            return switch (key)
            {
                case "standard_trait" -> decodeStandardTrait(data);
                case "in_tag" -> "in tag #" + data.getString("space") + ":" + data.getString("path");
                case "added_by" -> "added by " + data.getString("id");
                case "in_item_group" -> "in group " + data.getString("path");
                case "has_enchant" -> "enchanted with " + data.getString("id");
                case "has_name" -> "named \"" + data.getString("name") + "\"";
                case "book_author" -> "by author " + data.getString("author");
                case "has_color" -> "has color " + data.getString("color");
                case "has_fluid" -> "contains fluid " + data.getString("id");
                default -> formatAttributeKeyReadable(key, data);
            };
        }

        return "unknown filter";
    }


    private static String decodeStandardTrait(CompoundTag data)
    {
        for (String traitName : data.getAllKeys())
        {
            if (data.getBoolean(traitName))
            {
                return traitName.charAt(0) + traitName.substring(1).toLowerCase().replace("_", " ");
            }
        }

        return "unknown trait";
    }


    private static String formatAttributeKeyReadable(String key, CompoundTag data)
    {
        StringBuilder readable = new StringBuilder();

        for (String part : key.split("_"))
        {
            if (!part.isEmpty())
            {
                if (readable.length() > 0)
                {
                    readable.append(" ");
                }

                readable.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
        }

        for (String dataKey : data.getAllKeys())
        {
            String value = data.getString(dataKey);

            if (!value.isEmpty())
            {
                readable.append(": ").append(value);
                break;
            }
        }

        return readable.toString();
    }


    public static void handleKeyringTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultItemsHandler.appendItemGameLines(builder, itemStack);
    }


    public static void handleCardTooltip(StringBuilder builder, ItemStack itemStack)
    {
        Card card = CardItem.getCard(itemStack);

        VaultItemsHandler.appendGameLine(builder, new TextComponent("Tier: ").
            append(new TextComponent(String.valueOf(card.getTier())).withStyle(Style.EMPTY.withColor(LEVEL_COLOUR))));

        List<CardEntry.Color> colors = new ArrayList<>(card.getColors());

        if (!colors.isEmpty())
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Color: ").
                append(VaultItemsHandler.joinedCardList(colors.stream().map(CardEntry.Color::getColoredText).toList())));
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
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Type: ").
                append(VaultItemsHandler.joinedCardList(VaultItemsHandler.cardGroupTexts(types))));
        }

        if (!groups.isEmpty())
        {
            VaultItemsHandler.appendGameLine(builder, new TextComponent("Groups: ").
                append(VaultItemsHandler.joinedCardList(VaultItemsHandler.cardGroupTexts(groups))));
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


    public static void handleModifierScrollTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultItemsHandler.appendItemGameLines(builder, itemStack);
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void handleCardDeckTooltip(StringBuilder builder, ItemStack itemStack)
    {
        CardDeckItem.getCardDeck(itemStack).ifPresent(deck ->
        {
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

                        builder.append(" ").append(DOT).append(" ").
                            append(cardName).
                            append(" (T").append(card.getTier()).append(")").
                            append("\n");
                    });

                builder.append("\n");
            }

            List<VaultGearAttributeInstance<?>> attributes = deck.getSnapshotAttributes();

            if (!attributes.isEmpty())
            {
                builder.append("**Attributes:**\n");

                for (VaultGearAttributeInstance instance : attributes)
                {
                    MutableComponent text = instance.getAttribute().getReader().
                        getDisplay(instance, VaultGearModifier.AffixType.PREFIX);

                    if (text != null)
                    {
                        builder.append(" ").append(DOT).append(" ").
                            append(text.getString()).append("\n");
                    }
                }

                builder.append("\n");
            }

            VaultItemsHandler.appendDeckSockets(builder, DeckTooltips.deckLines(deck));
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
            effects.forEach(effect -> builder.append(" ").append(DOT).append(" ").append(effect).append("\n"));
        }
    }


// ---------------------------------------------------------------------
// Section: Private processing methods
// ---------------------------------------------------------------------


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
                VaultItemsHandler.appendStyledLine(builder, line.getString(), line);
            });
        }
        else
        {
            List<Component> components = new ArrayList<>();
            property.addText(components, 0, TooltipFlag.Default.ADVANCED, 0f, tier);

            components.forEach(component -> VaultItemsHandler.appendStyledLine(builder, component.getString(), component));
        }
    }


    @SuppressWarnings("unchecked")
    public static <C, T> MutableComponent uncheckedGetConfigRangeDisplay(
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
    public static <C, T> MutableComponent uncheckedGetConfigRangeDisplay(
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


    /**
     * This method adds affixes to the embed builder.
     * @param builder Embed builder that need to be populated.
     * @param data Vault Gear Data.
     * @param type Affix type.
     * @param itemStack Item Stack that is displayed.
     */
    private static void addAffixList(StringBuilder builder,
        VaultGearData data,
        VaultGearModifier.AffixType type,
        ItemStack itemStack)
    {
        List<VaultGearModifier<?>> affixes = data.getModifiers(type);

        VaultGearAttribute<Integer> affixAttribute = (type == VaultGearModifier.AffixType.PREFIX) ?
            ModGearAttributes.PREFIXES : ModGearAttributes.SUFFIXES;

        int emptyAffixes = data.getFirstValue(affixAttribute).orElse(0);

        builder.append("**").
            append(affixes.size() != 1 ? type.getPlural() : type.getSingular()).
            append(":** ");
        builder.append("\n");
        affixes.forEach(modifier -> VaultItemsHandler.addAffix(builder, modifier, data, type, itemStack));

        if (type != VaultGearModifier.AffixType.IMPLICIT)
        {
            for (int i = 0; i < emptyAffixes - affixes.size(); i++)
            {
                builder.append(VaultItemsHandler.createEmptyAffix(type));
                builder.append("\n");
            }
        }
    }


    /**
     * This method adds affix text to given builder.
     * @param builder Embed builder.
     * @param modifier Vault Gear Modifier
     * @param data Vault Gear data
     * @param type Affix Type.
     * @param stack ItemStack of item.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addAffix(StringBuilder builder,
        VaultGearModifier modifier,
        VaultGearData data,
        VaultGearModifier.AffixType type,
        ItemStack stack)
    {
        Optional.ofNullable(modifier.getAttribute().getReader().getDisplay(modifier, data, type, stack)).
            map(text -> {
                MutableComponent styled = AffixCategoryStyle.apply(modifier, text);
                String line = styled.getString();
                TooltipColorHints.record(line, styled);
                return line;
            }).
            ifPresent(text ->
            {
                MutableComponent tierDisplay = VaultGearTierConfig.getConfig(stack).map((tierConfig) ->
                {
                    Object config = tierConfig.getTierConfig(modifier);

                    if (config != null)
                    {
                        MutableComponent range = modifier.getAttribute().getGenerator().getConfigRangeDisplay(
                            modifier.getAttribute().getReader(),
                            config);
                        return range == null ? new TextComponent("") : range;
                    }
                    else
                    {
                        return null;
                    }
                }).orElse(null);

                builder.append(text);

                if (tierDisplay != null)
                {
                    String legendaryInfo = modifier.hasCategory(VaultGearModifier.AffixCategory.LEGENDARY) ? "**Legendary** " : "";

                    if (tierDisplay.getString().isEmpty())
                    {
                        builder.append(" (%sT%s)".formatted(legendaryInfo, modifier.getRolledTier() + 1));
                    }
                    else
                    {
                        builder.append(" (%sT%s: ".formatted(legendaryInfo, modifier.getRolledTier() + 1));
                        builder.append(tierDisplay.getString());
                        builder.append(")");
                    }
                }

                builder.append("\n");
            });
    }


    /**
     * This method creates empty affix of given type.
     * @param type Affix type.
     * @return Empty affix text.
     */
    private static String createEmptyAffix(VaultGearModifier.AffixType type)
    {
        String line = (SQUARE + " empty %s").formatted(type.name().toLowerCase(Locale.ROOT));
        TooltipColorHints.record(line, new TextComponent(line).withStyle(ChatFormatting.GRAY));
        return line;
    }


    /**
     * This method creates repair text based on used repairs and total repairs values.
     * @param usedRepairs Number of used repairs.
     * @param totalRepairs Number of total repairs.
     * @return Text for repairs.
     */
    private static String createRepairText(int usedRepairs, int totalRepairs)
    {
        int remaining = totalRepairs - usedRepairs;

        return "**Repairs:** " +
            VaultItemsHandler.createDots(usedRepairs, FULL_CIRCLE) +
            VaultItemsHandler.createDots(remaining, EMPTY_CIRCLE);
    }


    /**
     * This method generates a repair dots for gear.
     * @param amount Amount of dots.
     * @param symbol Dot symbol.
     * @return String that contains number of repairs for gear.
     */
    private static String createDots(int amount, String symbol)
    {
        return (symbol + " ").repeat(Math.max(0, amount));
    }


    /**
     * Writes one group of crystal modifiers under its header as the game does: each modifier indented, its count grey
     * (always for catalyst modifiers, else only when stacked) and its name in the modifier's colour.
     * @param builder Builder that need to be populated.
     * @param modifiers all modifiers of the crystal.
     * @param header the group's header.
     * @param alwaysShowCount whether a single modifier still shows its count, as catalyst modifiers do.
     * @param filter which modifiers belong to the group.
     */
    private static void appendModifierGroup(StringBuilder builder,
        List<VaultModifierStack> modifiers,
        Component header,
        boolean alwaysShowCount,
        Predicate<VaultModifierStack> filter)
    {
        List<VaultModifierStack> group = modifiers.stream().filter(filter).toList();

        if (group.isEmpty())
        {
            return;
        }

        VaultItemsHandler.appendGameLine(builder, header);

        for (VaultModifierStack modifierStack : group)
        {
            VaultModifier<?> vaultModifier = modifierStack.getModifier();
            MutableComponent name = new TextComponent(vaultModifier.getDisplayNameFormatted(modifierStack.getSize())).
                withStyle(Style.EMPTY.withColor(vaultModifier.getDisplayTextColor()));
            MutableComponent line;

            if (alwaysShowCount || modifierStack.getSize() > 1)
            {
                line = new TextComponent("  ").withStyle(ChatFormatting.GRAY).
                    append(new TextComponent("%dx".formatted(modifierStack.getSize())).withStyle(ChatFormatting.GRAY)).
                    append(" ").
                    append(name);
            }
            else
            {
                line = new TextComponent("  ").append(name);
            }

            VaultItemsHandler.appendStyledLine(builder, DOT + " " + line.getString().strip(), line);
        }
    }


    /**
     * Returns Crystal Objective name from instance of CrystalObjective.
     * @param objective class.
     */
    private static void addObjectiveName(StringBuilder builder, CrystalObjective objective)
    {
        String type = CrystalData.OBJECTIVE.getType(objective);

        if (type.equals("ascension"))
        {
            builder.append("**Ascension:** ");
        }
        else
        {
            builder.append("**Objective:** ");
        }

        switch (type)
        {
            case "null" -> builder.append("???");
            case "pool" -> builder.append("???");
            case "empty" -> builder.append("None");
            case "boss" -> builder.append("Hunt the Guardians");
            case "cake" -> builder.append("Cake Hunt");
            case "scavenger" -> builder.append("Scavenger Hunt");
            case "speedrun" -> builder.append("Speedrun");
            case "monolith" -> builder.append("Light the Braziers");
            case "elixir" -> builder.append("Elixir Rush");
            case "paradox" ->
            {
                ParadoxCrystalObjective paradox = (ParadoxCrystalObjective) objective;

                builder.append("Divine Paradox").append("\n");
                builder.append(" ").
                    append(VaultItemsHandler.DOT).
                    append(" ").
                    append("**Type:** ").
                    append(paradox.getType().getName()).
                    append("\n");

                CompoundTag paradoxTag = paradox.writeNbt().orElse(new CompoundTag());

                if (!paradoxTag.contains("player_uuid"))
                {
                    if (paradox.getType() == ParadoxObjective.Type.RUN)
                    {
                        builder.append(" ").
                            append(VaultItemsHandler.DOT).
                            append(" ").
                            append("**Cooldown:** ???").
                            append("\n");
                    }

                    builder.append(" ").
                        append(VaultItemsHandler.DOT).
                        append(" ").
                        append("**Player:** ???");
                }
                else
                {
                    long ticksLeft = paradox.getCooldown() / 50L;

                    if (paradox.getType() == ParadoxObjective.Type.RUN)
                    {
                        builder.append(" ").
                            append(VaultItemsHandler.DOT).
                            append(" ").
                            append("**Cooldown:** ").
                            append(ticksLeft < 0L ? "Ready" : VaultItemsHandler.formatTimeString((int)ticksLeft)).
                            append("\n");
                    }

                    builder.append(" ").
                        append(VaultItemsHandler.DOT).
                        append(" ").
                        append("**Player:** ").
                        append(paradoxTag.contains("player_name") ? "Unknown" : paradoxTag.getString("player_name"));
                }
            }
            case "herald" -> builder.append("Defeat the Herald");
            case "compound" -> builder.append("???");
            case "ascension" ->
            {
                AscensionCrystalObjective ascension = (AscensionCrystalObjective) objective;
                CompoundTag ascensionTag = ascension.writeNbt().orElse(new CompoundTag());

                if (!ascensionTag.contains("player_uuid"))
                {
                    builder.append(" ").
                        append(VaultItemsHandler.DOT).
                        append(" ").
                        append("**Player:** ???").
                        append("\n");
                }
                else
                {
                    builder.append(" ").
                        append(VaultItemsHandler.DOT).
                        append(" ").
                        append("**Player:** ").
                        append(ascensionTag.contains("player_name") ? "Unknown" : ascensionTag.getString("player_name")).
                        append("\n");
                }

                builder.append(" ").
                    append(VaultItemsHandler.DOT).
                    append(" ").
                    append("**Stacks:** ").
                    append(ascensionTag.getInt("stack"));
            }
            case "bingo" -> builder.append("Bingo");
            case "offering_boss" -> builder.append("Offering Boss");
            case "raid" -> builder.append("Raid");
            default -> builder.append("???");
        }

        builder.append("\n");
    }


    /**
     * Returns Crystal Theme name from instance of CrystalTheme.
     * @param theme class.
     * @return Name of the theme.
     */
    @SuppressWarnings("deprecation")
    private static String parseThemeName(CrystalTheme theme)
    {
        if (theme instanceof PoolCrystalTheme)
        {
            return "???";
        }
        else if (theme instanceof ValueCrystalTheme)
        {
            ThemeKey themeKey = VaultRegistry.THEME.getKey(theme.serializeNBT().getString("id"));

            if (themeKey == null)
            {
                return "Unknown";
            }
            else
            {
                return themeKey.getName();
            }
        }
        else if (theme instanceof NullCrystalTheme)
        {
            return "???";
        }
        else
        {
            return "???";
        }
    }


    /**
     * Returns Crystal Layout name from instance of CrystalLayout.
     * @param layout class.
     */
    private static void addLayoutName(StringBuilder builder, CrystalLayout layout)
    {
        builder.append("**Layout:** ");

        String type = CrystalData.LAYOUT.getType(layout);


        switch (type)
        {
            case "null" -> builder.append("???");
            case "infinite" -> builder.append("Infinite");
            case "circle" -> builder.append("Circle");
            case "polygon" -> builder.append("Polygon");
            case "spiral" -> builder.append("Spiral");
            case "architect" ->
            {
                ArchitectCrystalLayout architect = ((ArchitectCrystalLayout) layout);
                CompoundTag tag = architect.writeNbt().orElse(new CompoundTag());

                builder.append("Architect").append(" | %.1f%%".formatted(Math.min(tag.getFloat("completion"), 1.0F) * 100.0F));

                ListTag entries = tag.getList("entries", CompoundTag.TAG_COMPOUND);

                entries.forEach(entry ->
                {
                    CompoundTag roomTag = (CompoundTag) entry;
                    int count = roomTag.getInt("count");
                    String roomStr = count > 1 ? "Rooms" : "Room";

                    builder.append(" ").
                        append(VaultItemsHandler.DOT).
                        append(" ").
                        append(count).
                        append(" ").
                        append(roomTag.contains("type") ?
                            roomTag.getString("type") : roomTag.getString("pool")).
                        append(" ").
                        append(roomStr).
                        append("\n");

                });
            }
            case "paradox" ->
            {
                builder.append("Preset").append("\n");

                Map<VaultGod, Integer> godCounts = new HashMap<>();
                ParadoxCrystalLayout paradox = ((ParadoxCrystalLayout) layout);
                boolean hasUser = paradox.writeNbt().orElse(new CompoundTag()).contains("player_uuid");

                paradox.getPreset().getAll().forEach((regionPos, preset) ->
                {
                    if (preset instanceof ParadoxTemplatePreset paradoxTemplatePreset)
                    {
                        VaultGod god = paradoxTemplatePreset.getGod();

                        if (god != null)
                        {
                            godCounts.put(god, godCounts.getOrDefault(god, 0) + 1);
                        }
                    }
                });

                for (VaultGod god : VaultGod.values())
                {
                    int count = godCounts.getOrDefault(god, 0);
                    String roomStr = hasUser && count == 1 ? "Room" : "Rooms";

                    builder.append(" ").
                        append(VaultItemsHandler.DOT).
                        append(" ").
                        append(hasUser ? "?" : count).
                        append(" ").
                        append(god.getName()).
                        append(" ").
                        append(roomStr).
                        append("\n");
                }
            }
            case "herald" -> builder.append("Preset");
            case "raid" -> builder.append("Preset");
            case "compound" -> builder.append("???");
        }

        builder.append("\n");
    }


    /**
     * Returns Crystal Time name from instance of CrystalTime.
     * @param time class.
     * @return Name of the time.
     */
    private static String parseTime(CrystalTime time)
    {
        if (time instanceof PoolCrystalTime)
        {
            return "";
        }
        else if (time instanceof ValueCrystalTime vaultTime)
        {
            int min = vaultTime.getRoll().getMin();
            int max = vaultTime.getRoll().getMax();
            String text = formatTimeString(min);
            if (min != max) {
                text = text + " - " + formatTimeString(max);
            }

            return "**Time:** " + text;
        }
        else if (time instanceof NullCrystalTime)
        {
            return "";
        }
        else
        {
            return "";
        }
    }


    /**
     * Writes a crystal's modifiers as the game's crystal tooltip lists them: clarity and curses, then the positive,
     * negative and other modifiers, each in the modifier's colour. Vault Hunters builds these lines on the client only,
     * so they are rebuilt here with the same text and styles.
     * @param builder Builder that need to be populated.
     * @param modifiers The object that contains all crystal modifiers.
     */
    private static void parseModifiers(StringBuilder builder, CrystalModifiers modifiers)
    {
        List<VaultModifierStack> modifierList = new ArrayList<>();
        modifiers.forEach(modifierList::add);

        Style curseStyle = Style.EMPTY.withColor(ModConfigs.VAULT_CRYSTAL.MODIFIER_STABILITY.curseColor);
        boolean personal = modifiers instanceof PersonalCrystalModifiers;

        if (modifiers instanceof ParadoxCrystalModifiers)
        {
            VaultItemsHandler.appendModifierGroup(builder,
                modifierList,
                new TextComponent("Cursed").withStyle(curseStyle),
                true,
                stack -> ModConfigs.VAULT_CRYSTAL_CATALYST.isCurse(stack.getModifierId()));
        }
        else if (!personal)
        {
            if (modifiers.hasClarity())
            {
                VaultItemsHandler.appendGameLine(builder,
                    new TextComponent("Clarity").withStyle(Style.EMPTY.withColor(CLARITY_COLOUR)));
            }

            int curseCount = modifiers.getCurseCount();

            if (curseCount > 0 && modifiers.hasClarity())
            {
                VaultItemsHandler.appendModifierGroup(builder,
                    modifierList,
                    new TextComponent("Cursed").withStyle(curseStyle),
                    true,
                    stack -> ModConfigs.VAULT_CRYSTAL_CATALYST.isCurse(stack.getModifierId()));
            }
            else if (curseCount > 0)
            {
                VaultItemsHandler.appendGameLine(builder,
                    new TextComponent("Cursed ").withStyle(curseStyle).append(CURSE.repeat(curseCount)));
            }
        }

        String headerSuffix = personal ? "" : " Modifiers";

        VaultItemsHandler.appendModifierGroup(builder,
            modifierList,
            new TextComponent("Positive" + headerSuffix).withStyle(ChatFormatting.GREEN),
            true,
            stack -> ModConfigs.VAULT_CRYSTAL_CATALYST.isGood(stack.getModifierId()));
        VaultItemsHandler.appendModifierGroup(builder,
            modifierList,
            new TextComponent("Negative" + headerSuffix).withStyle(ChatFormatting.RED),
            true,
            stack -> ModConfigs.VAULT_CRYSTAL_CATALYST.isBad(stack.getModifierId()));
        VaultItemsHandler.appendModifierGroup(builder,
            modifierList,
            new TextComponent("Other" + headerSuffix).withStyle(ChatFormatting.WHITE),
            false,
            stack -> ModConfigs.VAULT_CRYSTAL_CATALYST.isUnlisted(stack.getModifierId()));
    }


    /**
     * Time parser
     * @param remainingTicks how many ticks remaining
     * @return remaining ticks parsed as string.
     */
    private static String formatTimeString(int remainingTicks)
    {
        long seconds = remainingTicks / 20 % 60;
        long minutes = remainingTicks / 20 / 60 % 60;
        long hours = remainingTicks / 20 / 60 / 60;
        return hours > 0L ? String.format("%02d:%02d:%02d", hours, minutes, seconds) :
            String.format("%02d:%02d", minutes, seconds);
    }


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------


    /**
     * symbol for text fields.
     */
    private static final String EMPTY_CIRCLE = "\u25CB";

    /**
     * symbol for text fields.
     */
    private static final String FULL_CIRCLE = "\u25CF";

    private static final String SQUARE = "\u25A0";

    /**
     * The colours the game's gear tooltip gives these values.
     */
    private static final int LEVEL_COLOUR = 0xB0C0FA;

    private static final int CRAFTER_COLOUR = 0xFFE400;

    private static final int SLOT_COLOUR = 0xE2E28A;

    /**
     * The colour the game's card tooltip gives types and groups.
     */
    private static final int CARD_GROUP_COLOUR = 0xCCCCCC;

    private static final int INSCRIPTION_COMPLETION_COLOUR = 0x48BAF8;

    /**
     * The colours the game's crystal, relic fragment, infused catalyst and potion bottle tooltips give these texts.
     */
    private static final int CLARITY_COLOUR = 0x4BE3C5;

    private static final int RELIC_COLOUR = 0xC6B11E;

    private static final int CATALYST_SIZE_COLOUR = 0xDDDDDD;

    private static final int BOTTLE_TEXT_COLOUR = 0xCFCFCF;

    /**
     * Wold's Vaults' rainbow text: the hue moves once around the circle in this time, at this saturation and brightness.
     */
    private static final long RAINBOW_PERIOD_MILLISECONDS = 2000L;

    private static final float RAINBOW_SATURATION = 0.85F;

    private static final float RAINBOW_BRIGHTNESS = 1.0F;

    /**
     * An etching description placeholder for an effect name, e.g. %svalue/effect.
     */
    private static final Pattern ETCHING_EFFECT_PLACEHOLDER = Pattern.compile("%s([\\w$]+)/effect(?![\\w$/])");

    private static final ResourceLocation GREED_TIER_ATTRIBUTE = new ResourceLocation("the_vault", "greed_tier");

    private static final String WOLDS_VAULTS_MOD_ID = "woldsvaults";

    private static final String COMBINED_TRINKET_CLASS = "xyz.iwolfking.woldsvaults.items.CombinedTrinketItem";

    private static final String INFUSED_AUGMENT_CLASS = "xyz.iwolfking.woldsvaults.items.InfusedAugmentItem";

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Crystal part and item classes whose tooltip text failed on the server, so each is logged once.
     */
    private static final Set<String> GAME_TEXT_FAILURES = ConcurrentHashMap.newKeySet();

    private static final int DURABILITY_COLOUR = 0xAAAAAA;

    private static final int BROKEN_COLOUR = 0xFF5555;

    private static final int IMBUED_COLOUR = 0xA821FC;

    private static final int SEALED_COLOUR = 0xC2FF7A;

    private static final int CORRUPTED_COLOUR = 0x780004;

    private static final String CURSE = "\u2620";

    /**
     * Symbol for text fields.
     */
    private static final String DOT = "\u2022";

    /**
     * How the game's deck tooltip starts its sockets line.
     */
    private static final String SOCKETS_PREFIX = "Sockets:";

    /**
     * Variable format for numbers.
     */
    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");


    private static String formatTimeString(long ticks)
    {
        long seconds = ticks / 20L % 60L;
        long minutes = ticks / 20L / 60L % 60L;
        long hours = ticks / 20L / 60L / 60L;

        return hours > 0L
            ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
            : String.format("%02d:%02d", minutes, seconds);
    }

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#.##%");
}
