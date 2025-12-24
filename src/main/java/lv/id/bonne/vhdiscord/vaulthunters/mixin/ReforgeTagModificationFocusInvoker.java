//
// Created by BONNe
// Copyright - 2024
//


package lv.id.bonne.vhdiscord.vaulthunters.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

import iskallia.vault.item.modification.ReforgeTagModificationFocus;
import net.minecraft.world.item.Item;


@Mixin(value = ReforgeTagModificationFocus.class, remap = false)
public interface ReforgeTagModificationFocusInvoker
{
    @Invoker("getItemsDisplay")
    static String invokeGetItemsDisplay(List<Item> items)
    {
        throw new AssertionError();
    }
}
