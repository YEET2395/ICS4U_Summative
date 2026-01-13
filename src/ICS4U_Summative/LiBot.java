package ICS4U_Summative;

import becker.robots.*;
import java.awt.*;

/**
 * Xinran's guarding robot
 * @author Xinran Li
 * @version 2025 12 30
 */
public class LiBot extends BaseBot {

    // Role constants
    private static final int ROLE_VIP = 1;
    private static final int ROLE_GUARD = 2;
    private static final int ROLE_CHASER = 3;
    private static final int DANGER_RADIUS = 5;
    private static final int ESCORT_DISTANCE = 2;
    private static final int ATTACK_LEASH = ESCORT_DISTANCE + 2; // attack leash radius

    /**
     * Constructor for XinranBot
     * @param city City the robot is in
     * @param str Street number
     * @param ave Avenue number
     * @param dir direction the robot is facing
     * @param id the robot's numerical id
     * @param role the robot's role
     * @param hp health points
     * @param movesPerTurn moves per turn
     * @param dodgeDiff dodging difficulty (double)
     */
    public LiBot(City city, int str, int ave, Direction dir, int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
        super(city, str, ave, dir, id, role, hp, movesPerTurn, dodgeDiff);

        //for debugging
        super.setColor(Color.BLUE);
        super.setLabel("Guard " + this.myRecords.getID());
    }

    public void updateOtherRecords(PlayerInfo[] records)
    {
        this.otherRecords = records;
    }

    public void initRecords(PlayerInfo[] records) {
        System.out.println("Initializing records");
    }

    public void takeTurn()
    {
        if (otherRecords == null || otherRecords.length == 0)
        {
            return;
        }

        int[] myPos;
        int hp = myRecords.getHP();

        PlayerInfo[] vips = new PlayerInfo[2];
        PlayerInfo[] chasers = new PlayerInfo[2];

        int vipCount = collectByRole(otherRecords, ROLE_VIP, vips);
        int chaserCount = collectByRole(otherRecords, ROLE_CHASER, chasers);

        if (vipCount == 0 || chaserCount == 0) {
            System.out.println("LiBot: vip or chaser missing");
            return;
        }

        PlayerInfo vip = pickMostThreatenedVIP(vips, vipCount, chasers, chaserCount);
        if (vip == null)
        {
            System.out.println("LiBot: vip or chaser missing after pickMostThreatenedVIP");
            return;
        }

        PlayerInfo chaser = pickNearestToPos(vip.getPosition(), chasers, chaserCount);
        if (chaser == null) {
            System.out.println("LiBot: vip or chaser missing after pick");
            return;
        }

        myPos = getMyPosition();
        int distCV = distance(chaser.getPosition(), vip.getPosition()); // dist(chaser, vip)
        int distGV = distance(myPos, vip.getPosition()); // dist(guard, vip)
        int distGC = distance(myPos, chaser.getPosition()); // dist(guard, chaser)

        double cSpeed = 0;
        double protect =
                60
                        + 12 * Math.max(0, DANGER_RADIUS - distCV)
                        -  8 * Math.max(0, distGV - ESCORT_DISTANCE)
                        - 10 * (vip.getHP() == 1 ? 1 : 0);

        double attack =
                20
                        + 10 * Math.max(0, 3 - distGC)
                        +  4 * cSpeed
                        - 25 * (hp <= 2 ? 1 : 0)
                        - 20 * (distGC <= 1 ? 1 : 0);

        double run =
                10
                        + 25 * (hp <= 2 ? 1 : 0)
                        + 15 * (distGC <= 1 ? 1 : 0)
                        - 10 * Math.max(0, 3 - distCV);

        int nextAct = insertionSortDescending(new double[] {protect, attack, run}, new int[] {0, 1, 2});

        int distToVipNow = distance(getMyPosition(), vip.getPosition());
        if (nextAct == 1 && distToVipNow > ATTACK_LEASH) {
            nextAct = 0;
        }
        if (nextAct == 0)
        {
            doProtect(vip, chaser, vips, vipCount, chasers, chaserCount);
        }
        else if(nextAct == 1)
        {
            doAttack(chaser);
        }
        else
        {
            doRun(chaser);
        }

    }

    private int collectByRole(PlayerInfo[] records, int role, PlayerInfo[] out)
    {
        int k = 0;
        for (PlayerInfo r : records)
        {
            if (r == null) continue;
            if (r.getState()) continue;
            if (r.getRole() != role) continue;

            if (k < out.length)
            {
                out[k] = r;
                k++;
            }
        }
        return k;
    }

