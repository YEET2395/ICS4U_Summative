package ICS4U_Summative;

import becker.robots.*;

public class LiTestWorld {

    private City city;
    private BaseBot[] bots = new BaseBot[6];
    private PlayerInfo[] infos = new PlayerInfo[6];

    // Index mapping
    public static final int IDX_VIP1 = 0;
    public static final int IDX_VIP2 = 1;
    public static final int IDX_GUARD = 2;
    public static final int IDX_GUARD2 = 3;
    public static final int IDX_CHASER1 = 4;
    public static final int IDX_CHASER2 = 5;

    public void setup() {
        city = new City();
        setupPlayground(city);

        bots[IDX_VIP1]   = new XiongBot(city, 1, 1, Direction.SOUTH, 1, 1, 2, 2, 0.50);
        bots[IDX_VIP2]   = new XiongBot(city, 1, 2, Direction.SOUTH, 2, 1, 2, 2, 0.50);

        bots[IDX_GUARD]  = new LiBot(city, 2, 1, Direction.NORTH, 3, 2, 5, 3, 0.50);
        bots[IDX_GUARD2] = new LiBot(city, 2, 2, Direction.NORTH, 4, 2, 5, 3, 0.50);

        bots[IDX_CHASER1]= new KureshyBot(city, 3, 1, Direction.NORTH, 5, 3, 3, 4, 0.50);
        bots[IDX_CHASER2]= new KureshyBot(city, 3, 2, Direction.NORTH, 6, 3, 3, 4, 0.50);

        for (int i = 0; i < bots.length; i++) {
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

    public BaseBot bot(int idx) {
        return bots[idx];
    }

    public LiBot guard() {
        return (LiBot) bots[IDX_GUARD];
    }

    public void setAllCaught() {
        for (int i = 0; i < bots.length; i++) {
            setCaught(i, true);
        }
    }

    public void setCaught(int idx, boolean caught) {
        BaseBot b = bots[idx];
        int hp = b.myRecords.getHP();
        b.myRecords.updateRecords(hp, b.getMyPosition(), caught);
    }

    public void setBot(int idx, int x, int y, int hp, boolean caught) {
        BaseBot b = bots[idx];
        b.moveToPos(new int[]{x, y});
        b.myRecords.updateRecords(hp, b.getMyPosition(), caught);
    }

    public void syncAll() {
        for (int i = 0; i < bots.length; i++) {
            BaseBot b = bots[i];
            int hp = b.myRecords.getHP();
            boolean caught = b.myRecords.getState();
            b.myRecords.updateRecords(hp, b.getMyPosition(), caught);
            infos[i].updateRecords(hp, b.getMyPosition(), caught);
        }
    }

    public void broadcastToGuard() {
        guard().updateOtherRecords(infos);
    }

    private void setupPlayground(City playground) {
        playground.setSize(1500, 900);

        for (int i = 1; i <= 13; i++) {
            new Wall(playground, i, 0, Direction.EAST);
            new Wall(playground, i, 25, Direction.WEST);
        }
        for (int i = 1; i <= 24; i++) {
            new Wall(playground, 0, i, Direction.SOUTH);
            new Wall(playground, 14, i, Direction.NORTH);
        }
    }
}
