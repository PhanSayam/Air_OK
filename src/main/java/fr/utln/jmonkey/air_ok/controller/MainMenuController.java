package fr.utln.jmonkey.air_ok.controller;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;

import fr.utln.jmonkey.air_ok.model.MenuModel;
import fr.utln.jmonkey.air_ok.view.MainMenuView;

public class MainMenuController extends BaseAppState {
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

        view.getStartButton().addClickCommands(source -> startGame());
        view.getQuitButton().addClickCommands(source -> app.stop());
    }

    private void startGame() {

        this.setEnabled(false);
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