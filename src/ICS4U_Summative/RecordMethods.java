package ICS4U_Summative;

public class RecordMethods {

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
    public void updateRecords(BaseBot[] array, PlayerInfo[] records) {

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
    public static int[][] getPosOfRole(PlayerInfo[] records, int numRobots, int role) {
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
    public static int[] getHPOfRole(PlayerInfo[] records, int numRobots, int role) {
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
    public static boolean[] getStates(PlayerInfo[] records, int numRobots) {
        boolean[] robotStatus = new boolean[numRobots];

        //iterate through records
        for (int i=0; i<records.length; i++) {
            robotStatus[i] = records[i].getState();
        }

        return robotStatus;
    }
}
