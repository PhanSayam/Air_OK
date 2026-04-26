package fr.utln.jmonkey.air_ok.controller.physics;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import fr.utln.jmonkey.air_ok.controller.states.GameState;
import fr.utln.jmonkey.air_ok.model.Paddle;
import fr.utln.jmonkey.air_ok.model.Puck;
import fr.utln.jmonkey.air_ok.model.Table;

public class PaddleController {

    // --------------- Constants -----------------------------------------------

    private static final float LEGACY_TABLE_WIDTH = 20f;
    private static final float LEGACY_TABLE_LENGTH = 30f;
    private static final float PADDLE_SPEED = 14f;
    private static final float AI_PADDLE_SPEED = 11.5f;
    private static final float AI_DEFENSIVE_LINE_Z = -10.5f;
    private static final float AI_INTERCEPT_LINE_Z = -7.2f;
    private static final float AI_ATTACK_OFFSET = 1.35f;
    private static final float SMASH_MIN_SPEED = 8.5f;
    private static final float LIFT_MIN_SPEED = 4.5f;
    private static final float FLIP_MAX_SPEED = 5.5f;
    private static final float MAX_SPIN_Y = 22f;
    private static final float SHOT_DEBUG_DISPLAY_SECONDS = 1.2f;

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

    // --------------- Fields --------------------------------------------------

    private final SimpleApplication app;
    private final BulletAppState bulletAppState;
    private final GameState.GameMode gameMode;
    private final Puck puck;
    private final Paddle playerOnePaddle;
    private final Paddle playerTwoPaddle;
    private final Paddle aiPaddle;
    private final Table table;
    private final float tablePlaneY;
    private final float centerNeutralHalfDepth;

    private float aiSpeedMultiplier = 1.0f;

    private boolean p1MoveLeft;
    private boolean p1MoveRight;
    private boolean p1MoveUp;
    private boolean p1MoveDown;

    private boolean p2MoveLeft;
    private boolean p2MoveRight;
    private boolean p2MoveUp;
    private boolean p2MoveDown;

    private ActionListener paddleInputListener;

    private Vector3f playerOnePaddleVelocity = Vector3f.ZERO;
    private Vector3f playerTwoPaddleVelocity = Vector3f.ZERO;
    private Vector3f aiPaddleVelocity = Vector3f.ZERO;

    private float aiServeDelaySeconds;

    private BitmapText shotDebugText;
    private float shotDebugTimerSeconds;

    // --------------- Constructor ---------------------------------------------

    public PaddleController(SimpleApplication app, BulletAppState bulletAppState,
            GameState.GameMode gameMode, Puck puck,
            Paddle playerOnePaddle, Paddle playerTwoPaddle, Paddle aiPaddle,
            Table table, float tablePlaneY) {
        this.app = app;
        this.bulletAppState = bulletAppState;
        this.gameMode = gameMode;
        this.puck = puck;
        this.playerOnePaddle = playerOnePaddle;
        this.playerTwoPaddle = playerTwoPaddle;
        this.aiPaddle = aiPaddle;
        this.table = table;
        this.tablePlaneY = tablePlaneY;
        this.centerNeutralHalfDepth = (table != null) ? table.getCenterNeutralHalfDepth() : 2.2f;
    }

    // --------------- Public setup API ----------------------------------------

    public void setAiSpeedMultiplier(float multiplier) {
        this.aiSpeedMultiplier = Math.max(0.1f, multiplier);
    }

    public void configureCollisionGroups() {
        configureHumanPaddleCollisions();
        if (gameMode == GameState.GameMode.SINGLE_PLAYER) {
            configureAIPaddleCollisions();
        } else {
            configurePlayerTwoPaddleCollisions();
        }
    }

