package ICS4U_Summative;
import becker.robots.*;

import java.util.Arrays;
import java.util.Random;

/**
 * Application class for the summative project
 * @author Aadil Kureshy, Austin Xiong,  Xinran Li
 * @version 2025 12 30
 */
public class App {
    private static boolean gameEnded = false;
    private static final int NUM_VIPS = 2;
    private static final int NUM_GUARDS = 2;
    private static final int NUM_CHASERS = 2;

    /**
     * Set up the playground for the robots
     * @author Xinran Li
     */
    private static void setupPlayground(City playground)
    {
        playground.setSize(1500, 900);
        for(int i = 1; i <= 13; i++)
        {
            new Wall(playground, i, 0, Direction.EAST);
            new Wall(playground, i, 25, Direction.WEST);
        }
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
            records[i].updateRecords
                    (
                    array[i].myRecords.getHP(),
                    array[i].getMyPosition(),
                    array[i].myRecords.getState()
                    );
        }
    }


    private static int manhattan(int[] a, int[] b)
    {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    private static void handleInteractions(BaseBot[] robots, PlayerInfo[] infos, Random rand)
    {
        for (int i = 0; i < robots.length; i++)
        {
            if (infos[i].getState()) continue;
            if (infos[i].getRole() != 3) continue;
            KureshyBot chaser = (KureshyBot) robots[i];
            int bestTarget = -1;
            int bestDist = 10000000;

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
                if (d <= 1 && d < bestDist)
                {
                    bestDist = d;
                    bestTarget = chaser.getTargetID();
                }
            }
            if (bestTarget != -1)
            {
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
                "Chaser: %s -- Target %d: %s -- Roll: %.2f\n",
                Arrays.toString(chaser.getMyPosition()),
                target.myRecords.getID(),
                Arrays.toString(target.getMyPosition()),
                diff
        );

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

        //iterate through the records
        for (int i=0; i<records.length; i++) {

            //check how many VIPs there are
            if (records[i].getRole() == 1) {
                numVIPs++;

                //check if they are caught
                if (records[i].getState())
                    numVIPsCaught++;
            }

            //check how many Chasers there are
            if (records[i].getRole() == 3) {
                numChasers++;

                //check if they are caught
                if (records[i].getState())
                    numChasersCaught++;
            }

        }

        //check if all VIPs have been caught
        if (numVIPsCaught==numVIPs) {
            gameEnded = true;
        }

        if (numChasersCaught==numChasers) {
            gameEnded = true;
        }

        if (turn >= maxTurns) {
            gameEnded = true;
        }
    }

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
                    1, // role
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
                    2, // role
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
                    3, // role
                    3, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i, 3, 3, dodgeDiff, robots[i].getMyPosition(), false);
        }

        int maxTurns = 50;
        for (int i = 4; i < 6; i++)
        {
            robots[i].initRecords(infos);
        }

        for (int turn = 1; turn <= maxTurns && !gameEnded; turn++)
        {

            App.updateRecords(robots, infos);

            for (BaseBot b : robots) {
                b.updateOtherRecords(infos);
            }

            // ========= 1) VIP phase =========
            for (int i = 0; i < 2; i++) {
                if (!robots[i].myRecords.getState())
                {
                    robots[i].takeTurn();
                    App.updateRecords(robots, infos);
                    handleInteractions(robots, infos, rand);
                }
            }
            App.checkForWinCondition(infos, maxTurns, turn);

            // ========= 2) Chaser phase =========
            for (int i = 4; i < 6; i++) {
                if (!robots[i].myRecords.getState())
                {
                    robots[i].takeTurn();
                    App.updateRecords(robots, infos);
                    handleInteractions(robots, infos, rand);
                }
            }
            App.checkForWinCondition(infos, maxTurns, turn);

            // ========= 3) Guard phase =========
            for (int i = 2; i < 4; i++) {
                if (!robots[i].myRecords.getState())
                {
                    robots[i].takeTurn();
                    App.updateRecords(robots, infos);
                    handleInteractions(robots, infos, rand);
                }
            }
            App.checkForWinCondition(infos, maxTurns, turn);
        }


    }

}
