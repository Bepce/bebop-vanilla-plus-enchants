package me.bebopenchants;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Shared logic for Strip Mining and Felling:
 * find connected blocks of the same type, then break them as the player.
 */
public final class VeinBreaker {

    /**
     * True while we are breaking extra blocks. destroyBlock() fires the block-break
     * event again for every block, so the handlers must ignore those events,
     * otherwise they would recurse forever and crash the game.
     */
    private static boolean breaking = false;

    private VeinBreaker() {}

    public static boolean isBreaking() {
        return breaking;
    }

    /**
     * Breadth-first search from {@code origin} for connected blocks of the same type.
     * Checks all 26 neighbours (diagonals included). Does not include the origin itself,
     * since the player already broke it.
     */
    public static List<BlockPos> findConnected(Level level, BlockPos origin, Block target, int limit) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos current = queue.poll();

            for (BlockPos neighbour : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                // betweenClosed reuses one mutable BlockPos, so store an immutable copy
                BlockPos pos = neighbour.immutable();
                if (!visited.add(pos)) continue;

                if (level.getBlockState(pos).is(target)) {
                    found.add(pos);
                    queue.add(pos);
                    if (found.size() >= limit) break;
                }
            }
        }
        return found;
    }

    /**
     * Breaks each block as if the player mined it: correct drops (Fortune/Silk Touch),
     * XP, tool durability, hunger and statistics are all handled by vanilla.
     * Stops early so the tool is never destroyed.
     *
     * Note: Fabric's AFTER event fires before vanilla damages the tool for the block the
     * player broke by hand. So while this runs, that 1 point of damage is still "pending",
     * and we must keep 2 durability in reserve: 1 for the pending hit, 1 left on the tool.
     */
    public static void breakAll(ServerPlayer player, List<BlockPos> positions) {
        ItemStack startTool = player.getMainHandItem();

        breaking = true;
        try {
            for (BlockPos pos : positions) {
                // Re-read the held item every time, in case it changed or broke
                ItemStack tool = player.getMainHandItem();
                if (tool.isEmpty() || tool != startTool || isAboutToBreak(tool)) break;
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            breaking = false;
        }
    }

    /** Durability kept in reserve: 1 for the player's own (still pending) break + 1 left on the tool. */
    private static final int RESERVED_DURABILITY = 2;

    /** True when breaking another block could destroy the tool (creative / unbreakable tools never are). */
    private static boolean isAboutToBreak(ItemStack tool) {
        return tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= RESERVED_DURABILITY;
    }
}
