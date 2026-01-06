package ICS4U_Summative;

/**
 * Application controlled records tracking each robot
 * @author Aadil Kureshy, Austin Xiong,  Xinran Li
 * @version 2026 1 5
 */
public class playerInfo {
    private final int ID;
    private int role;
    private int hp;
    private final int dodgeDifficulty;
    private int[] position;

    /**
     * Constructor for playerInfo
     * @param ID the numerical ID of the robot
     * @param role the role of the robot
     * @param hp the health of the robot
     * @param dodgeDifficulty the dodge/catch capability of the robot (higher is better)
     * @param pos the coordinates of the robot
     */
    public playerInfo (int ID, int role, int hp, int dodgeDifficulty, int[] pos) {
        this.ID = ID;
        this.role = role;
        this.hp = hp;
        this.dodgeDifficulty = dodgeDifficulty;
        this.position = pos;
    }

    /**
     * Get the ID number of the robot
     * @return the ID number of the robot
     */
    public int getID() {
        return ID;
    }

    /**
     * Get the role of the robot
     * @return the role of the robot
     */
    public int getRole() {
        return role;
    }

    /**
     * Changes the role after the robot runs out of HP
     * @param role the role of the robot
     */
    public void setRole(int role) {
        this.role = role;
    }

    /**
     * Get the health of the robot
     * @return the health of the robot
     */
    public int getHP() {
        return hp;
    }

    /**
     * Sets the new health of the robot
     */
    public void setHP(int newHP) {
        this.hp = newHP;
    }

    /**
     * The catch/dodge capability of the robot (higher is better)
     * @return the dodge capability of the robot
     */
    public int getDodgeDifficulty() {
        return dodgeDifficulty;
    }

    /**
     * Get the position of the robot
     * @return the coordinates of the robot
     */
    public int[] getPosition() {
        return position;
    }
}
