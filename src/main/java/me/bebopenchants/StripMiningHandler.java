package me.bebopenchants;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Strip Mining: sneak-mine an ore to mine the whole connected vein. */
public final class StripMiningHandler {

    /** Max extra blocks broken per activation. */
    private static final int MAX_BLOCKS = 64;

    /** If true, the player must sneak for the enchantment to activate. */
    private static final boolean REQUIRE_SNEAK = true;

    private StripMiningHandler() {}

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(
                (level, player, pos, state, blockEntity) -> onBlockBroken(level, player, pos, state));
    }

    private static void onBlockBroken(Level level, Player player, BlockPos pos, BlockState state) {
        if (VeinBreaker.isBreaking()) return;                 // ignore our own extra breaks
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (REQUIRE_SNEAK && !player.isShiftKeyDown()) return;
        if (!state.is(ConventionalBlockTags.ORES)) return;    // only ores (vanilla + other mods)

        ItemStack tool = player.getMainHandItem();
        if (ModEnchantments.getLevel(level, ModEnchantments.STRIP_MINING, tool) <= 0) return;
        if (!tool.isCorrectToolForDrops(state)) return;       // e.g. wooden pickaxe on diamond ore

        List<BlockPos> vein = VeinBreaker.findConnected(level, pos, state.getBlock(), MAX_BLOCKS);
        VeinBreaker.breakAll(serverPlayer, vein);
    }
}
