package fr.utln.jmonkey.air_ok.model;

import fr.utln.jmonkey.air_ok.model.rules.ScoreRules;

public class Player{
    private final String name;
    private int score;

    public Player(String name){
        this.name=name;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void resetScore(){
        this.score=0;
    }

    public void addPoint(int score){
        this.score = ScoreRules.addPointTo(score);
    }
}