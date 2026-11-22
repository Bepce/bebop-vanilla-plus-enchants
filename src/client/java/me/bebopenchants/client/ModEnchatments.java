package me.bebopenchants.client;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchatments {
    public static final ResourceKey<Enchantment> STRIP_MINING = key("strip_mining");
    public static final ResourceKey<Enchantment> FELLING = key("felling");

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                Identifier.fromNamespaceAndPath("bebop-vanilla-plus-enchants", path));
    }
}
