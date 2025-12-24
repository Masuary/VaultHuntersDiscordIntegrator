//
// Created by BONNe
// Copyright - 2025
//


package lv.id.bonne.vhdiscord.dcintegration.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;


@Mixin(value = PlayerList.class)
public class MixinPlayerList
{
    @Inject(method = "broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V", 
        at = @At("HEAD"))
    private void onBroadcastMessage(Component component, ChatType chatType, UUID uuid, CallbackInfo ci)
    {
        if (DiscordIntegration.INSTANCE == null)
        {
            // Discord integration is not setup. Do nothing.
            return;
        }

        // Only handle vault-related messages
        String message = component.getString();
        if (this.isVaultMessage(message))
        {
            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(message));
        }
    }

    /**
     * Check if the message is a vault-related message that we want to send to Discord
     * @param message The message text
     * @return true if this is a vault join/leave message
     */
    private boolean isVaultMessage(String message)
    {
        return message.contains("entered a") && message.contains("Vault") ||
               message.contains("entered an") && message.contains("Vault") ||
               message.contains("completed a") && message.contains("Vault") ||
               message.contains("survived a") && message.contains("Vault") ||
               message.contains("was defeated in a") && message.contains("Vault") ||
               message.contains("opened a Vault") ||
               message.contains("opened the Final Vault");
    }
}
