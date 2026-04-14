package fr.utln.jmonkey.air_ok.controller.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

import java.util.Random;

import fr.utln.jmonkey.air_ok.controller.physics.PaddleController;
import fr.utln.jmonkey.air_ok.controller.powerup.PowerUpManager;
import fr.utln.jmonkey.air_ok.model.Paddle;
import fr.utln.jmonkey.air_ok.model.Player;
import fr.utln.jmonkey.air_ok.model.Puck;
import fr.utln.jmonkey.air_ok.model.Table;
import fr.utln.jmonkey.air_ok.model.rules.ScoreRules;

public class GameState extends BaseAppState implements PhysicsCollisionListener {

    public enum GameMode {
        SINGLE_PLAYER,
        TWO_PLAYER
    }

    public enum Side {
        PLAYER_ONE,
        PLAYER_TWO,
        NONE
    }

    private static final float GOAL_MARGIN = 60f;
    private static final float TABLE_PLANE_Y = Puck.HALF_HEIGHT;
    private static final float CENTER_NEUTRAL_HALF_DEPTH = 220f;
    private static final float PLAYER_ONE_START_Z = 1200f;
    private static final float PLAYER_TWO_START_Z = -1200f;
    private static final float SERVE_SAFE_PADDING = 25f;
    private static final float GOAL_DETECTION_COOLDOWN_SECONDS = 0.75f;
    private static final float SERVE_ARM_DISTANCE = 120f;
    private static final float STUCK_SPEED_THRESHOLD = 0.18f;
    private static final float STUCK_TIME_THRESHOLD_SECONDS = 2.2f;
    private static final float AI_REACTION_DELAY_AFTER_SERVE_SECONDS = 0.45f;

    private static final String TOGGLE_DEBUG = "ToggleDebug";
    private static final String RETURN_TO_MENU = "ReturnToMenu";

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

    private final GameMode gameMode;
    private Side lastTouchSide = Side.NONE;
    private Side currentServer = Side.PLAYER_ONE;
    private float stuckTimerSeconds;
    private boolean rallyInProgress;
    private float goalDetectionCooldownSeconds;
    private Vector3f lastServePosition = Vector3f.ZERO;
    private boolean gameFinished;
    private final Random random = new Random();

    private PaddleController paddleController;
    private PowerUpManager powerUpManager;

    private ActionListener appInputListener;

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

        playerOnePaddle = new Paddle(
                simpleApp.getAssetManager(),
                gameNode,
                bulletAppState,
                new Vector3f(0f, TABLE_PLANE_Y, getPlayerStartZ(Side.PLAYER_ONE)),
                ColorRGBA.Red);
        playerOnePaddle.initPaddle();

        if (gameMode == GameMode.SINGLE_PLAYER) {
            aiPaddle = new Paddle(
                    simpleApp.getAssetManager(),
                    gameNode,
                    bulletAppState,
                    new Vector3f(0f, TABLE_PLANE_Y, getPlayerStartZ(Side.PLAYER_TWO)),
                    ColorRGBA.Blue);
            aiPaddle.initPaddle();
        } else {
            playerTwoPaddle = new Paddle(
                    simpleApp.getAssetManager(),
                    gameNode,
                    bulletAppState,
                    new Vector3f(0f, TABLE_PLANE_Y, getPlayerStartZ(Side.PLAYER_TWO)),
                    ColorRGBA.Blue);
            playerTwoPaddle.initPaddle();
        }

        paddleController = new PaddleController(
                simpleApp,
                bulletAppState,
                gameMode,
                puck,
                playerOnePaddle,
                playerTwoPaddle,
                aiPaddle,
                table,
                TABLE_PLANE_Y);
        paddleController.configureCollisionGroups();
        paddleController.setupInput(simpleApp.getInputManager());

        Paddle sideTwo = (gameMode == GameMode.TWO_PLAYER) ? playerTwoPaddle : aiPaddle;
        powerUpManager = new PowerUpManager(
                simpleApp.getAssetManager(),
                gameNode,
                bulletAppState,
                puck,
                table,
                random,
                gameMode,
                TABLE_PLANE_Y);
        powerUpManager.setPaddles(playerOnePaddle, sideTwo);

