package fr.utln.jmonkey.air_ok.controller.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;

import fr.utln.jmonkey.air_ok.model.MenuModel;
import fr.utln.jmonkey.air_ok.view.EndScreenView;

public class EndScreenState extends BaseAppState {
    private EndScreenView view;
    private MenuModel model;

    @Override
    protected void initialize(Application app) {
        model = new MenuModel();
        view = new EndScreenView();

        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        float menuX = (screenWidth / 2f) - 100f;
        float menuY = (screenHeight / 2f) + 100f;

        view.getMainContainer().setLocalTranslation(menuX, menuY, 0);

        view.getQuitButton().addClickCommands(source -> app.stop());
        view.getReturnToMenuButton().addClickCommands(source -> returnToMenu());
    }

    private void returnToMenu() {
        getStateManager().detach(this);
        getStateManager().attach(new MainMenuState());
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