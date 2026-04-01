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

        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));

        inputManager.addListener(analogListener, "Left", "Right");
    }

    @Override
    protected void cleanup(Application application) {
        inputManager.deleteMapping("Left");
        inputManager.deleteMapping("Right");
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

            if (name.equals("Right")) {
                paddle.move(currentPos.add(speed, 0, 0));
            }
            if (name.equals("Left")) {
                paddle.move(currentPos.add(-speed, 0, 0));
            }
        }
    };
}
