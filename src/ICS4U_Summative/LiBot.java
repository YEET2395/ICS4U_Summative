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
        for(int i = 0; i < records.length; i++)
        {
            this.otherRecords[i] = records[i];
        }
    }

    public void initRecords(PlayerInfo[] records) {
        System.out.println("Initializing records");
    }

    public void takeTurn()
    {
        if(otherRecords.length == 0)
        {
            return;
        }

        int[] myPos = getMyPosition();
        int hp = myRecords.getHP();

        PlayerInfo vip = findFirstByRole(otherRecords, 1);
        PlayerInfo chaser = findNearestByRole(otherRecords, 3, myPos);

        if (chaser == null || vip == null)
        {
            System.out.println("LiBot: vip or chaser missing");
            return; // No chaser or VIP found
        }

        int distCV = distance(chaser.getPosition(), vip.getPosition()); // dist(chaser, vip)
        int distGV = distance(myPos, vip.getPosition()); // dist(guard, vip)
        int distGC = distance(myPos, chaser.getPosition()); // dist(guard, chaser)

        double cSpeed = 0;
        double protect =
                60
                        + 12 * Math.max(0, DANGER_RADIUS - distCV)
                        -  8 * Math.max(0, distGV - ESCORT_DISTANCE)
                        - 10 * (hp == 1 ? 1 : 0);

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
        if(nextAct == 0)
        {
            //protect
        }
        else if(nextAct == 1)
        {
            //attack
        }
        else
        {
            //run
        }

        
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
     * @return
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
