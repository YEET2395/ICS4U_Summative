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
    private int[][] chaserPos = new int[3][3];
    private int movesPerTurn;
    // track chaser speeds: stores the speed (manhattan distance) observed last round for each chaser
    private double[] chaserSpeeds = new double[0];
    // store previous position (one round) to calculate speed and direction
    private int[][] chaserPrevPos = new int[0][2];


    // (removed cross-turn backtracking guard - using grid-based move planning externally)

    // how many future turns the bot wants to be sure chasers can't reach it in
    private final int safetyTurns = 3;

    // Revival safety thresholds
    private final int REVIVE_THREAT_THRESHOLD = 5; // maximum allowed threat value to attempt revival
    private final int REVIVE_MIN_CHASER_DIST = 2; // minimum Manhattan distance from any chaser to target to attempt revival

    // Fixed world bounds (matches App.setupPlayground walls): avenues [1..24], streets [1..13]
    private static final int WORLD_MIN_X = 1;  // avenue
    private static final int WORLD_MAX_X = 24;
    private static final int WORLD_MIN_Y = 1;  // street
    private static final int WORLD_MAX_Y = 13;

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
        // Populate BaseBot.otherRecords with all records except self so VIP can inspect caught allies
        int idx = 0;
        for (int i = 0; i < records.length; i++) {
            if (records[i].getID() == this.myRecords.getID()) continue;
            if (idx < this.otherRecords.length) {
                this.otherRecords[idx] = records[i];
                idx++;
            }
        }

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
        // On initialization, populate otherRecords so takeTurn() can operate immediately
        int idx = 0;
        for (int i = 0; i < records.length; i++) {
            if (records[i].getID() == this.myRecords.getID()) continue;
            if (idx < this.otherRecords.length) {
                this.otherRecords[idx] = records[i];
                idx++;
            }
        }

        // On init, also collect chaser coords for internal tracking
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

    // Helper: compute minimum Manhattan distance from any known chaser to the given tile
    private int minChaserDistanceTo(int x, int y) {
        if (this.chaserPos == null || this.chaserPos.length == 0) return Integer.MAX_VALUE;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < this.chaserPos.length; i++) {
            int[] c = this.chaserPos[i];
            if (c == null) continue;
            int d = Math.abs(x - c[0]) + Math.abs(y - c[1]);
            if (d < min) min = d;
        }
        return min;
    }

    public void takeTurn() {
        int movesAllowed = this.movesPerTurn;
        // single-per-turn planning: compute the best destination reachable within `movesAllowed` steps
        int myX = this.getMyPosition()[0];
        int myY = this.getMyPosition()[1];

        // PRIORITY: If there is a caught VIP (other than self), consider rushing to revive it
        int[] reviveTarget = null;
        if (this.otherRecords != null) {
            for (int i = 0; i < this.otherRecords.length; i++) {
                PlayerInfo p = this.otherRecords[i];
                if (p == null) continue;
                if (p.getRole() == 1 && p.getState()) {
                    int[] pos = p.getPosition();
                    if (pos != null) {
                        reviveTarget = new int[]{pos[0], pos[1]};
                        break;
                    }
                }
            }
        }

        if (reviveTarget != null) {
            // Safety checks before committing to revival
            int targetThreat = computeThreatForSquare(reviveTarget[0], reviveTarget[1]);
            int currentThreat = computeThreatForSquare(myX, myY);
            int minChaserDist = minChaserDistanceTo(reviveTarget[0], reviveTarget[1]);

            boolean safeToRevive = (targetThreat <= REVIVE_THREAT_THRESHOLD)
                    && (currentThreat <= REVIVE_THREAT_THRESHOLD)
                    && (minChaserDist >= REVIVE_MIN_CHASER_DIST);

            if (safeToRevive) {
                // Move greedily towards the caught VIP, using up to movesAllowed steps.
                this.moveTowards(reviveTarget[0], reviveTarget[1], movesAllowed);
                return;
            }
            // otherwise do not prioritize revival this turn
        }

        if (this.chaserPos == null || this.chaserPos.length == 0) {
            return; // nothing to plan against
        }

        // Compute conservative bounding box from known positions to avoid evaluating tiles outside walls/map ---
        int minX = myX, maxX = myX, minY = myY, maxY = myY;
        // include chasers
        for (int i = 0; i < this.chaserPos.length; i++) {
            int[] c = this.chaserPos[i];
            if (c == null) continue;
            if (c[0] < minX) minX = c[0];
            if (c[0] > maxX) maxX = c[0];
            if (c[1] < minY) minY = c[1];
            if (c[1] > maxY) maxY = c[1];
        }
        // include guard if available
        if (this.guardPos != null) {
            if (this.guardPos[0] < minX) minX = this.guardPos[0];
            if (this.guardPos[0] > maxX) maxX = this.guardPos[0];
            if (this.guardPos[1] < minY) minY = this.guardPos[1];
            if (this.guardPos[1] > maxY) maxY = this.guardPos[1];
        }
        // add a small margin so nearby edge tiles are still considered
        final int MARGIN = 2;
        minX -= MARGIN;
        minY -= MARGIN;
        maxX += MARGIN;
        maxY += MARGIN;
        // -----------------------------------------------------------------------------------------------

        // Clamp bounding box to the fixed playground interior defined in App.setupPlayground:
        // Vertical walls at avenue 0 and 25, horizontal walls at street 0 and 14, so playable
        // interior is avenues 1..24 and streets 1..13. This ensures we never evaluate tiles outside walls.
        final int WORLD_MIN_X = 1;  // avenue
        final int WORLD_MAX_X = 24;
        final int WORLD_MIN_Y = 1;  // street
        final int WORLD_MAX_Y = 13;
        if (minX < WORLD_MIN_X) minX = WORLD_MIN_X;
        if (maxX > WORLD_MAX_X) maxX = WORLD_MAX_X;
        if (minY < WORLD_MIN_Y) minY = WORLD_MIN_Y;
        if (maxY > WORLD_MAX_Y) maxY = WORLD_MAX_Y;

        // Build candidate list (only candidates that pass feasibility checks get added)
        ArrayList<int[]> candidates = new ArrayList<>(); // each entry: {cx, cy, threat, area, minDist}
        int currentThreat = computeThreatForSquare(myX, myY);

        for (int dx = -movesAllowed; dx <= movesAllowed; dx++) {
            for (int dy = -movesAllowed; dy <= movesAllowed; dy++) {
                int man = Math.abs(dx) + Math.abs(dy);
                if (man == 0 || man > movesAllowed) continue;
                int cx = myX + dx;
                int cy = myY + dy;

                // skip candidate if outside computed bounding box (likely outside walls/map)
                if (cx < minX || cx > maxX || cy < minY || cy > maxY) continue;

                // first-step feasibility check (non-rotating): ensure at least one adjacent
                // neighbor from current position reduces Manhattan distance and lies inside world bounds.
                int dxToTarget = cx - myX;
                int dyToTarget = cy - myY;
                boolean reachableFirstStep = false;
                int curMan = Math.abs(dxToTarget) + Math.abs(dyToTarget);
                if (curMan > 0) {
                    // preferred axis first (check coordinate only, do NOT rotate the robot)
                    if (Math.abs(dxToTarget) >= Math.abs(dyToTarget) && dxToTarget != 0) {
                        Direction pref = dxToTarget > 0 ? Direction.EAST : Direction.WEST;
                        int[] np = nextPos(new int[]{myX, myY}, pref);
                        if (np[0] >= WORLD_MIN_X && np[0] <= WORLD_MAX_X && np[1] >= WORLD_MIN_Y && np[1] <= WORLD_MAX_Y) reachableFirstStep = true;
                        else if (dyToTarget != 0) {
                            Direction alt = dyToTarget > 0 ? Direction.SOUTH : Direction.NORTH;
                            np = nextPos(new int[]{myX, myY}, alt);
                            if (np[0] >= WORLD_MIN_X && np[0] <= WORLD_MAX_X && np[1] >= WORLD_MIN_Y && np[1] <= WORLD_MAX_Y) reachableFirstStep = true;
                        }
                    } else if (dyToTarget != 0) {
                        Direction pref = dyToTarget > 0 ? Direction.SOUTH : Direction.NORTH;
                        int[] np = nextPos(new int[]{myX, myY}, pref);
                        if (np[0] >= WORLD_MIN_X && np[0] <= WORLD_MAX_X && np[1] >= WORLD_MIN_Y && np[1] <= WORLD_MAX_Y) reachableFirstStep = true;
                        else if (dxToTarget != 0) {
                            Direction alt = dxToTarget > 0 ? Direction.EAST : Direction.WEST;
                            np = nextPos(new int[]{myX, myY}, alt);
                            if (np[0] >= WORLD_MIN_X && np[0] <= WORLD_MAX_X && np[1] >= WORLD_MIN_Y && np[1] <= WORLD_MAX_Y) reachableFirstStep = true;
                        }
                    }

                    // fallback: any adjacent coordinate that reduces Manhattan distance and is inside bounds
                    if (!reachableFirstStep) {
                        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                            int[] np = nextPos(new int[]{myX, myY}, d);
                            int newMan = Math.abs(cx - np[0]) + Math.abs(cy - np[1]);
                            if (newMan < curMan && np[0] >= WORLD_MIN_X && np[0] <= WORLD_MAX_X && np[1] >= WORLD_MIN_Y && np[1] <= WORLD_MAX_Y) {
                                reachableFirstStep = true;
                                break;
                            }
                        }
                    }
                }
                if (!reachableFirstStep) continue;

                // avoid candidate squares occupied by chasers
                int minDist = Integer.MAX_VALUE;
                for (int j = 0; j < this.chaserPos.length; j++) {
                    int[] c = this.chaserPos[j];
                    if (c == null) continue;
                    int dist = Math.abs(cx - c[0]) + Math.abs(cy - c[1]);
                    if (dist < minDist) minDist = dist;
                }
                if (minDist == 0) continue; // don't plan to move onto a chaser

                int threat = computeThreatForSquare(cx, cy);
                int area = computeAreaThreat(cx, cy, computeMaxChaserReach(this.safetyTurns));
                // Only consider candidates that are not worse than current
                if (threat > currentThreat) continue;
                // add candidate
                candidates.add(new int[]{cx, cy, threat, area, minDist});
            }
        }

        if (candidates.size() == 0) {
            return; // no viable candidate
        }

        // Enforce policy: only consider candidates at the maximum Manhattan distance
        // from the current position (i.e., always go for the farthest reachable tile).
        int maxMan = 0;
        for (int[] cand : candidates) {
            int man = Math.abs(cand[0] - myX) + Math.abs(cand[1] - myY);
            if (man > maxMan) maxMan = man;
        }
        // Filter to only keep candidates at maxMan
        ArrayList<int[]> farCandidates = new ArrayList<>();
        for (int[] cand : candidates) {
            int man = Math.abs(cand[0] - myX) + Math.abs(cand[1] - myY);
            if (man == maxMan) farCandidates.add(cand);
        }
        // Replace candidates list with farthest-only list
        if (farCandidates.size() > 0) {
            candidates = farCandidates;
        }

        // Sort candidates by threat asc, area asc, minDist desc (selection sort for simplicity)
        for (int i = 0; i < candidates.size() - 1; i++) {
            int bestIdx = i;
            for (int j = i + 1; j < candidates.size(); j++) {
                int[] a = candidates.get(j);
                int[] b = candidates.get(bestIdx);
                if (a[2] < b[2] || (a[2] == b[2] && (a[3] < b[3] || (a[3] == b[3] && a[4] > b[4])))) {
                    bestIdx = j;
                }
            }
            if (bestIdx != i) {
                int[] tmp = candidates.get(i);
                candidates.set(i, candidates.get(bestIdx));
                candidates.set(bestIdx, tmp);
            }
        }

        // Try candidates in order until one produces movement using BaseBot.moveToPos
        for (int[] cand : candidates) {
            int cx = cand[0];
            int cy = cand[1];
            int[] before = this.getMyPosition();
            this.moveToPos(new int[]{cx, cy});
            int[] after = this.getMyPosition();
            if (before[0] != after[0] || before[1] != after[1]) {
                // we moved at least one step
                return;
            }
            // otherwise try next candidate
        }
        // none produced movement; stay
    }

    /**
     * Compute a simple threat score for a square: how many chasers can reach (Manhattan) that square within safetyTurns
     * Uses tracked chaserSpeeds but assumes minimum 1 step/turn when speed is not observed.
     * Each chaser that can reach the square within `safetyTurns` contributes
     * (safetyTurns - turnsNeeded) to the threat (higher means more urgent). If no chaser
     * can reach within safetyTurns the result is 0.
     */
    private int computeThreatForSquare(int x, int y) {
        // Immediate bounds check: if the coordinate is outside the known walled playground,
        // treat it as safe (threat 0) and avoid accessing chaser arrays or doing further work.
        if (x < WORLD_MIN_X || x > WORLD_MAX_X || y < WORLD_MIN_Y || y > WORLD_MAX_Y) return 0;
        if (this.chaserPos == null || this.chaserPos.length == 0) return 0;
        int threat = 0;
        for (int i = 0; i < this.chaserPos.length; i++) {
            int[] c = this.chaserPos[i];
            int dist = Math.abs(x - c[0]) + Math.abs(y - c[1]);
            double s = 1.0;
            if (i < this.chaserSpeeds.length) {
                s = this.chaserSpeeds[i];
            }
            if (s < 1.0) s = 1.0; // conservative minimum
            int turnsNeeded = (int) Math.ceil((double) dist / s);
            if (turnsNeeded <= this.safetyTurns) {
                // contribution: make arrivals exactly at safetyTurns count as 1 unit of threat
                // and closer arrivals contribute proportionally more. This avoids treating
                // borderline arrivals as '0' threat which led to unsafe candidate selection.
                threat += (this.safetyTurns - turnsNeeded + 1);
            }
        }
        return threat;
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
                // Skip cells outside the walled playground
                if (nx < WORLD_MIN_X || nx > WORLD_MAX_X || ny < WORLD_MIN_Y || ny > WORLD_MAX_Y) continue;
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


    // Test whether the adjacent cell in direction 'd' from current position is blocked (wall or obstacle)
    // Note: this checks frontIsClear after turning to `d` from the robot's current facing.
    private boolean isBlockedDir(Direction d) {
        this.turnDirection(d);
        return !this.frontIsClear();
    }

    /**
     * Move up to maxSteps toward (tx,ty) using stepwise moves.
     * Uses greedy dominant-axis preference and avoids revisiting cells during this movement
     * to prevent up/down oscillation near walls. Does not recompute threat while moving.
     */
    private int moveTowards(int tx, int ty, int maxSteps) {
        int curX = this.getX();
        int curY = this.getY();
        int stepsMoved = 0;
        for (int step = 0; step < maxSteps; step++) {
            curX = this.getX();
            curY = this.getY();
            if (curX == tx && curY == ty) break;

            int dx = tx - curX;
            int dy = ty - curY;
            boolean moved = false;

            // Prefer dominant axis
            if (Math.abs(dx) >= Math.abs(dy) && dx != 0) {
                Direction d = dx > 0 ? Direction.EAST : Direction.WEST;
                if (attemptMove(d)) {
                    moved = true;
                    stepsMoved++;
                }
                // fallback to other axis
                if (!moved && dy != 0) {
                    d = dy > 0 ? Direction.SOUTH : Direction.NORTH;
                    if (attemptMove(d)) {
                        moved = true;
                        stepsMoved++;
                    }
                }
            } else if (dy != 0) {
                Direction d = dy > 0 ? Direction.SOUTH : Direction.NORTH;
                if (attemptMove(d)) {
                    moved = true;
                    stepsMoved++;
                }
                if (!moved && dx != 0) {
                    d = dx > 0 ? Direction.EAST : Direction.WEST;
                    if (attemptMove(d)) {
                        moved = true;
                        stepsMoved++;
                    }
                }
            }

            // Try any other direction (N,S,E,W) if still stuck
            if (!moved) {
                Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                for (Direction d : dirs) {
                    if (attemptMove(d)) {
                        moved = true;
                        stepsMoved++;
                        break;
                    }
                }
            }

            if (!moved) {
                // No legal move possible; stop moving further
                break;
            }
        }
        return stepsMoved;
    }


    private int[] nextPos(int[] cur, Direction d) {
        int x = cur[0], y = cur[1];
        if (d == Direction.EAST)  x++;
        if (d == Direction.WEST)  x--;
        if (d == Direction.SOUTH) y++;
        if (d == Direction.NORTH) y--;
        return new int[]{x, y};
    }
}
