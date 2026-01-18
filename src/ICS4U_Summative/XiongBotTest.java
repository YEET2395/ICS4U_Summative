package ICS4U_Summative;

import becker.robots.*;
import java.awt.*;
import java.util.*;

/**
 * Testing class for XiongBot: deterministic scenario harness modeled after KureshyBotTest
 * - Creates a City playground, builds PlayerInfo[] and calls initRecords on robots
 * - Runs several deterministic scenarios that exercise tracking, prediction, revival, and wall-avoidance
 * @version 2026 01 15
 * @author Austin Xiong
 */
public class XiongBotTest {
    public static int testsRun = 0;
    public static int testsPassed = 0;

    /** Helper to print a test header
     *
     * @param title Test title
     * @param boundary what is being tested
     * @param expected what is expected
     */
    private static void printTestHeader(String title, String boundary, String expected) {
        System.out.println();
        System.out.println("---------");
        System.out.println("Test: " + title);
        System.out.println("What is tested: " + boundary);
        System.out.println("What is expected: " + expected);
        System.out.println("---------");
    }

    /**
     * Helper to assert a condition and print pass/fail
     * @param testName name of the test
     * @param cond condition to check
     * @param detail detail message on failure
     */
    private static void assertTrue(String testName, boolean cond, String detail) {
        testsRun++;
        if (cond) {
            testsPassed++;
            System.out.println("[PASS] " + testName);
        } else {
            System.out.println("[FAIL] " + testName + " :: " + detail);
        }
    }

    /**
     * Set up the playground for the robots
     * @author Austin Xiong
     * @version 2025 12 30
     */
    private static void setupPlayground(City playground)
    {
        playground.setSize(1500, 900);
        for(int i = 1; i <= 13; i++)
        {
            new Wall(playground, i, 0, Direction.EAST);
            new Wall(playground, i, 25, Direction.WEST);
        }
        for(int i = 1; i <= 24; i++)
        {
            new Wall(playground, 0, i, Direction.SOUTH);
            new Wall(playground, 14, i, Direction.NORTH);
        }
    }

    /** Build PlayerInfo[] from robots and call initRecords on each robot (mirrors App pattern)
     *
     * @param robots the array of robots
     * @return the built PlayerInfo[] array
     */
    private static PlayerInfo[] buildAndInitRecords(BaseBot[] robots) {
        // The application normally manages 6 robots (2 VIPs, 2 Guards, 2 Chasers).
        // Ensure the PlayerInfo[] has at least that many entries so bots that expect a full
        // roster (e.g., chasers) won't see nulls in their otherRecords arrays.
        int maxId = 0;
        // Loop: find highest assigned ID among robots
        for (int i = 0; i < robots.length; i++) {
            BaseBot b = robots[i];
            if (b != null && b.myRecords != null) {
                int candidate = b.myRecords.getID();
                if (candidate > maxId) {
                    maxId = candidate;
                }
            }
        }
        final int MIN_ROBOTS = 6;
        int size = Math.max(MIN_ROBOTS, maxId);

        PlayerInfo[] infos = new PlayerInfo[size];

        // Fill with safe placeholder records so no nulls exist. Placeholders use role -1.
        for (int i = 0; i < size; i++) {
            infos[i] = new PlayerInfo(i + 1, -1, 0, 0.0, new int[]{1, 1}, false);
        }

        // Overwrite placeholders with real robot info (index by ID-1)
        // Loop: place each robot's PlayerInfo at index (ID-1), expanding array if necessary
        for (int ri = 0; ri < robots.length; ri++) {
            BaseBot b = robots[ri];
            if (b == null || b.myRecords == null) {
                continue;
            }
            int id = b.myRecords.getID();
            if (id <= 0) {
                continue;
            }
            int idx = id - 1;
            if (idx >= infos.length) {
                // resize the infos array (unlikely) — create a larger array and copy
                PlayerInfo[] bigger = new PlayerInfo[idx + 1];
                System.arraycopy(infos, 0, bigger, 0, infos.length);
                for (int j = infos.length; j < bigger.length; j++) {
                    bigger[j] = new PlayerInfo(j + 1, -1, 0, 0.0, new int[]{1, 1}, false);
                }
                infos = bigger;
            }
            infos[idx] = new PlayerInfo(
                    id,
                    b.myRecords.getRole(),
                    b.myRecords.getHP(),
                    b.myRecords.getDodgeDifficulty(),
                    b.getMyPosition(),
                    b.myRecords.getState()
            );
        }

        // Initialize each robot's internal records (mirrors App behaviour)
        // Loop: call initRecords on each robot if present
        for (int i = 0; i < robots.length; i++) {
            BaseBot b = robots[i];
            if (b != null) {
                b.initRecords(infos);
            }
        }

        return infos;
    }

