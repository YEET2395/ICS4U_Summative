package ICS4U_Summative;

import becker.robots.*;

/**
 * Xinran's guarding robot
 * @author Xinran Li
 * @version 2025 12 30
 */
public class XinranBot extends BaseBot {

    public final int role; // GUARD, VIP, CHASER
    public final int id;
    public final int hp;

    private int[] vipPos = {0, 0};
    private int[] chaserPos = {0, 0};

    /**
     * Constructor for XinranBot
     * @param city City the robot is in
     * @param str Street number
     * @param ave Avenue number
     * @param dir direction the robot is facing
     */
    public XinranBot(City city, int str, int ave, Direction dir, int id, int hp, int role) {
        super(city, str, ave, dir, role, id, hp);
        this.hp = hp;
        this.id = id;
        this.role = role;
    }

    /**
     * Application class will send the position of VIP to XinranBot
     * @param str street number of VIP
     * @param ave Avenue number of VIP
     */
    public void getVIPPosition(int str, int ave)
    {
        this.vipPos[0] = str;
        this.vipPos[1] = ave;
    }

    /**
     * XinranBot should know the position of the chaser only if the chaser is within 5 intersections to the VIP
     * @param str street number of chaser
     * @param ave avenue number of chaser
     */
    public void getChaserPosition(int str, int ave)
    {
        chaserPos[0] = str;
        chaserPos[1] = ave;
    }


    public void takeTurn()
    {

    }

}
