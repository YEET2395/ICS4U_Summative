package ICS4U_Summative;
import becker.robots.*;
import java.util.*;

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
    private static void setupPlayground()
    {
        City playground = new City();
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
     * Updates the robots copy of their records
     * @param array the array of BaseBots
     * @param guardRecords the records containing the info for the Guards
     * @param vipRecords the records contianing the info for the VIP(s)
     * @param chaserRecords the records containing the info for the Chasers
     */
    public void updateBotRecords(BaseBot[] array, playerInfo[] guardRecords, playerInfo[] vipRecords, playerInfo[] chaserRecords) {
        //iterate through the robot array
        for (int i=0; i<array.length; i++) {

            //check for the robot's role and distribute records
            if (array[i].getMyRole() == 1) {
                array[i].setRecords(vipRecords);
            } else if (array[i].getMyRole() == 2) {
                array[i].setRecords(guardRecords);
            } else {
                array[i].setRecords(chaserRecords);
            }

        }
    }

    /**
     * Updates the records used by the application/robots
     * @param array the array of BaseBots
     * @param records the array of records to update
     * @param role the type of records to update, with 0 being application records
     */
    public void updateRecords(BaseBot[] array, playerInfo[] records, int role) {
        //iterate through the list of records (each record should match with the BaseBot in array)
        for (int i=0; i<records.length; i++) {

            //checks whether it's updating the application
            if (role == 0) {
                records[i].updateRecords(array[i].getMyHP(), array[i].getMyPosition(), array[i].getMyState());
            } else if (role == 1) { //checks whether it's updating the vip
                records[i].updateRecords(array[i].getMyPosition(), array[i].getMyState());
            } else if (role == 2) { //checks whether it's updating the guard
                records[i].updateRecords(array[i].getMyHP(), array[i].getMyPosition(), array[i].getMyState());
            } else { //updating the chaser records
                records[i].updateRecords(array[i].getMyPosition(), array[i].getMyState());
            }

        }
    }

    public static void main(String[] args){
        setupPlayground();

        playerInfo[] appRecords; //will have all info
        playerInfo[] guardRecords; //will have all info but dodgeDifficulty
        playerInfo[] vipRecords; //will have all info but dodgeDifficulty and hp
        playerInfo[] chaserRecords; //will have all info but dodgeDifficulty, hp, and role
    }
}
