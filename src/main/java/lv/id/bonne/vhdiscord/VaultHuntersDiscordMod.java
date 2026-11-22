package lv.id.bonne.vhdiscord;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.LoadingModList;


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
}
