//
// Created by Masuary
// Copyright - 2026
//


package lv.id.bonne.vhdiscord.parser;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import iskallia.vault.core.card.CardDeck;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.VaultUtils;
import iskallia.vault.gear.attribute.VaultGearAttributeInstance;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.item.AugmentItem;
import iskallia.vault.item.ItemShardPouch;
import iskallia.vault.item.SoulFlameItem;
import iskallia.vault.item.bottle.BottleItem;
import iskallia.vault.item.crystal.modifiers.CrystalModifiers;
import iskallia.vault.item.gear.VaultCharmItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


/**
 * The item API that differs from Vault Hunters Remastered 7209, which this mod compiles against: the API before 7209 that
 * Wold's Vaults (Vault Hunters 3.21) still runs, and the API added since (7226, Skyvault Update 4 on pack 4.0.1). Those
 * members are reached by reflection, and only where the running game has them.
 */
final class LegacyVaultItemApi
{
    private static final Optional<Method> BOTTLE_TYPE = LegacyVaultItemApi.findMethod(BottleItem.class, "getType", ItemStack.class);

    private static final Optional<Method> BOTTLE_RECHARGE = LegacyVaultItemApi.findMethod(BottleItem.class, "getRecharge", ItemStack.class);

    private static final Optional<Method> BOTTLE_EFFECT = LegacyVaultItemApi.findMethod(BottleItem.class, "getEffect", ItemStack.class);

    private static final Optional<Method> SOUL_FLAME_OWNER_UUID = LegacyVaultItemApi.findMethod(SoulFlameItem.class, "getOwnerUUID", ItemStack.class);

    private static final Optional<Method> SOUL_FLAME_OWNER_NAME = LegacyVaultItemApi.findMethod(SoulFlameItem.class, "getOwnerName", ItemStack.class);

    private static final Optional<Method> SOUL_FLAME_MODIFIERS = LegacyVaultItemApi.findMethod(SoulFlameItem.class, "getModifiers", ItemStack.class);

    private static final Optional<Method> SHARD_POUCH_CONTAINED_STACK =
        LegacyVaultItemApi.findMethod(ItemShardPouch.class, "getContainedStack", ItemStack.class);

    private static final Optional<Method> DECK_SNAPSHOT_ATTRIBUTES = LegacyVaultItemApi.findMethod(CardDeck.class, "getSnapshotAttributes");

    private static final Optional<Method> CHARM_LIVE_SCALE = LegacyVaultItemApi.findMethod(VaultCharmItem.class, "getLiveScale", ItemStack.class, int.class);

    private static final Optional<Method> AUGMENT_SERIES = LegacyVaultItemApi.findMethod(AugmentItem.class, "getSeries", ItemStack.class);

    private static final Optional<Method> TEMPORAL_MODIFIER_LOCK =
        LegacyVaultItemApi.findMethod(VaultUtils.class, "hasNoTemporalModifier", Vault.class);

    private static final Optional<Method> BOUGHT_ETCHING =
        LegacyVaultItemApi.findMethod("iskallia.vault.greed.house.HouseItems", "isBoughtEtching", ItemStack.class);

    private static final Optional<Method> GREEDY_MEAL_XP_RANGE =
        LegacyVaultItemApi.findMethod("iskallia.vault.config.greed.GreedBoxConfig", "getGreedyMealVaultXpRange");

    private static final Optional<Class<?>> ACTIVE_POTION = LegacyVaultItemApi.findClass("iskallia.vault.item.bottle.ActivePotion");


    private LegacyVaultItemApi()
    {
    }


    /**
     * Whether potion bottles have the types, recharge kinds and single effects of Vault Hunters before 7209.
     *
     * @return true on Wold's Vaults and older Vault Hunters builds.
     */
    static boolean hasBottleTypes()
    {
        return BOTTLE_TYPE.isPresent() && BOTTLE_RECHARGE.isPresent() && BOTTLE_EFFECT.isPresent();
    }


    /**
     * The bottle's type, an enum the game names {@code BottleItem.Type}.
     *
     * @param bottle the bottle.
     * @return the type, or empty when the bottle has none.
     */
    static Optional<Object> bottleType(ItemStack bottle)
    {
        return LegacyVaultItemApi.optionalResult(BOTTLE_TYPE, null, bottle);
    }


    /**
     * The name of the way the bottle restores charges (TIME, MOBS or CHESTS).
     *
     * @param bottle the bottle.
     * @return the recharge kind's name, or empty when the bottle has none.
     */
    static Optional<String> bottleRechargeName(ItemStack bottle)
    {
        return LegacyVaultItemApi.optionalResult(BOTTLE_RECHARGE, null, bottle).map(recharge -> ((Enum<?>) recharge).name());
    }


