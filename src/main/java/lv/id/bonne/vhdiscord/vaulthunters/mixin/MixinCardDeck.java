//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.vaulthunters.mixin;


import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import iskallia.vault.core.card.CardDeck;
import iskallia.vault.core.card.modifier.deck.DeckModifier;
import lv.id.bonne.vhdiscord.parser.DeckTooltips;
import lv.id.bonne.vhdiscord.vaulthunters.DeckTextGreedTier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;


/**
 * Lets the deck tooltip run on a dedicated server (this mixin is server-only): the client's Shift key reads as not held,
 * the greed tier is the one in {@link DeckTextGreedTier} (set by the parser's deck tooltip) instead of the client's, and
 * each modifier writes its text through the parser's {@code DeckTooltips.appendModifierText}. Since Vault Hunters 7209
 * the 4-argument {@code addText} only calls the 5-argument one, which holds these calls; before it (Wold's Vaults) the
 * 4-argument one does.
 */
@Mixin(value = CardDeck.class, remap = false)
public abstract class MixinCardDeck
{
    @Redirect(method = {
            "addText(Ljava/util/List;ILnet/minecraft/world/item/TooltipFlag;FZ)V",
            "addText(Ljava/util/List;ILnet/minecraft/world/item/TooltipFlag;F)V" },
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;hasShiftDown()Z", remap = true),
        require = 0)
    private boolean vhdiscord$shiftNotHeld()
    {
        return false;
    }


    @Redirect(method = "addText(Ljava/util/List;ILnet/minecraft/world/item/TooltipFlag;FZ)V",
        at = @At(value = "INVOKE", target = "Liskallia/vault/client/data/ClientGreedTreeData;getGreedTier()I"),
        require = 0)
    private int vhdiscord$requestedGreedTier()
    {
        return DeckTextGreedTier.get();
    }


    @Redirect(method = {
            "addText(Ljava/util/List;ILnet/minecraft/world/item/TooltipFlag;FZ)V",
            "addText(Ljava/util/List;ILnet/minecraft/world/item/TooltipFlag;F)V" },
        at = @At(value = "INVOKE",
            target = "Liskallia/vault/core/card/modifier/deck/DeckModifier;addText(Ljava/util/List;ILnet/minecraft/world/item/TooltipFlag;F)V"),
        require = 0)
    private void vhdiscord$modifierText(DeckModifier<?> modifier, List<Component> tooltip, int minIndex, TooltipFlag flag, float time)
    {
        DeckTooltips.appendModifierText(modifier, tooltip, minIndex, flag, time);
    }
}
