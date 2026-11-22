package lv.id.bonne.vhdiscord.parser;


/**
 * Bounds for text built from numbers in a linked item's NBT. A chat link carries the client's own item tag, so a count
 * read from it must never decide how much the server allocates.
 */
public final class TooltipLimits
{
    public static final int MAXIMUM_REPEATED_SYMBOLS = 64;

    public static final int MAXIMUM_DECK_SOCKETS = 32;

    public static final int MAXIMUM_DECK_SLOTS = 256;

    public static final int MAXIMUM_DESCRIPTION_LENGTH = 16_000;


    private TooltipLimits()
    {
    }


    /**
     * A symbol repeated as often as a count asks, at most {@link #MAXIMUM_REPEATED_SYMBOLS} times.
     *
     * @param symbol the text to repeat.
     * @param count the count from the item, may be negative or absurd.
     * @return the repeated text.
     */
    public static String repeat(String symbol, int count)
    {
        return symbol.repeat(Math.max(0, Math.min(count, MAXIMUM_REPEATED_SYMBOLS)));
    }


    /**
     * Whether a card deck is small enough for its text and picture to be built.
     *
     * @param socketCount the deck's socket count.
     * @param slotCount the number of card slots in the deck.
     * @return true for any deck the game makes.
     */
    public static boolean isDescribableDeck(int socketCount, int slotCount)
    {
        return socketCount <= MAXIMUM_DECK_SOCKETS && slotCount <= MAXIMUM_DECK_SLOTS;
    }


    /**
     * Whether a finished description is short enough to attach to a chat line.
     *
     * @param description the description.
     * @return true when it is within {@link #MAXIMUM_DESCRIPTION_LENGTH}.
     */
    public static boolean isAttachableDescription(String description)
    {
        return description.length() <= MAXIMUM_DESCRIPTION_LENGTH;
    }
}
