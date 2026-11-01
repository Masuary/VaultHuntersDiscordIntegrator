//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.filter.AttributeFilterMenu.WhitelistMode;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.ItemAttribute;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;


/**
 * This class allows to parse Create mod filter item tooltips to discord chat.
 */
public class CreateItemsHandler
{
    /**
     * This method generates string with tooltip of given Create filter item stack.
     * @param itemStack Item Stack object.
     * @return String with a tooltip, or null if the item is not a Create filter.
     */
    public static String generateCrateItemTooltips(ItemStack itemStack)
    {
        if (itemStack.getItem() instanceof FilterItem)
        {
            try
            {
                List<Component> components = filterSummary(itemStack);
                StringBuilder sb = new StringBuilder();

                for (Component cmp : components)
                {
                    sb.append(cmp.getString()).append('\n');
                }

                return sb.toString();
            }
            catch (Throwable throwable)
            {
                System.out.println(throwable);
            }
        }

        return null;
    }


    /**
     * This method builds the list of summary lines for a Create filter or attribute filter item.
     * @param itemStack Item Stack object.
     * @return List of components describing the filter contents.
     */
    private static List<Component> filterSummary(ItemStack itemStack)
    {
        List<Component> list = new ArrayList<>();

        if (!itemStack.hasTag())
        {
            return list;
        }

        CompoundTag tag = itemStack.getTag();

        if (itemStack.getItem().getRegistryName().equals(Create.asResource("filter")))
        {
            ItemStackHandler filterItems = getFilterItems(itemStack);
            boolean matchAll = tag != null && tag.getBoolean("MatchAll");
            MutableComponent matchTypeCmp = Components.literal(matchAll ? " (All)" : " (Any)").withStyle(ChatFormatting.GOLD);
            boolean blacklist = tag != null && tag.getBoolean("Blacklist");

            list.add(
                (blacklist ? Lang.translateDirect("gui.filter.deny_list") : Lang.translateDirect("gui.filter.allow_list"))
                    .withStyle(ChatFormatting.GOLD)
                    .append(matchTypeCmp)
            );

            for (int i = 0; i < filterItems.getSlots(); i++)
            {
                ItemStack innerStack = filterItems.getStackInSlot(i);

                if (!innerStack.isEmpty())
                {
                    if (innerStack.getItem() instanceof FilterItem)
                    {
                        MutableComponent firstComp = Components.literal("\\- ").append(innerStack.getHoverName()).append(" ").withStyle(ChatFormatting.GRAY);
                        List<Component> innerSummary = filterSummary(innerStack);
                        boolean isFirst = true;

                        for (Component component : innerSummary)
                        {
                            list.add((isFirst ? firstComp : Components.literal("   ")).append(component).withStyle(ChatFormatting.GRAY));
                            isFirst = false;
                        }
                    }
                    else
                    {
                        list.add(Components.literal("\\- ").append(innerStack.getHoverName()).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }

        if (itemStack.getItem().getRegistryName().equals(Create.asResource("attribute_filter")))
        {
            int whitelistModeOrdinal = tag != null ? tag.getInt("WhitelistMode") : 0;
            WhitelistMode whitelistMode = WhitelistMode.values()[whitelistModeOrdinal];

            list.add(
                (whitelistMode == WhitelistMode.WHITELIST_CONJ
                    ? Lang.translateDirect("gui.attribute_filter.allow_list_conjunctive")
                    : (whitelistMode == WhitelistMode.WHITELIST_DISJ
                        ? Lang.translateDirect("gui.attribute_filter.allow_list_disjunctive")
                        : Lang.translateDirect("gui.attribute_filter.deny_list")))
                    .withStyle(ChatFormatting.GOLD)
            );

            if (tag != null)
            {
                for (Tag inbt : tag.getList("MatchedAttributes", 10))
                {
                    CompoundTag compound = (CompoundTag) inbt;
                    ItemAttribute attribute = ItemAttribute.fromNBT(compound);

                    if (attribute != null)
                    {
                        boolean inverted = compound.getBoolean("Inverted");
                        list.add(Components.literal("\\- ").append(format(attribute, inverted)));
                    }
                }
            }
        }

        return list;
    }


    /**
     * This method formats a Create item attribute into its translated display component.
     * @param attribute Item attribute to format.
     * @param inverted Whether the attribute match is inverted.
     * @return The translated component.
     */
    private static Component format(ItemAttribute attribute, boolean inverted)
    {
        return Lang.translateDirect("item_attributes." + attribute.getTranslationKey() + (inverted ? ".inverted" : ""), attribute.getTranslationParameters());
    }


    /**
     * This method reads the inner item handler of a Create filter item from its NBT.
     * @param stack The filter item stack.
     * @return The item stack handler holding the filter's contents.
     */
    private static ItemStackHandler getFilterItems(ItemStack stack)
    {
        ItemStackHandler newInv = new ItemStackHandler(18);

        if (!stack.getItem().getRegistryName().equals(Create.asResource("filter")))
        {
            throw new IllegalArgumentException("Cannot get filter items from non-filter: " + stack);
        }

        if (!stack.hasTag())
        {
            return newInv;
        }

        CompoundTag invNBT = stack.getTagElement("Items");

        if (invNBT != null && !invNBT.isEmpty())
        {
            newInv.deserializeNBT(invNBT);
        }

        return newInv;
    }
}