    private PlayerInfo pickMostThreatenedVIP(PlayerInfo[] vips, int vipCount,
                                             PlayerInfo[] chasers, int chaserCount)
    {
        PlayerInfo bestVIP = null;
        int bestThreat = Integer.MAX_VALUE;

        for (int i = 0; i < vipCount; i++)
        {
            PlayerInfo v = vips[i];
            if (v == null) continue;

            int threat = Integer.MAX_VALUE;
            for (int j = 0; j < chaserCount; j++)
            {
                PlayerInfo c = chasers[j];
                if (c == null) continue;
                int d = distance(v.getPosition(), c.getPosition());
                if (d < threat) threat = d;
            }

            if (threat < bestThreat)
            {
                bestThreat = threat;
                bestVIP = v;
            } else if (threat == bestThreat && bestVIP != null)
            {
                if (v.getHP() < bestVIP.getHP()) bestVIP = v;
            }
        }
        return bestVIP;
    }

    private PlayerInfo pickNearestToPos(int[] pos, PlayerInfo[] arr, int count)
    {
        PlayerInfo best = null;
        int bestDist = Integer.MAX_VALUE;

        for (int i = 0; i < count; i++)
        {
            PlayerInfo r = arr[i];
            if (r == null) continue;
            int d = distance(pos, r.getPosition());
            if (d < bestDist)
            {
                bestDist = d;
                best = r;
            }
        }
        return best;
    }


