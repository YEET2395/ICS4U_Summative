package ICS4U_Summative;

/**
 * Application controlled records tracking each robot
 * @author Aadil Kureshy, Austin Xiong,  Xinran Li
 * @version 2026 1 5
 */
public class PlayerInfo {
    private final int ID;
    private final int ROLE;
    private int hp;
    private final double DODGE_DIFFICULTY;
    private int[] position;
    private boolean isCaught;

    /**
     * Constructor for playerInfo
     * @param ID the numerical ID of the robot
     * @param role the role of the robot
     * @param hp the health of the robot
     * @param dodgeDifficulty the dodge/catch capability of the robot (higher is better)
     * @param pos the x,y coordinates of the robot
     * @param isCaught the state of the robot
     */
    public PlayerInfo(int ID, int role, int hp, double dodgeDifficulty, int[] pos, boolean isCaught) {
        this.ID = ID;
        this.ROLE = role;
        this.hp = hp;
        this.DODGE_DIFFICULTY = dodgeDifficulty;
        this.position = pos;
        this.isCaught = isCaught;
    }

    /**
     * Get the ID number of the robot
     * @return the ID number of the robot
     */
    public int getID() {
        return this.ID;
    }

    /**
     * Get the role of the robot
     * @return the role of the robot
     */
    public int getRole() {
        return this.ROLE;
    }

    /**
     * Get the health of the robot
     * @return the health of the robot
     */
    public int getHP() {
        return this.hp;
    }

    /**
     * REDUNDANT METHOD BECAUSE OF updateRecords()
     * Sets the new health of the robot and updates the state of the robot
     */
    public void setHP(int newHP) {
        this.hp = newHP;
        //for redundancy
        if (this.hp == 0) {
            this.isCaught = true;
        }
    }

    /**
     * The catch/dodge capability of the robot (higher is better)
     * @return the dodge capability of the robot
     */
    public double getDodgeDifficulty() {
        return this.DODGE_DIFFICULTY;
    }

    /**
     * Get the position of the robot
     * @return the x,y coordinates of the robot
     */
    public int[] getPosition() {
        return this.position;
    }

    /**
     * Gets the state of the robot
     * @return whether the robot has been caught or not
     */
    public boolean getState() {
        return this.isCaught;
    }

    /**
     * Updates the position, health, and state of the robot
     * @param hp the health of the robot
     * @param pos the x,y coordinates of the robot
     * @param isCaught the state of the robot
     */
    public void updateRecords(int hp, int[] pos, boolean isCaught) {
        this.hp = hp;
        this.position = pos;
        this.isCaught = isCaught;
    }

    /**
     * Updates only the position and state of the robot
     * @param pos the x,y coordinates of the robot
     * @param isCaught the state of the robot
     */
    public void updateRecords(int[] pos, boolean isCaught) {
        this.position = pos;
        this.isCaught = isCaught;
    }
}
