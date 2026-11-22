//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.parser;


import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import iskallia.vault.config.PlayerTitlesConfig;
import iskallia.vault.config.TrinketConfig;
import iskallia.vault.config.VaultItemsConfig;
import iskallia.vault.config.gear.VaultGearTagConfig;
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
import iskallia.vault.core.vault.modifier.registry.VaultModifierRegistry;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.dynamodel.DynamicModel;
import iskallia.vault.dynamodel.model.item.PlainItemModel;
import iskallia.vault.dynamodel.registry.DynamicModelRegistry;
import iskallia.vault.gear.attribute.VaultGearAttributeInstance;
import iskallia.vault.gear.attribute.VaultGearModifier;
import iskallia.vault.gear.attribute.config.ConfigurableAttributeGenerator;
import iskallia.vault.gear.charm.CharmEffect;
import iskallia.vault.gear.data.AttributeGearData;
import iskallia.vault.gear.reader.DecimalModifierReader;
import iskallia.vault.gear.reader.VaultGearModifierReader;
import iskallia.vault.gear.tooltip.VaultGearTooltipItem;
import iskallia.vault.gear.trinket.TrinketEffect;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.init.ModDynamicModels;
import iskallia.vault.init.ModGearAttributes;
import iskallia.vault.init.ModRelics;
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
import iskallia.vault.item.bottle.DrinkMode;
import iskallia.vault.item.bottle.PotionEffect;
import iskallia.vault.item.bottle.PotionEffectDisplay;
import iskallia.vault.item.bottle.PotionRolls;
import iskallia.vault.item.bottle.PotionTiers;
import iskallia.vault.item.crystal.CrystalData;
import iskallia.vault.item.crystal.VaultCrystalItem;
import iskallia.vault.item.gear.CharmItem;
import iskallia.vault.item.gear.EtchingItem;
import iskallia.vault.item.gear.GreedTrinketItem;
import iskallia.vault.item.gear.TrinketItem;
import iskallia.vault.item.modification.GearModificationItem;
import iskallia.vault.item.modification.ReforgeTagModificationFocus;
import iskallia.vault.util.MiscUtils;
import iskallia.vault.util.PlayerRecoveryHelper;
import iskallia.vault.util.TextUtil;
import iskallia.vault.world.VaultDifficulty;
import lv.id.bonne.vhdiscord.vaulthunters.mixin.CardEntryAccessor;
import lv.id.bonne.vhdiscord.vaulthunters.mixin.GearCardModifierAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fml.ModList;
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
                VaultItemsHandler.handleBottleTooltip(builder, itemStack, linkingPlayer);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof EtchingItem)
            {
                GearTooltips.handleEtchingTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultGearTooltipItem)
            {
                GearTooltips.handleGearTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof VaultDollItem)
            {
                VaultItemsHandler.handleDollTooltip(builder, itemTag);
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
                CrystalTooltips.handleAugmentTooltip(builder, itemStack);
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
                VaultItemsHandler.handleCardDeckTooltip(builder, itemStack, linkingPlayer);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof ModifierScrollItem)
            {
                VaultItemsHandler.handleModifierScrollTooltip(builder, itemStack);
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
                VaultItemsHandler.handleCompanionRelicTooltip(builder, itemStack);
                return builder.toString();
            }
            else if (itemStack.getItem() instanceof SigilItem)
            {
                CrystalTooltips.handleSigilTooltip(builder, itemStack);
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
     * Writes a potion bottle's tooltip as the game draws it. Since Vault Hunters 7209 the charges, healing and effects
     * depend on the player, so they are the linking player's; the viewer's key bind line is left out. Older builds
     * (Wold's Vaults) write the bottle's recharge kind, healing and effect, without the recharge progress line the game
     * counts for the viewer.
     * @param builder Embed Builder.
     * @param itemStack Bottle Item.
     * @param linkingPlayer the player who linked the bottle, or null when unknown (no lines on 7209).
     */
    public static void handleBottleTooltip(StringBuilder builder, ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        if (ModConfigs.POTION == null)
        {
            // Potions are not defined.
            return;
        }

        if (LegacyVaultItemApi.hasBottleTypes())
        {
            VaultItemsHandler.appendTypedBottleLines(builder, itemStack);
        }
        else if (linkingPlayer != null && ModConfigs.VAULT_ALCHEMY_TABLE != null)
        {
            VaultItemsHandler.appendPlayerBottleLines(builder, itemStack, linkingPlayer);
        }
    }


    /**
     * The bottle lines of Vault Hunters 7209 for one player: charges, drink mode, healing, the slotted and infused
     * effects, how it fills and, in a vault, how far the next charge is.
     *
     * @param builder Embed Builder.
     * @param itemStack the bottle.
     * @param player the player the values are worked out for.
     */
    private static void appendPlayerBottleLines(StringBuilder builder, ItemStack itemStack, ServerPlayer player)
    {
        int charges = BottleItem.getCharges(player, itemStack);
        int maxCharges = BottleItem.getMaxCharges(player, itemStack);
        DrinkMode drinkMode = BottleItem.getDrinkMode(itemStack);
        float healing = PotionTiers.tierOf(player).getHeal() * (1.0F + PlayerRecoveryHelper.getHealingEfficiency(player));

        TooltipLines.appendTextLine(builder, VaultItemsHandler.bottleLabelled("Charges",
            new TextComponent(charges + " / " + maxCharges).withStyle(Style.EMPTY.withColor(POTION_VALUE_COLOUR))));
        TooltipLines.appendTextLine(builder, VaultItemsHandler.bottleLabelled("Drink Mode",
            new TextComponent(drinkMode.getDisplayName()).withStyle(Style.EMPTY.withColor(drinkMode.getColor()))));
        builder.append("\n");
        TooltipLines.appendTextLine(builder, new TextComponent("Heals  ").withStyle(Style.EMPTY.withColor(POTION_VALUE_COLOUR)).
            append(new TextComponent(Math.round(healing) + " Hit Points").withStyle(ChatFormatting.RED)));
        VaultItemsHandler.appendBottleEffectLines(builder, itemStack, player);
        builder.append("\n");
        TooltipLines.appendTextLine(builder,
            new TextComponent("Fills from kills and chests in a vault").withStyle(ChatFormatting.GRAY));
        TooltipLines.appendTextLine(builder, new TextComponent("Right-click to equip it").withStyle(ChatFormatting.DARK_GRAY));

        if (BottleItem.getBoundVault(itemStack).isPresent())
        {
            boolean full = charges >= maxCharges;
            String fill = full ? "100%" : Math.round(BottleItem.getFill(itemStack) * 100.0F) + "%";

            TooltipLines.appendTextLine(builder, new TextComponent((full ? "Full" : "Next charge") + "  ").withStyle(ChatFormatting.GRAY).
                append(new TextComponent(fill).withStyle(Style.EMPTY.withColor(POTION_FILL_COLOUR))));
        }
    }


    /**
     * One line per effect slot the player's potion tier has (an effect the player has not unlocked, or one already
     * shown, is an empty slot), then the infused effect.
     *
     * @param builder Embed Builder.
     * @param itemStack the bottle.
     * @param player the player the values are worked out for.
     */
    private static void appendBottleEffectLines(StringBuilder builder, ItemStack itemStack, ServerPlayer player)
    {
        List<PotionEffect> shownEffects = new ArrayList<>();

        for (Optional<PotionEffect> slot : BottleItem.getSlotEffects(itemStack, PotionTiers.slotCountOf(player)))
        {
            Optional<PotionEffect> effect =
                slot.filter(candidate -> !shownEffects.contains(candidate) && PotionRolls.isUnlocked(player, candidate));

            if (effect.isPresent())
            {
                shownEffects.add(effect.get());
                TooltipLines.appendTextLine(builder,
                    VaultItemsHandler.bottleEffectLine(player, effect.get(), PotionRolls.valueOf(player, effect.get())));
            }
            else
            {
                TooltipLines.appendTextLine(builder, new TextComponent("Empty slot").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        BottleItem.getInfused(itemStack).ifPresent(infusion -> TooltipLines.appendTextLine(builder,
            VaultItemsHandler.bottleEffectLine(player, infusion.effect(), infusion.roll()).
                append(new TextComponent("  Infused").withStyle(ChatFormatting.DARK_GRAY))));
    }


    private static MutableComponent bottleEffectLine(ServerPlayer player, PotionEffect effect, int value)
    {
        return PotionEffectDisplay.descriptionOf(player, effect, value).
            withStyle(Style.EMPTY.withColor(PotionEffectDisplay.colorOf(effect)));
    }


    private static MutableComponent bottleLabelled(String label, Component value)
    {
        return new TextComponent(label + "  ").withStyle(ChatFormatting.GRAY).append(value);
    }


    /**
     * The bottle lines of Vault Hunters before 7209 (Wold's Vaults): how the bottle restores charges, what a charge
     * heals and the bottle's effect.
     *
     * @param builder Embed Builder.
     * @param itemStack the bottle.
     */
    private static void appendTypedBottleLines(StringBuilder builder, ItemStack itemStack)
    {
        LegacyVaultItemApi.bottleType(itemStack).ifPresent(type ->
        {
            LegacyVaultItemApi.bottleRechargeName(itemStack).ifPresent(recharge ->
            {
                switch (recharge)
                {
                    case "TIME" -> TooltipLines.appendTextLine(builder,
                        new TextComponent("Passively restores charges while inside a vault").withStyle(ChatFormatting.GRAY));
                    case "MOBS" -> TooltipLines.appendTextLine(builder,
                        new TextComponent("Kill vault mobs to restore charges").withStyle(ChatFormatting.GRAY));
                    case "CHESTS" -> TooltipLines.appendTextLine(builder,
                        new TextComponent("Loot vault chests to restore charges").withStyle(ChatFormatting.GRAY));
                }
            });

            TooltipLines.appendTextLine(builder, new TextComponent("Consume a ").
                append(new TextComponent("charge").withStyle(ChatFormatting.WHITE)).
                append(new TextComponent(" to")).
                withStyle(Style.EMPTY.withColor(BOTTLE_TEXT_COLOUR)));
            TooltipLines.appendTextLine(builder, new TextComponent(" " + TooltipLines.DOT + " Heal ").
                append(new TextComponent(String.valueOf(LegacyVaultItemApi.bottleHealing(type)))).
                append(new TextComponent(" Hit Points")).
                withStyle(ChatFormatting.GREEN));

            LegacyVaultItemApi.bottleEffectTooltip(itemStack).ifPresent(effect ->
                TooltipLines.appendTextLine(builder, new TextComponent(" " + TooltipLines.DOT + " ").append(effect)));
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

            TooltipLines.appendGameLine(builder, new TranslatableComponent("tooltip.the_vault.doll_owner",
                new TextComponent(profile.getName()).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
            TooltipLines.appendGameLine(builder, VaultItemsHandler.dollEfficiencyLine("tooltip.the_vault.doll_loot_efficiency",
                itemTag.getFloat("lootPercent"),
                config.lootPercentageMin,
                config.lootPercentageMax));
            TooltipLines.appendGameLine(builder, VaultItemsHandler.dollEfficiencyLine("tooltip.the_vault.doll_experience_efficiency",
                itemTag.getFloat("xpPercent"),
                config.xpPercentageMin,
                config.xpPercentageMax));

            if (itemTag.contains("vaultUUID"))
            {
                TooltipLines.appendGameLine(builder, new TranslatableComponent("tooltip.the_vault.doll_status.completed_by",
                    itemTag.getString("completedBy"),
                    itemTag.getInt("vaultLevel")).withStyle(ChatFormatting.GRAY));
                TooltipLines.appendGameLine(builder,
                    new TranslatableComponent("tooltip.the_vault.doll_status.completed").withStyle(ChatFormatting.GREEN));
            }
            else
            {
                TooltipLines.appendGameLine(builder,
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
     * This method parses VaultTrinket item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Trinket Item Stack.
     * @param itemTag The item tag data.
     */
    public static void handleTrinketTooltip(StringBuilder builder, ItemStack itemStack, CompoundTag itemTag)
    {
        if (!TrinketItem.isIdentified(itemStack))
        {
            TooltipLines.appendStyledLine(builder,
                "Right click to identify",
                new TextComponent("Right click to identify").withStyle(ChatFormatting.GRAY));
            return;
        }

        int totalUses = itemTag.getInt("vaultUses");
        int used = itemTag.getList("usedVaults", 10).size();
        int remaining = Math.max(totalUses - used, 0);
        TooltipLines.appendGameLine(builder, new TextComponent("Uses: ").append(String.valueOf(remaining)));

        AttributeGearData data = AttributeGearData.read(itemStack);
        data.getFirstValue(ModGearAttributes.CRAFTED_BY).ifPresent(crafter ->
            TooltipLines.appendGameLine(builder, new TextComponent("Crafted by: ").
                append(new TextComponent(crafter).withStyle(Style.EMPTY.withColor(TooltipLines.CRAFTER_COLOUR)))));

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
                TooltipLines.appendStyledLine(builder,
                    "**" + trinket.getName() + "**",
                    new TextComponent(trinket.getName()).withStyle(ChatFormatting.GOLD));
            }

            for (TextComponent line : MiscUtils.splitDescriptionText(trinket.getEffectText()))
            {
                TooltipLines.appendStyledLine(builder, line.getString(), line.withStyle(ChatFormatting.GRAY));
            }
        }

        Optional<String> slot = itemStack.getItem() instanceof GreedTrinketItem ?
            Optional.of("red_trinket") :
            TrinketItem.getSlotIdentifier(itemStack);

        slot.ifPresent(identifier ->
        {
            builder.append("\n");
            TooltipLines.appendGameLine(builder, VaultItemsHandler.curiosSlotLine(identifier));
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
            TooltipLines.appendGameLine(builder, new TextComponent("Assembles: ").
                append(new TextComponent(relicModel.getDisplayName()).withStyle(Style.EMPTY.withColor(RELIC_COLOUR))));
            builder.append("\n");
            TooltipLines.appendTextLine(builder,
                new TextComponent("Craft a Relic Pedestal to assemble").withStyle(ChatFormatting.GRAY));
            TooltipLines.appendTextLine(builder,
                new TextComponent("this fragment into a Relic").withStyle(ChatFormatting.GRAY));
        });
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
                TooltipLines.appendBulletModifierLine(builder, vaultModifier));
        }
    }


    public static void handleDeckSocketTooltip(StringBuilder builder, ItemStack itemStack)
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
     * Writes a charm's tooltip as the game draws it. Wold's Vaults replaces Vault Hunters' charm tooltip with one that
     * leaves out the uses and the Curios slot.
     *
     * @param builder Embed Builder.
     * @param itemStack the charm.
     */
    @SuppressWarnings("deprecation")
    public static void handleCharmTooltip(StringBuilder builder, ItemStack itemStack)
    {
        boolean woldsLayout = ModList.get().isLoaded(TooltipLines.WOLDS_VAULTS_MOD_ID);

        if (!woldsLayout)
        {
            int totalUses = CharmItem.getUses(itemStack);
            int used = CharmItem.getUsedVaults(itemStack).size();
            int remaining = Math.max(totalUses - used, 0);
            TooltipLines.appendGameLine(builder, new TextComponent("Uses: ").append(remaining + "/" + totalUses));
        }

        AttributeGearData data = AttributeGearData.read(itemStack);

        data.getFirstValue(ModGearAttributes.CHARM_EFFECT).ifPresent(charmEffect ->
        {
            DecimalModifierReader.Percentage<?> percentage = (DecimalModifierReader.Percentage<?>)
                ((CharmEffect.Config<?>) charmEffect.getCharmConfig().getConfig()).getAttribute().getReader();
            String effectText = Math.round(CharmItem.getValue(itemStack) * 100.0F) + "% " + percentage.getModifierName();

            TooltipLines.appendStyledLine(builder,
                effectText,
                new TextComponent(effectText).withStyle(Style.EMPTY.withColor(percentage.getRgbColor())));
            TooltipLines.appendGameLine(builder, new TextComponent("Size: ").append(String.valueOf(10)));
            builder.append("\n");
        });

        if (!woldsLayout)
        {
            TooltipLines.appendGameLine(builder, VaultItemsHandler.curiosSlotLine("charm"));
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
                TooltipLines.appendTextLine(builder, new TextComponent("Use to remove selected specialization"));
                TooltipLines.appendTextLine(builder, new TextComponent("of ability ").
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
            TooltipLines.appendItemGameLines(builder, itemStack);
            return;
        }

        if (!ModConfigs.isInitialized())
        {
            return;
        }

        TooltipLines.appendGameLine(builder,
            new TranslatableComponent("the_vault.gear_modification.information").withStyle(ChatFormatting.GRAY));
        VaultGearTagConfig.ModTagGroup group = ReforgeTagModificationFocus.getModifierTag(itemStack);

        if (group != null)
        {
            if (group.getAffixType() != null)
            {
                TooltipLines.appendGameLine(builder,
                    new TextComponent("Add a new " + group.getJoinedAffix() + ", guaranteeing ").withStyle(ChatFormatting.GOLD).
                        append(group.getDisplayComponent()));
            }

            TooltipLines.appendGameLine(builder,
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
        TooltipLines.appendGameLine(builder, VaultItemsHandler.ownerLine(TitleScrollItem.getOwnerUUID(itemStack).
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

            TooltipLines.appendGameLine(builder, new TextComponent("Preview: ").append(preview));
        }

        if (color == null)
        {
            builder.append("\n");
            TooltipLines.appendTextLine(builder,
                new TextComponent("Combine with a dye in an anvil").withStyle(ChatFormatting.DARK_GRAY));
            TooltipLines.appendTextLine(builder,
                new TextComponent("to set the title color.").withStyle(ChatFormatting.DARK_GRAY));
        }
        else
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Color: ").
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
     * Writes a soul flame's tooltip as the game draws it: since Vault Hunters 7209 the embers it turns into; on older
     * builds (Wold's Vaults) its owner, stacks and the crystal modifiers it carries.
     *
     * @param builder Embed Builder.
     * @param itemStack the soul flame.
     */
    public static void handleSoulFlameTooltip(StringBuilder builder, ItemStack itemStack)
    {
        if (LegacyVaultItemApi.hasSoulFlameOwner())
        {
            VaultItemsHandler.appendOwnedSoulFlameLines(builder, itemStack);
        }
        else if (ModConfigs.isInitialized())
        {
            TooltipLines.appendTextLine(builder, new TextComponent("Turns into ").withStyle(ChatFormatting.GRAY).
                append(new TextComponent(String.valueOf(SoulFlameItem.getEmberValue(itemStack) * itemStack.getCount())).
                    withStyle(ChatFormatting.GOLD)).
                append(new TextComponent(" Embers when carried").withStyle(ChatFormatting.GRAY)));
        }
    }


    private static void appendOwnedSoulFlameLines(StringBuilder builder, ItemStack itemStack)
    {
        TooltipLines.appendGameLine(builder, VaultItemsHandler.ownerLine(LegacyVaultItemApi.soulFlameOwnerUuid(itemStack).
            map(uuid -> LegacyVaultItemApi.soulFlameOwnerName(itemStack).orElse("Unknown")).
            orElse(null)));
        TooltipLines.appendGameLine(builder, new TextComponent("Stacks: ").
            append(new TextComponent(String.valueOf(SoulFlameItem.getStacks(itemStack))).withStyle(ChatFormatting.AQUA)));

        LegacyVaultItemApi.soulFlameModifiers(itemStack).ifPresent(modifiers ->
            CrystalTooltips.parseModifiers(builder, modifiers));
    }


    public static void handleAntiqueTooltip(StringBuilder builder, ItemStack itemStack)
    {
        TooltipLines.appendItemGameLines(builder, itemStack);
    }


    public static void handleJewelPouchTooltip(StringBuilder builder, ItemStack itemStack)
    {
        TooltipLines.appendItemGameLines(builder, itemStack);
    }


    /**
     * Writes a coin pouch's own tooltip line; the coins themselves are drawn as slots in the picture.
     *
     * @param builder Embed Builder.
     * @param itemStack the coin pouch.
     */
    public static void handleCoinPouchTooltip(StringBuilder builder, ItemStack itemStack)
    {
        TooltipLines.appendItemGameLines(builder, itemStack);
    }


    /**
     * Writes a shard pouch's tooltip as the game draws it: since Vault Hunters 7209 its soul shard count and that it
     * becomes a vault pouch; on older builds (Wold's Vaults) the shards it holds.
     *
     * @param builder Embed Builder.
     * @param itemStack the shard pouch.
     */
    public static void handleShardPouchTooltip(StringBuilder builder, ItemStack itemStack)
    {
        if (LegacyVaultItemApi.hasShardPouchStack())
        {
            ItemStack shardStack = LegacyVaultItemApi.shardPouchContainedStack(itemStack);
            String text = shardStack.isEmpty() ? "Empty" :
                shardStack.getCount() + " Shard" + (shardStack.getCount() > 1 ? "s" : "");

            TooltipLines.appendGameLine(builder, new TextComponent(text).withStyle(ChatFormatting.GOLD));
            return;
        }

        int shards = ItemShardPouch.getStoredShardCount(itemStack);

        TooltipLines.appendGameLine(builder,
            new TextComponent(shards + " Soul Shard" + (shards == 1 ? "" : "s")).withStyle(ChatFormatting.GOLD));
        TooltipLines.appendTextLine(builder,
            new TextComponent("Becomes a Vault Pouch in your inventory").withStyle(ChatFormatting.GRAY));
    }


    public static void handleGreedyMealTooltip(StringBuilder builder, ItemStack itemStack)
    {
        int xpAmount = itemStack.getOrCreateTag().getInt("GreedXpBurger");

        if (xpAmount > 0)
        {
            TooltipLines.appendGameLine(builder,
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

        TooltipLines.appendGameLine(builder, new TextComponent("Awarded to: ").append(ownerName.isEmpty() ?
            new TextComponent("???").withStyle(ChatFormatting.GRAY) :
            new TextComponent(ownerName).withStyle(ChatFormatting.YELLOW)));
        TooltipLines.appendGameLine(builder, new TextComponent("Time: ").
            append(new TextComponent(TooltipLines.formatTimeString(blockEntityTag.getInt("Time"))).withStyle(ChatFormatting.GRAY)));

        VaultDifficulty difficulty = VaultDifficulty.byName(blockEntityTag.getString("Difficulty"));

        if (difficulty != null)
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Difficulty: ").
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
        TooltipLines.appendGameLine(builder, new TextComponent("Level: ").
            append(new TextComponent(String.valueOf(level)).withStyle(Style.EMPTY.withColor(TooltipLines.LEVEL_COLOUR))));

        int hearts = tag.contains("hearts") ? tag.getInt("hearts") : 1;

        if (hearts > 0)
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Health: ").
                append(new TextComponent(TooltipLimits.repeat("\u2764", hearts)).withStyle(ChatFormatting.RED)));
            TooltipLines.appendGameLine(builder, VaultItemsHandler.companionCooldownLine(tag));
        }
        else
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Health: ").
                append(new TextComponent("Retired").withStyle(ChatFormatting.GRAY)));
        }

        String seriesRaw = tag.getString("PetSeries");

        if (!seriesRaw.isEmpty())
        {
            String seriesDisplay = seriesRaw.substring(0, 1).toUpperCase() + seriesRaw.substring(1).toLowerCase();
            TooltipLines.appendGameLine(builder, new TextComponent("Series: ").
                append(new TextComponent(seriesDisplay).withStyle(Style.EMPTY.withColor(TooltipLines.LEVEL_COLOUR))));
        }

        builder.append("\n");

        if (tag.contains("temporal"))
        {
            ResourceLocation modifierId = ResourceLocation.tryParse(tag.getString("temporal"));

            if (modifierId != null)
            {
                VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
                {
                    TooltipLines.appendStyledLine(builder,
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

            TooltipLines.appendStyledLine(builder,
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

        String remaining = TooltipLines.formatTimeString(cooldownSeconds * 20L);
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
        TooltipLines.appendStyledLine(builder, TooltipLines.DOT + " " + line.getString().strip(), line);
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

            builder.append(TooltipLines.DOT).append(" ");

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
        TooltipLines.appendItemGameLines(builder, itemStack);
    }


    public static void handleCardTooltip(StringBuilder builder, ItemStack itemStack)
    {
        Card card = CardItem.getCard(itemStack);

        TooltipLines.appendGameLine(builder, new TextComponent("Tier: ").
            append(new TextComponent(String.valueOf(card.getTier())).withStyle(Style.EMPTY.withColor(TooltipLines.LEVEL_COLOUR))));

        List<CardEntry.Color> colors = new ArrayList<>(card.getColors());

        if (!colors.isEmpty())
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Color: ").
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
            TooltipLines.appendGameLine(builder, new TextComponent("Type: ").
                append(VaultItemsHandler.joinedCardList(VaultItemsHandler.cardGroupTexts(types))));
        }

        if (!groups.isEmpty())
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Groups: ").
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
        TooltipLines.appendItemGameLines(builder, itemStack);
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
    public static void handleCardDeckTooltip(StringBuilder builder, ItemStack itemStack, ServerPlayer linkingPlayer)
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

            VaultItemsHandler.appendDeckSockets(builder, DeckTooltips.deckLines(deck, greedTier));
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
     * The colour the game's card tooltip gives types and groups.
     */
    private static final int CARD_GROUP_COLOUR = 0xCCCCCC;

    private static final int RELIC_COLOUR = 0xC6B11E;

    private static final int BOTTLE_TEXT_COLOUR = 0xCFCFCF;

    private static final int POTION_VALUE_COLOUR = 0xFFFFFF;

    private static final int POTION_FILL_COLOUR = 0xF670DB;

    private static final String COMBINED_TRINKET_CLASS = "xyz.iwolfking.woldsvaults.items.CombinedTrinketItem";

    static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Crystal part and item classes whose tooltip text failed on the server, so each is logged once.
     */
    static final Set<String> GAME_TEXT_FAILURES = ConcurrentHashMap.newKeySet();

    /**
     * How the game's deck tooltip starts its sockets line.
     */
    private static final String SOCKETS_PREFIX = "Sockets:";
}
