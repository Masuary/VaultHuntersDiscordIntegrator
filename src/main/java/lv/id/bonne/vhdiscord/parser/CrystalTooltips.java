//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import iskallia.vault.config.entry.DescriptionData;
import iskallia.vault.config.sigil.SigilConfig;
import iskallia.vault.core.data.key.ThemeKey;
import iskallia.vault.core.vault.VaultRegistry;
import iskallia.vault.core.vault.influence.VaultGod;
import iskallia.vault.core.vault.modifier.VaultModifierStack;
import iskallia.vault.core.vault.modifier.registry.VaultModifierRegistry;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.core.vault.objective.ParadoxObjective;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.item.AugmentItem;
import iskallia.vault.item.InfusedCatalystItem;
import iskallia.vault.item.SigilItem;
import iskallia.vault.item.crystal.CrystalData;
import iskallia.vault.item.crystal.CrystalEntry;
import iskallia.vault.item.crystal.layout.ArchitectCrystalLayout;
import iskallia.vault.item.crystal.layout.CrystalLayout;
import iskallia.vault.item.crystal.layout.ParadoxCrystalLayout;
import iskallia.vault.item.crystal.layout.preset.ParadoxTemplatePreset;
import iskallia.vault.item.crystal.modifiers.CrystalModifiers;
import iskallia.vault.item.crystal.modifiers.ParadoxCrystalModifiers;
import iskallia.vault.item.crystal.modifiers.PersonalCrystalModifiers;
import iskallia.vault.item.crystal.objective.AscensionCrystalObjective;
import iskallia.vault.item.crystal.objective.CrystalObjective;
import iskallia.vault.item.crystal.objective.ParadoxCrystalObjective;
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
import iskallia.vault.util.SidedHelper;
import iskallia.vault.world.theme.ThemeMobGroups;
import iskallia.vault.world.theme.ThemeSeries;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;


/**
 * Tooltips of vault crystals and what goes into them (catalysts, augments and sigils), and the crystal modifier lists
 * soul flames carry too.
 */
final class CrystalTooltips
{
    /**
     * The colours the game's crystal and infused catalyst tooltips give these texts.
     */
    private static final int CLARITY_COLOUR = 0x4BE3C5;

    private static final int CATALYST_SIZE_COLOUR = 0xDDDDDD;

    private static final int SEASONAL_THEME_COLOUR = 0xFFAA00;

    private static final String CURSE = "\u2620";

    /**
     * Wold's Vaults' rainbow text: the hue moves once around the circle in this time, at this saturation and brightness.
     */
    private static final long RAINBOW_PERIOD_MILLISECONDS = 2000L;

    private static final float RAINBOW_SATURATION = 0.85F;

    private static final float RAINBOW_BRIGHTNESS = 1.0F;

    private static final String INFUSED_AUGMENT_CLASS = "xyz.iwolfking.woldsvaults.items.InfusedAugmentItem";

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#.##%");


    private CrystalTooltips()
    {
    }


