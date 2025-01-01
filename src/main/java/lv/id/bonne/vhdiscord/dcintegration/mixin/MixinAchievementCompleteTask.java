//
// Created by BONNe
// Copyright - 2025
//


package lv.id.bonne.vhdiscord.dcintegration.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import iskallia.vault.task.AchievementCompleteTask;
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

        if (DiscordIntegration.INSTANCE != null)
        {
            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(component.getString()));
        }
    }
}
