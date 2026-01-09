package ICS4U_Summative;
import becker.robots.*;
import java.util.Random;

/**
 * Application class for the summative project
 * @author Aadil Kureshy, Austin Xiong,  Xinran Li
 * @version 2025 12 30
 */
public class LiBotTestingApp {

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

    public static void main(String[] args)
    {
        City playground = new City();
        setupPlayground(playground);

        Random rand = new Random();

        // Random positions for LiBot (Guard) and LiTestChaserBot (Chaser)
        int guardRow = rand.nextInt(13) + 1;
        int guardCol = rand.nextInt(24) + 1;
        int chaserRow = rand.nextInt(13) + 1;
        int chaserCol = rand.nextInt(24) + 1;

        // Create LiBot (Guard, role=2)
        LiBot guard = new LiBot(
                playground,
                guardRow,
                guardCol,
                Direction.NORTH,
                0, // id
                2, // role
                5, // hp
                3, // movesPerTurn
                0.5 // dodgeDiff
        );

        // Create LiTestChaserBot (Chaser, role=3)
        LiTestChaserBot chaser = new LiTestChaserBot(
                playground,
                chaserRow,
                chaserCol,
                Direction.SOUTH,
                1, // id
                3, // role
                3, // hp
                4, // movesPerTurn
                0.8 // dodgeDiff
        );

        // Main loop: let chaser chase guard for 20 turns
        for (int turn = 0; turn < 20; turn++) {
            // Prepare positions and roles arrays for LiTestChaserBot
            int[][] positions = new int[2][2];
            int[] roles = new int[2];
            positions[0] = guard.getMyPosition();
            positions[1] = chaser.getMyPosition();
            roles[0] = 2; // Guard
            roles[1] = 3; // Chaser

            chaser.setBotsInfo(positions, roles);
            chaser.takeTurn();

            // Optionally, move the guard randomly (or keep it stationary)
            // Uncomment below to make the guard move randomly:
            /*
            int moveDir = rand.nextInt(4);
            Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            guard.turnDirection(dirs[moveDir]);
            if (guard.frontIsClear()) {
                guard.move();
            }
            */

            try {
                Thread.sleep(400); // Pause for visibility
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
