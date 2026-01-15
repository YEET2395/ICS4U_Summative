package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;
import java.util.Random;
import java.util.ArrayList;

//@TODO: make sure move dont crash into walls
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
        super.setLabel("VIP " + super.myRecords.getID());
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
        // single-per-turn planning: compute the best destination reachable within `movesAllowed` steps
        int myX = this.getMyPosition()[0];
        int myY = this.getMyPosition()[1];

        // decrement lastPos cooldown once per turn (was previously per-step)
        if (this.lastPosCooldown > 0) {
            this.lastPosCooldown = this.lastPosCooldown - 1;
            if (this.lastPosCooldown == 0) {
                this.lastPos = null;
            }
        }

        if (this.chaserPos == null || this.chaserPos.length == 0) {
            return; // nothing to plan against
        }

        // Build set of reachable coordinates within Manhattan radius = movesAllowed (ignore obstacles for planning)
        int bestX = myX;
        int bestY = myY;
        int bestArea = Integer.MAX_VALUE;
        int bestThreat = Integer.MAX_VALUE;
        int bestMinDist = Integer.MIN_VALUE;

        for (int dx = -movesAllowed; dx <= movesAllowed; dx++) {
            for (int dy = -movesAllowed; dy <= movesAllowed; dy++) {
                int man = Math.abs(dx) + Math.abs(dy);
                if (man == 0 || man > movesAllowed) continue;
                int cx = myX + dx;
                int cy = myY + dy;

                // avoid candidate squares occupied by chasers
                int minDist = Integer.MAX_VALUE;
                for (int j = 0; j < this.chaserPos.length; j++) {
                    int[] c = this.chaserPos[j];
                    int dist = Math.abs(cx - c[0]) + Math.abs(cy - c[1]);
                    if (dist < minDist) minDist = dist;
                }
                if (minDist == 0) continue; // don't plan to move onto a chaser

                int threat = computeThreatForSquare(cx, cy);
                int area = computeAreaThreat(cx, cy, computeMaxChaserReach(this.safetyTurns));

                // choose by lowest area, then lowest immediate threat, then highest minDist
                if (area < bestArea || (area == bestArea && (threat < bestThreat || (threat == bestThreat && minDist > bestMinDist)))) {
                    bestArea = area;
                    bestThreat = threat;
                    bestMinDist = minDist;
                    bestX = cx;
                    bestY = cy;
                }
            }
        }

        // If best is current position (no better found), stay
        if (bestX == myX && bestY == myY) {
            return;
        }

        // Attempt to move directly to the planned best position (may traverse multiple steps)
        int prevXForLastPos = myX;
        int prevYForLastPos = myY;
        this.moveToPos(new int[]{bestX, bestY});
        // record last pos to avoid immediate backtracking
        this.lastPos = new int[]{prevXForLastPos, prevYForLastPos};
        this.lastPosCooldown = 1;
    }

    /**
     * Compute a simple threat score for a square: how many chasers can reach (Manhattan) that square within safetyTurns
     * Uses tracked chaserSpeeds but assumes minimum 1 step/turn when speed is not observed.
     */
    private int computeThreatForSquare(int x, int y) {
        if (this.chaserPos == null || this.chaserPos.length == 0) return 0;
        int count = 0;
        for (int i = 0; i < this.chaserPos.length; i++) {
            int[] c = this.chaserPos[i];
            int dist = Math.abs(x - c[0]) + Math.abs(y - c[1]);
            double s = 1.0;
            if (i < this.chaserSpeeds.length) {
                s = this.chaserSpeeds[i];
            }
            if (s < 1.0) s = 1.0; // conservative minimum
            int turnsNeeded = (int) Math.ceil((double) dist / s);
            if (turnsNeeded <= this.safetyTurns) count++;
        }
        return count;
    }

    /**
     * Compute aggregated area threat for a square by summing threats for each cell within Manhattan radius `reach`.
     * This produces a simple score where lower is safer. It is intentionally inexpensive since `reach` is small.
     */
    private int computeAreaThreat(int x, int y, int reach) {
        if (reach < 0) reach = 0;
        int sum = 0;
        for (int dx = -reach; dx <= reach; dx++) {
            int rem = reach - Math.abs(dx);
            for (int dy = -rem; dy <= rem; dy++) {
                int nx = x + dx;
                int ny = y + dy;
                sum += computeThreatForSquare(nx, ny);
            }
        }
        return sum;
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
