package ICS4U_Summative;
import becker.robots.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Random;

/**
 * Application class for the summative project
 * @author Aadil Kureshy, Austin Xiong,  Xinran Li
 * @version 2025 12 30
 */
public class App {
    private static final int ROLE_VIP = 1;
    private static final int ROLE_GUARD = 2;
    private static final int ROLE_CHASER = 3;
    private static boolean gameEnded = false;
    private static final int NUM_VIPS = 2;
    private static final int NUM_GUARDS = 2;
    private static final int NUM_CHASERS = 2;

    // Debug toggle for test output
    private static final boolean DEBUG = true;

    /**
     * Set up the playground for the robots
     * @author Xinran Li
     * @param playground the City object to set up
     */
    private static void setupPlayground(City playground)
    {
        playground.setSize(1500, 900);
        // Build vertical walls
        for(int i = 1; i <= 13; i++)
        {
            new Wall(playground, i, 0, Direction.EAST);
            new Wall(playground, i, 25, Direction.WEST);
        }
        // Build horizontal walls
        for(int i = 1; i <= 24; i++)
        {
            new Wall(playground, 0, i, Direction.SOUTH);
            new Wall(playground, 14, i, Direction.NORTH);
        }
    }

    /**
     * Updates the records used by the application
     * @param array the array of BaseBots
     * @param records the array of records to update
     */
    public static void updateRecords(BaseBot[] array, PlayerInfo[] records)
    {
        for (int i = 0; i < records.length; i++)
        {
            // Capture previous values for lightweight change logging
            int oldHP = records[i].getHP();
            boolean oldState = records[i].getState();
            int[] oldPos = records[i].getPosition();

            records[i].updateRecords
                    (
                            array[i].myRecords.getHP(),
                            array[i].getMyPosition(),
                            array[i].myRecords.getState()
                    );

            // Sets caught players as black for visualization
            if (array[i].myRecords.getState()) {
                array[i].setColor(Color.BLACK);
            }

            // Print only meaningful state changes to avoid spam
            if (DEBUG) {
                int newHP = records[i].getHP();
                boolean newState = records[i].getState();

                if (oldHP != newHP || oldState != newState) {
                    System.out.format(
                            "RECORD UPDATE: id=%d role=%s pos %s -> %s | HP %d -> %d | caught %b -> %b%n",
                            array[i].myRecords.getID(),
                            roleName(records[i].getRole()),
                            Arrays.toString(oldPos),
                            Arrays.toString(records[i].getPosition()),
                            oldHP, newHP,
                            oldState, newState
                    );
                }
            }
        }
    }

