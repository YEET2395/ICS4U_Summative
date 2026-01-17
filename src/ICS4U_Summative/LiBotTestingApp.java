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

    private static final int ROLE_VIP = 1;
    private static final int ROLE_GUARD = 2;
    private static final int ROLE_CHASER = 3;

    private static final int IDX_VIP1 = 0;
    private static final int IDX_VIP2 = 1;
    private static final int IDX_GUARD = 2;
    private static final int IDX_GUARD2 = 3;
    private static final int IDX_CHASER1 = 4;
    private static final int IDX_CHASER2 = 5;

    private static final int MIN_X = 1;   // avenue
    private static final int MAX_X = 24;  // avenue
    private static final int MIN_Y = 1;   // street
    private static final int MAX_Y = 13;  // street

    private static final int PAUSE_MS = 0;

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args)
    {

        testA_ProtectMovesCloserWhenFar(newWorld());
        testB_EscortDistanceMaintained(newWorld());
        testC_BlockWhenChaserAdjacentToVIP(newWorld());
        testD_AttackLeashForcesProtect(newWorld());
        testF_RunWhenLowHP(newWorld());
        testG_RunWhenChaserAdjacent(newWorld());
        testH_VIPThreatTieBreakLowerHP(newWorld());
        testI_CornerRunDoesNotCrash(newWorld());
        testJ_EtaFasterChaserChosen(newWorld());
        System.out.printf("RESULT: %d/%d tests passed.%n", testsPassed, testsRun);
    }


    private static LiTestWorld newWorld()
    {
       LiTestWorld w = new LiTestWorld();
       w.setup();
       return w;
    }


    private static void testA_ProtectMovesCloserWhenFar(LiTestWorld w) {
        String name = "Test A - Protect moves closer when Guard is far";


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


    private static void testB_EscortDistanceMaintained(LiTestWorld w) {
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


        assertTrue(name, dist <= 4,
                "dist(G,VIP) should stay <= 3. dist=" + dist);
    }


    private static void testC_BlockWhenChaserAdjacentToVIP(LiTestWorld w) {
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


    private static void testD_AttackLeashForcesProtect(LiTestWorld w) {
        String name = "Test D - Attack leash forces Protect when dist(G,VIP)>4";


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


    private static void testF_RunWhenLowHP(LiTestWorld w) {
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


    private static void testG_RunWhenChaserAdjacent(LiTestWorld w) {
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


    private static void testH_VIPThreatTieBreakLowerHP(LiTestWorld w) {
        String name = "Test H - VIP tie-break chooses lower HP VIP";


        w.setAllCaught();
        w.setBot(IDX_VIP1, 5, 5, 2, false);     // hp=2
        w.setBot(IDX_VIP2, 20, 5, 1, false);    // hp=1 (should be chosen)
        w.setBot(IDX_GUARD, 12, 5, 5, false);


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


    private static void testI_CornerRunDoesNotCrash(LiTestWorld w) {
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


    private static void testJ_EtaFasterChaserChosen(LiTestWorld w) {
        String name = "Test J - ETA chooses faster chaser (speed factor)";

        w.setAllCaught();

        w.setBot(IDX_VIP1, 10, 7, 1, false);

        w.setBot(IDX_GUARD, 14, 7, 5, false);

        w.setBot(IDX_CHASER1, 10, 11, 3, false);

        w.setBot(IDX_CHASER2, 12, 9, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        w.bot(IDX_CHASER1).moveToPos(new int[]{10, 10});
        w.bot(IDX_CHASER2).moveToPos(new int[]{16, 9});

        w.syncAll();
        w.broadcastToGuard();

        int beforeToSlow = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER1).getMyPosition());
        int beforeToFast = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER2).getMyPosition());

        w.guard().takeTurn();
        pause(PAUSE_MS);

        w.syncAll();
        int afterToSlow = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER1).getMyPosition());
        int afterToFast = manhattan(w.bot(IDX_GUARD).getMyPosition(), w.bot(IDX_CHASER2).getMyPosition());

        boolean ok = (afterToFast < beforeToFast) && (afterToSlow > beforeToSlow);

        assertTrue(name, ok,
                "Expected Guard to prioritize faster chaser (smaller ETA). " +
                        "distToFast " + beforeToFast + "->" + afterToFast +
                        " | distToSlow " + beforeToSlow + "->" + afterToSlow);
    }



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
}
