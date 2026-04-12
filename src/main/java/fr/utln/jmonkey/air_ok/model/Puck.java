package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;

public class Puck {

    public static final float HALF_HEIGHT = 0.25f;
    private static final String MATERIAL_COLOR_PARAM = "Color";
    private static final float BASE_RADIUS = 0.8f;
    private static final float SIZE_POWERUP_ANIM_DURATION = 1.35f;
    private static final float SIZE_POWERUP_FLASH_DURATION = 1.20f;
    private static final float SIZE_POWERUP_OVERSHOOT = 0.16f;
    private static final ColorRGBA BASE_PUCK_COLOR = new ColorRGBA(1f, 0f, 0f, 1f);
    private static final ColorRGBA MARIO_GROW_FLASH_COLOR = new ColorRGBA(1f, 0.96f, 0.35f, 1f);
    private static final ColorRGBA MARIO_SHRINK_FLASH_COLOR = new ColorRGBA(0.58f, 0.86f, 1f, 1f);
    /**
     * Collision shape is much taller to prevent the puck from tipping over walls.
     */
    private static final float COLLISION_HALF_HEIGHT = 1.25f;

    private Vector3f position = new Vector3f(0, HALF_HEIGHT, 0);
    private float radius = BASE_RADIUS;

    private AssetManager assetManager;
    private Node rootNode;
    private BulletAppState bulletAppState;
    private RigidBodyControl rigidbodyControl;
    private Node puckNode;
    private Geometry puckGeo;
    private Geometry markerGeo;
    private Material puckMat;
    private ParticleEmitter speedFireEmitter;
    private float speedFirePulseTime;
    private float visualRadius = BASE_RADIUS;
    private float sizeAnimFromRadius;
    private float sizeAnimToRadius;
    private float sizeAnimTimer;
    private float sizeFlashTimer;
    private boolean sizeAnimActive;
    private boolean sizeAnimGrowing;

    public Puck(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.bulletAppState = bulletAppState;
    }

    public void initPuck() {
        puckNode = new Node("PuckNode");

        Cylinder puck = new Cylinder(2, 40, radius, 0.5f, true);
        puckGeo = new Geometry("Puck", puck);

        puckMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        puckMat.setColor(MATERIAL_COLOR_PARAM, BASE_PUCK_COLOR);
        puckGeo.setMaterial(puckMat);

        // Visual marker: one half in a contrasting color to make spin visible.
        Cylinder halfMarker = new Cylinder(2, 40, radius * 0.52f, 0.52f, true);
        markerGeo = new Geometry("PuckSpinMarker", halfMarker);
        Material markerMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        markerMat.setColor(MATERIAL_COLOR_PARAM, ColorRGBA.White);
        markerGeo.setMaterial(markerMat);
        markerGeo.setLocalTranslation(radius * 0.48f, 0f, 0f);
        // Rotate the individual visual meshes so their local Z axis points up (Y).
        puckGeo.rotate(FastMath.HALF_PI, 0f, 0f);
        markerGeo.rotate(FastMath.HALF_PI, 0f, 0f);

        puckNode.attachChild(puckGeo);
        puckNode.attachChild(markerGeo);
        visualRadius = radius;

        puckNode.setLocalTranslation(position);
        this.rootNode.attachChild(puckNode);

        // Collision shape is taller than the visual to prevent tipping at wall edges.
        // Axis 1 means the cylinder's height is along the Y axis.
        CylinderCollisionShape puckShape = new CylinderCollisionShape(
                new Vector3f(radius, COLLISION_HALF_HEIGHT, radius), 1);
        rigidbodyControl = new RigidBodyControl(puckShape, 5.0f);
        puckNode.addControl(rigidbodyControl);
        bulletAppState.getPhysicsSpace().add(rigidbodyControl);

        rigidbodyControl.setRestitution(1.0f);
        rigidbodyControl.setFriction(0f);
        rigidbodyControl.setDamping(0f, 0f);
        rigidbodyControl.setCcdMotionThreshold(0.01f);
        rigidbodyControl.setCcdSweptSphereRadius(radius * 0.9f);

        rigidbodyControl.setGravity(Vector3f.ZERO);
        // Allow rotation (spin around Y constrained in constrainToTablePlane).
        rigidbodyControl.setAngularFactor(1f);
    }

