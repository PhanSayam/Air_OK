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
import com.jme3.shadow.DirectionalLightShadowRenderer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fr.utln.jmonkey.air_ok.controller.physics.PaddleController;
import fr.utln.jmonkey.air_ok.replay.ReplayFrame;
import fr.utln.jmonkey.air_ok.controller.powerup.PowerUpManager;
import fr.utln.jmonkey.air_ok.model.Paddle;
import fr.utln.jmonkey.air_ok.model.Player;
import fr.utln.jmonkey.air_ok.model.Puck;
import fr.utln.jmonkey.air_ok.model.Table;
import fr.utln.jmonkey.air_ok.model.TournamentManager;
import fr.utln.jmonkey.air_ok.model.ArcadeEnvironment;
import fr.utln.jmonkey.air_ok.model.rules.ScoreRules;

public class GameState extends BaseAppState implements PhysicsCollisionListener {

    // --------------- Inner types ---------------------------------------------

    public enum GameMode {
        SINGLE_PLAYER,
        TWO_PLAYER
    }

    public enum Side {
        PLAYER_ONE,
        PLAYER_TWO,
        NONE
    }

    /** All configuration for a match. Build and pass to the constructor. */
    public static class GameConfig {
        public final GameMode mode;
        public TournamentManager tournament = null;
        public float aiSpeedMultiplier = 1.0f;
        public float aiReactionDelay = 0.45f;
        public String playerOneName = "Joueur 1";
        public String opponentName = "Joueur 2";

        public GameConfig(GameMode mode) {
            this.mode = mode;
        }
    }

    // --------------- Constants -----------------------------------------------

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

    private static final String TOGGLE_DEBUG = "ToggleDebug";
    private static final String RETURN_TO_MENU = "ReturnToMenu";

    // --------------- Fields --------------------------------------------------

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
    private ArcadeEnvironment arcadeEnvironment;
    private Player playerOne;
    private Player playerTwo;
    private BitmapText scoreText;

    private final GameConfig config;
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

    // --------------- Replay buffer -------------------------------------------

    private static final int   REPLAY_MAX_FRAMES =
            (int) (KillCamState.REPLAY_BUFFER_SECONDS * 60f);
    /** Minimum gap (s) between two recorded bounces to avoid duplicate collision events. */
    private static final float BOUNCE_MIN_INTERVAL = 0.12f;
    /** How many past bounce timestamps to keep for trimming the kill-cam window. */
    private static final int   BOUNCE_HISTORY = 6;
    /** Kill-cam replays from this many bounces before the goal. */
    private static final int   KILLCAM_BOUNCE_COUNT = 6;

    private final ArrayDeque<ReplayFrame> replayBuffer      = new ArrayDeque<>();
    private final ArrayDeque<Float>       bounceTimestamps  = new ArrayDeque<>();
    private float replayRecordTimer = 0f;
    private float gameTime          = 0f;

    // --------------- Constructors (keep backward compat) ---------------------

    public GameState() {
        this(new GameConfig(GameMode.SINGLE_PLAYER));
    }

    public GameState(GameMode gameMode) {
        this(new GameConfig(gameMode));
    }

    public GameState(GameConfig config) {
        this.config = config;
    }

    // --------------- GameMode accessor used by legacy callers ----------------

    public GameMode getGameMode() {
        return config.mode;
    }

    // --------------- Initialize ----------------------------------------------

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
        arcadeEnvironment = new ArcadeEnvironment(simpleApp.getAssetManager(), gameNode);
        arcadeEnvironment.initEnvironment();
        setupShadows();

        puck = new Puck(simpleApp.getAssetManager(), gameNode, bulletAppState);
        puck.initPuck();

        playerOnePaddle = new Paddle(
                simpleApp.getAssetManager(),
                gameNode,
                bulletAppState,
                new Vector3f(0f, TABLE_PLANE_Y, getPlayerStartZ(Side.PLAYER_ONE)),
                ColorRGBA.Red);
        playerOnePaddle.initPaddle();

