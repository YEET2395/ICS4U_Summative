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

    /**
     * Updates the robots copy of their records
     * @param array the array of BaseBots
     * @param publicRecords the records containing the info for the Guards/VIP
     * @param chaserRecords the records containing the info for the Chasers
     */
    public void updateBotRecords(BaseBot[] array, playerInfo[] publicRecords, playerInfo[] chaserRecords) {
        //iterate through the robot array
        for (int i=0; i<array.length; i++) {

            //check if the robot is a VIP/Guard; otherwise give chaser records
            if (array[i].getMyRole() != 3) {
                array[i].setRecords(publicRecords);
            } else {
                array[i].setRecords(chaserRecords);
            }

        }
    }

    /**
     * Updates the records used by the application/robots
     * @param array the array of BaseBots
     * @param records the array of records to update
     * @param isChaserRecords to check whether to update the records with health values or not
     */
    public void updateRecords(BaseBot[] array, playerInfo[] records, boolean isChaserRecords) {
        //iterate through the list of records (each record should match with the BaseBot in array)
        for (int i=0; i<records.length; i++) {

            //checks whether it's updating the application/public record or chaser record
            if (!isChaserRecords) {
                records[i].updateRecords(array[i].getMyHP(), array[i].getMyPosition(), array[i].getMyState());
            } else {
                records[i].updateRecords(array[i].getMyPosition(), array[i].getMyState());
            }

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
