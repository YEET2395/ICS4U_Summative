package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;
import java.util.Random;

/**
 * @Todo: improve movement algorithm to better avoid chasers + fine tune speed tracking and prediction
 */
public class XiongBot extends BaseBot {
    private int[] guardPos = {0, 0};
    // changed to support multiple chasers
    private int[][] chaserPos = new int[0][0];
    private int movesPerTurn = 1;
    // track chaser speeds: stores the speed (manhattan distance) observed last round for each chaser
    private double[] chaserSpeeds = new double[0];
    // store previous position (one round) to calculate speed and direction
    private int[][] chaserPrevPos = new int[0][2];
    // random generator for tie-breaking and probabilistic decisions
    private final Random rnd = new Random();

    /**
     * Constructor for BaseBot
     *
     * @param city      City the robot is in
     * @param str       Street number
     * @param ave       Avenue number
     * @param dir       direction the robot is facing
     * @param role      role of the bot
     * @param id        identifier of the bot
     * @param hp        health points of the bot
     * @param dodgeDiff dodging difficulty (double)
     */
    public XiongBot(City city, int str, int ave, Direction dir, int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
        super(city, str, ave, dir, id, role, hp, movesPerTurn, dodgeDiff);
        this.movesPerTurn = movesPerTurn;

        //for debugging
        super.setColor(Color.GREEN);
        super.setLabel("Robot " + super.getMyID());
    }

    public void getGuardPosition(int[] coord) {
        guardPos[0] = coord[0];
        guardPos[1] = coord[1];
    }

    // accepts multiple chaser coordinates
    public void setChaserPositions(int[][] coords) {
        if (coords == null) {
            this.chaserPos = new int[0][0];
            this.chaserSpeeds = new double[0];
            this.chaserPrevPos = new int[0][2];
            return;
        }

        // If chaser count unchanged and we already have prev positions, only update current positions
        if (this.chaserPrevPos.length == coords.length && this.chaserPrevPos.length > 0) {
            // ensure chaserPos and chaserSpeeds arrays are the right size
            this.chaserPos = new int[coords.length][2];
            this.chaserSpeeds = new double[coords.length];
            for (int i = 0; i < coords.length; i++) {
                this.chaserPos[i][0] = coords[i][0];
                this.chaserPos[i][1] = coords[i][1];
                // keep existing chaserPrevPos as-is so trackChaserSpeeds can compute a one-round delta
                // chaserSpeeds will be updated by trackChaserSpeeds
            }
            return;
        }

        // Otherwise (first time or count changed) initialize arrays and set previous positions to current
        this.chaserPos = new int[coords.length][2];
        this.chaserSpeeds = new double[coords.length];
        this.chaserPrevPos = new int[coords.length][2]; // keep only the previous round position
        for (int i = 0; i < coords.length; i++) {
            this.chaserPos[i][0] = coords[i][0];
            this.chaserPos[i][1] = coords[i][1];
            // initialize speeds and previous position to current (first round -> speed 0)
            this.chaserSpeeds[i] = 0;
            this.chaserPrevPos[i][0] = coords[i][0];
            this.chaserPrevPos[i][1] = coords[i][1];
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

    public void takeTurn() {
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
            int currentMinDist = Integer.MAX_VALUE;
            for (int i = 0; i < this.chaserPos.length; i++) {
                int[] c = this.chaserPos[i];
                int d = this.getDistances(c);
                if (d < currentMinDist) {
                    currentMinDist = d;
                }
            }

            // Evaluate candidate directions and pick the one that maximizes the minimum distance to any chaser
            // We'll track the best and second-best candidates so we can apply probabilistic tie-breaking
            Direction[] candidates = {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST};
            Direction bestDir = null;
            int bestMinDistance = Integer.MIN_VALUE;
            int bestIncrease = Integer.MIN_VALUE;

            Direction secondBestDir = null;
            int secondBestMinDistance = Integer.MIN_VALUE;
            int secondBestIncrease = Integer.MIN_VALUE;

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
                int minDist = Integer.MAX_VALUE;
                for (int j = 0; j < this.chaserPos.length; j++) {
                    int[] c = this.chaserPos[j];
                    int dist = Math.abs(newX - c[0]) + Math.abs(newY - c[1]);
                    if (dist < minDist) {
                        minDist = dist;
                    }
                }

                int increase = minDist - currentMinDist;

                // Update best/second-best accordingly
                if (minDist > bestMinDistance || (minDist == bestMinDistance && increase > bestIncrease)) {
                    // shift current best to second-best
                    secondBestDir = bestDir;
                    secondBestMinDistance = bestMinDistance;
                    secondBestIncrease = bestIncrease;

                    bestDir = d;
                    bestMinDistance = minDist;
                    bestIncrease = increase;
                } else if (minDist > secondBestMinDistance || (minDist == secondBestMinDistance && increase > secondBestIncrease)) {
                    secondBestDir = d;
                    secondBestMinDistance = minDist;
                    secondBestIncrease = increase;
                }
            }

            // After checks, restore original facing direction
            this.turnDirection(currentDir);

            // If no valid move was found, stop trying further steps
            if (bestDir == null) {
                break;
            }

            // Choose between best and second-best with the requested probabilities:
            // - If both have the same min-distance, pick randomly between them 50/50
            // - Otherwise pick best with 0.7 probability and second-best with 0.3
            Direction chosenDir = bestDir;
            if (secondBestDir != null) {
                if (bestMinDistance == secondBestMinDistance) {
                    chosenDir = rnd.nextBoolean() ? bestDir : secondBestDir;
                } else {
                    double p = rnd.nextDouble();
                    chosenDir = (p < 0.7) ? bestDir : secondBestDir;
                }
            }

            // Attempt to move in the chosen direction. If failed, stop further movement.
            if (!attemptMove(chosenDir)) {
                break;
            }
        }
    }

