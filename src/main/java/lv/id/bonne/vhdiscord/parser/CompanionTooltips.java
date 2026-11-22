//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import iskallia.vault.core.vault.modifier.registry.VaultModifierRegistry;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.dynamodel.DynamicModel;
import iskallia.vault.dynamodel.model.item.PlainItemModel;
import iskallia.vault.dynamodel.registry.DynamicModelRegistry;
import iskallia.vault.init.ModDynamicModels;
import iskallia.vault.init.ModRelics;
import iskallia.vault.item.CompanionRelicItem;
import iskallia.vault.item.RelicFragmentItem;
import iskallia.vault.world.VaultDifficulty;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;


/**
 * Tooltips of companions, companion relics, relic fragments and herald trophies.
 */
final class CompanionTooltips
{
    /**
     * The colour the game's relic fragment tooltip gives the relic it assembles.
     */
    private static final int RELIC_COLOUR = 0xC6B11E;


    private CompanionTooltips()
    {
    }


    /**
     * This method parses Vault Relic Fragment item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Trinket Item Stack.
     * @param relic The item.
     */
    static void handleRelicFragmentTooltip(StringBuilder builder, ItemStack itemStack, RelicFragmentItem relic)
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
    static void handleCompanionRelicTooltip(StringBuilder builder, ItemStack itemStack)
    {
        for (ResourceLocation modifierId : CompanionRelicItem.getModifiers(itemStack))
        {
            VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
                TooltipLines.appendBulletModifierLine(builder, vaultModifier));
        }
    }


    /**
     * Writes a herald trophy's tooltip as the game draws it. The game shows the owner's current name when the viewer
     * knows the player; here it is the name stored on the trophy.
     *
     * @param builder Embed Builder.
     * @param itemStack the trophy.
     */
    static void handleHeraldTrophyTooltip(StringBuilder builder, ItemStack itemStack)
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
    static void handleCompanionTooltip(StringBuilder builder, ItemStack itemStack)
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
            TooltipLines.appendGameLine(builder, CompanionTooltips.companionCooldownLine(tag));
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
                    CompanionTooltips.appendModifierLine(builder, vaultModifier, vaultModifier.getDisplayName(), 1);
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
                forEach(key -> CompanionTooltips.countRelicModifiers(relicsTag.getCompound(key), modifierCounts));

            if (ancientRelicsTag.contains("0"))
            {
                CompanionTooltips.countRelicModifiers(ancientRelicsTag.getCompound("0"), modifierCounts);
            }

            TooltipLines.appendStyledLine(builder,
                "**Relics**",
                new TextComponent("Relics").withStyle(ChatFormatting.GREEN));

            modifierCounts.forEach((id, count) ->
                VaultModifierRegistry.getOpt(id).ifPresent(vaultModifier ->
                    CompanionTooltips.appendModifierLine(builder,
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
}
