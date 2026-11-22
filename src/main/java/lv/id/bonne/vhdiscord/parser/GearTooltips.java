//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.mojang.datafixers.util.Pair;

import iskallia.vault.config.UniqueGearConfig;
import iskallia.vault.config.gear.VaultEtchingConfig;
import iskallia.vault.config.gear.VaultGearTierConfig;
import iskallia.vault.core.vault.influence.VaultGod;
import iskallia.vault.core.vault.modifier.registry.VaultModifierRegistry;
import iskallia.vault.dynamodel.model.armor.ArmorPieceModel;
import iskallia.vault.gear.VaultGearState;
import iskallia.vault.gear.VaultGearType;
import iskallia.vault.gear.attribute.VaultGearAttribute;
import iskallia.vault.gear.attribute.VaultGearAttributeInstance;
import iskallia.vault.gear.attribute.VaultGearModifier;
import iskallia.vault.gear.data.VaultGearData;
import iskallia.vault.gear.etching.EtchingHelper;
import iskallia.vault.gear.etching.EtchingTooltipLines;
import iskallia.vault.gear.item.VaultGearItem;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.init.ModDynamicModels;
import iskallia.vault.greed.house.HouseItems;
import iskallia.vault.init.ModGearAttributes;
import iskallia.vault.item.data.InscriptionData;
import iskallia.vault.item.gear.IVaultUsesItem;
import iskallia.vault.item.gear.VaultCharmItem;
import iskallia.vault.item.gear.VaultNecklaceItem;
import iskallia.vault.item.gear.VaultUsesHelper;
import iskallia.vault.item.gear.VoidStoneItem;
import iskallia.vault.item.tool.JewelItem;
import iskallia.vault.skill.prestige.helper.PrestigeHelper;
import iskallia.vault.util.SidedHelper;
import iskallia.vault.util.TextUtil;
import iskallia.vault.util.Throuple;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;


/**
 * Tooltips of Vault Hunters gear (weapons, armour, jewels, necklaces, void stones and god charms) with their affixes,
 * repairs and etching, and of etchings and inscriptions.
 */
final class GearTooltips
{
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
    private static final int SLOT_COLOUR = 0xE2E28A;

    private static final int DURABILITY_COLOUR = 0xAAAAAA;

    private static final int BROKEN_COLOUR = 0xFF5555;

    private static final int IMBUED_COLOUR = 0xA821FC;

    private static final int SEALED_COLOUR = 0xC2FF7A;

    private static final int CORRUPTED_COLOUR = 0x780004;

    private static final int INSCRIPTION_COMPLETION_COLOUR = 0x48BAF8;

    /**
     * An etching description placeholder for an effect name, e.g. %svalue/effect.
     */
    private static final Pattern ETCHING_EFFECT_PLACEHOLDER = Pattern.compile("%s([\\w$]+)/effect(?![\\w$/])");


    private GearTooltips()
    {
    }


    /**
     * This method parses gear tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Gear ItemStack.
     */
    static void handleGearTooltip(StringBuilder builder, ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        VaultGearData data = VaultGearData.read(itemStack);
        VaultGearState state = data.getState();
        boolean jewel = itemStack.getItem() instanceof JewelItem;

        if (itemStack.getItem() instanceof VaultCharmItem)
        {
            if (LegacyVaultItemApi.hasFixedCharmValues())
            {
                GearTooltips.handleFixedGodCharmTooltip(builder, itemStack, data, state);
            }
            else
            {
                GearTooltips.handleGodCharmTooltip(builder, itemStack, data, state, linkingPlayer);
            }

            return;
        }

        if (itemStack.getItem() instanceof VoidStoneItem && ModList.get().isLoaded(TooltipLines.WOLDS_VAULTS_MOD_ID))
        {
            return;
        }

        GearTooltips.appendGearUsesLines(builder, itemStack, state);

        // Add unique item lore
        data.getFirstValue(ModGearAttributes.UNIQUE_ITEM_KEY).
            flatMap(ModConfigs.UNIQUE_GEAR::getEntry).
            map(UniqueGearConfig.Entry::getLore).
            filter(lore -> !lore.isBlank()).
            ifPresent(lore -> builder.append("*").append(TextUtil.applyColorTags(lore).getString().strip()).append("*\n"));

        // Add gear Level
        builder.append("**Level:** ").append(data.getItemLevel()).append("\n");
        TooltipColorHints.recordValue("Level", TooltipLines.LEVEL_COLOUR);

        // Add crafter name
        data.getFirstValue(ModGearAttributes.CRAFTED_BY).ifPresent(crafter ->
        {
            builder.append("**Crafted by:** ").append(crafter).append("\n");
            TooltipColorHints.recordValue("Crafted by", TooltipLines.CRAFTER_COLOUR);
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
                builder.append(GearTooltips.createRepairText(usedRepairs, totalRepairs)).append("\n");
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
                    String etchingDesc = GearTooltips.etchingDescription(etchingEntry.getDescription(), itemStack);
                    etchingDesc = etchingDesc.replaceAll("<[^>]+>", "");
                    builder.append("**Etching:** ").append(etchingEntry.getName()).append("\n");
                    builder.append(etchingDesc).append("\n");
                }
            });

