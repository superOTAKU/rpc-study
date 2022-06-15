package org.summer.gobang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GoBangGame {
    private static final int TABLE_SIZE = 15;
    private static final int WIN_LEN = 5;
    private final Chess[][] table = new Chess[TABLE_SIZE][TABLE_SIZE];
    private GameState state = GameState.PLAYER1_ROUND;
    //下棋录像 TODO 悔棋...
    private final List<GameLog> logs = new ArrayList<>(64);

    public GoBangGame() {
        for (int i = 0; i < TABLE_SIZE; i++) {
            for(int j = 0; j < TABLE_SIZE; j++) {
                table[i][j] = Chess.NULL;
            }
        }
    }

    public void setChess(Player player, int x, int y) {
        Objects.requireNonNull(player, "player can't be null");
        Objects.checkIndex(x, TABLE_SIZE);
        Objects.checkIndex(y, TABLE_SIZE);
        checkState(player);
        if (!table[x][y].isNull()) {
            throw new RuntimeException(String.format("cell (%s,%s) has been filled", x, y));
        }
        table[x][y] = player.getChess();
        recordLog(player, x, y);
        changeState(player, isChessWin(player.getChess(), x, y));
    }

    public GameState getState() {
        return state;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GameState: ").append(state).append('\n');
        sb.append("ChessTable:\n");
        for (int i = 0; i < TABLE_SIZE; i++) {
            for (int j = 0; j < TABLE_SIZE; j++) {
                Chess chess = table[i][j];
                switch (chess) {
                    case NULL:
                        sb.append("- ");
                        break;
                    case BLACK:
                        sb.append("B ");
                        break;
                    case WHITE:
                        sb.append("W ");
                        break;
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void recordLog(Player player, int x, int y) {
        logs.add(new GameLog(player, x, y));
    }

    private void changeState(Player player, boolean win) {
        if (win) {
            state = player == Player.PLAYER1 ? GameState.PLAYER1_WIN : GameState.PLAYER2_WIN;
        } else {
            state = player == Player.PLAYER1 ? GameState.PLAYER2_ROUND : GameState.PLAYER1_ROUND;
        }
    }

    private void checkState(Player player) {
        if (state.isFinish()) {
            throw new RuntimeException("game is finish: " + state);
        }
        if (!state.isRoundMatch(player)) {
            throw new RuntimeException("not your round!");
        }
    }

    private boolean isChessWin(Chess chess, int x, int y) {
        return isWin(chess, x, y, TRANSLATOR_X) || isWin(chess, x, y, TRANSLATOR_Y)
                || isWin(chess, x, y, TRANSLATOR_LEFTTOP_RIGHTDOWN)
                || isWin(chess, x, y, TRANSLATOR_RIGHTTOP_LEFTDOWN);
    }


    private boolean isWin(Chess chess, int x, int y, Translator translator) {
        int len = 0;
        int delta = 1;
        boolean part1Ok = true;
        boolean part2Ok = true;
        while (delta <= WIN_LEN - 1) {
            Position part1 = translator.translatePart1(x, y, delta);
            Position part2 = translator.translatePart2(x, y, delta);
            if (part1Ok && part1.getX() >= 0 && part1.getX() < TABLE_SIZE
                    && part1.getY() >= 0 && part1.getY() < TABLE_SIZE && table[part1.getX()][part1.getY()] == chess) {
                len++;
            } else {
                part1Ok = false;
            }
            if (part2Ok && part2.getX() >= 0 && part2.getX() < TABLE_SIZE
                    && part2.getY() >= 0 && part2.getY() < TABLE_SIZE && table[part2.getX()][part2.getY()] == chess) {
                len++;
            } else {
                part2Ok = false;
            }
            if (!part1Ok && !part2Ok) {
                break;
            }
            delta++;
        }
        //包括自己
        return len + 1 == WIN_LEN;
    }

    interface Translator {
        Position translatePart1(int x, int y, int delta);
        Position translatePart2(int x, int y, int delta);
    }

    private static final Translator TRANSLATOR_X = new Translator() {
        @Override
        public Position translatePart1(int x, int y, int delta) {
            return new Position(x - delta, y);
        }

        @Override
        public Position translatePart2(int x, int y, int delta) {
            return new Position(x + delta, y);
        }
    };

    private static final Translator TRANSLATOR_Y = new Translator() {
        @Override
        public Position translatePart1(int x, int y, int delta) {
            return new Position(x, y - delta);
        }

        @Override
        public Position translatePart2(int x, int y, int delta) {
            return new Position(x, y + delta);
        }
    };

    private static final Translator TRANSLATOR_LEFTTOP_RIGHTDOWN = new Translator() {
        @Override
        public Position translatePart1(int x, int y, int delta) {
            return new Position(x - delta, y - delta);
        }

        @Override
        public Position translatePart2(int x, int y, int delta) {
            return new Position(x + delta, y + delta);
        }
    };

    private static final Translator TRANSLATOR_RIGHTTOP_LEFTDOWN = new Translator() {
        @Override
        public Position translatePart1(int x, int y, int delta) {
            return new Position(x + delta, y - delta);
        }

        @Override
        public Position translatePart2(int x, int y, int delta) {
            return new Position(x - delta, y + delta);
        }
    };


}
