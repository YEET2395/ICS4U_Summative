package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;
import java.util.Random;
import java.util.ArrayList;

/**
 * XiongBot with selection-sorted candidate choices, chaser tracking, and fallback to avoid corner-sticking.
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

    // track last position to avoid immediate back-and-forth movement
    private int[] lastPos = null;
    // cooldown (in steps) during which returning to lastPos is disallowed
    private int lastPosCooldown = 0;

    // how many future turns the bot wants to be sure chasers can't reach it in
    private int safetyTurns = 1;

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
        super.setLabel("Robot " + super.myRecords.getID());
    }

    public void updateOtherRecords(PlayerInfo[] records) {
        // Collect chaser positions from the global records and update internal tracking arrays
        ArrayList<int[]> chasers = new ArrayList<>();
        for (int i = 0; i < records.length; i++) {
            PlayerInfo r = records[i];
            // role 3 denotes chaser in this application
            if (r.getRole() == 3) {
                int[] pos = r.getPosition();
                if (pos != null) {
                    // make a defensive copy
                    chasers.add(new int[]{pos[0], pos[1]});
                }
            }
            // if there is a guard role (2) supply the guard position
            if (r.getRole() == 2) {
                int[] pos = r.getPosition();
                if (pos != null) {
                    this.getGuardPosition(new int[]{pos[0], pos[1]});
                }
            }
        }

        // convert to array and set
        int[][] coords = new int[chasers.size()][2];
        for (int i = 0; i < chasers.size(); i++) {
            coords[i][0] = chasers.get(i)[0];
            coords[i][1] = chasers.get(i)[1];
        }

        // Update chaser positions and then compute speeds based on previous positions
        this.setChaserPositions(coords);
        this.trackChaserSpeeds();
    }

    public void initRecords(PlayerInfo[] records) {
        System.out.println("Initializing records");
        // On initialization, populate chaser positions so takeTurn() can operate immediately
        ArrayList<int[]> chasers = new ArrayList<>();
        for (int i = 0; i < records.length; i++) {
            PlayerInfo r = records[i];
            if (r.getRole() == 3) {
                int[] pos = r.getPosition();
                if (pos != null) {
                    chasers.add(new int[]{pos[0], pos[1]});
                }
            }
            if (r.getRole() == 2) {
                int[] pos = r.getPosition();
                if (pos != null) {
                    this.getGuardPosition(new int[]{pos[0], pos[1]});
                }
            }
        }

        int[][] coords = new int[chasers.size()][2];
        for (int i = 0; i < chasers.size(); i++) {
            coords[i][0] = chasers.get(i)[0];
            coords[i][1] = chasers.get(i)[1];
        }

        // initialize internal chaser tracking
        this.setChaserPositions(coords);
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
            // Keep arrays consistent and sorted by distance
            this.sortChasersByDistance();
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

        // Keep arrays sorted by distance after initialization
        this.sortChasersByDistance();
    }

    // Selection sort the chaser arrays by Manhattan distance to this bot (closest first).
    // This keeps chaserPos, chaserSpeeds, and chaserPrevPos in sync.
    private void sortChasersByDistance() {
        if (this.chaserPos == null) {
            return;
        }
        int n = this.chaserPos.length;
        if (n <= 1) {
            return;
        }

        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            int minDist = Integer.MAX_VALUE;
            // compute distance for minIdx initially
            int[] minPos = this.chaserPos[minIdx];
            if (minPos != null) {
                minDist = this.getDistances(minPos);
            }

            for (int j = i + 1; j < n; j++) {
                int[] posJ = this.chaserPos[j];
                if (posJ == null) {
                    continue;
                }
                int distJ = this.getDistances(posJ);
                if (distJ < minDist) {
                    minDist = distJ;
                    minIdx = j;
                }
            }

            if (minIdx != i) {
                // swap chaserPos
                int[] tmpPos = this.chaserPos[i];
                this.chaserPos[i] = this.chaserPos[minIdx];
                this.chaserPos[minIdx] = tmpPos;

                // swap chaserSpeeds if present
                if (this.chaserSpeeds != null && this.chaserSpeeds.length > 0) {
                    double tmpSpeed = this.chaserSpeeds[i];
                    this.chaserSpeeds[i] = this.chaserSpeeds[minIdx];
                    this.chaserSpeeds[minIdx] = tmpSpeed;
                }

                // swap chaserPrevPos
                if (this.chaserPrevPos != null && this.chaserPrevPos.length > 0) {
                    int[] tmpPrev = this.chaserPrevPos[i];
                    this.chaserPrevPos[i] = this.chaserPrevPos[minIdx];
                    this.chaserPrevPos[minIdx] = tmpPrev;
                }
            }
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
            // manage lastPos cooldown so we avoid allowing immediate reversal for one following step
            boolean avoidReverse = false;
            if (this.lastPos != null && this.lastPosCooldown > 0) {
                avoidReverse = true;
            }
            // decrement cooldown at start of step so it applies to this step then expires
            if (this.lastPosCooldown > 0) {
                this.lastPosCooldown = this.lastPosCooldown - 1;
                if (this.lastPosCooldown == 0) {
                    this.lastPos = null;
                }
            }

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

            // Determine which chaser is "most reachable" (can reach us in the fewest turns)
            // We compute time-to-reach = ceil(distance / max(speed, 1)) using tracked chaserSpeeds.
            int mostReachableIndex = -1;
            int mostReachableTime = Integer.MAX_VALUE;
            int mostReachableDist = Integer.MAX_VALUE; // tie-breaker: smaller distance
            double minAssumedSpeed = 1.0;
            for (int i = 0; i < this.chaserPos.length; i++) {
                int[] c = this.chaserPos[i];
                int dist = this.getDistances(c);
                double s = minAssumedSpeed;
                if (i < this.chaserSpeeds.length) {
                    s = this.chaserSpeeds[i];
                }
                if (s < minAssumedSpeed) {
                    s = minAssumedSpeed;
                }
                int timeToReach = (int) Math.ceil((double) dist / s);
                if (timeToReach < mostReachableTime || (timeToReach == mostReachableTime && dist < mostReachableDist)) {
                    mostReachableTime = timeToReach;
                    mostReachableIndex = i;
                    mostReachableDist = dist;
                }
            }

            // Evaluate candidate directions and pick the one that maximizes the minimum distance to any chaser
            // We'll track the best and second-best candidates so we can apply probabilistic tie-breaking
            Direction[] candidates = {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST};
            Direction currentDirForChecks = this.getDirection();
            int startIdx = 0;
            for (int i = 0; i < candidates.length; i++) {
                if (candidates[i] == currentDirForChecks) {
                    startIdx = i;
                    break;
                }
            }

            // Sample frontIsClear once per direction by rotating through the four directions.
            // Cache results and neighbor coordinates so we avoid repeated turning inside the candidate loop.
            int mDirs = candidates.length;
            boolean[] frontClearCache = new boolean[mDirs];
            int[] neighX = new int[mDirs];
            int[] neighY = new int[mDirs];
            for (int t = 0; t < mDirs; t++) {
                Direction d = candidates[t];
                this.turnDirection(d);
                frontClearCache[t] = this.frontIsClear();
                neighX[t] = myX;
                neighY[t] = myY;
                if (d == Direction.NORTH) {
                    neighY[t] = myY - 1;
                } else if (d == Direction.SOUTH) {
                    neighY[t] = myY + 1;
                } else if (d == Direction.EAST) {
                    neighX[t] = myX + 1;
                } else if (d == Direction.WEST) {
                    neighX[t] = myX - 1;
                }
            }
            // restore original facing before evaluation
            this.turnDirection(currentDirForChecks);

            // Count free neighbors from cache
            int freeCountCurr = 0;
            for (int t = 0; t < mDirs; t++) {
                if (frontClearCache[t]) {
                    freeCountCurr = freeCountCurr + 1;
                }
            }

            // helper to map a Direction to index in candidates array
            // (N->0, W->1, S->2, E->3)
            java.util.Map<Direction, Integer> dirToIndex = new java.util.HashMap<>();
            dirToIndex.put(Direction.NORTH, 0);
            dirToIndex.put(Direction.WEST, 1);
            dirToIndex.put(Direction.SOUTH, 2);
            dirToIndex.put(Direction.EAST, 3);

            // Build candidate arrays and compute metrics for each valid candidate. We'll selection-sort them
            // so we can pick the top choices without separate best/second variables.
            int m = candidates.length;
            int[] minDistArr = new int[m];
            int[] increaseArr = new int[m];
            boolean[] validArr = new boolean[m];
            boolean[] reverseArr = new boolean[m];
            Direction[] orderedDirs = new Direction[m];

            for (int k = 0; k < m; k++) {
                // initialize
                minDistArr[k] = Integer.MIN_VALUE;
                increaseArr[k] = Integer.MIN_VALUE;
                validArr[k] = false;
                reverseArr[k] = false;
                orderedDirs[k] = candidates[k];
            }

            // Evaluate each candidate by rotating starting from current facing
            for (int k = 0; k < m; k++) {
                Direction d = candidates[(startIdx + k) % candidates.length];

                // Use cached front-clear and neighbor coords to avoid extra rotation
                int dirIdx = dirToIndex.get(d);
                if (!frontClearCache[dirIdx]) {
                    // mark as invalid and continue
                    for (int p = 0; p < m; p++) {
                        if (orderedDirs[p] == d) {
                            validArr[p] = false;
                            minDistArr[p] = Integer.MIN_VALUE;
                            increaseArr[p] = Integer.MIN_VALUE;
                            break;
                        }
                    }
                    continue;
                }

                int newX = neighX[dirIdx];
                int newY = neighY[dirIdx];

                // If we have an active avoidReverse flag, mark candidate as reverse when it moves back to lastPos
                if (this.lastPos != null && avoidReverse) {
                    if (newX == this.lastPos[0] && newY == this.lastPos[1]) {
                        for (int p = 0; p < m; p++) {
                            if (orderedDirs[p] == d) {
                                reverseArr[p] = true;
                                validArr[p] = false;
                                minDistArr[p] = Integer.MIN_VALUE;
                                increaseArr[p] = Integer.MIN_VALUE;
                                break;
                            }
                        }
                        // skip reverse in main pass
                        continue;
                    }
                }

                // If moving here would reduce our distance to the most reachable chaser, skip it.
                boolean reducesToMost = false;
                if (mostReachableIndex >= 0 && mostReachableIndex < this.chaserPos.length) {
                    int[] most = this.chaserPos[mostReachableIndex];
                    int curDistToMost = mostReachableDist;
                    int candDistToMost = Math.abs(newX - most[0]) + Math.abs(newY - most[1]);
                    if (candDistToMost < curDistToMost) {
                        reducesToMost = true;
                    }
                }
                if (reducesToMost) {
                    // mark as invalid
                    for (int p = 0; p < m; p++) {
                        if (orderedDirs[p] == d) {
                            validArr[p] = false;
                            minDistArr[p] = Integer.MIN_VALUE;
                            increaseArr[p] = Integer.MIN_VALUE;
                            break;
                        }
                    }
                    continue;
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

                // store metrics in the slot corresponding to this direction
                for (int p = 0; p < m; p++) {
                    if (orderedDirs[p] == d) {
                        minDistArr[p] = minDist;
                        increaseArr[p] = increase;
                        validArr[p] = true;

                        // Apply corner penalty using cached frontClearCache for left/right
                        Direction left = leftOf(d);
                        Direction right = rightOf(d);
                        int leftIdx = dirToIndex.get(left);
                        int rightIdx = dirToIndex.get(right);
                        boolean leftBlocked = !frontClearCache[leftIdx];
                        boolean rightBlocked = !frontClearCache[rightIdx];
                        if (leftBlocked && rightBlocked) {
                            // reduce its effective min distance to deprioritize corners
                            minDistArr[p] = minDistArr[p] - 5000;
                        }

                        // Additional penalty when current position itself is narrow (few free neighbors)
                        if (freeCountCurr <= 2) {
                            minDistArr[p] = minDistArr[p] - 2000;
                        }

                        break;
                    }
                }
            }

            // After evaluating candidates, selection-sort them by minDist (descending), tie-breaker increase (descending)
            for (int i = 0; i < m - 1; i++) {
                int maxIdx = i;
                for (int j = i + 1; j < m; j++) {
                    // choose element j if it is better than current max
                    boolean replace = false;
                    if (minDistArr[j] > minDistArr[maxIdx]) {
                        replace = true;
                    } else if (minDistArr[j] == minDistArr[maxIdx]) {
                        if (increaseArr[j] > increaseArr[maxIdx]) {
                            replace = true;
                        }
                    }
                    if (replace) {
                        maxIdx = j;
                    }
                }
                if (maxIdx != i) {
                    // swap minDistArr
                    int tmpMin = minDistArr[i];
                    minDistArr[i] = minDistArr[maxIdx];
                    minDistArr[maxIdx] = tmpMin;

                    // swap increaseArr
                    int tmpInc = increaseArr[i];
                    increaseArr[i] = increaseArr[maxIdx];
                    increaseArr[maxIdx] = tmpInc;

                    // swap validArr
                    boolean tmpVal = validArr[i];
                    validArr[i] = validArr[maxIdx];
                    validArr[maxIdx] = tmpVal;

                    // swap reverseArr
                    boolean tmpRev = reverseArr[i];
                    reverseArr[i] = reverseArr[maxIdx];
                    reverseArr[maxIdx] = tmpRev;

                    // swap orderedDirs
                    Direction tmpDir = orderedDirs[i];
                    orderedDirs[i] = orderedDirs[maxIdx];
                    orderedDirs[maxIdx] = tmpDir;
                }
            }

            // Restore original facing before making the chosen move
            this.turnDirection(currentDir);

            // Find the first valid candidate in the sorted list
            int chosenIdx = -1;
            for (int i = 0; i < m; i++) {
                if (validArr[i]) {
                    chosenIdx = i;
                    break;
                }
            }
            if (chosenIdx == -1) {
                // No strictly-valid non-reducing moves available. Try a relaxed fallback:
                // choose a front-clear candidate that reduces distance to the most-reachable chaser the least.
                int fallbackIdx = -1;
                int bestFallbackCandDistToMost = Integer.MIN_VALUE; // larger is better (less reduction)
                int bestFallbackMinDist = Integer.MIN_VALUE; // tie-breaker

                // current distance to most-reachable chaser
                int curDistToMost = Integer.MAX_VALUE;
                if (mostReachableIndex >= 0 && mostReachableIndex < this.chaserPos.length) {
                    int[] most = this.chaserPos[mostReachableIndex];
                    curDistToMost = Math.abs(myX - most[0]) + Math.abs(myY - most[1]);
                }

                for (int i = 0; i < m; i++) {
                    if (reverseArr[i]) {
                        continue;
                    }
                    Direction d = orderedDirs[i];
                    // skip if we already marked as valid (shouldn't happen) or if candidate was invalid due to blocked front
                    if (validArr[i]) {
                        continue;
                    }

                    // turn to check front
                    this.turnDirection(d);
                    if (!this.frontIsClear()) {
                        continue;
                    }

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

                    // compute distance to the most-reachable chaser for this candidate (if available)
                    int candDistToMost = Integer.MIN_VALUE;
                    if (mostReachableIndex >= 0 && mostReachableIndex < this.chaserPos.length) {
                        int[] most = this.chaserPos[mostReachableIndex];
                        candDistToMost = Math.abs(newX - most[0]) + Math.abs(newY - most[1]);
                    } else {
                        // if no most-reachable info, treat as neutral
                        candDistToMost = curDistToMost;
                    }

                    // avoid moving directly onto the chaser if possible
                    if (candDistToMost == 0) {
                        continue;
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

                    // prefer candidates that keep candDistToMost large (i.e., reduce least). Tie-break on minDist.
                    if (fallbackIdx == -1
                            || candDistToMost > bestFallbackCandDistToMost
                            || (candDistToMost == bestFallbackCandDistToMost && minDist > bestFallbackMinDist)) {
                        fallbackIdx = i;
                        bestFallbackCandDistToMost = candDistToMost;
                        bestFallbackMinDist = minDist;
                    }
                }

                if (fallbackIdx == -1) {
                    // truly no possible moves (all blocked or would land on a chaser) -> stay
                    break;
                }

                chosenIdx = fallbackIdx;
            }

            Direction chosenDir = orderedDirs[chosenIdx];

            // If there is a second valid candidate, probabilistically choose between top two
            int secondIdx = -1;
            for (int i = chosenIdx + 1; i < m; i++) {
                if (validArr[i]) {
                    secondIdx = i;
                    break;
                }
            }

            if (secondIdx != -1) {
                // configure thresholds for mapping distance -> probability
                int minDistanceForMax = 1; // at or below this distance, pick best almost always
                int dynamicReach = computeMaxChaserReach(this.safetyTurns);
                int maxRelevantDistance = dynamicReach + 1; // add small cushion

                double pBest;
                if (currentMinDist <= minDistanceForMax) {
                    pBest = 0.99;
                } else if (currentMinDist >= maxRelevantDistance) {
                    pBest = 0.60;
                } else {
                    double ratio = (double) (maxRelevantDistance - currentMinDist) / (double) (maxRelevantDistance - minDistanceForMax);
                    pBest = 0.60 + Math.pow(0.39, ratio);
                }

                double r = rnd.nextDouble();
                if (r >= pBest) {
                    chosenDir = orderedDirs[secondIdx];
                }
            }

            int prevXForLastPos = myX;
            int prevYForLastPos = myY;
            if (!attemptMove(chosenDir)) {
                break;
            }

            // record previous position so next step won't immediately move back; set cooldown for one step
            this.lastPos = new int[]{prevXForLastPos, prevYForLastPos};
            this.lastPosCooldown = 1;
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

        // Keep chaser arrays sorted by proximity after updating speeds/prev positions
        this.sortChasersByDistance();

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

    // Helper: get the direction to the left
    private Direction leftOf(Direction d) {
        if (d == Direction.NORTH) return Direction.WEST;
        if (d == Direction.WEST) return Direction.SOUTH;
        if (d == Direction.SOUTH) return Direction.EAST;
        return Direction.NORTH; // EAST -> NORTH
    }

    // Helper: get the direction to the right
    private Direction rightOf(Direction d) {
        if (d == Direction.NORTH) return Direction.EAST;
        if (d == Direction.EAST) return Direction.SOUTH;
        if (d == Direction.SOUTH) return Direction.WEST;
        return Direction.NORTH; // WEST -> NORTH
    }

    // Test whether the adjacent cell in direction 'd' from current position is blocked (wall or obstacle)
    // Note: this checks frontIsClear after turning to `d` from the robot's current facing.
    private boolean isBlockedDir(Direction d) {
        this.turnDirection(d);
        return !this.frontIsClear();
    }
}
