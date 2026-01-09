package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

/**
 * The robot which chases after the VIP
 * @author Aadil Kureshy
 * @version Janurary 6, 2025
 */
public class KureshyBot extends BaseBot{
    private int[][] botsPos;
    private int[][] chasersPos;
    private double[][] targetTable;
    private int [][] prevPos;
    private double[] priorityScore;
    private int[] targetIndex;
    private final int TURN_DIST = 0; //the first index for each bot in the target table
    private final int DODGE_EST = 1; //the second index for each bot in the target table
    private final int MAX_MOVE_OBS = 2; //the third index for each bot in the target table
    private final int HP_EST = 3; //the fourth index for each bot in the target table
    private final int PRESSURE = 4; //the fifth index for each bot in the target table
    private final int NUM_CATCHES = 5; //the sixth index for each bot in the target table
    private int targetID; //the current target
    private boolean isCatching; //to be used by application to know when to check dodge

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
    public KureshyBot(City city, int str, int ave, Direction dir, int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
        super(city, str, ave, dir, id, role, hp, movesPerTurn, dodgeDiff);
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
        this.robotCaught = states;
    }

    /**
     * Calculates the priority scores using data from the targetTable where lower is a better target
     */
    private void calculatePriorityScores() {

        for (int i=0; i<this.targetTable.length; i++) {
            double rolePrediction = this.calculateRolePrediction(this.targetTable[i][MAX_MOVE_OBS], this.targetTable[i][NUM_CATCHES]);

            //calculate a priority score where a lower value is more likely to be a VIP
            this.priorityScore[i] =
                    this.targetTable[i][TURN_DIST] +
                    (0.3 * this.targetTable[i][DODGE_EST]) +
                    (0.5 * -this.targetTable[i][HP_EST]/5.0) +
                    (0.3 * this.targetTable[i][PRESSURE]) +
                    (0.3 * rolePrediction);

            //Completely deprioritize those already caught
            if (this.robotCaught[i]) {
                this.priorityScore[i] = 1000; //don't consider any robot which is already caught
            }
        }

    }

    /**
     * Calculates how many turns it would take to reach the target
     * @param targetDistance the distance to the target
     * @param movesPerTurn how many moves the chaser can make per turn
     * @return the distance score
     */
    private double calculateDistanceScore(int targetDistance, int movesPerTurn) {
        int turnCount = targetDistance/movesPerTurn;
        if (targetDistance % movesPerTurn != 0){
            turnCount++;
        }

        return (double) -turnCount;
    }

    /**
     * Observes the maximum amount of movment observed by the chaser of each non-chaser
     * robot to use in predicting its role
     * @param currentPos the current position of the target
     * @param prevPos the position of the target last turn
     * @param maxMovementLearned the current maximum observed
     * @return the current maximum observed
     */
    private double calculateMaxMovement(int[] currentPos, int[] prevPos, double maxMovementLearned) {

        int newMove = super.getDistances(currentPos, prevPos);

        //check if movement was greater than previous movement observed
        if (((double) newMove) > maxMovementLearned) {
            return newMove;
        } else {
            return maxMovementLearned;
        }

    }

    /**
     * Gets the amount of chasers nearby to a possible target and delivers a value between 0-1
     * on how much "pressure" the target is under
     * @param targetPos the location of the target
     * @return a pressure value between 0-1 that tells how much pressure the target is under
     */
    private double calculateChaserPressure(int[] targetPos) {
        int nearbyChasers = 0;
        double pressureScore;

        //iterate through the nearby chasers
        for (int chaserIndex=0; chaserIndex<this.chasersPos.length; chaserIndex++) {

            //gets the distance between the target and the chaser
            int distance = super.getDistances(targetPos, this.chasersPos[chaserIndex]);

            //increases the amount of nearby chasers if it detects that they are within
            //two turns of a chaser's possible max movement
            if (distance <= (2*5) ) {
                nearbyChasers++;
            }
        }

        pressureScore = nearbyChasers / (1+nearbyChasers);
        return pressureScore;
    }

    /**
     * Predicts the role of the target using observations with negative numbers being more likely
     * to be VIPs
     * @param maxMovementLearned the maximum movement observed by the chaser
     * @param numCatches the number of catches the chaser has succeeded against the target
     * @return a value representing how likely it is to be a VIP (lower is likely VIP and vice versa)
     */
    private double calculateRolePrediction(double maxMovementLearned, double numCatches) {
        if (maxMovementLearned > 3 || numCatches >= 2) { //is a guard
            return 1.0; //deprioritize
        } else {
            return -(1.0 - (maxMovementLearned/4));
        }
    }

    /**
     * The application class will send the results of the tag attempt
     * @param ID the ID of the target
     * @param isSuccess whether the target dodged or not
     */
    public void sendTagResult(int ID, boolean isSuccess) {
        if (isSuccess) {
            this.targetTable[ID][DODGE_EST] -= 0.03;
            this.targetTable[ID][HP_EST]--;
            this.targetTable[ID][NUM_CATCHES]++;
        } else {
            this.targetTable[ID][DODGE_EST] += 0.03;
        }
    }

    /**
     * Updates the targetTable data
     */
    private void updateTargetTable() {

        //iterate through the table (position corresponds to id)
        for (int i=0; i<this.targetTable.length; i++) {
            this.targetTable[i][TURN_DIST] = this.calculateDistanceScore(super.getDistances(botsPos[i]), super.getMOVES_PER_TURN());
            this.targetTable[i][MAX_MOVE_OBS] = this.calculateMaxMovement(this.botsPos[i], this.prevPos[i], this.targetTable[i][MAX_MOVE_OBS]);
            this.targetTable[i][PRESSURE] = this.calculateChaserPressure(this.botsPos[i]);
        }

        //getting the current position after it has calculated the max movement
        for (int i=0; i<this.targetTable.length; i++) {
            this.prevPos[i] = botsPos[i];
        }
    }

    /**
     * Used by the application to tell the chaser start targeting
     * @param numTargets the number of non-chaser robots in the arena
     */
    public void initTargeteting(int numTargets) {

        //iterate through bots to get their positions and store them to calculate movement
        for (int i=0; i<numTargets; i++) {
            this.prevPos[i] = this.botsPos[i];
        }

        //iterate through the table (index corresponds to id)
        for (int i=0; i<numTargets; i++) {
            this.targetTable[i][TURN_DIST] = this.calculateDistanceScore(super.getDistances(this.botsPos[i]), super.getMOVES_PER_TURN());
            this.targetTable[i][DODGE_EST] = 0.45; //will be modified after a catch attempt
            this.targetTable[i][MAX_MOVE_OBS] = 1;
            this.targetTable[i][HP_EST] = 3;
            this.targetTable[i][PRESSURE] = this.calculateChaserPressure(this.botsPos[i]);
            this.targetTable[i][NUM_CATCHES] = 0;
        }
    }

    public void takeTurn() {

    }
}