        playerOne = new Player("Joueur 1");
        playerTwo = new Player("Joueur 2");
        setupScoreDisplay(simpleApp.getAssetManager());

        paddleController.attachShotDebugText(
                simpleApp.getGuiNode(),
                simpleApp.getAssetManager(),
                simpleApp.getCamera().getHeight());

        powerUpManager.attachToGui(
                simpleApp.getGuiNode(),
                simpleApp.getAssetManager(),
                simpleApp.getCamera().getHeight());

        setupAppInput();

        updateScoreText();
        startExchange(Side.PLAYER_ONE);
        powerUpManager.scheduleNextPowerUpSpawn();
    }

    private void setupAppInput() {
        simpleApp.getInputManager().addMapping(TOGGLE_DEBUG, new KeyTrigger(KeyInput.KEY_H));
        simpleApp.getInputManager().addMapping(RETURN_TO_MENU, new KeyTrigger(KeyInput.KEY_ESCAPE));

        appInputListener = (name, isPressed, tpf) -> {
            if (TOGGLE_DEBUG.equals(name) && isPressed) {
                bulletAppState.setDebugEnabled(!bulletAppState.isDebugEnabled());
            } else if (RETURN_TO_MENU.equals(name) && isPressed) {
                returnToMainMenu();
            }
        };

        simpleApp.getInputManager().addListener(appInputListener, TOGGLE_DEBUG, RETURN_TO_MENU);
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
            leftCamera.setFrustumPerspective(45f, splitAspect, 1f, 5000f);
            leftCamera.setViewPort(0f, 0.5f, 0f, 1f);
            leftCamera.setLocation(new Vector3f(0f, 2000f, 2000f));
            leftCamera.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);
            leftPlayerViewPort = simpleApp.getRenderManager().createMainView("LeftPlayerView", leftCamera);
            leftPlayerViewPort.setClearFlags(true, true, true);
            leftPlayerViewPort.setBackgroundColor(new ColorRGBA(0.12f, 0.16f, 0.22f, 1f));
            leftPlayerViewPort.attachScene(gameNode);

            Camera rightCamera = simpleApp.getCamera().clone();
            rightCamera.setFrustumPerspective(45f, splitAspect, 1f, 5000f);
            rightCamera.setViewPort(0.5f, 1f, 0f, 1f);
            rightCamera.setLocation(new Vector3f(0f, 2000f, -2000f));
            rightCamera.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);
            rightPlayerViewPort = simpleApp.getRenderManager().createMainView("RightPlayerView", rightCamera);
            rightPlayerViewPort.setClearFlags(true, true, true);
            rightPlayerViewPort.setBackgroundColor(new ColorRGBA(0.12f, 0.16f, 0.22f, 1f));
            rightPlayerViewPort.attachScene(gameNode);
        } else {
            simpleApp.getViewPort().setEnabled(true);
            simpleApp.getViewPort().setBackgroundColor(new ColorRGBA(0.12f, 0.16f, 0.22f, 1f));
            simpleApp.getCamera().setFrustumPerspective(
                    45f,
                    (float) simpleApp.getCamera().getWidth() / simpleApp.getCamera().getHeight(),
                    1f,
                    5000f);
            simpleApp.getCamera().setLocation(new Vector3f(0f, 2000f, 2000f));
            simpleApp.getCamera().lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);
        }
    }

    private void setupScoreDisplay(AssetManager assetManager) {
        BitmapFont guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        scoreText = new BitmapText(guiFont);
        scoreText.setSize(guiFont.getCharSet().getRenderedSize() * 1.5f);
        scoreText.setLocalTranslation(20f, simpleApp.getCamera().getHeight() - 20f, 0f);
        simpleApp.getGuiNode().attachChild(scoreText);
    }

    private void updateScoreText() {
        scoreText.setText(ScoreRules.formatScore(playerOne, playerTwo));
    }

    private void startExchange(Side serverSide) {
        currentServer = serverSide;
        lastTouchSide = Side.NONE;
        rallyInProgress = false;
        stuckTimerSeconds = 0f;

        if (paddleController != null) {
            paddleController.resetVelocities();
            paddleController.setAiServeDelay(
                    (gameMode == GameMode.SINGLE_PLAYER) ? AI_REACTION_DELAY_AFTER_SERVE_SECONDS : 0f);
        }
        if (powerUpManager != null) {
            powerUpManager.setLastTouchSide(lastTouchSide);
            powerUpManager.setCurrentServer(currentServer);
        }

        resetPaddlesToStartPositions();

        float serveX = 0f;
        float centerNeutralHalfDepth = getCenterNeutralHalfDepth();
        float serveZ = (serverSide == Side.PLAYER_ONE) ? centerNeutralHalfDepth : -centerNeutralHalfDepth;

        float halfWidth = table.getWidth() / 2f;
        float maxX = halfWidth - puck.getRadius();
        serveX = Math.max(-maxX, Math.min(maxX, serveX));

        Vector3f servePosition = computeSafeServePosition(
                new Vector3f(serveX, TABLE_PLANE_Y, serveZ), serverSide);
        puck.resetPosition(servePosition);
        lastServePosition = servePosition.clone();
        goalDetectionCooldownSeconds = GOAL_DETECTION_COOLDOWN_SECONDS;
    }

    private void resetPaddlesToStartPositions() {
        if (playerOnePaddle != null) {
            playerOnePaddle.setPosition(new Vector3f(0f, TABLE_PLANE_Y, getPlayerStartZ(Side.PLAYER_ONE)));
        }
        if (gameMode == GameMode.TWO_PLAYER) {
            if (playerTwoPaddle != null) {
                playerTwoPaddle.setPosition(new Vector3f(0f, TABLE_PLANE_Y, getPlayerStartZ(Side.PLAYER_TWO)));
            }
        } else if (aiPaddle != null) {
            aiPaddle.setPosition(new Vector3f(0f, TABLE_PLANE_Y, getPlayerStartZ(Side.PLAYER_TWO)));
        }
    }

    private Vector3f computeSafeServePosition(Vector3f desiredServePosition, Side serverSide) {
        Vector3f safe = desiredServePosition.clone();
        Paddle serverPaddle = getPaddleForSide(serverSide);
        Paddle otherPaddle = getPaddleForSide(oppositeSide(serverSide));

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
        float minDistance = puck.getRadius() + paddle.getRadius() + getServeSafePadding();

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
        float centerNeutralHalfDepth = getCenterNeutralHalfDepth();

        float minZ;
        float maxZ;
        if (serverSide == Side.PLAYER_ONE) {
            minZ = centerNeutralHalfDepth;
            maxZ = halfLength - puck.getRadius();
        } else {
            minZ = -halfLength + puck.getRadius();
            maxZ = -centerNeutralHalfDepth;
        }

        float clampedX = Math.max(-maxX, Math.min(maxX, position.x));
        float clampedZ = Math.max(minZ, Math.min(maxZ, position.z));
        return new Vector3f(clampedX, TABLE_PLANE_Y, clampedZ);
    }

    private void checkGoals() {
        if (goalDetectionCooldownSeconds > 0f) {
            return;
        }

        Vector3f puckPosition = puck.getPosition();
        float serveArmDistance = getServeArmDistance();

        if (puckPosition.distanceSquared(lastServePosition) < serveArmDistance * serveArmDistance) {
            return;
        }

        float halfLength = table.getLength() / 2f;
        float goalHalfWidth = table.getGoalWidth() / 2f;

        if (Math.abs(puckPosition.x) > goalHalfWidth) {
            return;
        }

        float goalMargin = getGoalMargin();
        if (puckPosition.z > halfLength + goalMargin) {
            handleGoal(Side.PLAYER_ONE);
        } else if (puckPosition.z < -halfLength - goalMargin) {
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
            Side nextServer = (lastTouchSide == Side.NONE)
                    ? oppositeSide(currentServer)
                    : oppositeSide(lastTouchSide);
            startExchange(nextServer);
        }
    }

    @Override
    protected void cleanup(Application app) {
        if (scoreText != null) {
            scoreText.removeFromParent();
        }
        if (paddleController != null && paddleController.getShotDebugText() != null) {
            paddleController.getShotDebugText().removeFromParent();
        }
        if (powerUpManager != null && powerUpManager.getStatusText() != null) {
            powerUpManager.getStatusText().removeFromParent();
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

        if (powerUpManager != null) {
            powerUpManager.clearActivePowerUp();
            powerUpManager.clearAllPowerUpEffects();
        }

        if (bulletAppState != null && getStateManager().hasState(bulletAppState)) {
            bulletAppState.getPhysicsSpace().removeCollisionListener(this);
            getStateManager().detach(bulletAppState);
        }

        if (paddleController != null) {
            paddleController.cleanupInput(simpleApp.getInputManager());
        }

        if (appInputListener != null) {
            simpleApp.getInputManager().removeListener(appInputListener);
        }

        deleteIfPresent(TOGGLE_DEBUG);
        deleteIfPresent(RETURN_TO_MENU);
    }

    private void deleteIfPresent(String mapping) {
        if (simpleApp.getInputManager().hasMapping(mapping)) {
            simpleApp.getInputManager().deleteMapping(mapping);
        }
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (gameFinished) {
            return;
        }

        if (goalDetectionCooldownSeconds > 0f) {
            goalDetectionCooldownSeconds = Math.max(0f, goalDetectionCooldownSeconds - tpf);
        }

        paddleController.update(tpf);

        playerOnePaddle.constrainToTablePlane(TABLE_PLANE_Y);
        if (playerTwoPaddle != null) {
            playerTwoPaddle.constrainToTablePlane(TABLE_PLANE_Y);
        }
        if (aiPaddle != null) {
            aiPaddle.constrainToTablePlane(TABLE_PLANE_Y);
        }
        puck.constrainToTablePlane(TABLE_PLANE_Y);

        powerUpManager.updatePowerUpAnimations(tpf);
        updateRallyStateAndStuckDetection(tpf);
        powerUpManager.update(tpf);
        checkGoals();
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

            if (powerUpManager != null) {
                powerUpManager.setLastTouchSide(lastTouchSide);
            }

            paddleController.applyShotPhysics(
                    Side.PLAYER_ONE,
                    paddleController.getPlayerOnePaddleVelocity());
            return;
        }

        Paddle sideTwoPaddle = (gameMode == GameMode.TWO_PLAYER) ? playerTwoPaddle : aiPaddle;
        if (sideTwoPaddle != null && other == sideTwoPaddle.getPhysicsControl()) {
            lastTouchSide = Side.PLAYER_TWO;
            rallyInProgress = true;
            stuckTimerSeconds = 0f;

            if (powerUpManager != null) {
                powerUpManager.setLastTouchSide(lastTouchSide);
            }

            Vector3f sideTwoVelocity = (gameMode == GameMode.TWO_PLAYER)
                    ? paddleController.getPlayerTwoPaddleVelocity()
                    : paddleController.getAiPaddleVelocity();

            paddleController.applyShotPhysics(Side.PLAYER_TWO, sideTwoVelocity);
        }
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

    private float getPlayerStartZ(Side side) {
        return (side == Side.PLAYER_ONE) ? PLAYER_ONE_START_Z : PLAYER_TWO_START_Z;
    }

    private float getCenterNeutralHalfDepth() {
        if (table == null) {
            return CENTER_NEUTRAL_HALF_DEPTH;
        }
        return table.getCenterNeutralHalfDepth();
    }

    private float getGoalMargin() {
        return GOAL_MARGIN;
    }

    private float getServeSafePadding() {
        return SERVE_SAFE_PADDING;
    }

    private float getServeArmDistance() {
        return SERVE_ARM_DISTANCE;
    }
}