    /**
     * This method parses Vault Crystal item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param crystalData Vault Crystal Data.
     */
    static void handleVaultCrystalTooltip(StringBuilder builder, CrystalData crystalData)
    {
        List<Component> gameLines = CrystalTooltips.crystalGameLines(crystalData);

        if (gameLines == null)
        {
            CrystalTooltips.appendCrystalSummary(builder, crystalData);
        }
        else
        {
            gameLines.forEach(line -> TooltipLines.appendGameLine(builder, line));
        }

        // Modifiers
        CrystalTooltips.parseModifiers(builder, crystalData.getModifiers());
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
                if (VaultItemsHandler.GAME_TEXT_FAILURES.add(part.getClass().getName()))
                {
                    VaultItemsHandler.LOGGER.warn("{} cannot write its crystal tooltip on the server; crystals with it use the summary text",
                        part.getClass().getName(), failure);
                }

                return null;
            }
        }

        return lines;
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
     * This method parses Infused Catalyst item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Infused Catalyst Item Stack.
     */
    static void handleInfusedCatalystTooltip(StringBuilder builder, ItemStack itemStack)
    {
        if (InfusedCatalystItem.isSuper(itemStack))
        {
            TooltipLines.appendTextLine(builder,
                new TextComponent("Used to modify your Personal Vault").withStyle(ChatFormatting.GRAY));
        }
        else
        {
            InfusedCatalystItem.getSize(itemStack).ifPresentOrElse(
                size -> TooltipLines.appendGameLine(builder, new TextComponent("Size: ").
                    append(new TextComponent(String.valueOf(size)).withStyle(Style.EMPTY.withColor(CATALYST_SIZE_COLOUR)))),
                () -> TooltipLines.appendGameLine(builder, new TextComponent("Size: ???").withStyle(ChatFormatting.GRAY)));
        }

        for (ResourceLocation modifierId : InfusedCatalystItem.getModifiers(itemStack))
        {
            VaultModifierRegistry.getOpt(modifierId).ifPresent(vaultModifier ->
                TooltipLines.appendBulletModifierLine(builder, vaultModifier));
        }
    }


    /**
     * This method parses base Catalyst item tooltip into discord chat.
     * @param builder Embed Builder.
     * @param itemStack Vault Catalyst Item Stack.
     */
    static void handleBaseCatalystTooltip(StringBuilder builder, ItemStack itemStack)
    {
        TooltipLines.appendItemGameLines(builder, itemStack);
    }


    /**
     * Writes a sigil's tooltip as the game draws it, with the crate tiers counted for the linking player's vault level,
     * as the game counts them for the viewer's.
     *
     * @param builder Embed Builder.
     * @param itemStack the sigil.
     * @param linkingPlayer the player who linked it, or null when unknown (the sigil's highest level is used).
     */
    static void handleSigilTooltip(StringBuilder builder, ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        SigilItem.readSigil(itemStack).ifPresent(sigil ->
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Sigil: ").withStyle(ChatFormatting.GRAY).
                append(new TextComponent(sigil.getDisplayName()).withStyle(Style.EMPTY.withColor(sigil.getColor()))));
            TooltipLines.appendTextLine(builder, new TextComponent("+").withStyle(ChatFormatting.GRAY).
                append(new TextComponent(PERCENT_FORMAT.format(sigil.getDifficulty())).withStyle(ChatFormatting.WHITE)).
                append(" Difficulty"));

            SigilConfig sigilConfig = ModConfigs.SIGIL.get(sigil.getId());

            if (sigilConfig != null)
            {
                int vaultLevel = linkingPlayer == null ? Integer.MAX_VALUE : SidedHelper.getVaultLevel(linkingPlayer);
                int crateTiers = sigilConfig.getLevel(vaultLevel).getExtraCrateTiers();
                TooltipLines.appendTextLine(builder, new TextComponent("+").withStyle(ChatFormatting.GRAY).
                    append(new TextComponent(String.valueOf(crateTiers)).withStyle(ChatFormatting.WHITE)).
                    append(" Crate Tiers"));
            }
        });
    }


    /**
     * A theme series augment's lines as Vault Hunters 7209 writes them: the biomes, each mob group with its families at the
     * player's vault level, the perk, the seasonal mark, and the conversion line for an augment that still names a theme.
     *
     * @param builder Embed Builder.
     * @param itemStack the augment.
     * @param series the augment's series.
     * @param linkingPlayer the player who linked it, or null when unknown.
     */
    private static void appendSeriesLines(StringBuilder builder, ItemStack itemStack, ThemeSeries series, ServerPlayer linkingPlayer)
    {
        TextColor seriesColour = TextColor.fromRgb(series.readableColor());
        TooltipLines.appendGameLine(builder, new TextComponent("Biomes: ").withStyle(ChatFormatting.GRAY).
            append(new TextComponent(String.join(", ", series.biomeNames())).withStyle(ChatFormatting.WHITE)));

        int vaultLevel = linkingPlayer == null ? 0 : SidedHelper.getVaultLevel(linkingPlayer);
        ThemeMobGroups.of(series.id(), vaultLevel).ifPresent(mobs ->
            mobs.groups().stream().
                filter(stock -> !stock.families().isEmpty()).
                forEach(stock -> TooltipLines.appendGameLine(builder, new TextComponent(stock.group().displayName() + ": ").withStyle(ChatFormatting.GRAY).
                    append(new TextComponent(String.join(", ", stock.families())).withStyle(ChatFormatting.WHITE)))));

        if (!series.perk().isEmpty())
        {
            TooltipLines.appendGameLine(builder, new TextComponent("Perk: ").withStyle(ChatFormatting.GRAY).
                append(new TextComponent(series.perk()).withStyle(Style.EMPTY.withColor(seriesColour))));
        }

        if (series.seasonal())
        {
            TooltipLines.appendTextLine(builder, new TextComponent("Seasonal theme").withStyle(Style.EMPTY.withColor(SEASONAL_THEME_COLOUR)));
        }

        CompoundTag tag = itemStack.getTag();
        boolean namesTheme = tag != null && !tag.contains("series", Tag.TAG_STRING) && tag.contains("theme", Tag.TAG_STRING);

        if (namesTheme && ThemeSeries.ofTheme(ResourceLocation.tryParse(tag.getString("theme"))).isPresent())
        {
            TooltipLines.appendTextLine(builder,
                new TextComponent("Turns into a " + series.displayName() + " Augment in your inventory").withStyle(ChatFormatting.DARK_GRAY));
        }
    }


    /**
     * Writes an augment's tooltip as the game draws it. Wold's Vaults' Infused Augment draws its theme in a rainbow
     * built with client classes, so that line is rebuilt here; a Vault Hunters 7209 augment of a theme series lists the
     * series' mobs for the viewer's vault level (read on the client), so those lines are rebuilt for the linking player;
     * any other augment writes its own lines.
     *
     * @param builder Embed Builder.
     * @param itemStack the augment.
     * @param linkingPlayer the player who linked it, or null when unknown (vault level 0).
     */
    static void handleAugmentTooltip(StringBuilder builder, ItemStack itemStack, ServerPlayer linkingPlayer)
    {
        if (!itemStack.getItem().getClass().getName().equals(INFUSED_AUGMENT_CLASS))
        {
            Optional<ThemeSeries> series = LegacyVaultItemApi.hasThemeSeries() ?
                AugmentItem.getSeries(itemStack).flatMap(ThemeSeries::byId) : Optional.empty();

            if (series.isPresent())
            {
                CrystalTooltips.appendSeriesLines(builder, itemStack, series.get(), linkingPlayer);
            }
            else
            {
                TooltipLines.appendItemGameLines(builder, itemStack);
            }

            return;
        }

        AugmentItem.getTheme(itemStack).ifPresent(key ->
            ModConfigs.THEME_AUGMENT_LORE.getAugmentLore(key.getId()).ifPresentOrElse(lore ->
                {
                    TooltipLines.appendGameLine(builder, new TextComponent("Theme: ").withStyle(ChatFormatting.GRAY).
                        append(CrystalTooltips.rainbowText(lore.displayName + " | " + key.getName())));

                    for (DescriptionData description : lore.description)
                    {
                        TooltipLines.appendGameLine(builder, description.getComponent());
                    }
                },
                () -> TooltipLines.appendGameLine(builder, new TextComponent("Theme: ").withStyle(ChatFormatting.GRAY).
                    append(CrystalTooltips.rainbowText(key.getName())))));
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

        TooltipLines.appendGameLine(builder, header);

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

            TooltipLines.appendStyledLine(builder, TooltipLines.DOT + " " + line.getString().strip(), line);
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
                    append(TooltipLines.DOT).
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
                            append(TooltipLines.DOT).
                            append(" ").
                            append("**Cooldown:** ???").
                            append("\n");
                    }

                    builder.append(" ").
                        append(TooltipLines.DOT).
                        append(" ").
                        append("**Player:** ???");
                }
                else
                {
                    long ticksLeft = paradox.getCooldown() / 50L;

                    if (paradox.getType() == ParadoxObjective.Type.RUN)
                    {
                        builder.append(" ").
                            append(TooltipLines.DOT).
                            append(" ").
                            append("**Cooldown:** ").
                            append(ticksLeft < 0L ? "Ready" : TooltipLines.formatTimeString((int)ticksLeft)).
                            append("\n");
                    }

                    builder.append(" ").
                        append(TooltipLines.DOT).
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
                        append(TooltipLines.DOT).
                        append(" ").
                        append("**Player:** ???").
                        append("\n");
                }
                else
                {
                    builder.append(" ").
                        append(TooltipLines.DOT).
                        append(" ").
                        append("**Player:** ").
                        append(ascensionTag.contains("player_name") ? "Unknown" : ascensionTag.getString("player_name")).
                        append("\n");
                }

                builder.append(" ").
                    append(TooltipLines.DOT).
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
                        append(TooltipLines.DOT).
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
                        append(TooltipLines.DOT).
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
            String text = TooltipLines.formatTimeString(min);
            if (min != max) {
                text = text + " - " + TooltipLines.formatTimeString(max);
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
    static void parseModifiers(StringBuilder builder, CrystalModifiers modifiers)
    {
        List<VaultModifierStack> modifierList = new ArrayList<>();
        modifiers.forEach(modifierList::add);

        Style curseStyle = Style.EMPTY.withColor(ModConfigs.VAULT_CRYSTAL.MODIFIER_STABILITY.curseColor);
        boolean personal = modifiers instanceof PersonalCrystalModifiers;

        if (modifiers instanceof ParadoxCrystalModifiers)
        {
            CrystalTooltips.appendModifierGroup(builder,
                modifierList,
                new TextComponent("Cursed").withStyle(curseStyle),
                true,
                stack -> ModConfigs.VAULT_CRYSTAL_CATALYST.isCurse(stack.getModifierId()));
        }
        else if (!personal)
        {
            if (modifiers.hasClarity())
            {
                TooltipLines.appendGameLine(builder,
                    new TextComponent("Clarity").withStyle(Style.EMPTY.withColor(CLARITY_COLOUR)));
            }

            int curseCount = modifiers.getCurseCount();

            if (curseCount > 0 && modifiers.hasClarity())
            {
                CrystalTooltips.appendModifierGroup(builder,
                    modifierList,
                    new TextComponent("Cursed").withStyle(curseStyle),
                    true,
                    stack -> ModConfigs.VAULT_CRYSTAL_CATALYST.isCurse(stack.getModifierId()));
            }
            else if (curseCount > 0)
            {
                TooltipLines.appendGameLine(builder,
                    new TextComponent("Cursed ").withStyle(curseStyle).append(TooltipLimits.repeat(CURSE, curseCount)));
            }
        }

        String headerSuffix = personal ? "" : " Modifiers";

        CrystalTooltips.appendModifierGroup(builder,
            modifierList,
            new TextComponent("Positive" + headerSuffix).withStyle(ChatFormatting.GREEN),
            true,
            stack -> ModConfigs.VAULT_CRYSTAL_CATALYST.isGood(stack.getModifierId()));
        CrystalTooltips.appendModifierGroup(builder,
            modifierList,
            new TextComponent("Negative" + headerSuffix).withStyle(ChatFormatting.RED),
            true,
            stack -> ModConfigs.VAULT_CRYSTAL_CATALYST.isBad(stack.getModifierId()));
        CrystalTooltips.appendModifierGroup(builder,
            modifierList,
            new TextComponent("Other" + headerSuffix).withStyle(ChatFormatting.WHITE),
            false,
            stack -> ModConfigs.VAULT_CRYSTAL_CATALYST.isUnlisted(stack.getModifierId()));
    }
}
