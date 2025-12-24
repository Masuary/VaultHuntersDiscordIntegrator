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
import iskallia.vault.command.OpenVaultSnapshotCommand;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;


@Mixin(value = OpenVaultSnapshotCommand.class)
public class MixinOpenVaultSnapshotCommand
{
    @Redirect(method = "sendCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V"))
    private static void onCommandSending(PlayerList instance, Component component, ChatType p_11265_, UUID p_11266_)
    {
        instance.broadcastMessage(component, p_11265_, p_11266_);

        if (DiscordIntegration.INSTANCE != null)
        {
            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(component.getString()));
        }
    }
}