    private void doProtect(PlayerInfo vip, PlayerInfo threatChaser,
                           PlayerInfo[] vips, int vipCount,
                           PlayerInfo[] chasers, int chaserCount)
    {
        int[] vipPos = vip.getPosition();
        int[] chPos  = threatChaser.getPosition();

        int moves = getMOVES_PER_TURN();

        for (int step = 0; step < moves; step++)
        {
            int[] myPos = getMyPosition();

            if (distance(myPos, vipPos) > ESCORT_DISTANCE + 1)
            {
                if (!moveTowardPos(vipPos)) break;
                continue;
            }

            int distCV = distance(chPos, vipPos);

            if (distCV <= 1)
            {
                Direction bestDir = null;
                int bestScore = Integer.MIN_VALUE;

                for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    if (!canMove(d)) continue;

                    int[] next = nextPos(myPos, d);

                    if (distance(next, vipPos) > ESCORT_DISTANCE + 1) continue;

                    int distNextToChaser = distance(next, chPos);
                    int distNextToVip    = distance(next, vipPos);

                    int score = 10 * distNextToChaser - 8 * distNextToVip;

                    if (score > bestScore)
                    {
                        bestScore = score;
                        bestDir = d;
                    }
                }

                if (bestDir == null || !tryMove(bestDir)) break;
                continue;
            }

            int[] blockPos = computeBlockPos(vipPos, chPos);

            if (distance(myPos, blockPos) == 0) break;

            if (!moveTowardPos(blockPos)) break;
        }
    }


    private void doAttack(PlayerInfo threatChaser)
    {
        int[] targetPos = threatChaser.getPosition();
        int moves = getMOVES_PER_TURN();

        for (int step = 0; step < moves; step++) {
            int[] myPos = getMyPosition();

            if (distance(myPos, targetPos) <= 1) {
                break;
            }
            if (!moveTowardPos(targetPos)) break;
        }
    }

    private void doRun(PlayerInfo threatChaser)
    {
        int[] chPos = threatChaser.getPosition();
        int moves = getMOVES_PER_TURN();

        for (int step = 0; step < moves; step++) {
            int[] myPos = getMyPosition();

            Direction bestDir = null;
            int bestDist = distance(myPos, chPos);

            for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                if (!canMove(d)) continue;
                int[] next = nextPos(myPos, d);
                int d2 = distance(next, chPos);
                if (d2 > bestDist) {
                    bestDist = d2;
                    bestDir = d;
                }
            }

            if (bestDir == null) {
                for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    if (canMove(d)) { bestDir = d; break; }
                }
            }

            boolean moved = false;

            if (bestDir != null) moved = tryMove(bestDir);

            if (!moved)
            {
                int curDist = distance(getMyPosition(), chPos);

                for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST})
                {
                    if (!canMove(d)) continue;
                    int[] next = nextPos(getMyPosition(), d);
                    int d2 = distance(next, chPos);
                    if (d2 >= curDist)
                    {
                        moved = tryMove(d);
                        if (moved) break;
                    }
                }
            }

            if (!moved) break;

        }
    }


    private boolean moveTowardPos(int[] targetPos) {
        int[] myPos = getMyPosition();
        int dx = targetPos[0] - myPos[0]; // x=avenue
        int dy = targetPos[1] - myPos[1]; // y=street

        Direction primary = null;
        Direction secondary = null;

        if (Math.abs(dx) >= Math.abs(dy))
        {
            if (dx != 0)
            {
                primary = (dx > 0 ? Direction.EAST : Direction.WEST);
            }
            if (dy != 0)
            {
                secondary = (dy > 0 ? Direction.SOUTH : Direction.NORTH);
            }
        }
        else
        {
            if (dy != 0)
            {
                primary = (dy > 0 ? Direction.SOUTH : Direction.NORTH);
            }
            if (dx != 0)
            {
                secondary = (dx > 0 ? Direction.EAST : Direction.WEST);
            }
        }

        if (primary != null && tryMove(primary))
        {
            return true;
        }

        if (secondary != null && tryMove(secondary))
        {
            return true;
        }

        int curDist = distance(myPos, targetPos);

        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST})
        {
            if (d == primary || d == secondary) continue;
            if (!canMove(d)) continue;

            int[] next = nextPos(myPos, d);
            if (distance(next, targetPos) <= curDist)
            {
                return tryMove(d);
            }
        }
        return false;
    }

    private int[] computeBlockPos(int[] vipPos, int[] chPos) {
        int dx = chPos[0] - vipPos[0];
        int dy = chPos[1] - vipPos[1];

        int bx = vipPos[0];
        int by = vipPos[1];

        if (Math.abs(dx) >= Math.abs(dy))
        {
            if (dx != 0) bx += (dx > 0 ? 1 : -1);
        }
        else
        {
            if (dy != 0) by += (dy > 0 ? 1 : -1);
        }

        return new int[]{bx, by};
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
        if (d == Direction.EAST) x++;
        else if (d == Direction.WEST) x--;
        else if (d == Direction.SOUTH) y++;
        else if (d == Direction.NORTH) y--;
        return new int[]{x, y};
    }


    /**
     * Calculate Manhattan distance between two points
     * @param a first point
     * @param b second point
     * @return the Manhattan distance
     */
    private int distance(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    /**
     * Insertion Sort (descending).
     * @param scores an array of scores
     * @param actions an array of actions
     */
    public static int insertionSortDescending(double[] scores, int[] actions) {
        for (int i = 1; i < scores.length; i++) {
            double keyScore = scores[i];
            int keyAction = actions[i];
            int j = i - 1;

            while (j >= 0 && scores[j] < keyScore) {
                scores[j + 1] = scores[j];
                actions[j + 1] = actions[j];
                j--;
            }
            scores[j + 1] = keyScore;
            actions[j + 1] = keyAction;
        }
        return actions[0]; // Return the action with the highest score
    }

    /**
     * Find the first record with the specified role
     * @param records the array of records to search
     * @param r the role to search for
     * @return the first PlayerInfo with the specified role, or null if none found
     */
    private PlayerInfo findFirstByRole(PlayerInfo[] records, int r)
    {
        for(PlayerInfo record : records)
        {
            if (record == null) continue;
            if (record.getState()) continue;
            if(record.getRole() == r)
            {
                return record;
            }
        }
        return null;
    }

    /**
     * Find the nearest record with the specified role
     * @param records Record array
     * @param role Role to search for
     * @param fromPos Position to measure distance from
     * @return The nearest PlayerInfo with the specified role, or null if none found
     */
    private PlayerInfo findNearestByRole(PlayerInfo[] records, int role, int[] fromPos) {
        PlayerInfo best = null;
        int bestDist = Integer.MAX_VALUE;

        for (PlayerInfo record : records) {
            if (record == null) continue;
            if (record.getState()) continue;
            if (record.getRole() != role) continue;

            int d = Math.abs(fromPos[0] - record.getPosition()[0])
                    + Math.abs(fromPos[1] - record.getPosition()[1]);

            if (d < bestDist) {
                bestDist = d;
                best = record;
            }
        }
        return best;
    }
}
