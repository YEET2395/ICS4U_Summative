package ICS4U_Summative;

import becker.robots.*;

public abstract class BaseBot extends RobotSE {

    private final int ROLE; // GUARD = 2, VIP = 1, CHASER = 3
    private final int ID;
    private final int MOVES_PER_TURN;
    private final double DODGE_DIFFICULTY;
    private int hp;
    private boolean isCaught;
    public boolean[] robotCaught;
    //playerInfo[] myRecords;

    /**
     * Constructor for BaseBot
     * @param city City the robot is in
     * @param str Street number
     * @param ave Avenue number
     * @param dir direction the robot is facing
     * @param role role of the bot
     * @param id identifier of the bot
     */
    public BaseBot(City city, int str, int ave, Direction dir, int role, int id, int hp, int movesPerTurn, double dodgeDiff)
    {
        super(city, str, ave, dir);
        this.ROLE = role;
        this.ID = id;
        this.hp = hp;
        this.DODGE_DIFFICULTY = dodgeDiff;
        this.MOVES_PER_TURN = movesPerTurn;
        this.isCaught = false;
    }

    /**
     * Get this bot's current position
     * @return an array containing street and avenue number of this bot
     */
    public int[] getMyPosition()
    {
        return new int[] {this.getX(), this.getY()};
    }

    /**
     * Gets the role of this robot
     * @return the role of this robot
     */
    public int getMyRole() {
        return this.ROLE;
    }

    /**
     * Gets the ID of this robot
     * @return the numerical ID of this robot
     */
    public int getMyID() {
        return this.ID;
    }

    /**
     * Gets the health of this robot
     * @return the health of the robot
     */
    public int getMyHP() {
        return this.hp;
    }

    /**
     * Gets the dodging/catching capability of this robot
     * @return the dodging/catching capability of this robot
     */
    public double getDodgeDifficulty() {
        return this.DODGE_DIFFICULTY;
    }

    /**
     * Gets the array containing the isCaught state of all the other robots
     * @return the array containing the isCaught state of the robots
     */
    public boolean[] getStates() {
        return this.robotCaught;
    }

    public int getMOVES_PER_TURN() {
        return this.MOVES_PER_TURN;
    }

    /**
     * Gets the state of this robot
     * @return whether the robot has been caught or not
     */
    public boolean getMyState() {
        return this.isCaught;
    }

    /** @return current X coordinate*/
    public int getX(){
        return this.getAvenue();
    }

    /** @return current Y coordinate*/
    public int getY(){
        return this.getStreet();
    }

//    /**
//     * Calculates the grid distance between the robot invoking the method and all other robots (including the invoker)
//     * @return the distances to each other robot
//     */
//    public int[] getDistances() {
//        int [] gridDistance = new int[myRecords.length];
//        int[] myCoords = this.getMyPosition();
//        int[] otherCoords;
//
//        //iterate through list of players and calculates the distance to get to each one
//        for (int i=0; i<this.myRecords.length; i++) {
//            otherCoords = myRecords[i].getPosition();
//            gridDistance[i] = Math.abs(myCoords[0] - otherCoords[0]) + Math.abs(myCoords[1] - otherCoords[1]);
//        }
//
//        return gridDistance;
//    }

    /**
     * Gets the distance between the robot invoking it and another location
     * @param point the location to calculate the distance to
     * @return the distance between the robot's current position and the desired location
     */
    public int getDistances(int[] point) {
        int[] myCoords = this.getMyPosition();
        return Math.abs(myCoords[0] - point[0]) + Math.abs(myCoords[1] - point[1]);
    }

    /**
     * Gets the distance between two positions
     * @param point1 the first location
     * @param point2 the second location
     * @return the distance between the two positions
     */
    public int getDistances(int[] point1, int[] point2) {
        return Math.abs(point1[0] - point2[0]) + Math.abs(point1[1] - point2[1]);
    }

    /**
     * Moves robot to the target position
     * @param pos Array of position required to move, format is x,y
     */
    public void moveToPos(int[] pos){
        int x = pos[0];
        int y = pos[1];
        this.moveHorizontal(x);
        this.moveVertical(y);
    }

    /**
     * Moves robot vertically to the target Y
     * @param loc Y location of the place required to move
     */
    private void moveVertical(int loc) {
        // Move north if above target
        if (this.getY()>loc){
            this.turnDirection(Direction.NORTH);
        }
        // Move south if below target
        else if (this.getY()<loc){
            this.turnDirection(Direction.SOUTH);
        }
        // Move while Y value not equal
        while(this.getY()!=loc){
            this.move();
        }
    }

    /**
     * Moves robot vertically to the target X
     * @param loc X location of the place required to move
     */
    private void moveHorizontal(int loc) {
        // We turn accordingly to if point X is left or right of us
        if (this.getX()> loc){
            this.turnDirection(Direction.WEST);
        }
        else if (this.getX()< loc){
            this.turnDirection(Direction.EAST);
        }
        // Move while X value not equal
        while(this.getX() != loc){
            this.move();
        }
    }


    /**
     * Every bot's own turn logic.
     * Application will call this once per turn.
     */
    public abstract void takeTurn();

    /**
     * Turns the robot left until it is facing the specified Direction
     * @param dir the Direction the robot must face
     */
    public void turnDirection(Direction dir)
    {
        //check for robot's Direction then does the proper turn based on the desired Direction
        for(int i = 0; i < 4; i++)
        {
            if(this.getDirection() == dir)
            {
                break;
            }
            this.turnLeft();
        }
    }

    /**
     * Reduces this bot's hp by the specified amount
     * @param amount the amount of hp to reduce
     */
    public void takeDamage(int amount)
    {
        this.hp -= amount;

        //change state if its lost all hp
        if (this.hp<=0) {
            this.isCaught = true;
        }
    }

//    /**
//     * Updates the personal records of this robot
//     * @param records the new records
//     */
//    public void setRecords(playerInfo[] records) {
//        this.myRecords = records;
//    }
//
//    /**
//     * For use of subclasses to get the personal records of this robot
//     * @return the personal records of this robot
//     */
//    public playerInfo[] getRecords() {
//        return this.myRecords;
//    }
//
//    /**
//     * Gets the record of a robot given an ID
//     * @param ID the ID of the desired robot
//     * @return the record of the desired robot
//     */
//    public playerInfo getRobotInfo(int ID) {
//        //initially sets targetInfo as you
//        playerInfo targetInfo = getRobotInfo(this.ID);
//
//        //iterates through the robot's records
//        for (int i=0; i<this.myRecords.length; i++) {
//
//            //checks if the target ID and record ID match
//            if (myRecords[i].getID() == ID) {
//                targetInfo = myRecords[i];
//            }
//        }
//
//        return targetInfo;
//    }
}
