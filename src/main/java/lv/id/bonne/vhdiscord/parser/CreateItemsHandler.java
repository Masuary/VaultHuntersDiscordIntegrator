package lv.id.bonne.vhdiscord.parser;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.filter.AttributeFilterMenu;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.ItemAttribute;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

// always check ModList.get().isLoaded("create") before loading this class
public class CreateItemsHandler {
    public static String generateCrateItemTooltips(ItemStack itemStack) {
        if (itemStack.getItem() instanceof FilterItem) {
            try {
                var components = filterSummary(itemStack);
                StringBuilder sb = new StringBuilder();
                for (var cmp : components) {
                    sb.append(cmp.getString()).append('\n');
                }
                return sb.toString();
            } catch (Throwable t) {
                System.out.println(t);
            }
        }
        return null;
    }

    private static List<Component> filterSummary(ItemStack itemStack) {
        List<Component> list = new ArrayList<>();
        if (!itemStack.hasTag()) {
            return list;
        }
        var tag = itemStack.getTag();
        if (itemStack.getItem().getRegistryName().equals(Create.asResource("filter"))) {
            ItemStackHandler filterItems = getFilterItems(itemStack);

            boolean matchAll = tag != null && tag.getBoolean("MatchAll");
            var matchTypeCmp = Components.literal(matchAll ? " (All)" : " (Any)").withStyle(ChatFormatting.GOLD);
            boolean blacklist = tag != null && tag.getBoolean("Blacklist");
            list.add((blacklist ? Lang.translateDirect("gui.filter.deny_list") : Lang.translateDirect("gui.filter.allow_list")).withStyle(ChatFormatting.GOLD).append(matchTypeCmp));

            for (int i = 0; i < filterItems.getSlots(); ++i) {
                ItemStack innerStack = filterItems.getStackInSlot(i);
                if (!innerStack.isEmpty()) {
                    if (innerStack.getItem() instanceof FilterItem) { // this branch is from VaultFilters recursive summary
                        MutableComponent firstComp = Components.literal("\\- ").append(innerStack.getHoverName()).append(" ").withStyle(ChatFormatting.GRAY);
                        List<Component> innerSummary = filterSummary(innerStack);
                        boolean isFst = true;
                        for (Component component : innerSummary) {
                            list.add((isFst ? firstComp : Components.literal("   " /*discord deletes normal spaces in embeds*/)).append(component).withStyle(ChatFormatting.GRAY));
                            isFst = false;
                        }
                    } else {
                        list.add(Components.literal("\\- ").append(innerStack.getHoverName()).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }

        if (itemStack.getItem().getRegistryName().equals(Create.asResource("attribute_filter"))) {
            int whitelistModeOrdinal = tag != null ? tag.getInt("WhitelistMode") : 0;
            AttributeFilterMenu.WhitelistMode whitelistMode = AttributeFilterMenu.WhitelistMode.values()[whitelistModeOrdinal];
            list.add((whitelistMode == AttributeFilterMenu.WhitelistMode.WHITELIST_CONJ ? Lang.translateDirect("gui.attribute_filter.allow_list_conjunctive") : (whitelistMode == AttributeFilterMenu.WhitelistMode.WHITELIST_DISJ ? Lang.translateDirect("gui.attribute_filter.allow_list_disjunctive") : Lang.translateDirect("gui.attribute_filter.deny_list"))).withStyle(ChatFormatting.GOLD));

            if (tag != null) {
                for (Tag inbt : tag.getList("MatchedAttributes", 10)) {
                    CompoundTag compound = (CompoundTag) inbt;
                    ItemAttribute attribute = ItemAttribute.fromNBT(compound);
                    if (attribute != null) {
                        boolean inverted = compound.getBoolean("Inverted");
                        list.add(Components.literal("\\- ").append(format(attribute, inverted)));
                    }
                }
            }

        }
        return list;

    }

    private static Component format(ItemAttribute attribute, boolean inverted){
        return Lang.translateDirect("item_attributes." + attribute.getTranslationKey() + (inverted ? ".inverted" : ""), attribute.getTranslationParameters());
    }

    private static ItemStackHandler getFilterItems(ItemStack stack) {
        ItemStackHandler newInv = new ItemStackHandler(18);
        if (!stack.getItem().getRegistryName().equals(Create.asResource("filter"))) {
            throw new IllegalArgumentException("Cannot get filter items from non-filter: " + stack);
        } else if (!stack.hasTag()) {
            return newInv;
        } else {
            CompoundTag invNBT = stack.getTagElement("Items");
            if (invNBT != null && !invNBT.isEmpty()) {
                newInv.deserializeNBT(invNBT);
            }

            return newInv;
        }
    }
}