    /**
     * Helper method to get the opposite direction
     *
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
     * Observes and tracks the speed of each chaser based on their last round movement.
     * Updates the chaserSpeeds array with the Manhattan distance moved in the last round.
     * This uses only the previous round (one-round tracking) for speed.
     */
    public void trackChaserSpeeds() {
        if (this.chaserPos.length == 0) {
            return;
        }

        // Ensure prev-pos array matches length
        if (this.chaserPrevPos.length != this.chaserPos.length) {
            int[][] newPrev = new int[this.chaserPos.length][2];
            for (int i = 0; i < this.chaserPos.length; i++) {
                if (i < this.chaserPrevPos.length) {
                    newPrev[i][0] = this.chaserPrevPos[i][0];
                    newPrev[i][1] = this.chaserPrevPos[i][1];
                } else {
                    newPrev[i][0] = this.chaserPos[i][0];
                    newPrev[i][1] = this.chaserPos[i][1];
                }
            }
            this.chaserPrevPos = newPrev;
        }

        for (int i = 0; i < this.chaserPos.length; i++) {
            int curX = this.chaserPos[i][0];
            int curY = this.chaserPos[i][1];
            int prevX = this.chaserPrevPos[i][0];
            int prevY = this.chaserPrevPos[i][1];

            // Manhattan distance moved since last round -> one-round speed
            int distance = Math.abs(curX - prevX) + Math.abs(curY - prevY);
            this.chaserSpeeds[i] = distance;

            // update prev to current for next round
            this.chaserPrevPos[i][0] = curX;
            this.chaserPrevPos[i][1] = curY;
        }
    }

    /**
     * Predicts the future position of a specific chaser based on their movement from the previous round.
     * Uses the single-round delta (current - previous) and linear extrapolation.
     *
     * @param chaserIndex the index of the chaser to predict
     * @param turnsAhead  the number of turns to predict ahead (defaults to 1 for one-round prediction)
     * @return an array containing the predicted [x, y] coordinates, or null if prediction is not possible
     */
    public int[] predictChaserPosition(int chaserIndex, int turnsAhead) {
        if (chaserIndex < 0 || chaserIndex >= this.chaserPos.length) {
            return null;
        }

        if (this.chaserPrevPos.length != this.chaserPos.length) {
            // Not enough info, return current pos
            return new int[]{this.chaserPos[chaserIndex][0], this.chaserPos[chaserIndex][1]};
        }

        int[] currentPos = this.chaserPos[chaserIndex];
        int[] previousPos = this.chaserPrevPos[chaserIndex];

        int deltaX = currentPos[0] - previousPos[0];
        int deltaY = currentPos[1] - previousPos[1];

        // If there's no movement detected last round, prediction is current position
        if (deltaX == 0 && deltaY == 0) {
            return new int[]{currentPos[0], currentPos[1]};
        }

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