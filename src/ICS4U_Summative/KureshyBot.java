package ICS4U_Summative;

import becker.robots.*;
import java.awt.*;
import java.util.*;

/**
 * The robot which chases after the VIP
 * @author Aadil Kureshy
 * @version Janurary 15, 2025
 */
public class KureshyBot extends BaseBot{
    private final int WIDTH = 24;
    private final int HEIGHT = 13;
    private int targetID; //the current target
    //private boolean isCatching; //to be used by application to know when to check dodge
    private int targetX;
    private int targetY;

    /**
     * Constructor for Chaser
     * @param city City the robot is in
     * @param str the Street of the robot (y)
     * @param ave the Avenue of the robot (x)
     * @param dir the Direction the robot is facing
     * @param id the robot's numerical ID
     * @param role the robot's role (Chaser)
     * @param hp the amount of health
     * @param movesPerTurn the max amount of moves the robot can make
     * @param dodgeDiff the dodging/catching capability of the robot
     */
    public KureshyBot(City city, int str, int ave, Direction dir, int id, int role, int hp, int movesPerTurn, double dodgeDiff) {
        super(city, str, ave, dir, id, role, hp, movesPerTurn, dodgeDiff);
        //for debugging
        super.setColor(Color.RED);
        super.setLabel("Chaser " + id);
        this.otherRecords = new PlayerInfo[5]; //changed length since chasers need to keep track of each other
    }

    /**
     * Updates the records of the chaser given the application classes with the position, previous
     * position, and state as well health for the chaser itself
     * @param records the records to get information from
     */
    public void updateOtherRecords(PlayerInfo[] records)
    {
        //System.out.println("Updating enemy locations and states for KureshyBot "); debug
        for (PlayerInfo record : records) {

            //find the position of the record in otherRecords to update
            int index = super.findRecordByID(record.getID());

            //checks if it's my records by checking if it got a result
            if (index==-1) {
                this.myRecords.updateRecords(record.getHP(), record.getPosition(), record.getState());
            } else {
                int[] prevPos = this.otherRecords[index].getPosition(); //gets the old enemy position
                ((ChaserPlayerInfo) this.otherRecords[index]).updateRecords(record.getPosition(), prevPos, record.getState());
            }
        }
    }

    /**
     * Initializes each robot's record of others (excludes itself) and uses negative values for info
     * it's not supposed to know
     * @param records the records to get information from
     */
    public void initRecords(PlayerInfo[] records) {
        //since otherRecords has a length of 5 and records will have a length of 6:
        //for loop's iterator variable will keep track of records and skip this chaser
        //based on conditional structure while otherRecordsIndex will be incremented only
        //when otherRecords gets a new record
        int otherRecordsIndex = 0;

        //go through the records
        for (PlayerInfo record : records) {

            //make sure it's not this chaser since it already has its personal records
            if (record.getID() != this.myRecords.getID()) {

                //checks if it's another chaser since it needs to know where the other chasers are
                if(record.getRole() == 3) {
                    this.otherRecords[otherRecordsIndex] = new ChaserPlayerInfo(record.getID(), 3, -1, -1, record.getPosition(), record.getState());
                    otherRecordsIndex++;
                } else {
                    this.otherRecords[otherRecordsIndex] = new ChaserPlayerInfo(record.getID(), -1, -1, -1, record.getPosition(), record.getState());
                    otherRecordsIndex++;
                }
            }
        }
    }

