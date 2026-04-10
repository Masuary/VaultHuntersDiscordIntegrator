//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.parser;


import com.google.gson.JsonObject;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Predicate;

import iskallia.vault.antique.Antique;
import iskallia.vault.config.AntiquesConfig;
import iskallia.vault.config.PlayerTitlesConfig;
import iskallia.vault.config.TrinketConfig;
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
import iskallia.vault.gear.attribute.VaultGearAttributeRegistry;
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
import iskallia.vault.item.crystal.VaultCrystalItem;
import iskallia.vault.item.crystal.layout.*;
import iskallia.vault.item.crystal.layout.preset.ParadoxTemplatePreset;
import iskallia.vault.item.crystal.modifiers.CrystalModifiers;
import iskallia.vault.item.crystal.modifiers.ParadoxCrystalModifiers;
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
import iskallia.vault.item.gear.TrinketItem;
import iskallia.vault.item.modification.GearModificationItem;
import iskallia.vault.item.modification.ReforgeTagModificationFocus;
import iskallia.vault.item.CompanionRelicItem;
import iskallia.vault.item.DeckSocketItem;
import iskallia.vault.item.SigilItem;
import iskallia.vault.item.tool.JewelItem;
import iskallia.vault.item.tool.PaxelItem;
import iskallia.vault.util.MiscUtils;
import iskallia.vault.world.data.PlayerTitlesData;
import lv.id.bonne.vhdiscord.vaulthunters.mixin.CardEntryAccessor;
import lv.id.bonne.vhdiscord.vaulthunters.mixin.GearCardModifierAccessor;
import lv.id.bonne.vhdiscord.vaulthunters.mixin.GearModificationItemAccessor;
import lv.id.bonne.vhdiscord.vaulthunters.mixin.ReforgeTagModificationFocusInvoker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.ForgeRegistries;


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
    @SuppressWarnings("deprecation")
    public static String generateVaultHuntersItemTooltips(JsonObject itemJson,
        ItemStack itemStack,
        CompoundTag itemTag)
    {
        try
        {
            StringBuilder builder = new StringBuilder();

            if (itemStack.getItem() instanceof BottleItem)
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
            else if (itemStack.getItem() instanceof JewelItem)
            {
                VaultItemsHandler.handleJewelTooltip(builder, itemStack);
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

        // Add gear Level
        builder.append("**Level:** ").append(data.getItemLevel()).append("\n");

        // Add crafter name
        data.getFirstValue(ModGearAttributes.CRAFTED_BY).ifPresent(crafter ->
            builder.append("**Crafted by:** ").append(crafter).append("\n"));

        // Add Rarity
        switch (state)
        {
            case UNIDENTIFIED ->
            {
                Objects.requireNonNull(ModConfigs.VAULT_GEAR_TYPE_CONFIG);
                data.getFirstValue(ModGearAttributes.GEAR_ROLL_TYPE).
                    flatMap(ModConfigs.VAULT_GEAR_TYPE_CONFIG::getRollPool).
                    ifPresent(pool -> builder.append("**Roll:** ").append(pool.getName()).append("\n"));
            }
            case IDENTIFIED ->
            {
                builder.append("**Rarity:** ").append(data.getRarity().getDisplayName().getString()).append("\n");
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
                }
            }

            // Add Repair text.
            int usedRepairs = data.getUsedRepairSlots();
            int totalRepairs = data.getRepairSlots();

            if (totalRepairs > 0)
            {
                builder.append(VaultItemsHandler.createRepairText(usedRepairs, totalRepairs)).append("\n");
            }

            // Add Durability
            if (VaultGearItem.of(itemStack).isBroken(itemStack))
            {
                builder.append("**Durability:** ").append("BROKEN").append("\n");
            }
            else if (itemStack.isDamageableItem() && itemStack.getMaxDamage() > 0)
            {
                builder.append("**Durability:** ").append("%d/%d".formatted(
                    itemStack.getMaxDamage() - itemStack.getDamageValue(),
                        itemStack.getMaxDamage())).
                    append("\n");
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
                    }
                });

            // Add Etching info if present
            data.getFirstValue(ModGearAttributes.ETCHING).ifPresent(etchingId -> {
                VaultEtchingConfig.EtchingEntry etchingEntry = ModConfigs.ETCHINGS.getEtchingConfig(etchingId);
                if (etchingEntry != null)
                {
                    String etchingDesc = EtchingHelper.formatDescription(
                        etchingEntry.getDescription(), EtchingHelper.getEtching(itemStack));
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

        if (data.isCorrupted())
        {
            builder.append("\n").append("**Corrupted**").append(" (Unmodifiable Item)");
        }

        if (data.isSealed())
        {
            builder.append("\n").append("**Sealed**");
        }

        if (data.isImbued())
        {
            builder.append("\n").append("**Imbued**");
        }
    }


    /**
     * This method parses bottle item into discord chat.
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

        BottleItem.getType(itemStack).ifPresent(type -> {
            builder.append(itemStack.getDisplayName().getString()).append("\n");

            BottleItem.getRecharge(itemStack).ifPresent(recharge -> {
                switch (recharge)
                {
                    case TIME -> builder.append("Passively restores charges while inside a vault").append("\n");
                    case MOBS -> builder.append("Kill vault mobs to restore charges").append("\n");
                    case CHESTS -> builder.append("Loot vault chests to restore charges").append("\n");
                }
            });

            builder.append("Consume a charge to").
                append("\n").
                append(" ").
                append(DOT).
                append(" Heal ").
                append(ModConfigs.POTION.getPotion(type).getHealing()).
                append(" Hit Points").
                append("\n");

            BottleItem.getEffect(itemStack).ifPresent(effect -> {
                builder.append(" ").
                    append(DOT).
                    append(" ").
                    append(effect.getTooltip().getString()).
                    append("\n");
            });
        });
    }


    /**
     * This method parses doll tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemTag Vault Doll Item Tag.
     */
    public static void handleDollTooltip(StringBuilder builder, CompoundTag itemTag)
    {
        String owner = itemTag.getCompound("playerProfile").getString("Name");

        builder.append("**Owner:**").
            append(" ").
            append(owner).
            append("\n");

        int lootPercent = (int) (itemTag.getFloat("lootPercent") * 100.0F);

        builder.append("**Loot Efficiency:**").
            append(" ").
            append(String.format("%d", lootPercent)).
            append("%").
            append("\n");

        int xpPercent = (int) (itemTag.getFloat("xpPercent") * 100.0F);

        builder.append("**Experience Efficiency:**").
            append(" ").
            append(String.format("%d", xpPercent)).
            append("%").
            append("\n");

        if (itemTag.contains("vaultUUID"))
        {
            builder.append("**Ready to be released!**");
        }
        else
        {
            builder.append("**Ready for a vault!**");
        }
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
            builder.append("Unidentified Etching\n");
            return;
        }

        builder.append("**Greed Tier:** ").append(data.getItemLevel()).append("\n");

        ResourceLocation etchingId = data.getAttributes(ModGearAttributes.ETCHING)
            .findFirst()
            .map(VaultGearAttributeInstance::getValue)
            .orElse(null);

        if (etchingId == null)
        {
            builder.append("No Etching Applied\n");
            return;
        }

        VaultEtchingConfig.EtchingEntry entry = ModConfigs.ETCHINGS.getEtchingConfig(etchingId);

        if (entry == null)
        {
            builder.append("Unknown Etching\n");
            return;
        }

        String description = EtchingHelper.formatDescription(
            entry.getDescription(), EtchingHelper.getEtching(itemStack));
        description = description.replaceAll("<[^>]+>", "");
        builder.append(description).append("\n\n");

        builder.append("**Type:** ").append(String.join(", ", entry.getTypeGroups())).append("\n");
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
            builder.append("**Completion:** ").
                append(Math.round(compoundTag.getFloat("completion") * 100.0F)).
                append("%").
                append("\n");
        }

        if (compoundTag.contains("time"))
        {
            builder.append("**Time:** ").
                append(VaultItemsHandler.formatTimeString(compoundTag.getInt("time"))).
                append("\n");
        }

        if (compoundTag.contains("instability"))
        {
            builder.append("**Instability:** ").
                append(Math.round(compoundTag.getInt("instability") * 100.0F)).
                append("%").
                append("\n");
        }

        builder.append("**Size:** ").
            append(compoundTag.getInt("size")).
            append("\n");

        for (InscriptionData.Entry entry : data.getEntries())
        {
            String roomStr = entry.count > 1 ? "Rooms" : "Room";

            builder.append(" ").
                append(VaultItemsHandler.DOT).
                append(" ").
                append(entry.count).
                append(" ").
                append(entry.toRoomEntry().getName().getString()).
                append(" ").
                append(roomStr).
                append("\n");
        }
    }


    /**
     * This method parses Vault Crystal item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param crystalData Vault Crystal Data.
     */
    public static void handleVaultCrystalTooltip(StringBuilder builder, CrystalData crystalData)
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

        // Modifiers
        VaultItemsHandler.parseModifiers(builder, crystalData.getModifiers());
    }


    /**
     * This method parses VaultTrinket item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Trinket Item Stack.
     * @param itemTag The item tag data.
     */
    public static void handleTrinketTooltip(StringBuilder builder, ItemStack itemStack, CompoundTag itemTag)
    {
        // I do not want to include Botania in dependencies. This is workaround.

        AttributeGearData data = AttributeGearData.read(itemStack);

        if (data.getFirstValue(ModGearAttributes.STATE).orElse(VaultGearState.UNIDENTIFIED) == VaultGearState.IDENTIFIED)
        {
            int totalUses = itemTag.getInt("vaultUses");
            int used = itemTag.getList("usedVaults", 10).size();
            int remaining = Math.max(totalUses - used, 0);

            builder.append("**Uses:** ").append(String.valueOf(remaining)).append("\n");

            data.getFirstValue(ModGearAttributes.CRAFTED_BY).ifPresent(crafter ->
                builder.append("**Crafted by:** ").append(crafter).append("\n"));

            data.getFirstValue(ModGearAttributes.TRINKET_EFFECT).ifPresent(effect -> {
                TrinketConfig.Trinket trinket = effect.getTrinketConfig();
                builder.append(trinket.getEffectText()).append("\n");
            });

            data.getFirstValue(ModGearAttributes.TRINKET_EFFECT).
                map(TrinketEffect::getConfig).
                filter(TrinketEffect.Config::hasCuriosSlot).
                map(TrinketEffect.Config::getCuriosSlot).
                ifPresent(slot -> {
                    MutableComponent slotTranslation = new TranslatableComponent("curios.slot").append(": ");
                    MutableComponent slotType = new TranslatableComponent("curios.identifier." + slot);

                    builder.append("\n").
                        append(slotTranslation.getString()).
                        append(slotType.getString());
                });
        }
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
            builder.append("**Assembles:** ").append(relicModel.getDisplayName()));
    }


