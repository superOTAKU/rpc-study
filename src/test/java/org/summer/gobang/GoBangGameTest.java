package org.summer.gobang;

public class GoBangGameTest {

    public static void main(String[] args) {
        GoBangGame game = new GoBangGame();
        System.out.println(game);
        game.setChess(Player.PLAYER1, 0, 0);
        System.out.println(game);
        game.setChess(Player.PLAYER2, 1, 1);
        System.out.println(game);
        game.setChess(Player.PLAYER1, 0, 1);
        System.out.println(game);
        game.setChess(Player.PLAYER2, 2, 2);
        System.out.println(game);
        game.setChess(Player.PLAYER1, 0, 2);
        System.out.println(game);
        game.setChess(Player.PLAYER2, 3, 3);
        System.out.println(game);
        game.setChess(Player.PLAYER1, 0, 3);
        System.out.println(game);
        game.setChess(Player.PLAYER2, 4, 4);
        System.out.println(game);
        game.setChess(Player.PLAYER1, 12, 12);
        System.out.println(game);
        game.setChess(Player.PLAYER2, 5, 5);
        System.out.println(game);
    }

}