    public RigidBodyControl getPhysicsControl() {
        return rigidbodyControl;
    }

    public Vector3f getPosition() {
        return puckNode.getLocalTranslation();
    }

    public Vector3f getVelocity() {
        return rigidbodyControl.getLinearVelocity();
    }

    public float getRadius() {
        return radius;
    }

    public void setRadiusScale(float scale) {
        float targetRadius = applyRadiusScalePhysics(scale);
        cancelSizePowerupAnimation();
        setVisualRadius(targetRadius);
    }

    public void setRadiusScaleWithMarioAnimation(float scale) {
        float targetRadius = applyRadiusScalePhysics(scale);

        if (puckNode == null) {
            visualRadius = targetRadius;
            return;
        }

        sizeAnimFromRadius = visualRadius;
        sizeAnimToRadius = targetRadius;
        sizeAnimTimer = 0f;
        sizeFlashTimer = SIZE_POWERUP_FLASH_DURATION;
        sizeAnimGrowing = targetRadius >= visualRadius;
        sizeAnimActive = Math.abs(sizeAnimToRadius - sizeAnimFromRadius) > 0.0001f;

        if (!sizeAnimActive) {
            setVisualRadius(targetRadius);
        }
    }

    public void updateSizePowerUpAnimation(float tpf) {
        if (sizeAnimActive) {
            sizeAnimTimer = Math.min(SIZE_POWERUP_ANIM_DURATION, sizeAnimTimer + tpf);
            float alpha = sizeAnimTimer / SIZE_POWERUP_ANIM_DURATION;
            float easeOut = 1f - FastMath.pow(1f - alpha, 3f);
            float overshoot = FastMath.sin(alpha * FastMath.PI) * SIZE_POWERUP_OVERSHOOT * (1f - alpha);
            float blend = FastMath.clamp(easeOut + (sizeAnimGrowing ? overshoot : -overshoot), 0f, 1.1f);
            float animatedRadius = FastMath.interpolateLinear(blend, sizeAnimFromRadius, sizeAnimToRadius);
            setVisualRadius(animatedRadius);

            if (alpha >= 1f) {
                sizeAnimActive = false;
                setVisualRadius(sizeAnimToRadius);
            }
        }

        updateSizePowerUpFlash(tpf);
    }

    private float applyRadiusScalePhysics(float scale) {
        float safeScale = Math.max(0.35f, scale);
        radius = BASE_RADIUS * safeScale;

        if (rigidbodyControl != null) {
            CylinderCollisionShape puckShape = new CylinderCollisionShape(
                    new Vector3f(radius, COLLISION_HALF_HEIGHT, radius), 1);
            rigidbodyControl.setCollisionShape(puckShape);
            rigidbodyControl.setCcdSweptSphereRadius(radius * 0.9f);
            rigidbodyControl.activate();
        }

        updateSpeedFireAppearance();
        return radius;
    }

    private void setVisualRadius(float newRadius) {
        visualRadius = Math.max(0.05f, newRadius);
        if (puckNode != null) {
            // Keep node scale neutral to avoid visual oval distortion.
            puckNode.setLocalScale(1f, 1f, 1f);
        }
        updateVisualRadius(visualRadius);
    }

