//
// Created by BONNe
// Copyright - 2025
//


package lv.id.bonne.vhdiscord.dcintegration.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import iskallia.vault.core.event.common.ListenerLeaveEvent;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.player.ClassicListenersLogic;
import iskallia.vault.core.vault.player.Completion;
import iskallia.vault.core.vault.stat.StatCollector;
import iskallia.vault.core.world.storage.VirtualWorld;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;


@Mixin(value = ClassicListenersLogic.class)
public class MixinClassicListenersLogic
{
    @Redirect(method = "lambda$initServer$1",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V"))
    private void onVaultExit(PlayerList instance, Component component, ChatType p_11265_, UUID p_11266_)
    {
        instance.broadcastMessage(component, p_11265_, p_11266_);

        if (DiscordIntegration.INSTANCE != null)
        {
            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(component.getString()));
        }
    }


    @Redirect(method = "printJoinMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V"))
    private void onVaultJoin(PlayerList instance, Component component, ChatType p_11265_, UUID p_11266_)
    {
        instance.broadcastMessage(component, p_11265_, p_11266_);

        if (DiscordIntegration.INSTANCE != null)
        {
            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(component.getString()));
        }
    }
}
