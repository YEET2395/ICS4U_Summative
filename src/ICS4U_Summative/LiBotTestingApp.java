package ICS4U_Summative;

import becker.robots.*;

public class LiBotTestingApp {

    public static final int ROLE_VIP = 1;
    public static final int ROLE_GUARD = 2;
    public static final int ROLE_CHASER = 3;

    public static final int I_VIP1 = 0;
    public static final int I_VIP2 = 1;
    public static final int I_GUARD = 2;
    public static final int I_GUARD2 = 3;
    public static final int I_CHASER1 = 4;
    public static final int I_CHASER2 = 5;

    public static final int MIN_X = 1;
    public static final int MAX_X = 24;
    public static final int MIN_Y = 1;
    public static final int MAX_Y = 13;

    public static final int PAUSE_MS = 0;

    public static int testsRun = 0;
    public static int testsPassed = 0;

    public static void main(String[] args) {

        System.out.println("My Tests: Guard");

        testA(newWorld());
        testB(newWorld());
        testC(newWorld());
        testD(newWorld());
        testF(newWorld());
        testG(newWorld());
        testH(newWorld());
        testI(newWorld());
        testJ(newWorld());

        System.out.println("---------");
        System.out.printf("RESULT: %d/%d tests passed.%n", testsPassed, testsRun);
    }

    private static LiTestWorld newWorld() {
        LiTestWorld w = new LiTestWorld();
        w.setup();
        return w;
    }

    private static void printTestHeader(int number, String boundary, String expectedBehavior) {
        System.out.println();
        System.out.println("---------");
        System.out.println("Test Case " + number);
        System.out.println("What is tested: " + boundary);
        System.out.println("What is expected: " + expectedBehavior);
        System.out.println("---------");
    }

    private static void testA(LiTestWorld w) {
        printTestHeader(
                1,
                "Protect, when Guard's far from VIP (distance should decrease)",
                "Guard should move closer to VIP"
        );

        String name = "Test A -> Protect and moves closer when Guard is far";

        w.setAllCaught();
        w.setBot(I_VIP1, 12, 7, 2, false);
        w.setBot(I_GUARD, 24, 13, 5, false);
        w.setBot(I_CHASER1, 2, 13, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        int before = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_VIP1).getMyPosition());

        w.guard().takeTurn();