    private void updateVisualRadius(float currentRadius) {
        if (puckGeo == null || markerGeo == null) {
            return;
        }

        // Rebuild meshes with a new radius to preserve a circular puck silhouette.
        puckGeo.setMesh(new Cylinder(2, 40, currentRadius, 0.5f, true));
        markerGeo.setMesh(new Cylinder(2, 40, currentRadius * 0.52f, 0.52f, true));
        markerGeo.setLocalTranslation(currentRadius * 0.48f, 0f, 0f);
    }

    private void updateSizePowerUpFlash(float tpf) {
        if (puckMat == null) {
            return;
        }

        if (sizeFlashTimer <= 0f) {
            puckMat.setColor(MATERIAL_COLOR_PARAM, BASE_PUCK_COLOR);
            return;
        }

        sizeFlashTimer = Math.max(0f, sizeFlashTimer - tpf);
        float elapsed = SIZE_POWERUP_FLASH_DURATION - sizeFlashTimer;
        float pulse = 0.5f + 0.5f * FastMath.sin(elapsed * 30f);
        float fade = Math.max(0f, sizeFlashTimer / SIZE_POWERUP_FLASH_DURATION);
        float intensity = pulse * fade;

        ColorRGBA target = sizeAnimGrowing ? MARIO_GROW_FLASH_COLOR : MARIO_SHRINK_FLASH_COLOR;
        float inv = 1f - intensity;
        ColorRGBA blended = new ColorRGBA(
                BASE_PUCK_COLOR.r * inv + target.r * intensity,
                BASE_PUCK_COLOR.g * inv + target.g * intensity,
                BASE_PUCK_COLOR.b * inv + target.b * intensity,
                1f);
        puckMat.setColor(MATERIAL_COLOR_PARAM, blended);
    }

    private void cancelSizePowerupAnimation() {
        sizeAnimActive = false;
        sizeAnimTimer = 0f;
        sizeFlashTimer = 0f;
        if (puckMat != null) {
            puckMat.setColor(MATERIAL_COLOR_PARAM, BASE_PUCK_COLOR);
        }
    }

    public void resetRadiusScale() {
        setRadiusScale(1f);
    }

    public void setSpeedFireEnabled(boolean enabled) {
        if (!enabled) {
            disableSpeedFire();
            return;
        }

        if (puckNode == null) {
            return;
        }

        if (speedFireEmitter == null) {
            speedFireEmitter = createSpeedFireEmitter();
        }

        if (speedFireEmitter.getParent() == null) {
            puckNode.attachChild(speedFireEmitter);
        }
        updateSpeedFireAppearance();
    }

    public void updateSpeedFire(float tpf) {
        if (speedFireEmitter == null || speedFireEmitter.getParent() == null) {
            return;
        }

        speedFirePulseTime += tpf;
        float pulse = 0.92f + 0.18f * FastMath.sin(speedFirePulseTime * 12f);
        speedFireEmitter.setParticlesPerSec(95f * pulse);
    }

    private void disableSpeedFire() {
        if (speedFireEmitter != null) {
            speedFireEmitter.removeFromParent();
            speedFirePulseTime = 0f;
            speedFireEmitter.setParticlesPerSec(95f);
        }
    }

    private ParticleEmitter createSpeedFireEmitter() {
        ParticleEmitter emitter = new ParticleEmitter("PuckSpeedFire", ParticleMesh.Type.Triangle, 140);
        Material fireMat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        fireMat.setTexture("Texture", loadFireTexture());
        emitter.setMaterial(fireMat);

        emitter.setImagesX(1);
        emitter.setImagesY(1);
        emitter.setSelectRandomImage(false);

        emitter.setStartColor(new ColorRGBA(1f, 0.75f, 0.18f, 0.95f));
        emitter.setEndColor(new ColorRGBA(0.95f, 0.06f, 0.02f, 0f));
        emitter.setLowLife(0.12f);
        emitter.setHighLife(0.32f);
        emitter.setParticlesPerSec(95f);

        emitter.setGravity(0f, -2.1f, 0f);
        emitter.getParticleInfluencer().setInitialVelocity(new Vector3f(0f, 2.9f, 0f));
        emitter.getParticleInfluencer().setVelocityVariation(0.42f);
        emitter.setRandomAngle(true);
        emitter.setRotateSpeed(8f);

        return emitter;
    }

