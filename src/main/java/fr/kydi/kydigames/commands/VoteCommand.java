package fr.kydi.kydigames.commands;

import com.mojang.brigadier.CommandDispatcher;
import fr.kydi.kydigames.core.MiniGamesManager;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class VoteCommand implements Command {

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("vote")
                        .then(CommandManager.literal("yes")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player == null) return 0;

                                    MiniGamesManager.getInstance().vote(player, true);
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("no")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player == null) return 0;

                                    MiniGamesManager.getInstance().vote(player, false);
                                    return 1;
                                })
                        )
        );
    }
}
