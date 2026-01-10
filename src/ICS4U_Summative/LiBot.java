package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

/**
 * Xinran's guarding robot
 * @author Xinran Li
 * @version 2025 12 30
 */
public class LiBot extends BaseBot {
    private int[][] vipPos;
    private int[][] chaserPos;

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
        super.setLabel("Robot " + super.myRecords.getID());
    }

    public void updateEnemyRecords(PlayerInfo[] records)
    {
        System.out.println("Updating enemy records for LiBot ");
    }


    public void takeTurn()
    {

    }

}
