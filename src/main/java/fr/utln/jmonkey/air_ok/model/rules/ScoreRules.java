package fr.utln.jmonkey.air_ok.model.rules;

public class ScoreRules{
    public static final int WINNING_SCORE = 12;

    public static int addPointTo(int score){
        score = score + 1;
        return score;
    }

    public static int ownGoal(int score){
        return Math.max(0, score-1);
    }

    public static boolean hasWon(int score){
        return score >= WINNING_SCORE;
    }
}