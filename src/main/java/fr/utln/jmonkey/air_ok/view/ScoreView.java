package fr.utln.jmonkey.air_ok.view;

import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

public class ScoreView {
    private Container container;
    private Label scoreP1;
    private Label scoreP2;

    public ScoreView() {
        container = new Container();
        scoreP1 = new Label("0");
        scoreP2 = new Label("0");
        container.addChild(scoreP1);
        container.addChild(scoreP2);
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public Label getScoreP1() {
        return scoreP1;
    }

    public void setScoreP1(Label scoreP1) {
        this.scoreP1 = scoreP1;
    }

    public Label getScoreP2() {
        return scoreP2;
    }

    public void setScoreP2(Label scoreP2) {
        this.scoreP2 = scoreP2;
    }
}

