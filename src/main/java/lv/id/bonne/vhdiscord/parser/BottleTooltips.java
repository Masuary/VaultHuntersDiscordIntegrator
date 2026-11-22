//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import iskallia.vault.config.PotionConfig;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.item.bottle.BottleItem;
import iskallia.vault.item.bottle.DrinkMode;
import iskallia.vault.item.bottle.PotionEffect;
import iskallia.vault.item.bottle.PotionEffectDisplay;
import iskallia.vault.item.bottle.PotionRolls;
import iskallia.vault.item.bottle.PotionTiers;
import iskallia.vault.util.PlayerRecoveryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;


/**
 * Tooltips of potion bottles, for the linking player on Vault Hunters 7209 and later.
 */
final class BottleTooltips
{
    /**
     * The colours the game's potion bottle tooltip gives these texts.
     */
    private static final int BOTTLE_TEXT_COLOUR = 0xCFCFCF;

    private static final int POTION_VALUE_COLOUR = 0xFFFFFF;

    private static final int POTION_FILL_COLOUR = 0xF670DB;


    private BottleTooltips()
    {
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
    static void handleBottleTooltip(StringBuilder builder, ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        if (ModConfigs.POTION == null)
        {
            // Potions are not defined.
            return;
        }

        if (LegacyVaultItemApi.hasBottleTypes())
        {
            BottleTooltips.appendTypedBottleLines(builder, itemStack);
        }
        else if (linkingPlayer != null && ModConfigs.VAULT_ALCHEMY_TABLE != null)
        {
            BottleTooltips.appendPlayerBottleLines(builder, itemStack, linkingPlayer);
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
    /**
     * A Vault Hunters 7209 potion's name as the game shows it to a player: the name of the player's potion tier, in its
     * colour.
     *
     * @param itemStack the item.
     * @param linkingPlayer the player who linked it, or null when unknown.
     * @return the name, or empty for any other item, an older Vault Hunters or an unknown player.
     */
    static Optional<Component> potionTitle(ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        if (!(itemStack.getItem() instanceof BottleItem) || itemStack.hasCustomHoverName() || linkingPlayer == null ||
            ModConfigs.POTION == null || LegacyVaultItemApi.hasBottleTypes())
        {
            return Optional.empty();
        }

        PotionConfig.Tier tier = PotionTiers.tierOf(linkingPlayer);
        return Optional.of(new TextComponent(tier.getName()).withStyle(Style.EMPTY.withColor(tier.getNameColor())));
    }


    /**
     * The Heals line for the picture: the label and the hearts the game draws for the healing, with a "+" for more than
     * it shows.
     *
     * @param healing the hit points the potion heals.
     * @return the line.
     */
    private static Component healsLine(float healing)
    {
        MutableComponent line = new TextComponent("Heals").withStyle(Style.EMPTY.withColor(POTION_VALUE_COLOUR));
        String hearts = HeartRow.hearts(healing);

        if (!hearts.isEmpty())
        {
            line.append(new TextComponent(hearts).withStyle(ChatFormatting.RED));
        }

        if (HeartRow.hasMoreThanShown(healing))
        {
            line.append(new TextComponent("+").withStyle(Style.EMPTY.withColor(POTION_VALUE_COLOUR)));
        }

        return line;
    }


    /**
     * Whether the linked potion is the one in the player's potion slot; the game compares the very stack, a link carries
     * a copy, so the item and its tag are compared.
     *
     * @param itemStack the linked potion.
     * @param player the player who linked it.
     * @return true for the slotted potion.
     */
    private static boolean isSlotted(ItemStack itemStack, ServerPlayer player)
    {
        ItemStack slotted = BottleItem.getSlotted(player);
        return !slotted.isEmpty() && ItemStack.isSameItemSameTags(slotted, itemStack);
    }


    private static void appendPlayerBottleLines(StringBuilder builder, ItemStack itemStack, ServerPlayer player)
    {
        int charges = BottleItem.getCharges(player, itemStack);
        int maxCharges = BottleItem.getMaxCharges(player, itemStack);
        DrinkMode drinkMode = BottleItem.getDrinkMode(itemStack);
        float healing = PotionTiers.tierOf(player).getHeal() * (1.0F + PlayerRecoveryHelper.getHealingEfficiency(player));

        TooltipLines.appendTextLine(builder, BottleTooltips.bottleLabelled("Charges",
            new TextComponent(charges + " / " + maxCharges).withStyle(Style.EMPTY.withColor(POTION_VALUE_COLOUR))));
        TooltipLines.appendTextLine(builder, BottleTooltips.bottleLabelled("Drink Mode",
            new TextComponent(drinkMode.getDisplayName()).withStyle(Style.EMPTY.withColor(drinkMode.getColor()))));
        builder.append("\n");
        TooltipLines.appendStyledLine(builder, "Heals  " + Math.round(healing) + " Hit Points", BottleTooltips.healsLine(healing));
        BottleTooltips.appendBottleEffectLines(builder, itemStack, player);
        builder.append("\n");
        TooltipLines.appendTextLine(builder,
            new TextComponent("Fills from kills and chests in a vault").withStyle(ChatFormatting.GRAY));
        TooltipLines.appendTextLine(builder,
            new TextComponent(BottleTooltips.isSlotted(itemStack, player) ? "Right-click to change the Drink Mode" : "Right-click to equip it").
                withStyle(ChatFormatting.DARK_GRAY));

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
                    BottleTooltips.bottleEffectLine(player, effect.get(), PotionRolls.valueOf(player, effect.get())));
            }
            else
            {
                TooltipLines.appendTextLine(builder, new TextComponent("Empty slot").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        BottleItem.getInfused(itemStack).ifPresent(infusion -> TooltipLines.appendTextLine(builder,
            BottleTooltips.bottleEffectLine(player, infusion.effect(), infusion.roll()).
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
}
