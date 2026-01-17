package ICS4U_Summative;

import becker.robots.*;

public class LiTestWorld {

    // Becker city
    private City city;

    // All robot objects used in the test world (VIPs, Guards, Chasers)
    private BaseBot[] bots = new BaseBot[6];

    // Master records list that will be broadcast to the Guard under test
    private PlayerInfo[] infos = new PlayerInfo[6];

    // Index mapping for bots[] and infos[]
    public static final int IDX_VIP1 = 0;
    public static final int IDX_VIP2 = 1;
    public static final int IDX_GUARD = 2;
    public static final int IDX_GUARD2 = 3;
    public static final int IDX_CHASER1 = 4;
    public static final int IDX_CHASER2 = 5;

    /**
     * Sets up a test world
     */
    public void setup()
    {
        city = new City();
        setupPlayground(city);

        bots[IDX_VIP1]   = new XiongBot(city, 1, 1, Direction.SOUTH, 1, 1, 2, 2, 0.50);
        bots[IDX_VIP2]   = new XiongBot(city, 1, 2, Direction.SOUTH, 2, 1, 2, 2, 0.50);

        bots[IDX_GUARD]  = new LiBot(city, 2, 1, Direction.NORTH, 3, 2, 5, 3, 0.50);
        bots[IDX_GUARD2] = new LiBot(city, 2, 2, Direction.NORTH, 4, 2, 5, 3, 0.50);

        bots[IDX_CHASER1]= new KureshyBot(city, 3, 1, Direction.NORTH, 5, 3, 3, 4, 0.50);
        bots[IDX_CHASER2]= new KureshyBot(city, 3, 2, Direction.NORTH, 6, 3, 3, 4, 0.50);

        for (int i = 0; i < bots.length; i++)
        {
            BaseBot b = bots[i];
            infos[i] = new PlayerInfo(
                    b.myRecords.getID(),
                    b.myRecords.getRole(),
                    b.myRecords.getHP(),
                    b.myRecords.getDodgeDifficulty(),
                    b.getMyPosition(),
                    b.myRecords.getState()
            );
        }

        setAllCaught();
        syncAll();
    }

    /**
     * Returns the robot object at the given index in the internal bots[] array.
     * @param idx index in bots[] (0..5)
     * @return the BaseBot instance stored at that index
     */
    public BaseBot bot(int idx)
    {
        return bots[idx];
    }

    /**
     * Returns the primary Guard under test.
     * @return the LiBot instance at IDX_GUARD
     */
    public LiBot guard()
    {
        return (LiBot) bots[IDX_GUARD];
    }

    /**
     * Marks every robot as caught (inactive) in the test world.
     */
    public void setAllCaught()
    {
        for (int i = 0; i < bots.length; i++)
        {
            setCaught(i, true);
        }
    }

    /**
     * Sets a robot's caught state without changing its position.
     * HP is preserved from the robot's current myRecords.
     * @param idx index of robot in bots[]
     * @param caught true if the robot should be treated as caught/inactive
     */
    public void setCaught(int idx, boolean caught)
    {
        BaseBot b = bots[idx];
        int hp = b.myRecords.getHP();
        b.myRecords.updateRecords(hp, b.getMyPosition(), caught);
    }

    /**
     * Repositions a robot deterministically and sets its HP and caught state.
     * @param idx index of robot in bots[]
     * @param x avenue coordinate
     * @param y street coordinate
     * @param hp new HP value to store in the robot's records
     * @param caught true if the robot should be treated as caught/inactive
     */
    public void setBot(int idx, int x, int y, int hp, boolean caught)
    {
        BaseBot b = bots[idx];
        b.moveToPos(new int[]{x, y});
        b.myRecords.updateRecords(hp, b.getMyPosition(), caught);
    }

    /**
     * Sync:
     * 1) each robot's internal myRecords
     * 2) the master infos[] records that will be broadcast to the Guard
     */
    public void syncAll()
    {
        for (int i = 0; i < bots.length; i++)
        {
            BaseBot b = bots[i];
            int hp = b.myRecords.getHP();
            boolean caught = b.myRecords.getState();
            b.myRecords.updateRecords(hp, b.getMyPosition(), caught);
            infos[i].updateRecords(hp, b.getMyPosition(), caught);
        }
    }

    /**
     * Sends the master infos[] records list to the Guard under test.
     */
    public void broadcastToGuard()
    {
        guard().updateOtherRecords(infos);
    }

    /**
     * Builds the same arena walls as the main App setup:
     * - Vertical walls at avenue 0 and 25
     * - Horizontal walls at street 0 and 14
     * @param playground the Becker City to place walls into
     */
    private void setupPlayground(City playground)
    {
        playground.setSize(1500, 900);
        for (int i = 1; i <= 13; i++)
        {
            new Wall(playground, i, 0, Direction.EAST);
            new Wall(playground, i, 25, Direction.WEST);
        }
        for (int i = 1; i <= 24; i++)
        {
            new Wall(playground, 0, i, Direction.SOUTH);
            new Wall(playground, 14, i, Direction.NORTH);
        }
    }
}
