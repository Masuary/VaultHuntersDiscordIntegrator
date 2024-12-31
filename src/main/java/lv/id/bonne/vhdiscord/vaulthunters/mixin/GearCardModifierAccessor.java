//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.vaulthunters.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

import iskallia.vault.core.card.GearCardModifier;


@Mixin(value = GearCardModifier.class, remap = false)
public interface GearCardModifierAccessor<T>
{
    @Accessor("values")
    Map<Integer, T> getValues();
}