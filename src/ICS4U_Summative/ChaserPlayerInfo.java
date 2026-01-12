package ICS4U_Summative;

public class ChaserPlayerInfo extends PlayerInfo {
    private double turnDistance;
    private double dodgeEst = 0.5;
    private int hpEst = 3;
    private int[] prevPos;
    private int speedObs = 1;
    private int numDodges = 0;
    private int numCatches = 0;
    private final double DELTA = 0.15;
    private double rolePrediction = 0;
    private double priorityScore = 0;
    private final double TURN_DIST_WEIGHT = 1;
    private final double DODGE_EST_WEIGHT = 0.5;
    private final double HP_EST_WEIGHT = 0.5;
    private final double MAX_HP = 5.0;
    private final double PRESSURE_WEIGHT = 0.3;
    private final double ROLE_PREDICTION_WEIGHT = 0.3;
    private final int MAX_VIP_SPEED = 3;
    private final int MAX_GUARD_SPEED = 4;
    private final int VIP_HP = 2;
    private final int CHASER_ROLE = 3;



    /**
     * Constructor for ChaserPlayerInfo which is used by KureshyBot
     * @param ID the numerical ID of the robot
     * @param role the role of the robot
     * @param hp the health of the robot
     * @param dodgeDifficulty the dodge/catch capability of the robot (higher is better)
     * @param pos the x,y coordinates of the robot
     * @param isCaught the state of the robot
     */
    public ChaserPlayerInfo(int ID, int role, int hp, double dodgeDifficulty, int[] pos, boolean isCaught) {
        super(ID, role, hp, dodgeDifficulty, pos, isCaught);
        this.prevPos = pos; //update this only after calculating speed for the first time since the first speed estimate will be 0, which is lower than 1
    }

    /**
     * Updates only the position, previous position and state of the robot
     * @param pos the x,y coordinates of the robot
     * @param prevPos the previous x,y coordinate of the robot
     * @param isCaught the state of the robot
     */
    public void updateRecords(int[] pos, int[] prevPos, boolean isCaught) {
        super.updateRecords(pos, isCaught);
        this.prevPos = prevPos;
    }

    /**
     * Gets the priorityScore of the robot
     * @return the priorityScore of the robot
     */
    public double getPriorityScore() {
        return this.priorityScore;
    }

    /**
     * Adds the chaser pressure value and the amount of turns it would take to get to the robot
     * separately since it requires information about the chaser itself and other chasers
     * @param pressureScore the pressure value to add to the priorityScore
     * @param turnDist the amount of turns it would take the chaser to get to the robot
     */
    public void addTurnDistAndPressure(double pressureScore, double turnDist) {
        this.priorityScore += (this.PRESSURE_WEIGHT * pressureScore);
        this.priorityScore += (this.TURN_DIST_WEIGHT * turnDist);
        this.turnDistance = turnDist; //for deciding the strategy
    }

    /**
     * Gets the amount of turns it would take to reach the robot
     * @return the amount of turns it would take to reach the robot
     */
    public double getTurnDistance() {
        return this.turnDistance;
    }

    /**
     * Gets the estimated dodge capability of the robot
     * @return the estimated dodge capability of the robot
     */
    public double getDodgeEst() {
        return this.dodgeEst;
    }

    /**
     * Gets the estimated health of the robot
     * @return the estimated health of the robot
     */
    public int getHPEst() {
        return this.hpEst;
    }

    public double getRolePrediction() {
        return this.rolePrediction;
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
     * Calculates the priorityScore of the
     */
    public void calculatePriorityScore() {

        calculateRolePrediction();

        //calculate a priority score where a lower value is more likely to be a VIP
        this.priorityScore =
                        (this.DODGE_EST_WEIGHT * this.getDodgeEst()) +
                        (this.HP_EST_WEIGHT * (this.getHPEst()/this.MAX_HP)) +
                        (this.ROLE_PREDICTION_WEIGHT * this.rolePrediction);

        //for debugging
        System.out.format("ID: %d -- DODGE: %.3f -- HP: %.3f -- ROLE: %.2f\n", this.getID(),
                        (this.DODGE_EST_WEIGHT * this.getDodgeEst()),
                        (this.HP_EST_WEIGHT * (this.getHPEst()/this.MAX_HP)),
                        (this.ROLE_PREDICTION_WEIGHT * this.rolePrediction));
        //System.out.format("The robot %d has a priority score of %.2f\n", targetIndex[i], priorityScore[i]);

        //Completely deprioritize those already caught, or other chasers
        if (this.getState() || this.getRole() == this.CHASER_ROLE) {
            this.priorityScore = 1000;
        }
    }

    /**
     * Predicts the role based on observed movement and how many times it's been caught (by the chaser using these records)
     */
    private void calculateRolePrediction() {

        //updates the maximum observed speed of the robot
        this.calculateMaxSpeed();

        if (this.speedObs > this.MAX_VIP_SPEED || this.numCatches >= this.VIP_HP) { //confirmed as a guard
            this.rolePrediction = 1.0; //deprioritize
        } else {
            this.rolePrediction =  -(1.0 - ((double) this.speedObs/this.MAX_GUARD_SPEED));
        }
    }

    /**
     * Observes the maximum amount of movment observed by the chaser of each non-chaser
     * robot to use in predicting its role
     */
    private void calculateMaxSpeed() {

        int newMove = this.getDistances(this.getPosition(), this.prevPos);

        //check if movement was greater than previous movement observed
        if (((double) newMove) > this.speedObs) {
            this.speedObs =  newMove;
        }
    }

    /**
     * Updated by the chaser
     * @param isSuccess whether the tag was successful or not
     */
    public void takeDamage(boolean isSuccess) {

        //decide how much to increase the dodge estimation (slowly decreases the amount over many attempts)
        double delta = this.DELTA / (1+numCatches+numDodges);

        //decrease stats
        if (isSuccess) {
            this.hpEst--;
            this.dodgeEst -= delta;
            this.numCatches++;
            if (numCatches == 2) { //confirmed guard, revise hp estimate
                this.hpEst = 3;
            }
        } else {
            this.dodgeEst += delta;
            this.numDodges++;
            System.out.println("Increasing dodge estimate of " + this.getID());
        }

    }

    /**
     * Debug method
     * @return a String containing important info about the target
     */
    public String toString() {
        return "ID: " + this.getID() + " POS: " + this.getPosition()[0] + "," + this.getPosition()[1]
                + " TURN DISTANCE: " + this.turnDistance + " DODGE_EST: " + this.dodgeEst + " HP_EST " + this.hpEst
         + " prevPos: " + this.prevPos[0] + "," + this.prevPos[1] + " SPEED: " + this.speedObs +
                " DODGES: " + this.numDodges + " CATCHES: " + this.numCatches + " PRIORITY SCORE: " + this.priorityScore;
    }
}
