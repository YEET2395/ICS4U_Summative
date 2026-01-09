package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

/**
 * LiTestChaserBot: Chases the nearest Guard or VIP bot.
 * @author Xinran Li
 * @version 2026 01 10
 */
public class LiTestChaserBot extends BaseBot {
    private int[][] botsPositions; // [id][2]
    private int[] botsRoles;       // [id]
    private int myIndex;           // This bot's index in the arrays

    public LiTestChaserBot(City city, int str, int ave, Direction dir, int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
        super(city, str, ave, dir, id, role, hp, movesPerTurn, dodgeDiff);
        super.setColor(Color.MAGENTA);
        super.setLabel("LiChaser " + super.getMyID());
        this.myIndex = id;
    }

    /**
     * Provide all bots' positions and roles for targeting.
     * @param positions array of [y, x] for each bot
     * @param roles array of role for each bot
     */
    public void setBotsInfo(int[][] positions, int[] roles) {
        this.botsPositions = positions;
        this.botsRoles = roles;
    }

    @Override
    public void takeTurn() {
        if (botsPositions == null || botsRoles == null) return;

        int[] myPos = this.getMyPosition();
        int minDist = Integer.MAX_VALUE;
        int[] targetPos = null;

        // Find nearest Guard (role 2) or VIP (role 1)
        for (int i = 0; i < botsPositions.length; i++) {
            if (i == myIndex) continue;
            if (botsRoles[i] == 1 || botsRoles[i] == 2) {
                int dist = Math.abs(myPos[0] - botsPositions[i][0]) + Math.abs(myPos[1] - botsPositions[i][1]);
                if (dist < minDist) {
                    minDist = dist;
                    targetPos = botsPositions[i];
                }
            }
        }

        if (targetPos == null) return; // No target

        // Move toward the target position, up to MOVES_PER_TURN steps
        int moves = this.getMOVES_PER_TURN();
        int[] cur = this.getMyPosition();
        for (int step = 0; step < moves; step++) {
            int dx = targetPos[0] - cur[0];
            int dy = targetPos[1] - cur[1];
            if (dx == 0 && dy == 0) break; // Already at target

            // Prefer horizontal move if not aligned, else vertical
            if (dx != 0) {
                this.turnDirection(dx > 0 ? Direction.SOUTH : Direction.NORTH);
                if (this.frontIsClear()) {
                    this.move();
                    cur = this.getMyPosition();
                    continue;
                }
            }
            if (dy != 0) {
                this.turnDirection(dy > 0 ? Direction.EAST : Direction.WEST);
                if (this.frontIsClear()) {
                    this.move();
                    cur = this.getMyPosition();
                }
            }
        }
    }
}
