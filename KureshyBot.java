package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

/**
 * The robot which chases after the VIP
 * @author Aadil Kureshy
 * @version Janurary 6, 2025
 */
public class KureshyBot extends BaseBot{

    /**
     * Constructor for Chaser
     * @param city City the robot is in
     * @param str the Street of the robot (y)
     * @param ave the Avenue of the robot (x)
     * @param dir the Direction the robot is facing
     * @param id the robot's numerical ID
     * @param role the robot's role (Chaser)
     * @param hp the amount of health
     * @param movesPerTurn the max amount of moves the robot can make
     * @param dodgeDiff the dodging/catching capability of the robot
     */
    public KureshyBot(City city, int str, int ave, Direction dir, int id, int role, int hp, int movesPerTurn, int dodgeDiff) {
        super(city, str, ave, dir, id, role, hp, movesPerTurn, dodgeDiff);

        //for debugging
        super.setColor(Color.RED);
        super.setLabel("Robot " + super.getMyID());
    }



    public void takeTurn() {}
}
