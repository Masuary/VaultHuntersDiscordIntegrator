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

import iskallia.vault.core.card.CardDeck;
import iskallia.vault.gear.attribute.VaultGearAttributeInstance;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.item.ItemShardPouch;
import iskallia.vault.item.SoulFlameItem;
import iskallia.vault.item.bottle.BottleItem;
import iskallia.vault.item.crystal.modifiers.CrystalModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;


/**
 * The item API Vault Hunters had before Remastered 7209 (Skyvault Update 4) and Wold's Vaults (Vault Hunters 3.21) still
 * runs. This mod compiles against 7209, so those members are reached by reflection, and only where the running game has
 * them.
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