    private Texture loadFireTexture() {
        try {
            return assetManager.loadTexture("Effects/Explosion/flame.png");
        } catch (RuntimeException ex) {
            return createFallbackFireTexture();
        }
    }

    private Texture createFallbackFireTexture() {
        final int size = 32;
        ByteBuffer buffer = BufferUtils.createByteBuffer(size * size * 4);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float nx = (x + 0.5f) / size * 2f - 1f;
                float ny = (y + 0.5f) / size * 2f - 1f;
                float radial = FastMath.sqrt(nx * nx + ny * ny);

                float alpha = FastMath.clamp(1f - radial * 1.35f, 0f, 1f);
                float heat = FastMath.clamp(1f - radial, 0f, 1f);

                int r = 255;
                int g = (int) (90f + 150f * heat);
                int b = (int) (15f + 35f * heat);
                int a = (int) (alpha * 255f);

                buffer.put((byte) r);
                buffer.put((byte) g);
                buffer.put((byte) b);
                buffer.put((byte) a);
            }
        }

        buffer.flip();
        Image image = new Image(Image.Format.RGBA8, size, size, buffer, ColorSpace.Linear);
        Texture2D texture = new Texture2D(image);
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        return texture;
    }

    private void updateSpeedFireAppearance() {
        if (speedFireEmitter == null) {
            return;
        }

        speedFireEmitter.setStartSize(radius * 1.25f);
        speedFireEmitter.setEndSize(radius * 0.38f);
        speedFireEmitter.setLocalTranslation(0f, HALF_HEIGHT * 0.55f, 0f);
    }

    public void resetPosition(Vector3f newPosition) {
        position = new Vector3f(newPosition.x, HALF_HEIGHT, newPosition.z);
        puckNode.setLocalTranslation(position);
        rigidbodyControl.clearForces();
        rigidbodyControl.setPhysicsLocation(position);
        rigidbodyControl.setLinearVelocity(Vector3f.ZERO);
        rigidbodyControl.setAngularVelocity(Vector3f.ZERO);
        rigidbodyControl.activate();
    }

    public void constrainToTablePlane(float yValue) {
        Vector3f currentPos = rigidbodyControl.getPhysicsLocation();
        if (Math.abs(currentPos.y - yValue) > 0.0001f) {
            rigidbodyControl.setPhysicsLocation(new Vector3f(currentPos.x, yValue, currentPos.z));
        }

        Vector3f velocity = rigidbodyControl.getLinearVelocity();
        if (Math.abs(velocity.y) > 0.0001f) {
            rigidbodyControl.setLinearVelocity(new Vector3f(velocity.x, 0f, velocity.z));
        }

        // Zero out any residual tilt angular velocity on X and Z axes.
        Vector3f angularVelocity = rigidbodyControl.getAngularVelocity();
        if (Math.abs(angularVelocity.x) > 0.0001f || Math.abs(angularVelocity.z) > 0.0001f) {
            rigidbodyControl.setAngularVelocity(new Vector3f(0f, angularVelocity.y, 0f));
        }

        // Force the orientation to remain perfectly flat (no pitch or roll).
        com.jme3.math.Quaternion currentRot = rigidbodyControl.getPhysicsRotation();
        float[] angles = new float[3];
        currentRot.toAngles(angles);
        if (Math.abs(angles[0]) > 0.0001f || Math.abs(angles[2]) > 0.0001f) {
            angles[0] = 0f;
            angles[2] = 0f;
            com.jme3.math.Quaternion flatRot = new com.jme3.math.Quaternion().fromAngles(angles);
            rigidbodyControl.setPhysicsRotation(flatRot);
        }
    }
}