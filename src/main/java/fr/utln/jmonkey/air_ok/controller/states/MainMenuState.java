package fr.utln.jmonkey.air_ok.controller.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.shadow.DirectionalLightShadowRenderer;

import java.util.Random;

import fr.utln.jmonkey.air_ok.model.Paddle;
import fr.utln.jmonkey.air_ok.model.Puck;
import fr.utln.jmonkey.air_ok.model.Table;
import fr.utln.jmonkey.air_ok.model.TournamentManager;
import fr.utln.jmonkey.air_ok.view.MainMenuView;

public class MainMenuState extends BaseAppState {

    private static final float PREVIEW_TABLE_PLANE_Y = Puck.HALF_HEIGHT;
    private static final float PREVIEW_CENTER_NEUTRAL_HALF_DEPTH = 220f;
    private static final float PREVIEW_PADDLE_SPEED = 960f;
    private static final float PREVIEW_PADDLE_DEFENSIVE_Z = 1060f;
    private static final float PREVIEW_GOAL_MARGIN = 70f;
    private static final float PREVIEW_SERVE_COOLDOWN_SECONDS = 0.55f;
    private static final float PREVIEW_STUCK_SPEED_THRESHOLD = 35f;
    private static final float PREVIEW_STUCK_RESET_SECONDS = 2.4f;
    private static final float PREVIEW_MIN_PUCK_SPEED = 720f;
    private static final float PREVIEW_SERVE_MIN_SPEED = 1080f;
    private static final float PREVIEW_SERVE_MAX_SPEED = 1380f;
    private static final float PREVIEW_SERVE_SPREAD_X = 220f;
    private static final float PREVIEW_SERVE_RANDOM_X_VELOCITY = 400f;
    private static final float PREVIEW_STRIKE_OFFSET = 120f;
    private static final float PREVIEW_START_Z = 1200f;

    // Menu option indices
    private static final int OPT_ONE_PLAYER  = 0;
    private static final int OPT_TWO_PLAYER  = 1;
    private static final int OPT_TOURNAMENT  = 2;
    private static final int OPT_QUIT        = 3;

    private static final String MENU_UP                  = "MainMenuUp";
    private static final String MENU_DOWN                = "MainMenuDown";
    private static final String MENU_SELECT              = "MainMenuSelect";
    private static final String MENU_CLICK               = "MainMenuClick";
    private static final String MENU_SHORTCUT_ONE        = "MainMenuShortcutOne";
    private static final String MENU_SHORTCUT_TWO        = "MainMenuShortcutTwo";
    private static final String MENU_SHORTCUT_TOURNAMENT = "MainMenuShortcutTournament";
    private static final String MENU_SHORTCUT_QUIT       = "MainMenuShortcutQuit";

    private SimpleApplication app;
    private MainMenuView view;
    private BulletAppState menuPreviewBulletAppState;
    private Node menuPreviewNode;
    private Table menuPreviewTable;
    private Puck menuPreviewPuck;
    private Paddle menuPreviewNearPaddle;
    private Paddle menuPreviewFarPaddle;
    private int selectedIndex;
    private float selectionPulseTimer;
    private float previewServeCooldownSeconds;
    private float previewStuckTimerSeconds;
    private DirectionalLightShadowRenderer menuShadowRenderer;

    private final Random random = new Random();

