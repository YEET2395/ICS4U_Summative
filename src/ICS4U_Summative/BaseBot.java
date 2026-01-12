package ICS4U_Summative;

import becker.robots.*;

public abstract class BaseBot extends RobotSE {
    public PlayerInfo[] otherRecords = new PlayerInfo[5];
    public PlayerInfo myRecords;
    public final int movesPerTurn;

    /**
     * Constructor for BaseBot
     * @param city City the robot is in
     * @param str Street number
     * @param ave Avenue number
     * @param dir direction the robot is facing
     * @param id identifier of the bot
     * @param role role of the bot
     * @param hp the health of the bot
     * @param movesPerTurn the amount of moves the bot can make in a turn
     * @param dodgeDiff the dodging capability of the bot
     */
    public BaseBot(City city, int str, int ave, Direction dir, int id, int role, int hp, int movesPerTurn, double dodgeDiff)
    {
        super(city, str, ave, dir);
        this.movesPerTurn = movesPerTurn;
        myRecords = new PlayerInfo(id, role, hp, dodgeDiff, new int[] {ave, str}, false);
    }

    /**
     * Updates this bot's enemy records
     * @param records the records to get information from
     */
    abstract public void updateOtherRecords(PlayerInfo[] records);

    /**
     * Initializes this bot's enemy records with only the info they are supposed to know
     * @param records the records to get information from
     */
    abstract public void initRecords(PlayerInfo[] records);

    /**
     * Finds the index of the desired record given the ID
     * @param ID the ID of the robot whose record is desired
     * @return the position of the record in the array
     */
    public int findRecordByID(int ID) {
        for (int i=0; i<this.otherRecords.length; i++) {
            //System.out.println(this.otherRecords[i] + "\n trying to find " + ID); for debugging
            if (this.otherRecords[i].getID() == ID) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get this bot's current position
     * @return an array containing street and avenue number of this bot
     */
    public int[] getMyPosition()
    {
        return new int[] {this.getX(), this.getY()};
    }


    /** @return current X coordinate*/
    public int getX(){
        return this.getAvenue();
    }

    /** @return current Y coordinate*/
    public int getY(){
        return this.getStreet();
    }


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
        int hp = this.myRecords.getHP();
        hp -= amount;

        System.out.println("MY HP IS " + hp);

        //change state if its lost all hp
        if (hp<=0) {
            this.myRecords.updateRecords(hp, this.getMyPosition(), true);
            System.out.println(this.myRecords.getID() + " IS DEAD");
        } else {
            this.myRecords.updateRecords(hp, this.getMyPosition(), false);
        }
    }

    public int getMOVES_PER_TURN()
    {
        return movesPerTurn;
    }

}
