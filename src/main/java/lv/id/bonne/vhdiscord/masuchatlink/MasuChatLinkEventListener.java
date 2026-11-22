//
// Created by Masuary
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
import iskallia.vault.item.CoinPouchItem;
import iskallia.vault.item.modification.ReforgeTagModificationFocus;
import lv.id.bonne.vhdiscord.parser.TooltipColorHints;
import lv.id.bonne.vhdiscord.parser.TooltipLimits;
import lv.id.bonne.vhdiscord.parser.VaultItemsHandler;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    /**
     * Item ids whose description was over the length limit, so each is logged once.
     */
    private static final Set<String> OVERSIZED_ITEMS = ConcurrentHashMap.newKeySet();

    private static final String POUCH_OWNER_KEY = "Owner";

    /**
     * The Vault Pouch's contents to the site's slot icons.
     */
    private static final Map<String, String> POUCH_SLOT_ICONS = Map.of(
        "the_vault:vault_bronze", "coin_bronze",
        "the_vault:vault_silver", "coin_silver",
        "the_vault:vault_gold", "coin_gold",
        "the_vault:vault_platinum", "coin_platinum",
        "the_vault:greed_coin", "coin_greed",
        "the_vault:soul_dust", "soul_dust",
        "the_vault:soul_shard", "soul_shard",
        "the_vault:ember", "ember");


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
            List<TooltipSlot> slots = this.pouchSlotsOf(itemStack, itemTag, event.getPlayer());

            TooltipColorHints.start();
            String generated;
            String clientEventLines = "";

            try
            {
                generated = VaultItemsHandler.generateVaultHuntersItemTooltips(itemJson, itemStack, itemTag, event.getPlayer());

                if (generated == null || generated.isBlank())
                {
                    clientEventLines = VaultItemsHandler.clientEventLines(itemStack);
                }
            }
            finally
            {
                this.colourHints = TooltipColorHints.take();
            }

            String description = MasuChatLinkEventListener.attachableOrNull(itemJson.get("id").getAsString(), generated);

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
            Optional<Component> playerTitle = VaultItemsHandler.playerTitleOf(itemStack, event.getPlayer());
            Component name = playerTitle.orElseGet(itemStack::getHoverName);
            String title = playerTitle.map(Component::getString).orElse(itemName.isBlank() ? itemStack.getHoverName().getString() : itemName);

            List<List<TooltipSegment>> serverLines = generic == null || generic.lines() == null ? List.of() : generic.lines();
            List<TooltipSegment> modLine = serverLines.isEmpty() ? null : serverLines.get(serverLines.size() - 1);
            Optional<DeckPicture.Picture> deck = DeckPicture.of(itemStack, modLine, event.getPlayer());

            event.setItem(new ChatRelayItem(title,
                this.colourOf(name),
                description,
                deck.map(DeckPicture.Picture::lines).orElseGet(() -> this.linesOf(generic, name, title, itemStack, description)),
                slots,
                deck.map(DeckPicture.Picture::deck).orElse(null)));
        }
        catch (CommandSyntaxException unparseable)
        {
            LOGGER.debug("A linked item's tag does not parse: {}", unparseable.getMessage());
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
     * The description when it is short enough to attach; an oversized one (only a forged item makes one) is logged once
     * per item id and dropped.
     *
     * @param itemId the item id.
     * @param description the generated description, or null.
     * @return the description, or null.
     */
    private static String attachableOrNull(String itemId, String description)
    {
        if (description == null || TooltipLimits.isAttachableDescription(description))
        {
            return description;
        }

        if (OVERSIZED_ITEMS.add(itemId))
        {
            LOGGER.warn("The description of a linked {} is {} characters long; it is posted without it", itemId, description.length());
        }

        return null;
    }


    /**
     * The tooltip lines for the in-game picture: this handler's description in the game's colours, followed by the lines
     * of the tooltip MasuChatLink built on the server that the description lacks (enchantments: on a server Vault Hunters
     * builds only those vanilla parts, its own gear tooltip is client-only).
     *
     * @param generic the item MasuChatLink attached, or null.
     * @param name the item's name as the game shows it to the linking player.
     * @param title the item's name.
     * @param itemStack the item.
     * @param description the description VaultItemsHandler wrote.
     * @return the lines.
     */
    private List<List<TooltipSegment>> linesOf(ChatRelayItem generic, Component name, String title, ItemStack itemStack, String description)
    {
        List<List<TooltipSegment>> serverLines = generic == null || generic.lines() == null ? List.of() : generic.lines();
        List<TooltipSegment> modLine = serverLines.isEmpty() ? null : serverLines.get(serverLines.size() - 1);
        String described = description.replace("**", "");
        List<List<TooltipSegment>> missing = serverLines.size() <= 2 || this.hidesServerLines(itemStack) ? List.of() :
            serverLines.subList(1, serverLines.size() - 1).stream().
                filter(line -> !line.isEmpty()).
                filter(line -> !MasuChatLinkEventListener.isDescribed(described, line)).
                toList();

        return MarkdownTooltipLines.convert(this.titleLineOf(name, itemStack.hasCustomHoverName(), title), description, this.colourHints, missing, modLine);
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
     * The name line as the game draws it: the name's runs in their colours, italic for a renamed item.
     *
     * @param name the item's name as the game shows it.
     * @param renamed whether the item carries a custom name.
     * @param title the name text, used when the name has no text of its own.
     * @return the name's segments.
     */
    private List<TooltipSegment> titleLineOf(Component name, boolean renamed, String title)
    {
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
     * The contents of a Vault Hunters Vault Pouch as slots for the picture, in the game's slot order; the game draws them
     * on the client only. Since Vault Hunters 7209 the pouch holds only its owner and the contents are kept per player,
     * so a pouch owned by someone else than the linking player shows none. Before 7209 (Wold's Vaults) the coins are on
     * the item.
     *
     * @param itemStack the item.
     * @param itemTag the item's tag.
     * @param linkingPlayer the player who linked the pouch, or null when unknown.
     * @return the non-empty slots, or null when the item is not a Vault Pouch.
     */
    private List<TooltipSlot> pouchSlotsOf(ItemStack itemStack, CompoundTag itemTag, ServerPlayer linkingPlayer)
    {
        ResourceLocation id = VH_DISCORD_ITEM_REGISTRY.getKey(itemStack.getItem());

        if (id == null || !id.toString().equals("the_vault:coin_pouch"))
        {
            return null;
        }

        if (itemTag.hasUUID(POUCH_OWNER_KEY) && (linkingPlayer == null || !itemTag.getUUID(POUCH_OWNER_KEY).equals(linkingPlayer.getUUID())))
        {
            return List.of();
        }

        List<TooltipSlot> slots = new ArrayList<>();

        for (ItemStack contained : CoinPouchItem.getContainedStacks(itemStack))
        {
            ResourceLocation containedId = VH_DISCORD_ITEM_REGISTRY.getKey(contained.getItem());
            String icon = containedId == null ? null : POUCH_SLOT_ICONS.get(containedId.toString());

            if (icon != null && contained.getCount() > 0)
            {
                slots.add(new TooltipSlot(icon, contained.getCount()));
            }
        }

        return slots;
    }


    /**
     * Reads the name's own text colour (Vault Hunters applies the rarity/tier colour to the item's display name for every
     * item type this handles), falling back to an empty string when the name carries no explicit colour.
     *
     * @param name the item's name as the game shows it.
     * @return the serialised colour (a named colour or {@code #rrggbb}), or an empty string.
     */
    private String colourOf(Component name)
    {
        TextColor colour = name.getStyle().getColor();
        return colour == null ? "" : colour.serialize();
    }
}
