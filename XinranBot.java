package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

/**
 * Xinran's guarding robot
 * @author Xinran Li
 * @version 2025 12 30
 */
public class XinranBot extends BaseBot {
    private int[] vipPos = {0, 0};
    private int[] chaserPos = {0, 0};

    /**
     * Constructor for XinranBot
     * @param city City the robot is in
     * @param str Street number
     * @param ave Avenue number
     * @param dir direction the robot is facing
     * @param id the robot's numerical id
     * @param role the robot's role
     */
    public XinranBot(City city, int str, int ave, Direction dir, int id, int role, int hp, int movesPerTurn, int dodgeDiff) {
        super(city, str, ave, dir, role, id, hp, movesPerTurn, dodgeDiff);

        //for debugging
        super.setColor(Color.BLUE);
        super.setLabel("Robot " + super.getMyID());
    }

    /**
     * Application class will send the position of VIP to XinranBot
     * @param pos array containing street and avenue number of VIP
     */
    public void sendVIPPosition(int[] pos)
    {
        this.vipPos[0] = pos[0];
        this.vipPos[1] = pos[1];
    }

    /**
     * XinranBot should know the position of the chaser only if the chaser is within 5 intersections to the VIP
     * @param pos array containing street and avenue number of chaser
     */
    public void sendChaserPosition(int[] pos)
    {
        chaserPos[0] = pos[0];
        chaserPos[1] = pos[1];
    }


    public void takeTurn()
    {

    }

}
