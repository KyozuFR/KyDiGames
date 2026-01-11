package fr.kydi.kydigames;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class Kydigames implements ModInitializer {

    @Override
    public void onInitialize() {
        MiniGamesCore core = MiniGamesCore.getInstance();

        ServerLifecycleEvents.SERVER_STARTED.register(core::setServer);
    }
}