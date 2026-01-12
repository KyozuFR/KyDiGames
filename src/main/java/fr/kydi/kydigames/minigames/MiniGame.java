package fr.kydi.kydigames.minigames;

public interface MiniGame {
    String getName();
    int getDuration();

    void start();
    void stop();
}
