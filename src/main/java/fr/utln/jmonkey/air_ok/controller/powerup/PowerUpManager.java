package fr.utln.jmonkey.air_ok.controller.powerup;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import java.util.Locale;
import java.util.Random;

import fr.utln.jmonkey.air_ok.controller.states.GameState;
import fr.utln.jmonkey.air_ok.model.Paddle;
import fr.utln.jmonkey.air_ok.model.PowerUp;
import fr.utln.jmonkey.air_ok.model.Puck;
import fr.utln.jmonkey.air_ok.model.Table;

/**
 * Manages the power-up lifecycle: spawning, collection, effect timers, and
 * the on-screen status display.  GameState owns an instance of this class and
 * delegates all power-up responsibilities to it.
 */
public class PowerUpManager {

    // --------------- Constants -----------------------------------------------

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
    private static final float LEGACY_TABLE_WIDTH = 20f;
    private static final float LEGACY_TABLE_LENGTH = 30f;

    // --------------- Fields --------------------------------------------------

    private final AssetManager assetManager;
    private final Node gameNode;
    private final Puck puck;
    private final Table table;
    private final Random random;
    private final GameState.GameMode gameMode;

    /** The Y position of the table play plane (equals Puck.HALF_HEIGHT). */
    private final float tablePlaneY;

    private Paddle playerOnePaddle;
    private Paddle playerTwoPaddle; // may be null in SINGLE_PLAYER

    private PowerUp activePowerUp;
    private boolean hitboxDebugEnabled = false;
    private float powerUpSpawnTimerSeconds;
    private float puckSpeedBoostTimerSeconds;
    private float puckSpeedBoostMinSpeed;
    private float puckSizeTimerSeconds;
    private float puckSizeScale = 1f;
    private float playerOnePaddleSizeTimerSeconds;
    private float playerTwoPaddleSizeTimerSeconds;
    private float playerOnePaddleSizeScale = 1f;
    private float playerTwoPaddleSizeScale = 1f;

    /** Reference to the last side that touched the puck – set by GameState. */
    private GameState.Side lastTouchSide = GameState.Side.NONE;
    /** Reference to the current server – set by GameState. */
    private GameState.Side currentServer = GameState.Side.PLAYER_ONE;

    /** On-screen status text owned and created by this manager. */
    private BitmapText powerUpStatusText;

    // --------------- Constructor ---------------------------------------------

    public PowerUpManager(AssetManager assetManager, Node gameNode,
            BulletAppState bulletAppState, Puck puck, Table table,
            Random random, GameState.GameMode gameMode, float tablePlaneY) {
        this.assetManager = assetManager;
        this.gameNode = gameNode;
        this.puck = puck;
        this.table = table;
        this.random = random;
        this.gameMode = gameMode;
        this.tablePlaneY = tablePlaneY;
    }

    // --------------- Public setup API ----------------------------------------

    /**
     * Provide paddle references after they have been created.
     * playerTwo is either the human player-two paddle or the AI paddle depending
     * on game mode.  Pass {@code null} if not applicable.
     */
    public void setPaddles(Paddle playerOne, Paddle playerTwo) {
        this.playerOnePaddle = playerOne;
        this.playerTwoPaddle = playerTwo;
    }

    /**
     * Create and attach the status text label to the GUI.
     * Call after setPaddles, before the first update.
     */
    public void attachToGui(Node guiNode, AssetManager am, float screenHeight) {
        BitmapFont guiFont = am.loadFont("Interface/Fonts/Default.fnt");
        powerUpStatusText = new BitmapText(guiFont);
        powerUpStatusText.setSize(guiFont.getCharSet().getRenderedSize() * 0.95f);
        powerUpStatusText.setColor(ColorRGBA.White);
        powerUpStatusText.setLocalTranslation(20f, screenHeight - 76f, 0f);
        powerUpStatusText.setText("Effets actifs : aucun");
        guiNode.attachChild(powerUpStatusText);
    }

    public BitmapText getStatusText() {
        return powerUpStatusText;
    }

    // --------------- Notification from GameState -----------------------------

    /** Called by GameState whenever the last-touch side changes. */
    public void setLastTouchSide(GameState.Side side) {
        this.lastTouchSide = side;
    }

