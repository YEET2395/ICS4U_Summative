package ICS4U_Summative;
import becker.robots.*;
import java.util.Random;

/**
 * Initial Testing Application for XiongBot features, Deprecated
 * @author Austin Xiong
 * @version 2025 12 30
 */
public class DeprecatedXiongBotTestingApp {

    /**
     * Set up the playground for the robots
     * @author Austin Xiong
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
     * @param array the array of BaseBot
     * @param records the array of records to update
     */
    public void updateRecords(BaseBot[] array, PlayerInfo[] records) {

        //iterate through the list of records (each record should match with the BaseBot in array)
        for (int i=0; i<records.length; i++) {

            //update the info that will change
            records[i].updateRecords(array[i].myRecords.getHP(), array[i].getMyPosition(), array[i].myRecords.getState());
            }

    }

    /**
     * Gets the position of all robots of a specific role from the records
     * @param records the application records
     * @param numRobots the number of robots of that role
     * @param role the role of desired robots
     * @return the x,y coordinates of the robots of the specified role
     */
    public static int[][] getPosOfRole(PlayerInfo[] records, int numRobots, int role) {
        int[][] robotPos = new int[numRobots][2];
        int count = 0; //robotPos index

        //iterate through records
        for (int i=0; i<records.length; i++) {

            //checks role
            if (records[i].getRole() == role) {
                robotPos[count] = records[i].getPosition();
                count++;
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
     * Randomly generates a number and compares it to the chaser and target's dodging capability
     * before applying damage and sending the results to the chaser
     * @param chaser
     * @param target
     * @param r
     * @return
     */
    public static void checkDodge(KureshyBot chaser, BaseBot target, Random r) {
        double diff = r.nextDouble(); // 0~1 double

        //check which robots dodged and which didn't
        if (chaser.myRecords.getDodgeDifficulty() >= diff && target.myRecords.getDodgeDifficulty() >= diff) {
            chaser.sendTagResult(target.myRecords.getID(), false);
            //means both dodged
        } else if (chaser.myRecords.getDodgeDifficulty() >= diff && target.myRecords.getDodgeDifficulty() < diff) {
            chaser.sendTagResult(target.myRecords.getID(), true);
            target.takeDamage(1);
            //means chaser dodged but target didn't
        } else if (chaser.myRecords.getDodgeDifficulty() < diff && target.myRecords.getDodgeDifficulty() >= diff) {
            chaser.sendTagResult(target.myRecords.getID(), false);
            chaser.takeDamage(1);
            //means target dodged but chaser didn't
        } else {
            chaser.sendTagResult(target.myRecords.getID(), true);
            chaser.takeDamage(1);
            target.takeDamage(1);
            //means both didn't dodge
        }

    }
    
    public static void main(String[] args) {
        PlayerInfo[] appRecords;
        PlayerInfo[] publicRecords;
        PlayerInfo[] chaserRecords;
        testXiongBotSpeedTracking();
    }
    public static void testXiongBotSpeedTracking() {
        // Create a simple test scenario with XiongBot VIP and TestChaserBot
        City playground = new City();
        setupPlayground(playground);
        playground.setSize(1500, 900);

        // Create two XiongBot VIPs at different positions
        XiongBot[] vipBots = new XiongBot[2];
        vipBots[0] = new XiongBot(playground, 5, 2, Direction.EAST, 1, 1, 100, 1, 5.0);
        vipBots[1] = new XiongBot(playground, 6, 3, Direction.EAST, 2, 1, 100, 1, 5.0);

        // Create two TestChaserBots at different positions
        TestChaserBot[] testChasers = new TestChaserBot[2];
        testChasers[0] = new TestChaserBot(playground, 10, 2, Direction.NORTH, 3, 3, 100, 1, 1.0);
        testChasers[1] = new TestChaserBot(playground, 11, 3, Direction.NORTH, 4, 3, 100, 1, 1.0);

        // Example: set up the chaser positions for the first VIP to track
        int[][] chaserPositions = new int[][]{
            {testChasers[0].getX(), testChasers[0].getY()},
            {testChasers[1].getX(), testChasers[1].getY()}
        };
        vipBots[0].setChaserPositions(chaserPositions);
        vipBots[1].setChaserPositions(chaserPositions);

        // Test loop - simulate 50 turns
        System.out.println("Starting Test: XiongBot Speed Tracking and Position Prediction");
        System.out.println("========================================================");

        for (int turn = 0; turn < 50; turn++) {
            // Update chaser positions
            chaserPositions[0][0] = testChasers[0].getX();
            chaserPositions[0][1] = testChasers[0].getY();
            chaserPositions[1][0] = testChasers[1].getX();
            chaserPositions[1][1] = testChasers[1].getY();
            vipBots[0].setChaserPositions(chaserPositions);
            vipBots[1].setChaserPositions(chaserPositions);

            // Track speeds for both VIPs
            vipBots[0].trackChaserSpeeds();
            vipBots[1].trackChaserSpeeds();

            // Get current speed and predict future position for both VIPs
            for (int i = 0; i < vipBots.length; i++) {
                System.out.println("VIPBot " + i + " Turn " + turn + ":");
                for (int j = 0; j < testChasers.length; j++) {
                    double speed = vipBots[i].getChaserSpeed(j);
                    int[] predictedPos = vipBots[i].predictChaserPosition(j, 5);
                    System.out.println("  Chaser " + j + " Position: (" + testChasers[j].getX() + ", " + testChasers[j].getY() + ")");
                    System.out.println("  Chaser " + j + " Speed: " + String.format("%.2f", speed) + " units/turn");
                    if (predictedPos != null) {
                        System.out.println("  Predicted Position (5 turns ahead): (" + predictedPos[0] + ", " + predictedPos[1] + ")");
                    }
                }
            }

            // Let both chasers and VIPs take their turn
            for (TestChaserBot chaser : testChasers) {
                chaser.takeTurn();
            }
            for (XiongBot vip : vipBots) {
                vip.takeTurn();
            }

            System.out.println();
        }

        System.out.println("Test Complete!");
    }
}
