package fr.kydi.kydigames;

import com.mojang.brigadier.arguments.StringArgumentType;
import fr.kydi.kydigames.minigames.GameState;
import fr.kydi.kydigames.minigames.MiniGame;
import fr.kydi.kydigames.minigames.TestGame;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    private final List<MiniGame> registeredGames = new ArrayList<>();
    private MiniGame currentGame;
    private GameState state = GameState.WAITING;

    private final List<ServerPlayerEntity> votedPlayers = new ArrayList<>();
    private int yesVotes = 0;
    private int noVotes = 0;

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
    public void setServer(MinecraftServer server) {
        this.server = server;
        scheduleNextVote(VOTE_TIMEOUT_DURATION);
    }

    /** Schedule the next vote after a delay */
    private void scheduleNextVote(int delaySeconds) {
        state = GameState.WAITING;
        runDelayed(this::startVote, delaySeconds);
    }

    /** Start the vote for the next mini-game */
    private void startVote() {
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            scheduleNextVote(VOTE_TIMEOUT_DURATION);
            return;
        }

        state = GameState.VOTING;
        votedPlayers.clear();
        yesVotes = 0;
        noVotes = 0;

        currentGame = registeredGames.get(new Random().nextInt(registeredGames.size()));

        broadcastChatMessage("§6Vote for the next mini-game §b" + currentGame.getName());
        broadcastChatMessage("§eType /vote yes or /vote no");

        runDelayed(this::endVote, VOTE_DURATION);
    }

    /** Handle player votes */
    public void vote(ServerPlayerEntity player, boolean yes) {
        if (state != GameState.VOTING) return;
        if (votedPlayers.contains(player)) return;

        votedPlayers.add(player);

        if (yes) yesVotes++;
        else noVotes++;

        player.sendMessage(Text.literal("§7Vote registered"), false);

        if (votedPlayers.size() >= server.getPlayerManager().getPlayerList().size()) {
            endVote();
        }
    }

    /** End the vote and start or cancel the mini-game */
    private void endVote() {
        if (state != GameState.VOTING) return;

        if (yesVotes > noVotes) {
            startGame();
        } else {
            broadcastChatMessage("§cVote refused");
            currentGame = null;
            scheduleNextVote(VOTE_TIMEOUT_DURATION);
        }
    }

    /** Start the selected mini-game */
    private void startGame() {
        if (currentGame == null) {
            scheduleNextVote(VOTE_TIMEOUT_DURATION);
            return;
        }

        state = GameState.RUNNING;
        currentGame.start();
        broadcastChatMessage("§6The minigame §b" + currentGame.getName() + " §6started!");

        runDelayed(() -> {
            broadcastChatMessage("§6The minigame §b" + currentGame.getName() + " §6finished!");
            currentGame.stop();
            currentGame = null;
            scheduleNextVote(VOTE_TIMEOUT_DURATION);
        }, currentGame.getGameDuration());
    }



    // Utility methods should move later

    /** Send chat messages to all players */
    public void broadcastChatMessage(String message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.literal(message), false);
        }
    }

    /** Run a delayed task */
    private void runDelayed(Runnable task, int seconds) {
        new Thread(() -> {
            try {
                Thread.sleep(seconds * 1000L);
                task.run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
