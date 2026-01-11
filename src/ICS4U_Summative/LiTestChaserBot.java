package ICS4U_Summative;

import becker.robots.*;
import java.awt.*;

public class LiTestChaserBot extends BaseBot {

    public LiTestChaserBot(City city, int str, int ave, Direction dir,
                           int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
        super(city, str, ave, dir, id, role, hp, movesPerTurn, dodgeDiff);
        setColor(Color.RED);
        setLabel("Chaser " + myRecords.getID());
    }

    @Override
    public void updateOtherRecords(PlayerInfo[] records) {
        this.otherRecords = records;
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