    /**
     * The hit points one charge of a bottle type heals, from the potion config.
     *
     * @param bottleType the bottle's type.
     * @return the healing.
     */
    static int bottleHealing(Object bottleType)
    {
        Class<?> typeClass = ((Enum<?>) bottleType).getDeclaringClass();
        Method getPotion = LegacyVaultItemApi.findMethod(ModConfigs.POTION.getClass(), "getPotion", typeClass).
            orElseThrow(() -> new IllegalStateException("PotionConfig.getPotion(" + typeClass.getName() + ") is missing"));
        Object potion = LegacyVaultItemApi.invoke(getPotion, ModConfigs.POTION, bottleType);
        Method getHealing = LegacyVaultItemApi.findMethod(potion.getClass(), "getHealing").
            orElseThrow(() -> new IllegalStateException(potion.getClass().getName() + ".getHealing() is missing"));

        return (Integer) LegacyVaultItemApi.invoke(getHealing, potion);
    }


    /**
     * The tooltip line of the bottle's extra effect.
     *
     * @param bottle the bottle.
     * @return the effect's line, or empty when the bottle has no effect.
     */
    static Optional<Component> bottleEffectTooltip(ItemStack bottle)
    {
        return LegacyVaultItemApi.optionalResult(BOTTLE_EFFECT, null, bottle).map(effect ->
        {
            Method getTooltip = LegacyVaultItemApi.findMethod(effect.getClass(), "getTooltip").
                orElseThrow(() -> new IllegalStateException(effect.getClass().getName() + ".getTooltip() is missing"));
            return (Component) LegacyVaultItemApi.invoke(getTooltip, effect);
        });
    }


    /**
     * Whether soul flames carry an owner and crystal modifiers, as before 7209.
     *
     * @return true on Wold's Vaults and older Vault Hunters builds.
     */
    static boolean hasSoulFlameOwner()
    {
        return SOUL_FLAME_OWNER_UUID.isPresent() && SOUL_FLAME_OWNER_NAME.isPresent() && SOUL_FLAME_MODIFIERS.isPresent();
    }


    static Optional<UUID> soulFlameOwnerUuid(ItemStack soulFlame)
    {
        return LegacyVaultItemApi.optionalResult(SOUL_FLAME_OWNER_UUID, null, soulFlame).map(UUID.class::cast);
    }


    static Optional<String> soulFlameOwnerName(ItemStack soulFlame)
    {
        return LegacyVaultItemApi.optionalResult(SOUL_FLAME_OWNER_NAME, null, soulFlame).map(String.class::cast);
    }


    static Optional<CrystalModifiers> soulFlameModifiers(ItemStack soulFlame)
    {
        return LegacyVaultItemApi.optionalResult(SOUL_FLAME_MODIFIERS, null, soulFlame).map(CrystalModifiers.class::cast);
    }


    /**
     * Whether a shard pouch holds its shards as an item stack, as before 7209.
     *
     * @return true on Wold's Vaults and older Vault Hunters builds.
     */
    static boolean hasShardPouchStack()
    {
        return SHARD_POUCH_CONTAINED_STACK.isPresent();
    }


    static ItemStack shardPouchContainedStack(ItemStack shardPouch)
    {
        return (ItemStack) LegacyVaultItemApi.invoke(LegacyVaultItemApi.required(SHARD_POUCH_CONTAINED_STACK), null, shardPouch);
    }


    /**
     * Whether a god charm's values are the ones it rolled, with its stored reputation shown, as before 7209 (7209 scales
     * them by the viewer's current reputation with the charm's god).
     *
     * @return true on Wold's Vaults and older Vault Hunters builds.
     */
    static boolean hasFixedCharmValues()
    {
        return CHARM_LIVE_SCALE.isEmpty();
    }


    /**
     * Whether augments can belong to a theme series, as since 7209.
     *
     * @return true on Vault Hunters 7209 and later, false on Wold's Vaults.
     */
    static boolean hasThemeSeries()
    {
        return AUGMENT_SERIES.isPresent();
    }


    /**
     * Whether a vault's objective can lock Temporal Shards, as since 7209.
     *
     * @return true on Vault Hunters 7209 and later, false on Wold's Vaults.
     */
    static boolean hasTemporalModifierLock()
    {
        return TEMPORAL_MODIFIER_LOCK.isPresent();
    }


