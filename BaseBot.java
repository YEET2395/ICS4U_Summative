package ICS4U_Summative;

import becker.robots.*;

public abstract class BaseBot extends Robot {

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

}
