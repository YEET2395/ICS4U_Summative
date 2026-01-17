package ICS4U_Summative;

import becker.robots.*;

import java.util.Arrays;

public class LiBotTestingApp {

    private static final int ROLE_VIP = 1;
    private static final int ROLE_GUARD = 2;
    private static final int ROLE_CHASER = 3;

    private static final int I_VIP1 = 0;
    private static final int I_VIP2 = 1;
    private static final int I_GUARD = 2;
    private static final int I_GUARD2 = 3;
    private static final int I_CHASER1 = 4;
    private static final int I_CHASER2 = 5;

    private static final int MIN_X = 1;
    private static final int MAX_X = 24;
    private static final int MIN_Y = 1;
    private static final int MAX_Y = 13;

    private static final int PAUSE_MS = 0;

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {

        System.out.println("My Tests: Guard");

        testA_ProtectMovesCloserWhenFar(newWorld());
        testB_EscortDistanceMaintained(newWorld());
        testC_BlockWhenChaserAdjacentToVIP(newWorld());
        testD_AttackLeashForcesProtect(newWorld());
        testF_RunWhenLowHP(newWorld());
        testG_RunWhenChaserAdjacent(newWorld());
        testH_VIPThreatTieBreakLowerHP(newWorld());
        testI_CornerRunDoesNotCrash(newWorld());
        testJ_EtaFasterChaserChosen(newWorld());

        System.out.println("---------");
        System.out.printf("RESULT: %d/%d tests passed.%n", testsPassed, testsRun);
    }

    private static TestWorld newWorld() {
        TestWorld w = new TestWorld();
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

    private static void testA_ProtectMovesCloserWhenFar(TestWorld w) {
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

    private static void testB_EscortDistanceMaintained(TestWorld w) {
        printTestHeader(
                2,
                "Escort/leash distance boundary (Guard should not drift too far from VIP)",
                "Guard should stay within leash distance of VIP (dist(G,VIP) <= 4) after its turn."
        );

        String name = "Test B - Escort distance maintained (<=4 leash)";

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

    private static void testC_BlockWhenChaserAdjacentToVIP(TestWorld w) {
        printTestHeader(
                3,
                "Immediate threat boundary (Chaser adjacent to VIP)",
                "Guard should respond by blocking/intercepting while staying close to VIP (dist(G,VIP) should remain small)."
        );

        String name = "Test C - Block when chaser adjacent to VIP";

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
                "After blocking, Guard should remain within escort range. dist(G,VIP)=" + distGV);
    }

    private static void testD_AttackLeashForcesProtect(TestWorld w) {
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
                "Leash should force Protect -> dist(G,VIP) decreases. before=" + beforeGV + " after=" + afterGV);
    }

    private static void testF_RunWhenLowHP(TestWorld w) {
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

    private static void testG_RunWhenChaserAdjacent(TestWorld w) {
        printTestHeader(
                6,
                "Close-quarters boundary (Chaser adjacent to Guard, dist<=1)",
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

    private static void testH_VIPThreatTieBreakLowerHP(TestWorld w) {
        printTestHeader(
                7,
                "VIP tie-break boundary (equal threat, different VIP HP)",
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
                        " | beforeVIP2=" + beforeToVIP2 + " afterVIP2=" + afterToVIP2);
    }

    private static void testI_CornerRunDoesNotCrash(TestWorld w) {
        printTestHeader(
                8,
                "Corner / wall boundary (movement near arena edge)",
                "Guard should not go out of bounds or crash; final position must remain within the arena."
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


    private static void testJ_EtaFasterChaserChosen(TestWorld w) {
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

    private static class TestWorld {
        private City city;
        private BaseBot[] bots = new BaseBot[6];
        private PlayerInfo[] infos = new PlayerInfo[6];

        public void setup() {
            city = new City();
            setupPlayground(city);

            bots[I_VIP1]   = new XiongBot(city, 1, 1, Direction.SOUTH, 1, ROLE_VIP, 2, 2, 0.50);
            bots[I_VIP2]   = new XiongBot(city, 1, 2, Direction.SOUTH, 2, ROLE_VIP, 2, 2, 0.50);

            bots[I_GUARD]  = new LiBot(city, 2, 1, Direction.NORTH, 3, ROLE_GUARD, 5, 3, 0.50);
            bots[I_GUARD2] = new LiBot(city, 2, 2, Direction.NORTH, 4, ROLE_GUARD, 5, 3, 0.50);

            bots[I_CHASER1]= new KureshyBot(city, 3, 1, Direction.NORTH, 5, ROLE_CHASER, 3, 4, 0.50);
            bots[I_CHASER2]= new KureshyBot(city, 3, 2, Direction.NORTH, 6, ROLE_CHASER, 3, 4, 0.50);

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

            setAllCaught();
            syncAll();
        }

        public BaseBot bot(int idx) {
            return bots[idx];
        }

        public LiBot guard() {
            return (LiBot) bots[I_GUARD];
        }

        public void setAllCaught() {
            for (int i = 0; i < bots.length; i++) {
                setCaught(i, true);
            }
        }

        public void setCaught(int idx, boolean caught) {
            BaseBot b = bots[idx];
            int hp = b.myRecords.getHP();
            b.myRecords.updateRecords(hp, b.getMyPosition(), caught);
        }

        public void setBot(int idx, int x, int y, int hp, boolean caught) {
            BaseBot b = bots[idx];
            b.moveToPos(new int[]{x, y});
            b.myRecords.updateRecords(hp, b.getMyPosition(), caught);
        }

        public void syncAll() {
            for (int i = 0; i < bots.length; i++) {
                BaseBot b = bots[i];
                int hp = b.myRecords.getHP();
                boolean caught = b.myRecords.getState();
                b.myRecords.updateRecords(hp, b.getMyPosition(), caught);
                infos[i].updateRecords(hp, b.getMyPosition(), caught);
            }
        }

        public void broadcastToGuard() {
            guard().updateOtherRecords(infos);
        }

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
