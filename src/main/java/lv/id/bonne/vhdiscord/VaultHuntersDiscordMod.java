package lv.id.bonne.vhdiscord;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.event.server.ServerStartingEvent;


/**
 * Mod initializing system.
 */
@Mod("vhdiscord")
public class VaultHuntersDiscordMod
{
    public VaultHuntersDiscordMod()
    {
        if (LoadingModList.get().getModFileById("masuchatlink") != null)
        {
            // Kept behind this presence check so the class (and its compileOnly MasuChatLink import)
            // is never loaded when the mod is absent.
            MinecraftForge.EVENT_BUS.register(new lv.id.bonne.vhdiscord.masuchatlink.MasuChatLinkEventListener());
        }
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
    }
}
