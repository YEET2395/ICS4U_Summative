package ICS4U_Summative;

import becker.robots.*;
import java.awt.*;
import java.util.Random;

public class LiTestVIPBot extends BaseBot {

    private final Random rnd = new Random();

    public LiTestVIPBot(City city, int str, int ave, Direction dir,
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

        // 找最近的chaser
        PlayerInfo nearestChaser = null;
        int bestDist = Integer.MAX_VALUE;

        int[] myPos = getMyPosition();

        for (PlayerInfo r : otherRecords) {
            if (r == null || r.getState()) continue;
            if (r.getRole() != 3) continue; // 只看chaser
            int d = manhattan(myPos, r.getPosition());
            if (d < bestDist) {
                bestDist = d;
                nearestChaser = r;
            }
        }

        int moves = getMOVES_PER_TURN();

        // 没有chaser就随便走走（可选）
        if (nearestChaser == null) {
            for (int step = 0; step < moves; step++) {
                Direction d = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}[rnd.nextInt(4)];
                if (tryMove(d)) return;
            }
            return;
        }

        // 有chaser：每一步选一个能让“离chaser更远”的方向
        for (int step = 0; step < moves; step++) {
            int[] cur = getMyPosition();
            int[] cpos = nearestChaser.getPosition();

            Direction bestDir = null;
            int best = manhattan(cur, cpos);

            for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                int[] next = nextPos(cur, d);
                if (!canMove(d)) continue;

                int dist = manhattan(next, cpos);
                if (dist > best) {
                    best = dist;
                    bestDir = d;
                }
            }

            if (bestDir == null) break; // 四个方向都走不了
            tryMove(bestDir);
        }
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
