package fr.kydi.kydigames.minigames;

public class TestMniGame implements MiniGame {
    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public int getDuration() {
        return 10;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}