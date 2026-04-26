package fr.utln.jmonkey.air_ok.controller.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;

import java.util.List;

import fr.utln.jmonkey.air_ok.model.Paddle;
import fr.utln.jmonkey.air_ok.model.Puck;
import fr.utln.jmonkey.air_ok.replay.ReplayFrame;

public class KillCamState extends BaseAppState {

    public static final float REPLAY_SPEED = 0.3f;
    public static final float REPLAY_BUFFER_SECONDS = 8f;

    private static final float RECORD_FPS = 60f;
    private static final float FRAME_INTERVAL = 1f / RECORD_FPS;

    // Camera offset from puck (units match game scale: table ~2400 long)
    private static final float CAM_BEHIND  = 180f;
    private static final float CAM_HEIGHT  = 65f;
    private static final float CAM_AHEAD   = 350f;
    private static final float CAM_FOV     = 72f;
    /** Exponential smoothing speed for camera position and look-at (higher = snappier). */
    private static final float CAM_SMOOTH  = 6f;

    private static final String ACTION_SKIP = "KillCamSkip";

    private final List<ReplayFrame> frames;
    private final Puck puck;
    private final Paddle p1Paddle;
    private final Paddle p2Paddle;          // null when AI game
    private final BulletAppState bulletAppState;
    private final ViewPort leftViewPort;    // null in single-player
    private final ViewPort rightViewPort;
    private final GameState gameState;
    private final AppState nextState;

    private SimpleApplication app;
    private float playbackTime = 0f;
    private boolean finished = false;

    // Smoothed camera state — null until first frame
    private Vector3f smoothCamPos = null;
    private Vector3f smoothLookAt = null;

    private BitmapText replayLabel;
    private ActionListener skipListener;

    public KillCamState(List<ReplayFrame> frames,
                        Puck puck, Paddle p1Paddle, Paddle p2Paddle,
                        BulletAppState bulletAppState,
                        ViewPort leftViewPort, ViewPort rightViewPort,
                        GameState gameState, AppState nextState) {
        this.frames         = frames;
        this.puck           = puck;
        this.p1Paddle       = p1Paddle;
        this.p2Paddle       = p2Paddle;
        this.bulletAppState = bulletAppState;
        this.leftViewPort   = leftViewPort;
        this.rightViewPort  = rightViewPort;
        this.gameState      = gameState;
        this.nextState      = nextState;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;

        // Freeze physics so the simulation doesn't override replay positions
        if (bulletAppState != null) {
            bulletAppState.setEnabled(false);
        }

        // Ensure a single full-screen viewport is active
        if (leftViewPort  != null) leftViewPort.setEnabled(false);
        if (rightViewPort != null) rightViewPort.setEnabled(false);
        this.app.getViewPort().setEnabled(true);
        this.app.getViewPort().setBackgroundColor(new ColorRGBA(0.12f, 0.16f, 0.22f, 1f));

        // Wide FOV for a dramatic ground-level feel
        this.app.getCamera().setFrustumPerspective(
                CAM_FOV,
                (float) this.app.getCamera().getWidth() / this.app.getCamera().getHeight(),
                1f, 50000f);

        setupLabel();
        registerSkip();

        if (frames.isEmpty()) {
            finish();
        }
    }

    private void setupLabel() {
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        replayLabel = new BitmapText(font);
        replayLabel.setSize(font.getCharSet().getRenderedSize() * 1.4f);
        replayLabel.setColor(new ColorRGBA(1f, 0.85f, 0.2f, 1f));
        replayLabel.setText("REPLAY  x0.5  [Espace pour passer]");
        float x = (app.getCamera().getWidth() - replayLabel.getLineWidth()) * 0.5f;
        float y = app.getCamera().getHeight() * 0.93f;
        replayLabel.setLocalTranslation(x, y, 1f);
        app.getGuiNode().attachChild(replayLabel);
    }

