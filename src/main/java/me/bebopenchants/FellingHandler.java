package me.bebopenchants;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Felling: sneak-chop a log to fell the whole tree.
 * Level 1: thin trees only (1x1 trunk). Level 2: also big 2x2 trees and rows of touching trees.
 */
public final class FellingHandler {

    private static final int MAX_BLOCKS_LEVEL_1 = 64;
    private static final int MAX_BLOCKS_LEVEL_2 = 256;

    /**
     * Log types that only ever grow as thick (2x2) trees, so Felling I never works on them.
     * Defined in data/bebop-vanilla-plus-enchants/tags/block/thick_trees.json,
     * so modpacks can add their own thick trees without touching the code.
     */
    private static final TagKey<Block> THICK_TREES =
            TagKey.create(Registries.BLOCK, BebopVanillaPlusEnchants.id("thick_trees"));

    /** If true, the player must sneak for the enchantment to activate. */
    private static final boolean REQUIRE_SNEAK = true;

    private FellingHandler() {}

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(
                (level, player, pos, state, blockEntity) -> onBlockBroken(level, player, pos, state));
    }

    private static void onBlockBroken(Level level, Player player, BlockPos pos, BlockState state) {
        if (VeinBreaker.isBreaking()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (REQUIRE_SNEAK && !player.isShiftKeyDown()) return;
        if (!state.is(BlockTags.LOGS)) return;

        ItemStack tool = player.getMainHandItem();
        int enchantLevel = ModEnchantments.getLevel(level, ModEnchantments.FELLING, tool);
        if (enchantLevel <= 0) return;

        Block log = state.getBlock();

        // Always search with the bigger cap, so the thickness check sees the whole tree
        List<BlockPos> logs = VeinBreaker.findConnected(level, pos, log, MAX_BLOCKS_LEVEL_2);

        // Only fell real trees: at least one log must touch naturally grown leaves.
        // Player-placed leaves are "persistent", so log cabins and builds are safe.
        if (!touchesNaturalLeaves(level, pos, logs)) return;

        if (enchantLevel == 1) {
            if (state.is(THICK_TREES)) return;                       // dark oak, pale oak...
            if (hasSideBySideTrunks(level, pos, state, logs)) return; // 2x2 spruce/jungle, tree rows
            if (logs.size() > MAX_BLOCKS_LEVEL_1) logs = logs.subList(0, MAX_BLOCKS_LEVEL_1);
        }

        VeinBreaker.breakAll(serverPlayer, logs);
    }

    /**
     * True if anywhere in the tree two upright trunk logs stand side by side at the same height.
     * That's what a 2x2 trunk looks like (at every height, so removing a few blocks at the
     * bottom doesn't fool it), and also what a row of touching trees looks like.
     * Sideways logs (big oak and cherry branches) are ignored, so branchy trees still count as thin.
     */
    private static boolean hasSideBySideTrunks(Level level, BlockPos origin, BlockState originState, List<BlockPos> logs) {
        Set<BlockPos> trunks = new HashSet<>();
        // The broken log is already air, so use its old state
        if (isUpright(originState)) trunks.add(origin);
        for (BlockPos pos : logs) {
            if (isUpright(level.getBlockState(pos))) trunks.add(pos);
        }
        for (BlockPos pos : trunks) {
            // Checking east and south covers every side-by-side pair exactly once
            if (trunks.contains(pos.east()) || trunks.contains(pos.south())) return true;
        }
        return false;
    }

    /** Upright = growing along the Y axis. Logs without an axis (some modded ones) count as upright. */
    private static boolean isUpright(BlockState state) {
        return state.getValueOrElse(RotatedPillarBlock.AXIS, Direction.Axis.Y) == Direction.Axis.Y;
    }

    private static boolean touchesNaturalLeaves(Level level, BlockPos origin, List<BlockPos> logs) {
        if (isNextToNaturalLeaves(level, origin)) return true;
        for (BlockPos pos : logs) {
            if (isNextToNaturalLeaves(level, pos)) return true;
        }
        return false;
    }

    private static boolean isNextToNaturalLeaves(Level level, BlockPos pos) {
        for (BlockPos neighbour : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(neighbour);
            if (state.is(BlockTags.LEAVES)
                    && state.hasProperty(LeavesBlock.PERSISTENT)
                    && !state.getValue(LeavesBlock.PERSISTENT)) {
                return true;
            }
        }
        return false;
    }
}