//    /**
//     * This method parses Vault Catalyst item tooltip into discord chat.
//     * @param builder Embed Builder.
//     * @param itemStack Vault Catalyst Item Stack.
//     */
    /**
     * This method parses Infused Catalyst item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Infused Catalyst Item Stack.
     */
    public static void handleInfusedCatalystTooltip(StringBuilder builder, ItemStack itemStack)
    {
        if (InfusedCatalystItem.isSuper(itemStack))
        {
            builder.append("Used to modify your Personal Vault\n");
        }

        if (!InfusedCatalystItem.isSuper(itemStack))
        {
            InfusedCatalystItem.getSize(itemStack).ifPresent(size ->
                builder.append("**Size:** ").append(size).append("\n"));
        }

        List<ResourceLocation> modifiers = InfusedCatalystItem.getModifiers(itemStack);

        if (!modifiers.isEmpty())
        {
            for (ResourceLocation modifierId : modifiers)
            {
                VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
                    builder.append(" - ").append(vaultModifier.getDisplayName()).append("\n"));
            }
        }
    }


    /**
     * This method parses base Catalyst item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Catalyst Item Stack.
     */
    public static void handleBaseCatalystTooltip(StringBuilder builder, ItemStack itemStack)
    {
        builder.append("Use in a Crystal Workbench to modify/upgrade a Vault Crystal\n");
    }


    public static void handleCompanionRelicTooltip(StringBuilder builder, ItemStack itemStack)
    {
        builder.append("Use in a Companion Home to add a modifier to your companion.\n");

        List<ResourceLocation> modifiers = CompanionRelicItem.getModifiers(itemStack);

        if (!modifiers.isEmpty())
        {
            for (ResourceLocation modifierId : modifiers)
            {
                VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
                    builder.append(" ").append(DOT).append(" ").append(vaultModifier.getDisplayName()).append("\n"));
            }
        }
    }


    public static void handleJewelTooltip(StringBuilder builder, ItemStack itemStack)
    {
        VaultGearData data = VaultGearData.read(itemStack);
        VaultGearState state = data.getState();

        builder.append("**Level:** ").append(data.getItemLevel()).append("\n");

        if (state == VaultGearState.IDENTIFIED)
        {
            data.getFirstValue(ModGearAttributes.JEWEL_SIZE).ifPresent(size ->
                builder.append(size).append(" Size\n"));

            for (VaultGearModifier.AffixType affixType : VaultGearModifier.AffixType.values())
            {
                for (VaultGearModifier<?> modifier : data.getModifiers(affixType))
                {
                    VaultItemsHandler.addAffix(builder, modifier, data, affixType, itemStack);
                }
            }
        }
    }


    public static void handleSigilTooltip(StringBuilder builder, ItemStack itemStack)
    {
        SigilItem.readSigil(itemStack).ifPresent(sigil ->
        {
            builder.append("**Sigil:** ").append(sigil.getDisplayName()).append("\n");
            builder.append("+").append(PERCENT_FORMAT.format(sigil.getDifficulty())).append(" Difficulty\n");

            SigilConfig sigilConfig = ModConfigs.SIGIL.get(sigil.getId());

            if (sigilConfig != null)
            {
                int crateTiers = sigilConfig.getLevel(Integer.MAX_VALUE).getExtraCrateTiers();

                if (crateTiers > 0)
                {
                    builder.append("+").append(crateTiers).append(" Crate Tiers\n");
                }
            }
        });
    }


    public static void handleDeckSocketTooltip(StringBuilder builder, ItemStack itemStack)
    {
        DeckSocketItem.getDeckModifier(itemStack).ifPresent(modifier ->
        {
            float value = modifier.getModifierValue();

            if (modifier instanceof StatEfficiencyDeckModifier)
            {
                builder.append(String.format("+%.1f%% Stat card efficiency per unique deck color", value * 100f)).append("\n");
            }
            else if (modifier instanceof NonFoilEfficiencyDeckModifier)
            {
                builder.append(String.format("+%.1f%% Efficiency to all Stat and Evolution cards per non-foil card in the deck", value * 100f)).append("\n");
            }
            else if (modifier instanceof ResourceDoubleDeckModifier)
            {
                builder.append(String.format("+%.0f%% chance for Resource cards to double rewards", value * 100f)).append("\n");
            }
            else if (modifier instanceof ResourceRequirementDeckModifier)
            {
                builder.append(String.format("-%.0f%% Resource card requirements", value * 100f)).append("\n");
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
                builder.append(String.format("+%.0f%% efficiency to %s cards", value * 100f, target)).append("\n");
            }
            else if (modifier instanceof BountyDeckModifier bountyModifier)
            {
                int tierIncrease = (int) Math.ceil(bountyModifier.getTierIncrease());
                CompoundTag nbt = modifier.writeNbt().orElse(new CompoundTag());
                int resourcesNeeded = nbt.getInt("resourcesNeeded");
                builder.append(String.format("+%d Crate %s Completing a vault with at least %d Resource %s",
                    tierIncrease, tierIncrease == 1 ? "Tier" : "Tiers",
                    resourcesNeeded, resourcesNeeded == 1 ? "Card" : "Cards")).append("\n");
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
                builder.append(String.format("+%.0f%% efficiency for %d %s %s",
                    value * 100f, slotRoll, target, slotRoll == 1 ? "card" : "cards")).append("\n");
            }
        });
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
     * This method adds Vault Augment Item description the discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault augment item.
     */
    public static void handleAugmentTooltip(StringBuilder builder, ItemStack itemStack)
    {
        builder.append("**Theme:** ");
        AugmentItem.getTheme(itemStack).ifPresentOrElse(
            (key) -> builder.append(key.getName()),
            () -> builder.append("???"));
    }



    @SuppressWarnings("deprecation")
    public static void handleCharmTooltip(StringBuilder builder, ItemStack itemStack)
    {
        int totalUses = CharmItem.getUses(itemStack);
        int used = CharmItem.getUsedVaults(itemStack).size();
        int remaining = Math.max(totalUses - used, 0);

        builder.append("**Uses:** ").append(remaining).append("\n");

        AttributeGearData data = AttributeGearData.read(itemStack);

        data.getFirstValue(ModGearAttributes.CHARM_EFFECT).ifPresent((charmEffect) ->
        {
            DecimalModifierReader.Percentage<?> percentage = (DecimalModifierReader.Percentage<?>)
                ((CharmEffect.Config<?>) charmEffect.getCharmConfig().getConfig()).getAttribute().getReader();

            int value = Math.round(CharmItem.getValue(itemStack) * 100.0F);

            builder.append(value).append("% ").append(percentage.getModifierName()).append("\n");
            builder.append("**Size:** ").append("% ").append(10).append("\n");
            builder.append("\n");
        });

        builder.append("**Slot:** ").
            append(new TranslatableComponent("curios.identifier.charm").getString()).
            append("\n");
    }


    public static void handleFlaskTooltip(StringBuilder builder, ItemStack itemStack)
    {
        String abilityStr = ItemRespecFlask.getAbility(itemStack);

        if (abilityStr != null)
        {
            ModConfigs.ABILITIES.getAbilityById(abilityStr).ifPresent((grp) ->
            {
                builder.append("Use to remove selected specialization").
                    append("\n").
                    append("of ability ").
                    append(grp.getName());
            });
        }
    }


    public static void handleFocusTooltip(StringBuilder builder, ItemStack itemStack)
    {
        if (ModConfigs.isInitialized())
        {
            builder.append(new TranslatableComponent("the_vault.gear_modification.information").getString());
            GearModificationItemAccessor accessor = (GearModificationItemAccessor) itemStack.getItem();

            accessor.getModification().getDescription(itemStack).
                forEach(component -> builder.append(component.getString()).append("\n"));
        }

        if (itemStack.getItem() instanceof ReforgeTagModificationFocus)
        {
            VaultGearTagConfig.ModTagGroup group = ReforgeTagModificationFocus.getModifierTag(itemStack);

            if (group != null)
            {
                getAttributes(group).forEach((attribute, items) ->
                {
                    VaultGearModifierReader<?> reader = attribute.getReader();
                    builder.append(" - ").append(reader.getModifierName()).append("\n");
                    String text = ReforgeTagModificationFocusInvoker.invokeGetItemsDisplay(items);
                    builder.append("(").append(text).append(")").append("\n");
                });
            }
        }
    }


    public static void handleTitleScrollTooltip(StringBuilder builder, ItemStack itemStack)
    {
        String abilityStr = ItemRespecFlask.getAbility(itemStack);

        if (abilityStr != null)
        {
            ModConfigs.ABILITIES.getAbilityById(abilityStr).ifPresent((grp) ->
            {
                builder.append("Use to remove selected specialization").
                    append("\n").
                    append("of ability ").
                    append(grp.getName());
            });
        }

        TitleScrollItem.getOwnerUUID(itemStack).ifPresentOrElse(uuid ->
            {
                String name = TitleScrollItem.getOwnerName(itemStack).orElse("Unknown");
                builder.append("Player: ").append(name).append("\n");
            },
            () -> builder.append("Player: ???").append("\n"));


        PlayerTitlesConfig.Affix affix = TitleScrollItem.getAffix(itemStack).orElse(PlayerTitlesConfig.Affix.PREFIX);
        String tabText = TitleScrollItem.getTabText(itemStack).orElse(null);
        String chatText = TitleScrollItem.getChatText(itemStack).orElse(null);
        String color = TitleScrollItem.getColor(itemStack).orElse(null);

        if (tabText != null || chatText != null)
        {
            PlayerTitlesData.TitleDisplay titleDisplay = new PlayerTitlesData.TitleDisplay(tabText, chatText, color);
            PlayerTitlesData.Entry entry = new PlayerTitlesData.Entry();

            if (affix == PlayerTitlesConfig.Affix.PREFIX)
            {
                entry.setPrefix(titleDisplay);
            }
            else if (affix == PlayerTitlesConfig.Affix.SUFFIX)
            {
                entry.setSuffix(titleDisplay);
            }

            entry.getCustomName(new TextComponent("<Player>"), PlayerTitlesData.Type.TAB_LIST).
                ifPresent(display ->
                    builder.append("**Preview:** ").append(display.getString()).append("\n"));
        }
    }


    public static void handleSoulFlameTooltip(StringBuilder builder, ItemStack itemStack)
    {
        SoulFlameItem.getOwnerUUID(itemStack).ifPresentOrElse(uuid ->
            {
                String name = SoulFlameItem.getOwnerName(itemStack).orElse("Unknown");
                builder.append("Player: ").append(name).append("\n");
            },
            () -> builder.append("Player: ???").append("\n"));

        builder.append("Stacks: ").append(SoulFlameItem.getStacks(itemStack)).append("\n");

        SoulFlameItem.getModifiers(itemStack).ifPresent(modifiers ->
            VaultItemsHandler.parseModifiers(builder, modifiers));
    }


    public static void handleAntiqueTooltip(StringBuilder builder, ItemStack itemStack)
    {
        Antique antique = AntiqueItem.getAntique(itemStack);

        Optional.ofNullable(antique).
            map(Antique::getConfig).
            map(AntiquesConfig.Entry::getInfo).
            filter(info -> info.getRewardDescription() != null && info.getSubtext() != null).
            ifPresent(info ->
            {
                builder.append("\n").append(info.getRewardDescription()).append("\n");
                builder.append(info.getSubtext()).append("\n");

                String author = antique.getAuthorName() != null ? antique.getAuthorName() : "Unknown";

                builder.append("**By:** ").append(author).append("\n");
            });
    }


    public static void handleJewelPouchTooltip(StringBuilder builder, ItemStack itemStack)
    {
        String levelDescription = JewelPouchItem.getStoredLevel(itemStack).map(String::valueOf).orElse("???");
        builder.append("**Level:** ").append(levelDescription).append("\n");
    }


    public static void handleCoinPouchTooltip(StringBuilder builder, ItemStack itemStack)
    {
        ItemStack[] stacks = CoinPouchItem.getContainedStacks(itemStack);
        String[] names = {"Bronze", "Silver", "Gold", "Platinum", "Greed Coins"};

        boolean hasAny = false;
        for (int i = stacks.length - 1; i >= 0; i--)
        {
            int count = stacks[i].getCount();
            if (count > 0)
            {
                builder.append("**").append(names[i]).append(":** ")
                    .append(String.format("%,d", count)).append("\n");
                hasAny = true;
            }
        }

        if (!hasAny)
        {
            builder.append("Empty\n");
        }
    }


    public static void handleShardPouchTooltip(StringBuilder builder, ItemStack itemStack)
    {
        ItemStack shardStack = ItemShardPouch.getContainedStack(itemStack);
        int count = shardStack.isEmpty() ? 0 : shardStack.getCount();

        if (count > 0)
        {
            builder.append("**Soul Shards:** ").append(String.format("%,d", count)).append("\n");
        }
        else
        {
            builder.append("Empty\n");
        }
    }


    public static void handleGreedyMealTooltip(StringBuilder builder, ItemStack itemStack)
    {
        int xpAmount = itemStack.getTag().getInt("GreedXpBurger");

        if (xpAmount > 0)
        {
            builder.append("**Grants ").append(String.format("%,d", xpAmount)).append(" Vault XP**\n");
        }
    }


    public static void handleHeraldTrophyTooltip(StringBuilder builder, ItemStack itemStack)
    {
        CompoundTag blockEntityTag = itemStack.getOrCreateTagElement("BlockEntityTag");

        String ownerName = blockEntityTag.getString("OwnerName");

        if (!ownerName.isEmpty())
        {
            builder.append("**Awarded to:** ").append(ownerName).append("\n");
        }

        int timeTicks = blockEntityTag.getInt("Time");

        if (timeTicks > 0)
        {
            builder.append("**Time:** ").append(VaultItemsHandler.formatTimeString(timeTicks)).append("\n");
        }

        String difficultyKey = blockEntityTag.getString("Difficulty");

        if (!difficultyKey.isEmpty())
        {
            String displayName = difficultyKey.substring(0, 1).toUpperCase() +
                difficultyKey.substring(1).replace("_", " ");
            builder.append("**Difficulty:** ").append(displayName).append("\n");
        }
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void handleCompanionTooltip(StringBuilder builder, ItemStack itemStack)
    {
        CompoundTag tag = itemStack.getTag();

        if (tag == null || !tag.contains("PetName") || !tag.contains("PetSeries"))
        {
            return;
        }

        int level = tag.contains("level") ? tag.getInt("level") : 1;
        builder.append("**Level:** ").append(level).append("\n");

        int hearts = tag.contains("hearts") ? tag.getInt("hearts") : 1;

        if (hearts > 0)
        {
            builder.append("**Health:** ").append("\u2764".repeat(hearts)).append("\n");
        }
        else
        {
            builder.append("**Health:** Retired\n");
        }

        int cooldownSeconds = 0;

        if (tag.contains("cooldownExpiry"))
        {
            long now = System.currentTimeMillis();
            long expiry = tag.getLong("cooldownExpiry");
            cooldownSeconds = (int) Math.max(0L, (expiry - now) / 1000L);
        }

        if (cooldownSeconds <= 0)
        {
            builder.append("**Cooldown:** Ready!\n");
        }
        else
        {
            long remainingTicks = cooldownSeconds * 20L;
            builder.append("**Cooldown:** ").append(VaultItemsHandler.formatTimeString(remainingTicks)).append("\n");
        }

        String seriesRaw = tag.getString("PetSeries");

        if (!seriesRaw.isEmpty())
        {
            String seriesDisplay = seriesRaw.substring(0, 1).toUpperCase() + seriesRaw.substring(1).toLowerCase();
            builder.append("**Series:** ").append(seriesDisplay).append("\n");
        }

        if (tag.contains("temporal"))
        {
            ResourceLocation modifierId = ResourceLocation.tryParse(tag.getString("temporal"));

            if (modifierId != null)
            {
                VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
                {
                    builder.append("\n**Temporal Modifier**\n");
                    builder.append(DOT).append(" ").append(vaultModifier.getDisplayName()).append("\n");
                });
            }
        }

        CompoundTag relicsTag = tag.getCompound("relics");

        if (!relicsTag.isEmpty())
        {
            Map<ResourceLocation, Integer> modifierCounts = new LinkedHashMap<>();

            for (String key : relicsTag.getAllKeys())
            {
                CompoundTag relicData = relicsTag.getCompound(key);
                ListTag modifiersList = relicData.getList("modifiers", 8);

                for (int i = 0; i < modifiersList.size(); i++)
                {
                    ResourceLocation id = ResourceLocation.tryParse(modifiersList.getString(i));

                    if (id != null)
                    {
                        modifierCounts.merge(id, 1, Integer::sum);
                    }
                }
            }

            if (!modifierCounts.isEmpty())
            {
                builder.append("\n**Relics**\n");

                modifierCounts.forEach((id, count) ->
                    VaultModifierRegistry.getOpt(id).ifPresent(vaultModifier ->
                    {
                        if (count > 1)
                        {
                            builder.append(DOT).append(" ").append(count).append("x ")
                                .append(vaultModifier.getDisplayName()).append("\n");
                        }
                        else
                        {
                            builder.append(DOT).append(" ").append(vaultModifier.getDisplayName()).append("\n");
                        }
                    }));
            }
        }
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
        CompoundTag tag = itemStack.getTag();

        if (tag == null || !tag.contains("stacks"))
        {
            builder.append("Empty\n");
            return;
        }

        ListTag stacksTag = tag.getList("stacks", 10);
        List<Map.Entry<String, Integer>> contents = new ArrayList<>();

        for (int i = 0; i < stacksTag.size(); i++)
        {
            CompoundTag entry = stacksTag.getCompound(i);
            int amount = entry.getInt("amount");

            if (amount <= 0)
            {
                continue;
            }

            CompoundTag stackTag = entry.getCompound("stack");
            String itemId = stackTag.getString("id");
            ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);

            if (resourceLocation != null)
            {
                Item keyItem = ForgeRegistries.ITEMS.getValue(resourceLocation);

                if (keyItem != null)
                {
                    String displayName = new ItemStack(keyItem).getHoverName().getString();
                    contents.add(Map.entry(displayName, amount));
                }
            }
        }

        if (contents.isEmpty())
        {
            builder.append("Empty\n");
            return;
        }

        builder.append("**Contains:**\n");

        for (Map.Entry<String, Integer> entry : contents)
        {
            builder.append(DOT).append(" ").append(entry.getValue()).append("x ")
                .append(entry.getKey()).append("\n");
        }
    }


    public static void handleCardTooltip(StringBuilder builder, ItemStack itemStack)
    {
        Card card = CardItem.getCard(itemStack);

        builder.append("**Tier:** ").append(card.getTier()).append("\n");

        List<CardEntry.Color> colors = new ArrayList<>(card.getColors());

        if (!colors.isEmpty())
        {
            builder.append("**Color:** ");

            for (int i = 0; i < colors.size(); ++i)
            {
                builder.append(colors.get(i).getColoredText().getString());

                if (i != colors.size() - 1)
                {
                    builder.append(", ");
                }
            }

            builder.append("\n");
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
            builder.append("**Type:** ");

            for (int i = 0; i < types.size(); ++i)
            {
                builder.append(types.get(i));

                if (i != types.size() - 1)
                {
                    builder.append(", ");
                }
            }

            builder.append("\n");
        }

        if (!groups.isEmpty())
        {
            builder.append("**Groups:** ");

            for (int i = 0; i < groups.size(); ++i)
            {
                builder.append(groups.get(i));

                if (i != groups.size() - 1)
                {
                    builder.append(", ");
                }
            }

            builder.append("\n");
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


    public static void handleModifierScrollTooltip(StringBuilder builder, ItemStack itemStack)
    {
        builder.append("**Opened by** ").append(ModifierScrollItem.getPlayerName(itemStack)).append("\n");
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

            int socketCount = deck.getSocketCount();

            if (socketCount > 0)
            {
                int filledSockets = deck.getModifiers().size();
                int emptySockets = socketCount - filledSockets;

                builder.append("**Sockets:** ").
                    append(createDots(filledSockets, "\u2B22")).
                    append(createDots(emptySockets, "\u2B21")).
                    append("\n");

                if (filledSockets > 0)
                {
                    builder.append("**Modifiers:**\n");

                    for (DeckModifier<?> modifier : deck.getModifiers())
                    {
                        String name = modifier.getName();

                        builder.append(" ").append(DOT).append(" ").
                            append(name != null ? name : "Unknown").
                            append("\n");
                    }
                }
            }
        });
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

                builder.append(name.getString()).append(" (");

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
                    builder.append(minMaxRangeCmp.getString()).append(",").append(" ");
                }

                MutableComponent rangeCmp = uncheckedGetConfigRangeDisplay(
                    modifier.getAttribute().getGenerator(),
                    modifier.getAttribute().getReader(),
                    modifier.getConfig().getConfig(tier));

                CardEntry.getTier(accessor.getValues(), tier).ifPresent((t) ->
                {
                    if (rangeCmp != null)
                    {
                        builder.append("T%s: ".formatted(t)).append(rangeCmp.getString());
                    }
                });

                builder.append(")").append("\n");
            });
        }
        else
        {
            List<Component> components = new ArrayList<>();
            property.addText(components, 0, TooltipFlag.Default.ADVANCED, 0f, tier);

            components.forEach(component -> builder.append(component.getString()).append("\n"));
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
                String prefix = "";

                String suffix = "";

                if (modifier.hasCategory(VaultGearModifier.AffixCategory.LEGENDARY))
                {
                    prefix = LEGENDARY_SYMBOL + " ";
                }

                if (modifier.hasCategory(VaultGearModifier.AffixCategory.GREATER))
                {
                    prefix = GREATER_SYMBOL + " ";
                }

                if (modifier.hasCategory(VaultGearModifier.AffixCategory.IMBUED))
                {
                    prefix = IMBUED_SYMBOL + " ";
                }

                if (modifier.hasCategory(VaultGearModifier.AffixCategory.CORRUPTED))
                {
                    prefix = CORRUPTED_OPEN + " ";
                    suffix = " " + CORRUPTED_CLOSE;
                }

                if (modifier.hasCategory(VaultGearModifier.AffixCategory.FROZEN))
                {
                    prefix = FROZEN_OPEN + " ";
                    suffix = " " + FROZEN_CLOSE;
                }

                return prefix + text.getString() + suffix;
            }).
            ifPresent(text ->
            {
                MutableComponent tierDisplay = VaultGearTierConfig.getConfig(stack).map((tierConfig) ->
                {
                    Object config = tierConfig.getTierConfig(modifier);

                    if (config != null)
                    {
                        return modifier.getAttribute().getGenerator().getConfigDisplay(
                            modifier.getAttribute().getReader(),
                            config);
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
        return (SQUARE + " empty %s").formatted(type.name().toLowerCase(Locale.ROOT));
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
     * This method adds crystal modifier data to the discord embed based on given filter.
     * @param builder Builder that need to be populated.
     * @param data Crystal Data object.
     * @param header Header of elements.
     * @param filter Filter for modifiers.
     */
    private static void populateCatalystInformation(StringBuilder builder,
        List<VaultModifierStack> data,
        String header,
        Predicate<VaultModifierStack> filter)
    {
        List<VaultModifierStack> modifierList = data.stream().filter(filter).toList();

        if (!modifierList.isEmpty())
        {
            builder.append(header).append("\n");

            for (VaultModifierStack modifierStack : modifierList)
            {
                VaultModifier<?> vaultModifier = modifierStack.getModifier();
                String formattedName = vaultModifier.getDisplayNameFormatted(modifierStack.getSize());

                builder.append("  ").
                    append("%dx".formatted(modifierStack.getSize())).
                    append(formattedName).
                    append("\n");
            }
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
        builder.append("**Layout**: ");

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
     * This method parses crystal modifiers.
     * @param builder Builder that need to be populated.
     * @param modifiers The object that contains all crystal modifiers.
     */
    private static void parseModifiers(StringBuilder builder, CrystalModifiers modifiers)
    {
        List<VaultModifierStack> modifierList = new ArrayList<>();

        for (VaultModifierStack modifier : modifiers)
        {
            modifierList.add(modifier);
        }

        if (modifiers instanceof ParadoxCrystalModifiers)
        {
            VaultItemsHandler.populateCatalystInformation(builder,
                modifierList,
                "**Cursed:**",
                catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isCurse(catalyst.getModifierId()));

            VaultItemsHandler.populateCatalystInformation(builder,
                modifierList,
                "**Positive Modifiers:**",
                catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isGood(catalyst.getModifierId()));

            VaultItemsHandler.populateCatalystInformation(builder,
                modifierList,
                "**Negative Modifiers:**",
                catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isBad(catalyst.getModifierId()));

            VaultItemsHandler.populateCatalystInformation(builder,
                modifierList,
                "**Other Modifiers:**",
                catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isUnlisted(catalyst.getModifierId()));
        }
        else
        {
            if (modifiers.hasClarity())
            {
                builder.append("*Clarity*\n");
            }

            int curseCount = modifiers.getCurseCount();

            if (curseCount > 0)
            {
                if (modifiers.hasClarity())
                {
                    VaultItemsHandler.populateCatalystInformation(builder,
                        modifierList,
                        "**Cursed:**",
                        catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isCurse(catalyst.getModifierId()));
                }
                else
                {
                    builder.append("**Cursed** ").
                        append(CURSE.repeat(curseCount)).
                        append("\n");
                }
            }

            VaultItemsHandler.populateCatalystInformation(builder,
                modifierList,
                "**Positive Modifiers:**",
                catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isGood(catalyst.getModifierId()));

            VaultItemsHandler.populateCatalystInformation(builder,
                modifierList,
                "**Negative Modifiers:**",
                catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isBad(catalyst.getModifierId()));

            VaultItemsHandler.populateCatalystInformation(builder,
                modifierList,
                "**Other Modifiers:**",
                catalyst -> ModConfigs.VAULT_CRYSTAL_CATALYST.isUnlisted(catalyst.getModifierId()));
        }
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


    private static Map<VaultGearAttribute<?>, List<Item>> getAttributes(VaultGearTagConfig.ModTagGroup tagGroup)
    {
        Map<VaultGearAttribute<?>, List<Item>> attributes = new LinkedHashMap<>();

        ModConfigs.VAULT_GEAR_CONFIG.forEach((item, config) -> tagGroup.getTags().
            forEach(tag -> config.getGenericGroupsWithModifierTag(tag).
                forEach(tpl ->
                {
                    VaultGearAttribute<?> attribute = VaultGearAttributeRegistry.getAttribute(tpl.getB().getAttribute());

                    ForgeRegistries.ITEMS.getHolder(item).ifPresent(holder ->
                        attributes.computeIfAbsent(attribute, a -> new ArrayList<>()).
                            add(holder.value()));
                })));

        return attributes;
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

    private static final String LEGENDARY_SYMBOL = "\u2726";

    private static final String GREATER_SYMBOL = "\u29EB";

    private static final String IMBUED_SYMBOL = "\u2724";

    private static final String CORRUPTED_OPEN = "\u3010";

    private static final String CORRUPTED_CLOSE = "\u3011";

    private static final String FROZEN_OPEN = "\u276E";

    private static final String FROZEN_CLOSE = "\u276F";

    private static final String CURSE = "\u2620";

    /**
     * Symbol for text fields.
     */
    private static final String DOT = "\u2022";

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
