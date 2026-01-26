package fr.kydi.kydigames;

import fr.kydi.kydigames.core.MiniGamesManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class Kydigames implements ModInitializer {

    @Override
    public void onInitialize() {
        MiniGamesManager core = MiniGamesManager.getInstance();

        ServerLifecycleEvents.SERVER_STARTED.register(core::setupManager);
    }
}