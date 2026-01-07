package ICS4U_Summative;

import becker.robots.*;

import java.awt.*;

public class XiongBot extends BaseBot{
    private int[] guardPos = {0, 0};
    private int[] chaserPos = {0, 0};
    private int movesPerTurn = 1;

    /**
     * Constructor for BaseBot
     *
     * @param city City the robot is in
     * @param str  Street number
     * @param ave  Avenue number
     * @param dir  direction the robot is facing
     * @param role role of the bot
     * @param id   identifier of the bot
     * @param hp   health points of the bot
     */
    public XiongBot(City city, int str, int ave, Direction dir, int role, int id, int hp, int movesPerTurn, int dodgeDiff) {
        super(city, str, ave, dir, role, id, hp, movesPerTurn, dodgeDiff);
        this.movesPerTurn = movesPerTurn;

        //for debugging
        super.setColor(Color.GREEN);
        super.setLabel("Robot " + super.getMyID());
    }

    public void getGuardPosition(int[] coord){
        guardPos[0]=coord[0];
        guardPos[1]=coord[1];
    }

    public void getChaserPosition(int[] coord){
        chaserPos[0]=coord[0];
        chaserPos[1]=coord[1];
    }

    private boolean attemptMove(Direction d) {
        this.turnDirection(d);
        if (this.frontIsClear()) {
            this.move();
            return true;
        }
        return false;
    }

    public void takeTurn(){
        int movesAllowed = this.movesPerTurn;

        for (int step = 0; step < movesAllowed; step++) {
            int myX = this.getX();
            int myY = this.getY();
            int chX = this.chaserPos[0];
            int chY = this.chaserPos[1];

            int dx = myX - chX; // positive = we're east of chaser
            int dy = myY - chY; // positive = we're south of chaser

            // choose primary escape direction along the axis with larger separation
            Direction primary;
            if (Math.abs(dx) >= Math.abs(dy)) {
                primary = (dx >= 0) ? Direction.EAST : Direction.WEST;
            } else {
                primary = (dy >= 0) ? Direction.SOUTH : Direction.NORTH;
            }

            boolean moved = false;

            // try primary
            if (attemptMove(primary)) {
                moved = true;
            } else {
                // determine and try secondary
                Direction secondary;
                if (primary == Direction.EAST || primary == Direction.WEST) {
                    if (dy>=0){
                        secondary = Direction.SOUTH;
                    } else {
                        secondary = Direction.NORTH;
                    }
                } else {
                    if(dx>=0){
                        secondary = Direction.EAST;
                    } else {
                        secondary = Direction.WEST;
                    }
                }

                if (attemptMove(secondary)) {
                    moved = true;
                } else {
                    // fallback: try all directions until one works
                    Direction[] all = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
                    for (Direction d : all) {
                        if (attemptMove(d)) {
                            moved = true; break;
                        }
                    }
                }
            }

            // if cannot move at this step, stop trying further steps
            if (!moved) break;
        }
    }
}
