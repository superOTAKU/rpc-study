package org.summer.gobang;

public class GameLog {
    private Player player;
    private int x;
    private int y;

    public GameLog() {}

    public GameLog(Player player, int x, int y) {
        this.player = player;
        this.x = x;
        this.y = y;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getY() {
        return y;
    }
}
