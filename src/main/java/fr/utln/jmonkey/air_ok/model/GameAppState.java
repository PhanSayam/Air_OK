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

    @Override
    protected void initialize(Application app) {
        SimpleApplication simpleApp = (SimpleApplication) app;

        BulletAppState bullet = new BulletAppState();
        app.getStateManager().attach(bullet);

        simpleApp.getCamera().setLocation(new Vector3f(0, 30f, 35f));
        simpleApp.getCamera().lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        new Table(simpleApp.getAssetManager(), simpleApp.getRootNode(), bullet).initTable();
        new Puck(simpleApp.getAssetManager(), simpleApp.getRootNode(), bullet).initPuck();

        paddle = new Paddle(simpleApp.getAssetManager(), simpleApp.getRootNode(), bullet);
        paddle.initPaddle();

        app.getStateManager().attach(new PaddleController(paddle));

    }

    @Override protected void cleanup(Application app) {}
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}

