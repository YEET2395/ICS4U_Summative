package ICS4U_Summative;

import becker.robots.*;

public abstract class BaseBot extends RobotSE {

    public final String role; // GUARD, VIP, CHASER
    public final int id;
    public final int hp;

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
    public BaseBot(City city, int str, int ave, Direction dir, String role, int id, int hp)
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
        return new int[] {this.getStreet(), this.getAvenue()};
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