    /**
     * Calculates the priorityScore of each target and then uses a selection sort to sort otherRecords based on their
     * priorityScore to get the best target
     */
    private void sortByPriority() {

        //Calculate the priorityScore and then add to the priorityScore the two factors which require information about
        //the chaser itself or other chasers (which isn't stored in the record)
        for (PlayerInfo record : this.otherRecords) {
            double turnDistance = this.calculateTurnDistance(super.getDistances(record.getPosition()), super.getMOVES_PER_TURN());
            ((ChaserPlayerInfo) record).calculatePriorityScore();
            ((ChaserPlayerInfo) record).addTurnDistAndPressure(calculateChaserPressure(record.getPosition()), turnDistance);
        }

        //iterate through the records
        for (int i=0; i<this.otherRecords.length; i++) {
            //temporarily store the next value in the unsorted section
            ChaserPlayerInfo temp = (ChaserPlayerInfo) this.otherRecords[i];
            int counter = i-1;

            //go down until it reaches the beginning of the array, or until the current score is smaller
            while (counter>=0 && ((ChaserPlayerInfo) this.otherRecords[counter]).getPriorityScore()>temp.getPriorityScore()) {
                //shift things up
                this.otherRecords[counter+1] = this.otherRecords[counter];
                counter--;
            }

            //insert into the right position
            this.otherRecords[counter+1] = temp;
        }
    }

    /**
     * Calculates how many turns it would take to reach the target
     * @param targetDistance the distance to the target
     * @param movesPerTurn how many moves the chaser can make per turn
     * @return the amount of turns it would take to reach the target
     */
    private double calculateTurnDistance(int targetDistance, int movesPerTurn) {
        return (double) targetDistance/movesPerTurn;
    }

    /**
     * Gets the amount of chasers nearby to a possible target and delivers a value between 0-1
     * on how much "pressure" the target is under
     * @param targetPos the location of the target
     * @return a pressure value between 0-1 that tells how much pressure the target is under
     * (higher means more chasers)
     */
    private double calculateChaserPressure(int[] targetPos) {
        int nearbyChasers = 0;
        double pressureScore;

        //iterate through the records
        for (PlayerInfo record : this.otherRecords) {

            //checks for chasers and gets the distance between the target and chaser
            if (record.getRole()==3) {
                int distance = super.getDistances(targetPos, record.getPosition());

                //increases the amount of nearby chasers if it detects that they are within
                //two turns of a chaser's possible max movement
                if (distance <= (2*5) ) {
                    nearbyChasers++;
                }
            }
        }

        pressureScore = (double) nearbyChasers / (1+nearbyChasers);
        return pressureScore;
    }

    /**
     * The application class will send the results of the tag attempt
     * @param ID the ID of the target
     * @param isSuccess whether the target dodged or not
     */
    public void sendTagResult(int ID, boolean isSuccess) {
        int index = super.findRecordByID(ID);
        ((ChaserPlayerInfo) this.otherRecords[index]).takeDamage(isSuccess);
    }

    /**
     * Avoids attacking guards while low on health
     * @return the location of the closest, safest target's record in the record array
     */
    private int checkSafety() {

        int targetIndex = super.findRecordByID(this.targetID);
        int index = 0;
        //checks if the target is a confirmed guard
        while (((ChaserPlayerInfo) this.otherRecords[targetIndex]).getRolePrediction() == 1 && this.myRecords.getHP()==1) {
            //System.out.format("Avoiding robot %d since they're a Guard and I'm low \n", this.targetID); //debug
            index++;
            //set target to the next safest target
            this.targetID = this.otherRecords[index].getID();
            this.targetX = this.otherRecords[index].getPosition()[0];
            this.targetY = this.otherRecords[index].getPosition()[1];
            targetIndex = super.findRecordByID(this.targetID);
            //System.out.format("My new target is robot %d\n", this.targetID); //debug
        }

        return targetIndex;
    }

    /**
     * Calculates the best, achievable position to close the gap to the target
     * @return the x,y coordinates to go to
     */
    private int[] calcChasePath() {
        int[] myPos = this.getMyPosition();
        int[] targetPos = {this.targetX, this.targetY};
        int movesLeft = this.getMOVES_PER_TURN();

        //aligns with the x-axis of the target first (makes it easier to corner the target since the arena is wider than it is tall)
        for (int i=0; i<movesLeft; i++) {
            //if already on the x axis
            if (myPos[0] == targetPos[0]) {
                break;
            }

            if (myPos[0] < targetPos[0]) {
                myPos[0]++;
            } else {
                myPos[0]--;
            }
            movesLeft--;
        }

        //aligns with the y-axis of the target
        for (int i=0; i<movesLeft; i++) {
            //if already on the y axis
            if (myPos[1] == targetPos[1]) {
                break;
            }

            if (myPos[1] < targetPos[1]) {
                myPos[1]++;
            } else {
                myPos[1]--;
            }
            movesLeft--;
        }

        return myPos;
    }

