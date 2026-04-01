package fr.utln.jmonkey.air_ok.model;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;
import fr.utln.jmonkey.air_ok.controller.physics.PaddleController;

public class GameState extends SimpleApplication {

    public static void main(String[] args) {
        GameState app = new GameState();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);

        BulletAppState bullet = new BulletAppState();
        stateManager.attach(bullet);

        cam.setLocation(new Vector3f(0, 30f, 35f));
        cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);

        Table table = new Table(assetManager, rootNode, bullet);
        table.initTable();

        Puck puck = new Puck(assetManager, rootNode, bullet);
        puck.initPuck();

        Paddle paddle = new Paddle(assetManager, rootNode, bullet);
        paddle.initPaddle();

        PaddleController paddleControl = new PaddleController(paddle);
        stateManager.attach(paddleControl);


    }

}
