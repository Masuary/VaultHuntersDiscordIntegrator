//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.vaulthunters.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import iskallia.vault.core.card.CardCondition;
import iskallia.vault.core.card.CardEntry;


@Mixin(value = CardEntry.class, remap = false)
public interface CardEntryAccessor
{
    @Accessor("condition")
    CardCondition getCondition();
}