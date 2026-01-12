package ICS4U_Summative;

import becker.robots.*;
import java.util.*;

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
        }

    }

    /**
     * Randomly generates a number and compares it to the chaser and target's dodging capability
     * before applying damage and sending the results to the chaser
     * @param chaser the chaser initiating the catch
     * @param target the target of the chaser
     * @param r the Random object
     */
    public static void checkDodge(KureshyBot chaser, BaseBot target, Random r) {
        double diff = r.nextDouble();
        System.out.format("Chaser: %s -- Target %d: %s -- Difficulty: %.2f\n",
                Arrays.toString(chaser.myRecords.getPosition()),
                target.myRecords.getID(),
                Arrays.toString(target.myRecords.getPosition()),
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
     * Checks if either the VIPs/Guards or Chasers has reached their win conditions
     * @param records the application records
     * @param maxTurns the max number of turns
     * @param turn the current turn
     */
    public void checkForWinCondition(PlayerInfo[] records, int maxTurns, int turn) {
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

        // VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i=0; i<5; i++) {
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
            infos[i] = new PlayerInfo(i, 1, 2, dodgeDiff, pos, false);
        }

        // Chasers: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
            int movesPerTurn = rand.nextInt(3) + 3;
            double dodgeDiff = 0.7 + rand.nextDouble() * 0.2;
            int row = rand.nextInt(13) + 1;
            int col = rand.nextInt(24) + 1;
            int[] pos = {row, col};
            robots[5] = new KureshyBot(
                    playground,
                    row,
                    col,
                    Direction.NORTH, // str, ave, dir
                    5, // id
                    3, // role
                    3, // hp
                    movesPerTurn,
                    dodgeDiff
            );
            infos[5] = new PlayerInfo(5, 3, 3, dodgeDiff, pos, false);

            //init records
            for (BaseBot bot : robots) {
                bot.initRecords(infos);
            }

        //simulating 10 turns to test the checkDodge function and see if the hp and dodging
        //predictions (and chaser pressure) from priorityScore are working as intended (mainly, to serve
        //as a tie-breaker when deciding which target to go after if they're both close by)
        for (int turns=0; turns<10; turns++) {
            for (int i=0; i<6; i++) {
                robots[i].updateOtherRecords(infos);
                System.out.format("TURN: %d \n ROBOT ACTIVE: %d\n", turns, infos[i].getID());
                robots[i].takeTurn();
                updateRecords(robots, infos);
            }

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
