//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.masuchatlink.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import com.masuary.masuchatlink.api.ChatEventType;
import com.masuary.masuchatlink.api.MasuChatLinkApi;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;


/**
 * Forwards the vault entered/completed/failed broadcasts (the same ones the mc2discord and
 * dcintegration integrations mixin into) to MasuChatLink as a {@code vault} chat entry. There is no
 * Forge event for {@link PlayerList#broadcastMessage}, so this mirrors their mixin rather than using a
 * plain listener.
 */
@Mixin(value = PlayerList.class)
public class MixinPlayerList
{
    @Inject(method = "broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V",
        at = @At("HEAD"))
    private void onBroadcastMessage(Component component, ChatType chatType, UUID uuid, CallbackInfo ci)
    {
        String message = component.getString();
        if (this.isVaultMessage(message))
        {
            MasuChatLinkApi.publish(ChatEventType.VAULT, null, message, null);
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
