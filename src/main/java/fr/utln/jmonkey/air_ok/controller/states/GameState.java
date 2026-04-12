package fr.utln.jmonkey.air_ok.controller.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;

import java.util.Locale;
import java.util.Random;

import fr.utln.jmonkey.air_ok.model.Paddle;
import fr.utln.jmonkey.air_ok.model.Player;
import fr.utln.jmonkey.air_ok.model.PowerUp;
import fr.utln.jmonkey.air_ok.model.Puck;
import fr.utln.jmonkey.air_ok.model.Table;
import fr.utln.jmonkey.air_ok.model.rules.ScoreRules;

public class GameState extends BaseAppState implements PhysicsCollisionListener {

    public enum GameMode {
        SINGLE_PLAYER,
        TWO_PLAYER
    }

    private static final float GOAL_MARGIN = 0.6f;
    private static final float PADDLE_SPEED = 14f;
    private static final float AI_PADDLE_SPEED = 11.5f;
    private static final float AI_REACTION_DELAY_AFTER_SERVE_SECONDS = 0.45f;
    private static final float TABLE_PLANE_Y = Puck.HALF_HEIGHT;
    private static final float CENTER_NEUTRAL_HALF_DEPTH = 2.2f;
    private static final float PLAYER_ONE_START_Z = 12f;
    private static final float PLAYER_TWO_START_Z = -12f;
    private static final float AI_DEFENSIVE_LINE_Z = -10.5f;
    private static final float AI_INTERCEPT_LINE_Z = -7.2f;
    private static final float AI_ATTACK_OFFSET = 1.35f;
    private static final float SERVE_SAFE_PADDING = 0.25f;
    private static final float GOAL_DETECTION_COOLDOWN_SECONDS = 0.75f;
    private static final float SERVE_ARM_DISTANCE = 1.2f;
    private static final float STUCK_SPEED_THRESHOLD = 0.18f;
    private static final float STUCK_TIME_THRESHOLD_SECONDS = 2.2f;
    private static final float SMASH_MIN_SPEED = 8.5f;
    private static final float LIFT_MIN_SPEED = 4.5f;
    private static final float FLIP_MAX_SPEED = 5.5f;
    private static final float MAX_SPIN_Y = 22f;
    private static final float SHOT_DEBUG_DISPLAY_SECONDS = 1.2f;
    private static final float POWER_UP_SPAWN_MIN_SECONDS = 6f;
    private static final float POWER_UP_SPAWN_MAX_SECONDS = 13f;
    private static final float POWER_UP_EDGE_PADDING = 2.6f;
    private static final float POWER_UP_GOAL_PADDING = 3.2f;
    private static final float POWER_UP_SAFE_DISTANCE_FROM_PADDLE = 3.5f;
    private static final float POWER_UP_SAFE_DISTANCE_FROM_PUCK = 2.5f;
    private static final float POWER_UP_PUCK_EFFECT_DURATION_SECONDS = 10f;
    private static final float POWER_UP_PADDLE_EFFECT_DURATION_SECONDS = 20f;
    private static final float POWER_UP_SPEED_MULTIPLIER = 1.5f;
    private static final float POWER_UP_PUCK_SIZE_PLUS_SCALE = 1.2f;
    private static final float POWER_UP_PUCK_SIZE_MINUS_SCALE = 0.8f;
    private static final float POWER_UP_PADDLE_SIZE_PLUS_SCALE = 1.30f;
    private static final float POWER_UP_PADDLE_SIZE_MINUS_SCALE = 0.70f;
    private static final float SHOT_ON_GOAL_MIN_SPEED = 9f;

    private static final int HUMAN_PADDLE_GROUP = PhysicsCollisionObject.COLLISION_GROUP_02;
    private static final int AI_PADDLE_GROUP = PhysicsCollisionObject.COLLISION_GROUP_03;
    private static final int PLAYER2_PADDLE_GROUP = PhysicsCollisionObject.COLLISION_GROUP_04;

    private static final String P1_MOVE_LEFT = "P1MoveLeft";
    private static final String P1_MOVE_RIGHT = "P1MoveRight";
    private static final String P1_MOVE_UP = "P1MoveUp";
    private static final String P1_MOVE_DOWN = "P1MoveDown";
    private static final String P2_MOVE_LEFT = "P2MoveLeft";
    private static final String P2_MOVE_RIGHT = "P2MoveRight";
    private static final String P2_MOVE_UP = "P2MoveUp";
    private static final String P2_MOVE_DOWN = "P2MoveDown";
    private static final String TOGGLE_DEBUG = "ToggleDebug";
    private static final String RETURN_TO_MENU = "ReturnToMenu";

    private enum Side {
        PLAYER_ONE,
        PLAYER_TWO,
        NONE
    }

    private Puck puck;
    private Paddle playerOnePaddle;
    private Paddle playerTwoPaddle;
    private Paddle aiPaddle;
    private SimpleApplication simpleApp;
    private BulletAppState bulletAppState;
    private Node gameNode;
    private ViewPort leftPlayerViewPort;
    private ViewPort rightPlayerViewPort;
    private Table table;
    private Player playerOne;
    private Player playerTwo;
    private BitmapText scoreText;
    private BitmapText shotDebugText;
    private BitmapText powerUpStatusText;
    private ActionListener paddleInputListener;

    private boolean p1MoveLeft;
    private boolean p1MoveRight;
    private boolean p1MoveUp;
    private boolean p1MoveDown;
    private boolean p2MoveLeft;
    private boolean p2MoveRight;
    private boolean p2MoveUp;
    private boolean p2MoveDown;
    private boolean gameFinished;
    private final GameMode gameMode;
    private Side lastTouchSide = Side.NONE;
    private Side currentServer = Side.PLAYER_ONE;
    private float stuckTimerSeconds;
    private boolean rallyInProgress;
    private Vector3f playerOnePaddleVelocity = Vector3f.ZERO;
    private Vector3f playerTwoPaddleVelocity = Vector3f.ZERO;
    private Vector3f aiPaddleVelocity = Vector3f.ZERO;
    private float shotDebugTimerSeconds;
    private float goalDetectionCooldownSeconds;
    private float aiServeDelaySeconds;
    private Vector3f lastServePosition = Vector3f.ZERO;
    private final Random random = new Random();
    private PowerUp activePowerUp;
    private float powerUpSpawnTimerSeconds;
    private float puckSpeedBoostTimerSeconds;
    private float puckSpeedBoostMinSpeed;
    private float puckSizeTimerSeconds;
    private float puckSizeScale = 1f;
    private float playerOnePaddleSizeTimerSeconds;
    private float playerTwoPaddleSizeTimerSeconds;
    private float playerOnePaddleSizeScale = 1f;
    private float playerTwoPaddleSizeScale = 1f;