    public void setupInput(InputManager inputManager) {
        inputManager.addMapping(P1_MOVE_LEFT, new KeyTrigger(KeyInput.KEY_Q),
                new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(P1_MOVE_RIGHT, new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(P1_MOVE_UP, new KeyTrigger(KeyInput.KEY_Z),
                new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(P1_MOVE_DOWN, new KeyTrigger(KeyInput.KEY_S));

        inputManager.addMapping(P2_MOVE_LEFT, new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping(P2_MOVE_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping(P2_MOVE_UP, new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping(P2_MOVE_DOWN, new KeyTrigger(KeyInput.KEY_DOWN));

        paddleInputListener = (name, isPressed, tpf) -> {
            switch (name) {
                case P1_MOVE_LEFT -> p1MoveLeft = isPressed;
                case P1_MOVE_RIGHT -> p1MoveRight = isPressed;
                case P1_MOVE_UP -> p1MoveUp = isPressed;
                case P1_MOVE_DOWN -> p1MoveDown = isPressed;
                case P2_MOVE_LEFT -> p2MoveLeft = isPressed;
                case P2_MOVE_RIGHT -> p2MoveRight = isPressed;
                case P2_MOVE_UP -> p2MoveUp = isPressed;
                case P2_MOVE_DOWN -> p2MoveDown = isPressed;
            }
        };

        inputManager.addListener(paddleInputListener,
                P1_MOVE_LEFT, P1_MOVE_RIGHT, P1_MOVE_UP, P1_MOVE_DOWN,
                P2_MOVE_LEFT, P2_MOVE_RIGHT, P2_MOVE_UP, P2_MOVE_DOWN);
    }

    public void attachShotDebugText(Node guiNode, AssetManager am, float screenHeight) {
        BitmapFont guiFont = am.loadFont("Interface/Fonts/Default.fnt");
        shotDebugText = new BitmapText(guiFont);
        shotDebugText.setSize(guiFont.getCharSet().getRenderedSize() * 1.1f);
        shotDebugText.setColor(ColorRGBA.Yellow);
        shotDebugText.setLocalTranslation(20f, screenHeight - 48f, 0f);
        shotDebugText.setText("");
        guiNode.attachChild(shotDebugText);
    }

    public BitmapText getShotDebugText() {
        return shotDebugText;
    }

    public void setAiServeDelay(float seconds) {
        this.aiServeDelaySeconds = seconds;
    }

    public void resetVelocities() {
        playerOnePaddleVelocity = Vector3f.ZERO;
        playerTwoPaddleVelocity = Vector3f.ZERO;
        aiPaddleVelocity = Vector3f.ZERO;
    }

    // --------------- Per-frame update ----------------------------------------

    public void update(float tpf) {
        if (shotDebugTimerSeconds > 0f) {
            shotDebugTimerSeconds -= tpf;
            if (shotDebugTimerSeconds <= 0f && shotDebugText != null) {
                shotDebugText.setText("");
            }
        }

        if (aiServeDelaySeconds > 0f) {
            aiServeDelaySeconds = Math.max(0f, aiServeDelaySeconds - tpf);
        }

        updatePlayerOnePaddlePosition(tpf);

        if (gameMode == GameState.GameMode.TWO_PLAYER) {
            updatePlayerTwoPaddlePosition(tpf);
        } else {
            if (aiServeDelaySeconds <= 0f) {
                updateAIPaddlePosition(tpf);
            } else {
                aiPaddleVelocity = Vector3f.ZERO;
            }
        }
    }

    // --------------- Velocity accessors --------------------------------------

    public Vector3f getPlayerOnePaddleVelocity() {
        return playerOnePaddleVelocity;
    }

    public Vector3f getPlayerTwoPaddleVelocity() {
        return playerTwoPaddleVelocity;
    }

    public Vector3f getAiPaddleVelocity() {
        return aiPaddleVelocity;
    }

    // --------------- Shot physics --------------------------------------------

    public void applyShotPhysics(GameState.Side strikerSide, Vector3f paddleVelocity) {
        if (paddleVelocity == null) {
            return;
        }
        float paddleSpeed = paddleVelocity.length();
        if (paddleSpeed < 0.05f) {
            return;
        }

        Vector3f goalDirection = (strikerSide == GameState.Side.PLAYER_ONE)
                ? new Vector3f(0f, 0f, -1f)
                : new Vector3f(0f, 0f, 1f);
        Vector3f paddleDir = paddleVelocity.normalize();
        float onTarget = paddleDir.dot(goalDirection);
        float lateral = (float) Math.sqrt(Math.max(0f, 1f - onTarget * onTarget));

        Vector3f puckVelocity = puck.getVelocity();
        float puckSpeed = puckVelocity.length();
        String detectedShot = null;

        if (paddleSpeed >= SMASH_MIN_SPEED && onTarget > 0.7f) {
            float boost = 1.05f + (float) Math.random() * 0.10f;
            Vector3f boosted = puckVelocity.mult(boost);
            if (boosted.length() < paddleSpeed * 0.5f) {
                boosted = paddleDir.mult(paddleSpeed * 0.5f);
            }
            puck.getPhysicsControl().setLinearVelocity(boosted);
            detectedShot = "SMASH";
        }

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

        if (paddleSpeed <= FLIP_MAX_SPEED && onTarget < 0.25f && puckSpeed > 0.1f) {
            float slowdown = 0.78f + (float) Math.random() * 0.12f;
            puck.getPhysicsControl().setLinearVelocity(puckVelocity.mult(slowdown));
            detectedShot = "FLIP";
        }

        if (detectedShot != null) {
            showShotDebug(detectedShot);
        }
    }

    public void showShotDebug(String shotLabel) {
        if (shotDebugText == null) {
            return;
        }
        shotDebugText.setText("Shot: " + shotLabel);
        shotDebugTimerSeconds = SHOT_DEBUG_DISPLAY_SECONDS;
    }

    // --------------- Input cleanup -------------------------------------------

    public void cleanupInput(InputManager inputManager) {
        if (paddleInputListener != null) {
            inputManager.removeListener(paddleInputListener);
        }
        deleteIfPresent(inputManager, P1_MOVE_LEFT);
        deleteIfPresent(inputManager, P1_MOVE_RIGHT);
        deleteIfPresent(inputManager, P1_MOVE_UP);
        deleteIfPresent(inputManager, P1_MOVE_DOWN);
        deleteIfPresent(inputManager, P2_MOVE_LEFT);
        deleteIfPresent(inputManager, P2_MOVE_RIGHT);
        deleteIfPresent(inputManager, P2_MOVE_UP);
        deleteIfPresent(inputManager, P2_MOVE_DOWN);
    }

    private void deleteIfPresent(InputManager inputManager, String mapping) {
        if (inputManager.hasMapping(mapping)) {
            inputManager.deleteMapping(mapping);
        }
    }

    // --------------- Paddle movement -----------------------------------------

    private void updatePlayerOnePaddlePosition(float tpf) {
        float safeTpf = Math.max(tpf, 0.0001f);
        float paddleSpeed = scaleLength(PADDLE_SPEED);
        float deltaX = 0f;
        float deltaZ = 0f;

        if (p1MoveLeft)  deltaX -= paddleSpeed * tpf;
        if (p1MoveRight) deltaX += paddleSpeed * tpf;
        if (p1MoveUp)    deltaZ -= paddleSpeed * tpf;
        if (p1MoveDown)  deltaZ += paddleSpeed * tpf;

        Vector3f currentPos = playerOnePaddle.getPosition();
        float halfWidth = table.getWidth() / 2f;
        float halfLength = table.getLength() / 2f;
        float radius = playerOnePaddle.getRadius();
        float centerLimit = centerNeutralHalfDepth + radius;

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
        float paddleSpeed = scaleLength(PADDLE_SPEED);

        boolean left  = p2MoveLeft;
        boolean right = p2MoveRight;
        boolean up    = p2MoveUp;
        boolean down  = p2MoveDown;

        float deltaX = 0f;
        float deltaZ = 0f;

        if (left)  deltaX += paddleSpeed * tpf;
        if (right) deltaX -= paddleSpeed * tpf;
        // Camera is reversed for player two, so up/down are inverted in world Z.
        if (up)   deltaZ += paddleSpeed * tpf;
        if (down) deltaZ -= paddleSpeed * tpf;

        Vector3f currentPos = playerTwoPaddle.getPosition();
        float halfWidth = table.getWidth() / 2f;
        float halfLength = table.getLength() / 2f;
        float radius = playerTwoPaddle.getRadius();
        float centerLimit = centerNeutralHalfDepth + radius;

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
        Vector3f targetGoal = new Vector3f(0f, tablePlaneY, halfLength + scaleLength(1f));
        float aiInterceptLineZ = scaleLength(AI_INTERCEPT_LINE_Z);
        float aiDefensiveLineZ = scaleLength(AI_DEFENSIVE_LINE_Z);

        float targetX;
        float targetZ;
        if (puckPos.z < aiInterceptLineZ && puckVel.z < -0.15f) {
            float timeToIntercept = (aiInterceptLineZ - puckPos.z) / puckVel.z;
            float predictedX = puckPos.x + puckVel.x * Math.max(0f, timeToIntercept);
            targetX = predictedX;
            targetZ = aiInterceptLineZ;
        } else if (puckPos.z < -0.5f) {
            Vector3f strikeDir = targetGoal.subtract(puckPos);
            strikeDir.y = 0f;
            if (strikeDir.lengthSquared() > 0.0001f) {
                strikeDir.normalizeLocal();
            } else {
                strikeDir.set(0f, 0f, 1f);
            }
            Vector3f strikePoint = puckPos.subtract(strikeDir.mult(scaleLength(AI_ATTACK_OFFSET)));
            targetX = strikePoint.x;
            targetZ = strikePoint.z;
        } else {
            targetX = puckPos.x * 0.4f;
            targetZ = aiDefensiveLineZ;
        }

        float effectiveSpeed = AI_PADDLE_SPEED * aiSpeedMultiplier;
        float maxStep = scaleLength(effectiveSpeed) * tpf;
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
        float centerLimit = centerNeutralHalfDepth + radius;

        nextX = Math.max(-(halfWidth - radius), Math.min(halfWidth - radius, nextX));
        nextZ = Math.max(-(halfLength - radius), Math.min(-centerLimit, nextZ));

        Vector3f newPosition = new Vector3f(nextX, tablePlaneY, nextZ);
        aiPaddleVelocity = newPosition.subtract(aiPos).mult(1f / safeTpf);
        aiPaddle.setPosition(newPosition);
    }

    // --------------- Collision group configuration ---------------------------

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

    // --------------- Scale helpers -------------------------------------------

    private float scaleLength(float legacyValue) {
        if (table == null) {
            return legacyValue;
        }
        return legacyValue * (table.getLength() / LEGACY_TABLE_LENGTH);
    }
}
