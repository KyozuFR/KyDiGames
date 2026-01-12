package fr.kydi.kydigames.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public interface Command {
    void register(CommandDispatcher<ServerCommandSource> dispatcher);
}
