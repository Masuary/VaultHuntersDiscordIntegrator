//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.util.List;
import java.util.Optional;

import iskallia.vault.config.PlayerTitlesConfig;
import iskallia.vault.config.TrinketConfig;
import iskallia.vault.config.VaultItemsConfig;
import iskallia.vault.config.gear.VaultGearTagConfig;
import iskallia.vault.config.greed.GreedBoxConfig;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.VaultUtils;
import iskallia.vault.core.vault.influence.VaultGod;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.gear.charm.CharmEffect;
import iskallia.vault.gear.data.AttributeGearData;
import iskallia.vault.gear.reader.DecimalModifierReader;
import iskallia.vault.gear.trinket.TrinketEffect;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.init.ModGearAttributes;
import iskallia.vault.item.ItemRespecFlask;
import iskallia.vault.item.ItemShardPouch;
import iskallia.vault.item.SoulFlameItem;
import iskallia.vault.item.TitleScrollItem;
import iskallia.vault.item.VaultDollItem;
import iskallia.vault.item.gear.CharmItem;
import iskallia.vault.item.gear.GreedTrinketItem;
import iskallia.vault.item.gear.GreedyCharmItem;
import iskallia.vault.item.gear.TemporalShardItem;
import iskallia.vault.item.gear.TrinketItem;
import iskallia.vault.item.gear.VaultCharmItem;
import iskallia.vault.item.modification.ReforgeTagModificationFocus;
import iskallia.vault.skill.prestige.helper.PrestigeHelper;
import iskallia.vault.util.MiscUtils;
import iskallia.vault.util.SidedHelper;
import iskallia.vault.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;


/**
 * Tooltips of the smaller Vault Hunters items: trinkets, charms, respec flasks, focuses, title scrolls, soul flames,
 * pouches, greedy meals, keyrings, dolls, antiques, modifier scrolls and Create attribute filters.
 */
final class MiscItemTooltips
{
    private static final String COMBINED_TRINKET_CLASS = "xyz.iwolfking.woldsvaults.items.CombinedTrinketItem";

    private static final String VAULT_CHARM_SLOT = "vault_charm";

    private static final String POUCH_OWNER_KEY = "Owner";

    private static final int GREEDY_CHARM_VALUE_COLOUR = 0xFFC530;


    private MiscItemTooltips()
    {
    }