    private void registerSkip() {
        app.getInputManager().addMapping(ACTION_SKIP,
                new KeyTrigger(KeyInput.KEY_SPACE),
                new KeyTrigger(KeyInput.KEY_RETURN),
                new KeyTrigger(KeyInput.KEY_ESCAPE));
        skipListener = (name, isPressed, tpf) -> {
            if (isPressed) finish();
        };
        app.getInputManager().addListener(skipListener, ACTION_SKIP);
    }

    @Override
    public void update(float tpf) {
        if (finished || frames.isEmpty()) return;

        playbackTime += tpf * REPLAY_SPEED;

        int frameIndex = Math.min((int) (playbackTime / FRAME_INTERVAL), frames.size() - 1);
        ReplayFrame frame = frames.get(frameIndex);

        applyFrame(frame, frameIndex);
        updateCamera(frame, frameIndex, tpf);

        // End of replay
        if (playbackTime >= frames.size() * FRAME_INTERVAL) {
            finish();
        }
    }

    private void applyFrame(ReplayFrame frame, int frameIndex) {
        // resetPosition sets both the spatial and the physics body location
        puck.resetPosition(frame.puckPosition);
        p1Paddle.setPosition(frame.p1Position);
        if (p2Paddle != null) {
            p2Paddle.setPosition(frame.p2Position);
        }
    }

    private void updateCamera(ReplayFrame frame, int frameIndex, float tpf) {
        // Derive travel direction from adjacent frames
        Vector3f dir = getTravelDirection(frame, frameIndex);
        Vector3f puckPos = frame.puckPosition;

        Vector3f targetCamPos;
        Vector3f targetLookAt;

        if (dir.lengthSquared() > 0.001f) {
            Vector3f d = dir.normalize();
            targetCamPos = puckPos.subtract(d.mult(CAM_BEHIND)).addLocal(0f, CAM_HEIGHT, 0f);
            targetLookAt = puckPos.add(d.mult(CAM_AHEAD));
        } else {
            targetCamPos = puckPos.add(new Vector3f(0f, CAM_HEIGHT * 3f, CAM_BEHIND));
            targetLookAt = puckPos.clone();
        }

        // First frame: snap directly to avoid flying in from nowhere
        if (smoothCamPos == null) {
            smoothCamPos = targetCamPos.clone();
            smoothLookAt = targetLookAt.clone();
        } else {
            // Exponential smoothing — frame-rate independent
            float alpha = 1f - (float) Math.exp(-CAM_SMOOTH * tpf);
            smoothCamPos.interpolateLocal(targetCamPos, alpha);
            smoothLookAt.interpolateLocal(targetLookAt, alpha);
        }

        app.getCamera().setLocation(smoothCamPos);
        app.getCamera().lookAt(smoothLookAt, Vector3f.UNIT_Y);
    }

    private Vector3f getTravelDirection(ReplayFrame frame, int frameIndex) {
        if (frameIndex + 1 < frames.size()) {
            return frames.get(frameIndex + 1).puckPosition.subtract(frame.puckPosition);
        }
        if (frameIndex > 0) {
            return frame.puckPosition.subtract(frames.get(frameIndex - 1).puckPosition);
        }
        return new Vector3f(0f, 0f, 1f);
    }

    private void finish() {
        if (finished) return;
        finished = true;
        getStateManager().detach(this);
        getStateManager().detach(gameState);
        getStateManager().attach(nextState);
    }

    @Override
    protected void cleanup(Application app) {
        if (replayLabel != null) {
            replayLabel.removeFromParent();
            replayLabel = null;
        }
        if (skipListener != null) {
            app.getInputManager().removeListener(skipListener);
            skipListener = null;
        }
        if (app.getInputManager().hasMapping(ACTION_SKIP)) {
            app.getInputManager().deleteMapping(ACTION_SKIP);
        }
    }

    @Override protected void onEnable()  {}
    @Override protected void onDisable() {}
}
