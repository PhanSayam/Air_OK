package fr.utln.jmonkey.air_ok.controller.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;

import fr.utln.jmonkey.air_ok.controller.physics.PaddleController;
import fr.utln.jmonkey.air_ok.model.GameAppState;
import fr.utln.jmonkey.air_ok.model.GameState;
import fr.utln.jmonkey.air_ok.model.MenuModel;
import fr.utln.jmonkey.air_ok.model.Paddle;
import fr.utln.jmonkey.air_ok.view.MainMenuView;

public class MainMenuState extends BaseAppState {
    private MainMenuView view;
    private MenuModel model;

    @Override
    protected void initialize(Application app) {
        model = new MenuModel();
        view = new MainMenuView(model.getGameTitle());

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
        getStateManager().attach(new GameAppState());
    }

    private void startTwoPlayerGame() {
        getStateManager().detach(this);
        getStateManager().attach(new EndScreenState());
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