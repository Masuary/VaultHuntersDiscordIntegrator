//
// Created by BONNe
// Copyright - 2025
//


package lv.id.bonne.vhdiscord.sdlink.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.UUID;

import iskallia.vault.task.AchievementCompleteTask;
import me.hypherionmc.sdlink.server.ServerEvents;
import me.hypherionmc.sdlinklib.config.ConfigController;
import me.hypherionmc.sdlinklib.discord.DiscordMessage;
import me.hypherionmc.sdlinklib.discord.messages.MessageAuthor;
import me.hypherionmc.sdlinklib.discord.messages.MessageType;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;


@Mixin(value = AchievementCompleteTask.class)
public class MixinAchievementCompleteTask
{
    @Redirect(method = "onConsume",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V"))
    private void onBroadCast(PlayerList instance, Component component, ChatType p_11265_, UUID p_11266_)
    {
        instance.broadcastMessage(component, p_11265_, p_11266_);

        if (ServerEvents.getInstance().getBotEngine() == null)
        {
            // Bot engine is not setup. Do nothing.
            return;
        }

        if (!ConfigController.modConfig.generalConfig.enabled)
        {
            // Config is disabled. Do nothing.
            return;
        }

        DiscordMessage discordMessage = new DiscordMessage.Builder(ServerEvents.getInstance().getBotEngine(), MessageType.JOIN_LEAVE).
            withMessage(component.getString()).
            withAuthor(MessageAuthor.SERVER).
            build();
        discordMessage.sendMessage();
    }
}