    /**
     * Writes a vault doll's tooltip as the game draws it for a player outside the doll's vault.
     * @param builder Embed Builder.
     * @param itemTag Vault Doll Item Tag.
     */
    static void handleDollTooltip(StringBuilder builder, CompoundTag itemTag)
    {
        VaultDollItem.getPlayerGameProfile(itemTag).ifPresent(profile ->
        {
            VaultItemsConfig.VaultDoll config = ModConfigs.VAULT_ITEMS.VAULT_DOLL;

            TooltipLines.appendGameLine(builder, new TranslatableComponent("tooltip.the_vault.doll_owner",
                new TextComponent(profile.getName()).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
            TooltipLines.appendGameLine(builder, MiscItemTooltips.dollEfficiencyLine("tooltip.the_vault.doll_loot_efficiency",
                itemTag.getFloat("lootPercent"),
                config.lootPercentageMin,
                config.lootPercentageMax));
            TooltipLines.appendGameLine(builder, MiscItemTooltips.dollEfficiencyLine("tooltip.the_vault.doll_experience_efficiency",
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
    static void handleTrinketTooltip(StringBuilder builder, ItemStack itemStack, CompoundTag itemTag)
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

        Optional<List<TrinketEffect<?>>> combined = MiscItemTooltips.combinedTrinketEffects(itemStack);
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
            TooltipLines.appendGameLine(builder, MiscItemTooltips.curiosSlotLine(identifier));
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
     * Writes a charm's tooltip as the game draws it. Wold's Vaults replaces Vault Hunters' charm tooltip with one that
     * leaves out the uses and the Curios slot.
     *
     * @param builder Embed Builder.
     * @param itemStack the charm.
     */
    @SuppressWarnings("deprecation")
    static void handleCharmTooltip(StringBuilder builder, ItemStack itemStack)
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
            TooltipLines.appendGameLine(builder, MiscItemTooltips.curiosSlotLine(MiscItemTooltips.charmSlotIdentifier()));
        }
    }


    /**
     * The Curios slot a charm's tooltip names: Vault Hunters 7209 moved charms to its own {@code vault_charm} slot.
     *
     * @return the slot identifier.
     */
    private static String charmSlotIdentifier()
    {
        return CuriosApi.getSlotHelper().getSlotType(VAULT_CHARM_SLOT).isPresent() ? VAULT_CHARM_SLOT : "charm";
    }


    /**
     * Writes a respec flask's tooltip as the game draws it.
     *
     * @param builder Embed Builder.
     * @param itemStack the flask.
     */
    static void handleFlaskTooltip(StringBuilder builder, ItemStack itemStack)
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
    static void handleFocusTooltip(StringBuilder builder, ItemStack itemStack)
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
    static void handleTitleScrollTooltip(StringBuilder builder, ItemStack itemStack)
    {
        TooltipLines.appendGameLine(builder, MiscItemTooltips.ownerLine(TitleScrollItem.getOwnerUUID(itemStack).
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
    static void handleSoulFlameTooltip(StringBuilder builder, ItemStack itemStack)
    {
        if (LegacyVaultItemApi.hasSoulFlameOwner())
        {
            MiscItemTooltips.appendOwnedSoulFlameLines(builder, itemStack);
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
        TooltipLines.appendGameLine(builder, MiscItemTooltips.ownerLine(LegacyVaultItemApi.soulFlameOwnerUuid(itemStack).
            map(uuid -> LegacyVaultItemApi.soulFlameOwnerName(itemStack).orElse("Unknown")).
            orElse(null)));
        TooltipLines.appendGameLine(builder, new TextComponent("Stacks: ").
            append(new TextComponent(String.valueOf(SoulFlameItem.getStacks(itemStack))).withStyle(ChatFormatting.AQUA)));

        LegacyVaultItemApi.soulFlameModifiers(itemStack).ifPresent(modifiers ->
            CrystalTooltips.parseModifiers(builder, modifiers));
    }


    static void handleAntiqueTooltip(StringBuilder builder, ItemStack itemStack)
    {
        TooltipLines.appendItemGameLines(builder, itemStack);
    }


    static void handleJewelPouchTooltip(StringBuilder builder, ItemStack itemStack)
    {
        TooltipLines.appendItemGameLines(builder, itemStack);
    }


    /**
     * Writes a coin pouch's own tooltip line; the coins themselves are drawn as slots in the picture. Since Vault Hunters
     * 7209 the game reads that line from the owner's saved contents, so a pouch owned by someone else than the linking
     * player is written as an empty one instead of reading another player's data.
     *
     * @param builder Embed Builder.
     * @param itemStack the coin pouch.
     * @param linkingPlayer the player who linked it, or null when unknown.
     */
    static void handleCoinPouchTooltip(StringBuilder builder, ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        CompoundTag tag = itemStack.getTag();

        if (tag != null && tag.hasUUID(POUCH_OWNER_KEY) && (linkingPlayer == null || !tag.getUUID(POUCH_OWNER_KEY).equals(linkingPlayer.getUUID())))
        {
            TooltipLines.appendTextLine(builder, new TextComponent("Empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        TooltipLines.appendItemGameLines(builder, itemStack);
    }


    /**
     * Writes a shard pouch's tooltip as the game draws it: since Vault Hunters 7209 its soul shard count and that it
     * becomes a vault pouch; on older builds (Wold's Vaults) the shards it holds.
     *
     * @param builder Embed Builder.
     * @param itemStack the shard pouch.
     */
    static void handleShardPouchTooltip(StringBuilder builder, ItemStack itemStack)
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


    /**
     * Writes a rolled Greedy Charm's tooltip as Vault Hunters 7209 draws it without Shift, for the linking player: what
     * the charm adds at the player's total reputation with the gods, that reputation, and the uses left (hidden for a
     * player with the immortal charms prestige power). An unrolled charm's text is server-safe and comes from the game.
     *
     * @param builder Embed Builder.
     * @param itemStack the charm.
     * @param linkingPlayer the player who linked it, or null when unknown (reputation 0).
     * @return true when the lines were written, false when the game's own text describes the charm.
     */
    static boolean handleGreedyCharmTooltip(StringBuilder builder, ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        if (ModConfigs.GREEDY_CHARM == null || !GreedyCharmItem.isRolled(itemStack))
        {
            return false;
        }

        int reputation = MiscItemTooltips.totalReputation(linkingPlayer);
        TooltipLines.appendTextLine(builder,
            MiscItemTooltips.greedyCharmImpactLine(GreedyCharmItem.getDamagePerReputation(itemStack) * reputation, " more damage"));
        TooltipLines.appendTextLine(builder,
            MiscItemTooltips.greedyCharmImpactLine(GreedyCharmItem.getDamageTakenPerReputation(itemStack) * reputation, " less damage taken"));
        TooltipLines.appendGameLine(builder, new TextComponent("Reputation: ").withStyle(ChatFormatting.GRAY).
            append(new TextComponent(String.valueOf(reputation)).withStyle(ChatFormatting.WHITE)).
            append(new TextComponent(" / " + GreedyCharmItem.MAX_TOTAL_REPUTATION).withStyle(ChatFormatting.GRAY)));

        if (reputation == 0)
        {
            TooltipLines.appendTextLine(builder, new TextComponent("Earn reputation with the gods to wake it").withStyle(ChatFormatting.DARK_GRAY));
        }

        if (linkingPlayer == null || !VaultCharmItem.hasImmortalCharms(PrestigeHelper.getPrestige(linkingPlayer)))
        {
            TooltipLines.appendGameLine(builder, VaultCharmItem.usesLine(itemStack, false));
        }

        return true;
    }


    private static int totalReputation(ServerPlayer player)
    {
        if (player == null)
        {
            return 0;
        }

        int total = 0;

        for (VaultGod god : VaultGod.values())
        {
            total += SidedHelper.getGodReputation(player, god).orElse(0);
        }

        return total;
    }


    private static Component greedyCharmImpactLine(float share, String words)
    {
        return new TextComponent(Math.round(share * 100.0F) + "%").withStyle(Style.EMPTY.withColor(GREEDY_CHARM_VALUE_COLOUR)).
            append(new TextComponent(words).withStyle(ChatFormatting.WHITE));
    }


    /**
     * Writes an identified Temporal Shard's tooltip as Vault Hunters 7209 draws it without Shift: its vault modifier, the
     * duration, and whether it can be consumed where the linking player is.
     *
     * @param builder Embed Builder.
     * @param itemStack the shard.
     * @param linkingPlayer the player who linked it, or null when unknown (taken as outside a vault).
     * @return true when the lines were written, false for a shard this does not describe.
     */
    static boolean handleTemporalShardTooltip(StringBuilder builder, ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        if (!LegacyVaultItemApi.hasTemporalModifierLock() || !TemporalShardItem.isIdentified(itemStack))
        {
            return false;
        }

        VaultModifier<?> modifier = TemporalShardItem.getVaultModifier(itemStack);

        if (modifier == null)
        {
            return false;
        }

        Style modifierStyle = Style.EMPTY.withColor(modifier.getDisplayTextColor());
        TooltipLines.appendGameLine(builder, new TextComponent("Modifier: ").withStyle(ChatFormatting.WHITE).
            append(new TextComponent(modifier.getDisplayName()).withStyle(modifierStyle)));
        TooltipLines.appendGameLine(builder, new TextComponent("Duration: ").withStyle(ChatFormatting.WHITE).
            append(new TextComponent(TooltipLines.formatTimeString(TemporalShardItem.getDuration(itemStack))).withStyle(modifierStyle)));

        Optional<Vault> vault = linkingPlayer == null ? Optional.empty() : VaultUtils.getVault(linkingPlayer.level);

        if (vault.isEmpty())
        {
            TooltipLines.appendTextLine(builder, new TextComponent("Consume this relic in a vault to gain its effects").withStyle(ChatFormatting.RED));
        }
        else if (VaultUtils.isSpecialVault(vault.get()) || VaultUtils.hasNoTemporalModifier(vault.get()))
        {
            TooltipLines.appendTextLine(builder, new TextComponent("This relic is locked due to the objective of this vault.").withStyle(ChatFormatting.RED));
        }
        else
        {
            TooltipLines.appendTextLine(builder, new TextComponent("Consume this relic to gain its effects").withStyle(ChatFormatting.GRAY));
        }

        return true;
    }


    static void handleGreedyMealTooltip(StringBuilder builder, ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        int xpAmount = itemStack.getOrCreateTag().getInt("GreedXpBurger");

        if (LegacyVaultItemApi.hasUnrangedGreedyMeal())
        {
            if (xpAmount > 0)
            {
                TooltipLines.appendGameLine(builder,
                    new TextComponent("Grants " + String.format("%,d", xpAmount) + " Vault XP").withStyle(ChatFormatting.GREEN));
            }

            return;
        }

        GreedBoxConfig.Range range = ModConfigs.GREED_BOX.getGreedyMealVaultXpRange();
        String amountText = xpAmount > 0 ? TextUtil.fullNumber(xpAmount) :
            TextUtil.fullNumber(range.min()) + " - " + TextUtil.fullNumber(range.max());
        TooltipLines.appendGameLine(builder, new TextComponent("Grants " + amountText + " Vault XP").withStyle(ChatFormatting.GREEN));

        if (linkingPlayer != null && DeckTooltips.greedTierOf(linkingPlayer) < 1)
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Unlocks at Greed Tier 1").withStyle(ChatFormatting.RED));
        }
    }


    static void handleAttributeFilterTooltip(StringBuilder builder, ItemStack itemStack)
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
            String description = MiscItemTooltips.decodeAttributeFilterEntry(attributeTag);

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


    static void handleKeyringTooltip(StringBuilder builder, ItemStack itemStack)
    {
        TooltipLines.appendItemGameLines(builder, itemStack);
    }


    static void handleModifierScrollTooltip(StringBuilder builder, ItemStack itemStack)
    {
        TooltipLines.appendItemGameLines(builder, itemStack);
    }
}
