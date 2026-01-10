package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

/**
 * Simple test chaser bot that moves forward in a straight line
 * Used for testing the XiongBot VIP's speed tracking and position prediction
 * @author Test Bot
 * @version 2026 01 08
 */
public class TestChaserBot extends BaseBot {

    /**
     * Constructor for TestChaserBot
     * @param city City the robot is in
     * @param str the Street of the robot (y)
     * @param ave the Avenue of the robot (x)
     * @param dir the Direction the robot is facing
     * @param id the robot's numerical ID
     * @param role the robot's role (should be 3 for Chaser)
     * @param hp the amount of health
     * @param movesPerTurn the max amount of moves the robot can make
     * @param dodgeDiff the dodging/catching capability of the robot
     */
    public TestChaserBot(City city, int str, int ave, Direction dir, int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
        super(city, str, ave, dir, role, id, hp, movesPerTurn, dodgeDiff);

        // for debugging - make it blue to distinguish from other robots
        super.setColor(Color.BLUE);
        super.setLabel("TestChaser " + super.myRecords.getID());
    }

    public void updateEnemyRecords(PlayerInfo[] records) {
        // For testing, we won't implement this method
    }

    /**
     * Simple takeTurn method that just moves forward if the path is clear
     */
    public void takeTurn() {
        // int moveTurn = 2;
        // Move forward as many times as allowed per turn
        for (int i = 0; i < 2; i++) {
            if (this.frontIsClear()) {
                this.move();
            } else {
                // If blocked, try turning right and moving
                this.turnRight();
                if (this.frontIsClear()) {
                    this.move();
                } else {
                    // If still blocked, turn around
                    this.turnAround();
                }
            }
        }
    }

    /**
     * Helper method to get moves per turn
     * @return the number of moves this robot can make per turn
     */
    private double getMovesPerTurn() {
        return super.myRecords.getDodgeDifficulty(); // Using dodge difficulty as a proxy for moves per turn
    }
}