    public GameState() {
        this(GameMode.SINGLE_PLAYER);
    }

    public GameState(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    @Override
    protected void initialize(Application app) {
        simpleApp = (SimpleApplication) app;

        bulletAppState = new BulletAppState();
        getStateManager().attach(bulletAppState);
        bulletAppState.getPhysicsSpace().addCollisionListener(this);

        gameNode = new Node("GameNode");
        simpleApp.getRootNode().attachChild(gameNode);

        setupCameras();

        table = new Table(simpleApp.getAssetManager(), gameNode, bulletAppState);
        table.initTable();

        puck = new Puck(simpleApp.getAssetManager(), gameNode, bulletAppState);
        puck.initPuck();

        playerOnePaddle = new Paddle(simpleApp.getAssetManager(), gameNode, bulletAppState,
                new Vector3f(0f, TABLE_PLANE_Y, PLAYER_ONE_START_Z), ColorRGBA.Cyan);
        playerOnePaddle.initPaddle();
        configureHumanPaddleCollisions();

        if (gameMode == GameMode.SINGLE_PLAYER) {
            aiPaddle = new Paddle(simpleApp.getAssetManager(), gameNode, bulletAppState,
                    new Vector3f(0f, TABLE_PLANE_Y, PLAYER_TWO_START_Z), ColorRGBA.Orange);
            aiPaddle.initPaddle();
            configureAIPaddleCollisions();
        } else {
            playerTwoPaddle = new Paddle(simpleApp.getAssetManager(), gameNode, bulletAppState,
                    new Vector3f(0f, TABLE_PLANE_Y, PLAYER_TWO_START_Z), ColorRGBA.Green);
            playerTwoPaddle.initPaddle();
            configurePlayerTwoPaddleCollisions();
        }

        playerOne = new Player("Joueur 1");
        playerTwo = new Player("Joueur 2");

        setupScoreDisplay(simpleApp.getAssetManager());
        setupPaddleInput();
        updateScoreText();
        startExchange(Side.PLAYER_ONE);
        scheduleNextPowerUpSpawn();
    }

    private void setupPaddleInput() {
        // Support AZERTY (ZQSD) and QWERTY (WASD) layouts.
        simpleApp.getInputManager().addMapping(P1_MOVE_LEFT, new KeyTrigger(KeyInput.KEY_Q),
                new KeyTrigger(KeyInput.KEY_A));
        simpleApp.getInputManager().addMapping(P1_MOVE_RIGHT, new KeyTrigger(KeyInput.KEY_D));
        simpleApp.getInputManager().addMapping(P1_MOVE_UP, new KeyTrigger(KeyInput.KEY_Z),
                new KeyTrigger(KeyInput.KEY_W));
        simpleApp.getInputManager().addMapping(P1_MOVE_DOWN, new KeyTrigger(KeyInput.KEY_S));

        simpleApp.getInputManager().addMapping(P2_MOVE_LEFT, new KeyTrigger(KeyInput.KEY_LEFT));
        simpleApp.getInputManager().addMapping(P2_MOVE_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT));
        simpleApp.getInputManager().addMapping(P2_MOVE_UP, new KeyTrigger(KeyInput.KEY_UP));
        simpleApp.getInputManager().addMapping(P2_MOVE_DOWN, new KeyTrigger(KeyInput.KEY_DOWN));
        simpleApp.getInputManager().addMapping(TOGGLE_DEBUG, new KeyTrigger(KeyInput.KEY_H));
        simpleApp.getInputManager().addMapping(RETURN_TO_MENU, new KeyTrigger(KeyInput.KEY_ESCAPE));

        paddleInputListener = (name, isPressed, tpf) -> {
            if (P1_MOVE_LEFT.equals(name)) {
                p1MoveLeft = isPressed;
            } else if (P1_MOVE_RIGHT.equals(name)) {
                p1MoveRight = isPressed;
            } else if (P1_MOVE_UP.equals(name)) {
                p1MoveUp = isPressed;
            } else if (P1_MOVE_DOWN.equals(name)) {
                p1MoveDown = isPressed;
            } else if (P2_MOVE_LEFT.equals(name)) {
                p2MoveLeft = isPressed;
            } else if (P2_MOVE_RIGHT.equals(name)) {
                p2MoveRight = isPressed;
            } else if (P2_MOVE_UP.equals(name)) {
                p2MoveUp = isPressed;
            } else if (P2_MOVE_DOWN.equals(name)) {
                p2MoveDown = isPressed;
            } else if (TOGGLE_DEBUG.equals(name) && isPressed) {
                bulletAppState.setDebugEnabled(!bulletAppState.isDebugEnabled());
            } else if (RETURN_TO_MENU.equals(name) && isPressed) {
                returnToMainMenu();
            }
        };

        simpleApp.getInputManager().addListener(
                paddleInputListener,
                P1_MOVE_LEFT,
                P1_MOVE_RIGHT,
                P1_MOVE_UP,
                P1_MOVE_DOWN,
                P2_MOVE_LEFT,
                P2_MOVE_RIGHT,
                P2_MOVE_UP,
                P2_MOVE_DOWN,
                TOGGLE_DEBUG,
                RETURN_TO_MENU);
    }

    private void returnToMainMenu() {
        if (gameFinished) {
            return;
        }

        gameFinished = true;
        getStateManager().detach(this);
        getStateManager().attach(new MainMenuState());
    }

    private void setupCameras() {
        if (gameMode == GameMode.TWO_PLAYER) {
            simpleApp.getViewPort().setEnabled(false);

            float splitAspect = (simpleApp.getCamera().getWidth() * 0.5f) / simpleApp.getCamera().getHeight();

            Camera leftCamera = simpleApp.getCamera().clone();
            leftCamera.setFrustumPerspective(45f, splitAspect, 1f, 1000f);
            leftCamera.setViewPort(0f, 0.5f, 0f, 1f);
            leftCamera.setLocation(new Vector3f(0, 30f, 30f));
            leftCamera.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
            leftPlayerViewPort = simpleApp.getRenderManager().createMainView("LeftPlayerView", leftCamera);
            leftPlayerViewPort.setClearFlags(true, true, true);
            leftPlayerViewPort.attachScene(gameNode);

            Camera rightCamera = simpleApp.getCamera().clone();
            rightCamera.setFrustumPerspective(45f, splitAspect, 1f, 1000f);
            rightCamera.setViewPort(0.5f, 1f, 0f, 1f);
            rightCamera.setLocation(new Vector3f(0, 30f, -30f));
            rightCamera.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
            rightPlayerViewPort = simpleApp.getRenderManager().createMainView("RightPlayerView", rightCamera);
            rightPlayerViewPort.setClearFlags(true, true, true);
            rightPlayerViewPort.attachScene(gameNode);
        } else {
            simpleApp.getViewPort().setEnabled(true);
            simpleApp.getCamera().setLocation(new Vector3f(0, 30f, 30f));
            simpleApp.getCamera().lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
        }
    }