        if (config.mode == GameMode.SINGLE_PLAYER) {
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
                config.mode,
                puck,
                playerOnePaddle,
                playerTwoPaddle,
                aiPaddle,
                table,
                TABLE_PLANE_Y);
        paddleController.setAiSpeedMultiplier(config.aiSpeedMultiplier);
        paddleController.configureCollisionGroups();
        paddleController.setupInput(simpleApp.getInputManager());

        Paddle sideTwo = (config.mode == GameMode.TWO_PLAYER) ? playerTwoPaddle : aiPaddle;
        powerUpManager = new PowerUpManager(
                simpleApp.getAssetManager(),
                gameNode,
                bulletAppState,
                puck,
                table,
                random,
                config.mode,
                TABLE_PLANE_Y);
        powerUpManager.setPaddles(playerOnePaddle, sideTwo);

        playerOne = new Player(config.playerOneName);
        playerTwo = new Player(config.opponentName);
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
                boolean debug = !bulletAppState.isDebugEnabled();
                bulletAppState.setDebugEnabled(debug);
                if (powerUpManager != null) powerUpManager.setHitboxDebugEnabled(debug);
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

    private void setupShadows() {
        com.jme3.light.DirectionalLight keyLight = table.getShadowKeyLight();
        if (keyLight == null) return;

        if (config.mode == GameMode.TWO_PLAYER) {
            DirectionalLightShadowRenderer dlsr1 = new DirectionalLightShadowRenderer(
                    simpleApp.getAssetManager(), 2048, 3);
            dlsr1.setLight(keyLight);
            dlsr1.setShadowIntensity(0.55f);
            leftPlayerViewPort.addProcessor(dlsr1);

            DirectionalLightShadowRenderer dlsr2 = new DirectionalLightShadowRenderer(
                    simpleApp.getAssetManager(), 2048, 3);
            dlsr2.setLight(keyLight);
            dlsr2.setShadowIntensity(0.55f);
            rightPlayerViewPort.addProcessor(dlsr2);
        } else {
            DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(
                    simpleApp.getAssetManager(), 2048, 3);
            dlsr.setLight(keyLight);
            dlsr.setShadowIntensity(0.55f);
            simpleApp.getViewPort().addProcessor(dlsr);
        }
    }

