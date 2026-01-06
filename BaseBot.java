package ICS4U_Summative;

import becker.robots.*;

public abstract class BaseBot extends RobotSE {

    public final int role; // GUARD = 2, VIP = 1, CHASER = 3
    public final int id;

    /**
     * Constructor for BaseBot
     * @param city City the robot is in
     * @param str Street number
     * @param ave Avenue number
     * @param dir direction the robot is facing
     * @param role role of the bot
     * @param id identifier of the bot
     */
    public BaseBot(City city, int str, int ave, Direction dir, int role, int id)
    {
        super(city, str, ave, dir);
        this.role = role;
        this.id = id;
    }

    /**
     * Get this bot's current position
     * @return an array containing street and avenue number of this bot
     */
    public int[] getMyPosition()
    {
        return new int[] {this.getStreet(), this.getAvenue()};
    }

    /**
     * Calculates the grid distance between the robot invoking the method and all other robots (including the invoker)
     * @param records the array of other players
     * @return the distances to each other robot
     */
    public int[] getDistances(playerInfo [] records) {
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
     * Every bot's own turn logic.
     * Application will call this once per turn.
     */
    public abstract void takeTurn();

    /**
     * Turns the robot left until it is facing the specified Direction
     * @param dir the Direction the robot must face
     */
    public void turnDirection(Direction dir) {
        //check for robot's Direction then does the proper turn based on the desired Direction
        if (this.isFacingNorth()) {
            if (dir==Direction.SOUTH) {
                this.turnAround();
            } else if (dir==Direction.EAST) {
                this.turnRight();
            } else if (dir==Direction.WEST) {
                this.turnLeft();
            }
        } else if (this.isFacingSouth()) {
            if (dir==Direction.NORTH) {
                this.turnAround();
            } else if (dir==Direction.EAST) {
                this.turnLeft();
            } else if (dir==Direction.WEST) {
                this.turnRight();
            }
        } else if (this.isFacingEast()) {
            if (dir==Direction.WEST) {
                this.turnAround();
            } else if (dir==Direction.NORTH) {
                this.turnLeft();
            } else if (dir==Direction.SOUTH) {
                this.turnRight();
            }
        } else {
            if (dir==Direction.EAST) {
                this.turnAround();
            } else if (dir==Direction.NORTH) {
                this.turnRight();
            } else if (dir==Direction.SOUTH) {
                this.turnLeft();
            }
        }
    }

}