    /** Called by GameState whenever the current server changes. */
    public void setCurrentServer(GameState.Side server) {
        this.currentServer = server;
    }

    // --------------- Spawn scheduling ----------------------------------------

    public void scheduleNextPowerUpSpawn() {
        float delta = POWER_UP_SPAWN_MAX_SECONDS - POWER_UP_SPAWN_MIN_SECONDS;
        powerUpSpawnTimerSeconds = POWER_UP_SPAWN_MIN_SECONDS + random.nextFloat() * delta;
    }

    // --------------- Main update ---------------------------------------------

    public void update(float tpf) {
        updatePowerUpSystem(tpf);
        updatePowerUpStatusDisplay();
    }

    // --------------- Animations (called from GameState.update) ---------------

    public void updatePowerUpAnimations(float tpf) {
        if (activePowerUp != null) {
            activePowerUp.updateAnimation(tpf);
        }
        puck.updateSpeedFire(tpf);
        puck.updateSizePowerUpAnimation(tpf);

        if (playerOnePaddle != null) {
            playerOnePaddle.updateSizePowerUpAnimation(tpf);
        }
        if (playerTwoPaddle != null) {
            playerTwoPaddle.updateSizePowerUpAnimation(tpf);
        }
    }

    // --------------- Power-up system -----------------------------------------

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

    private void spawnRandomPowerUp() {
        PowerUp.Type[] availableTypes = PowerUp.Type.values();
        PowerUp.Type selectedType = availableTypes[random.nextInt(availableTypes.length)];
        Vector3f spawnPosition = findPowerUpSpawnPosition();
        activePowerUp = new PowerUp(selectedType, spawnPosition, assetManager);
        activePowerUp.attachTo(gameNode);
        if (hitboxDebugEnabled) {
            activePowerUp.setHitboxVisible(true);
        }
    }

    /** Toggles the yellow wireframe hitbox disc on the active (and future) power-ups. */
    public void setHitboxDebugEnabled(boolean enabled) {
        hitboxDebugEnabled = enabled;
        if (activePowerUp != null) {
            activePowerUp.setHitboxVisible(enabled);
        }
    }

    private Vector3f findPowerUpSpawnPosition() {
        float halfWidth = table.getWidth() / 2f;
        float halfLength = table.getLength() / 2f;
        float edgePadding = scaleWidth(POWER_UP_EDGE_PADDING);
        float goalPadding = scaleLength(POWER_UP_GOAL_PADDING);
        float minX = -(halfWidth - edgePadding);
        float maxX = halfWidth - edgePadding;
        float minZ = -(halfLength - goalPadding);
        float maxZ = halfLength - goalPadding;

        for (int i = 0; i < 12; i++) {
            float x = minX + random.nextFloat() * (maxX - minX);
            float z = minZ + random.nextFloat() * (maxZ - minZ);
            Vector3f candidate = new Vector3f(x, tablePlaneY, z);
            if (isPowerUpSpawnSafe(candidate)) {
                return candidate;
            }
        }

        return new Vector3f(0f, tablePlaneY, 0f);
    }

    private boolean isPowerUpSpawnSafe(Vector3f candidate) {
        float minPuckDistance = scaleAverage(POWER_UP_SAFE_DISTANCE_FROM_PUCK);
        if (candidate.distanceSquared(puck.getPosition()) < minPuckDistance * minPuckDistance) {
            return false;
        }

        float minPaddleDistance = scaleAverage(POWER_UP_SAFE_DISTANCE_FROM_PADDLE);
        float minPaddleDistSq = minPaddleDistance * minPaddleDistance;

        if (playerOnePaddle != null
                && candidate.distanceSquared(playerOnePaddle.getPosition()) < minPaddleDistSq) {
            return false;
        }
        return playerTwoPaddle == null
                || candidate.distanceSquared(playerTwoPaddle.getPosition()) >= minPaddleDistSq;
    }

