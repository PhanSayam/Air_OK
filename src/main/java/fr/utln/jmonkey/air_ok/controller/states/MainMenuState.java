package fr.utln.jmonkey.air_ok.controller.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;

import fr.utln.jmonkey.air_ok.view.MainMenuView;

public class MainMenuState extends BaseAppState {
    private MainMenuView view;

    @Override
    protected void initialize(Application app) {
        view = new MainMenuView();

        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        float menuX = (screenWidth / 2f) - 100f;
        float menuY = (screenHeight / 2f) + 100f;

        view.getMainContainer().setLocalTranslation(menuX, menuY, 0);

        view.getOnePlayerButton().addClickCommands(source -> startOnePlayerGame());
        view.getTwoPlayerButton().addClickCommands(source -> startTwoPlayerGame());
        view.getTournamentButton().addClickCommands(source -> startTournament());
        view.getQuitButton().addClickCommands(source -> app.stop());
    }

    private void startOnePlayerGame() {
        getStateManager().detach(this);
        getStateManager().attach(new GameState(GameState.GameMode.SINGLE_PLAYER));
    }

    private void startTwoPlayerGame() {
        getStateManager().detach(this);
        BaseAppState game = new GameState(GameState.GameMode.TWO_PLAYER);
        getStateManager().attach(game);
    }

    private void startTournament() {
        getStateManager().detach(this);
        getStateManager().attach(new EndScreenState());
    }

    @Override
    protected void onEnable() {
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(view.getMainContainer());
    }

    @Override
    protected void onDisable() {
        view.getMainContainer().removeFromParent();
    }

    @Override
    protected void cleanup(Application app) {
        /* Optional cleanup */ }
}