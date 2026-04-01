package fr.utln.jmonkey.air_ok.view;

import com.simsilica.lemur.Container;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;

public class MainMenuView {
    private Container mainContainer;
    private Button startButton;
    private Button quitButton;

    public MainMenuView(String title) {
        mainContainer = new Container();
        mainContainer.addChild(new Label(title));
        startButton = mainContainer.addChild(new Button("Lancer une partie"));
        quitButton = mainContainer.addChild(new Button("Quitter"));
    }

    public Container getMainContainer() {
        return mainContainer;
    }

    public Button getStartButton() {
        return startButton;
    }

    public Button getQuitButton() {
        return quitButton;
    }
}