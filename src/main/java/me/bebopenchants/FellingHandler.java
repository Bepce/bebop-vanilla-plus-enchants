package me.bebopenchants;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Felling: sneak-chop a log to fell the whole tree.
 * Level 1: thin (1x1 trunk) trees only. Level 2: also big 2x2 trees.
 */
public final class FellingHandler {

    private static final int MAX_BLOCKS_LEVEL_1 = 64;
    private static final int MAX_BLOCKS_LEVEL_2 = 256;

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

        // Level 1 can't fell thick trees: a log right beside the broken one means a 2x2 trunk.
        if (enchantLevel == 1 && hasHorizontalNeighbour(level, pos, log)) return;

        int limit = enchantLevel >= 2 ? MAX_BLOCKS_LEVEL_2 : MAX_BLOCKS_LEVEL_1;
        List<BlockPos> logs = VeinBreaker.findConnected(level, pos, log, limit);

        // Only fell real trees: at least one log must touch naturally grown leaves.
        // Player-placed leaves are "persistent", so log cabins and builds are safe.
        if (!touchesNaturalLeaves(level, pos, logs)) return;

        VeinBreaker.breakAll(serverPlayer, logs);
    }

    private static boolean hasHorizontalNeighbour(Level level, BlockPos pos, Block log) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(dir)).is(log)) return true;
        }
        return false;
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
