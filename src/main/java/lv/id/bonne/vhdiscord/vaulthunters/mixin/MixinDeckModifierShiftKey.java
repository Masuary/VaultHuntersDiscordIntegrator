//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.vaulthunters.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


/**
 * Every deck modifier's text asks the client whether Shift is held (to add roll ranges); on a dedicated server that class
 * does not exist. This server-only mixin answers "not held" in the modifiers of VH and of Wold's Vaults, which is only
 * present on that pack (hence {@link Pseudo}). A modifier missing here falls back in
 * {@link lv.id.bonne.vhdiscord.parser.DeckTooltips#appendModifierText}.
 */
@Pseudo
@Mixin(targets = {
    "iskallia.vault.core.card.modifier.deck.BountyDeckModifier",
    "iskallia.vault.core.card.modifier.deck.GlobalDeckModifier",
    "iskallia.vault.core.card.modifier.deck.NonFoilEfficiencyDeckModifier",
    "iskallia.vault.core.card.modifier.deck.ResourceDoubleDeckModifier",
    "iskallia.vault.core.card.modifier.deck.ResourceRequirementDeckModifier",
    "iskallia.vault.core.card.modifier.deck.SlotDeckModifier",
    "iskallia.vault.core.card.modifier.deck.StatEfficiencyDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.AdjacencyBonusDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.CardNeighborTypeDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.ChainReactionDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.ColorMismatchAdjacencyModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.CreateGroupSlotDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.CreateSlotDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.DominanceDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.EmptySlotDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.GroupSynergyDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.GroupSynergyMultiplierModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.NitwitDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.RowPositionDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.SymmetricBalanceDeckModifier",
    "xyz.iwolfking.woldsvaults.modifiers.deck.UniqueGroupsDeckModifier" }, remap = false)
public abstract class MixinDeckModifierShiftKey
{
    @Redirect(method = "addText",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;hasShiftDown()Z", remap = true),
        require = 0)
    private boolean vhdiscord$shiftNotHeld()
    {
        return false;
    }
}
