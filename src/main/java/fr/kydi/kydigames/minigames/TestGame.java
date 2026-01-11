package fr.kydi.kydigames.minigames;

import fr.kydi.kydigames.MiniGamesCore;

public class TestGame implements MiniGame {
    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public int getGameDuration() {
        return 10;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}