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
     * @param city         City the robot is in
     * @param str          Street number
     * @param ave          Avenue number
     * @param dir          direction the robot is facing
     * @param id           the robot's numerical id
     * @param role         the robot's role
     * @param hp           health points
     * @param movesPerTurn moves per turn
     * @param dodgeDiff    dodging difficulty (double)
     */
    public LiBot(City city, int str, int ave, Direction dir, int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
        super(city, str, ave, dir, id, role, hp, movesPerTurn, dodgeDiff);

        //for debugging
        super.setColor(Color.BLUE);
        super.setLabel("Guard " + this.myRecords.getID());
    }

    /**
     * Updates this bot's enemy records
     *
     * @param records the records to get information from
     */
    public void updateOtherRecords(PlayerInfo[] records) {
        this.otherRecords = records;
    }

    /**
     * Initializes this bot's enemy records with only the info they are supposed to know
     *
     * @param records the records to get information from
     */
    public void initRecords(PlayerInfo[] records) {
        System.out.println("Initializing records");
    }

    /**
     * The main logic for the bot's turn
     */
    public void takeTurn()
    {
        // Validate records
        if (otherRecords == null || otherRecords.length == 0) {
            return;
        }

        // Main logic
        int[] myPos;
        int hp = myRecords.getHP();

        // Collect VIPs and Chasers
        PlayerInfo[] vips = new PlayerInfo[2];
        PlayerInfo[] chasers = new PlayerInfo[2];

        // Collect by role
        int vipCount = collectByRole(otherRecords, ROLE_VIP, vips);
        int chaserCount = collectByRole(otherRecords, ROLE_CHASER, chasers);

        // Validate presence of VIPs and Chasers
        if (vipCount == 0 || chaserCount == 0) {
            System.out.println("LiBot: vip or chaser missing");
            return;
        }

        // Pick the most threatened VIP
        PlayerInfo vip = pickMostThreatenedVIP(vips, vipCount, chasers, chaserCount);
        if (vip == null) {
            System.out.println("LiBot: vip or chaser missing after pickMostThreatenedVIP");
            return;
        }

        // Pick the nearest chaser to that VIP
        PlayerInfo chaser = pickNearestToPos(vip.getPosition(), chasers, chaserCount);
        if (chaser == null) {
            System.out.println("LiBot: vip or chaser missing after pick");
            return;
        }

        // Calculate distances
        myPos = getMyPosition();
        int distCV = distance(chaser.getPosition(), vip.getPosition()); // dist(chaser, vip)
        int distGV = distance(myPos, vip.getPosition()); // dist(guard, vip)
        int distGC = distance(myPos, chaser.getPosition()); // dist(guard, chaser)

        double cSpeed = 0;

        // Chose action based on scoring
        double protect =
                60
                        + 12 * Math.max(0, DANGER_RADIUS - distCV)
                        - 8 * Math.max(0, distGV - ESCORT_DISTANCE)
                        - 10 * (vip.getHP() == 1 ? 1 : 0);

        double attack =
                20
                        + 10 * Math.max(0, 3 - distGC)
                        + 4 * cSpeed
                        - 25 * (hp <= 2 ? 1 : 0)
                        - 20 * (distGC <= 1 ? 1 : 0);

        double run =
                10
                        + 25 * (hp <= 2 ? 1 : 0)
                        + 15 * (distGC <= 1 ? 1 : 0)
                        - 10 * Math.max(0, 3 - distCV);

        // Choose action with highest score
        int nextAct = insertionSortDescending(new double[]{protect, attack, run}, new int[]{0, 1, 2});

        // Enforce leash for attack
        int distToVipNow = distance(getMyPosition(), vip.getPosition());
        if (nextAct == 1 && distToVipNow > ATTACK_LEASH) {
            nextAct = 0;
        }
        // Execute chosen action
        if (nextAct == 0) {
            doProtect(vip, chaser, vips, vipCount, chasers, chaserCount);
        } else if (nextAct == 1) {
            doAttack(chaser);
        } else {
            doRun(chaser);
        }

    }

    /**
     * Collect PlayerInfo records by role
     *
     * @param records the array of PlayerInfo records
     * @param role    the role to filter by
     * @param out     the output array to store collected records
     * @return the number of records collected
     */
    private int collectByRole(PlayerInfo[] records, int role, PlayerInfo[] out) {
        // Reset output array
        int k = 0;
        for (int i = 0; i < records.length; i++) {
            PlayerInfo r = records[i];
            if (r == null) {
                continue;
            }
            if (r.getState()) {
                continue;
            }
            if (r.getRole() != role) {
                continue;
            }

            if (k < out.length) {
                out[k] = r;
                k++;
            }
        }
        return k;
    }

    /**
     * Pick the most threatened VIP
     *
     * @param vips        array of VIP PlayerInfo
     * @param vipCount    number of VIPs in the array
     * @param chasers     array of Chaser PlayerInfo
     * @param chaserCount number of Chasers in the array
     * @return the most threatened VIP PlayerInfo
     */
    private PlayerInfo pickMostThreatenedVIP(PlayerInfo[] vips, int vipCount,
                                             PlayerInfo[] chasers, int chaserCount)
    {
        // Find the VIP with the smallest distance to any chaser
        PlayerInfo bestVIP = null;
        int bestThreat = Integer.MAX_VALUE;

        // Evaluate each VIP
        for (int i = 0; i < vipCount; i++) {
            PlayerInfo v = vips[i];
            if (v == null) continue;

            // Find the closest chaser to this VIP
            int threat = Integer.MAX_VALUE;
            for (int j = 0; j < chaserCount; j++) {
                PlayerInfo c = chasers[j];
                if (c == null) continue;
                int d = distance(v.getPosition(), c.getPosition());
                if (d < threat) threat = d;
            }

            // Update best VIP based on threat level
            if (threat < bestThreat) {
                bestThreat = threat;
                bestVIP = v;
            } else if (threat == bestThreat && bestVIP != null) {
                if (v.getHP() < bestVIP.getHP()) bestVIP = v;
            }
        }
        return bestVIP;
    }

    /**
     * Pick the nearest PlayerInfo to a given position
     *
     * @param pos   the reference position
     * @param arr   array of PlayerInfo
     * @param count number of PlayerInfo in the array
     * @return the nearest PlayerInfo to the given position
     */
    private PlayerInfo pickNearestToPos(int[] pos, PlayerInfo[] arr, int count)
    {
        // Find the nearest PlayerInfo to the given position
        PlayerInfo best = null;
        int bestDist = Integer.MAX_VALUE;

        // Evaluate each PlayerInfo
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

    /**
     * Protect the VIP from the threat chaser
     * @param vip the VIP to protect
     * @param threatChaser the chaser to protect against
     * @param vips array of VIP PlayerInfo
     * @param vipCount number of VIPs
     * @param chasers array of Chaser PlayerInfo
     * @param chaserCount number of Chasers
     */
    private void doProtect(PlayerInfo vip, PlayerInfo threatChaser,
                           PlayerInfo[] vips, int vipCount,
                           PlayerInfo[] chasers, int chaserCount)
    {
        // Positions
        int[] vipPos = vip.getPosition();
        int[] chPos = threatChaser.getPosition();

        // Number of moves
        int moves = getMOVES_PER_TURN();

        // Main loop
        for (int step = 0; step < moves; step++) {
            int[] myPos = getMyPosition();

            if (distance(myPos, vipPos) > ESCORT_DISTANCE + 1) {
                if (!moveTowardPos(vipPos)) break;
                continue;
            }

            int distCV = distance(chPos, vipPos);

            // If chaser is very close to VIP, try to block
            if (distCV <= 1) {
                Direction bestDir = null;
                int bestScore = Integer.MIN_VALUE;

                // Evaluate all directions
                Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                for(int i = 0; i < dirs.length; i++)
                {
                    Direction d = dirs[i];
                    if (!canMove(d))
                    {
                        continue;
                    }
                    int[] next = nextPos(myPos, d);

                    if (distance(next, vipPos) > ESCORT_DISTANCE + 1)
                    {
                        continue;
                    }
                    int distNextToChaser = distance(next, chPos);
                    int distNextToVip = distance(next, vipPos);
                    int score = 10 * distNextToChaser - 8 * distNextToVip;

                    // Update the best direction based on score
                    if (score > bestScore)
                    {
                        bestScore = score;
                        bestDir = d;
                    }
                }

                // Try to move in the best blocking direction
                if (bestDir == null || !this.tryMove(bestDir))
                {
                    break;
                }
                continue;
            }

            // Move to block position between VIP and chaser
            int[] blockPos = computeBlockPos(vipPos, chPos);
            if (distance(myPos, blockPos) == 0)
            {
                break;
            }
            if (!this.moveTowardPos(blockPos))
            {
                break;
            }
        }
    }

    /**
     * Attack the threat chaser
     * @param threatChaser the chaser to attack
     */
    private void doAttack(PlayerInfo threatChaser)
    {
        int moves = getMOVES_PER_TURN();

        // Main loop
        for (int step = 0; step < moves; step++)
        {
            int[] myPos = getMyPosition();

            // Check for nearest VIP
            PlayerInfo nearestVip = null;
            int bestVipDist = Integer.MAX_VALUE;
            for(int i = 0; i < otherRecords.length; i++)
            {
                // Find nearest VIP
                PlayerInfo r = otherRecords[i];
                if (r == null)
                {
                    continue;
                }
                if (r.getState())
                {
                    continue;
                }
                if (r.getRole() != ROLE_VIP)
                {
                    continue;
                }

                // Calculate distance to VIP
                int d = distance(myPos, r.getPosition());
                if (d < bestVipDist)
                {
                    bestVipDist = d;
                    nearestVip = r;
                }
            }
            // Enforce leash distance
            if (nearestVip != null && bestVipDist > ATTACK_LEASH)
            {
                break;
            }
            // Move toward the threat chaser
            int[] targetPos = threatChaser.getPosition();
            if (distance(myPos, targetPos) <= 1)
            {
                break;
            }
            if (!moveTowardPos(targetPos))
            {
                break;
            }
        }
    }

    /**
     * Run away from the threat chaser
     * @param threatChaser the chaser to run from
     */
    private void doRun(PlayerInfo threatChaser)
    {
        // Get chaser position
        int[] chPos = threatChaser.getPosition();
        int moves = getMOVES_PER_TURN();

        // Main loop
        for (int step = 0; step < moves; step++)
        {
            int[] myPos = getMyPosition();

            Direction bestDir = null;
            int bestDist = distance(myPos, chPos);

            // Evaluate all directions to maximize distance from chaser
            Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            for (int i = 0; i < dirs.length; i++) {
                // Check each direction
                Direction d = dirs[i];
                if (!canMove(d))
                {
                    continue;
                }
                int[] next = nextPos(myPos, d);
                int d2 = distance(next, chPos);
                if (d2 > bestDist)
                {
                    bestDist = d2;
                    bestDir = d;
                }
            }

            // If no best direction found, pick any valid direction
            if (bestDir == null)
            {
                for(int i = 0; i < dirs.length; i++)
                {
                    Direction d = dirs[i];
                    if (canMove(d))
                    {
                        bestDir = d;
                        break;
                    }
                }
            }
            //  Try to move away from chaser
            boolean moved = false;

            // Try to move in the best direction
            if (bestDir != null)
            {
                moved = tryMove(bestDir);
            }

            if (!moved)
            {
                int curDist = distance(getMyPosition(), chPos);
                // Try any direction that doesn't decrease distance from chaser
                for (int i = 0; i < dirs.length; i++)
                {
                    Direction d = dirs[i];
                    // Skip already tried direction
                    if (!canMove(d))
                    {
                        continue;
                    }
                    int[] next = nextPos(getMyPosition(), d);
                    int d2 = distance(next, chPos);
                    // Only try if distance doesn't decrease
                    if (d2 >= curDist)
                    {
                        moved = tryMove(d);
                        // Break if moved
                        if (moved)
                        {
                            break;
                        }
                    }
                }
            }
            if (!moved)
            {
                break;
            }
        }
    }

    /**
     * Move toward a target position
     * @param targetPos the target position to move toward
     * @return true if moved, false otherwise
     */
    private boolean moveTowardPos(int[] targetPos)
    {
        // Get current position
        int[] myPos = getMyPosition();
        int dx = targetPos[0] - myPos[0]; // x=avenue
        int dy = targetPos[1] - myPos[1]; // y=street

        // Determine primary and secondary directions
        Direction primary = null;
        Direction secondary = null;

        // Prioritize axis with greater distance
        if (Math.abs(dx) >= Math.abs(dy))
        {
            // X axis prioritized
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
            // Y axis prioritized
            if (dy != 0)
            {
                primary = (dy > 0 ? Direction.SOUTH : Direction.NORTH);
            }
            if (dx != 0)
            {
                secondary = (dx > 0 ? Direction.EAST : Direction.WEST);
            }
        }

        // Try to move in primary direction
        if (primary != null && tryMove(primary))
        {
            return true;
        }

        // Try to move in secondary direction
        if (secondary != null && tryMove(secondary))
        {
            return true;
        }

        // Try other directions
        int curDist = distance(myPos, targetPos);

        // Evaluate all other directions
        Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int i = 0; i < dirs.length; i++)
        {
            Direction d = dirs[i];
            if (d == primary || d == secondary)
            {
                continue;
            }
            if (!canMove(d))
            {
                continue;
            }

            int[] next = nextPos(myPos, d);
            if (distance(next, targetPos) <= curDist)
            {
                return tryMove(d);
            }
        }
        return false;
    }

    /**
     * Compute blocking position between VIP and chaser
     * @param vipPos ViP position
     * @param chPos  Chaser position
     * @return the blocking position
     */
    private int[] computeBlockPos(int[] vipPos, int[] chPos)
    {
        // Compute blocking position between VIP and chaser
        int dx = chPos[0] - vipPos[0];
        int dy = chPos[1] - vipPos[1];

        // Start at VIP position
        int bx = vipPos[0];
        int by = vipPos[1];

        // Move one step toward chaser along dominant axis
        if (Math.abs(dx) >= Math.abs(dy))
        {
            // Move along X axis
            if (dx != 0)
            {
                bx += (dx > 0 ? 1 : -1);
            }
        }
        else
        {
            // Move along Y axis
            if (dy != 0)
            {
                by += (dy > 0 ? 1 : -1);
            }
        }
        return new int[]{bx, by};
    }

    /**
     * Check if the bot can move in the specified direction
     *
     * @param d the direction to check
     * @return true if the bot can move, false otherwise
     */
    private boolean canMove(Direction d)
    {
        turnDirection(d);
        return frontIsClear();
    }

    /**
     * Try to move in the specified direction
     * @param d the direction to move
     * @return true if moved, false otherwise
     */
    private boolean tryMove(Direction d)
    {
        turnDirection(d);
        if (frontIsClear())
        {
            move();
            return true;
        }
        return false;
    }

    /**
     * Get the next position if moving in the specified direction
     *
     * @param cur current position
     * @param d   direction to move
     * @return the next position
     */
    private int[] nextPos(int[] cur, Direction d)
    {
        int x = cur[0], y = cur[1];
        // Calculate next position based on direction
        if (d == Direction.EAST)
        {
            x++;
        }
        else if (d == Direction.WEST)
        {
            x--;
        }
        else if (d == Direction.SOUTH)
        {
            y++;
        }
        else if (d == Direction.NORTH)
        {
            y--;
        }
        return new int[]{x, y};
    }


    /**
     * Calculate Manhattan distance between two points
     * @param a first point
     * @param b second point
     * @return the Manhattan distance
     */
    private int distance(int[] a, int[] b)
    {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    /**
     * Insertion Sort (descending).
     * @param scores  an array of scores
     * @param actions an array of actions
     */
    public static int insertionSortDescending(double[] scores, int[] actions)
    {
        // Insertion sort algorithm to sort scores in descending order
        for(int i = 1; i < scores.length; i++)
        {
            double keyScore = scores[i];
            int keyAction = actions[i];
            int j = i - 1;

            // Move elements of scores 0..i-1, that are less than keyScore
            while (j >= 0 && scores[j] < keyScore)
            {
                scores[j + 1] = scores[j];
                actions[j + 1] = actions[j];
                j--;
            }
            // Place keyScore and keyAction at the correct position
            scores[j + 1] = keyScore;
            actions[j + 1] = keyAction;
        }
        return actions[0];
    }
}

