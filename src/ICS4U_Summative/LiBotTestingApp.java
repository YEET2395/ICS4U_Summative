package ICS4U_Summative;

import becker.robots.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Deterministic test harness for LiBot (Guard).
 * - No randomness: fixed positions, HP, moves, dodge.
 * - Only calls guard.takeTurn(); other bots are "static targets".
 * - Prints PASS/FAIL for each boundary test.
 */
public class LiBotTestingApp {

    // ===== roles (match App.java) =====
    private static final int ROLE_VIP = 1;
    private static final int ROLE_GUARD = 2;
    private static final int ROLE_CHASER = 3;

    // ===== IDs / indices (match your App.java layout) =====
    // 0 VIP1 (id 1), 1 VIP2 (id 2), 2 Guard1 (id 3), 3 Guard2 (id 4), 4 Chaser1 (id 5), 5 Chaser2 (id 6)
    private static final int IDX_VIP1 = 0;
    private static final int IDX_VIP2 = 1;
    private static final int IDX_GUARD = 2;     // <-- under test
    private static final int IDX_GUARD2 = 3;
    private static final int IDX_CHASER1 = 4;
    private static final int IDX_CHASER2 = 5;

    // ===== arena bounds (based on your setupPlayground walls) =====
    private static final int MIN_X = 1;   // avenue
    private static final int MAX_X = 24;  // avenue
    private static final int MIN_Y = 1;   // street
    private static final int MAX_Y = 13;  // street

    // Optional: slow down to watch moves (set to 0 for fastest)
    private static final int PAUSE_MS = 0;

    // ===== simple test counters =====
    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {

        // One city + one set of robots; each test repositions them deterministically.
        TestWorld w = new TestWorld();
        w.setup();

        System.out.println("===== LiBot Deterministic Tests =====");

        testA_ProtectMovesCloserWhenFar(w);
        testB_EscortDistanceMaintained(w);
        testC_BlockWhenChaserAdjacentToVIP(w);
        testD_AttackLeashForcesProtect(w);
        testF_RunWhenLowHP(w);
        testG_RunWhenChaserAdjacent(w);
        testH_VIPThreatTieBreakLowerHP(w);
        testI_CornerRunDoesNotCrash(w);

        // Requires your updated LiBot that tracks chaser speed + sorts by ETA
        testJ_EtaFasterChaserChosen(w);

        System.out.println("=====================================");
        System.out.printf("RESULT: %d/%d tests passed.%n", testsPassed, testsRun);
    }

    // =========================================================
    // ======================= TESTS ===========================
    // =========================================================

    /**
     * Test A: Guard far from VIP -> should move closer (Protect behavior).
     */
    private static void testA_ProtectMovesCloserWhenFar(TestWorld w) {
        String name = "Test A - Protect moves closer when Guard is far";

        // Active: VIP1, Guard, Chaser1. Others caught.
        w.setAllCaught();
        w.setBot(IDX_VIP1, 12, 7, 2, false);
        w.setBot(IDX_GUARD, 24, 13, 5, false);
        w.setBot(IDX_CHASER1, 2, 13, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        int before = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_VIP1).getMyPosition());

        w.guard().takeTurn();
        pause(PAUSE_MS);

