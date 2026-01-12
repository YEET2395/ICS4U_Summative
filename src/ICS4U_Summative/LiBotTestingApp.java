package ICS4U_Summative;

import becker.robots.*;
import java.awt.Color;
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

    static class SimpleVIPBot extends BaseBot {
        private final Random rnd = new Random();

        public SimpleVIPBot(City city, int str, int ave, Direction dir,
                            int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
            super(city, str, ave, dir, id, role, hp, movesPerTurn, dodgeDiff);
            setColor(Color.GREEN);
            setLabel("VIP " + myRecords.getID());
        }

        @Override
        public void updateOtherRecords(PlayerInfo[] records) {
            this.otherRecords = records;
        }

        public void initRecords(PlayerInfo[] records) {
            System.out.println("Initializing records");
        }

        @Override
        public void takeTurn() {
            if (otherRecords == null || otherRecords.length == 0) return;

            PlayerInfo nearestChaser = null;
            int bestDist = Integer.MAX_VALUE;

            int[] myPos = getMyPosition();

            for (PlayerInfo r : otherRecords) {
                if (r == null || r.getState()) continue;
                if (r.getRole() != 3) continue;
                int d = manhattan(myPos, r.getPosition());
                if (d < bestDist) {
                    bestDist = d;
                    nearestChaser = r;
                }
            }

            int moves = getMOVES_PER_TURN();

            if (nearestChaser == null) {
                for (int step = 0; step < moves; step++) {
                    Direction d = randomDir();
                    tryMove(d);
                }
                return;
            }

            for (int step = 0; step < moves; step++) {
                int[] cur = getMyPosition();
                int[] cpos = nearestChaser.getPosition();

                Direction bestDir = null;
                int best = manhattan(cur, cpos);

                for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    if (!canMove(d)) continue;
                    int[] next = nextPos(cur, d);
                    int dist = manhattan(next, cpos);
                    if (dist > best) {
                        best = dist;
                        bestDir = d;
                    }
                }

                if (bestDir == null) {
                    Direction d = randomDir();
                    tryMove(d);
                } else {
                    tryMove(bestDir);
                }
            }
        }

        private Direction randomDir() {
            int r = rnd.nextInt(4);
            if (r == 0) return Direction.NORTH;
            if (r == 1) return Direction.SOUTH;
            if (r == 2) return Direction.EAST;
            return Direction.WEST;
        }

        private boolean canMove(Direction d) {
            turnDirection(d);
            return frontIsClear();
        }

        private boolean tryMove(Direction d) {
            turnDirection(d);
            if (frontIsClear()) {
                move();
                return true;
            }
            return false;
        }

        private int[] nextPos(int[] cur, Direction d) {
            int x = cur[0], y = cur[1];
            if (d == Direction.EAST)  x++;
            if (d == Direction.WEST)  x--;
            if (d == Direction.SOUTH) y++;
            if (d == Direction.NORTH) y--;
            return new int[]{x, y};
        }

        private int manhattan(int[] a, int[] b) {
            return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
        }
    }

    static class SimpleChaserBot extends BaseBot {

        public SimpleChaserBot(City city, int str, int ave, Direction dir,
                               int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
            super(city, str, ave, dir, id, role, hp, movesPerTurn, dodgeDiff);
            setColor(Color.RED);
            setLabel("Chaser " + myRecords.getID());
        }

        @Override
        public void updateOtherRecords(PlayerInfo[] records) {
            this.otherRecords = records;
        }

        public void initRecords(PlayerInfo[] records) {
            System.out.println("Initializing records");
        }

        @Override
        public void takeTurn() {
            if (otherRecords == null || otherRecords.length == 0) return;

            PlayerInfo target = null;
            int bestDist = Integer.MAX_VALUE;

            int[] myPos = getMyPosition();

            for (PlayerInfo r : otherRecords) {
                if (r == null || r.getState()) continue;
                if (r.getID() == myRecords.getID()) continue;
                if (r.getRole() == 3) continue;

                int d = manhattan(myPos, r.getPosition());
                if (d < bestDist) {
                    bestDist = d;
                    target = r;
                }
            }

            if (target == null) return;

            int moves = getMOVES_PER_TURN();
            for (int step = 0; step < moves; step++) {
                int[] cur = getMyPosition();
                int[] tp = target.getPosition();

                int dx = tp[0] - cur[0];
                int dy = tp[1] - cur[1];
                if (dx == 0 && dy == 0) break;

                boolean moved = false;

                if (Math.abs(dx) >= Math.abs(dy) && dx != 0) {
                    moved = tryMove(dx > 0 ? Direction.EAST : Direction.WEST);
                    if (!moved && dy != 0) moved = tryMove(dy > 0 ? Direction.SOUTH : Direction.NORTH);
                } else if (dy != 0) {
                    moved = tryMove(dy > 0 ? Direction.SOUTH : Direction.NORTH);
                    if (!moved && dx != 0) moved = tryMove(dx > 0 ? Direction.EAST : Direction.WEST);
                }

                if (!moved) break;
            }
        }

        private boolean tryMove(Direction d) {
            turnDirection(d);
            if (frontIsClear()) {
                move();
                return true;
            }
            return false;
        }

        private int manhattan(int[] a, int[] b) {
            return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
        }
    }

    private static void refreshAllRecords(BaseBot[] bots, PlayerInfo[] records) {
        for (BaseBot b : bots) {
            int id = b.myRecords.getID();
            records[id].updateRecords(
                    b.myRecords.getHP(),
                    b.getMyPosition(),
                    b.myRecords.getState()
            );
        }
    }

    public static void main(String[] args) {

        City playground = new City();
        setupPlayground(playground);

        Random rand = new Random();

        SimpleVIPBot[] VIPs = new SimpleVIPBot[2];
        SimpleChaserBot[] chasers = new SimpleChaserBot[2];
        LiBot[] guards = new LiBot[2];

        VIPs[0] = new SimpleVIPBot(playground, rand.nextInt(13)+1, rand.nextInt(24)+1, Direction.SOUTH,
                0, 1, 2, rand.nextInt(3)+1, 0.3 + rand.nextDouble()*0.1);
        VIPs[1] = new SimpleVIPBot(playground, rand.nextInt(13)+1, rand.nextInt(24)+1, Direction.SOUTH,
                1, 1, 2, rand.nextInt(3)+1, 0.3 + rand.nextDouble()*0.1);

        chasers[0] = new SimpleChaserBot(playground, rand.nextInt(13)+1, rand.nextInt(24)+1, Direction.NORTH,
                2, 3, 3, rand.nextInt(3)+3, 0.7 + rand.nextDouble()*0.2);
        chasers[1] = new SimpleChaserBot(playground, rand.nextInt(13)+1, rand.nextInt(24)+1, Direction.NORTH,
                3, 3, 3, rand.nextInt(3)+3, 0.7 + rand.nextDouble()*0.2);

        guards[0] = new LiBot(playground, rand.nextInt(13)+1, rand.nextInt(24)+1, Direction.NORTH,
                4, 2, 5, rand.nextInt(3)+2, 0.45 + rand.nextDouble()*0.1);
        guards[1] = new LiBot(playground, rand.nextInt(13)+1, rand.nextInt(24)+1, Direction.NORTH,
                5, 2, 5, rand.nextInt(3)+2, 0.45 + rand.nextDouble()*0.1);

        BaseBot[] allBots = new BaseBot[] {
                VIPs[0], VIPs[1],
                chasers[0], chasers[1],
                guards[0], guards[1]
        };

        PlayerInfo[] allRecords = new PlayerInfo[6];
        for (BaseBot b : allBots) {
            int id = b.myRecords.getID();
            allRecords[id] = new PlayerInfo(
                    id,
                    b.myRecords.getRole(),
                    b.myRecords.getHP(),
                    b.myRecords.getDodgeDifficulty(),
                    b.getMyPosition(),
                    b.myRecords.getState()
            );
        }

        for (int turn = 1; turn <= 30; turn++) {

            refreshAllRecords(allBots, allRecords);

            for (SimpleVIPBot vip : VIPs) vip.updateOtherRecords(allRecords);
            for (SimpleVIPBot vip : VIPs) vip.takeTurn();

            refreshAllRecords(allBots, allRecords);

            for (SimpleChaserBot c : chasers) c.updateOtherRecords(allRecords);
            for (SimpleChaserBot c : chasers) c.takeTurn();

            refreshAllRecords(allBots, allRecords);

            for (LiBot g : guards) g.otherRecords = allRecords;
            for (LiBot g : guards) g.takeTurn();

            refreshAllRecords(allBots, allRecords);

            int[] v0 = VIPs[0].getMyPosition();
            int[] c0 = chasers[0].getMyPosition();
            int[] g0 = guards[0].getMyPosition();
            System.out.println("Turn " + turn +
                    " | VIP0=(" + v0[0] + "," + v0[1] + ")" +
                    " | Chaser0=(" + c0[0] + "," + c0[1] + ")" +
                    " | Guard0=(" + g0[0] + "," + g0[1] + ")");

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
