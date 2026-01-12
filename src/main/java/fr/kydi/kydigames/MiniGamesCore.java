package fr.kydi.kydigames;

import fr.kydi.kydigames.minigames.GameState;
import fr.kydi.kydigames.minigames.MiniGame;
import fr.kydi.kydigames.minigames.TestGame;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

/**
 * Core class for managing mini-games
 * Singleton pattern
 *
 * @Author Amaury Mulcey
 */
public class MiniGamesCore {
    // Constant (should be moved away later)
    private static final int VOTE_DURATION = 15;
    private static final int VOTE_TIMEOUT_DURATION = 5;


    private static MiniGamesCore instance;
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
    private MiniGamesCore() {}

    /** Get the singleton instance */
    public static MiniGamesCore getInstance() {
        if (instance == null) {
            instance = new MiniGamesCore();

            // Register mini-games (may become a loop to load from config)
            instance.registeredGames.add(new TestGame());

            // Register vote command
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                dispatcher.register(CommandManager.literal("vote")
                        .then(CommandManager.argument("choice", StringArgumentType.word())
                                .executes(context -> {
                                    String choice = StringArgumentType.getString(context, "choice");
                                    ServerCommandSource source = context.getSource();
                                    ServerPlayerEntity player = source.getPlayer();

                                    if (choice.equalsIgnoreCase("yes") || choice.equalsIgnoreCase("y")) {
                                        instance.vote(player, true);
                                    } else if (choice.equalsIgnoreCase("no") || choice.equalsIgnoreCase("n")) {
                                        instance.vote(player, false);
                                    } else if (player != null) {
                                        player.sendMessage(Text.literal("§eNeed Help? -> type /vote yes or /vote no"), false);
                                    }
                                    return 1;
                                })
                        )
                );
            });
        }
        return instance;
    }

    /** Connect the manager to a server instance */
    public void setupManager(MinecraftServer server) {
        this.server = server;
        this.playerManager = server.getPlayerManager();
        scheduleNextVote(VOTE_TIMEOUT_DURATION);

        ServerPlayConnectionEvents.DISCONNECT.register((player, serverInstance) -> {
            votes.remove(player.getPlayer());
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
        playerManager.broadcast(Text.of("§eType /vote yes or /vote no"), false);

        if (voteTask != null) {
            voteTask.cancel(false);
        }

        voteTask = runDelayed(this::endVote, VOTE_DURATION);
    }

    /** Handle player votes */
    public void vote(ServerPlayerEntity player, boolean yes) {
        if (currentState != GameState.VOTING) {
            player.sendMessage(Text.literal("§cNo vote in progress"), false);
            return;
        }
        if (votes.containsKey(player)) return;

        if (yes) {
            votes.put(player, true);
        } else {
            votes.put(player, false);
        }

        player.sendMessage(Text.literal("§7Vote registered"), false);

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

        currentGame.start();
        currentState = GameState.PLAYING;

        playerManager.broadcast(Text.of("§6The Mini-Game §b" + currentGame.getName() + " §6started!"), false);

        if (gameTask != null) {
            gameTask.cancel(false);
        }

        gameTask = runDelayed(this::stopGame, currentGame.getGameDuration());
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