    private void setupScoreDisplay(AssetManager assetManager) {
        BitmapFont guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        scoreText = new BitmapText(guiFont);
        scoreText.setSize(guiFont.getCharSet().getRenderedSize() * 1.5f);
        scoreText.setLocalTranslation(20f, simpleApp.getCamera().getHeight() - 20f, 0f);
        simpleApp.getGuiNode().attachChild(scoreText);

        shotDebugText = new BitmapText(guiFont);
        shotDebugText.setSize(guiFont.getCharSet().getRenderedSize() * 1.1f);
        shotDebugText.setColor(ColorRGBA.Yellow);
        shotDebugText.setLocalTranslation(20f, simpleApp.getCamera().getHeight() - 48f, 0f);
        shotDebugText.setText("");
        simpleApp.getGuiNode().attachChild(shotDebugText);

        powerUpStatusText = new BitmapText(guiFont);
        powerUpStatusText.setSize(guiFont.getCharSet().getRenderedSize() * 0.95f);
        powerUpStatusText.setColor(ColorRGBA.White);
        powerUpStatusText.setLocalTranslation(20f, simpleApp.getCamera().getHeight() - 76f, 0f);
        powerUpStatusText.setText("Effets actifs : aucun");
        simpleApp.getGuiNode().attachChild(powerUpStatusText);
    }

