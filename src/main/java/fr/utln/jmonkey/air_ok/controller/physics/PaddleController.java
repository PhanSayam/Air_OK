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

    public PaddleController(Paddle paddle) {
        this.paddle = paddle;
    }





    @Override
    protected void initialize(Application app) {
        this.inputManager = app.getInputManager();

        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addListener(analogListener, "Left", "Right", "Up", "Down");

    }

    @Override
    protected void cleanup(Application application) {
        inputManager.deleteMapping("Left");
        inputManager.deleteMapping("Right");
        inputManager.deleteMapping("Up");
        inputManager.deleteMapping("Down");
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

            if (name.equals("Right") && currentPos.x < 7.9f) {
                paddle.move(currentPos.add(speed, 0, 0));
            }
            if (name.equals("Left") && currentPos.x > -8f) {
                paddle.move(currentPos.add(-speed, 0, 0));
            }
            if (name.equals("Down") && currentPos.z < 14.5f) {
                paddle.move(currentPos.add(0, 0, speed));
            }
            if (name.equals("Up") && currentPos.z > 3f) {
                paddle.move(currentPos.add(0, 0, -speed));
            }
        }
    };


}
