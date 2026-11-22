//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.vaulthunters.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import iskallia.vault.client.render.ColorBlindMode;
import iskallia.vault.item.gear.VaultCharmItem;
import iskallia.vault.options.VaultOption;


/**
 * An identified god charm's name reads the client's colour blind mode from {@code ModOptions}, which since Vault Hunters
 * 7209 cannot initialise on a dedicated server, so every server lookup of the name (Quark item links, death drops, the
 * anvil) throws. This server-only mixin skips the option and answers as a server always does: no colour blind mode.
 */
@Mixin(VaultCharmItem.class)
public abstract class MixinVaultCharmItem
{
    @Redirect(method = "getName(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/chat/Component;",
        at = @At(value = "FIELD", target = "Liskallia/vault/init/ModOptions;COLOR_BLIND_MODE:Liskallia/vault/options/VaultOption;", remap = false))
    private VaultOption<ColorBlindMode> vhdiscord$noColourBlindOption()
    {
        return null;
    }


    @Redirect(method = "getName(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/chat/Component;",
        at = @At(value = "INVOKE", target = "Liskallia/vault/options/VaultOption;getValue()Ljava/lang/Object;", remap = false))
    private Object vhdiscord$noColourBlindMode(VaultOption<ColorBlindMode> option)
    {
        return ColorBlindMode.NONE;
    }
}
