package me.bebopenchants;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * IDs ("addresses") of our data-driven enchantments.
 * The enchantment JSON files live in data/bebop-vanilla-plus-enchants/enchantment/.
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> STRIP_MINING = key("strip_mining");
    public static final ResourceKey<Enchantment> FELLING = key("felling");

    private ModEnchantments() {}

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, BebopVanillaPlusEnchants.id(path));
    }

    /** Returns the level of the given enchantment on the stack, or 0 if it doesn't have it. */
    public static int getLevel(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Holder<Enchantment> holder = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }
}
