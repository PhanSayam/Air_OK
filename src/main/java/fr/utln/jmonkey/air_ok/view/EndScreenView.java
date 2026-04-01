package fr.utln.jmonkey.air_ok.view;

import com.simsilica.lemur.Container;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;

public class EndScreenView {
    private Container mainContainer;
    private Label scoresLabel;
    private Button quitButton;
    private Button returnToMenuButton;

    public EndScreenView() {
        mainContainer = new Container();
        mainContainer.addChild(new Label("Fin de la partie"));
        scoresLabel = mainContainer.addChild(new Label("And the winner is player X with Y points!"));
        quitButton = mainContainer.addChild(new Button("Quitter l'application"));
        returnToMenuButton = mainContainer.addChild(new Button("Revenir au menu"));
    }

    public Container getMainContainer() {
        return mainContainer;
    }

    public Button getQuitButton() {
        return quitButton;
    }

    public Button getReturnToMenuButton() {
        return returnToMenuButton;
    }

    public Label getScoresLabel() {
        return scoresLabel;
    }
}