package ICS4U_Summative;

import becker.robots.*;
import java.util.*;

public class KureshyBotTest {

    /**
     * Set up the playground for the robots
     * @author Xinran Li
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

    /**
     * Updates the records used by the application
     * @param array the array of BaseBots
     * @param records the array of records to update
     */
    public void updateRecords(BaseBot[] array, playerInfo[] records) {

        //iterate through the list of records (each record should match with the BaseBot in array)
        for (int i=0; i<records.length; i++) {

            //update the info that will change
            records[i].updateRecords(array[i].getMyHP(), array[i].getMyPosition(), array[i].getMyState());
        }

    }

    /**
     * Gets the position of all robots of a specific role from the records
     * @param records the application records
     * @param numRobots the number of robots of that role
     * @param role the role of desired robots (4 counts for both Guards and VIPs)
     * @return the x,y coordinates of the robots of the specified role
     */
    public static int[][] getPosOfRole(playerInfo[] records, int numRobots, int role) {
        int[][] robotPos = new int[numRobots][2];
        int count = 0; //robotPos index

        //iterate through records
        for (int i=0; i<records.length; i++) {

            //for chaser, checks for both guards and VIPs
            if (role==4) {
                if (records[i].getRole() == 1 || records[i].getRole()==2) {
                    robotPos[count] = records[i].getPosition();
                    count++;
                    System.out.println(count);
                }
            } else {
                //checks role
                if (records[i].getRole() == role) {
                    robotPos[count] = records[i].getPosition();
                    count++;
                }
            }

        }

        return robotPos;
    }

    /**
     * Gets the HP of all robots of a role from the records
     * @param records the application records
     * @param numRobots the number of robots of that role
     * @param role the role of desired robots
     * @return the HP of the robots of the specified role
     */
    public static int[] getHPOfRole(playerInfo[] records, int numRobots, int role) {
        int[] robotHP = new int[numRobots];
        int count = 0; //robotHP index

        //iterate through records
        for (int i=0; i<records.length; i++) {

            //checks role
            if (records[i].getRole() == role) {
                robotHP[count] = records[i].getHP();
                count++;
            }
        }

        return robotHP;
    }

    /**
     * Gets the status of all robots from the records
     * @param records the application records
     * @param numRobots the total number of robots
     * @return the isCaught status of all robots with the index corresponding to ID
     */
    public static boolean[] getStates(playerInfo[] records, int numRobots) {
        boolean[] robotStatus = new boolean[numRobots];

        //iterate through records
        for (int i=0; i<records.length; i++) {
            robotStatus[i] = records[i].getState();
        }

        return robotStatus;
    }

    public static void main(String[] args) {
        City playground = new City();
        setupPlayground(playground);
        playground.setSize(1500, 900);

        Random rand = new Random();

        BaseBot[] robots = new BaseBot[5];
        playerInfo[] records = new playerInfo[5];
        int index = 0;

        //create multiple VIPs
        for (int i=0; i<4; i++) {
            robots[i] = new XiongBot(
                    playground,
                    rand.nextInt(13) + 1,
                    rand.nextInt(24) + 1,
                    Direction.SOUTH, index, 1, 2,
                    rand.nextInt(3) + 1,
                    0.3 + rand.nextDouble() * 0.1
            );
            index++;
        }

        System.out.println("Chaser robot is " + index);
        //create chaser
        KureshyBot chaser = new KureshyBot(
                playground,
                rand.nextInt(13) + 1,
                rand.nextInt(24) + 1,
                Direction.NORTH, index, 3, 3,
                rand.nextInt(3) + 3,
                0.7 + rand.nextDouble() * 0.2
        );

        robots[4] = chaser;

        //initialize records
        for (int i=0; i<robots.length; i++) {
            records[i] = new playerInfo(robots[i].getMyID(), robots[i].getMyRole(),
                    robots[i].getMyHP(), robots[i].getMyDodgeDifficulty(),
                    robots[i].getMyPosition(), robots[i].getMyState());
        }


        chaser.initTargeteting(4, 1);
        chaser.sendBotsPos(getPosOfRole(records, 4, 4));
        chaser.sendChasersPos(getPosOfRole(records, 1, 3));
        chaser.sendStates(getStates(records, robots.length));
        chaser.takeTurn();
    }
}