    private void configureHumanPaddleCollisions() {
        playerOnePaddle.getPhysicsControl().setCollisionGroup(HUMAN_PADDLE_GROUP);
        playerOnePaddle.getPhysicsControl().setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_01);
    }

    private void configureAIPaddleCollisions() {
        aiPaddle.getPhysicsControl().setCollisionGroup(AI_PADDLE_GROUP);
        aiPaddle.getPhysicsControl().setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_01);
    }

    private void configurePlayerTwoPaddleCollisions() {
        playerTwoPaddle.getPhysicsControl().setCollisionGroup(PLAYER2_PADDLE_GROUP);
        playerTwoPaddle.getPhysicsControl().setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_01);
    }

    private void startExchange(Side serverSide) {
        currentServer = serverSide;
        lastTouchSide = Side.NONE;
        rallyInProgress = false;
        stuckTimerSeconds = 0f;
        playerOnePaddleVelocity = Vector3f.ZERO;
        playerTwoPaddleVelocity = Vector3f.ZERO;
        aiPaddleVelocity = Vector3f.ZERO;

        resetPaddlesToStartPositions();

        float serveX = 0f;
        // Respawn points are aligned with the center neutral-zone boundary on each
        // side.
        float serveZ = (serverSide == Side.PLAYER_ONE) ? CENTER_NEUTRAL_HALF_DEPTH : -CENTER_NEUTRAL_HALF_DEPTH;

        float halfWidth = table.getWidth() / 2f;
        float maxX = halfWidth - puck.getRadius();
        serveX = Math.max(-maxX, Math.min(maxX, serveX));

        Vector3f servePosition = computeSafeServePosition(new Vector3f(serveX, TABLE_PLANE_Y, serveZ), serverSide);
        puck.resetPosition(servePosition);
        lastServePosition = servePosition.clone();
        goalDetectionCooldownSeconds = GOAL_DETECTION_COOLDOWN_SECONDS;
        aiServeDelaySeconds = (gameMode == GameMode.SINGLE_PLAYER) ? AI_REACTION_DELAY_AFTER_SERVE_SECONDS : 0f;
    }

    private void resetPaddlesToStartPositions() {
        if (playerOnePaddle != null) {
            playerOnePaddle.setPosition(new Vector3f(0f, TABLE_PLANE_Y, PLAYER_ONE_START_Z));
        }

        if (gameMode == GameMode.TWO_PLAYER) {
            if (playerTwoPaddle != null) {
                playerTwoPaddle.setPosition(new Vector3f(0f, TABLE_PLANE_Y, PLAYER_TWO_START_Z));
            }
        } else if (aiPaddle != null) {
            aiPaddle.setPosition(new Vector3f(0f, TABLE_PLANE_Y, PLAYER_TWO_START_Z));
        }
    }

    private Vector3f computeSafeServePosition(Vector3f desiredServePosition, Side serverSide) {
        Vector3f safe = desiredServePosition.clone();
        Paddle serverPaddle = getPaddleForSide(serverSide);
        Paddle otherPaddle = getPaddleForSide(oppositeSide(serverSide));

        // Iterate a few times because resolving one overlap can create another.
        for (int i = 0; i < 4; i++) {
            safe = pushServeAwayFromPaddle(safe, serverPaddle, serverSide);
            safe = pushServeAwayFromPaddle(safe, otherPaddle, serverSide);
        }

        return clampServeToValidArea(safe, serverSide);
    }

    private Vector3f pushServeAwayFromPaddle(Vector3f servePosition, Paddle paddle, Side serverSide) {
        if (paddle == null) {
            return servePosition;
        }

        Vector3f paddlePos = paddle.getPosition();
        float minDistance = puck.getRadius() + paddle.getRadius() + SERVE_SAFE_PADDING;

        float dx = servePosition.x - paddlePos.x;
        float dz = servePosition.z - paddlePos.z;
        float distSq = dx * dx + dz * dz;
        if (distSq >= minDistance * minDistance) {
            return servePosition;
        }

        float dist = (float) Math.sqrt(distSq);
        float pushDistance = (minDistance - dist) + 0.02f;

        Vector3f pushDir;
        if (dist > 0.0001f) {
            pushDir = new Vector3f(dx / dist, 0f, dz / dist);
        } else {
            // If perfectly overlapping, nudge toward table center from serving side.
            float centerDirection = (serverSide == Side.PLAYER_ONE) ? -1f : 1f;
            pushDir = new Vector3f(0f, 0f, centerDirection);
        }

        Vector3f adjusted = servePosition.add(pushDir.mult(pushDistance));
        return clampServeToValidArea(adjusted, serverSide);
    }

    private Vector3f clampServeToValidArea(Vector3f position, Side serverSide) {
        float halfWidth = table.getWidth() / 2f;
        float halfLength = table.getLength() / 2f;
        float maxX = halfWidth - puck.getRadius();

        float minZ;
        float maxZ;
        if (serverSide == Side.PLAYER_ONE) {
            minZ = CENTER_NEUTRAL_HALF_DEPTH;
            maxZ = halfLength - puck.getRadius();
        } else {
            minZ = -halfLength + puck.getRadius();
            maxZ = -CENTER_NEUTRAL_HALF_DEPTH;
        }

        float clampedX = Math.max(-maxX, Math.min(maxX, position.x));
        float clampedZ = Math.max(minZ, Math.min(maxZ, position.z));
        return new Vector3f(clampedX, TABLE_PLANE_Y, clampedZ);
    }

    private void updateScoreText() {
        scoreText.setText(ScoreRules.formatScore(playerOne, playerTwo));
    }

    private void checkGoals() {
        if (goalDetectionCooldownSeconds > 0f) {
            return;
        }

        Vector3f puckPosition = puck.getPosition();

        // Ignore goal checks while the puck is still around the fresh serve point.
        if (puckPosition.distanceSquared(lastServePosition) < SERVE_ARM_DISTANCE * SERVE_ARM_DISTANCE) {
            return;
        }

        float halfLength = table.getLength() / 2f;
        float goalHalfWidth = table.getGoalWidth() / 2f;

        if (Math.abs(puckPosition.x) > goalHalfWidth) {
            return;
        }

        if (puckPosition.z > halfLength + GOAL_MARGIN) {
            handleGoal(Side.PLAYER_ONE);
        } else if (puckPosition.z < -halfLength - GOAL_MARGIN) {
            handleGoal(Side.PLAYER_TWO);
        }
    }

    private void handleGoal(Side concedingSide) {
        Player concedingPlayer = getPlayerForSide(concedingSide);
        Side scoringSide = oppositeSide(concedingSide);
        Player scoringPlayer = getPlayerForSide(scoringSide);

        scoringPlayer.addPoint();

        if (lastTouchSide == concedingSide) {
            concedingPlayer.removePoint();
        }

        updateScoreText();
        handleScoreAfterGoal(scoringPlayer, concedingSide);
    }

    private void handleScoreAfterGoal(Player scorer, Side concedingSide) {
        if (ScoreRules.hasWinner(scorer)) {
            endGame(scorer);
        } else {
            startExchange(concedingSide);
        }
    }

    private void endGame(Player winner) {
        gameFinished = true;
        startExchange(currentServer);

        String summary = winner.getName() + " gagne !  "
                + playerOne.getName() + " : " + playerOne.getScore() + " | "
                + playerTwo.getName() + " : " + playerTwo.getScore();

        getStateManager().detach(this);
        getStateManager().attach(new EndScreenState(summary));
    }

    @Override
    protected void cleanup(Application app) {
        if (scoreText != null) {
            scoreText.removeFromParent();
        }
        if (shotDebugText != null) {
            shotDebugText.removeFromParent();
        }
        if (powerUpStatusText != null) {
            powerUpStatusText.removeFromParent();
        }

        if (gameNode != null) {
            gameNode.removeFromParent();
        }

        if (leftPlayerViewPort != null) {
            leftPlayerViewPort.setEnabled(false);
            leftPlayerViewPort.clearScenes();
        }
        if (rightPlayerViewPort != null) {
            rightPlayerViewPort.setEnabled(false);
            rightPlayerViewPort.clearScenes();
        }
        simpleApp.getViewPort().setEnabled(true);

        clearActivePowerUp();
        clearAllPowerUpEffects();

        if (bulletAppState != null && getStateManager().hasState(bulletAppState)) {
            bulletAppState.getPhysicsSpace().removeCollisionListener(this);
            getStateManager().detach(bulletAppState);
        }

        if (paddleInputListener != null) {
            simpleApp.getInputManager().removeListener(paddleInputListener);
        }
        if (simpleApp.getInputManager().hasMapping(P1_MOVE_LEFT)) {
            simpleApp.getInputManager().deleteMapping(P1_MOVE_LEFT);
        }
        if (simpleApp.getInputManager().hasMapping(P1_MOVE_RIGHT)) {
            simpleApp.getInputManager().deleteMapping(P1_MOVE_RIGHT);
        }
        if (simpleApp.getInputManager().hasMapping(P1_MOVE_UP)) {
            simpleApp.getInputManager().deleteMapping(P1_MOVE_UP);
        }
        if (simpleApp.getInputManager().hasMapping(P1_MOVE_DOWN)) {
            simpleApp.getInputManager().deleteMapping(P1_MOVE_DOWN);
        }
        if (simpleApp.getInputManager().hasMapping(P2_MOVE_LEFT)) {
            simpleApp.getInputManager().deleteMapping(P2_MOVE_LEFT);
        }
        if (simpleApp.getInputManager().hasMapping(P2_MOVE_RIGHT)) {
            simpleApp.getInputManager().deleteMapping(P2_MOVE_RIGHT);
        }
        if (simpleApp.getInputManager().hasMapping(P2_MOVE_UP)) {
            simpleApp.getInputManager().deleteMapping(P2_MOVE_UP);
        }
        if (simpleApp.getInputManager().hasMapping(P2_MOVE_DOWN)) {
            simpleApp.getInputManager().deleteMapping(P2_MOVE_DOWN);
        }
        if (simpleApp.getInputManager().hasMapping(TOGGLE_DEBUG)) {
            simpleApp.getInputManager().deleteMapping(TOGGLE_DEBUG);
        }
        if (simpleApp.getInputManager().hasMapping(RETURN_TO_MENU)) {
            simpleApp.getInputManager().deleteMapping(RETURN_TO_MENU);
        }
    }

    @Override
    protected void onEnable() {
        /* Optional enable logic */
    }

    @Override
    protected void onDisable() {
        /* Optional disable logic */
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (gameFinished) {
            return;
        }

        if (shotDebugTimerSeconds > 0f) {
            shotDebugTimerSeconds -= tpf;
            if (shotDebugTimerSeconds <= 0f && shotDebugText != null) {
                shotDebugText.setText("");
            }
        }

        if (goalDetectionCooldownSeconds > 0f) {
            goalDetectionCooldownSeconds = Math.max(0f, goalDetectionCooldownSeconds - tpf);
        }

        if (aiServeDelaySeconds > 0f) {
            aiServeDelaySeconds = Math.max(0f, aiServeDelaySeconds - tpf);
        }

        updatePlayerOnePaddlePosition(tpf);
        if (gameMode == GameMode.TWO_PLAYER) {
            updatePlayerTwoPaddlePosition(tpf);
        } else {
            if (aiServeDelaySeconds <= 0f) {
                updateAIPaddlePosition(tpf);
            } else {
                aiPaddleVelocity = Vector3f.ZERO;
            }
        }
        playerOnePaddle.constrainToTablePlane(TABLE_PLANE_Y);
        if (playerTwoPaddle != null) {
            playerTwoPaddle.constrainToTablePlane(TABLE_PLANE_Y);
        }
        if (aiPaddle != null) {
            aiPaddle.constrainToTablePlane(TABLE_PLANE_Y);
        }
        puck.constrainToTablePlane(TABLE_PLANE_Y);
        updatePowerUpAnimations(tpf);
        updateRallyStateAndStuckDetection(tpf);
        updatePowerUpSystem(tpf);
        updatePowerUpStatusDisplay();
        checkGoals();
    }

    private void updatePowerUpAnimations(float tpf) {
        puck.updateSpeedFire(tpf);
        puck.updateSizePowerUpAnimation(tpf);

        if (playerOnePaddle != null) {
            playerOnePaddle.updateSizePowerUpAnimation(tpf);
        }
        if (playerTwoPaddle != null) {
            playerTwoPaddle.updateSizePowerUpAnimation(tpf);
        }
        if (aiPaddle != null) {
            aiPaddle.updateSizePowerUpAnimation(tpf);
        }
    }

    private void updatePowerUpSystem(float tpf) {
        if (activePowerUp == null) {
            powerUpSpawnTimerSeconds -= tpf;
            if (powerUpSpawnTimerSeconds <= 0f) {
                spawnRandomPowerUp();
            }
        } else {
            tryCollectActivePowerUp();
        }

        updateTimedPowerUpEffects(tpf);
    }

    private void scheduleNextPowerUpSpawn() {
        float delta = POWER_UP_SPAWN_MAX_SECONDS - POWER_UP_SPAWN_MIN_SECONDS;
        powerUpSpawnTimerSeconds = POWER_UP_SPAWN_MIN_SECONDS + random.nextFloat() * delta;
    }

    private void spawnRandomPowerUp() {
        PowerUp.Type[] availableTypes = PowerUp.Type.values();
        PowerUp.Type selectedType = availableTypes[random.nextInt(availableTypes.length)];
        Vector3f spawnPosition = findPowerUpSpawnPosition();
        activePowerUp = new PowerUp(selectedType, spawnPosition, simpleApp.getAssetManager());
        activePowerUp.attachTo(gameNode);
    }

    private Vector3f findPowerUpSpawnPosition() {
        float halfWidth = table.getWidth() / 2f;
        float halfLength = table.getLength() / 2f;
        float minX = -(halfWidth - POWER_UP_EDGE_PADDING);
        float maxX = halfWidth - POWER_UP_EDGE_PADDING;
        float minZ = -(halfLength - POWER_UP_GOAL_PADDING);
        float maxZ = halfLength - POWER_UP_GOAL_PADDING;

        for (int i = 0; i < 12; i++) {
            float x = minX + random.nextFloat() * (maxX - minX);
            float z = minZ + random.nextFloat() * (maxZ - minZ);
            Vector3f candidate = new Vector3f(x, TABLE_PLANE_Y, z);
            if (isPowerUpSpawnSafe(candidate)) {
                return candidate;
            }
        }

        return new Vector3f(0f, TABLE_PLANE_Y, 0f);
    }

    private boolean isPowerUpSpawnSafe(Vector3f candidate) {
        float minPuckDistanceSq = POWER_UP_SAFE_DISTANCE_FROM_PUCK * POWER_UP_SAFE_DISTANCE_FROM_PUCK;
        if (candidate.distanceSquared(puck.getPosition()) < minPuckDistanceSq) {
            return false;
        }

        float minPaddleDistanceSq = POWER_UP_SAFE_DISTANCE_FROM_PADDLE * POWER_UP_SAFE_DISTANCE_FROM_PADDLE;
        Paddle sideOne = getPaddleForSide(Side.PLAYER_ONE);
        if (sideOne != null && candidate.distanceSquared(sideOne.getPosition()) < minPaddleDistanceSq) {
            return false;
        }

        Paddle sideTwo = getPaddleForSide(Side.PLAYER_TWO);
        return sideTwo == null || candidate.distanceSquared(sideTwo.getPosition()) >= minPaddleDistanceSq;
    }

    private void tryCollectActivePowerUp() {
        if (activePowerUp == null) {
            return;
        }

        float triggerDistance = puck.getRadius() + activePowerUp.getRadius();
        float triggerDistanceSq = triggerDistance * triggerDistance;
        if (puck.getPosition().distanceSquared(activePowerUp.getPosition()) > triggerDistanceSq) {
            return;
        }

        Side collectingSide = resolveCollectingSide();
        applyPowerUpEffect(activePowerUp.getType(), collectingSide);
        showShotDebug("PowerUp: " + activePowerUp.getType().getLabel());

        clearActivePowerUp();
        scheduleNextPowerUpSpawn();
    }

    private Side resolveCollectingSide() {
        if (lastTouchSide != Side.NONE) {
            return lastTouchSide;
        }

        Vector3f puckVelocity = puck.getVelocity();
        if (Math.abs(puckVelocity.z) > 0.2f) {
            return (puckVelocity.z < 0f) ? Side.PLAYER_ONE : Side.PLAYER_TWO;
        }

        return currentServer;
    }

    private void applyPowerUpEffect(PowerUp.Type type, Side collectingSide) {
        switch (type) {
            case SPEED_PLUS -> applyPuckSpeedBoost();
            case SIZE_PLUS -> applyPuckSizeScale(POWER_UP_PUCK_SIZE_PLUS_SCALE);
            case SIZE_MINUS -> applyPuckSizeScale(POWER_UP_PUCK_SIZE_MINUS_SCALE);
            case SHOT_ON_GOAL -> applyShotOnGoal(collectingSide);
            case PADDLE_PLUS -> applyPaddleSizeScale(collectingSide, POWER_UP_PADDLE_SIZE_PLUS_SCALE);
            case PADDLE_MINUS -> applyPaddleSizeScale(oppositeSide(collectingSide), POWER_UP_PADDLE_SIZE_MINUS_SCALE);
            default -> {
                // No default effect.
            }
        }
    }

    private void applyPuckSpeedBoost() {
        Vector3f velocity = puck.getVelocity();
        float speed = velocity.length();

        if (speed > 0.05f) {
            float boostedSpeed = speed * POWER_UP_SPEED_MULTIPLIER;
            puck.getPhysicsControl().setLinearVelocity(velocity.normalize().mult(boostedSpeed));
            puckSpeedBoostMinSpeed = boostedSpeed;
        } else {
            puckSpeedBoostMinSpeed = 0f;
        }

        puckSpeedBoostTimerSeconds = POWER_UP_PUCK_EFFECT_DURATION_SECONDS;
        puck.setSpeedFireEnabled(true);
    }

    private void applyPuckSizeScale(float scale) {
        puck.setRadiusScaleWithMarioAnimation(scale);
        puckSizeScale = scale;
        puckSizeTimerSeconds = POWER_UP_PUCK_EFFECT_DURATION_SECONDS;
    }

    private void applyShotOnGoal(Side collectingSide) {
        Side targetSide = oppositeSide(collectingSide);
        float targetZ = (targetSide == Side.PLAYER_ONE) ? table.getLength() / 2f + 1f : -(table.getLength() / 2f + 1f);
        Vector3f target = new Vector3f(0f, TABLE_PLANE_Y, targetZ);

        Vector3f direction = target.subtract(puck.getPosition());
        direction.y = 0f;
        if (direction.lengthSquared() < 0.0001f) {
            return;
        }

        direction.normalizeLocal();
        float currentSpeed = puck.getVelocity().length();
        float launchSpeed = Math.max(SHOT_ON_GOAL_MIN_SPEED, currentSpeed);
        puck.getPhysicsControl().setLinearVelocity(direction.mult(launchSpeed));

        if (puckSpeedBoostTimerSeconds > 0f) {
            puckSpeedBoostMinSpeed = Math.max(puckSpeedBoostMinSpeed, launchSpeed);
        }
    }

    private void applyPaddleSizeScale(Side side, float scale) {
        Paddle paddle = getPaddleForSide(side);
        if (paddle == null) {
            return;
        }

        paddle.setRadiusScaleWithMarioAnimation(scale);
        if (side == Side.PLAYER_ONE) {
            playerOnePaddleSizeScale = scale;
            playerOnePaddleSizeTimerSeconds = POWER_UP_PADDLE_EFFECT_DURATION_SECONDS;
        } else if (side == Side.PLAYER_TWO) {
            playerTwoPaddleSizeScale = scale;
            playerTwoPaddleSizeTimerSeconds = POWER_UP_PADDLE_EFFECT_DURATION_SECONDS;
        }
    }

    private void updateTimedPowerUpEffects(float tpf) {
        updatePuckSpeedBoostEffect(tpf);
        updatePuckSizeEffect(tpf);
        updatePlayerOnePaddleSizeEffect(tpf);
        updatePlayerTwoPaddleSizeEffect(tpf);
    }

    private void updatePuckSpeedBoostEffect(float tpf) {
        if (puckSpeedBoostTimerSeconds <= 0f) {
            return;
        }

        puckSpeedBoostTimerSeconds = Math.max(0f, puckSpeedBoostTimerSeconds - tpf);
        sustainPuckSpeedBoost();
        if (puckSpeedBoostTimerSeconds <= 0f) {
            puckSpeedBoostMinSpeed = 0f;
            puck.setSpeedFireEnabled(false);
        }
    }

    private void updatePuckSizeEffect(float tpf) {
        if (puckSizeTimerSeconds <= 0f) {
            return;
        }

        puckSizeTimerSeconds = Math.max(0f, puckSizeTimerSeconds - tpf);
        if (puckSizeTimerSeconds <= 0f) {
            puckSizeScale = 1f;
            puck.resetRadiusScale();
        }
    }

    private void updatePowerUpStatusDisplay() {
        if (powerUpStatusText == null) {
            return;
        }

        StringBuilder status = new StringBuilder("Effets actifs :");
        boolean hasActive = false;

        hasActive = appendTimedEffect(status, hasActive, "Speed +", puckSpeedBoostTimerSeconds);

        if (puckSizeTimerSeconds > 0f) {
            String puckSizeLabel = (puckSizeScale >= 1f) ? "Size +" : "Size -";
            hasActive = appendTimedEffect(status, hasActive, puckSizeLabel, puckSizeTimerSeconds);
        }

        if (playerOnePaddleSizeTimerSeconds > 0f) {
            String p1Label = (playerOnePaddleSizeScale >= 1f) ? "Paddle + J1" : "Paddle - J1";
            hasActive = appendTimedEffect(status, hasActive, p1Label, playerOnePaddleSizeTimerSeconds);
        }

        if (playerTwoPaddleSizeTimerSeconds > 0f) {
            String sideTwoLabel = (gameMode == GameMode.TWO_PLAYER) ? "J2" : "IA";
            String p2Label = (playerTwoPaddleSizeScale >= 1f)
                    ? "Paddle + " + sideTwoLabel
                    : "Paddle - " + sideTwoLabel;
            hasActive = appendTimedEffect(status, hasActive, p2Label, playerTwoPaddleSizeTimerSeconds);
        }

        if (!hasActive) {
            status.append(" aucun");
        }

        powerUpStatusText.setText(status.toString());
    }

    private boolean appendTimedEffect(StringBuilder status, boolean hasActive, String effectLabel,
            float remainingSeconds) {
        if (remainingSeconds <= 0f) {
            return hasActive;
        }

        status.append('\n')
                .append("- ")
                .append(effectLabel)
                .append(" : ")
                .append(formatRemainingSeconds(remainingSeconds));
        return true;
    }

    private String formatRemainingSeconds(float seconds) {
        float safeSeconds = Math.max(0f, seconds);
        return String.format(Locale.ROOT, "%.1fs", safeSeconds);
    }

    private void updatePlayerOnePaddleSizeEffect(float tpf) {
        if (playerOnePaddleSizeTimerSeconds <= 0f) {
            return;
        }

        playerOnePaddleSizeTimerSeconds = Math.max(0f, playerOnePaddleSizeTimerSeconds - tpf);
        if (playerOnePaddleSizeTimerSeconds > 0f || Math.abs(playerOnePaddleSizeScale - 1f) <= 0.0001f) {
            return;
        }

        playerOnePaddleSizeScale = 1f;
        if (playerOnePaddle != null) {
            playerOnePaddle.resetRadiusScale();
        }
    }

    private void updatePlayerTwoPaddleSizeEffect(float tpf) {
        if (playerTwoPaddleSizeTimerSeconds <= 0f) {
            return;
        }

        playerTwoPaddleSizeTimerSeconds = Math.max(0f, playerTwoPaddleSizeTimerSeconds - tpf);
        if (playerTwoPaddleSizeTimerSeconds > 0f || Math.abs(playerTwoPaddleSizeScale - 1f) <= 0.0001f) {
            return;
        }

        playerTwoPaddleSizeScale = 1f;
        Paddle sideTwo = getPaddleForSide(Side.PLAYER_TWO);
        if (sideTwo != null) {
            sideTwo.resetRadiusScale();
        }
    }

    private void sustainPuckSpeedBoost() {
        if (puckSpeedBoostMinSpeed <= 0f) {
            return;
        }

        Vector3f velocity = puck.getVelocity();
        float speed = velocity.length();
        if (speed > 0.05f && speed < puckSpeedBoostMinSpeed) {
            puck.getPhysicsControl().setLinearVelocity(velocity.normalize().mult(puckSpeedBoostMinSpeed));
        }
    }

    private void clearActivePowerUp() {
        if (activePowerUp != null) {
            activePowerUp.removeFromParent();
            activePowerUp = null;
        }
    }

    private void clearAllPowerUpEffects() {
        puckSpeedBoostTimerSeconds = 0f;
        puckSpeedBoostMinSpeed = 0f;
        puckSizeTimerSeconds = 0f;
        puckSizeScale = 1f;
        playerOnePaddleSizeTimerSeconds = 0f;
        playerTwoPaddleSizeTimerSeconds = 0f;
        playerOnePaddleSizeScale = 1f;
        playerTwoPaddleSizeScale = 1f;

        if (puck != null) {
            puck.setSpeedFireEnabled(false);
            puck.resetRadiusScale();
        }
        if (playerOnePaddle != null) {
            playerOnePaddle.resetRadiusScale();
        }

        Paddle sideTwo = getPaddleForSide(Side.PLAYER_TWO);
        if (sideTwo != null) {
            sideTwo.resetRadiusScale();
        }
    }

    private void updateRallyStateAndStuckDetection(float tpf) {
        float speed = puck.getVelocity().length();
        if (!rallyInProgress && speed > STUCK_SPEED_THRESHOLD) {
            rallyInProgress = true;
        }

        if (!rallyInProgress) {
            return;
        }

        if (speed < STUCK_SPEED_THRESHOLD) {
            stuckTimerSeconds += tpf;
        } else {
            stuckTimerSeconds = 0f;
        }

        if (stuckTimerSeconds >= STUCK_TIME_THRESHOLD_SECONDS) {
            Side nextServer;
            if (lastTouchSide == Side.NONE) {
                nextServer = oppositeSide(currentServer);
            } else {
                nextServer = oppositeSide(lastTouchSide);
            }
            startExchange(nextServer);
        }
    }

    private void updatePlayerOnePaddlePosition(float tpf) {
        float safeTpf = Math.max(tpf, 0.0001f);
        float deltaX = 0f;
        float deltaZ = 0f;

        if (p1MoveLeft) {
            deltaX -= PADDLE_SPEED * tpf;
        }
        if (p1MoveRight) {
            deltaX += PADDLE_SPEED * tpf;
        }
        if (p1MoveUp) {
            deltaZ -= PADDLE_SPEED * tpf;
        }
        if (p1MoveDown) {
            deltaZ += PADDLE_SPEED * tpf;
        }

        Vector3f currentPos = playerOnePaddle.getPosition();
        float halfWidth = table.getWidth() / 2f;
        float halfLength = table.getLength() / 2f;
        float radius = playerOnePaddle.getRadius();
        float centerLimit = CENTER_NEUTRAL_HALF_DEPTH + radius;

        float tableX = Math.max(-(halfWidth - radius), Math.min(halfWidth - radius, currentPos.x + deltaX));
        float tableZ = Math.max(centerLimit, Math.min(halfLength - radius, currentPos.z + deltaZ));

        Vector3f newPosition = new Vector3f(tableX, currentPos.y, tableZ);
        playerOnePaddleVelocity = newPosition.subtract(currentPos).mult(1f / safeTpf);
        playerOnePaddle.setPosition(newPosition);
    }

    private void updatePlayerTwoPaddlePosition(float tpf) {
        if (playerTwoPaddle == null) {
            return;
        }

        float safeTpf = Math.max(tpf, 0.0001f);

        float deltaX = 0f;
        float deltaZ = 0f;

        if (p2MoveLeft) {
            deltaX += PADDLE_SPEED * tpf;
        }
        if (p2MoveRight) {
            deltaX -= PADDLE_SPEED * tpf;
        }
        // Camera is reversed for player two, so up/down are inverted in world Z.
        if (p2MoveUp) {
            deltaZ += PADDLE_SPEED * tpf;
        }
        if (p2MoveDown) {
            deltaZ -= PADDLE_SPEED * tpf;
        }

        Vector3f currentPos = playerTwoPaddle.getPosition();
        float halfWidth = table.getWidth() / 2f;
        float halfLength = table.getLength() / 2f;
        float radius = playerTwoPaddle.getRadius();
        float centerLimit = CENTER_NEUTRAL_HALF_DEPTH + radius;

        float tableX = Math.max(-(halfWidth - radius), Math.min(halfWidth - radius, currentPos.x + deltaX));
        float tableZ = Math.max(-(halfLength - radius), Math.min(-centerLimit, currentPos.z + deltaZ));

        Vector3f newPosition = new Vector3f(tableX, currentPos.y, tableZ);
        playerTwoPaddleVelocity = newPosition.subtract(currentPos).mult(1f / safeTpf);
        playerTwoPaddle.setPosition(newPosition);
    }

    private void updateAIPaddlePosition(float tpf) {
        if (aiPaddle == null) {
            return;
        }

        float safeTpf = Math.max(tpf, 0.0001f);

        Vector3f puckPos = puck.getPosition();
        Vector3f puckVel = puck.getVelocity();
        Vector3f aiPos = aiPaddle.getPosition();
        float halfLength = table.getLength() / 2f;
        Vector3f targetGoal = new Vector3f(0f, TABLE_PLANE_Y, halfLength + 1f);

        float targetX;
        float targetZ;
        if (puckPos.z < AI_INTERCEPT_LINE_Z && puckVel.z < -0.15f) {
            float timeToIntercept = (AI_INTERCEPT_LINE_Z - puckPos.z) / puckVel.z;
            float predictedX = puckPos.x + puckVel.x * Math.max(0f, timeToIntercept);
            targetX = predictedX;
            targetZ = AI_INTERCEPT_LINE_Z;
        } else if (puckPos.z < -0.5f) {
            Vector3f strikeDir = targetGoal.subtract(puckPos);
            strikeDir.y = 0f;
            if (strikeDir.lengthSquared() > 0.0001f) {
                strikeDir.normalizeLocal();
            } else {
                strikeDir.set(0f, 0f, 1f);
            }

            Vector3f strikePoint = puckPos.subtract(strikeDir.mult(AI_ATTACK_OFFSET));
            targetX = strikePoint.x;
            targetZ = strikePoint.z;
        } else {
            targetX = puckPos.x * 0.4f;
            targetZ = AI_DEFENSIVE_LINE_Z;
        }

        float maxStep = AI_PADDLE_SPEED * tpf;
        float dx = targetX - aiPos.x;
        float dz = targetZ - aiPos.z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);

        float nextX = aiPos.x;
        float nextZ = aiPos.z;
        if (distance > 0.0001f) {
            float ratio = Math.min(1f, maxStep / distance);
            nextX += dx * ratio;
            nextZ += dz * ratio;
        }

        float halfWidth = table.getWidth() / 2f;
        float radius = aiPaddle.getRadius();
        float centerLimit = CENTER_NEUTRAL_HALF_DEPTH + radius;

        nextX = Math.max(-(halfWidth - radius), Math.min(halfWidth - radius, nextX));
        nextZ = Math.max(-(halfLength - radius), Math.min(-centerLimit, nextZ));

        Vector3f newPosition = new Vector3f(nextX, TABLE_PLANE_Y, nextZ);
        aiPaddleVelocity = newPosition.subtract(aiPos).mult(1f / safeTpf);
        aiPaddle.setPosition(newPosition);
    }

    private void applyShotPhysics(Side strikerSide, Vector3f paddleVelocity) {
        if (paddleVelocity == null) {
            return;
        }

        float paddleSpeed = paddleVelocity.length();
        if (paddleSpeed < 0.05f) {
            return;
        }

        Vector3f goalDirection = (strikerSide == Side.PLAYER_ONE) ? new Vector3f(0f, 0f, -1f)
                : new Vector3f(0f, 0f, 1f);
        Vector3f paddleDir = paddleVelocity.normalize();
        float onTarget = paddleDir.dot(goalDirection);
        float lateral = (float) Math.sqrt(Math.max(0f, 1f - onTarget * onTarget));

        Vector3f puckVelocity = puck.getVelocity();
        float puckSpeed = puckVelocity.length();
        String detectedShot = null;

        // Smash: hard, well-targeted hit gives +5% to +15% speed.
        if (paddleSpeed >= SMASH_MIN_SPEED && onTarget > 0.7f) {
            float boost = 1.05f + (float) Math.random() * 0.10f;
            Vector3f boosted = puckVelocity.mult(boost);
            if (boosted.length() < paddleSpeed * 0.5f) {
                boosted = paddleDir.mult(paddleSpeed * 0.5f);
            }
            puck.getPhysicsControl().setLinearVelocity(boosted);
            detectedShot = "SMASH";
        }

        // Lift: angled strike imparts spin around vertical axis.
        if (paddleSpeed >= LIFT_MIN_SPEED && lateral > 0.35f) {
            Vector3f spinAxis = paddleDir.cross(Vector3f.UNIT_Y);
            if (spinAxis.lengthSquared() > 0.0001f) {
                spinAxis.normalizeLocal();
            } else {
                spinAxis.set(1f, 0f, 0f);
            }

            float spinAmount = Math.min(MAX_SPIN_Y, lateral * paddleSpeed * 1.2f);
            Vector3f currentSpin = puck.getPhysicsControl().getAngularVelocity();
            puck.getPhysicsControl().setAngularVelocity(currentSpin.add(spinAxis.mult(spinAmount)));
            detectedShot = (detectedShot == null) ? "LIFT" : detectedShot + " + LIFT";
        }

        // Flip: poorly oriented / soft hit slows the puck down.
        if (paddleSpeed <= FLIP_MAX_SPEED && onTarget < 0.25f && puckSpeed > 0.1f) {
            float slowdown = 0.78f + (float) Math.random() * 0.12f;
            puck.getPhysicsControl().setLinearVelocity(puckVelocity.mult(slowdown));
            detectedShot = "FLIP";
        }

        if (detectedShot != null) {
            showShotDebug(detectedShot);
        }
    }

    private void showShotDebug(String shotLabel) {
        if (shotDebugText == null) {
            return;
        }
        shotDebugText.setText("Shot: " + shotLabel);
        shotDebugTimerSeconds = SHOT_DEBUG_DISPLAY_SECONDS;
    }

    private Player getPlayerForSide(Side side) {
        if (side == Side.PLAYER_ONE) {
            return playerOne;
        }
        if (side == Side.PLAYER_TWO) {
            return playerTwo;
        }
        return null;
    }

    private Paddle getPaddleForSide(Side side) {
        if (side == Side.PLAYER_ONE) {
            return playerOnePaddle;
        }
        if (side == Side.PLAYER_TWO) {
            return (gameMode == GameMode.TWO_PLAYER) ? playerTwoPaddle : aiPaddle;
        }
        return null;
    }

    private Side oppositeSide(Side side) {
        if (side == Side.PLAYER_ONE) {
            return Side.PLAYER_TWO;
        }
        if (side == Side.PLAYER_TWO) {
            return Side.PLAYER_ONE;
        }
        return Side.NONE;
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        if (puck == null) {
            return;
        }

        PhysicsCollisionObject puckControl = puck.getPhysicsControl();
        PhysicsCollisionObject objectA = event.getObjectA();
        PhysicsCollisionObject objectB = event.getObjectB();

        boolean puckInvolved = (objectA == puckControl) || (objectB == puckControl);
        if (!puckInvolved) {
            return;
        }

        PhysicsCollisionObject other = (objectA == puckControl) ? objectB : objectA;
        if (other == null) {
            return;
        }

        if (playerOnePaddle != null && other == playerOnePaddle.getPhysicsControl()) {
            lastTouchSide = Side.PLAYER_ONE;
            rallyInProgress = true;
            stuckTimerSeconds = 0f;
            applyShotPhysics(Side.PLAYER_ONE, playerOnePaddleVelocity);
            return;
        }

        Paddle sideTwoPaddle = (gameMode == GameMode.TWO_PLAYER) ? playerTwoPaddle : aiPaddle;
        if (sideTwoPaddle != null && other == sideTwoPaddle.getPhysicsControl()) {
            lastTouchSide = Side.PLAYER_TWO;
            rallyInProgress = true;
            stuckTimerSeconds = 0f;
            Vector3f sideTwoVelocity = (gameMode == GameMode.TWO_PLAYER) ? playerTwoPaddleVelocity : aiPaddleVelocity;
            applyShotPhysics(Side.PLAYER_TWO, sideTwoVelocity);
        }
    }

}