        w.syncAll();
        int after = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_VIP1).getMyPosition());

        assertTrue(name, after < before,
                "dist(G,VIP) should decrease. before=" + before + " after=" + after);
    }

    private static void testB(LiTestWorld w) {
        printTestHeader(
                2,
                "Escort distance boundary (Guard should not drift too far from VIP)",
                "Guard should stay within leash distance of VIP (dist(G,VIP) <= 4) after its turn."
        );

        String name = "Test B -> Escort distance is correct (<=4 leash)";

        w.setAllCaught();
        w.setBot(I_VIP1, 12, 7, 2, false);
        w.setBot(I_GUARD, 13, 7, 5, false);
        w.setBot(I_CHASER1, 20, 7, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        w.guard().takeTurn();

        w.syncAll();
        int dist = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_VIP1).getMyPosition());

        assertTrue(name, dist <= 4,
                "dist(G,VIP) should stay <= 4. dist=" + dist);
    }

    private static void testC(LiTestWorld w) {
        printTestHeader(
                3,
                "Immediate threat boundary (Chaser very close to VIP)",
                "Guard should respond by blocking while staying close to VIP (dist(G,VIP) should remain small)."
        );

        String name = "Test C - Block when chaser close to VIP";

        w.setAllCaught();
        w.setBot(I_VIP1, 2, 2, 2, false);
        w.setBot(I_GUARD, 3, 2, 5, false);
        w.setBot(I_CHASER1, 2, 3, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        w.guard().takeTurn();

        w.syncAll();
        int distGV = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_VIP1).getMyPosition());

        assertTrue(name, distGV <= 3,
                "After blocking, Guard should remain in escort range. dist(G,VIP)=" + distGV);
    }

    private static void testD(LiTestWorld w) {
        printTestHeader(
                4,
                "Attack leash boundary (Guard cannot chase when too far from VIP)",
                "Even if attack is tempting, when dist(G,VIP)>4 Guard should move toward VIP (Protect), reducing dist(G,VIP)."
        );

        String name = "Test D - Attack leash forces Protect when dist(G,VIP)>4";

        w.setAllCaught();
        w.setBot(I_VIP1, 1, 1, 2, false);
        w.setBot(I_GUARD, 6, 1, 5, false);
        w.setBot(I_CHASER1, 8, 1, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        int beforeGV = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_VIP1).getMyPosition());

        w.guard().takeTurn();

        w.syncAll();
        int afterGV = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_VIP1).getMyPosition());

        assertTrue(name, afterGV < beforeGV,
                "Leash should force Protect, dist(G,VIP) decreases. before=" + beforeGV + " after=" + afterGV);
    }

    private static void testF(LiTestWorld w) {
        printTestHeader(
                5,
                "Low HP boundary (hp<=2)",
                "Guard should prioritize survival and avoid approaching the chaser (dist(G,Chaser) should not decrease)."
        );

        String name = "Test F - Run when low HP (hp<=2)";

        w.setAllCaught();
        w.setBot(I_VIP1, 12, 7, 2, false);
        w.setBot(I_GUARD, 20, 7, 2, false);
        w.setBot(I_CHASER1, 19, 7, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        int beforeGC = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_CHASER1).getMyPosition());

        w.guard().takeTurn();

        w.syncAll();
        int afterGC = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_CHASER1).getMyPosition());

        assertTrue(name, afterGC >= beforeGC,
                "With low HP, Guard should not move closer to chaser. before=" + beforeGC + " after=" + afterGC);
    }

    private static void testG(LiTestWorld w) {
        printTestHeader(
                6,
                "Close boundary (Chaser adjacent to Guard, dist<=1)",
                "Guard should avoid moving closer to chaser (dist(G,Chaser) should not decrease)."
        );

        String name = "Test G - Run when chaser adjacent (distGC<=1)";

        w.setAllCaught();
        w.setBot(I_VIP1, 1, 13, 2, false);
        w.setBot(I_GUARD, 10, 7, 5, false);
        w.setBot(I_CHASER1, 10, 8, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        int beforeGC = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_CHASER1).getMyPosition());

        w.guard().takeTurn();

        w.syncAll();
        int afterGC = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_CHASER1).getMyPosition());

        assertTrue(name, afterGC >= beforeGC,
                "When adjacent, Guard should not reduce dist(G,C). before=" + beforeGC + " after=" + afterGC);
    }

    private static void testH(LiTestWorld w) {
        printTestHeader(
                7,
                "VIP tie boundary (equal threat, different VIP HP)",
                "When threats are equal, Guard should move more toward the lower-HP VIP."
        );

        String name = "Test H - VIP tie-break chooses lower HP VIP";

        w.setAllCaught();
        w.setBot(I_VIP1, 5, 5, 2, false);
        w.setBot(I_VIP2, 20, 5, 1, false);
        w.setBot(I_GUARD, 12, 5, 5, false);

        w.setBot(I_CHASER1, 5, 7, 3, false);
        w.setBot(I_CHASER2, 20, 7, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        int beforeToVIP1 = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_VIP1).getMyPosition());
        int beforeToVIP2 = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_VIP2).getMyPosition());

        w.guard().takeTurn();

        w.syncAll();
        int afterToVIP1 = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_VIP1).getMyPosition());
        int afterToVIP2 = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_VIP2).getMyPosition());

        int delta1 = beforeToVIP1 - afterToVIP1;
        int delta2 = beforeToVIP2 - afterToVIP2;

        assertTrue(name, delta2 > delta1,
                "Guard should move more toward VIP2 (hp=1). " +
                        "deltaVIP1=" + delta1 + " deltaVIP2=" + delta2 +
                        " / beforeVIP2=" + beforeToVIP2 + " afterVIP2=" + afterToVIP2);
    }

    private static void testI(LiTestWorld w) {
        printTestHeader(
                8,
                "Corner and wall boundary (movement near edge)",
                "Guard should not go out of bounds or crash"
        );

        String name = "Test I - Corner run does not crash / out of bounds";

        w.setAllCaught();
        w.setBot(I_VIP1, 3, 3, 2, false);
        w.setBot(I_GUARD, 1, 1, 5, false);
        w.setBot(I_CHASER1, 1, 2, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        w.guard().takeTurn();

        w.syncAll();
        int[] g = w.bot(I_GUARD).getMyPosition();

        boolean inBounds = (g[0] >= MIN_X && g[0] <= MAX_X && g[1] >= MIN_Y && g[1] <= MAX_Y);
        assertTrue(name, inBounds, "Guard must stay within bounds. pos=" + posToString(g));
    }

    private static String posToString(int[] p)
    {
        return "[" + p[0] + ", " + p[1] + "]";
    }


    private static void testJ(LiTestWorld w) {
        printTestHeader(
                9,
                "Speed factor boundary (ETA = distance/speed)",
                "Guard should prioritize the faster chaser (smaller ETA) after observing movement."
        );

        String name = "Test J - ETA chooses faster chaser (speed factor)";

        w.setAllCaught();

        w.setBot(I_VIP1, 10, 7, 1, false);
        w.setBot(I_GUARD, 14, 7, 5, false);

        w.setBot(I_CHASER1, 10, 11, 3, false);
        w.setBot(I_CHASER2, 12, 9, 3, false);

        w.syncAll();
        w.broadcastToGuard();

        w.bot(I_CHASER1).moveToPos(new int[]{10, 10});
        w.bot(I_CHASER2).moveToPos(new int[]{16, 9});

        w.syncAll();
        w.broadcastToGuard();

        int beforeToSlow = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_CHASER1).getMyPosition());
        int beforeToFast = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_CHASER2).getMyPosition());

        w.guard().takeTurn();

        w.syncAll();
        int afterToSlow = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_CHASER1).getMyPosition());
        int afterToFast = manhattan(w.bot(I_GUARD).getMyPosition(), w.bot(I_CHASER2).getMyPosition());

        boolean ok = (afterToFast < beforeToFast) && (afterToSlow > beforeToSlow);

        assertTrue(name, ok,
                "Expected Guard to prioritize faster chaser (smaller ETA). " +
                        "distToFast " + beforeToFast + "->" + afterToFast +
                        " / distToSlow " + beforeToSlow + "->" + afterToSlow);
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


}