    /**
     * Update PlayerInfo[] records from the current state of the robots
     * @param array the array of robots
     * @param records the array of PlayerInfo records to update
     */
    public static void updateRecords(BaseBot[] array, PlayerInfo[] records) {
        // Match each PlayerInfo to its corresponding robot by ID. This avoids assumptions
        // that the records[] and array[] are the same length or in the same order.
        for (int i = 0; i < records.length; i++) {
            PlayerInfo rec = records[i];
            if (rec == null) {
                continue;
            }

            int id = rec.getID();
            BaseBot match = null;
            // Loop: find the robot with matching ID
            for (int j = 0; j < array.length; j++) {
                BaseBot b = array[j];
                if (b != null && b.myRecords != null && b.myRecords.getID() == id) {
                    match = b;
                    break;
                }
            }
            if (match == null) {
                // no live robot with this ID in the current robots array; skip
                continue;
            }

            rec.updateRecords(match.myRecords.getHP(), match.getMyPosition(), match.myRecords.getState());
            if (match.myRecords.getState()) {
                match.setColor(Color.BLACK);
            }
        }
    }


    /**
     * Calculate Manhattan distance between two positions
     * @param a point a
     * @param b point b
     * @return Manhattan distance
     */
    private static int manhattan(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }


    /** Format predicted position arrays for printing (handles null)
     *
     * @param p predicted position array
     * @return formatted string
     */
    private static String formatPred(int[] p) {
        // Explicit null check and formatted return (no ternary)
        if (p == null) {
            return "null";
        } else {
            return "[" + p[0] + "," + p[1] + "]";
        }
    }


    /**
     * Get coordinates as string for printing
     * @param b the bot
     * @return string of coordinates
     */
    private static String coords(BaseBot b) {
        int[] p = b.getMyPosition();
        return "("+p[0]+","+p[1]+")";
    }

    /**
     * Scenario 1: Speed tracking & prediction — two chasers moving straight lines observed by VIP
     */
    public static void scenarioSpeedPrediction() {
        System.out.println("=== Scenario: Speed Tracking & Prediction ===");
        City playground = new City(); setupPlayground(playground);

        XiongBot vip = new XiongBot(playground, 8, 8, Direction.SOUTH, 1, 1, 2, 1, 0.35);
        KureshyBot ch1 = new KureshyBot(playground, 12, 8, Direction.WEST, 2, 3, 3, 1, 0.6);
        KureshyBot ch2 = new KureshyBot(playground, 12, 12, Direction.WEST, 3, 3, 3, 1, 0.6);

        BaseBot[] robots = new BaseBot[] { vip, ch1, ch2 };
        PlayerInfo[] infos = buildAndInitRecords(robots);

        // initial chaser positions for VIP
        int[][] chPos = new int[][] { {ch1.getX(), ch1.getY()}, {ch2.getX(), ch2.getY()} };
        vip.setChaserPositions(chPos);

        // simulate 4 turns where chasers move west each turn
        boolean sawNonZeroSpeed = false;
        for (int t=0; t<4; t++) {
            System.out.println("Turn " + t + ": vip="+coords(vip)+" ch1="+coords(ch1)+" ch2="+coords(ch2));

            // move chasers deterministically
            ch1.moveToPos(new int[] {ch1.getX(), ch1.getY()-1});
            ch2.moveToPos(new int[] {ch2.getX(), ch2.getY()-1});

            updateRecords(robots, infos);

            // VIP updates (single call — updateOtherRecords sets positions and tracks speeds)
            vip.updateOtherRecords(infos);

            // report speeds and prediction
            for (int i=0;i<2;i++) {
                double s = vip.getChaserSpeed(i);
                int[] p = vip.predictChaserPosition(i, 3);
                System.out.println("  Chaser " + i + " speed="+s+" predicted(3)="+(p==null?"null":p[0]+","+p[1]));
                if (s > 0) sawNonZeroSpeed = true;
            }

            vip.takeTurn();
            updateRecords(robots, infos);
        }
        System.out.println("End scenario: vip="+coords(vip)+" ch1="+coords(ch1)+" ch2="+coords(ch2)+"\n");
        printTestHeader("Speed Tracking & Prediction", "VIP should observe chaser speeds > 0 when they move", "At least one observed chaser speed > 0 in the simulation");
        assertTrue("SpeedPrediction observed movement", sawNonZeroSpeed, "VIP never observed non-zero chaser speed");
    }

