package org.summer.gobang;

public enum GameState {
    PLAYER1_ROUND, PLAYER2_ROUND, PLAYER1_WIN, PLAYER2_WIN;

    public boolean isFinish() {
        return this == PLAYER1_WIN || this == PLAYER2_WIN;
    }

    public boolean isRoundMatch(Player player) {
        return  (this == PLAYER1_ROUND && player == Player.PLAYER1) || (this == PLAYER2_ROUND && player == Player.PLAYER2);
    }

}