            // Add Implicits
            List<VaultGearModifier<?>> implicits = data.getModifiers(VaultGearModifier.AffixType.IMPLICIT);

            if (!implicits.isEmpty())
            {
                GearTooltips.addAffixList(builder, data, VaultGearModifier.AffixType.IMPLICIT, itemStack);
                builder.append("\n");
            }

            int maxPrefixes = data.getFirstValue(ModGearAttributes.PREFIXES).orElse(0);
            List<VaultGearModifier<?>> prefixes = data.getModifiers(VaultGearModifier.AffixType.PREFIX);

            if (maxPrefixes > 0 || !prefixes.isEmpty())
            {
                GearTooltips.addAffixList(builder, data, VaultGearModifier.AffixType.PREFIX, itemStack);
                builder.append("\n");
            }

            int maxSuffixes = data.getFirstValue(ModGearAttributes.SUFFIXES).orElse(0);
            List<VaultGearModifier<?>> suffixes = data.getModifiers(VaultGearModifier.AffixType.SUFFIX);

            if (maxSuffixes > 0 || !suffixes.isEmpty())
            {
                GearTooltips.addAffixList(builder, data, VaultGearModifier.AffixType.SUFFIX, itemStack);
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
     * Writes a god charm's tooltip as Vault Hunters 7209 draws it without Shift, for the linking player: the uses left
     * (hidden for a player with the immortal charms prestige power), the god, then the roll of an unidentified charm, or
     * the modifiers of an identified one scaled by the player's reputation with that god and a closing reputation line.
     *
     * @param builder Embed Builder.
     * @param itemStack the god charm.
     * @param data the charm's gear data.
     * @param state the charm's identification state.
     * @param linkingPlayer the player who linked the charm, or null when unknown (values as rolled, reputation 0).
     */
    private static void handleGodCharmTooltip(StringBuilder builder,
        ItemStack itemStack,
        VaultGearData data,
        VaultGearState state,
        ServerPlayer linkingPlayer)
    {
        boolean immortalCharms = linkingPlayer != null && VaultCharmItem.hasImmortalCharms(PrestigeHelper.getPrestige(linkingPlayer));

        if (state == VaultGearState.IDENTIFIED && !immortalCharms)
        {
            GearTooltips.appendCharmUsesLine(builder, itemStack);
        }

        Optional<VaultGod> god = VaultCharmItem.getGod(itemStack);
        god.ifPresent(vaultGod -> GearTooltips.appendCharmGodLine(builder, vaultGod));

        if (state == VaultGearState.UNIDENTIFIED)
        {
            GearTooltips.appendCharmRollLine(builder, data);
            return;
        }

        if (state != VaultGearState.IDENTIFIED || god.isEmpty())
        {
            return;
        }

        int reputation = linkingPlayer == null ? 0 : SidedHelper.getGodReputation(linkingPlayer, god.get()).orElse(0);
        float liveScale = linkingPlayer == null ? 1.0F : VaultCharmItem.getLiveScale(itemStack, reputation);

        data.getModifiers(VaultGearModifier.AffixType.PREFIX).forEach(modifier ->
            GearTooltips.addAffix(builder,
                VaultCharmItem.scaleModifier(modifier, liveScale),
                data,
                VaultGearModifier.AffixType.PREFIX,
                itemStack));

        TooltipLines.appendGameLine(builder, new TextComponent(god.get().getName() + " reputation: ").withStyle(ChatFormatting.GRAY).
            append(new TextComponent(String.valueOf(reputation)).withStyle(ChatFormatting.WHITE)).
            append(new TextComponent(" / 50").withStyle(ChatFormatting.GRAY)));
    }


    private static void appendCharmUsesLine(StringBuilder builder, ItemStack itemStack)
    {
        int remaining = Math.max(VaultUsesHelper.getUses(itemStack) - VaultUsesHelper.getUsedVaults(itemStack).size(), 0);
        TooltipLines.appendGameLine(builder, new TextComponent("Uses: ").withStyle(ChatFormatting.GRAY).
            append(new TextComponent(String.valueOf(remaining)).withStyle(ChatFormatting.WHITE)));
    }


    private static void appendCharmGodLine(StringBuilder builder, VaultGod god)
    {
        TooltipLines.appendGameLine(builder, new TextComponent("Vault God: ").
            append(new TextComponent(god.getName()).withStyle(god.getChatColor())));
    }


    private static void appendCharmRollLine(StringBuilder builder, VaultGearData data)
    {
        data.getFirstValue(ModGearAttributes.GEAR_ROLL_TYPE).
            flatMap(ModConfigs.VAULT_GEAR_TYPE_CONFIG::getRollPool).
            ifPresent(pool ->
            {
                builder.append("**Roll:** ").append(pool.getName()).append("\n");
                TooltipColorHints.recordValue("Roll", pool.getColor());
            });
    }


    /**
     * Writes a god charm's tooltip as Vault Hunters before 7209 (Wold's Vaults) draws it without Shift: the uses left, its
     * reputation and god, then the roll of an unidentified charm or the modifiers of an identified one (on Wold's Vaults
     * the implicits first, without a header). The game hides the uses from players with the matching prestige power,
     * which is not known here.
     *
     * @param builder Embed Builder.
     * @param itemStack the god charm.
     * @param data the charm's gear data.
     * @param state the charm's identification state.
     */
    private static void handleFixedGodCharmTooltip(StringBuilder builder, ItemStack itemStack, VaultGearData data, VaultGearState state)
    {
        if (state == VaultGearState.IDENTIFIED)
        {
            GearTooltips.appendCharmUsesLine(builder, itemStack);
        }

        TooltipLines.appendGameLine(builder, new TextComponent("Reputation: ").withStyle(ChatFormatting.GRAY).
            append(new TextComponent(String.valueOf(VaultCharmItem.getGodReputation(itemStack))).withStyle(ChatFormatting.WHITE)));
        VaultCharmItem.getGod(itemStack).ifPresent(god -> GearTooltips.appendCharmGodLine(builder, god));

        if (state == VaultGearState.UNIDENTIFIED)
        {
            GearTooltips.appendCharmRollLine(builder, data);
            return;
        }

        if (ModList.get().isLoaded(TooltipLines.WOLDS_VAULTS_MOD_ID))
        {
            data.getModifiers(VaultGearModifier.AffixType.IMPLICIT).forEach(modifier ->
                GearTooltips.addAffix(builder, modifier, data, VaultGearModifier.AffixType.IMPLICIT, itemStack));
        }

        data.getModifiers(VaultGearModifier.AffixType.PREFIX).forEach(modifier ->
            GearTooltips.addAffix(builder, modifier, data, VaultGearModifier.AffixType.PREFIX, itemStack));
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
                TooltipLines.appendGameLine(builder, GearTooltips.usesLine(necklace, itemStack));
            }
            else
            {
                TooltipLines.appendStyledLine(builder,
                    "Right click to identify",
                    new TextComponent("Right click to identify").withStyle(ChatFormatting.GRAY));
            }
        }
        else if (itemStack.getItem() instanceof VoidStoneItem voidStone && identified)
        {
            TooltipLines.appendGameLine(builder, GearTooltips.usesLine(voidStone, itemStack));
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
     * This method parses Etching item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Etching Item Stack.
     */
    static void handleEtchingTooltip(StringBuilder builder, ItemStack itemStack)
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
            TooltipLines.appendStyledLine(builder,
                "No Etching Applied",
                new TextComponent("No Etching Applied").withStyle(ChatFormatting.GRAY));
            return;
        }

