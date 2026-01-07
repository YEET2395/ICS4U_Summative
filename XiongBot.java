package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

public class XiongBot extends BaseBot{
    private int[] guardPos = {0, 0};
    private int[] chaserPos = {0, 0};

    /**
     * Constructor for BaseBot
     *
     * @param city City the robot is in
     * @param str  Street number
     * @param ave  Avenue number
     * @param dir  direction the robot is facing
     * @param role role of the bot
     * @param id   identifier of the bot
     * @param hp   health points of the bot
     */
    public XiongBot(City city, int str, int ave, Direction dir, int role, int id, int hp, int movesPerTurn, int dodgeDiff) {
        super(city, str, ave, dir, role, id, hp, movesPerTurn, dodgeDiff);

        //for debugging
        super.setColor(Color.GREEN);
        super.setLabel("Robot " + super.getMyID());
    }

    public void getGuardPosition(int[] coord){
        guardPos[0]=coord[0];
        guardPos[1]=coord[1];
    }

    public void getChaserPosition(int[] coord){
        chaserPos[0]=coord[0];
        chaserPos[1]=coord[1];
    }

    public void takeTurn(){

    }
}
