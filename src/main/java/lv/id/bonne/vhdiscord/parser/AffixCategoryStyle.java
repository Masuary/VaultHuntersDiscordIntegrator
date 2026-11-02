//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.util.Map;

import iskallia.vault.gear.attribute.VaultGearModifier;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;


/**
 * The symbols and colours the game puts around a gear modifier for each of its categories, as VH's
 * {@code ModifierCategoryTooltip} draws them, without the animated highlight. VH's own formatter
 * ({@code VaultGearModifier#modifyDisplayComponentCategoryFormatters}) is client-only, so the server rebuilds it.
 */
public final class AffixCategoryStyle
{
    /**
     * Category name to its marks. Categories this VH version lacks are never matched.
     */
    private static final Map<String, Marks> MARKS = Map.of(
        "LEGENDARY", new Marks("✦ ", "", 0xF1E734, false),
        "ABYSSAL", new Marks("ᚼ ", "", 0x00724C, false),
        "ABILITY_ENHANCEMENT", new Marks("⏶ ", "", 0x0092A8, false),
        "CRAFTED", new Marks("⛏ ", "", 0xF5CF8E, false),
        "FROZEN", new Marks("❰ ", " ❱", 0x00BFFF, true),
        "CORRUPTED", new Marks("【", "】", 0xC2FF7A, true),
        "GREATER", new Marks("⧫ ", "", 0x59D216, false),
        "IMBUED", new Marks("✤ ", "", 0x8F65FF, false));


    private AffixCategoryStyle()
    {
    }


    /**
     * Wraps a modifier's text in the marks of each of its categories, in the game's category order (later
     * categories sit outermost).
     *
     * @param modifier the modifier.
     * @param text the modifier's display text.
     * @return the text with its category marks.
     */
    public static MutableComponent apply(VaultGearModifier<?> modifier, MutableComponent text)
    {
        MutableComponent styled = text;

        for (VaultGearModifier.AffixCategory category : VaultGearModifier.AffixCategory.values())
        {
            Marks marks = MARKS.get(category.name());

            if (marks != null && modifier.hasCategory(category))
            {
                styled = marks.wrap(styled);
            }
        }

        return styled;
    }


    /**
     * One category's marks.
     *
     * @param prefix the text before the modifier.
     * @param suffix the text after it, empty for none.
     * @param colour the marks' RGB colour.
     * @param bold whether the marks are bold.
     */
    private record Marks(String prefix, String suffix, int colour, boolean bold)
    {
        MutableComponent wrap(MutableComponent text)
        {
            Style style = Style.EMPTY.withColor(TextColor.fromRgb(this.colour)).withBold(this.bold);
            MutableComponent wrapped = new TextComponent("").append(new TextComponent(this.prefix).setStyle(style)).append(text);

            if (!this.suffix.isEmpty())
            {
                wrapped.append(new TextComponent(this.suffix).setStyle(style));
            }

            return wrapped;
        }
    }
}