    /**
     * Scenario 2: Corner escape — VIP starts near corner with a chaser approaching
     */
    public static void scenarioCornerEscape() {
        System.out.println("=== Scenario: Corner Escape ===");
        City playground = new City(); setupPlayground(playground);

        XiongBot vip = new XiongBot(playground, 2, 2, Direction.EAST, 1, 1, 2, 1, 0.35);
        KureshyBot ch = new KureshyBot(playground, 2, 6, Direction.WEST, 2, 3, 3, 1, 0.6);

        BaseBot[] robots = new BaseBot[] { vip, ch };
        PlayerInfo[] infos = buildAndInitRecords(robots);

        int initialDist = manhattan(vip.getMyPosition(), ch.getMyPosition());
        for (int t=0; t<6; t++) {
            System.out.println("Turn " + t + ": vip="+coords(vip)+" ch="+coords(ch));
            ch.moveToPos(new int[] {ch.getX(), ch.getY()-1});
            updateRecords(robots, infos);
            // VIP updates (single call)
            vip.updateOtherRecords(infos);
            vip.takeTurn();
            updateRecords(robots, infos);
        }
        System.out.println("End scenario: vip="+coords(vip)+" ch="+coords(ch)+"\n");
        int finalDist = manhattan(vip.getMyPosition(), ch.getMyPosition());
        printTestHeader("Corner Escape", "VIP should increase distance from chaser when escaping", "finalDist > initialDist");
        assertTrue("CornerEscape increases separation", finalDist > initialDist, "initialDist="+initialDist+" finalDist="+finalDist);
    }

