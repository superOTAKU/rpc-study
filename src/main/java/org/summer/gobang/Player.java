package org.summer.gobang;

public enum Player {
    PLAYER1(Chess.BLACK), PLAYER2(Chess.WHITE);

    private final Chess chess;

    Player(Chess chess) {
        this.chess = chess;
    }

    public Chess getChess() {
        return chess;
    }

}
