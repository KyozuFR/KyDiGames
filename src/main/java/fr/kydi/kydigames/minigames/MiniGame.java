package fr.kydi.kydigames.minigames;

public interface MiniGame {
    String getName();
    int getGameDuration();

    void start();
    void stop();
}
