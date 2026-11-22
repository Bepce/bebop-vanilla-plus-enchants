package me.bebopenchants;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

/**
 * Adds our enchanted books to vanilla loot.
 * Each modified table gets one extra pool: "with X% chance, add one of these books".
 * The existing loot is untouched, so this stays compatible with other mods and datapacks.
 */
public final class ModLootTables {

    // Chance (0.0 - 1.0) that a chest / vault reward contains one of our books.
    private static final float MINESHAFT_CHANCE = 0.08f;
    private static final float DUNGEON_CHANCE = 0.10f;
    private static final float ANCIENT_CITY_CHANCE = 0.12f;
    private static final float TRIAL_VAULT_CHANCE = 0.10f;         // normal trial key -> Felling
    private static final float OMINOUS_TRIAL_VAULT_CHANCE = 0.15f; // ominous trial key -> Strip Mining

    /** TESTING ONLY: when true, every modified table always drops a book. Set back to false before release! */
    private static final boolean DEBUG_ALWAYS_DROP = false;

    private ModLootTables() {}

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            // Only touch the vanilla tables, not versions replaced by a datapack
            if (!source.isBuiltin()) return;

            if (key.equals(BuiltInLootTables.ABANDONED_MINESHAFT)) {
                addBooks(tableBuilder, registries, MINESHAFT_CHANCE,
                        ModEnchantments.STRIP_MINING, ModEnchantments.FELLING);
            } else if (key.equals(BuiltInLootTables.SIMPLE_DUNGEON)) {
                addBooks(tableBuilder, registries, DUNGEON_CHANCE,
                        ModEnchantments.STRIP_MINING, ModEnchantments.FELLING);
            } else if (key.equals(BuiltInLootTables.ANCIENT_CITY)) {
                addBooks(tableBuilder, registries, ANCIENT_CITY_CHANCE,
                        ModEnchantments.STRIP_MINING, ModEnchantments.FELLING);
            } else if (key.equals(BuiltInLootTables.TRIAL_CHAMBERS_REWARD)) {
                addBooks(tableBuilder, registries, TRIAL_VAULT_CHANCE,
                        ModEnchantments.FELLING);
            } else if (key.equals(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS)) {
                addBooks(tableBuilder, registries, OMINOUS_TRIAL_VAULT_CHANCE,
                        ModEnchantments.STRIP_MINING);
            }
        });
    }

    /**
     * Adds a pool that, with the given chance, gives one enchanted book.
     * If several enchantments are listed, each is equally likely.
     * The level is random between 1 and the enchantment's max level.
     */
    @SafeVarargs
    private static void addBooks(LootTable.Builder table, HolderLookup.Provider registries,
                                 float chance, ResourceKey<Enchantment>... enchantments) {
        LootPool.Builder pool = LootPool.lootPool()   // rolls default to 1
                .when(LootItemRandomChanceCondition.randomChance(DEBUG_ALWAYS_DROP ? 1.0f : chance));

        for (ResourceKey<Enchantment> key : enchantments) {
            Holder<Enchantment> enchantment = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
            pool.add(LootItem.lootTableItem(Items.BOOK)
                    .apply(new EnchantRandomlyFunction.Builder().withEnchantment(enchantment)));
        }

        table.withPool(pool);
    }
}
