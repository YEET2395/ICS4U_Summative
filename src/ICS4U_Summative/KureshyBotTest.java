package ICS4U_Summative;

import becker.robots.*;
import becker.util.Test;

import java.awt.*;
import java.util.*;

/**
 * Testing class for KureshyBot to test targeting and movement
 * @author Aadil Kureshy
 * @version January 16, 2025
 */
public class KureshyBotTest {
    public static boolean gameEnded = false;
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
    public static void updateRecords(BaseBot[] array, PlayerInfo[] records) {

        //iterate through the list of records (each record should match with the BaseBot in array)
        for (int i=0; i<records.length; i++) {

            //update the info that will change
            records[i].updateRecords(array[i].myRecords.getHP(), array[i].getMyPosition(), array[i].myRecords.getState());
            if (array[i].myRecords.getState()) {
                array[i].setColor(Color.BLACK);
            }
        }

    }

    /**
     * Randomly generates a number and compares it to the chaser and target's dodging capability
     * before applying damage and sending the results to the chaser
     * @param chaser the chaser initiating the catch
     * @param target the target of the chaser
     * @param r the Random object used for ehcking who dodged
     */
    public static void checkDodge(KureshyBot chaser, BaseBot target, Random r) {
        double diff = r.nextDouble();
        System.out.format("Chaser: %.2f -- Target %d: %.2f -- Difficulty: %.2f\n",
                chaser.myRecords.getDodgeDifficulty(),
                target.myRecords.getID(),
                target.myRecords.getDodgeDifficulty(),
                diff);

        //check which robots dodged and which didn't
        if (chaser.myRecords.getDodgeDifficulty() >= diff && target.myRecords.getDodgeDifficulty() >= diff) {
            chaser.sendTagResult(target.myRecords.getID(), false);
            System.out.println("BOTH DODGED");
            //means both dodged
        } else if (chaser.myRecords.getDodgeDifficulty() >= diff && target.myRecords.getDodgeDifficulty() < diff) {
            chaser.sendTagResult(target.myRecords.getID(), true);
            target.takeDamage(1);
            System.out.println("CHASER DODGED");
            //means chaser dodged but target didn't
        } else {
            chaser.sendTagResult(target.myRecords.getID(), true);
            chaser.takeDamage(1);
            target.takeDamage(1);
            System.out.println("NONE DODGED");
            //means both didn't dodge
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
     * Checks if a chaser is tagging
     * @param array the BaseBot array
     * @param rand the Random object used for checking who dodged
     */
    public static void handleInteractions(BaseBot[] array, PlayerInfo[] infos, Random rand) {
        for (int i = 0; i < array.length; i++)
        {
            // Skip if this robot is caught or not a chaser
            if (infos[i].getState()) continue;
            if (infos[i].getRole() != 3) continue;
            KureshyBot chaser = (KureshyBot) array[i];
            int bestTarget = -1;
            int bestDist = 10000000;

            // Find the closest uncaught VIP or Guard
            for (int j = 0; j < array.length; j++)
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
                checkDodge(chaser, array[bestTarget], rand);
                updateRecords(array, infos);
            }
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
        for (PlayerInfo record : records) {

            //check how many VIPs there are
            if (record.getRole() == 1) {
                numVIPs++;

                //check if they are caught
                if (record.getState())
                    numVIPsCaught++;
            }

            //check how many Chasers there are
            if (record.getRole() == 3) {
                numChasers++;

                //check if they are caught
                if (record.getState())
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

    public static void main(String[] args) {
        City playground = new City();
        setupPlayground(playground);
        playground.setSize(1500, 900);

        Random rand = new Random();

        BaseBot[] robots = new BaseBot[6];
        PlayerInfo[] infos = new PlayerInfo[6];

        /*Test Case #1.1
        //Chaser will automatically assign the robot closest to it with the highest priorityScore,
        //even if multiple are within range of the current turn (just using distance and the default
        //values for the other factors)
        // VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<4; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = 12;
            int col = 10;
            int[] pos = {col, row};
            robots[i] = new XiongBot(
                    playground,
                    row,
                    col,
                    Direction.SOUTH, // str, ave, dir
                    i+1, // id
                    1, // role
                    2, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i+1, 1, 2, dodgeDiff, pos, false);
        }

        //ACTUAL TEST VIP
        int movesPerTurn = rand.nextInt(3) + 1;
        double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
        int row = 13;
        int col = 12;
        int[] pos = {col, row};
        robots[4] = new XiongBot(
                playground,
                row,
                col,
                Direction.SOUTH, // str, ave, dir
                5, // id
                1, // role
                2, // hp
                movesPerTurn,
                dodgeDiff
        );
        infos[4] = new PlayerInfo(5, 1, 2, dodgeDiff, pos, false);

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        int chMovesPerTurn = 4;
        double chDodgeDiff = 0.7 + rand.nextDouble() * 0.2;
        int chRow = 12;
        int chCol = 12;
        int[] chPos = {chCol, chRow};
        robots[5] = new KureshyBot(
                    playground,
                    chRow,
                    chCol,
                    Direction.NORTH, // str, ave, dir
                    6, // id
                    3, // role
                    3, // hp
                    chMovesPerTurn,
                    chDodgeDiff
            );
        infos[5] = new PlayerInfo(6, 3, 3, chDodgeDiff, chPos, false);
        */

        /*Test Case #1.2 (Boundary)
        //Chaser will automatically assign the robot closest to it with the highest priorityScore,
        //even if multiple are within range of the current turn (just using distance and the default
        //values for the other factors). Targets are equal distance apart and at the edge of possible
        //movement so it should just go for the lowest ID.
        // VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<4; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = 10;
            int col = 10;
            int[] pos = {col, row};
            robots[i] = new XiongBot(
                    playground,
                    row,
                    col,
                    Direction.SOUTH, // str, ave, dir
                    i+1, // id
                    1, // role
                    2, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i+1, 1, 2, dodgeDiff, pos, false);
        }

        //ACTUAL TEST VIP
        int movesPerTurn = rand.nextInt(3) + 1;
        double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
        int row = 10;
        int col = 18;
        int[] pos = {col, row};
        robots[4] = new XiongBot(
                playground,
                row,
                col,
                Direction.SOUTH, // str, ave, dir
                5, // id
                1, // role
                2, // hp
                movesPerTurn,
                dodgeDiff
        );
        infos[4] = new PlayerInfo(5, 1, 2, dodgeDiff, pos, false);

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        int chMovesPerTurn = 4;
        double chDodgeDiff = 0.7 + rand.nextDouble() * 0.2;
        int chRow = 10;
        int chCol = 14;
        int[] chPos = {chCol, chRow};
        robots[5] = new KureshyBot(
                playground,
                chRow,
                chCol,
                Direction.NORTH, // str, ave, dir
                6, // id
                3, // role
                3, // hp
                chMovesPerTurn,
                chDodgeDiff
        );
        infos[5] = new PlayerInfo(6, 3, 3, chDodgeDiff, chPos, false);
        */

        /*Test Case #2.1
        //Chaser will go after the nearest target even if they are out of range (just using default
        //values for the other factors)
        // VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<4; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = 12; //put them in a corner
            int col = 10;
            int[] pos = {col, row};
            robots[i] = new XiongBot(
                    playground,
                    row,
                    col,
                    Direction.SOUTH, // str, ave, dir
                    i+1, // id
                    1, // role
                    2, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i+1, 1, 2, dodgeDiff, pos, false);
        }

        //ACTUAL TEST VIP
        int movesPerTurn = rand.nextInt(3) + 1;
        double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
        int row = 13;
        int col = 12;
        int[] pos = {col, row};
        robots[4] = new XiongBot(
                playground,
                row,
                col,
                Direction.SOUTH, // str, ave, dir
                5, // id
                1, // role
                2, // hp
                movesPerTurn,
                dodgeDiff
        );
        infos[4] = new PlayerInfo(5, 1, 2, dodgeDiff, pos, false);

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        int chMovesPerTurn = rand.nextInt(3) + 3;
        double chDodgeDiff = 0.7 + rand.nextDouble() * 0.2;
        int chRow = 13;
        int chCol = 18;
        int[] chPos = {chCol, chRow};
        robots[5] = new KureshyBot(
                    playground,
                    chRow,
                    chCol,
                    Direction.NORTH, // str, ave, dir
                    6, // id
                    3, // role
                    3, // hp
                    chMovesPerTurn,
                    chDodgeDiff
            );
        infos[5] = new PlayerInfo(6, 3, 3, chDodgeDiff, chPos, false);
        */

        /*Test Case #2.2 (Boundary)
        //Chaser will go after the nearest target even if they are out of range (just using default
        //values for the other factors). If the targets are equal distance, go after the lowest ID.
        // VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<4; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = 10; //put them in a corner
            int col = 6;
            int[] pos = {col, row};
            robots[i] = new XiongBot(
                    playground,
                    row,
                    col,
                    Direction.SOUTH, // str, ave, dir
                    i+1, // id
                    1, // role
                    2, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i+1, 1, 2, dodgeDiff, pos, false);
        }

        //ACTUAL TEST VIP
        int movesPerTurn = rand.nextInt(3) + 1;
        double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
        int row = 10;
        int col = 20;
        int[] pos = {col, row};
        robots[4] = new XiongBot(
                playground,
                row,
                col,
                Direction.SOUTH, // str, ave, dir
                5, // id
                1, // role
                2, // hp
                movesPerTurn,
                dodgeDiff
        );
        infos[4] = new PlayerInfo(5, 1, 2, dodgeDiff, pos, false);

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        int chMovesPerTurn = rand.nextInt(3) + 3;
        double chDodgeDiff = 0.7 + rand.nextDouble() * 0.2;
        int chRow = 10;
        int chCol = 13;
        int[] chPos = {chCol, chRow};
        robots[5] = new KureshyBot(
                playground,
                chRow,
                chCol,
                Direction.NORTH, // str, ave, dir
                6, // id
                3, // role
                3, // hp
                chMovesPerTurn,
                chDodgeDiff
        );
        infos[5] = new PlayerInfo(6, 3, 3, chDodgeDiff, chPos, false);
        */

        /*
        Test Case #3: Chaser will go after the robot with the lower dodge estimation (distance and other
        factors are the same/default)
        VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<4; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = 4; //put them in a corner
            int col = 5;
            int[] pos = {col, row};
            robots[i] = new XiongBot(
                    playground,
                    row,
                    col,
                    Direction.SOUTH, // str, ave, dir
                    i+1, // id
                    1, // role
                    2, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i+1, 1, 2, dodgeDiff, pos, false);
        }

        //ACTUAL TEST VIP
        int movesPerTurn = rand.nextInt(3) + 1;
        double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
        int row = 4;
        int col = 17;
        int[] pos = {col, row};
        robots[4] = new XiongBot(
                playground,
                row,
                col,
                Direction.SOUTH, // str, ave, dir
                5, // id
                1, // role
                2, // hp
                movesPerTurn,
                dodgeDiff
        );
        infos[4] = new PlayerInfo(5, 1, 2, dodgeDiff, pos, false);

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        int chMovesPerTurn = rand.nextInt(3) + 3;
        double chDodgeDiff = 0.7 + rand.nextDouble() * 0.2;
        int chRow = 4;
        int chCol = 11;
        int[] chPos = {chCol, chRow};
        robots[5] = new KureshyBot(
                    playground,
                    chRow,
                    chCol,
                    Direction.NORTH, // str, ave, dir
                    6, // id
                    3, // role
                    3, // hp
                    chMovesPerTurn,
                    chDodgeDiff
            );
        infos[5] = new PlayerInfo(6, 3, 3, chDodgeDiff, chPos, false);
        */

        /*Test Case #4
        Chaser should be slightly disincentivized from going after the same target if another chaser
        is already close to it (distance and other factors are the same/default)
        VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<3; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = 6;
            int col = 7;
            int[] pos = {col, row};
            robots[i] = new XiongBot(
                    playground,
                    row,
                    col,
                    Direction.SOUTH, // str, ave, dir
                    i+1, // id
                    1, // role
                    2, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i+1, 1, 2, dodgeDiff, pos, false);
        }

        //ACTUAL TEST VIP
        int movesPerTurn = rand.nextInt(3) + 1;
        double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
        int row = 6;
        int col = 15;
        int[] pos = {col, row};
        robots[3] = new XiongBot(
                playground,
                row,
                col,
                Direction.SOUTH, // str, ave, dir
                4, // id
                1, // role
                2, // hp
                movesPerTurn,
                dodgeDiff
        );
        infos[3] = new PlayerInfo(4, 1, 2, dodgeDiff, pos, false);

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        int chMovesPerTurn = rand.nextInt(3) + 3;
        double chDodgeDiff = 0.7 + rand.nextDouble() * 0.2;
        int chRow = 6;
        int chCol = 11;
        int[] chPos = {chCol, chRow};
        robots[4] = new KureshyBot(
                    playground,
                    chRow,
                    chCol,
                    Direction.NORTH, // str, ave, dir
                    5, // id
                    3, // role
                    3, // hp
                    chMovesPerTurn,
                    chDodgeDiff
            );
        infos[4] = new PlayerInfo(5, 3, 3, chDodgeDiff, chPos, false);

        //TEST CHASER
        int chMovesPerTurn2 = rand.nextInt(3) + 3;
        double chDodgeDiff2 = 0.7 + rand.nextDouble() * 0.2;
        int chRow2 = 6;
        int chCol2 = 19;
        int[] chPos2 = {chCol2, chRow2};
        robots[5] = new KureshyBot(
                playground,
                chRow2,
                chCol2,
                Direction.NORTH, // str, ave, dir
                6, // id
                3, // role
                3, // hp
                chMovesPerTurn2,
                chDodgeDiff2
        );
        infos[5] = new PlayerInfo(6, 3, 3, chDodgeDiff2, chPos2, false);
        */

        /* Test Case #5
        Chaser will automatically avoid confirmed guards (in this case confirmed using number of catches
        by manipulating their dodgeDifficulty) if low on health (set to 1), even if they have the best
        priorityScore
        // VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<4; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = 4;
            int col = 10;
            int[] pos = {col, row};
            robots[i] = new XiongBot(
                    playground,
                    row,
                    col,
                    Direction.SOUTH, // str, ave, dir
                    i+1, // id
                    1, // role
                    2, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i+1, 1, 2, dodgeDiff, pos, false);
        }

        //GUARD
        int movesPerTurn = rand.nextInt(3) + 2;
        double dodgeDiff = 0.01;
        int row = 4;
        int col = 18;
        int[] pos = {col, row};
        robots[4] = new LiBot(
                playground,
                row,
                col,
                Direction.SOUTH, // str, ave, dir
                5, // id
                1, // role
                5, // hp
                movesPerTurn,
                dodgeDiff
        );
        infos[4] = new PlayerInfo(5, 2, 5, dodgeDiff, pos, false);

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        int chMovesPerTurn = rand.nextInt(3) + 3;
        double chDodgeDiff = 0.99;
        int chRow = 4;
        int chCol = 16;
        int[] chPos = {chCol, chRow};
        robots[5] = new KureshyBot(
                    playground,
                    chRow,
                    chCol,
                    Direction.NORTH, // str, ave, dir
                    6, // id
                    3, // role
                    1, // hp
                    chMovesPerTurn,
                    chDodgeDiff
            );
        infos[5] = new PlayerInfo(6, 3, 3, chDodgeDiff, chPos, false);
        */

        /*Test Case #6
        Chaser can calculate the speed of targets and use that to deprioritize them if is  matches a guard's
        // VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<4; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = 4;
            int col = 8;
            int[] pos = {col, row};
            robots[i] = new XiongBot(
                    playground,
                    row,
                    col,
                    Direction.SOUTH, // str, ave, dir
                    i+1, // id
                    1, // role
                    2, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i+1, 1, 2, dodgeDiff, pos, false);
        }

        //ACTUAL TEST VIP
        int movesPerTurn = rand.nextInt(3)+1;
        double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
        int row = 4;
        int col = 16;
        int[] pos = {col, row};
        robots[4] = new XiongBot(
                playground,
                row,
                col,
                Direction.SOUTH, // str, ave, dir
                5, // id
                1, // role
                2, // hp
                movesPerTurn,
                dodgeDiff
        );
        infos[4] = new PlayerInfo(5, 1, 2, dodgeDiff, pos, false);

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        int chMovesPerTurn = rand.nextInt(3) + 3;
        double chDodgeDiff = 0.7 + rand.nextDouble() * 0.2;
        int chRow = 4;
        int chCol = 12;
        int[] chPos = {chCol, chRow};
        robots[5] = new KureshyBot(
                    playground,
                    chRow,
                    chCol,
                    Direction.NORTH, // str, ave, dir
                    6, // id
                    3, // role
                    3, // hp
                    chMovesPerTurn,
                    chDodgeDiff
            );
        infos[5] = new PlayerInfo(6, 3, 3, chDodgeDiff, chPos, false);
        */

        /*Test Case #7
        Chaser will cut-off targets under certain conditions, whether it be a vertical cutoff or a
        horizontal cutoff, pressuring it diagonally and only chasing if the VIP moves out of two
        turns of the chaser's movement or if they are directly in range
        // VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<4; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = 13;
            int col = 1;
            int[] pos = {col, row};
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
            infos[i] = new PlayerInfo(i, 1, 2, dodgeDiff, pos, false);
        }

        //ACTUAL TEST VIP
        int movesPerTurn = rand.nextInt(3)+1;
        double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
        int row = 3; //for vertical cutoff set: 4
        int col = 20; //for vertical cutoff set: 22
        int[] pos = {col, row};
        robots[4] = new XiongBot(
                playground,
                row,
                col,
                Direction.SOUTH, // str, ave, dir
                4, // id
                1, // role
                2, // hp
                movesPerTurn,
                dodgeDiff
        );
        infos[4] = new PlayerInfo(4, 1, 2, dodgeDiff, pos, false);

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        int chMovesPerTurn = 4;
        double chDodgeDiff = 0.7 + rand.nextDouble() * 0.2;
        int chRow = 6; //for vertical cutoff set: 7
        int chCol = 17; //for vertical cutoff set: 19
        int[] chPos = {chCol, chRow};
        robots[5] = new KureshyBot(
                    playground,
                    chRow,
                    chCol,
                    Direction.NORTH, // str, ave, dir
                    5, // id
                    3, // role
                    3, // hp
                    chMovesPerTurn,
                    chDodgeDiff
            );
        infos[5] = new PlayerInfo(5, 3, 3, chDodgeDiff, chPos, false);
        */

        /*Test Case #8
        //Chaser should ignore caught targets and other chasers, even if they are the closest.
        //VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<3; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = 1;
            int col = 13;
            int[] pos = {col, row};
            robots[i] = new XiongBot(
                    playground,
                    row,
                    col,
                    Direction.SOUTH, // str, ave, dir
                    i+1, // id
                    1, // role
                    2, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(i+1, 1, 2, dodgeDiff, pos, false);
        }

        //ACTUAL TEST VIP
        int movesPerTurn = rand.nextInt(3) + 1;
        double dodgeDiff = 0.01;
        int row = 6;
        int col = 15;
        int[] pos = {col, row};
        robots[3] = new XiongBot(
                playground,
                row,
                col,
                Direction.SOUTH, // str, ave, dir
                4, // id
                1, // role
                2, // hp
                movesPerTurn,
                dodgeDiff
        );
        infos[3] = new PlayerInfo(4, 1, 2, dodgeDiff, pos, false);

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        int chMovesPerTurn = rand.nextInt(3) + 3;
        double chDodgeDiff = 0.99;
        int chRow = 6;
        int chCol = 16;
        int[] chPos = {chCol, chRow};
        robots[4] = new KureshyBot(
                    playground,
                    chRow,
                    chCol,
                    Direction.NORTH, // str, ave, dir
                    5, // id
                    3, // role
                    3, // hp
                    chMovesPerTurn,
                    chDodgeDiff
            );
        infos[4] = new PlayerInfo(5, 3, 3, chDodgeDiff, chPos, false);

        //TEST CHASER
        int chMovesPerTurn2 = rand.nextInt(3) + 3;
        double chDodgeDiff2 = 0.7 + rand.nextDouble() * 0.2;
        int chRow2 = 6;
        int chCol2 = 17;
        int[] chPos2 = {chCol2, chRow2};
        robots[5] = new KureshyBot(
                playground,
                chRow2,
                chCol2,
                Direction.NORTH, // str, ave, dir
                6, // id
                3, // role
                3, // hp
                chMovesPerTurn2,
                chDodgeDiff2
        );
        infos[5] = new PlayerInfo(6, 3, 3, chDodgeDiff2, chPos2, false);
        */
        

        //init records
        for (BaseBot bot : robots) {
            bot.initRecords(infos);
        }

        int turns = 0;
        while (!gameEnded) {

            for (int i=0; i<robots.length; i++) {

                System.out.format(" TURN: %d Robot: %d \n", turns, robots[i].myRecords.getID());

                //for Test Case 8
                //checkDodge((KureshyBot) robots[4], robots[3], rand);

                robots[i].updateOtherRecords(infos);

                //for Test Case 6
                //if (!robots[i].myRecords.getState() && robots[i].myRecords.getID() == 5) {
                //    int[] newPos = {14,6};
                //    robots[i].moveToPos(newPos);
                //}

                if (!robots[i].myRecords.getState() && (robots[i].myRecords.getRole()==3 /*|| bot.myRecords.getID() == 4*/)) {
                    //second and third (only for Test Case 7) condition for testing purposes

                    //for Test Case 3
                    //checkDodge((KureshyBot) robots[i], robots[i-1], rand);

                    //for Test Case 5
                    //checkDodge((KureshyBot) robots[i], robots[i-1], rand);
                    //checkDodge((KureshyBot) robots[i], robots[i-1], rand);

                    robots[i].takeTurn();
                }

                updateRecords(robots, infos); //update application records
                handleInteractions(robots, infos, rand);
                checkForWinCondition(infos, 1, turns);

            }

            turns++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("DONE!");
    }
}
