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

    public static void main(String[] args)
    {
        City playground = new City();
        setupPlayground(playground);

        // Initialize arrays for chasers, VIPs, and Guards, each with two bots
        KureshyBot[] chasers = new KureshyBot[2];
        XiongBot[] VIPs = new XiongBot[2];
        LiBot[] Guards = new LiBot[2];

        Random rand = new Random();

        // VIP: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        VIPs[0] = new XiongBot(
            playground,
            rand.nextInt(13) + 1,
            rand.nextInt(24) + 1,
            Direction.SOUTH, 1001, 1, 2,
            rand.nextInt(3) + 1,
            0.3 + rand.nextDouble() * 0.1
        );
        VIPs[1] = new XiongBot(
            playground,
            rand.nextInt(13) + 1,
            rand.nextInt(24) + 1,
            Direction.WEST, 1002, 1, 2,
            rand.nextInt(3) + 1,
            0.3 + rand.nextDouble() * 0.1
        );

        // Guard: movesPerTurn [2,4], dodgeDiff [0.45, 0.55]
        Guards[0] = new LiBot(
            playground,
            rand.nextInt(13) + 1,
            rand.nextInt(24) + 1,
            Direction.NORTH, 2001, 2, 5,
            rand.nextInt(3) + 2,
            0.45 + rand.nextDouble() * 0.1
        );
        Guards[1] = new LiBot(
            playground,
            rand.nextInt(13) + 1,
            rand.nextInt(24) + 1,
            Direction.EAST, 2002, 2, 5,
            rand.nextInt(3) + 2,
            0.45 + rand.nextDouble() * 0.1
        );

        // Chaser: movesPerTurn [3,5], dodgeDiff [0.7, 0.9]
        chasers[0] = new KureshyBot(
            playground,
            rand.nextInt(13) + 1,
            rand.nextInt(24) + 1,
            Direction.NORTH, 3001, 3, 3,
            rand.nextInt(3) + 3,
            0.7 + rand.nextDouble() * 0.2
        );
        chasers[1] = new KureshyBot(
            playground,
            rand.nextInt(13) + 1,
            rand.nextInt(24) + 1,
            Direction.EAST, 3001, 3, 3,
            rand.nextInt(3) + 3,
            0.7 + rand.nextDouble() * 0.2
        );
    }

}
