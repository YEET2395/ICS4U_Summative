package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

public class XiongBot extends BaseBot{
    private int[] guardPos = {0, 0};
    // changed to support multiple chasers
    private int[][] chaserPos = new int[0][0];
    private int movesPerTurn = 1;

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
        this.movesPerTurn = movesPerTurn;

        //for debugging
        super.setColor(Color.GREEN);
        super.setLabel("Robot " + super.getMyID());
    }

    public void getGuardPosition(int[] coord){
        guardPos[0]=coord[0];
        guardPos[1]=coord[1];
    }

    // accepts multiple chaser coordinates
    public void setChaserPositions(int[][] coords) {
        if (coords == null) {
            this.chaserPos = new int[0][0];
            return;
        }
        this.chaserPos = new int[coords.length][2];
        for (int i = 0; i < coords.length; i++) {
            this.chaserPos[i][0] = coords[i][0];
            this.chaserPos[i][1] = coords[i][1];
        }
    }

    private boolean attemptMove(Direction d) {
        this.turnDirection(d);
        if (this.frontIsClear()) {
            this.move();
            return true;
        }
        return false;
    }

    public void takeTurn(){
        int movesAllowed = this.movesPerTurn;

        for (int step = 0; step < movesAllowed; step++) {
            int myX = this.getMyPosition()[0];
            int myY = this.getMyPosition()[1];

            // If we have no chaser info, do nothing this turn
            if (this.chaserPos.length == 0) {
                break;
            }

            // Current minimum distance to any chaser (used for tie-breaker)
            int currentMinDist = Integer.MAX_VALUE;
            for (int i = 0; i < this.chaserPos.length; i++) {
                int[] c = this.chaserPos[i];
                int d = this.getDistances(c);
                if (d < currentMinDist) {
                    currentMinDist = d;
                }
            }

            // Evaluate candidate directions and pick the one that maximizes the minimum distance to any chaser
            Direction[] candidates = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            Direction bestDir = null;
            int bestMinDistance = Integer.MIN_VALUE;
            int bestIncrease = Integer.MIN_VALUE;

            for (int i=0; i<candidates.length; i++) {
                Direction d = candidates[i];
                // turn to the direction to check if front is clear
                this.turnDirection(d);
                if (!this.frontIsClear()) {
                    continue;
                }

                // compute the coordinates if we move one step in this direction
                int newX = myX;
                int newY = myY;

                if (d == Direction.NORTH) {
                    newY = myY - 1;
                } else if (d == Direction.SOUTH) {
                    newY = myY + 1;
                } else if (d == Direction.EAST) {
                    newX = myX + 1;
                } else if (d == Direction.WEST) {
                    newX = myX - 1;
                }

                // compute minimum distance to any chaser from the new position
                int minDist = Integer.MAX_VALUE;
                for (int j = 0; j < this.chaserPos.length; j++) {
                    int[] c = this.chaserPos[j];
                    int dist = this.getDistances(new int[]{newX, newY});
                    if (dist < minDist) {
                        minDist = dist;
                    }
                }

                int increase = minDist - currentMinDist;

                // prefer the direction which gives the largest minimum-distance; break ties by largest increase
                if (minDist > bestMinDistance || (minDist == bestMinDistance && increase > bestIncrease)) {
                    bestMinDistance = minDist;
                    bestDir = d;
                    bestIncrease = increase;
                }
            }

            // If no valid move was found, stop trying further steps
            if (bestDir == null) {
                break;
            }

            // Attempt to move in the chosen best direction. If failed, stop further movement.
            if (!attemptMove(bestDir)) {
                break;
            }
        }
    }
}
