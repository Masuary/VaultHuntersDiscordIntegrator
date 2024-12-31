//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.vaulthunters.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import iskallia.vault.gear.modification.GearModification;
import iskallia.vault.item.modification.GearModificationItem;


@Mixin(value = GearModificationItem.class, remap = false)
public interface GearModificationItemAccessor
{
    @Accessor("modification")
    GearModification getModification();
}