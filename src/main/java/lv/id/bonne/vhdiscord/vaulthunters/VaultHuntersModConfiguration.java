//
// Created by BONNe
// Copyright - 2023
//


package lv.id.bonne.vhdiscord.vaulthunters;

import lv.id.bonne.vhdiscord.config.MixinConfigPlugin;
import net.minecraftforge.fml.loading.LoadingModList;



public class VaultHuntersModConfiguration extends MixinConfigPlugin
{
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        return true;
    }
}
