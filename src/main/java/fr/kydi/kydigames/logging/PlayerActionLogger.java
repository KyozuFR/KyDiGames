package fr.kydi.kydigames.logging;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;

public final class PlayerActionLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("KyDiGames/PlayerActions");

    private PlayerActionLogger() {
    }

    public static void logBlockBreak(PlayerEntity player, BlockPos pos, BlockState state) {
        if (!shouldLog(player) || state == null || pos == null) {
            return;
        }
        LOGGER.info("[BlockBreak] player={} uuid={} block={} pos={} dimension={}",
                player.getName().getString(),
                player.getUuidAsString(),
                idForBlock(state),
                formatPos(pos),
                dimensionId(player.getEntityWorld()));
    }

    public static void logBlockPlace(PlayerEntity player, BlockPos pos, BlockState state) {
        if (!shouldLog(player) || pos == null || state == null) {
            return;
        }
        LOGGER.info("[BlockPlace] player={} uuid={} block={} pos={} dimension={}",
                player.getName().getString(),
                player.getUuidAsString(),
                idForBlock(state),
                formatPos(pos),
                dimensionId(player.getEntityWorld()));
    }

    public static void logInventorySet(PlayerEntity player, int slot, ItemStack previous, ItemStack current) {
        if (!shouldLog(player) || (Objects.equals(previous, current))) {
            return;
        }
        LOGGER.info("[InventorySet] player={} uuid={} slot={} previous={} current={} dimension={}",
                player.getName().getString(),
                player.getUuidAsString(),
                slot,
                describeStack(previous),
                describeStack(current),
                dimensionId(player.getEntityWorld()));
    }

    public static void logInventoryRemove(PlayerEntity player, int slot, ItemStack removed, ItemStack remaining) {
        if (!shouldLog(player) || removed == null || removed.isEmpty()) {
            return;
        }
        LOGGER.info("[InventoryRemove] player={} uuid={} slot={} removed={} remaining={} dimension={}",
                player.getName().getString(),
                player.getUuidAsString(),
                slot,
                describeStack(removed),
                describeStack(remaining),
                dimensionId(player.getEntityWorld()));
    }

    private static boolean shouldLog(PlayerEntity player) {
        //return player != null && player.getEntityWorld() != null && !player.getEntityWorld().isClient();
        return true;
    }

    private static String formatPos(BlockPos pos) {
        return String.format(Locale.ROOT, "(%d,%d,%d)", pos.getX(), pos.getY(), pos.getZ());
    }

    private static String dimensionId(World world) {
        Identifier id = world.getRegistryKey().getValue();
        return id != null ? id.toString() : "unknown";
    }

    private static String idForBlock(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return Registries.ITEM.getId(stack.getItem()) + "x" + stack.getCount();
    }
}