        VaultEtchingConfig.EtchingEntry entry = ModConfigs.ETCHINGS.getEtchingConfig(etchingId);

        if (entry == null)
        {
            TooltipLines.appendStyledLine(builder,
                "Unknown Etching",
                new TextComponent("Unknown Etching").withStyle(ChatFormatting.RED));
            return;
        }

        if (LegacyVaultItemApi.hasPlainEtchingLines())
        {
            GearTooltips.appendPlainEtchingLines(builder, itemStack, data, entry);
        }
        else
        {
            GearTooltips.appendEtchingLines(builder, itemStack, data, entry);
        }
    }


    /**
     * An identified etching's lines as Vault Hunters 7209 draws them without Shift: its greed tier, the description, the
     * gear it goes on and the Etching Table lines.
     *
     * @param builder Embed Builder.
     * @param itemStack the etching.
     * @param data the etching's gear data.
     * @param entry the etching's config entry.
     */
    private static void appendEtchingLines(StringBuilder builder, ItemStack itemStack, VaultGearData data, VaultEtchingConfig.EtchingEntry entry)
    {
        TooltipLines.appendTextLine(builder, EtchingTooltipLines.tier(data.getFirstValue(ModGearAttributes.GREED_TIER).orElse(0)));
        builder.append("\n");

        Component description = TextUtil.applyColorTags(GearTooltips.etchingDescription(entry.getDescription(), itemStack));
        TooltipLines.appendStyledLine(builder, description.getString(), description);
        builder.append("\n");

        TooltipLines.appendGameLine(builder, new TextComponent("Goes on: ").withStyle(ChatFormatting.GRAY).
            append(new TextComponent(GearTooltips.etchingGearTypes(entry)).withStyle(ChatFormatting.WHITE)));
        TooltipLines.appendTextLine(builder, new TextComponent("Apply at the Etching Table").withStyle(ChatFormatting.DARK_GRAY));

        if (HouseItems.isBoughtEtching(itemStack))
        {
            TooltipLines.appendTextLine(builder, new TextComponent("Bought at the Etching Table").withStyle(ChatFormatting.DARK_GRAY));
        }

        int rerolls = HouseItems.rerollsOf(itemStack);

        if (rerolls > 0)
        {
            TooltipLines.appendTextLine(builder,
                new TextComponent("Rerolled " + rerolls + (rerolls == 1 ? " time" : " times") + " at Mr Greedy").withStyle(ChatFormatting.DARK_GRAY));
        }
    }


    /**
     * The gear an etching goes on, as the game names it: the display names of every gear type in its groups.
     *
     * @param entry the etching's config entry.
     * @return the names, or "Any gear" for an etching without groups.
     */
    private static String etchingGearTypes(VaultEtchingConfig.EtchingEntry entry)
    {
        return entry.getTypeGroups().isEmpty() ? "Any gear" : entry.getTypeGroups().stream().
            flatMap(group -> ModConfigs.ETCHINGS.getGroup(group).stream()).
            map(VaultGearType::getDisplayName).
            distinct().
            collect(Collectors.joining(", "));
    }


    /**
     * An identified etching's lines as Vault Hunters before 7209 (Wold's Vaults) draws them: its greed tier, the
     * description and its gear type groups.
     *
     * @param builder Embed Builder.
     * @param itemStack the etching.
     * @param data the etching's gear data.
     * @param entry the etching's config entry.
     */
    private static void appendPlainEtchingLines(StringBuilder builder, ItemStack itemStack, VaultGearData data, VaultEtchingConfig.EtchingEntry entry)
    {
        GearTooltips.greedTierOf(data).ifPresent(greedTier ->
            TooltipLines.appendGameLine(builder, new TextComponent("Greed Tier: ").
                append(new TextComponent(String.valueOf(greedTier)).withStyle(Style.EMPTY.withColor(TooltipLines.LEVEL_COLOUR)))));

        Component description = TextUtil.applyColorTags(GearTooltips.etchingDescription(entry.getDescription(), itemStack));
        TooltipLines.appendStyledLine(builder, description.getString(), description);
        builder.append("\n");

        TooltipLines.appendGameLine(builder, new TextComponent("Type: ").withStyle(ChatFormatting.GRAY).
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
     * The greed tier an etching needs.
     *
     * @param data the etching's gear data.
     * @return the tier, or empty when the etching has none.
     */
    private static Optional<Integer> greedTierOf(VaultGearData data)
    {
        return data.getAttributes().stream().
            filter(instance -> instance.getAttribute() == ModGearAttributes.GREED_TIER).
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
    static void handleInscriptionTooltip(StringBuilder builder, ItemStack itemStack)
    {
        InscriptionData data = InscriptionData.from(itemStack);
        CompoundTag compoundTag = data.serializeNBT();

        if (compoundTag.contains("completion"))
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Completion: ").
                append(new TextComponent(Math.round(compoundTag.getFloat("completion") * 100.0F) + "%").
                    withStyle(Style.EMPTY.withColor(INSCRIPTION_COMPLETION_COLOUR))));
        }

        if (data.getTime() != null)
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Time: ").
                append(new TextComponent(TooltipLines.formatTimeString(data.getTime())).withStyle(ChatFormatting.GRAY)));
        }

        if (compoundTag.contains("instability"))
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Instability: ").
                append(new TextComponent("%.1f%%".formatted(compoundTag.getFloat("instability") * 100.0F)).
                    withStyle(ChatFormatting.RED)));
        }

        if (!data.isSuper())
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Size: ").
                append(new TextComponent(String.valueOf(data.getSize())).withStyle(ChatFormatting.RED)));
        }

        for (InscriptionData.Entry entry : data.getEntries())
        {
            String roomStr = entry.count > 1 ? "Rooms" : "Room";
            Component line = new TextComponent(" " + TooltipLines.DOT + " ").withStyle(ChatFormatting.GRAY).
                append(new TextComponent(String.valueOf(entry.count)).withStyle(ChatFormatting.GRAY)).
                append(" ").
                append(entry.toRoomEntry().getName()).
                append(new TextComponent(" " + roomStr).withStyle(ChatFormatting.GRAY));
            TooltipLines.appendStyledLine(builder, line.getString(), line);
        }

        for (ResourceLocation modifierId : data.getModifiers())
        {
            VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
            {
                TextColor textColor = vaultModifier.getDisplayTextColor();
                Component line = new TextComponent(" " + TooltipLines.DOT + " " + vaultModifier.getDisplayName()).
                    withStyle(textColor == null ? Style.EMPTY : Style.EMPTY.withColor(textColor));
                TooltipLines.appendStyledLine(builder, line.getString(), line);
            });
        }

        if (data.getEchoCost() != null)
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Echo Cost: ").
                append(new TextComponent(String.valueOf(data.getEchoCost())).withStyle(ChatFormatting.DARK_PURPLE)));
        }
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
        affixes.forEach(modifier -> GearTooltips.addAffix(builder, modifier, data, type, itemStack));

        if (type != VaultGearModifier.AffixType.IMPLICIT)
        {
            for (int i = 0; i < emptyAffixes - affixes.size(); i++)
            {
                builder.append(GearTooltips.createEmptyAffix(type));
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
            GearTooltips.createDots(usedRepairs, FULL_CIRCLE) +
            GearTooltips.createDots(remaining, EMPTY_CIRCLE);
    }


    /**
     * This method generates a repair dots for gear.
     * @param amount Amount of dots.
     * @param symbol Dot symbol.
     * @return String that contains number of repairs for gear.
     */
    private static String createDots(int amount, String symbol)
    {
        return TooltipLimits.repeat(symbol + " ", amount);
    }
}
