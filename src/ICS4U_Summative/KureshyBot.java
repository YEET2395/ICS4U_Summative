package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

/**
 * The robot which chases after the VIP
 * @author Aadil Kureshy
 * @version Janurary 6, 2025
 */
public class KureshyBot extends BaseBot{
    private final int WIDTH = 24;
    private final int HEIGHT = 13;
    private int targetID; //the current target
    private boolean isCatching; //to be used by application to know when to check dodge
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
        super.setLabel("Robot " + id);
        this.otherRecords = new PlayerInfo[5]; //changed length since chasers need to keep track of each other
    }

    public void updateOtherRecords(PlayerInfo[] records)
    {
        System.out.println("Updating enemy locations and states for KureshyBot ");
        for ( int i=0; i<records.length; i++) {

            //find the position of the record in otherRecords to update
            int index = super.findRecordByID(records[i].getID());

            //checks if it's my records by checking if it got a result
            if (index==-1) {
                this.myRecords.updateRecords(records[i].getHP(), records[i].getPosition(), records[i].getState());
            } else {
                int[] prevPos = this.otherRecords[index].getPosition(); //gets the old enemy position
                ((ChaserPlayerInfo) this.otherRecords[index]).updateRecords(records[i].getPosition(), prevPos, records[i].getState());
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
        for (int i=0; i<records.length; i++) {

            //makes sure it's not this chaser (since it already has its personal records) and that it's another chaser
            if (records[i].getID() != this.myRecords.getID() && records[i].getRole()==3) {
                this.otherRecords[otherRecordsIndex] = new ChaserPlayerInfo(records[i].getID(), 3, -1, -1, records[i].getPosition(), records[i].getState());
                otherRecordsIndex++;
            } else {
                this.otherRecords[otherRecordsIndex] = new ChaserPlayerInfo(records[i].getID(), -1, -1, -1, records[i].getPosition(), records[i].getState());
                otherRecordsIndex++;
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
        for (int i=0; i<this.otherRecords.length; i++) {
            int turnDistance = this.calculateTurnDistance(super.getDistances(this.otherRecords[i].getPosition()), super.getMOVES_PER_TURN());
            ((ChaserPlayerInfo) this.otherRecords[i]).calculatePriorityScore();
            ((ChaserPlayerInfo) this.otherRecords[i]).addTurnDistAndPressure(calculateChaserPressure(this.otherRecords[i].getPosition()), turnDistance);
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
    private int calculateTurnDistance(int targetDistance, int movesPerTurn) {
        int turns = targetDistance/movesPerTurn;
        if (targetDistance % movesPerTurn != 0){
            turns++;
        }

        return turns;
    }

    /**
     * Gets the amount of chasers nearby (including itself) to a possible target and delivers a value between 0-1
     * on how much "pressure" the target is under
     * @param targetPos the location of the target
     * @return a pressure value between 0-1 that tells how much pressure the target is under
     */
    private double calculateChaserPressure(int[] targetPos) {
        int nearbyChasers = 0;
        double pressureScore;

        //iterate through the records
        for (int i=0; i<this.otherRecords.length; i++) {

            //checks for chasers and gets the distance between the target and chaser
            if (this.otherRecords[i].getRole()==3) {
                int distance = super.getDistances(targetPos, this.otherRecords[i].getPosition());

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
     * Calculates the distance between the target and the nearest wall
     * @return the distance to the closest wall
     */
    private int calcDistanceToWall() {
        int distance = this.targetX; //distance between the left wall and target

        //checks the distance between the right wall and the target and chooses the higher one
        if (this.WIDTH-this.targetX < distance) {
            distance = this.WIDTH - this.targetX;
        }

        //checks the distance between the top wall and the target
        if (this.targetY < distance) {
            distance = this.targetY;
        }

        //checks the distance between the bottom wall and the target
        if (this.HEIGHT-this.targetY < distance) {
            distance = this.HEIGHT-this.targetY;
        }

        return distance;
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

        return nearestCorner;
    }

    /**
     * Finds the target position to travel towards to be able to corner the target using movement per turn
     * @return the buffered target x,y coordinates
     */
    private int[] findTargetCorner() {
        int[] goalPos = this.findNearestCorner();
        int buffer = this.getMOVES_PER_TURN();

        //gets a buffer position to let me corner the target with chaser's (usually) greater movement
        if (goalPos[0] == 0) {
            goalPos[0] = buffer;
        } else {
            goalPos[0] = this.WIDTH-buffer;
        }

        if (goalPos[0] == 0) {
            goalPos[1] = buffer;
        } else {
            goalPos[1] = this.HEIGHT-buffer;
        }

        return goalPos;
    }

    /**
     * Calculates the best, achievable position to move towards that corners the target
     * @return the x,y to go to
     */
    private int[] calcCutOffPath() {
        int[] myPos = this.getMyPosition();
        int[] goalPos = this.findTargetCorner();
        int movesLeft = this.getMOVES_PER_TURN();

        for (int i=0; i<movesLeft; i++) {

            //find the horizontal and vertical distance to the buffered corner position
            int distanceX = goalPos[0] - myPos[0];
            int distanceY = goalPos[1] - myPos[1];

            //check whether the horizontal gap or vertical gap is bigger
            if (Math.abs(distanceX) >= Math.abs(distanceY)) {

                //close the gap on the larger distance
                if (distanceX > 0) {
                    myPos[0]++;
                } else if (distanceX < 0) {
                    myPos[0]--;
                }
            } else {
                //close the gap on the larger distance
                if (distanceY > 0 ) {
                    myPos[1]++;
                } else if (distanceY < 0) {
                    myPos[1]--;
                }
            }
        }

        return myPos;
    }

    /**
     * Only activates once it's directly on a target
     */
    private void attemptTag() {
        this.isCatching = true;
    }

    /**
     * Decides how to go after the designated target: immediate tag if in range, corner the target, or just chase
     */
    private void executeStrat() {
        int targetIndex = super.findRecordByID(this.targetID); //finds the target's record index
        int turns = ((ChaserPlayerInfo) this.otherRecords[targetIndex]).getTurnDistance(); //gets how many turns away it is

        //used for deciding on whether to chase or cutoff
        int distanceToWall = this.calcDistanceToWall();
        int[] nearestTargetCorner = this.findNearestCorner();
        int targetCornerDistance = this.getDistances(nearestTargetCorner, this.otherRecords[targetIndex].getPosition());
        int chaserCornerDistance = this.getDistances(nearestTargetCorner);

        //checks if the target can be caught within this turn
        if (turns == 1) {
            super.moveToPos(this.otherRecords[targetIndex].getPosition());
            this.attemptTag();
        //check if the target is close to a wall, the chaser, and closer to the nearest corner than the chaser
        } else if (turns == 2 && distanceToWall <= (this.getMOVES_PER_TURN()-1) &&
                    targetCornerDistance<chaserCornerDistance) {
            super.moveToPos(this.calcCutOffPath());
        } else { //otherwise just chase in an open area
            super.moveToPos(this.calcChasePath());
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
        System.out.format("My target is %d who has a priority score of %.2f\n", this.targetID, ((ChaserPlayerInfo) this.otherRecords[0]).getPriorityScore());
        for (int i=1; i<this.otherRecords.length; i++) {
            System.out.format("My next target is %d who has a priority score of %.2f\n", this.otherRecords[i].getID(), ((ChaserPlayerInfo) this.otherRecords[i]).getPriorityScore());
        }
        System.out.println("==============================================");
        this.executeStrat();
    }
}