    /**
     * Scenario 3: Revival flow — one VIP caught, another VIP moves onto tile to revive
     */
    public static void scenarioRevival() {
        System.out.println("=== Scenario: Revival Attempt ===");
        City playground = new City(); setupPlayground(playground);

        XiongBot vipA = new XiongBot(playground, 6, 6, Direction.SOUTH, 1, 1, 2, 1, 0.35);
        XiongBot vipB = new XiongBot(playground, 6, 8, Direction.SOUTH, 2, 1, 0, 1, 0.35); // caught
        KureshyBot ch = new KureshyBot(playground, 6, 10, Direction.WEST, 3, 3, 3, 1, 0.6);

        BaseBot[] robots = new BaseBot[] { vipA, vipB, ch };
        PlayerInfo[] infos = buildAndInitRecords(robots);

        // mark vipB as caught on robot object
        vipB.myRecords.updateRecords(0, vipB.getMyPosition(), true);
        updateRecords(robots, infos);
        System.out.println("Before revival: vipB caught="+infos[1].getState()+" pos="+coords(vipB));

        // move vipA onto vipB
        vipA.moveToPos(vipB.getMyPosition());
        updateRecords(robots, infos);

        // revival logic: if vipA on same tile and vipB caught -> revive
        for (int i=0;i<robots.length;i++) {
            if (infos[i].getRole()!=1) continue;
            if (infos[i].getState()) continue; // must be alive
            for (int j=0;j<robots.length;j++) {
                if (i==j) continue;
                if (infos[j].getRole()!=1) continue;
                if (!infos[j].getState()) continue; // only revive if caught
                if (manhattan(infos[i].getPosition(), infos[j].getPosition())==0) {
                    robots[j].myRecords.updateRecords(1, robots[j].getMyPosition(), false);
                    infos[j].updateRecords(1, robots[j].getMyPosition(), false);
                    robots[j].setColor(Color.GREEN);
                    System.out.println("Revival performed: vipB revived by vipA at " + coords(vipB));
                }
            }
        }
        System.out.println("After revival: vipB caught="+infos[1].getState()+" HP="+infos[1].getHP()+"\n");
        printTestHeader("Revival Attempt", "Moving a VIP onto a caught VIP should revive it", "target VIP state == not caught and HP>0");
        boolean revived = (infos[1].getState() == false) && (infos[1].getHP() > 0);
        assertTrue("Revival performed successfully", revived, "vipB state="+infos[1].getState()+" HP="+infos[1].getHP());
    }


    /**
     * Scenario 4: Wall avoidance — VIP near the left wall should not move off-map
     */
    public static void scenarioWallAvoidance() {
        System.out.println("=== Scenario: Wall Avoidance ===");
        City playground = new City(); setupPlayground(playground);

        XiongBot vip = new XiongBot(playground, 3, 1, Direction.EAST, 1, 1, 2, 1, 0.35);
        KureshyBot ch = new KureshyBot(playground, 3, 4, Direction.WEST, 2, 3, 3, 1, 0.6);

        BaseBot[] robots = new BaseBot[] { vip, ch };
        PlayerInfo[] infos = buildAndInitRecords(robots);

        for (int t=0;t<6;t++) {
            System.out.println("Turn " + t + ": vip="+coords(vip)+" ch="+coords(ch));
            ch.moveToPos(new int[] {ch.getX(), ch.getY()-1});
            updateRecords(robots, infos);
            vip.updateOtherRecords(infos);
            // VIP updates (single call — updateOtherRecords sets positions and tracks speeds)
            vip.updateOtherRecords(infos);
            vip.takeTurn();
            updateRecords(robots, infos);
            int[] p = vip.getMyPosition();
            boolean inside = (p[0]>=1 && p[0]<=24 && p[1]>=1 && p[1]<=13);
            System.out.println("  insideBounds="+inside);
        }
        System.out.println("End scenario: vip="+coords(vip)+"\n");
        printTestHeader("Wall Avoidance", "VIP must stay inside bounds during movement", "VIP position inside world bounds after scenario");
        int[] pfin = vip.getMyPosition();
        boolean inBounds = (pfin[0]>=1 && pfin[0]<=24 && pfin[1]>=1 && pfin[1]<=13);
        assertTrue("WallAvoidance in-bounds", inBounds, "final pos="+formatPred(pfin));
    }


