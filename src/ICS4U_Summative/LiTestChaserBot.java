package ICS4U_Summative;

import becker.robots.*;
import java.awt.*;

/**
 * Simple test chaser: always chase the nearest Guard (role 2).
 */
public class LiTestChaserBot extends BaseBot {

    private PlayerInfo[] visibleRecords;

    public LiTestChaserBot(City city, int str, int ave, Direction dir,
                         int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
        super(city, str, ave, dir, id, role, hp, movesPerTurn, dodgeDiff);

        super.setColor(Color.MAGENTA);
        super.setLabel("TestChaser " + super.myRecords.getID());
    }


    public void updateOtherRecords(PlayerInfo[] records) {
        this.visibleRecords = records;
    }


    public void takeTurn() {
        if (visibleRecords == null || visibleRecords.length == 0) return;

        int[] myPos = this.getMyPosition();


        PlayerInfo target = null;
        int bestDist = Integer.MAX_VALUE;

        for (PlayerInfo r : visibleRecords) {
            if (r == null || r.getState()) continue;
            if (r.getRole() != 2) continue;


            if (r.getID() == this.myRecords.getID()) continue;

            int d = manhattan(myPos, r.getPosition());
            if (d < bestDist) {
                bestDist = d;
                target = r;
            }
        }

        if (target == null) return;

        int[] targetPos = target.getPosition();


        int moves = this.getMOVES_PER_TURN();
        int[] cur = this.getMyPosition();

        for (int step = 0; step < moves; step++) {
            int dStreet = targetPos[0] - cur[0];
            int dAve    = targetPos[1] - cur[1];

            if (dStreet == 0 && dAve == 0) break;


            if (dStreet != 0) {
                this.turnDirection(dStreet > 0 ? Direction.SOUTH : Direction.NORTH);
                if (this.frontIsClear()) {
                    this.move();
                    cur = this.getMyPosition();
                    continue;
                }
            }

            if (dAve != 0) {
                this.turnDirection(dAve > 0 ? Direction.EAST : Direction.WEST);
                if (this.frontIsClear()) {
                    this.move();
                    cur = this.getMyPosition();
                }
            }
        }
    }

    private int manhattan(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }
}
