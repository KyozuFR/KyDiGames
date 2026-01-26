package fr.kydi.kydigames.core;

import fr.kydi.kydigames.commands.VoteCommand;
import fr.kydi.kydigames.minigames.MiniGame;
import fr.kydi.kydigames.minigames.OneHeartChallenge;

import java.util.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.sound.Sound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * Core class for managing mini-games
 * Singleton pattern
 */
public class MiniGamesManager {
    // Constant (should be moved away later)
    private static final int VOTE_DURATION = 15;
    private static final int VOTE_TIMEOUT_DURATION = 5;


    private static volatile MiniGamesManager instance;
    private MinecraftServer server;
    private PlayerManager playerManager;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> voteTask;
    private ScheduledFuture<?> gameTask;

    private final List<MiniGame> registeredGames = new ArrayList<>();
    private MiniGame currentGame;
    private GameState currentState;

    private final Map<ServerPlayerEntity, Boolean> votes = new HashMap<>();

    /** Private constructor for singleton */
    private MiniGamesManager() {}

    /** Get the singleton instance */
    public static MiniGamesManager getInstance() {
        if (MiniGamesManager.instance == null) {
            synchronized(MiniGamesManager.class) {
                if (MiniGamesManager.instance == null) {
                    MiniGamesManager.instance = new MiniGamesManager();

                    // Register mini-games (may become a loop to load from config)
                    instance.registeredGames.add(new OneHeartChallenge());

                    // Register vote command
                    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                        new VoteCommand().register(dispatcher);
                    });
                }
            }
        }
        return MiniGamesManager.instance;
    }

    /** Connect the manager to a server instance */
    public void setupManager(MinecraftServer server) {
        this.server = server;
        this.playerManager = server.getPlayerManager();
        scheduleNextVote(VOTE_TIMEOUT_DURATION);

        ServerPlayConnectionEvents.DISCONNECT.register((player, serverInstance) -> {
            votes.remove(player.getPlayer());
            checkVote();
        });
    }

    private void scheduleNextVote(int delaySeconds) {
        if (voteTask != null) {
            voteTask.cancel(false);
        }

        voteTask = runDelayed(this::startVote, delaySeconds);
    }

    private void startVote() {
        if (playerManager.getPlayerList().isEmpty()) {
            scheduleNextVote(VOTE_TIMEOUT_DURATION);
            return;
        }

        currentGame = registeredGames.get(new Random().nextInt(registeredGames.size()));
        currentState = GameState.VOTING;

        playerManager.broadcast(Text.of("§6Vote for the next mini-game §b" + currentGame.getName()), false);
        playerManager.broadcast(Text.of("§eType /vote"), false);

        if (voteTask != null) {
            voteTask.cancel(false);
        }

        voteTask = runDelayed(this::endVote, VOTE_DURATION);
    }

    /** Handle player votes */
    public void vote(ServerPlayerEntity player, boolean choice) {
        if (currentState != GameState.VOTING) {
            player.sendMessage(Text.literal("§cNo vote in progress"), false);
            return;
        }

        votes.put(player, choice);

        player.sendMessage(Text.literal("§7Vote registered"), false);

        checkVote();
    }

    private void checkVote() {
        if (votes.size() >= playerManager.getCurrentPlayerCount()) {
            endVote();
        }
    }

    private void endVote() {
        if (currentState != GameState.VOTING) return;
        if (voteTask != null) {
            voteTask.cancel(false);
            voteTask = null;
        }

        long yesVotes = votes.values().stream().filter(v -> v).count();
        int requiredYesVotes = (int) Math.ceil(playerManager.getCurrentPlayerCount() * 0.6);

        if (yesVotes >= requiredYesVotes) {
            playerManager.broadcast(Text.of("§aMini-Game accepted (" + yesVotes + "/" + playerManager.getCurrentPlayerCount() + ")"), false);
            startGame();
        } else {
            playerManager.broadcast(Text.of("§cMini-Game rejected (" + yesVotes + "/" + playerManager.getCurrentPlayerCount() + ")"), false);
            currentGame = null;
            scheduleNextVote(VOTE_TIMEOUT_DURATION);
        }

        votes.clear();
    }

    private void startGame() {
        if (currentGame == null) {
            scheduleNextVote(VOTE_TIMEOUT_DURATION);
            return;
        }

        currentGame.start(server, playerManager);
        currentState = GameState.PLAYING;

        playerManager.broadcast(Text.of("§6The Mini-Game §b" + currentGame.getName() + " §6started!"), false);

        if (gameTask != null) {
            gameTask.cancel(false);
        }

        gameTask = runDelayed(this::stopGame, currentGame.getDuration());
    }

    private void stopGame() {
        if (gameTask != null) {
            gameTask.cancel(false);
            gameTask = null;
        }

        playerManager.broadcast(
                Text.of("§6The Mini-Game §b" + currentGame.getName() + " §6finished!"),
                false
        );

        currentGame.stop();
        currentGame = null;
        currentState = GameState.IDLE;

        scheduleNextVote(VOTE_TIMEOUT_DURATION);
    }



    // Utility methods should move later

    /** Run a delayed task */
    private ScheduledFuture<?> runDelayed(Runnable task, int seconds) {
        return scheduler.schedule(() -> server.execute(task), seconds, TimeUnit.SECONDS);
    }
}
