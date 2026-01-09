package fr.kydi.kydigames;

import net.fabricmc.api.ModInitializer;
import fr.kydi.kydigames.events.PlayerActionEvents;

public class Kydigames implements ModInitializer {

    @Override
    public void onInitialize() {
        PlayerActionEvents.register();
    }
}
