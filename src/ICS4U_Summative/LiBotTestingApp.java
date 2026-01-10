package ICS4U_Summative;

import becker.robots.*;
import java.util.Random;

public class LiBotTestingApp {

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

    public static void main(String[] args) {

        City playground = new City();
        setupPlayground(playground);

        PlayerInfo[] allRecords = new PlayerInfo[6];
        XiongBot[] VIPs = new XiongBot[2];
        LiBot[] guards = new LiBot[2];
        KureshyBot[] chasers = new KureshyBot[2];

        Random rand = new Random();

        // VIPs: movesPerTurn [1,3], dodgeDiff [0.3, 0.4]
        for (int i = 0; i < 2; i++) {
            int movesPerTurn = rand.nextInt(3) + 1;
            double dodgeDiff = 0.3 + rand.nextDouble() * 0.1;
            int row = rand.nextInt(13) + 1;
            int col = rand.nextInt(24) + 1;
            int[] pos = {row, col};

            VIPs[i] = new XiongBot(playground, row, col, Direction.SOUTH,
                    i, 1, 2, movesPerTurn, dodgeDiff);

            allRecords[i] = new PlayerInfo(i, 1, 2, dodgeDiff, pos, false);
        }

        // Guards: i=2..3  -> guards[0..1]
        for (int i = 2; i < 4; i++) {
            int movesPerTurn = rand.nextInt(3) + 2;
            double dodgeDiff = 0.45 + rand.nextDouble() * 0.1;
            int row = rand.nextInt(13) + 1;
            int col = rand.nextInt(24) + 1;
            int[] pos = {row, col};

            guards[i - 2] = new LiBot(playground, row, col, Direction.NORTH,
                    i, 2, 5, movesPerTurn, dodgeDiff);

            allRecords[i] = new PlayerInfo(i, 2, 5, dodgeDiff, pos, false);
        }

        // Chasers: i=4..5 -> chasers[0..1]
        for (int i = 4; i < 6; i++) {
            int movesPerTurn = rand.nextInt(3) + 3;
            double dodgeDiff = 0.7 + rand.nextDouble() * 0.2;
            int row = rand.nextInt(13) + 1;
            int col = rand.nextInt(24) + 1;
            int[] pos = {row, col};

            chasers[i - 4] = new KureshyBot(playground, row, col, Direction.NORTH,
                    i, 3, 3, movesPerTurn, dodgeDiff);

            allRecords[i] = new PlayerInfo(i, 3, 3, dodgeDiff, pos, false);
        }

        for(int i = 0; i < 30; i++)
        {
            // Update records for all bots
            for (XiongBot vip : VIPs) {
                vip.updateOtherRecords(allRecords);
            }
            for (LiBot guard : guards) {
                guard.updateOtherRecords(allRecords);
            }
            for (KureshyBot chaser : chasers) {
                chaser.updateOtherRecords(allRecords);
            }

            // Each bot takes its turn
            for (XiongBot vip : VIPs) {
                vip.takeTurn();
            }
            for (LiBot guard : guards) {
                guard.takeTurn();
            }
            for (KureshyBot chaser : chasers) {
                chaser.takeTurn();
            }

            // Pause between turns for visibility
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
