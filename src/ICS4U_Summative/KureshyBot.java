package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

/**
 * The robot which chases after the VIP
 * @author Aadil Kureshy
 * @version Janurary 6, 2025
 */
public class KureshyBot extends BaseBot{
    private final int MOVES_PER_TURN;
    private int[][] botsPos;
    private int[][] chasersPos;
    private double[][] targetTable;
    private final int TURN_DIST = 0;
    private final int DODGE_EST = 1;
    private final int MAX_MOVE_OBS = 2;
    private final int HP_EST = 3;
    private final int PRESSURE = 4;
    final int PRIORITY_SCORE= 5;
    private int targetID;
    private boolean isCatching;

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
        this.MOVES_PER_TURN = movesPerTurn;
        //for debugging
        super.setColor(Color.RED);
        super.setLabel("Robot " + super.getMyID());
    }

    /**
     * The application class will send the position of all non-chasers
     * @param pos the locations of non-chasers
     */
    public void sendBotsPos(int[][] pos) {
        this.botsPos = pos;
    }

    /**
     * The application class will send the position of all non-chasers
     * @param pos the locations of other chasers
     */
    public void sendChasersPos(int[][] pos) {
        this.chasersPos = pos;
    }

    /**
     * The application class will send the position of all non-chasers
     * @param states
     */
    public void sendStates(boolean[] states) {
        this.playerCaught = states;
    }

    private double calculatePriorityScore() {
        return 0.0;
    }

    private double calculateDistanceScore(int targetDistance, int movesPerTurn) {
        return -(Math.ceil( (double) targetDistance / movesPerTurn));
    }

    private int calculateChaserPressure(int[][] chaserPos) {
        final int AVG_CHASER_MOVEMENT = 8;
        int localChasers = -1; //since it includes this robot

        //iterate through the chaser locations
        for (int i=0; i<chaserPos.length; i++) {

            int distanceToMe = 0;

            //check that the chaser isn't me
            if (chaserPos[i] != this.getMyPosition()) {
                distanceToMe = super.getDistances(chaserPos[i]);
            }

            //check that it is less than the average
            if (distanceToMe<AVG_CHASER_MOVEMENT) {
                localChasers++;
            }
        }
        return localChasers;
    }

    /**
     * The application class will send the results of the tag attempt
     * @param ID the ID of the target
     * @param isSuccess whether the target dodged or not
     */
    public void sendTagResult(int ID, boolean isSuccess) {
        if (isSuccess) {
            targetTable[ID][DODGE_EST] -= 0.1;
            targetTable[ID][HP_EST] -= 1.0;
            //maybe something to ignore if caught here
        } else {
            targetTable[ID][DODGE_EST] += 0.1;
        }
    }

    private void updateTargetTable() {
        //for readability

        //iterate through the first dimension (each is a bot with the same id from the array)
        for (int i=0; i<targetTable.length; i++) {
            targetTable[i][TURN_DIST] = this.calculateDistanceScore(super.getDistances(botsPos[i]), this.MOVES_PER_TURN);
            targetTable[i][PRESSURE] = this.calculateChaserPressure(this.chasersPos);
        }
    }

    public void takeTurn() {

    }
}
