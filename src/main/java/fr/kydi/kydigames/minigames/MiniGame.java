package fr.kydi.kydigames.minigames;

public interface MiniGame {
    String getName();
    int getDuration();

    void start(MinecraftServer server, PlayerManager playerManager);
    void stop();
}
