package ICS4U_Summative;

import becker.robots.*;
import java.util.Random;

/**
 * Application class for the summative project
 * @author Aadil Kureshy, Austin Xiong,  Xinran Li
 * @version 2025 12 30
 */
public class SummativeAppClass {

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

//    /**
//     * Updates the robots copy of their records
//     * @param array the array of BaseBots
//     * @param publicRecords the records containing the info for the Guards/VIP
//     * @param chaserRecords the records containing the info for the Chasers
//     */
//    public void updateBotRecords(BaseBot[] array, playerInfo[] publicRecords, playerInfo[] chaserRecords) {
//        //iterate through the robot array
//        for (int i=0; i<array.length; i++) {
//
//            //check if the robot is a VIP/Guard; otherwise give chaser records
//            if (array[i].getMyRole() != 3) {
//                array[i].setRecords(publicRecords);
//            } else {
//                array[i].setRecords(chaserRecords);
//            }
//
//        }
//    }

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
     * @param role the role of desired robots
     * @return the x,y coordinates of the robots of the specified role
     */
    public static int[][] getPosOfRole(playerInfo[] records, int numRobots, int role) {
        int[][] robotPos = new int[numRobots][2];
        int count = 0; //robotPos index

        //iterate through records
        for (int i=0; i<records.length; i++) {

            //checks role
            if (records[i].getRole() == role) {
                robotPos[count] = records[i].getPosition();
                count++;
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

    public static void main(String[] args){

        // Constants
        final int MAX_TURNS = 30;
        final int MIN_TURNS = 25;
        final int MAX_STREET = 13;
        final int MAX_AVENUE = 24;

        // Initialize city, robots, and other objects
        City playground = new City();
        Random rand = new Random();

        // Set up the playground
        setupPlayground(playground);

        // Initialize robots, 2 bots for each role
        KureshyBot[] chasers = new KureshyBot[2];
        chasers[0] = new KureshyBot(playground, rand.nextInt(1, MAX_STREET), rand.nextInt(1, MAX_AVENUE), Direction.NORTH, 1001, 1, 100, rand.nextInt(3, 6), rand.nextDouble(0.7, 0.91));
        chasers[1] = new KureshyBot(playground, rand.nextInt(1, MAX_STREET), rand.nextInt(1, MAX_AVENUE), Direction.NORTH, 1002, 1, 100, rand.nextInt(3, 6), rand.nextDouble(0.7, 0.91));

        LiBot[] guards = new LiBot[2];
        guards[0] = new LiBot(playground, rand.nextInt(1, MAX_STREET), rand.nextInt(1, MAX_AVENUE), Direction.SOUTH, 2001, 2, 100, rand.nextInt(2, 5), rand.nextDouble(0.45, 0.61));
        guards[1] = new LiBot(playground, rand.nextInt(1, MAX_STREET), rand.nextInt(1, MAX_AVENUE), Direction.SOUTH, 2002, 2, 100, rand.nextInt(2, 5), rand.nextDouble(0.45, 0.61));

        XiongBot[] vips = new XiongBot[2];
        vips[0] = new XiongBot(playground, rand.nextInt(1, MAX_STREET), rand.nextInt(1, MAX_AVENUE), Direction.NORTH, 3001, 3, 100, rand.nextInt(1, 4), rand.nextDouble(0.3, 0.41));
        vips[1] = new XiongBot(playground, rand.nextInt(1, MAX_STREET), rand.nextInt(1, MAX_AVENUE), Direction.NORTH, 3002, 3, 100, rand.nextInt(1, 4), rand.nextDouble(0.3, 0.41));

        // Set up basic statistics for game loop
        int totalTurns = rand.nextInt(MIN_TURNS, MAX_TURNS + 1);

        // Start main game loop
        for(int i = 0; i < totalTurns; i++)
        {
            if(i % 3 == 0)
            {
                // Chasers take their turn
                for (KureshyBot chaser : chasers)
                {
                    chaser.takeTurn();
                }
            }
            else if(i % 3 == 1)
            {

                // VIPs take their turn
                for (XiongBot vip : vips)
                {
                    vip.takeTurn();
                }
            }
            else
            {
                // Guards take their turn
                for (LiBot guard : guards)
                {
                    guard.takeTurn();
                }
            }
        }
    }
}