    /**
     * Finds the nearest corner to the target
     * @return the x,y coordinates of the nearest coordinate
     */
    private int[] findNearestCorner() {
        int[] nearestCorner = new int[2];

        //checks whether the corner is at the left or right
        if (this.targetX <= this.WIDTH/2) {
            nearestCorner[0] = 1;
        } else {
            nearestCorner[0] = this.WIDTH;
        }

        //check whether the corner is at the top or bottom
        if (this.targetY <= this.HEIGHT/2) {
            nearestCorner[1] = 1;
        } else {
            nearestCorner[1] = this.HEIGHT;
        }

        //System.out.format("The nearest corner to Robot %d is %s\n", this.targetID, Arrays.toString(nearestCorner)); debug
        return nearestCorner;
    }

    /**
     * Calculates the distance between the target and the nearest wall
     * @return the distance to the closest wall
     */
    private int calcDistanceToWall() {
        int distance = this.targetX-1; //distance between the left wall and target

        //checks the distance between the right wall and the target and chooses the higher one
        if (this.WIDTH-this.targetX < distance) {
            distance = this.WIDTH - this.targetX;
        }

        //checks the distance between the top wall and the target
        if (this.targetY-1 < distance) {
            distance = this.targetY-1;
        }

        //checks the distance between the bottom wall and the target
        if (this.HEIGHT-this.targetY < distance) {
            distance = this.HEIGHT-this.targetY;
        }

        //System.out.format("The distance between Robot %d and the nearest wall is %d\n", this.targetID, distance); debug
        return distance;
    }

    /**
     * Checks the feasibility of cornering the target
     * @param turns the number of turns it would take to get to the target
     * @return whether to cut off and corner the target or not
     */
    private boolean checkCutOff(int turns) {
        //all used for calculating the specific conditions where to cut off is reasonable
        int distanceToWall = this.calcDistanceToWall();
        int[] nearestTargetCorner = this.findNearestCorner();

        int horizontalDistanceToCorner = Math.abs(this.targetX - nearestTargetCorner[0]);
        int verticalDistanceToCorner = Math.abs(this.targetY - nearestTargetCorner[1]);
        int myHorizontalDistanceToCorner = Math.abs(this.getMyPosition()[0] - nearestTargetCorner[0]);
        int myVerticalDistanceToCorner = Math.abs(this.getMyPosition()[1] - nearestTargetCorner[1]);

        //check that the target is within 2 turns, that it's distance to the closest wall is less than
        //my speed-1, and that both the horizontal/vertical distance of the chaser to the nearest corner
        //is greater than that of the target's
        return turns == 2 && distanceToWall <= (this.getMOVES_PER_TURN() - 1) &&
                myHorizontalDistanceToCorner > horizontalDistanceToCorner &&
                myVerticalDistanceToCorner > verticalDistanceToCorner;
    }

