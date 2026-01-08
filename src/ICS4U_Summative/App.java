package ICS4U_Summative;
import becker.robots.*;
import java.util.Random;

/**
 * Application class for the summative project
 * @author Aadil Kureshy, Austin Xiong,  Xinran Li
 * @version 2025 12 30
 */
public class App {

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

    public static void testXiongBotSpeedTracking() {
        // Create a simple test scenario with XiongBot VIP and TestChaserBot
        City playground = new City();
        setupPlayground(playground);
        playground.setSize(1500, 900);

        // Create two XiongBot VIPs at different positions
        XiongBot[] vipBots = new XiongBot[2];
        vipBots[0] = new XiongBot(playground, 5, 2, Direction.EAST, 1, 1, 100, 1, 5);
        vipBots[1] = new XiongBot(playground, 6, 3, Direction.EAST, 2, 1, 100, 1, 5);

        // Create two TestChaserBots at different positions
        TestChaserBot[] testChasers = new TestChaserBot[2];
        testChasers[0] = new TestChaserBot(playground, 10, 2, Direction.NORTH, 3, 3, 100, 1, 1);
        testChasers[1] = new TestChaserBot(playground, 11, 3, Direction.NORTH, 4, 3, 100, 1, 1);

        // Example: set up the chaser positions for the first VIP to track
        int[][] chaserPositions = new int[][]{
                {testChasers[0].getX(), testChasers[0].getY()},
                {testChasers[1].getX(), testChasers[1].getY()}
        };
        vipBots[0].setChaserPositions(chaserPositions);
        vipBots[1].setChaserPositions(chaserPositions);

        // Test loop - simulate 20 turns
        System.out.println("Starting Test: XiongBot Speed Tracking and Position Prediction");
        System.out.println("========================================================");

        for (int turn = 0; turn < 20; turn++) {
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

    public static void main(String[] args) {
        playerInfo[] appRecords;
        playerInfo[] publicRecords;
        playerInfo[] chaserRecords;
        testXiongBotSpeedTracking();
    }

}
