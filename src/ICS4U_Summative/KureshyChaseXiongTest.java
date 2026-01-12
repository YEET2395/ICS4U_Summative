package ICS4U_Summative;

import becker.robots.*;
import java.util.*;

public class KureshyChaseXiongTest {

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

    public static void updateRecords(BaseBot[] array, PlayerInfo[] records) {
        for (int i=0; i<records.length; i++) {
            records[i].updateRecords(array[i].myRecords.getHP(), array[i].getMyPosition(), array[i].myRecords.getState());
        }
    }

    // Simulate the dodge/catch resolution when a chaser collides with a target
    public static void checkDodge(KureshyBot chaser, BaseBot target, PlayerInfo[] infos, Random r) {
        double diff = r.nextDouble();
        System.out.format("Collision: Chaser %d at %s -- Target %d at %s -- Difficulty: %.2f\n",
                chaser.myRecords.getID(), Arrays.toString(chaser.getMyPosition()),
                target.myRecords.getID(), Arrays.toString(target.getMyPosition()), diff);

        if (chaser.myRecords.getDodgeDifficulty() >= diff && target.myRecords.getDodgeDifficulty() >= diff) {
            // both dodged -> no damage
            System.out.println("Both dodged.");
        } else if (chaser.myRecords.getDodgeDifficulty() >= diff && target.myRecords.getDodgeDifficulty() < diff) {
            // chaser dodged (irrelevant) -> target takes damage
            target.takeDamage(1);
            System.out.println("Target failed to dodge and takes 1 damage.");
        } else {
            // chaser failed to dodge -> both take damage
            chaser.takeDamage(1);
            target.takeDamage(1);
            System.out.println("Chaser failed; both take damage.");
        }

        // update infos from the bots' internal records
        updateRecords(new BaseBot[] { /* placeholder, caller will update after */ }, infos);
    }

    public static void main(String[] args) {
        City playground = new City();
        setupPlayground(playground);

        // create six robots: five Xiong (targets) and one Kureshy (chaser)
        BaseBot[] robots = new BaseBot[6];
        PlayerInfo[] infos = new PlayerInfo[6];

        // deterministic positions for the 5 XiongBots
        int baseRow = 7;
        int baseCol = 6;
        for (int i = 0; i < 5; i++) {
            int row = baseRow;
            int col = baseCol + i; // spread them horizontally
            robots[i] = new XiongBot(playground, row, col, Direction.SOUTH, i, 1, 2, 2, 0.35);
            infos[i] = new PlayerInfo(i, 1, 2, 0.35, new int[]{row, col}, false);
        }

        // KureshyBot: the chaser at a fixed start
        int kRow = 1; int kCol = 1;
        robots[5] = new KureshyBot(playground, kRow, kCol, Direction.NORTH, 5, 3, 3, 4, 0.8);
        infos[5] = new PlayerInfo(5, 3, 3, 0.8, new int[]{kRow, kCol}, false);

        // init records for all robots
        for (BaseBot bot : robots) {
            bot.initRecords(infos);
        }

        // run deterministic turns
        int maxTurns = 20;
        for (int t = 0; t < maxTurns; t++) {
            System.out.println("--- TURN " + t + " ---");

            // each robot updates their view and takes their turn
            for (int i=0; i<robots.length; i++) {
                robots[i].updateOtherRecords(infos);
                System.out.format("Robot %d BEFORE: pos=%s hp=%d\n", i, Arrays.toString(infos[i].getPosition()), infos[i].getHP());
                robots[i].takeTurn();
                // update records immediately after each robot's move
                updateRecords(robots, infos);
                System.out.format("Robot %d AFTER: pos=%s hp=%d stateCaught=%b\n", i, Arrays.toString(infos[i].getPosition()), infos[i].getHP(), infos[i].getState());

                // if the robot that just moved is the chaser, check for collisions with VIPs
                if (robots[i] instanceof KureshyBot) {
                    KureshyBot ch = (KureshyBot) robots[i];
                    Random rand = new Random();
                    for (int j = 0; j < 5; j++) {
                        if (!infos[j].getState() && Arrays.equals(ch.getMyPosition(), infos[j].getPosition())) {
                            // collision detected between chaser and VIP j
                            // resolve dodge/catch: use the single Random instance for determinism if desired
                            double diff = rand.nextDouble();
                            System.out.format("Collision: Chaser %d at %s -- Target %d at %s -- Difficulty: %.2f\n",
                                    ch.myRecords.getID(), Arrays.toString(ch.getMyPosition()), infos[j].getID(), Arrays.toString(infos[j].getPosition()), diff);

                            // apply dodge logic same as original checkDodge
                            if (ch.myRecords.getDodgeDifficulty() >= diff && infos[j].getDodgeDifficulty() >= diff) {
                                System.out.println("Both dodged.");
                            } else if (ch.myRecords.getDodgeDifficulty() >= diff && infos[j].getDodgeDifficulty() < diff) {
                                // target takes damage
                                robots[j].takeDamage(1);
                                System.out.println("Target failed to dodge and takes 1 damage.");
                            } else {
                                // both take damage
                                ch.takeDamage(1);
                                robots[j].takeDamage(1);
                                System.out.println("Chaser failed; both take damage.");
                            }

                            // update infos immediately after resolution
                            updateRecords(robots, infos);
                        }
                    }
                }
            }

            // small pause so the UI can update if running with graphics
            try { Thread.sleep(200); } catch (InterruptedException e) { }

            // check if either is caught
            boolean someoneCaught = false;
            for (PlayerInfo p : infos) if (p.getState()) someoneCaught = true;
            if (someoneCaught) {
                System.out.println("A robot has been caught. Ending test.");
                break;
            }
        }

        System.out.println("Test finished.");
    }
}
