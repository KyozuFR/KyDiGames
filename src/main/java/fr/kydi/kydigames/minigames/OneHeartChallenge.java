package fr.kydi.kydigames.minigames;

public class TestMniGame implements MiniGame {
    @Override
    public String getName() {
        return "One Heart Challenge";
    }

    @Override
    public int getDuration() {
        return 60;
    }

    @Override
    public void start() {
        for (ServerPlayerEntity player : playerManager.getPlayerList()) {
            player.getAttributeInstance(
                    net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH
            ).setBaseValue(2.0);
        }
    }

    @Override
    public void stop() {

    }
}