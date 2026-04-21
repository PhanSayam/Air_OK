package fr.utln.jmonkey.air_ok.model;

public class TournamentManager {

    public static final int TOTAL_ROUNDS = 5;

    private static final String[] OPPONENT_NAMES = {
        "Le Debutant", "Le Rookie", "Le Challenger", "Le Veteran", "Le Champion"
    };

    private static final float[] AI_SPEED_MULTIPLIERS = {
        0.58f, 0.72f, 0.88f, 1.05f, 1.22f
    };

    private static final float[] AI_REACTION_DELAYS = {
        0.90f, 0.65f, 0.45f, 0.25f, 0.08f
    };

    private int currentRound = 0;

    public int getCurrentRound() {
        return currentRound;
    }

    public String getCurrentOpponentName() {
        return OPPONENT_NAMES[Math.min(currentRound, OPPONENT_NAMES.length - 1)];
    }

    public float getCurrentAiSpeedMultiplier() {
        return AI_SPEED_MULTIPLIERS[Math.min(currentRound, AI_SPEED_MULTIPLIERS.length - 1)];
    }

    public float getCurrentAiReactionDelay() {
        return AI_REACTION_DELAYS[Math.min(currentRound, AI_REACTION_DELAYS.length - 1)];
    }

    public void advanceRound() {
        currentRound++;
    }

    public boolean isTournamentComplete() {
        return currentRound >= TOTAL_ROUNDS;
    }
}