    private void setupCameras() {
        if (config.mode == GameMode.TWO_PLAYER) {
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
        String text = ScoreRules.formatScore(playerOne, playerTwo);
        if (config.tournament != null) {
            text += "  [Tour " + (config.tournament.getCurrentRound() + 1)
                    + "/" + TournamentManager.TOTAL_ROUNDS + "]";
        }
        scoreText.setText(text);
    }

    // --------------- Game flow -----------------------------------------------

    private void startExchange(Side serverSide) {
        currentServer = serverSide;
        lastTouchSide = Side.NONE;
        rallyInProgress = false;
        stuckTimerSeconds = 0f;

        if (paddleController != null) {
            paddleController.resetVelocities();
            paddleController.setAiServeDelay(
                    (config.mode == GameMode.SINGLE_PLAYER) ? config.aiReactionDelay : 0f);
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
        if (config.mode == GameMode.TWO_PLAYER) {
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

        String summary = winner.getName() + " gagne !  "
                + playerOne.getName() + " : " + playerOne.getScore() + " | "
                + playerTwo.getName() + " : " + playerTwo.getScore();

        boolean playerOneWon = (winner == playerOne);

        com.jme3.app.state.AppState nextState;
        if (config.tournament != null) {
            if (playerOneWon) {
                config.tournament.advanceRound();
            }
            nextState = new TournamentInterstitialState(config.tournament, playerOneWon, summary);
        } else {
            nextState = new EndScreenState(summary);
        }

        Paddle p2 = (config.mode == GameMode.TWO_PLAYER) ? playerTwoPaddle : aiPaddle;

        // Trim replay buffer to the last 3 bounces only
        List<ReplayFrame> killCamFrames = buildKillCamFrames();

        // KillCamState will detach this GameState once the replay finishes
        getStateManager().attach(new KillCamState(
                killCamFrames,
                puck, playerOnePaddle, p2,
                bulletAppState,
                leftPlayerViewPort, rightPlayerViewPort,
                this, nextState));
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

    // --------------- Lifecycle -----------------------------------------------

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

        gameTime += tpf;
        recordReplayFrame(tpf);

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

        // Record every distinct bounce (paddle or wall) for kill-cam trimming
        if (!gameFinished) {
            float lastBounce = bounceTimestamps.isEmpty() ? -1f : bounceTimestamps.peekLast();
            if (gameTime - lastBounce >= BOUNCE_MIN_INTERVAL) {
                bounceTimestamps.addLast(gameTime);
                while (bounceTimestamps.size() > BOUNCE_HISTORY) {
                    bounceTimestamps.pollFirst();
                }
            }
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

        Paddle sideTwoPaddle = (config.mode == GameMode.TWO_PLAYER) ? playerTwoPaddle : aiPaddle;
        if (sideTwoPaddle != null && other == sideTwoPaddle.getPhysicsControl()) {
            lastTouchSide = Side.PLAYER_TWO;
            rallyInProgress = true;
            stuckTimerSeconds = 0f;

            if (powerUpManager != null) {
                powerUpManager.setLastTouchSide(lastTouchSide);
            }

            Vector3f sideTwoVelocity = (config.mode == GameMode.TWO_PLAYER)
                    ? paddleController.getPlayerTwoPaddleVelocity()
                    : paddleController.getAiPaddleVelocity();

            paddleController.applyShotPhysics(Side.PLAYER_TWO, sideTwoVelocity);
        }
    }

    /** Returns frames from the 3rd-to-last bounce onwards (or all frames if fewer bounces). */
    private List<ReplayFrame> buildKillCamFrames() {
        List<ReplayFrame> all = new ArrayList<>(replayBuffer);
        if (all.isEmpty()) return all;

        float startTime = 0f;
        if (bounceTimestamps.size() >= KILLCAM_BOUNCE_COUNT) {
            Float[] bounces = bounceTimestamps.toArray(new Float[0]);
            startTime = bounces[bounces.length - KILLCAM_BOUNCE_COUNT];
        }

        final float t = startTime;
        int startIdx = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).timestamp >= t) {
                startIdx = i;
                break;
            }
        }
        return new ArrayList<>(all.subList(startIdx, all.size()));
    }

    private void recordReplayFrame(float tpf) {
        replayRecordTimer += tpf;
        if (replayRecordTimer < 1f / 60f) return;
        replayRecordTimer -= 1f / 60f;

        Paddle p2 = (config.mode == GameMode.TWO_PLAYER) ? playerTwoPaddle : aiPaddle;
        Vector3f p2Pos = (p2 != null) ? p2.getPosition() : Vector3f.ZERO;
        replayBuffer.addLast(new ReplayFrame(
                gameTime, puck.getPosition(), playerOnePaddle.getPosition(), p2Pos));

        while (replayBuffer.size() > REPLAY_MAX_FRAMES) {
            replayBuffer.pollFirst();
        }
    }

    // --------------- Helpers -------------------------------------------------

    private Player getPlayerForSide(Side side) {
        if (side == Side.PLAYER_ONE) return playerOne;
        if (side == Side.PLAYER_TWO) return playerTwo;
        return null;
    }

    private Paddle getPaddleForSide(Side side) {
        if (side == Side.PLAYER_ONE) return playerOnePaddle;
        if (side == Side.PLAYER_TWO) {
            return (config.mode == GameMode.TWO_PLAYER) ? playerTwoPaddle : aiPaddle;
        }
        return null;
    }

    private Side oppositeSide(Side side) {
        if (side == Side.PLAYER_ONE) return Side.PLAYER_TWO;
        if (side == Side.PLAYER_TWO) return Side.PLAYER_ONE;
        return Side.NONE;
    }

    private float getPlayerStartZ(Side side) {
        return (side == Side.PLAYER_ONE) ? PLAYER_ONE_START_Z : PLAYER_TWO_START_Z;
    }

    private float getCenterNeutralHalfDepth() {
        if (table == null) return CENTER_NEUTRAL_HALF_DEPTH;
        return table.getCenterNeutralHalfDepth();
    }

    private float getGoalMargin()       { return GOAL_MARGIN; }
    private float getServeSafePadding() { return SERVE_SAFE_PADDING; }
    private float getServeArmDistance() { return SERVE_ARM_DISTANCE; }
}