    /**
     * Gets diagonal to the target and then moves diagonally to box them in until they get into
     * the direct range
     * @return the x,y to go to
     */
    private int[] calcCutOffPath() {
        int[] myPos = this.getMyPosition();
        int movesLeft = this.getMOVES_PER_TURN();

        //used for deciding on where to cutoff
        int distanceX = this.targetX - myPos[0];
        int distanceY = this.targetY - myPos[1];
        int absDistanceX = Math.abs(distanceX);
        int absDistanceY = Math.abs(distanceY);
        int horizontalMove ;
        int verticalMove;

        //check whether to move right or left
        if (distanceX > 0) {
            horizontalMove = 1;
        } else {
            horizontalMove = -1;
        }
        //check whether to move up or down
        if (distanceY > 0) {
            verticalMove = 1;
        } else {
            verticalMove = -1;
        }

        //get diagonal to the target to box them in
        while (movesLeft>0 && absDistanceX != absDistanceY) {

            //if horizontal gap is bigger,
            if (absDistanceX > absDistanceY) {
                myPos[0] += horizontalMove;
                absDistanceX--;
            } else {
                myPos[1] += verticalMove;
                absDistanceY--;
            }
            movesLeft--;
        }

        //System.out.format("Diagonally aligned at %s! Moves left: %d\n", Arrays.toString(myPos), movesLeft); debug
        //move diagonally (2 movements at a time) given chaser speed
        while (movesLeft>=2) {

            //never align on an axis with the target
            if (myPos[0] + horizontalMove == this.targetX || myPos[1] + verticalMove == this.targetY) {
                break;
            }

            myPos[0]+= horizontalMove;
            myPos[1]+= verticalMove;

            absDistanceX--;
            absDistanceY--;
            movesLeft -= 2; //subtract two movements
        }

        return myPos;
    }

    /**
     * Decides how to go after the designated target: immediate tag if in range, corner the target, or just chase
     */
    private void executeStrat() {
        //this.isCatching = false;
        //avoids attacking confirmed guards ONLY when low on health, goes to the next best option
        int targetIndex = checkSafety();

        //casts as an int to get how many turns away it is
        int turns = (int) Math.ceil(((ChaserPlayerInfo) this.otherRecords[targetIndex]).getTurnDistance());

        //checks if the target can be caught within this turn
        if (turns <= 1) {
            //System.out.println("IN RANGE: " + this.targetX + "," + this.targetY); //debug
            super.moveToPos(this.otherRecords[targetIndex].getPosition());
            //this.attemptTag();

        //check if the target is close to a wall, the chaser, and closer to the nearest corner than the chaser
        } else if (checkCutOff(turns)) {
            int[] nextPos = this.calcCutOffPath(); //for debugging statement
            super.moveToPos(nextPos);
            //System.out.println("CUTTING OFF AT: " + Arrays.toString(nextPos)); //debug

        } else { //otherwise just chase in an open area
            int[] nextPos = this.calcChasePath(); //also for debugging statement
            super.moveToPos(nextPos);
            //System.out.println("CHASING AT" + Arrays.toString(nextPos)); //debug
        }
    }

    /**
     * Called by the application when it's the chaser's turn
     */
    public void takeTurn() {
        //sort the records by their priority scores
        this.sortByPriority();
        this.targetID = this.otherRecords[0].getID();
        this.targetX = this.otherRecords[0].getPosition()[0];
        this.targetY = this.otherRecords[0].getPosition()[1];
        //System.out.println("=============================================="); //debug organisation
        /*System.out.format("My target is %d who has a priority score of %.2f and is located at %s, which is %d turns away from me " +
                        "while I am located at %s\n",
                this.targetID, ((ChaserPlayerInfo) this.otherRecords[0]).getPriorityScore(),
                Arrays.toString((this.otherRecords[0]).getPosition()),
                (int) Math.ceil(((ChaserPlayerInfo) this.otherRecords[0]).getTurnDistance()),
                Arrays.toString(this.getMyPosition())); //debug
        for (int i=1; i<this.otherRecords.length; i++) {
            System.out.format("My next target is %d who has a priority score of %.2f and is located at %s, which is %d turns away from me " +
                            "while I am located at %s\n",
                    this.otherRecords[i].getID(), ((ChaserPlayerInfo) this.otherRecords[i]).getPriorityScore(),
                    Arrays.toString((this.otherRecords[i]).getPosition()),
                    (int) Math.ceil(((ChaserPlayerInfo) this.otherRecords[i]).getTurnDistance()),
                    Arrays.toString(this.getMyPosition())); //debug
        }*/
        //System.out.println("=============================================="); //debug organisation
        //the above debug statements are inaccurate if the chaser decides to change its target because of low health
        //and a confirmed guard as a target
        this.executeStrat();
    }
}
