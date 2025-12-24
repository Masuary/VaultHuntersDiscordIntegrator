//
// Created by BONNe
// Copyright - 2025
//


package lv.id.bonne.vhdiscord.mc2discord.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

import fr.denisd3d.mc2discord.core.M2DUtils;
import fr.denisd3d.mc2discord.core.MessageManager;
import fr.denisd3d.mc2discord.shadow.discord4j.discordjson.possible.Possible;
import fr.denisd3d.mc2discord.shadow.reactor.core.publisher.Flux;
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
        if (M2DUtils.isNotConfigured())
        {
            // Mc2Discord is not configured. Do nothing.
            return;
        }

        // Only handle vault-related messages
        String message = component.getString();
        if (this.isVaultMessage(message))
        {
            this.sendToDiscord(message);
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

    /**
     * Sends a vault join/leave message to Discord using mc2discord's MessageManager
     * @param message The message to send
     */
    private void sendToDiscord(String message)
    {
        List<String> types = List.of("chat", "join_leave");

        Flux.fromIterable(MessageManager.getMatchingChannels(types)).
            flatMap(channel -> switch (channel.mode) {
                case WEBHOOK -> MessageManager.createWebhookMessage(channel.channel_id,
                    message,
                    Possible.absent(), // No specific username
                    Possible.absent(), // No specific avatar
                    false,
                    List.of(), // No embeds
                    null);
                case PLAIN_TEXT -> MessageManager.createPlainTextMessage(channel.channel_id,
                    message,
                    Possible.absent(), // No specific username
                    false);
                case EMBED -> MessageManager.createEmbedMessage(channel.channel_id,
                    message,
                    Possible.absent(), // No specific username
                    Possible.absent(), // No specific avatar
                    types);
            }).subscribe();
    }
}
