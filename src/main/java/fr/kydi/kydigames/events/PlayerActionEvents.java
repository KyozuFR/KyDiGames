package fr.kydi.kydigames.events;

import fr.kydi.kydigames.logging.PlayerActionLogger;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PlayerActionEvents {

    private PlayerActionEvents() {
    }

    public static void register() {
        registerBlockBreak();
        registerBlockPlace();
    }

    private static void registerBlockBreak() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) {
                return;
            }
            PlayerActionLogger.logBlockBreak(player, pos, state);
        });
    }

    private static void registerBlockPlace() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            ItemStack stack = serverPlayer.getStackInHand(hand);
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                return ActionResult.PASS;
            }

            ItemPlacementContext context = new ItemPlacementContext(serverPlayer, hand, stack, hitResult);
            BlockState placedState = blockItem.getBlock().getPlacementState(context);
            BlockPos pos = hitResult.getBlockPos().offset(hitResult.getSide());
            if (placedState != null) {
                PlayerActionLogger.logBlockPlace(serverPlayer, pos, placedState);
            }
            return ActionResult.PASS;
        });
    }
}

