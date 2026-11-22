//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.vaulthunters.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import iskallia.vault.gear.reader.VaultGearModifierReader;
import net.minecraft.network.chat.Component;


/**
 * VH greys modifier text unless the client's gear tooltip colours option is on, reading {@code ModOptions}, which since
 * Vault Hunters 7209 cannot initialise on a dedicated server (it references client screen classes), so every deck or gear
 * card text that reaches this method fails there. This server-only mixin answers as the option's default (colours on)
 * does: the text unchanged.
 */
@Mixin(value = VaultGearModifierReader.class, remap = false)
public abstract class MixinVaultGearModifierReader
{
    @Inject(method = "applyAccessibilityColour", at = @At("HEAD"), cancellable = true)
    private static void vhdiscord$coloursOn(Component component, CallbackInfoReturnable<Component> callback)
    {
        callback.setReturnValue(component);
    }
}