    private final ActionListener inputListener = (name, isPressed, tpf) -> {
        if (!isPressed) {
            return;
        }

        switch (name) {
            case MENU_UP                  -> moveSelection(-1);
            case MENU_DOWN                -> moveSelection(1);
            case MENU_SELECT              -> activateSelection(selectedIndex);
            case MENU_CLICK               -> {
                int hovered = view.pickOption(app.getInputManager().getCursorPosition());
                if (hovered >= 0) {
                    selectedIndex = hovered;
                    view.setSelectedIndex(selectedIndex);
                    activateSelection(selectedIndex);
                }
            }
            case MENU_SHORTCUT_ONE        -> activateSelection(OPT_ONE_PLAYER);
            case MENU_SHORTCUT_TWO        -> activateSelection(OPT_TWO_PLAYER);
            case MENU_SHORTCUT_TOURNAMENT -> activateSelection(OPT_TOURNAMENT);
            case MENU_SHORTCUT_QUIT       -> activateSelection(OPT_QUIT);
        }
    };

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        view = new MainMenuView(app.getAssetManager(), app.getCamera().getWidth(), app.getCamera().getHeight());
        selectedIndex = 0;
        selectionPulseTimer = 0f;
        view.setSelectedIndex(selectedIndex);
        registerInputMappings();
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        updateHoverSelection();
        selectionPulseTimer += tpf;
        view.updateSelectionPulse(selectionPulseTimer);
        updateMenuPreviewMatch(tpf);
    }

    private void moveSelection(int delta) {
        int count = view.getOptionCount();
        selectedIndex = (selectedIndex + delta + count) % count;
        view.setSelectedIndex(selectedIndex);
    }

    private void updateHoverSelection() {
        if (app == null || view == null || !app.getInputManager().isCursorVisible()) {
            return;
        }

        int hovered = view.pickOption(app.getInputManager().getCursorPosition());
        if (hovered >= 0 && hovered != selectedIndex) {
            selectedIndex = hovered;
            view.setSelectedIndex(selectedIndex);
        }
    }

    private void activateSelection(int optionIndex) {
        switch (optionIndex) {
            case OPT_ONE_PLAYER  -> startOnePlayerGame();
            case OPT_TWO_PLAYER  -> startTwoPlayerGame();
            case OPT_TOURNAMENT  -> startTournament();
            case OPT_QUIT        -> app.stop();
            default              -> { }
        }
    }

    private void startOnePlayerGame() {
        getStateManager().detach(this);
        getStateManager().attach(new GameState(GameState.GameMode.SINGLE_PLAYER));
    }

    private void startTwoPlayerGame() {
        getStateManager().detach(this);
        getStateManager().attach(new GameState(GameState.GameMode.TWO_PLAYER));
    }

    private void startTournament() {
        TournamentManager tournament = new TournamentManager();
        GameState.GameConfig cfg = new GameState.GameConfig(GameState.GameMode.SINGLE_PLAYER);
        cfg.tournament        = tournament;
        cfg.aiSpeedMultiplier = tournament.getCurrentAiSpeedMultiplier();
        cfg.aiReactionDelay   = tournament.getCurrentAiReactionDelay();
        cfg.opponentName      = tournament.getCurrentOpponentName();
        cfg.playerOneName     = "Joueur 1";

        getStateManager().detach(this);
        getStateManager().attach(new GameState(cfg));
    }

    // --------------- Input registration --------------------------------------

    private void registerInputMappings() {
        if (!app.getInputManager().hasMapping(MENU_UP)) {
            app.getInputManager().addMapping(MENU_UP,
                    new KeyTrigger(KeyInput.KEY_UP),
                    new KeyTrigger(KeyInput.KEY_Z),
                    new KeyTrigger(KeyInput.KEY_W));
        }
        if (!app.getInputManager().hasMapping(MENU_DOWN)) {
            app.getInputManager().addMapping(MENU_DOWN,
                    new KeyTrigger(KeyInput.KEY_DOWN),
                    new KeyTrigger(KeyInput.KEY_S));
        }
        if (!app.getInputManager().hasMapping(MENU_SELECT)) {
            app.getInputManager().addMapping(MENU_SELECT,
                    new KeyTrigger(KeyInput.KEY_RETURN),
                    new KeyTrigger(KeyInput.KEY_SPACE));
        }
        if (!app.getInputManager().hasMapping(MENU_CLICK)) {
            app.getInputManager().addMapping(MENU_CLICK,
                    new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        }
        if (!app.getInputManager().hasMapping(MENU_SHORTCUT_ONE)) {
            app.getInputManager().addMapping(MENU_SHORTCUT_ONE, new KeyTrigger(KeyInput.KEY_1));
        }
        if (!app.getInputManager().hasMapping(MENU_SHORTCUT_TWO)) {
            app.getInputManager().addMapping(MENU_SHORTCUT_TWO, new KeyTrigger(KeyInput.KEY_2));
        }
        if (!app.getInputManager().hasMapping(MENU_SHORTCUT_TOURNAMENT)) {
            app.getInputManager().addMapping(MENU_SHORTCUT_TOURNAMENT, new KeyTrigger(KeyInput.KEY_T));
        }
        if (!app.getInputManager().hasMapping(MENU_SHORTCUT_QUIT)) {
            app.getInputManager().addMapping(MENU_SHORTCUT_QUIT, new KeyTrigger(KeyInput.KEY_ESCAPE));
        }
    }

    // --------------- Preview match -------------------------------------------

    private void setupMenuPreviewBackground() {
        menuPreviewBulletAppState = new BulletAppState();
        getStateManager().attach(menuPreviewBulletAppState);

        menuPreviewNode = new Node("MainMenuPreviewNode");
        app.getRootNode().attachChild(menuPreviewNode);

        menuPreviewTable = new Table(app.getAssetManager(), menuPreviewNode, menuPreviewBulletAppState);
        menuPreviewTable.initTable();
        setupMenuShadows();

        menuPreviewPuck = new Puck(app.getAssetManager(), menuPreviewNode, menuPreviewBulletAppState);
        menuPreviewPuck.initPuck();

        menuPreviewNearPaddle = new Paddle(app.getAssetManager(), menuPreviewNode, menuPreviewBulletAppState,
                new Vector3f(0f, PREVIEW_TABLE_PLANE_Y, getPreviewStartZ(true)), ColorRGBA.Red);
        menuPreviewNearPaddle.initPaddle();

        menuPreviewFarPaddle = new Paddle(app.getAssetManager(), menuPreviewNode, menuPreviewBulletAppState,
                new Vector3f(0f, PREVIEW_TABLE_PLANE_Y, getPreviewStartZ(false)), ColorRGBA.Blue);
        menuPreviewFarPaddle.initPaddle();

        previewServeCooldownSeconds = 0f;
        previewStuckTimerSeconds = 0f;
        launchPreviewServe(random.nextBoolean() ? 1f : -1f);
    }

    private void updateMenuPreviewMatch(float tpf) {
        if (menuPreviewTable == null || menuPreviewPuck == null || menuPreviewNearPaddle == null
                || menuPreviewFarPaddle == null) {
            return;
        }

        if (previewServeCooldownSeconds > 0f) {
            previewServeCooldownSeconds = Math.max(0f, previewServeCooldownSeconds - tpf);
        }

        updatePreviewAIPaddle(menuPreviewNearPaddle, true, tpf);
        updatePreviewAIPaddle(menuPreviewFarPaddle, false, tpf);

        menuPreviewNearPaddle.constrainToTablePlane(PREVIEW_TABLE_PLANE_Y);
        menuPreviewFarPaddle.constrainToTablePlane(PREVIEW_TABLE_PLANE_Y);
        menuPreviewPuck.constrainToTablePlane(PREVIEW_TABLE_PLANE_Y);

        sustainPreviewPuckSpeed();
        resetPreviewOnGoal();
        resetPreviewWhenStuck(tpf);
    }

    private void updatePreviewAIPaddle(Paddle paddle, boolean onPositiveZSide, float tpf) {
        Vector3f puckPosition = menuPreviewPuck.getPosition();
        Vector3f puckVelocity = menuPreviewPuck.getVelocity();
        Vector3f paddlePosition = paddle.getPosition();

        float targetX;
        float targetZ;
        boolean puckApproaching = onPositiveZSide ? puckVelocity.z > 12f : puckVelocity.z < -12f;

        if (puckApproaching) {
            targetX = puckPosition.x + puckVelocity.x * 0.16f;
            float strikeOffset = onPositiveZSide ? -PREVIEW_STRIKE_OFFSET : PREVIEW_STRIKE_OFFSET;
            targetZ = puckPosition.z + strikeOffset;
        } else {
            targetX = puckPosition.x * 0.34f;
            float defensiveZ = getPreviewPaddleDefensiveZ();
            targetZ = onPositiveZSide ? defensiveZ : -defensiveZ;
        }

        float maxStep = getPreviewPaddleSpeed() * tpf;
        float dx = targetX - paddlePosition.x;
        float dz = targetZ - paddlePosition.z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);

        float nextX = paddlePosition.x;
        float nextZ = paddlePosition.z;

        if (distance > 0.0001f) {
            float ratio = Math.min(1f, maxStep / distance);
            nextX += dx * ratio;
            nextZ += dz * ratio;
        }

        float halfWidth = menuPreviewTable.getWidth() / 2f;
        float halfLength = menuPreviewTable.getLength() / 2f;
        float radius = paddle.getRadius();
        float centerLimit = getPreviewCenterNeutralHalfDepth() + radius;

        nextX = Math.max(-(halfWidth - radius), Math.min(halfWidth - radius, nextX));
        if (onPositiveZSide) {
            nextZ = Math.max(centerLimit, Math.min(halfLength - radius, nextZ));
        } else {
            nextZ = Math.max(-(halfLength - radius), Math.min(-centerLimit, nextZ));
        }

        paddle.setPosition(new Vector3f(nextX, PREVIEW_TABLE_PLANE_Y, nextZ));
    }

    private void sustainPreviewPuckSpeed() {
        Vector3f velocity = menuPreviewPuck.getVelocity();
        float speed = velocity.length();
        float minPuckSpeed = getPreviewMinPuckSpeed();

        if (speed <= 0.05f || speed >= minPuckSpeed) {
            return;
        }

        menuPreviewPuck.getPhysicsControl().setLinearVelocity(velocity.normalize().mult(minPuckSpeed));
    }

    private void resetPreviewOnGoal() {
        if (previewServeCooldownSeconds > 0f) {
            return;
        }

        Vector3f puckPosition = menuPreviewPuck.getPosition();
        float halfLength = menuPreviewTable.getLength() / 2f;
        float goalHalfWidth = menuPreviewTable.getGoalWidth() / 2f;

        if (Math.abs(puckPosition.x) > goalHalfWidth) {
            return;
        }

        float goalMargin = getPreviewGoalMargin();
        if (puckPosition.z > halfLength + goalMargin) {
            launchPreviewServe(-1f);
        } else if (puckPosition.z < -halfLength - goalMargin) {
            launchPreviewServe(1f);
        }
    }

    private void resetPreviewWhenStuck(float tpf) {
        if (previewServeCooldownSeconds > 0f) {
            previewStuckTimerSeconds = 0f;
            return;
        }

        float speed = menuPreviewPuck.getVelocity().length();
        if (speed < PREVIEW_STUCK_SPEED_THRESHOLD) {
            previewStuckTimerSeconds += tpf;
        } else {
            previewStuckTimerSeconds = 0f;
        }

        if (previewStuckTimerSeconds >= PREVIEW_STUCK_RESET_SECONDS) {
            launchPreviewServe(random.nextBoolean() ? 1f : -1f);
        }
    }

    private void launchPreviewServe(float towardPositiveZ) {
        float serveX = (random.nextFloat() - 0.5f) * getPreviewServeSpreadX();
        menuPreviewPuck.resetPosition(new Vector3f(serveX, PREVIEW_TABLE_PLANE_Y, 0f));

        float serveMinSpeed = getPreviewServeMinSpeed();
        float serveMaxSpeed = getPreviewServeMaxSpeed();
        float speed = serveMinSpeed + random.nextFloat() * (serveMaxSpeed - serveMinSpeed);
        float vx = (random.nextFloat() - 0.5f) * PREVIEW_SERVE_RANDOM_X_VELOCITY;
        float vz = speed * (towardPositiveZ >= 0f ? 1f : -1f);

        menuPreviewPuck.getPhysicsControl().setLinearVelocity(new Vector3f(vx, 0f, vz));
        menuPreviewPuck.getPhysicsControl()
                .setAngularVelocity(new Vector3f(0f, (random.nextFloat() - 0.5f) * 10f, 0f));

        previewServeCooldownSeconds = PREVIEW_SERVE_COOLDOWN_SECONDS;
        previewStuckTimerSeconds = 0f;
    }

    private float getPreviewStartZ(boolean onPositiveZSide) {
        return onPositiveZSide ? PREVIEW_START_Z : -PREVIEW_START_Z;
    }

    private float getPreviewCenterNeutralHalfDepth() {
        return (menuPreviewTable == null) ? PREVIEW_CENTER_NEUTRAL_HALF_DEPTH
                : menuPreviewTable.getCenterNeutralHalfDepth();
    }

    private float getPreviewPaddleSpeed()    { return PREVIEW_PADDLE_SPEED; }
    private float getPreviewPaddleDefensiveZ() { return PREVIEW_PADDLE_DEFENSIVE_Z; }
    private float getPreviewGoalMargin()     { return PREVIEW_GOAL_MARGIN; }
    private float getPreviewMinPuckSpeed()   { return PREVIEW_MIN_PUCK_SPEED; }
    private float getPreviewServeMinSpeed()  { return PREVIEW_SERVE_MIN_SPEED; }
    private float getPreviewServeMaxSpeed()  { return PREVIEW_SERVE_MAX_SPEED; }
    private float getPreviewServeSpreadX()   { return PREVIEW_SERVE_SPREAD_X; }

    private void setupMenuShadows() {
        com.jme3.light.DirectionalLight keyLight = menuPreviewTable.getShadowKeyLight();
        if (keyLight == null) return;

        menuShadowRenderer = new DirectionalLightShadowRenderer(app.getAssetManager(), 2048, 3);
        menuShadowRenderer.setLight(keyLight);
        menuShadowRenderer.setShadowIntensity(0.55f);
        app.getViewPort().addProcessor(menuShadowRenderer);
    }

    private void setupMenuCamera() {
        app.getViewPort().setEnabled(true);
        app.getViewPort().setBackgroundColor(new ColorRGBA(0.12f, 0.16f, 0.22f, 1f));
        app.getCamera().setFrustumPerspective(
                45f,
                (float) app.getCamera().getWidth() / app.getCamera().getHeight(),
                1f,
                500000f);
        app.getCamera().setLocation(new Vector3f(0f, 3400f, 5600f));
        app.getCamera().lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);
    }

    // --------------- AppState lifecycle --------------------------------------

    @Override
    protected void onEnable() {
        setupMenuPreviewBackground();
        setupMenuCamera();
        app.getGuiNode().attachChild(view.getRootNode());
        app.getInputManager().setCursorVisible(true);
        app.getInputManager().addListener(
                inputListener,
                MENU_UP, MENU_DOWN, MENU_SELECT, MENU_CLICK,
                MENU_SHORTCUT_ONE, MENU_SHORTCUT_TWO, MENU_SHORTCUT_TOURNAMENT,
                MENU_SHORTCUT_QUIT);
    }

    @Override
    protected void onDisable() {
        view.getRootNode().removeFromParent();
        app.getInputManager().removeListener(inputListener);

        if (menuPreviewNode != null) {
            menuPreviewNode.removeFromParent();
            menuPreviewNode = null;
        }
        menuPreviewTable = null;
        menuPreviewPuck = null;
        menuPreviewNearPaddle = null;
        menuPreviewFarPaddle = null;
        previewServeCooldownSeconds = 0f;
        previewStuckTimerSeconds = 0f;

        if (menuPreviewBulletAppState != null && getStateManager().hasState(menuPreviewBulletAppState)) {
            getStateManager().detach(menuPreviewBulletAppState);
        }
        menuPreviewBulletAppState = null;

        if (menuShadowRenderer != null) {
            app.getViewPort().removeProcessor(menuShadowRenderer);
            menuShadowRenderer = null;
        }
    }

    @Override
    protected void cleanup(Application app) {
        deleteIfPresent(MENU_UP);
        deleteIfPresent(MENU_DOWN);
        deleteIfPresent(MENU_SELECT);
        deleteIfPresent(MENU_CLICK);
        deleteIfPresent(MENU_SHORTCUT_ONE);
        deleteIfPresent(MENU_SHORTCUT_TWO);
        deleteIfPresent(MENU_SHORTCUT_TOURNAMENT);
        deleteIfPresent(MENU_SHORTCUT_QUIT);
    }

    private void deleteIfPresent(String mapping) {
        if (this.app.getInputManager().hasMapping(mapping)) {
            this.app.getInputManager().deleteMapping(mapping);
        }
    }
}
