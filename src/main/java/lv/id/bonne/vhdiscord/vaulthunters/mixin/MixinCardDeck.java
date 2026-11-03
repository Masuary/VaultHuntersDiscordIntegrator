//
// Created by BONNe
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;


/**
 * Lets the deck tooltip run on a dedicated server (this mixin is server-only): the client's Shift key reads as not held,
 * gear colours stay the game's (VH's default; its accessibility option is client-side), and each modifier writes its text
 * through {@link DeckTooltips#appendModifierText}.
 */
@Mixin(value = CardDeck.class, remap = false)
public abstract class MixinCardDeck
{
    @Redirect(method = "addText",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;hasShiftDown()Z", remap = true),
        require = 0)
    private boolean vhdiscord$shiftNotHeld()
    {
        return false;
    }


    @Redirect(method = "addText",
        at = @At(value = "INVOKE",
            target = "Liskallia/vault/gear/reader/VaultGearModifierReader;applyAccessibilityColour(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;"),
        require = 0)
    private Component vhdiscord$gearColours(Component component)
    {
        return component;
    }


    @Redirect(method = "addText",
        at = @At(value = "INVOKE",
            target = "Liskallia/vault/core/card/modifier/deck/DeckModifier;addText(Ljava/util/List;ILnet/minecraft/world/item/TooltipFlag;F)V"),
        require = 0)
    private void vhdiscord$modifierText(DeckModifier<?> modifier, List<Component> tooltip, int minIndex, TooltipFlag flag, float time)
    {
        DeckTooltips.appendModifierText(modifier, tooltip, minIndex, flag, time);
    }
}
