package fr.utln.jmonkey.air_ok.model.rules;

import fr.utln.jmonkey.air_ok.model.Player;

public final class ScoreRules {

    private static final int WINNING_SCORE = 2;

    private ScoreRules() {
    }

    public static String formatScore(Player playerOne, Player playerTwo) {
        return playerOne.getName() + " : " + playerOne.getScore() + "    "
                + playerTwo.getName() + " : " + playerTwo.getScore();
    }

    public static boolean hasWinner(Player player) {
        return player.getScore() >= WINNING_SCORE;
    }
}
