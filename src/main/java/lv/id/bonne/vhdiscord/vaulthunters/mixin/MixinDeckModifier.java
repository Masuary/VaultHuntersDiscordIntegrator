//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.vaulthunters.mixin;


import java.util.List;

import org.spongepowered.asm.mixin.Mixin;

import iskallia.vault.core.card.modifier.deck.DeckModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;


/**
 * VH marks {@code DeckModifier#addText} client-only, so Forge strips it from the class on a dedicated server, while the
 * modifiers that override it still call it and the deck tooltip calls it on every modifier. This server-only mixin puts it
 * back with the game's own empty body, so {@link lv.id.bonne.vhdiscord.parser.DeckTooltips} can run the deck's text.
 */
@Mixin(value = DeckModifier.class, remap = false)
public abstract class MixinDeckModifier
{
    public void addText(List<Component> tooltip, int minIndex, TooltipFlag flag, float time)
    {
    }
}