    private void tryCollectActivePowerUp() {
        if (activePowerUp == null) {
            return;
        }

        float triggerDistance = puck.getRadius() + activePowerUp.getRadius();
        if (puck.getPosition().distanceSquared(activePowerUp.getPosition()) > triggerDistance * triggerDistance) {
            return;
        }

        GameState.Side collectingSide = resolveCollectingSide();
        applyPowerUpEffect(activePowerUp.getType(), collectingSide);

        clearActivePowerUp();
        scheduleNextPowerUpSpawn();
    }

    private GameState.Side resolveCollectingSide() {
        if (lastTouchSide != GameState.Side.NONE) {
            return lastTouchSide;
        }

        Vector3f puckVelocity = puck.getVelocity();
        if (Math.abs(puckVelocity.z) > 0.2f) {
            return (puckVelocity.z < 0f) ? GameState.Side.PLAYER_ONE : GameState.Side.PLAYER_TWO;
        }

        return currentServer;
    }

    // --------------- Effect application --------------------------------------

    private void applyPowerUpEffect(PowerUp.Type type, GameState.Side collectingSide) {
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

    private void applyShotOnGoal(GameState.Side collectingSide) {
        GameState.Side targetSide = oppositeSide(collectingSide);
        float goalTargetOffset = scaleLength(1f);
        float targetZ = (targetSide == GameState.Side.PLAYER_ONE)
                ? table.getLength() / 2f + goalTargetOffset
                : -(table.getLength() / 2f + goalTargetOffset);
        Vector3f target = new Vector3f(0f, tablePlaneY, targetZ);

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

    private void applyPaddleSizeScale(GameState.Side side, float scale) {
        Paddle paddle = getPaddleForSide(side);
        if (paddle == null) {
            return;
        }

        paddle.setRadiusScaleWithMarioAnimation(scale);
        if (side == GameState.Side.PLAYER_ONE) {
            playerOnePaddleSizeScale = scale;
            playerOnePaddleSizeTimerSeconds = POWER_UP_PADDLE_EFFECT_DURATION_SECONDS;
        } else if (side == GameState.Side.PLAYER_TWO) {
            playerTwoPaddleSizeScale = scale;
            playerTwoPaddleSizeTimerSeconds = POWER_UP_PADDLE_EFFECT_DURATION_SECONDS;
        }
    }

    // --------------- Timed-effect updates ------------------------------------

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
        if (playerTwoPaddle != null) {
            playerTwoPaddle.resetRadiusScale();
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

    // --------------- Cleanup -------------------------------------------------

    public void clearActivePowerUp() {
        if (activePowerUp != null) {
            activePowerUp.removeFromParent();
            activePowerUp = null;
        }
    }

    public void clearAllPowerUpEffects() {
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
        if (playerTwoPaddle != null) {
            playerTwoPaddle.resetRadiusScale();
        }
    }

    // --------------- Status display ------------------------------------------

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
            String sideTwoLabel = (gameMode == GameState.GameMode.TWO_PLAYER) ? "J2" : "IA";
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

    private boolean appendTimedEffect(StringBuilder status, boolean hasActive,
            String effectLabel, float remainingSeconds) {
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

    // --------------- Helpers -------------------------------------------------

    private Paddle getPaddleForSide(GameState.Side side) {
        if (side == GameState.Side.PLAYER_ONE) {
            return playerOnePaddle;
        }
        if (side == GameState.Side.PLAYER_TWO) {
            return playerTwoPaddle;
        }
        return null;
    }

    private GameState.Side oppositeSide(GameState.Side side) {
        if (side == GameState.Side.PLAYER_ONE) {
            return GameState.Side.PLAYER_TWO;
        }
        if (side == GameState.Side.PLAYER_TWO) {
            return GameState.Side.PLAYER_ONE;
        }
        return GameState.Side.NONE;
    }

    private float scaleLength(float legacyValue) {
        if (table == null) {
            return legacyValue;
        }
        return legacyValue * (table.getLength() / LEGACY_TABLE_LENGTH);
    }

    private float scaleWidth(float legacyValue) {
        if (table == null) {
            return legacyValue;
        }
        return legacyValue * (table.getWidth() / LEGACY_TABLE_WIDTH);
    }

    private float scaleAverage(float legacyValue) {
        return (scaleLength(legacyValue) + scaleWidth(legacyValue)) * 0.5f;
    }
}
