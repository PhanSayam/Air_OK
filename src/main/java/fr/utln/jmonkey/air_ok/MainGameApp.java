package fr.utln.jmonkey.air_ok;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;

import fr.utln.jmonkey.air_ok.controller.states.MainMenuState;

public class MainGameApp extends SimpleApplication {

    private BulletAppState bulletAppState;
    public static void main(String[] args) {
        MainGameApp app = new MainGameApp();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        // desactiver l'overlay
        setDisplayStatView(false);
        setDisplayFps(false);


        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        // dans le menu on doit pas pouvoir bouger la camera
        flyCam.setEnabled(false);

        // on affiche la souris
        inputManager.setCursorVisible(true);


        MainMenuState menuState = new MainMenuState();
        stateManager.attach(menuState);

    }
}