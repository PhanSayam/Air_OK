package fr.utln.jmonkey.air_ok.model;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;
import fr.utln.jmonkey.air_ok.controller.physics.PaddleController;
import fr.utln.jmonkey.air_ok.model.Paddle;
import fr.utln.jmonkey.air_ok.model.Puck;
import fr.utln.jmonkey.air_ok.model.Table;


public class GameAppState extends BaseAppState {

    private Paddle paddle;

    private boolean twoPlayers;

    public GameAppState(boolean twoPlayers) {
        this.twoPlayers = twoPlayers;
    }
    @Override
    protected void initialize(Application app) {
        SimpleApplication simpleApp = (SimpleApplication) app;

        BulletAppState bullet = new BulletAppState();
        app.getStateManager().attach(bullet);

        simpleApp.getCamera().setLocation(new Vector3f(0, 30f, 35f));
        simpleApp.getCamera().lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        Table table = new Table(simpleApp.getAssetManager(), simpleApp.getRootNode(), bullet);
        table.initTable();

        Puck puck = new Puck(simpleApp.getAssetManager(), simpleApp.getRootNode(), bullet);
        puck.initPuck();

        Paddle paddle1 = new Paddle(simpleApp.getAssetManager(), simpleApp.getRootNode(), bullet, new Vector3f(0, 0.2f, 12f));
        paddle1.initPaddle();
        app.getStateManager().attach(new PaddleController(paddle1,1));

        if (twoPlayers) {
        Paddle paddle2 = new Paddle(simpleApp.getAssetManager(), simpleApp.getRootNode(), bullet, new Vector3f(0, 0.2f, -12f));
        paddle2.initPaddle();
        app.getStateManager().attach(new PaddleController(paddle2, 2));}

    }

    @Override protected void cleanup(Application app) {}
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}

