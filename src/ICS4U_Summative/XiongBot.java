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

    // how many future turns the bot wants to be sure chasers can't reach it in
    private int safetyTurns = 3;

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
            this.chaserSpeeds[i] = 5;
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

            // Choose between best and second-best with a dynamic probability:
            // The closer we are to the nearest chaser (smaller currentMinDist), the higher
            // the probability of choosing the most optimal (best) direction.
            Direction chosenDir = bestDir;
            if (secondBestDir != null) {
                // configure thresholds for mapping distance -> probability
                int minDistanceForMax = 1; // at or below this distance, pick best almost always
                // Dynamically compute a maxRelevantDistance so that if the bot's currentMinDist
                // exceeds this value, no chaser could reach the bot within `safetyTurns` turns.
                int dynamicReach = computeMaxChaserReach(this.safetyTurns);
                int maxRelevantDistance = dynamicReach + 1; // add small cushion

                double pBest;
                if (currentMinDist <= minDistanceForMax) {
                    pBest = 0.99;
                } else if (currentMinDist >= maxRelevantDistance) {
                    pBest = 0.60;
                } else {
                    // exponentioal interpolate between 0.99 (close) and 0.60 (far)
                    double ratio = (double)(maxRelevantDistance - currentMinDist) / (double)(maxRelevantDistance - minDistanceForMax);
                    pBest = 0.60 + Math.pow(0.39, ratio); // exponential curve for smoother transition
                }

                // If both candidates have equal min-distance, still bias toward best depending on proximity
                if (bestMinDistance == secondBestMinDistance) {
                    double r = rnd.nextDouble();
                    if (r < pBest) {
                        chosenDir = bestDir;
                    } else {
                        chosenDir = secondBestDir;
                    }
                } else {
                    double r = rnd.nextDouble();
                    if (r < pBest) {
                        chosenDir = bestDir;
                    } else {
                        chosenDir = secondBestDir;
                    }
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

        // If we don't have prev/current data, return current position
        if (this.chaserPos.length == 0) {
            return null;
        }

        // Use the tracked speed for this chaser. If no speed tracked yet, assume 0 (stay in place).
        double trackedSpeed = 0.0;
        if (chaserIndex >= 0 && chaserIndex < this.chaserSpeeds.length) {
            trackedSpeed = this.chaserSpeeds[chaserIndex];
        }

        // Number of individual steps the chaser will take in the next `turnsAhead` turns.
        // Round to nearest int (you could also floor/ceil depending on desired behaviour).
        int stepsToSimulate = (int) Math.round(trackedSpeed * Math.max(1, turnsAhead));

        int curX = this.chaserPos[chaserIndex][0];
        int curY = this.chaserPos[chaserIndex][1];

        // Target is this bot's current position (assume chaser actively chases this bot)
        int[] myPos = this.getMyPosition();
        int targetX = myPos[0];
        int targetY = myPos[1];

        // If no movement expected, return current position
        if (stepsToSimulate <= 0) {
            return new int[]{curX, curY};
        }

        // Greedily move the chaser one step at a time towards the bot, reducing Manhattan distance each step.
        for (int s = 0; s < stepsToSimulate; s++) {
            int dx = targetX - curX;
            int dy = targetY - curY;

            if (dx == 0 && dy == 0) {
                // Already at the bot's position
                break;
            }

            // Prioritize the axis with the larger absolute difference to reduce distance faster.
            if (Math.abs(dx) >= Math.abs(dy)) {
                // Replace ternary with explicit if/else
                if (dx > 0) {
                    curX = curX + 1;
                } else {
                    curX = curX - 1;
                }
            } else {
                // Replace ternary with explicit if/else
                if (dy > 0) {
                    curY = curY + 1;
                } else {
                    curY = curY - 1;
                }
            }
        }

        return new int[]{curX, curY};
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

    /**
     * Set how many turns the bot uses to evaluate whether a chaser could reach it.
     * A value < 1 will be clamped to 1.
     * @param turns number of turns to be "safe" for
     */
    public void setSafetyTurns(int turns) {
        if (turns < 1) {
            this.safetyTurns = 1;
        } else {
            this.safetyTurns = turns;
        }
    }

    /**
     * Compute the maximum Manhattan distance any chaser could cover in `turns` turns
     * based on tracked chaser speeds. To be conservative, if a chaser's tracked speed
     * is < 1 (not observed or stationary), we assume at least 1 step/turn.
     * @param turns number of turns to look ahead
     * @return maximum reachable Manhattan distance by any chaser in `turns` turns
     */
    private int computeMaxChaserReach(int turns) {
        if (turns < 1) {
            turns = 1;
        }
        if (this.chaserSpeeds == null || this.chaserSpeeds.length == 0) {
            // no data -> fallback to a small conservative default
            return 3 * turns;
        }

        double minAssumedSpeed = 1.0; // conservative assumption when speed is unknown/zero
        int maxReach = 0;
        for (int i = 0; i < this.chaserSpeeds.length; i++) {
            double s = this.chaserSpeeds[i];
            if (s < minAssumedSpeed) {
                s = minAssumedSpeed;
            }
            int reach = (int) Math.ceil(s * turns);
            if (reach > maxReach) {
                maxReach = reach;
            }
        }
        return maxReach;
    }
}
