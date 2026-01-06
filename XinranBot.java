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
    private int hp;

    private int[] vipPos = {0, 0};
    private int[] chaserPos = {0, 0};

    /**
     * Constructor for XinranBot
     * @param city City the robot is in
     * @param str Street number
     * @param ave Avenue number
     * @param dir direction the robot is facing
     */
    public XinranBot(City city, int str, int ave, Direction dir, int role, int id, int hp) {
        super(city, str, ave, dir, role, id, hp);
        this.hp = hp;
        this.id = id;
        this.role = role;
    }

    /**
     * Application class will send the position of VIP to XinranBot
     * @param coord array of x,y of VIP
     */
    public void getVIPPosition(int[] coord) {
        this.vipPos[0] = coord[0];
        this.vipPos[1] = coord[1];
    }

    /**
     * XinranBot should know the position of the chaser only if the chaser is within 5 intersections to the VIP
     * @param coord array of x,y of chaser
     */
    public void getChaserPosition(int[] coord){
        this.chaserPos[0]=coord[0];
        this.chaserPos[1]=coord[1];
    }


    public void takeTurn()
    {

    }

}
