package fr.utln.jmonkey.air_ok;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

import fr.utln.jmonkey.air_ok.controller.states.MainMenuState;

public class MainGameApp extends SimpleApplication {

    public static void main(String[] args) {
        MainGameApp app = new MainGameApp();
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1920, 1080);
        settings.setResizable(true);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Disable jME default ESC->exit mapping to let states handle ESC behavior.
        if (inputManager.hasMapping("SIMPLEAPP_Exit")) {
            inputManager.deleteMapping("SIMPLEAPP_Exit");
        }

        MainMenuState menuState = new MainMenuState();

        stateManager.attach(menuState);

        // desactiver l'overlay
        setDisplayStatView(false);
        setDisplayFps(false);

        // dans le menu on doit pas pouvoir bouger la camera
        flyCam.setEnabled(false);

        // on affiche la souris
        inputManager.setCursorVisible(true);

    }
}