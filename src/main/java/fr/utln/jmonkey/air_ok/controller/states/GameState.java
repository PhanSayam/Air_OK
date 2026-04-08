package fr.utln.jmonkey.air_ok.controller.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
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

import fr.utln.jmonkey.air_ok.model.Paddle;
import fr.utln.jmonkey.air_ok.model.Player;
import fr.utln.jmonkey.air_ok.model.Puck;
import fr.utln.jmonkey.air_ok.model.Table;
import fr.utln.jmonkey.air_ok.model.rules.ScoreRules;

public class GameState extends BaseAppState {

    public enum GameMode {
        SINGLE_PLAYER,
        TWO_PLAYER
    }

    private static final float GOAL_MARGIN = 0.6f;
    private static final float PADDLE_SPEED = 14f;
    private static final float AI_PADDLE_SPEED = 11.5f;
    private static final float TABLE_PLANE_Y = Puck.HALF_HEIGHT;
    private static final float AI_DEFENSIVE_LINE_Z = -10.5f;
    private static final float AI_INTERCEPT_LINE_Z = -7.2f;
    private static final float AI_ATTACK_OFFSET = 1.35f;

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

        gameNode = new Node("GameNode");
        simpleApp.getRootNode().attachChild(gameNode);

        setupCameras();

        table = new Table(simpleApp.getAssetManager(), gameNode, bulletAppState);
        table.initTable();

        puck = new Puck(simpleApp.getAssetManager(), gameNode, bulletAppState);
        puck.initPuck();

        playerOnePaddle = new Paddle(simpleApp.getAssetManager(), gameNode, bulletAppState,
                new Vector3f(0f, TABLE_PLANE_Y, 12f), ColorRGBA.Cyan);
        playerOnePaddle.initPaddle();
        configureHumanPaddleCollisions();

        if (gameMode == GameMode.SINGLE_PLAYER) {
            aiPaddle = new Paddle(simpleApp.getAssetManager(), gameNode, bulletAppState,
                    new Vector3f(0f, TABLE_PLANE_Y, -12f), ColorRGBA.Orange);
            aiPaddle.initPaddle();
            configureAIPaddleCollisions();
        } else {
            playerTwoPaddle = new Paddle(simpleApp.getAssetManager(), gameNode, bulletAppState,
                    new Vector3f(0f, TABLE_PLANE_Y, -12f), ColorRGBA.Green);
            playerTwoPaddle.initPaddle();
            configurePlayerTwoPaddleCollisions();
        }

        playerOne = new Player("Joueur 1");
        playerTwo = new Player("Joueur 2");

        setupScoreDisplay(simpleApp.getAssetManager());
        setupPaddleInput();
        updateScoreText();
        servePuck();
    }

    private void setupPaddleInput() {
        // Support AZERTY (ZQSD) and QWERTY (WASD) layouts.
        simpleApp.getInputManager().addMapping(P1_MOVE_LEFT, new KeyTrigger(KeyInput.KEY_Q), new KeyTrigger(KeyInput.KEY_A));
        simpleApp.getInputManager().addMapping(P1_MOVE_RIGHT, new KeyTrigger(KeyInput.KEY_D));
        simpleApp.getInputManager().addMapping(P1_MOVE_UP, new KeyTrigger(KeyInput.KEY_Z), new KeyTrigger(KeyInput.KEY_W));
        simpleApp.getInputManager().addMapping(P1_MOVE_DOWN, new KeyTrigger(KeyInput.KEY_S));

        simpleApp.getInputManager().addMapping(P2_MOVE_LEFT, new KeyTrigger(KeyInput.KEY_LEFT));
        simpleApp.getInputManager().addMapping(P2_MOVE_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT));
        simpleApp.getInputManager().addMapping(P2_MOVE_UP, new KeyTrigger(KeyInput.KEY_UP));
        simpleApp.getInputManager().addMapping(P2_MOVE_DOWN, new KeyTrigger(KeyInput.KEY_DOWN));

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
                P2_MOVE_DOWN);
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

    private void servePuck() {
        puck.resetPosition(new Vector3f(0f, TABLE_PLANE_Y, 0f));
    }

    private void updateScoreText() {
        scoreText.setText(ScoreRules.formatScore(playerOne, playerTwo));
    }

    private void checkGoals() {
        Vector3f puckPosition = puck.getPosition();
        float halfLength = table.getLength() / 2f;
        float goalHalfWidth = table.getGoalWidth() / 2f;

        if (Math.abs(puckPosition.x) > goalHalfWidth) {
            return;
        }

        if (puckPosition.z > halfLength + GOAL_MARGIN) {
            playerTwo.addPoint();
            updateScoreText();
            handleScoreAfterGoal(playerTwo);
        } else if (puckPosition.z < -halfLength - GOAL_MARGIN) {
            playerOne.addPoint();
            updateScoreText();
            handleScoreAfterGoal(playerOne);
        }
    }

    private void handleScoreAfterGoal(Player scorer) {
        if (ScoreRules.hasWinner(scorer)) {
            endGame(scorer);
        } else {
            servePuck();
        }
    }

    private void endGame(Player winner) {
        gameFinished = true;
        servePuck();

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

        if (bulletAppState != null && getStateManager().hasState(bulletAppState)) {
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

        updatePlayerOnePaddlePosition(tpf);
        if (gameMode == GameMode.TWO_PLAYER) {
            updatePlayerTwoPaddlePosition(tpf);
        } else {
            updateAIPaddlePosition(tpf);
        }
        playerOnePaddle.constrainToTablePlane(TABLE_PLANE_Y);
        if (playerTwoPaddle != null) {
            playerTwoPaddle.constrainToTablePlane(TABLE_PLANE_Y);
        }
        if (aiPaddle != null) {
            aiPaddle.constrainToTablePlane(TABLE_PLANE_Y);
        }
        puck.constrainToTablePlane(TABLE_PLANE_Y);
        checkGoals();
    }

    private void updatePlayerOnePaddlePosition(float tpf) {
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

        float tableX = Math.max(-(halfWidth - radius), Math.min(halfWidth - radius, currentPos.x + deltaX));
        float tableZ = Math.max(radius, Math.min(halfLength - radius, currentPos.z + deltaZ));

        Vector3f newPosition = new Vector3f(tableX, currentPos.y, tableZ);
        playerOnePaddle.setPosition(newPosition);
    }

    private void updatePlayerTwoPaddlePosition(float tpf) {
        if (playerTwoPaddle == null) {
            return;
        }

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

        float tableX = Math.max(-(halfWidth - radius), Math.min(halfWidth - radius, currentPos.x + deltaX));
        float tableZ = Math.max(-(halfLength - radius), Math.min(-radius, currentPos.z + deltaZ));

        playerTwoPaddle.setPosition(new Vector3f(tableX, currentPos.y, tableZ));
    }

    private void updateAIPaddlePosition(float tpf) {
        if (aiPaddle == null) {
            return;
        }

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

        nextX = Math.max(-(halfWidth - radius), Math.min(halfWidth - radius, nextX));
        nextZ = Math.max(-(halfLength - radius), Math.min(-radius, nextZ));

        aiPaddle.setPosition(new Vector3f(nextX, TABLE_PLANE_Y, nextZ));
    }

}