    /**
     * Gets the Manhattan distance between two points
     * @param a the first point
     * @param b the second point
     * @return the distance between them
     */
    private static int manhattan(int[] a, int[] b)
    {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    /**
     * Handles all chaser-target interactions for the current turn.
     * @param robots array of all robots
     * @param infos array of all player info
     * @param rand random number generator
     */
    private static void handleInteractions(BaseBot[] robots, PlayerInfo[] infos, Random rand)
    {
        for (int i = 0; i < robots.length; i++)
        {
            // Skip if this robot is caught or not a chaser
            if (infos[i].getState()) continue;
            if (infos[i].getRole() != 3) continue;
            KureshyBot chaser = (KureshyBot) robots[i];
            int bestTarget = -1;
            int bestDist = 10000000;

            // Find the closest uncaught VIP or Guard
            for (int j = 0; j < robots.length; j++)
            {
                if (i == j)
                {
                    continue;
                }
                if (infos[j].getState())
                {
                    continue;
                }
                if (infos[j].getRole() == 3)
                {
                    continue;
                }

                int d = manhattan(infos[i].getPosition(), infos[j].getPosition());
                if (d < 1 && d < bestDist)
                {
                    bestDist = d;
                    bestTarget = j;
                }
            }
            // If a valid target is found, resolve the dodge/tag interaction
            if (bestTarget != -1)
            {
                if (DEBUG) {
                    System.out.format(
                            "INTERACTION DETECTED: Chaser id=%d pos=%s overlaps Target id=%d role=%s pos=%s%n",
                            chaser.myRecords.getID(),
                            Arrays.toString(chaser.getMyPosition()),
                            robots[bestTarget].myRecords.getID(),
                            roleName(robots[bestTarget].myRecords.getRole()),
                            Arrays.toString(robots[bestTarget].getMyPosition())
                    );
                }

                checkDodge(chaser, robots[bestTarget], rand);
                App.updateRecords(robots, infos);
            }
        }
    }

    /**
     * Performs a single "dodge vs. tag" interaction between a Chaser and a target robot.
     * A random roll in the range [0.0, 1.0) is generated. Each participant "dodges" if their
     * dodge difficulty is greater than or equal to the roll. Based on the outcomes, this
     * method applies damage and notifies the chaser of the tag result.
     * @param chaser initiating the tag attempt
     * @param target being targeted (VIP or Guard)
     * @param r  instance used to generate the roll
     */
    public static void checkDodge(KureshyBot chaser, BaseBot target, Random r) {
        // Generate a random roll in [0.0, 1.0)
        double diff = r.nextDouble();

        // Read dodge capabilities (expected range: 0.0 to 1.0)
        double c = chaser.myRecords.getDodgeDifficulty();
        double t = target.myRecords.getDodgeDifficulty();

        // Debug output: positions and roll value
        System.out.format(
                "Chaser %d: %s -- Target %d: %s -- Roll: %.2f%n",
                chaser.myRecords.getID(),
                Arrays.toString(chaser.getMyPosition()),
                target.myRecords.getID(),
                Arrays.toString(target.getMyPosition()),
                diff
        );

        // Extra debug output: dodge caps + HP before resolution
        if (DEBUG) {
            System.out.format(
                    "DODGE CAPS: chaser=%.2f target=%.2f | HP BEFORE: chaser=%d target=%d%n",
                    c, t,
                    chaser.myRecords.getHP(),
                    target.myRecords.getHP()
            );
        }

        // Determine dodge results for both participants
        boolean chaserDodged = (c >= diff);
        boolean targetDodged = (t >= diff);

        // Apply outcome rules and notify the chaser of the tag attempt result
        if (chaserDodged && targetDodged) {
            // Both dodged: no one takes damage; tag attempt fails
            chaser.sendTagResult(target.myRecords.getID(), false);
            System.out.println("BOTH DODGED");
        } else if (chaserDodged && !targetDodged) {
            // Chaser dodged but target failed: target takes damage; tag attempt succeeds
            chaser.sendTagResult(target.myRecords.getID(), true);
            target.takeDamage(1);
            System.out.println("CHASER DODGED, TARGET HIT");
        } else if (!chaserDodged && targetDodged) {
            // Target dodged but chaser failed: chaser takes damage; tag attempt fails
            chaser.sendTagResult(target.myRecords.getID(), false);
            chaser.takeDamage(1);
            System.out.println("TARGET DODGED, CHASER HIT");
        } else {
            // Neither dodged: both take damage; tag attempt succeeds
            chaser.sendTagResult(target.myRecords.getID(), true);
            chaser.takeDamage(1);
            target.takeDamage(1);
            System.out.println("NONE DODGED, BOTH HIT");
        }

        // Extra debug output: HP after resolution (immediate state on the bot objects)
        if (DEBUG) {
            System.out.format(
                    "HP AFTER: chaser=%d (caught=%b) target=%d (caught=%b)%n",
                    chaser.myRecords.getHP(),
                    chaser.myRecords.getState(),
                    target.myRecords.getHP(),
                    target.myRecords.getState()
            );
        }
    }

    /**
     * Checks if either the VIPs/Guards or Chasers has reached their win conditions
     * @param records the application records
     * @param maxTurns the max number of turns
     * @param turn the current turn
     */
    public static void checkForWinCondition(PlayerInfo[] records, int maxTurns, int turn) {
        int numVIPs = 0;
        int numChasers = 0;
        int numVIPsCaught = 0;
        int numChasersCaught = 0;

        // Iterate through the records to count roles and caught players
        for (int i=0; i<records.length; i++) {

            // Count VIPs and check if caught
            if (records[i].getRole() == 1) {
                numVIPs++;

                if (records[i].getState())
                    numVIPsCaught++;
            }

            // Count Chasers and check if caught
            if (records[i].getRole() == 3) {
                numChasers++;

                if (records[i].getState())
                    numChasersCaught++;
            }
        }
        // End game if all VIPs or all Chasers are caught, or max turns reached
        boolean endedNow = false;
        String reason = null;

        if (numVIPsCaught==numVIPs) {
            endedNow = true;
            reason = "All VIPs have been caught";
        }
        if (numChasersCaught==numChasers) {
            endedNow = true;
            reason = "All Chasers have been caught";
        }
        if (turn >= maxTurns) {
            endedNow = true;
            reason = "Max turns reached";
        }

        if (endedNow && !gameEnded && DEBUG) {
            System.out.format(
                    "GAME ENDED at turn %d: %s (VIPs caught %d/%d, Chasers caught %d/%d)%n",
                    turn, reason, numVIPsCaught, numVIPs, numChasersCaught, numChasers
            );
        }

        if (endedNow) {
            gameEnded = true;
        }
    }

    /**
     * Main entry point for the application.
     * Initializes the playground, robots, and runs the game loop.
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args)
    {
        City playground = new City();
        setupPlayground(playground);

        // Initialize arrays for all robots
        BaseBot[] robots = new BaseBot[6];
        PlayerInfo[] infos = new PlayerInfo[6]; // Add this array to store playerInfo

        Random rand = new Random();

        // VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<2; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = rand.nextInt(13) + 1;
            int col = rand.nextInt(24) + 1;
            int[] pos = {row, col};
            robots[i] = new XiongBot(
                    playground,
                    row,
                    col,
                    Direction.SOUTH, // str, ave, dir
                    i, // id
                    ROLE_VIP, // role
                    2, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i, 1, 2, dodgeDiff, robots[i].getMyPosition(), false);
        }

        // Guards: movesPerTurn [2,4], dodgeDiff [0.45, 0.55]
        for (int i=2; i<4; i++) {
            int movesPerTurn = rand.nextInt(3) + 2;
            double dodgeDiff = 0.45 + rand.nextDouble() * 0.1;
            int row = rand.nextInt(13) + 1;
            int col = rand.nextInt(24) + 1;
            int[] pos = {row, col};
            robots[i] = new LiBot(
                    playground,
                    row,
                    col,
                    Direction.NORTH, // str, ave, dir
                    i, // id
                    ROLE_GUARD, // role
                    5, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i, 2, 5, dodgeDiff, robots[i].getMyPosition(), false);
        }

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        for (int i=4; i<6; i++) {
            int movesPerTurn = rand.nextInt(3) + 3;
            double dodgeDiff = 0.7 + rand.nextDouble() * 0.2;
            int row = rand.nextInt(13) + 1;
            int col = rand.nextInt(24) + 1;
            int[] pos = {row, col};
            robots[i] = new KureshyBot(
                    playground,
                    row,
                    col,
                    Direction.NORTH, // str, ave, dir
                    i, // id
                    ROLE_CHASER, // role
                    3, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i, 3, 3, dodgeDiff, robots[i].getMyPosition(), false);
        }

        int maxTurns = 50;
        // Initialize chaser bots with player info
        for (int i = 4; i < 6; i++)
        {
            robots[i].initRecords(infos);
        }

        // Initial roster printout for quick verification
        if (DEBUG) {
            System.out.println("===== INITIAL ROBOT ROSTER =====");
            for (int i = 0; i < robots.length; i++) {
                System.out.format(
                        "INIT: id=%d role=%s pos=%s hp=%d dodge=%.2f caught=%b%n",
                        robots[i].myRecords.getID(),
                        roleName(robots[i].myRecords.getRole()),
                        Arrays.toString(robots[i].getMyPosition()),
                        robots[i].myRecords.getHP(),
                        robots[i].myRecords.getDodgeDifficulty(),
                        robots[i].myRecords.getState()
                );
            }
            System.out.println("================================");
        }

        // Main game loop
        for (int turn = 1; turn <= maxTurns && !gameEnded; turn++)
        {
            if (DEBUG) {
                System.out.println();
                System.out.println("========== TURN " + turn + " ==========");
            }

            App.updateRecords(robots, infos);

            // Broadcast the latest records to every bot at the start of the turn
            for (BaseBot b : robots)
            {
                b.updateOtherRecords(infos);
            }

            // Interleaved turn order to reduce same-role symmetry:
            // VIP0 -> Guard0 -> Chaser0 -> VIP1 -> Guard1 -> Chaser1
            for (int k = 0; k < 2; k++)
            {
                int vipIndex = k; // VIPs are robots[0], robots[1]
                int guardIndex = 2 + k; // Guards are robots[2], robots[3]
                int chaserIndex = 4 + k; // Chasers are robots[4], robots[5]

                // ----- VIP move -----
                if (!robots[vipIndex].myRecords.getState())
                {
                    if (DEBUG) {
                        System.out.format(
                                "[Turn %d] VIP acting: id=%d pos=%s hp=%d%n",
                                turn,
                                robots[vipIndex].myRecords.getID(),
                                Arrays.toString(robots[vipIndex].getMyPosition()),
                                robots[vipIndex].myRecords.getHP()
                        );
                    }

                    robots[vipIndex].updateOtherRecords(infos);
                    robots[vipIndex].takeTurn();
                    App.updateRecords(robots, infos);
                    handleInteractions(robots, infos, rand);

                    if (DEBUG) {
                        System.out.format(
                                "[Turn %d] VIP done:   id=%d pos=%s hp=%d caught=%b%n",
                                turn,
                                robots[vipIndex].myRecords.getID(),
                                Arrays.toString(robots[vipIndex].getMyPosition()),
                                robots[vipIndex].myRecords.getHP(),
                                robots[vipIndex].myRecords.getState()
                        );
                    }
                }
                App.checkForWinCondition(infos, maxTurns, turn);
                if (gameEnded) break;

                // ----- Guard move -----
                if (!robots[guardIndex].myRecords.getState())
                {
                    if (DEBUG) {
                        System.out.format(
                                "[Turn %d] Guard acting: id=%d pos=%s hp=%d%n",
                                turn,
                                robots[guardIndex].myRecords.getID(),
                                Arrays.toString(robots[guardIndex].getMyPosition()),
                                robots[guardIndex].myRecords.getHP()
                        );
                    }

                    robots[guardIndex].updateOtherRecords(infos);
                    robots[guardIndex].takeTurn();
                    App.updateRecords(robots, infos);
                    handleInteractions(robots, infos, rand);

                    if (DEBUG) {
                        System.out.format(
                                "[Turn %d] Guard done:   id=%d pos=%s hp=%d caught=%b%n",
                                turn,
                                robots[guardIndex].myRecords.getID(),
                                Arrays.toString(robots[guardIndex].getMyPosition()),
                                robots[guardIndex].myRecords.getHP(),
                                robots[guardIndex].myRecords.getState()
                        );
                    }
                }
                App.checkForWinCondition(infos, maxTurns, turn);
                if (gameEnded) break;

                // ----- Chaser move -----
                if (!robots[chaserIndex].myRecords.getState())
                {
                    if (DEBUG) {
                        System.out.format(
                                "[Turn %d] Chaser acting: id=%d pos=%s hp=%d%n",
                                turn,
                                robots[chaserIndex].myRecords.getID(),
                                Arrays.toString(robots[chaserIndex].getMyPosition()),
                                robots[chaserIndex].myRecords.getHP()
                        );
                    }

                    robots[chaserIndex].updateOtherRecords(infos);
                    robots[chaserIndex].takeTurn();
                    App.updateRecords(robots, infos);
                    handleInteractions(robots, infos, rand);

                    if (DEBUG) {
                        System.out.format(
                                "[Turn %d] Chaser done:   id=%d pos=%s hp=%d caught=%b%n",
                                turn,
                                robots[chaserIndex].myRecords.getID(),
                                Arrays.toString(robots[chaserIndex].getMyPosition()),
                                robots[chaserIndex].myRecords.getHP(),
                                robots[chaserIndex].myRecords.getState()
                        );
                    }
                }
                App.checkForWinCondition(infos, maxTurns, turn);
                if (gameEnded) break;
            }

            // End-of-turn snapshot (compact status line for all robots)
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("TURN ").append(turn).append(" SUMMARY: ");
                for (int i = 0; i < robots.length; i++) {
                    sb.append(String.format(
                        "[%s id=%d hp=%d pos=%s caught=%b] ",
                        roleName(robots[i].myRecords.getRole()),
                        robots[i].myRecords.getID(),
                        robots[i].myRecords.getHP(),
                        Arrays.toString(robots[i].getMyPosition()),
                        robots[i].myRecords.getState()
                    ));
                }
                System.out.println(sb);
            }
        }
    }

    private static String roleName(int role) {
        if (role == ROLE_VIP) return "VIP";
        if (role == ROLE_GUARD) return "GUARD";
        if (role == ROLE_CHASER) return "CHASER";
        return "UNKNOWN";
    }
}