    /**
     * Whether etchings show a plain greed tier and their gear type groups, without the Etching Table lines, as before 7209.
     *
     * @return true on Wold's Vaults and older Vault Hunters builds.
     */
    static boolean hasPlainEtchingLines()
    {
        return BOUGHT_ETCHING.isEmpty();
    }


    /**
     * Whether Greedy Meals have no configured Vault XP range and no greed tier lock, as before 7209.
     *
     * @return true on Wold's Vaults and older Vault Hunters builds.
     */
    static boolean hasUnrangedGreedyMeal()
    {
        return GREEDY_MEAL_XP_RANGE.isEmpty();
    }


    /**
     * Whether a deck's stats do not depend on the greed tier, as before 7209.
     *
     * @return true on Wold's Vaults and older Vault Hunters builds.
     */
    static boolean hasTierlessDeckAttributes()
    {
        return DECK_SNAPSHOT_ATTRIBUTES.isPresent();
    }


    @SuppressWarnings("unchecked")
    static List<VaultGearAttributeInstance<?>> deckSnapshotAttributes(CardDeck deck)
    {
        return (List<VaultGearAttributeInstance<?>>) LegacyVaultItemApi.invoke(LegacyVaultItemApi.required(DECK_SNAPSHOT_ATTRIBUTES), deck);
    }


    /**
     * Whether one carried potion is the active one and the others are spares (Vault Hunters 7226 and later).
     *
     * @return true on 7226 and later.
     */
    static boolean hasActivePotion()
    {
        return ACTIVE_POTION.isPresent();
    }


    /**
     * The game's {@code ActivePotion.of(Player)}: the potion that counts for the player.
     *
     * @param player the player.
     * @return the very carried stack, or the empty stack.
     */
    static ItemStack activePotion(Player player)
    {
        return (ItemStack) LegacyVaultItemApi.invoke(LegacyVaultItemApi.activePotionMethod("of"), null, player);
    }


    /**
     * The game's {@code ActivePotion.carried(Player)}: the potions the player carries, in the game's order.
     *
     * @param player the player.
     * @return the very carried stacks.
     */
    static List<ItemStack> carriedPotions(Player player)
    {
        Stream<?> carried = (Stream<?>) LegacyVaultItemApi.invoke(LegacyVaultItemApi.activePotionMethod("carried"), null, player);
        return carried.map(ItemStack.class::cast).toList();
    }


    private static Method activePotionMethod(String name)
    {
        Class<?> activePotion = ACTIVE_POTION.orElseThrow(() -> new IllegalStateException("This Vault Hunters build has no ActivePotion"));
        return LegacyVaultItemApi.findMethod(activePotion, name, Player.class).
            orElseThrow(() -> new IllegalStateException(activePotion.getName() + "." + name + "(Player) is missing"));
    }


    private static Optional<Class<?>> findClass(String name)
    {
        try
        {
            return Optional.of(Class.forName(name, false, LegacyVaultItemApi.class.getClassLoader()));
        }
        catch (ClassNotFoundException absent)
        {
            return Optional.empty();
        }
    }


    private static Optional<Method> findMethod(String ownerName, String name, Class<?>... parameterTypes)
    {
        return LegacyVaultItemApi.findClass(ownerName).flatMap(owner -> LegacyVaultItemApi.findMethod(owner, name, parameterTypes));
    }


    private static Optional<Method> findMethod(Class<?> owner, String name, Class<?>... parameterTypes)
    {
        try
        {
            return Optional.of(owner.getMethod(name, parameterTypes));
        }
        catch (NoSuchMethodException absent)
        {
            return Optional.empty();
        }
    }


    private static Method required(Optional<Method> method)
    {
        return method.orElseThrow(() -> new IllegalStateException("This Vault Hunters build does not have the pre-7209 item API"));
    }


    private static Optional<Object> optionalResult(Optional<Method> method, Object target, Object... arguments)
    {
        Object result = LegacyVaultItemApi.invoke(LegacyVaultItemApi.required(method), target, arguments);

        if (!(result instanceof Optional<?> optional))
        {
            throw new IllegalStateException(method.get() + " did not return an Optional");
        }

        return optional.map(Object.class::cast);
    }


    private static Object invoke(Method method, Object target, Object... arguments)
    {
        try
        {
            return method.invoke(target, arguments);
        }
        catch (IllegalAccessException failure)
        {
            throw new IllegalStateException("Cannot call " + method, failure);
        }
        catch (InvocationTargetException failure)
        {
            throw new IllegalStateException(method + " failed", failure.getCause());
        }
    }
}