    /**
     * Scenario 5: Prediction edge-case — chaser abruptly changes observed speed (teleport-like), VIP prediction robustness
     */
    public static void scenarioPredictionEdgeCase() {
        System.out.println("=== Scenario: Prediction Edge Case ===");
        City playground = new City(); setupPlayground(playground);

        XiongBot vip = new XiongBot(playground, 10, 10, Direction.SOUTH, 1, 1, 2, 1, 0.35);
        KureshyBot ch = new KureshyBot(playground, 14, 10, Direction.WEST, 2, 3, 3, 1, 0.6);

        BaseBot[] robots = new BaseBot[] { vip, ch };
        PlayerInfo[] infos = buildAndInitRecords(robots);

        // Initialize with current ch position
        int[][] chPos = new int[][] { { ch.getX(), ch.getY() } };
        vip.setChaserPositions(chPos);

        // Turn 0: chaser moves normally (west by 1)
        ch.moveToPos(new int[] { ch.getX(), ch.getY()-1 });
        updateRecords(robots, infos);
        vip.updateOtherRecords(infos);
        // VIP updates (single call — updateOtherRecords sets positions and tracks speeds)
        vip.updateOtherRecords(infos);
        System.out.println("After normal move: ch="+coords(ch)+" speed="+vip.getChaserSpeed(0)+" pred(3)="+formatPred(vip.predictChaserPosition(0,3)));

        // Turn 1: simulate sudden big jump (teleport-like) to test prediction clamping
        ch.moveToPos(new int[] { ch.getX(), ch.getY()-5 }); // big jump
        updateRecords(robots, infos);
        vip.updateOtherRecords(infos);
        // VIP updates (single call — updateOtherRecords sets positions and tracks speeds)
        vip.updateOtherRecords(infos);
        System.out.println("After jump: ch="+coords(ch)+" speed="+vip.getChaserSpeed(0)+" pred(3)="+formatPred(vip.predictChaserPosition(0,3)));

        // Turn 2: chaser slows/stops
        // simulate zero observed movement
        vip.setChaserPositions(new int[][] { { ch.getX(), ch.getY() } });
        vip.trackChaserSpeeds();
        System.out.println("After stop observed: speed="+vip.getChaserSpeed(0)+" pred(3)="+formatPred(vip.predictChaserPosition(0,3)));

        System.out.println("End scenario: vip="+coords(vip)+" ch="+coords(ch)+"\n");
        printTestHeader("Prediction Edge Case", "VIP should have non-negative observed speed after abrupt movements", "getChaserSpeed >= 0");
        assertTrue("PredictionEdgeCase speed non-negative", vip.getChaserSpeed(0) >= 0, "speed="+vip.getChaserSpeed(0));
    }


    /**
     * Scenario 6: Revival chain — demonstrate chained revivals when multiple VIPs are caught
     */
    public static void scenarioRevivalChain() {
        System.out.println("=== Scenario: Revival Chain ===");
        City playground = new City(); setupPlayground(playground);

        XiongBot vipA = new XiongBot(playground, 5, 5, Direction.SOUTH, 1, 1, 2, 1, 0.35);
        XiongBot vipB = new XiongBot(playground, 5, 7, Direction.SOUTH, 2, 1, 0, 1, 0.35); // caught
        XiongBot vipC = new XiongBot(playground, 5, 9, Direction.SOUTH, 3, 1, 0, 1, 0.35); // caught

        BaseBot[] robots = new BaseBot[] { vipA, vipB, vipC };
        PlayerInfo[] infos = buildAndInitRecords(robots);

        // mark vipB and vipC as caught
        vipB.myRecords.updateRecords(0, vipB.getMyPosition(), true);
        vipC.myRecords.updateRecords(0, vipC.getMyPosition(), true);
        updateRecords(robots, infos);

        System.out.println("Before: vipB caught="+infos[1].getState()+" vipC caught="+infos[2].getState());

        // Move vipA onto vipB to revive it
        vipA.moveToPos(vipB.getMyPosition()); updateRecords(robots, infos);
        // perform revival check
        for (int i=0;i<robots.length;i++) {
            if (infos[i].getRole()!=1) continue; if (infos[i].getState()) continue;
            for (int j=0;j<robots.length;j++) {
                if (i==j) continue; if (infos[j].getRole()!=1) continue; if (!infos[j].getState()) continue;
                if (manhattan(infos[i].getPosition(), infos[j].getPosition())==0) {
                    robots[j].myRecords.updateRecords(1, robots[j].getMyPosition(), false);
                    infos[j].updateRecords(1, robots[j].getMyPosition(), false);
                    robots[j].setColor(Color.GREEN);
                    System.out.println("Revived: " + infos[j].getID() + " by " + infos[i].getID());
                }
            }
        }

        // Now move vipB onto vipC to chain-revive vipC (vipB should now be alive)
        vipB.moveToPos(vipC.getMyPosition()); updateRecords(robots, infos);
        for (int i=0;i<robots.length;i++) {
            if (infos[i].getRole()!=1) continue; if (infos[i].getState()) continue;
            for (int j=0;j<robots.length;j++) {
                if (i==j) continue; if (infos[j].getRole()!=1) continue; if (!infos[j].getState()) continue;
                if (manhattan(infos[i].getPosition(), infos[j].getPosition())==0) {
                    robots[j].myRecords.updateRecords(1, robots[j].getMyPosition(), false);
                    infos[j].updateRecords(1, robots[j].getMyPosition(), false);
                    robots[j].setColor(Color.GREEN);
                    System.out.println("Chain revived: " + infos[j].getID() + " by " + infos[i].getID());
                }
            }
        }

        System.out.println("After chain revival states: vipA="+infos[0].getState()+" vipB="+infos[1].getState()+" vipC="+infos[2].getState()+"\n");
    }


