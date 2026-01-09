package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

public class XiongBot extends BaseBot{
    private int[] guardPos = {0, 0};
    // changed to support multiple chasers
    private int[][] chaserPos = new int[0][0];
    private int movesPerTurn = 1;
    // track chaser speeds: stores the average speed of each chaser
    private double[] chaserSpeeds = new double[0];
    // store previous positions to calculate speed
    private int[][][] chaserPositionHistory = new int[0][0][2];

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
     * @param dodgeDiff dodging difficulty (double)
     */
    public XiongBot(City city, int str, int ave, Direction dir, int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
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
            this.chaserSpeeds = new double[0];
            this.chaserPositionHistory = new int[0][0][2];
            return;
        }
        this.chaserPos = new int[coords.length][2];
        this.chaserSpeeds = new double[coords.length];
        this.chaserPositionHistory = new int[coords.length][10][2]; // keep last 10 positions
        for (int i = 0; i < coords.length; i++) {
            this.chaserPos[i][0] = coords[i][0];
            this.chaserPos[i][1] = coords[i][1];
            // initialize speeds and history
            this.chaserSpeeds[i] = 0;
            this.chaserPositionHistory[i][0][0] = coords[i][0];
            this.chaserPositionHistory[i][0][1] = coords[i][1];
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
            Direction currentDir = this.getDirection();

            // If we have no chaser info, do nothing this turn
            if (this.chaserPos.length == 0) {
                break;
            }

            // Current minimum distance to any chaser (used for tie-breaker)
            int currentMinDist = 2147483647;
            for (int i = 0; i < this.chaserPos.length; i++) {
                int[] c = this.chaserPos[i];
                int d = this.getDistances(c);
                if (d < currentMinDist) {
                    currentMinDist = d;
                }
            }

            // Evaluate candidate directions and pick the one that maximizes the minimum distance to any chaser
            // Order follows repeated turnLeft() sequence so changing to the next candidate requires at most one turn
            Direction[] candidates = {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST};
            Direction bestDir = null;
            int bestMinDistance = -2147483648;
            int bestIncrease = -2147483648;

            // Determine index of current direction so we can iterate candidates starting from current facing
            Direction currentDirForChecks = this.getDirection();
            int startIdx = 0;
            for (int i = 0; i < candidates.length; i++) {
                if (candidates[i] == currentDirForChecks) {
                    startIdx = i;
                    break;
                }
            }

            // Iterate through directions once (rotate through them) instead of turning back-and-forth for each check
            for (int k = 0; k < candidates.length; k++) {
                Direction d = candidates[(startIdx + k) % candidates.length];

                // Turn to the candidate direction (this rotates sequentially and avoids back-and-forth spinning)
                this.turnDirection(d);

                // If front is blocked, skip this candidate
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
                int minDist = 2147483647;
                for (int j = 0; j < this.chaserPos.length; j++) {
                    int[] c = this.chaserPos[j];
                    int dist = Math.abs(newX - c[0]) + Math.abs(newY - c[1]);
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

            // After checks, restore original facing direction
            this.turnDirection(currentDir);

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

    /**
     * Helper method to get the opposite direction
     * @param dir the direction to reverse
     * @return the opposite direction
     */
    private Direction getOpposite(Direction dir) {
        if (dir == Direction.NORTH) return Direction.SOUTH;
        if (dir == Direction.SOUTH) return Direction.NORTH;
        if (dir == Direction.EAST) return Direction.WEST;
        return Direction.EAST;
    }

    /**
     * Observes and tracks the speed of each chaser based on their movement history.
     * Updates the chaserSpeeds array with the average speed (manhattan distance per turn) for each chaser.
     */
    public void trackChaserSpeeds() {
        if (this.chaserPos.length == 0 || this.chaserPositionHistory.length == 0) {
            return;
        }

        for (int i = 0; i < this.chaserPos.length; i++) {
            // Shift history: move all positions back and add the new one
            for (int j = this.chaserPositionHistory[i].length - 1; j > 0; j--) {
                this.chaserPositionHistory[i][j][0] = this.chaserPositionHistory[i][j - 1][0];
                this.chaserPositionHistory[i][j][1] = this.chaserPositionHistory[i][j - 1][1];
            }
            // Add current position as the most recent
            this.chaserPositionHistory[i][0][0] = this.chaserPos[i][0];
            this.chaserPositionHistory[i][0][1] = this.chaserPos[i][1];

            // Calculate average speed based on position history
            double totalDistance = 0;
            int validMeasurements = 0;

            for (int j = 0; j < this.chaserPositionHistory[i].length - 1; j++) {
                int x1 = this.chaserPositionHistory[i][j][0];
                int y1 = this.chaserPositionHistory[i][j][1];
                int x2 = this.chaserPositionHistory[i][j + 1][0];
                int y2 = this.chaserPositionHistory[i][j + 1][1];

                // Skip if positions are the same (chaser didn't move)
                if (x1 != x2 || y1 != y2) {
                    int distance = Math.abs(x1 - x2) + Math.abs(y1 - y2);
                    totalDistance += distance;
                    validMeasurements++;
                }
            }

            // Update the speed for this chaser
            if (validMeasurements > 0) {
                this.chaserSpeeds[i] = totalDistance / validMeasurements;
            } else {
                this.chaserSpeeds[i] = 0;
            }
        }
    }

    /**
     * Predicts the future position of a specific chaser based on their current position and tracked speed.
     * Uses linear extrapolation assuming the chaser continues moving in the same direction.
     *
     * @param chaserIndex the index of the chaser to predict
     * @param turnsAhead the number of turns to predict ahead
     * @return an array containing the predicted [x, y] coordinates, or null if prediction is not possible
     */
    public int[] predictChaserPosition(int chaserIndex, int turnsAhead) {
        if (chaserIndex < 0 || chaserIndex >= this.chaserPos.length) {
            return null;
        }

        if (this.chaserPositionHistory[chaserIndex].length < 2) {
            // Not enough history to determine direction
            return new int[]{this.chaserPos[chaserIndex][0], this.chaserPos[chaserIndex][1]};
        }

        // Get the current and previous position to determine direction
        int[] currentPos = this.chaserPos[chaserIndex];
        int[] previousPos = this.chaserPositionHistory[chaserIndex][1];

        // Calculate the direction vector
        int deltaX = currentPos[0] - previousPos[0];
        int deltaY = currentPos[1] - previousPos[1];

        // Predict future position by extrapolating the direction
        int predictedX = currentPos[0] + (deltaX * turnsAhead);
        int predictedY = currentPos[1] + (deltaY * turnsAhead);

        return new int[]{predictedX, predictedY};
    }

    /**
     * Gets the tracked speed of a specific chaser.
     *
     * @param chaserIndex the index of the chaser
     * @return the average speed (manhattan distance per turn) of the chaser, or -1 if invalid index
     */
    public double getChaserSpeed(int chaserIndex) {
        if (chaserIndex < 0 || chaserIndex >= this.chaserSpeeds.length) {
            return -1;
        }
        return this.chaserSpeeds[chaserIndex];
    }
}
