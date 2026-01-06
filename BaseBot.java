package ICS4U_Summative;

import becker.robots.*;

public abstract class BaseBot extends RobotSE {

    private final int role; // GUARD = 2, VIP = 1, CHASER = 3
    private final int id;
    private int hp;

    /**
     * Constructor for BaseBot
     * @param city City the robot is in
     * @param str Street number
     * @param ave Avenue number
     * @param dir direction the robot is facing
     * @param role role of the bot
     * @param id identifier of the bot
     * @param hp health points of the bot
     */
    public BaseBot(City city, int str, int ave, Direction dir, int role, int id, int hp)
    {
        super(city, str, ave, dir);
        this.role = role;
        this.id = id;
        this.hp = hp;
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
     * Calculates the grid distance between the robot invoking the method and all other robots (including the invoker)
     * @param records the array of other players
     * @return the distances to each other robot
     */
    private int[] getDistances(playerInfo [] records) {
        int [] gridDistance = new int[records.length-1];
        int[] myCoords = this.getMyPosition();
        int[] otherCoords;
        //iterate through list of players (excludes the robot calling it using ID)
        //and calculates the distance to get to each one
        for (int i=0; i<records.length; i++) {
            otherCoords = records[i].getPosition();
            //check that the robot's position being compared is not the BaseBot invoking the method
            if (this.id != records[i].getID()) {
                gridDistance[i] = Math.abs(myCoords[0] - otherCoords[0]) + Math.abs(myCoords[1] - otherCoords[1]);
            }
        }
        return gridDistance;
    }

    /**
     * Moves robot to the target position
     * @param pos Array of position required to move, format is x,y
     */
    private void moveToPos(int[] pos){
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


}
