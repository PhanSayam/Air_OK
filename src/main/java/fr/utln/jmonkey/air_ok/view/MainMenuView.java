package fr.utln.jmonkey.air_ok.view;

import com.simsilica.lemur.Container;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;

public class MainMenuView {
    private Container mainContainer;
    private Button onePlayerButton;
    private Button twoPlayerButton;
    private Button tournamentButton;
    private Button quitButton;

    public MainMenuView() {
        mainContainer = new Container();
        mainContainer.addChild(new Label("Air OK"));
        onePlayerButton = mainContainer.addChild(new Button("1 Joueur"));
        twoPlayerButton = mainContainer.addChild(new Button("2 Joueurs"));
        tournamentButton = mainContainer.addChild(new Button("Tournoi"));
        quitButton = mainContainer.addChild(new Button("Quitter"));
    }

    public Container getMainContainer() {
        return mainContainer;
    }

    public Button getOnePlayerButton() {
        return onePlayerButton;
    }

    public Button getTwoPlayerButton() {
        return twoPlayerButton;
    }

    public Button getTournamentButton() {
        return tournamentButton;
    }

    public Button getQuitButton() {
        return quitButton;
    }
}