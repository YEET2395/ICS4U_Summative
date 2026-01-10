package ICS4U_Summative;
import becker.robots.*;
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
     * @version 2025 12 30
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
    public void updateRecords(BaseBot[] array, PlayerInfo[] records) {

        //iterate through the list of records (each record should match with the BaseBot in array)
        for (int i=0; i<records.length; i++) {

            //update the info that will change
            records[i].updateRecords(array[i].getMyHP(), array[i].getMyPosition(), array[i].getMyState());
        }

    }

    /**
     * Gets the position of all robots of a specific role from the records
     * @param records the application records
     * @param numRobots the number of robots of that role
     * @param role the role of desired robots (4 counts for both Guards and VIPs)
     * @return the x,y coordinates of the robots of the specified role
     */
    public static int[][] getPosOfRole(PlayerInfo[] records, int numRobots, int role) {
        int[][] robotPos = new int[numRobots][2];
        int count = 0; //robotPos index

        //iterate through records
        for (int i=0; i<records.length; i++) {

            //for chaser, checks for both guards and VIPs
            if (role==4) {
                if (records[i].getRole() == 1 || records[i].getRole()==2) {
                    robotPos[count] = records[i].getPosition();
                    count++;
                    System.out.println(count);
                }
            } else {
                //checks role
                if (records[i].getRole() == role) {
                    robotPos[count] = records[i].getPosition();
                    count++;
                }
            }

        }

        return robotPos;
    }

    /**
     * Gets the HP of all robots of a role from the records
     * @param records the application records
     * @param numRobots the number of robots of that role
     * @param role the role of desired robots
     * @return the HP of the robots of the specified role
     */
    public static int[] getHPOfRole(PlayerInfo[] records, int numRobots, int role) {
        int[] robotHP = new int[numRobots];
        int count = 0; //robotHP index

        //iterate through records
        for (int i=0; i<records.length; i++) {

            //checks role
            if (records[i].getRole() == role) {
                robotHP[count] = records[i].getHP();
                count++;
            }
        }

        return robotHP;
    }

    /**
     * Gets the status of all robots from the records
     * @param records the application records
     * @param numRobots the total number of robots
     * @return the isCaught status of all robots with the index corresponding to ID
     */
    public static boolean[] getStates(PlayerInfo[] records, int numRobots) {
        boolean[] robotStatus = new boolean[numRobots];

        //iterate through records
        for (int i=0; i<records.length; i++) {
            robotStatus[i] = records[i].getState();
        }

        return robotStatus;
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

    /**
     * Randomly generates a number and compares it to the chaser and target's dodging capability
     * before applying damage and sending the results to the chaser
     * @param chaser the chaser initiating the catch
     * @param target the target of the chaser
     * @param r the Random object
     */
    public static void checkDodge(KureshyBot chaser, BaseBot target, Random r) {
        int diff = r.nextInt(101);

        //check which robots dodged and which didn't
        if (chaser.getMyDodgeDifficulty() >= diff && target.getMyDodgeDifficulty() >= diff) {
            chaser.sendTagResult(target.getMyID(), false);
            //means both dodged
        } else if (chaser.getMyDodgeDifficulty() >= diff && target.getMyDodgeDifficulty() < diff) {
            chaser.sendTagResult(target.getMyID(), true);
            target.takeDamage(1);
            //means chaser dodged but target didn't
        } else {
            chaser.sendTagResult(target.getMyID(), true);
            chaser.takeDamage(1);
            target.takeDamage(1);
            //means both didn't dodge
        }

    }

    public static void sendInfo(PlayerInfo[] records, BaseBot[] array, int turn) {

        //iterate through the robots
        for (int i=0; i<array.length; i++) {

            if (array[i].getMyRole() == 1) { //for VIPs
                ((XiongBot) array[i]).setChaserPositions(getPosOfRole(records, NUM_CHASERS, 3));
            } else if (array[i].getMyRole() == 2) { //for Guards
                ((LiBot) array[i]).sendVIPPosition(getPosOfRole(records, NUM_VIPS, 1));
                ((LiBot) array[i]).sendChaserPosition(getPosOfRole(records, NUM_CHASERS, 1));
            } else { //for Chasers
                if (turn==1) {
                    ((KureshyBot) array[i]).initTargeting(NUM_VIPS+NUM_GUARDS, NUM_CHASERS);
                }
                ((KureshyBot) array[i]).sendBotsPos(getPosOfRole(records, 4, 4));
                ((KureshyBot) array[i]).sendChasersPos(getPosOfRole(records, 1, 3));
                ((KureshyBot) array[i]).sendStates(getStates(records, NUM_VIPS+NUM_GUARDS+NUM_CHASERS));
            }
        }
    }

    public static void main(String[] args)
    {
        City playground = new City();
        setupPlayground(playground);

        // Initialize arrays for all robots
        BaseBot[] robots = new BaseBot[6];
        PlayerInfo[] infos = new PlayerInfo[6]; // Add this array to store playerInfo
        int index = 0;

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
                    Direction.SOUTH, index, 1, 2,
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(index, 1, 2, dodgeDiff, pos, false);
            index++;
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
                    Direction.NORTH, index, 2, 5,
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(index, 2, 5, dodgeDiff, pos, false);
            index++;
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
                    Direction.NORTH, index, 3, 3,
                    movesPerTurn,
                    dodgeDiff
            );
            infos[i] = new PlayerInfo(index, 3, 3, dodgeDiff, pos, false);
            index++;
        }


    }

}