    /**
     * Scenario 7: Corner diagonal threat — VIP in corner with speed 3 and chaser on diagonal 1 block away
     */
    public static void scenarioCornerDiagonalSpeed() {
        System.out.println("=== Scenario: Corner Diagonal Threat (VIP speed=3) ===");
        City playground = new City();
        setupPlayground(playground);

        // Place VIP in top-left corner (street 1, avenue 1) with movesPerTurn = 3
        XiongBot vip = new XiongBot(playground, 1, 1, Direction.SOUTH, 1, 1, 2, 3, 0.35);
        // Place chaser diagonally at (2,2) — one block away diagonally
        KureshyBot ch = new KureshyBot(playground, 2, 2, Direction.NORTH, 2, 3, 3, 2, 0.6);

        BaseBot[] robots = new BaseBot[] { vip, ch };
        PlayerInfo[] infos = buildAndInitRecords(robots);

        // Initialize VIP's chaser positions and tracking
        int[][] chPos = new int[][] { { ch.getX(), ch.getY() } };
        vip.setChaserPositions(chPos);
        vip.trackChaserSpeeds();

        // Simulate a few turns to see how VIP uses its speed-3 to escape/corner logic
        int initialDist = manhattan(vip.getMyPosition(), ch.getMyPosition());
        for (int t = 0; t < 1; t++) {
            System.out.println("Turn " + t + ": vip=" + coords(vip) + " ch=" + coords(ch));

            // Ensure PlayerInfo[] reflects the latest robot positions before the chaser decides
            updateRecords(robots, infos);
            ch.updateOtherRecords(infos);

            // Let chaser act (may move toward VIP)
            ch.takeTurn();

            // sync the infos after chaser moved
            updateRecords(robots, infos);
            // VIP updates perception and acts (so it can escape using its speed=3)
            vip.updateOtherRecords(infos);
            vip.takeTurn();
            updateRecords(robots, infos);
        }

        System.out.println("End scenario: vip=" + coords(vip) + " ch=" + coords(ch) + "\n");
        int finalDist = manhattan(vip.getMyPosition(), ch.getMyPosition());
        printTestHeader("Corner Diagonal Threat", "VIP should increase distance from the approaching chaser", "finalDist > initialDist");
        assertTrue("CornerDiagonal increases separation", finalDist > initialDist, "initialDist="+initialDist+" finalDist="+finalDist);
    }

    /**
     * Main method to run all scenarios
     * @param args
     */
    public static void main(String[] args) {
        // Run deterministic XiongBot scenarios
        scenarioSpeedPrediction();
        scenarioCornerEscape();
        scenarioRevival();
        scenarioWallAvoidance();
        scenarioPredictionEdgeCase();
        scenarioRevivalChain();
        scenarioCornerDiagonalSpeed();
        System.out.println("XiongBotTest scenarios complete.");
        System.out.println("---------");
        System.out.printf("RESULT: %d/%d tests passed.%n", testsPassed, testsRun);
    }
}

