//
// Created by BONNe
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.masuchatlink;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masuary.masuchatlink.api.ChatRelayEvent;
import com.masuary.masuchatlink.api.ChatRelayItem;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lv.id.bonne.vhdiscord.parser.VaultItemsHandler;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;


/**
 * Plain Forge listener (no mixin) on MasuChatLink's {@link ChatRelayEvent}: when a chat message's item
 * hover is a Vault Hunters item, attaches the same tooltip description {@code mc2discord}'s
 * {@code MixinEvents} already generates via {@link VaultItemsHandler}, so the published entry becomes
 * type {@code item} instead of plain {@code chat}.
 */
public class MasuChatLinkEventListener
{
    /**
     * The Forge Item registry.
     */
    private static final IForgeRegistry<Item> VH_DISCORD_ITEM_REGISTRY = ForgeRegistries.ITEMS;


    @SubscribeEvent
    public void onChatRelay(ChatRelayEvent event)
    {
        final JsonObject json =
            JsonParser.parseString(Component.Serializer.toJson(event.getOriginalComponent())).getAsJsonObject();

        if (json.has("with") && json.get("with").isJsonArray())
        {
            this.searchAndAttachItem(event, json.getAsJsonArray("with"));
        }
        else if (json.has("extra") && json.get("extra").isJsonArray())
        {
            this.searchAndAttachItem(event, json.getAsJsonArray("extra"));
        }
    }


    /**
     * This method searches for a "show_item" hoverEvent and, if it describes a Vault Hunters item,
     * attaches its tooltip description to the relay event.
     *
     * @param event the chat relay event to attach the item to.
     * @param array of json objects.
     */
    private void searchAndAttachItem(ChatRelayEvent event, JsonArray array)
    {
        for (JsonElement object : array)
        {
            if (object instanceof JsonObject singleElement &&
                singleElement.has("hoverEvent"))
            {
                final JsonObject hoverEvent = singleElement.getAsJsonObject("hoverEvent");

                if (hoverEvent.has("action") &&
                    hoverEvent.get("action").getAsString().equals("show_item") &&
                    hoverEvent.has("contents") &&
                    hoverEvent.getAsJsonObject("contents").has("tag"))
                {
                    this.parseAndAttach(event, hoverEvent.getAsJsonObject("contents").getAsJsonObject());
                    return;
                }
            }
        }
    }


    /**
     * This method parses the given item json data and, if it is a recognised Vault Hunters item,
     * attaches a {@link ChatRelayItem} to the event.
     *
     * @param event the chat relay event to attach the item to.
     * @param itemJson item json data.
     */
    private void parseAndAttach(ChatRelayEvent event, JsonObject itemJson)
    {
        try
        {
            ItemStack itemStack =
                new ItemStack(VH_DISCORD_ITEM_REGISTRY.getValue(new ResourceLocation(itemJson.get("id").getAsString())));

            if (itemJson.has("tag"))
            {
                CompoundTag tag =
                    (CompoundTag) NbtTagArgument.nbtTag().parse(new StringReader(itemJson.get("tag").getAsString()));
                itemStack.setTag(tag);
            }

            CompoundTag itemTag = itemStack.getOrCreateTag();

            String description = VaultItemsHandler.generateVaultHuntersItemTooltips(itemJson, itemStack, itemTag);

            if (description == null || description.isBlank())
            {
                return;
            }

            String itemName = itemStack.hasCustomHoverName() ? itemStack.getDisplayName().getContents() :
                new TranslatableComponent(itemStack.getItem().getDescriptionId()).getContents();
            String title = itemName.isBlank() ? itemStack.getHoverName().getString() : itemName;

            event.setItem(new ChatRelayItem(title, this.colourOf(itemStack), description));
        }
        catch (CommandSyntaxException ignored)
        {
        }
    }


    /**
     * Reads the hover name's own text colour (Vault Hunters applies the rarity/tier colour to the
     * item's display name for every item type this handles), falling back to an empty string when the
     * name carries no explicit colour.
     *
     * @param itemStack the item to read the colour from.
     * @return the serialised colour (a named colour or {@code #rrggbb}), or an empty string.
     */
    private String colourOf(ItemStack itemStack)
    {
        TextColor colour = itemStack.getHoverName().getStyle().getColor();
        return colour == null ? "" : colour.serialize();
    }
}
