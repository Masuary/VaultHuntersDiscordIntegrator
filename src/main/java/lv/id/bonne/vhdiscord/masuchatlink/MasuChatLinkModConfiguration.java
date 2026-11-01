//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.masuchatlink;


import lv.id.bonne.vhdiscord.config.MixinConfigPlugin;
import net.minecraftforge.fml.loading.LoadingModList;


/**
 * Configuration for MasuChatLink mod.
 */
public class MasuChatLinkModConfiguration extends MixinConfigPlugin
{
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        return LoadingModList.get().getModFileById("masuchatlink") != null;
    }
}
