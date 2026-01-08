package ICS4U_Summative;
import becker.robots.*;

/**
 * Application class for the summative project
 * @author Aadil Kureshy, Austin Xiong,  Xinran Li
 * @version 2025 12 30
 */
public class SummativeAppClass {

    /**
     * Set up the playground for the robots
     * @author Xinran Li
     * @version 2025 12 30
     */
    private static void setupPlayground()
    {
        City playground = new City();
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

//    /**
//     * Updates the robots copy of their records
//     * @param array the array of BaseBots
//     * @param publicRecords the records containing the info for the Guards/VIP
//     * @param chaserRecords the records containing the info for the Chasers
//     */
//    public void updateBotRecords(BaseBot[] array, playerInfo[] publicRecords, playerInfo[] chaserRecords) {
//        //iterate through the robot array
//        for (int i=0; i<array.length; i++) {
//
//            //check if the robot is a VIP/Guard; otherwise give chaser records
//            if (array[i].getMyRole() != 3) {
//                array[i].setRecords(publicRecords);
//            } else {
//                array[i].setRecords(chaserRecords);
//            }
//
//        }
//    }

    /**
     * Updates the records used by the application
     * @param array the array of BaseBots
     * @param records the array of records to update
     */
    public void updateRecords(BaseBot[] array, playerInfo[] records) {

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
     * @param role the role of desired robots
     * @return the x,y coordinates of the robots of the specified role
     */
    public static int[][] getPosOfRole(playerInfo[] records, int numRobots, int role) {
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
    public static int[] getHPOfRole(playerInfo[] records, int numRobots, int role) {
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
    public static boolean[] getStates(playerInfo[] records, int numRobots) {
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
        int diff = r.nextInt(101);

        //check which robots dodged and which didn't
        if (chaser.getDodgeDifficulty() >= diff && target.getDodgeDifficulty() >= diff) {
            chaser.sendTagResult(target.getMyID(), false);
            //means both dodged
        } else if (chaser.getDodgeDifficulty() >= diff && target.getDodgeDifficulty() < diff) {
            chaser.sendTagResult(target.getMyID(), true);
            target.takeDamage(1);
            //means chaser dodged but target didn't
        } else if (chaser.getDodgeDifficulty() < diff && target.getDodgeDifficulty() >= diff) {
            chaser.sendTagResult(target.getMyID(), false);
            chaser.takeDamage(1);
            //means target dodged but chaser didn't
        } else {
            chaser.sendTagResult(target.getMyID(), true);
            chaser.takeDamage(1);
            target.takeDamage(1);
            //means both didn't dodge
        }

    }
    
    public static void main(String[] args) {
        setupPlayground();

        playerInfo[] appRecords;
        playerInfo[] publicRecords;
        playerInfo[] chaserRecords;
        testXiongBotSpeedTracking();
    }
    public static void testXiongBotSpeedTracking() {
        // Create a simple test scenario with XiongBot VIP and TestChaserBot
        City testCity = new City();
        testCity.setSize(1500, 900);

        // Create the XiongBot VIP at position (2, 5)
        XiongBot vipBot = new XiongBot(testCity, 5, 2, Direction.EAST, 1, 1, 100, 1, 5);

        // Create TestChaserBot at position (2, 10) moving towards the VIP
        TestChaserBot testChaser = new TestChaserBot(testCity, 10, 2, Direction.NORTH, 2, 3, 100, 1, 1);

        // Set up the chaser positions for the VIP to track
        int[][] chaserPositions = new int[][]{{2, 10}};
        vipBot.setChaserPositions(chaserPositions);

        // Test loop - simulate 20 turns
        System.out.println("Starting Test: XiongBot Speed Tracking and Position Prediction");
        System.out.println("========================================================");

        for (int turn = 0; turn < 20; turn++) {
            // Update chaser position
            chaserPositions[0][0] = testChaser.getX();
            chaserPositions[0][1] = testChaser.getY();
            vipBot.setChaserPositions(chaserPositions);

            // Track speeds
            vipBot.trackChaserSpeeds();

            // Get current speed and predict future position
            double speed = vipBot.getChaserSpeed(0);
            int[] predictedPos = vipBot.predictChaserPosition(0, 5);

            // Print debug information
            System.out.println("Turn " + turn + ":");
            System.out.println("  Chaser Position: (" + testChaser.getX() + ", " + testChaser.getY() + ")");
            System.out.println("  Chaser Speed: " + String.format("%.2f", speed) + " units/turn");
            if (predictedPos != null) {
                System.out.println("  Predicted Position (5 turns ahead): (" + predictedPos[0] + ", " + predictedPos[1] + ")");
            }

            // Let both bots take their turn
            testChaser.takeTurn();
            vipBot.takeTurn();

            System.out.println();
        }

        System.out.println("Test Complete!");
    }
}
