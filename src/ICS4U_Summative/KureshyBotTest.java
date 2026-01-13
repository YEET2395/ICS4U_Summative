package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;
import java.util.*;

/**
 * Testing class for KureshyBot to test targeting and movement
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
     * Checks if a chaser is tagging
     * @param array the BaseBot array
     * @param rand the Random object used for checking who dodged
     */
    public static void checkForTags(BaseBot[] array, Random rand) {
        //iterate through the array
        for (BaseBot bot : array) {

            //check if the bot is a chser
            if (bot.myRecords.getRole() == 3) {
                for (BaseBot targets : array) {

                    //check if the chaser is actually catching and if it's position is the same as its target
                    if (((KureshyBot) bot).getIsCatching() && Arrays.equals(bot.getMyPosition(), targets.getMyPosition())) {
                        int target = ((KureshyBot) bot).getTargetID();

                        //deals the damage
                        checkDodge((KureshyBot) bot, array[target], rand);
                    }
                }
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

    public static void main(String[] args) {
        City playground = new City();
        setupPlayground(playground);
        playground.setSize(1500, 900);

        Random rand = new Random();

        BaseBot[] robots = new BaseBot[6];
        PlayerInfo[] infos = new PlayerInfo[6];

        /* Test Case #1
        Chaser will automatically assign the robot closest to it with the highest
        priorityScore, even if they within range of the current turn (unless it has additional
        like whether it's actually a guard based on movement observation/hp estimates)
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

        /*Test Case #2
        Chaser will go after the nearest target even if they are out of range (unless
        one of the other factors like dodgeEst, hpEst, or rolePrediction influence it to prefer an
        easier target)
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

        /*
        Test Case #3: Chaser will go after the robot with the lower dodge estimation (assuming other
        factors are the same)
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
        Chaser should be slightly disincentivised from going after the same target if another chaser
        is already close to it
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
        //Chaser will automatically avoid confirmed guards (in this case confirmed using number of catches)
        //if low on health (set to 1), even if they have the best priorityScore
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
        //Chaser can calculate the speed of targets and use that to deprioritize them if is  matches a guard's
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


        //init records
        for (BaseBot bot : robots) {
            bot.initRecords(infos);
        }

        int turns = 0;
        while (!gameEnded) {

            for (int i=0; i<robots.length; i++) {

                System.out.format(" TURN: %d Robot: %d \n", turns, robots[i].myRecords.getID());
                robots[i].updateOtherRecords(infos);

                //for Test Case 6
                //if (!robots[i].myRecords.getState() && robots[i].myRecords.getID() == 5) {
                //    int[] newPos = {14,6};
                //    robots[i].moveToPos(newPos);
                //}

                if (!robots[i].myRecords.getState() && robots[i].myRecords.getRole()==3) {
                    //second condition for testing purposes

                    //for Test Case 3
                    //checkDodge((KureshyBot) robots[i], robots[i-1], rand);

                    //for Test Case 5
                    //checkDodge((KureshyBot) robots[i], robots[i-1], rand);
                    //checkDodge((KureshyBot) robots[i], robots[i-1], rand);

                    robots[i].takeTurn();

                }
                checkForTags(robots, rand);

                checkForWinCondition(infos, 1, turns);

                updateRecords(robots, infos); //update application records
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
