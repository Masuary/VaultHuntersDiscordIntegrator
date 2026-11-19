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
import com.masuary.masuchatlink.api.TooltipSegment;
import com.masuary.masuchatlink.api.TooltipSlot;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import iskallia.vault.item.modification.ReforgeTagModificationFocus;
import lv.id.bonne.vhdiscord.parser.TooltipColorHints;
import lv.id.bonne.vhdiscord.parser.VaultItemsHandler;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


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


    /**
     * The modifier colours noted while the last tooltip was built (chat events run on the server thread, one at a time).
     */
    private TooltipColorHints.Hints colourHints = TooltipColorHints.Hints.EMPTY;

    /**
     * The logger: a failed picture is reported once per item id, the chat entry keeps MasuChatLink's own item.
     */
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Item ids whose picture failed, so each is logged once.
     */
    private static final Set<String> FAILED_ITEMS = ConcurrentHashMap.newKeySet();


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
                    hoverEvent.getAsJsonObject("contents").has("id"))
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
            ChatRelayItem generic = event.getItem();
            List<TooltipSlot> slots = this.coinSlotsOf(itemStack, itemTag);

            TooltipColorHints.start();
            String description;
            String clientEventLines = "";

            try
            {
                description = VaultItemsHandler.generateVaultHuntersItemTooltips(itemJson, itemStack, itemTag);

                if (description == null || description.isBlank())
                {
                    clientEventLines = VaultItemsHandler.clientEventLines(itemStack);
                }
            }
            finally
            {
                this.colourHints = TooltipColorHints.take();
            }

            if (description == null || description.isBlank())
            {
                if (generic != null && generic.lines() != null && generic.lines().size() >= 2 && !clientEventLines.isBlank())
                {
                    ChatRelayItem withEventLines = this.withClientEventLines(generic, clientEventLines);
                    event.setItem(slots == null ? withEventLines : withEventLines.withSlots(slots));
                }
                else if (generic != null && slots != null)
                {
                    event.setItem(generic.withSlots(slots));
                }

                return;
            }

            String itemName = itemStack.hasCustomHoverName() ? itemStack.getDisplayName().getContents() :
                new TranslatableComponent(itemStack.getItem().getDescriptionId()).getContents();
            String title = itemName.isBlank() ? itemStack.getHoverName().getString() : itemName;

            List<List<TooltipSegment>> serverLines = generic == null || generic.lines() == null ? List.of() : generic.lines();
            List<TooltipSegment> modLine = serverLines.isEmpty() ? null : serverLines.get(serverLines.size() - 1);
            Optional<DeckPicture.Picture> deck = DeckPicture.of(itemStack, modLine);

            event.setItem(new ChatRelayItem(title,
                this.colourOf(itemStack),
                description,
                deck.map(DeckPicture.Picture::lines).orElseGet(() -> this.linesOf(generic, title, itemStack, description)),
                slots,
                deck.map(DeckPicture.Picture::deck).orElse(null)));
        }
        catch (CommandSyntaxException ignored)
        {
        }
        catch (RuntimeException | LinkageError failure)
        {
            String id = itemJson.has("id") ? itemJson.get("id").getAsString() : "an unknown item";

            if (FAILED_ITEMS.add(id))
            {
                LOGGER.warn("Could not build the tooltip picture of {}; it is posted without one", id, failure);
            }
        }
    }


    /**
     * The tooltip lines for the in-game picture: this handler's description in the game's colours, followed by the lines
     * of the tooltip MasuChatLink built on the server that the description lacks (enchantments: on a server Vault Hunters
     * builds only those vanilla parts, its own gear tooltip is client-only).
     *
     * @param generic the item MasuChatLink attached, or null.
     * @param title the item's name.
     * @param itemStack the item.
     * @param description the description VaultItemsHandler wrote.
     * @return the lines.
     */
    private List<List<TooltipSegment>> linesOf(ChatRelayItem generic, String title, ItemStack itemStack, String description)
    {
        List<List<TooltipSegment>> serverLines = generic == null || generic.lines() == null ? List.of() : generic.lines();
        List<TooltipSegment> modLine = serverLines.isEmpty() ? null : serverLines.get(serverLines.size() - 1);
        String described = description.replace("**", "");
        List<List<TooltipSegment>> missing = serverLines.size() <= 2 || this.hidesServerLines(itemStack) ? List.of() :
            serverLines.subList(1, serverLines.size() - 1).stream().
                filter(line -> !line.isEmpty()).
                filter(line -> !MasuChatLinkEventListener.isDescribed(described, line)).
                toList();

        return MarkdownTooltipLines.convert(this.titleLineOf(itemStack, title), description, this.colourHints, missing, modLine);
    }


    /**
     * MasuChatLink's item with the lines client tooltip events add (Curios slot, Soul Value, tooltip config description)
     * under its name, above the server tooltip lines, which stay as they are.
     *
     * @param generic the item MasuChatLink attached, with its name line first and its mod line last.
     * @param clientEventLines the client event lines as markdown.
     * @return the item with those lines.
     */
    private ChatRelayItem withClientEventLines(ChatRelayItem generic, String clientEventLines)
    {
        List<List<TooltipSegment>> serverLines = generic.lines();
        List<List<TooltipSegment>> following = new ArrayList<>();

        if (clientEventLines.endsWith("\n\n"))
        {
            following.add(List.of());
        }

        following.addAll(serverLines.subList(1, serverLines.size() - 1));
        List<List<TooltipSegment>> lines = MarkdownTooltipLines.convert(serverLines.get(0),
            clientEventLines,
            this.colourHints,
            following,
            serverLines.get(serverLines.size() - 1));
        String description = clientEventLines.replace("**", "").strip() + "\n" + generic.description();

        return new ChatRelayItem(generic.name(), generic.colour(), description.strip(), lines, generic.slots(), generic.deck());
    }


    /**
     * Whether a server tooltip line is already in the description; a line holding line breaks (config lore) counts when
     * each of its lines is.
     *
     * @param described the description without markdown.
     * @param line the server line.
     * @return true when the description has it.
     */
    private static boolean isDescribed(String described, List<TooltipSegment> line)
    {
        String text = line.stream().map(TooltipSegment::text).collect(Collectors.joining());

        return text.lines().map(String::strip).filter(part -> !part.isEmpty()).allMatch(described::contains);
    }


    /**
     * Whether the item's server tooltip must not be merged: Forge strips a faceted focus's client-only tooltip on the
     * server, so its server lines are the parent focus text the game never shows.
     *
     * @param itemStack the item.
     * @return true when the server lines are left out.
     */
    private boolean hidesServerLines(ItemStack itemStack)
    {
        return itemStack.getItem() instanceof ReforgeTagModificationFocus;
    }


    /**
     * The name line as the game draws it: the hover name's runs in their colours, italic for a renamed item.
     *
     * @param itemStack the item.
     * @param title the name text, used when the hover name has no text of its own.
     * @return the name's segments.
     */
    private List<TooltipSegment> titleLineOf(ItemStack itemStack, String title)
    {
        Component name = itemStack.getHoverName();
        boolean renamed = itemStack.hasCustomHoverName();
        List<TooltipSegment> segments = new ArrayList<>();

        name.visit((style, text) ->
        {
            if (!text.isEmpty())
            {
                TextColor colour = style.getColor();
                String hex = colour == null ? null : String.format("#%06X", colour.getValue() & 0xFFFFFF);
                segments.add(new TooltipSegment(text, hex, style.isBold(), style.isItalic() || renamed));
            }

            return Optional.empty();
        }, Style.EMPTY);

        if (segments.isEmpty() || !name.getString().equals(title))
        {
            TextColor colour = name.getStyle().getColor();
            return List.of(new TooltipSegment(title, colour == null ? null : String.format("#%06X", colour.getValue() & 0xFFFFFF), false, renamed));
        }

        return segments;
    }


    /**
     * The coins in a Vault Hunters Coin Pouch as slots for the picture; the game draws them on the client only, but the
     * counts are on the item.
     *
     * @param itemStack the item.
     * @param itemTag the item's tag.
     * @return the non-empty coin slots, or null when the item is not a coin pouch.
     */
    private List<TooltipSlot> coinSlotsOf(ItemStack itemStack, CompoundTag itemTag)
    {
        ResourceLocation id = VH_DISCORD_ITEM_REGISTRY.getKey(itemStack.getItem());

        if (id == null || !id.toString().equals("the_vault:coin_pouch"))
        {
            return null;
        }

        CompoundTag coins = itemTag.getCompound("Inventory");
        List<TooltipSlot> slots = new ArrayList<>();
        String[][] coinKeys = {
            { "BronzeStackSize", "coin_bronze" },
            { "SilverStackSize", "coin_silver" },
            { "GoldStackSize", "coin_gold" },
            { "PlatinumStackSize", "coin_platinum" },
            { "GreedCoinStackSize", "coin_greed" } };

        for (String[] coin : coinKeys)
        {
            int count = coins.getInt(coin[0]);

            if (count > 0)
            {
                slots.add(new TooltipSlot(coin[1], count));
            }
        }

        return slots;
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
