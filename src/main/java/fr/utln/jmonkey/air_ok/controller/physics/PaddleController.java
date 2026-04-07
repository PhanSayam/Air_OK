package fr.utln.jmonkey.air_ok.controller.physics;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import fr.utln.jmonkey.air_ok.model.Paddle;


import java.awt.event.MouseAdapter;

public class PaddleController extends BaseAppState {

    private Paddle paddle;
    private InputManager inputManager;
    private int playerId;

    public PaddleController(Paddle paddle, int playerId) {
        this.paddle = paddle;
        this.playerId = playerId;
    }


    @Override
    protected void initialize(Application app) {
        this.inputManager = app.getInputManager();

        if (playerId == 1) {
            inputManager.addMapping(playerId + "Left", new KeyTrigger(KeyInput.KEY_LEFT));
            inputManager.addMapping(playerId + "Right", new KeyTrigger(KeyInput.KEY_RIGHT));
            inputManager.addMapping(playerId + "Up", new KeyTrigger(KeyInput.KEY_UP));
            inputManager.addMapping(playerId + "Down", new KeyTrigger(KeyInput.KEY_DOWN));
            inputManager.addListener(analogListener, "Left", "Right", "Up", "Down");
        }
        else {
            inputManager.addMapping(playerId + "Left",  new KeyTrigger(KeyInput.KEY_A));
            inputManager.addMapping(playerId + "Right", new KeyTrigger(KeyInput.KEY_D));
            inputManager.addMapping(playerId + "Up",    new KeyTrigger(KeyInput.KEY_W));
            inputManager.addMapping(playerId + "Down",  new KeyTrigger(KeyInput.KEY_S));
        }

        inputManager.addListener(analogListener,
                playerId + "Left",
                playerId + "Right",
                playerId + "Up",
                playerId + "Down");
    }

    @Override
    protected void cleanup(Application application) {

        inputManager.deleteMapping(playerId + "Left");
        inputManager.deleteMapping(playerId + "Right");
        inputManager.deleteMapping(playerId + "Up");
        inputManager.deleteMapping(playerId + "Down");
        inputManager.removeListener(analogListener);

    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }

    private final AnalogListener analogListener = new AnalogListener() {
        @Override
        public void onAnalog(String name, float value, float tpf) {
            float speed = 15f * tpf;
            Vector3f currentPos = paddle.getPosition();

            float minZ;
            float maxZ;

            if (playerId == 1) {
                minZ = 3f;
                maxZ = 14.5f;
            } else {
                minZ = -14.5f;
                maxZ = -3f;
            }

            if (name.equals(playerId + "Right") && currentPos.x < 7.9f) {
                paddle.move(currentPos.add(speed, 0, 0));
            }
            if (name.equals(playerId + "Left") && currentPos.x > -8f) {
                paddle.move(currentPos.add(-speed, 0, 0));
            }
            if (name.equals(playerId + "Down") && currentPos.z < maxZ) {
                paddle.move(currentPos.add(0, 0, speed));
            }
            if (name.equals(playerId + "Up") && currentPos.z > minZ) {
                paddle.move(currentPos.add(0, 0, -speed));
            }
        }
    };


}