        w.syncAll();
        int after = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_VIP1).getMyPosition());

        assertTrue(name, after < before,
                "dist(G,VIP) should decrease. before=" + before + " after=" + after);
    }

    /**
     * Test B: Guard already near VIP -> should remain within escort range after turn.
     * (Your LiBot's protect logic enforces escort distance around VIP.)
     */
    private static void testB_EscortDistanceMaintained(TestWorld w) {
        String name = "Test B - Escort distance maintained (<=3)";

        w.setAllCaught();
        w.setBot(IDX_VIP1, 12, 7, 2, false);
        w.setBot(IDX_GUARD, 13, 7, 5, false); // dist=1
        w.setBot(IDX_CHASER1, 20, 7, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        w.guard().takeTurn();
        pause(PAUSE_MS);

        w.syncAll();
        int dist = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_VIP1).getMyPosition());

        // LiBot protect uses ESCORT_DISTANCE+1 (2+1=3) as the hard boundary.
        assertTrue(name, dist <= 3,
                "dist(G,VIP) should stay <= 3. dist=" + dist);
    }

    /**
     * Test C: Chaser is adjacent to VIP (distCV <= 1) -> Guard should enter "block" behavior
     * and still remain within escort range (<=3).
     */
    private static void testC_BlockWhenChaserAdjacentToVIP(TestWorld w) {
        String name = "Test C - Block when chaser adjacent to VIP";

        w.setAllCaught();
        w.setBot(IDX_VIP1, 2, 2, 2, false);
        w.setBot(IDX_GUARD, 3, 2, 5, false);     // near VIP
        w.setBot(IDX_CHASER1, 2, 3, 3, false);   // distCV=1

        w.syncAll();
        w.broadcastToGuard();

        w.guard().takeTurn();
        pause(PAUSE_MS);

        w.syncAll();
        int distGV = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_VIP1).getMyPosition());

        assertTrue(name, distGV <= 3,
                "After blocking, Guard should remain within escort range. dist(G,VIP)=" + distGV);
    }

    /**
     * Test D: Attack leash boundary.
     * If Guard chooses Attack but is too far from VIP (>4), LiBot forces Protect instead.
     * We verify that Guard moves closer to VIP (not chasing chaser).
     */
    private static void testD_AttackLeashForcesProtect(TestWorld w) {
        String name = "Test D - Attack leash forces Protect when dist(G,VIP)>4";

        // Setup so Attack would be attractive, but dist(G,VIP)=5 triggers leash.
        w.setAllCaught();
        w.setBot(IDX_VIP1, 1, 1, 2, false);
        w.setBot(IDX_GUARD, 6, 1, 5, false);      // distGV=5
        w.setBot(IDX_CHASER1, 8, 1, 3, false);    // distGC=2 (attack tempting)

        w.syncAll();
        w.broadcastToGuard();

        int beforeGV = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_VIP1).getMyPosition());

        w.guard().takeTurn();
        pause(PAUSE_MS);

        w.syncAll();
        int afterGV = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_VIP1).getMyPosition());

        assertTrue(name, afterGV < beforeGV,
                "Leash should force Protect -> dist(G,VIP) decreases. before=" + beforeGV + " after=" + afterGV);
    }

    /**
     * Test F: Low HP boundary (hp<=2).
     * Run score gets a big boost; Attack gets a big penalty.
     * We verify Guard increases distance from chaser.
     */
    private static void testF_RunWhenLowHP(TestWorld w) {
        String name = "Test F - Run when low HP (hp<=2)";

        w.setAllCaught();
        w.setBot(IDX_VIP1, 12, 7, 2, false);
        w.setBot(IDX_GUARD, 20, 7, 2, false);    // low HP
        w.setBot(IDX_CHASER1, 19, 7, 3, false);  // adjacent

        w.syncAll();
        w.broadcastToGuard();

        int beforeGC = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER1).getMyPosition());

        w.guard().takeTurn();
        pause(PAUSE_MS);

        w.syncAll();
        int afterGC = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER1).getMyPosition());

        assertTrue(name, afterGC >= beforeGC,
                "With low HP, Guard should not move closer to chaser. before=" + beforeGC + " after=" + afterGC);
    }

    /**
     * Test G: Chaser adjacent (distGC<=1) boundary.
     * Run gets +15 and Attack gets -20, so Guard should prefer Run (or at least not Attack).
     */
    private static void testG_RunWhenChaserAdjacent(TestWorld w) {
        String name = "Test G - Run when chaser adjacent (distGC<=1)";

        w.setAllCaught();
        w.setBot(IDX_VIP1, 1, 13, 2, false);
        w.setBot(IDX_GUARD, 10, 7, 5, false);
        w.setBot(IDX_CHASER1, 10, 8, 3, false); // distGC=1

        w.syncAll();
        w.broadcastToGuard();

        int beforeGC = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER1).getMyPosition());

        w.guard().takeTurn();
        pause(PAUSE_MS);

        w.syncAll();
        int afterGC = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER1).getMyPosition());

        assertTrue(name, afterGC >= beforeGC,
                "When adjacent, Guard should not reduce dist(G,C). before=" + beforeGC + " after=" + afterGC);
    }

    /**
     * Test H: Two VIPs have equal threat distance, but different HP.
     * LiBot should pick the lower-HP VIP.
     * We verify Guard moves more toward VIP2 (hp=1) than VIP1 (hp=2) when threats tie.
     */
    private static void testH_VIPThreatTieBreakLowerHP(TestWorld w) {
        String name = "Test H - VIP tie-break chooses lower HP VIP";

        // Active: 2 VIPs, Guard, 2 Chasers
        w.setAllCaught();
        w.setBot(IDX_VIP1, 5, 5, 2, false);     // hp=2
        w.setBot(IDX_VIP2, 20, 5, 1, false);    // hp=1 (should be chosen)
        w.setBot(IDX_GUARD, 12, 5, 5, false);

        // Tie threat: each VIP has a chaser at distance 2
        w.setBot(IDX_CHASER1, 5, 7, 3, false);   // dist to VIP1 = 2
        w.setBot(IDX_CHASER2, 20, 7, 3, false);  // dist to VIP2 = 2

        w.syncAll();
        w.broadcastToGuard();

        int beforeToVIP1 = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_VIP1).getMyPosition());
        int beforeToVIP2 = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_VIP2).getMyPosition());

        w.guard().takeTurn();
        pause(PAUSE_MS);

        w.syncAll();
        int afterToVIP1 = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_VIP1).getMyPosition());
        int afterToVIP2 = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_VIP2).getMyPosition());

        int delta1 = beforeToVIP1 - afterToVIP1; // positive = moved closer
        int delta2 = beforeToVIP2 - afterToVIP2;

        assertTrue(name, delta2 > delta1,
                "Guard should move more toward VIP2 (hp=1). " +
                        "deltaVIP1=" + delta1 + " deltaVIP2=" + delta2 +
                        " | beforeVIP2=" + beforeToVIP2 + " afterVIP2=" + afterToVIP2);
    }

    /**
     * Test I: Corner boundary. Guard at (1,1) with chaser nearby.
     * Should not crash / go out of bounds.
     */
    private static void testI_CornerRunDoesNotCrash(TestWorld w) {
        String name = "Test I - Corner run does not crash / out of bounds";

        w.setAllCaught();
        w.setBot(IDX_VIP1, 3, 3, 2, false);
        w.setBot(IDX_GUARD, 1, 1, 5, false);       // corner
        w.setBot(IDX_CHASER1, 1, 2, 3, false);     // adjacent

        w.syncAll();
        w.broadcastToGuard();

        w.guard().takeTurn();
        pause(PAUSE_MS);

        w.syncAll();
        int[] g = w.bot(IDX_GUARD).getMyPosition();

        boolean inBounds = (g[0] >= MIN_X && g[0] <= MAX_X && g[1] >= MIN_Y && g[1] <= MAX_Y);
        assertTrue(name, inBounds, "Guard must stay within bounds. pos=" + Arrays.toString(g));
    }

    /**
     * Test J: Speed/ETA boundary (requires LiBot ETA sorting).
     * Two chasers have same distance to VIP, but different observed speed.
     * Faster chaser should have smaller ETA => chosen as threat.
     *
     * We make Guard likely choose ATTACK, so we can infer which chaser was chosen by
     * whether Guard moves closer to that faster chaser after takeTurn().
     */
    private static void testJ_EtaFasterChaserChosen(TestWorld w) {
        String name = "Test J - ETA chooses faster chaser (speed factor)";

        // Active: VIP1, Guard, 2 Chasers
        w.setAllCaught();
        w.setBot(IDX_VIP1, 1, 1, 2, false);

        // Guard at distance 4 from VIP => leash not triggered, but Protect reduced a bit.
        w.setBot(IDX_GUARD, 5, 1, 5, false);

        // Place two chasers with same distance to VIP.
        // Chaser1 (slow) starts at (7,1)  -> later move by 1
        // Chaser2 (fast) starts at (5,3)  -> later move by 4
        w.setBot(IDX_CHASER1, 7, 1, 3, false);
        w.setBot(IDX_CHASER2, 5, 3, 3, false);

        // Snapshot 0: initialize guard tracking (speed = 0 so far)
        w.syncAll();
        w.broadcastToGuard();

        // Move chasers deterministically to create observed speeds:
        // slow: +1 step (y + 1)
        w.bot(IDX_CHASER1).moveToPos(new int[]{7, 2});
        // fast: +4 steps (x + 4)
        w.bot(IDX_CHASER2).moveToPos(new int[]{9, 3});

        // Snapshot 1: update records + guard tracking => speedObs computed
        w.syncAll();
        w.broadcastToGuard();

        int beforeToSlow = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER1).getMyPosition());
        int beforeToFast = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER2).getMyPosition());

        w.guard().takeTurn();
        pause(PAUSE_MS);

        w.syncAll();
        int afterToSlow = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER1).getMyPosition());
        int afterToFast = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER2).getMyPosition());

        // Expect Guard to move closer to FAST chaser (smaller ETA => chosen threat)
        boolean ok = (afterToFast < beforeToFast) && (afterToFast <= afterToSlow);

        assertTrue(name, ok,
                "Expected Guard to prioritize faster chaser (smaller ETA). " +
                        "distToFast " + beforeToFast + "->" + afterToFast +
                        " | distToSlow " + beforeToSlow + "->" + afterToSlow);
    }

    // =========================================================
    // =================== TEST UTILITIES =======================
    // =========================================================

    private static int manhattan(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    private static void assertTrue(String testName, boolean cond, String detail) {
        testsRun++;
        if (cond) {
            testsPassed++;
            System.out.println("[PASS] " + testName);
        } else {
            System.out.println("[FAIL] " + testName + " :: " + detail);
        }
    }

    private static void pause(int ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    // =========================================================
    // ==================== TEST WORLD ==========================
    // =========================================================

    private static class TestWorld {
        private City city;
        private BaseBot[] bots = new BaseBot[6];
        private PlayerInfo[] infos = new PlayerInfo[6];

        public void setup() {
            city = new City();
            setupPlayground(city);

            // Create robots with deterministic default stats (we will reposition per test)
            // Constructor signature: (City, str(y), ave(x), dir, id, role, hp, movesPerTurn, dodgeDiff)

            bots[IDX_VIP1]   = new XiongBot(city, 1, 1, Direction.SOUTH, 1, ROLE_VIP, 2, 2, 0.50);
            bots[IDX_VIP2]   = new XiongBot(city, 1, 2, Direction.SOUTH, 2, ROLE_VIP, 2, 2, 0.50);

            bots[IDX_GUARD]  = new LiBot(city, 2, 1, Direction.NORTH, 3, ROLE_GUARD, 5, 3, 0.50);
            bots[IDX_GUARD2] = new LiBot(city, 2, 2, Direction.NORTH, 4, ROLE_GUARD, 5, 3, 0.50);

            bots[IDX_CHASER1]= new KureshyBot(city, 3, 1, Direction.NORTH, 5, ROLE_CHASER, 3, 4, 0.50);
            bots[IDX_CHASER2]= new KureshyBot(city, 3, 2, Direction.NORTH, 6, ROLE_CHASER, 3, 4, 0.50);

            // Create master records
            for (int i = 0; i < bots.length; i++) {
                BaseBot b = bots[i];
                infos[i] = new PlayerInfo(
                        b.myRecords.getID(),
                        b.myRecords.getRole(),
                        b.myRecords.getHP(),
                        b.myRecords.getDodgeDifficulty(),
                        b.getMyPosition(),
                        b.myRecords.getState()
                );
            }

            // Default: mark all caught initially (tests will enable what they need)
            setAllCaught();
            syncAll();
        }

        public BaseBot bot(int idx) {
            return bots[idx];
        }

        public LiBot guard() {
            return (LiBot) bots[IDX_GUARD];
        }

        public void setAllCaught() {
            for (int i = 0; i < bots.length; i++) {
                setCaught(i, true);
            }
        }

        public void setCaught(int idx, boolean caught) {
            BaseBot b = bots[idx];
            // keep hp as-is, just update caught status
            int hp = b.myRecords.getHP();
            b.myRecords.updateRecords(hp, b.getMyPosition(), caught);
        }

        /**
         * Reposition + set HP + set caught state (deterministic).
         * x=avenue, y=street
         */
        public void setBot(int idx, int x, int y, int hp, boolean caught) {
            BaseBot b = bots[idx];
            b.moveToPos(new int[]{x, y});
            b.myRecords.updateRecords(hp, b.getMyPosition(), caught);
        }

        /**
         * Sync BOTH:
         * - each bot.myRecords position (so it's consistent)
         * - master infos[] records for LiBot decision-making
         */
        public void syncAll() {
            for (int i = 0; i < bots.length; i++) {
                BaseBot b = bots[i];
                // keep existing hp/state, update position to current
                int hp = b.myRecords.getHP();
                boolean caught = b.myRecords.getState();
                b.myRecords.updateRecords(hp, b.getMyPosition(), caught);

                infos[i].updateRecords(hp, b.getMyPosition(), caught);
            }
        }

        /**
         * Only broadcast records to the Guard under test.
         * (Avoids needing to initRecords() on chasers to prevent NullPointer issues.)
         */
        public void broadcastToGuard() {
            guard().updateOtherRecords(infos);
        }

        // Same walls as your App.setupPlayground()
        private void setupPlayground(City playground) {
            playground.setSize(1500, 900);

            for (int i = 1; i <= 13; i++) {
                new Wall(playground, i, 0, Direction.EAST);
                new Wall(playground, i, 25, Direction.WEST);
            }
            for (int i = 1; i <= 24; i++) {
                new Wall(playground, 0, i, Direction.SOUTH);
                new Wall(playground, 14, i, Direction.NORTH);
            }
        }
    }
}
