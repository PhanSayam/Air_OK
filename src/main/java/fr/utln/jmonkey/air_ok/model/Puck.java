package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;

public class Puck {

    public static final float HALF_HEIGHT = 25f;
    private static final float BASE_RADIUS = 80f;
    private static final float SIZE_POWERUP_ANIM_DURATION = 1.35f;
    private static final float SIZE_POWERUP_OVERSHOOT = 0.16f;

    /**
     * Collision shape is much taller to prevent the puck from tipping over walls.
     */
    private static final float COLLISION_HALF_HEIGHT = 125f;

    private Vector3f position = new Vector3f(0, HALF_HEIGHT, 0);
    private float radius = BASE_RADIUS;

    private AssetManager assetManager;
    private Node rootNode;
    private BulletAppState bulletAppState;
    private RigidBodyControl rigidbodyControl;
    private Node puckNode;
    private Spatial puckVisualModel;
    private float puckVisualBaseScale = 1f;
    private ParticleEmitter speedFireEmitter;
    private float speedFirePulseTime;
    private float visualRadius = BASE_RADIUS;
    private float sizeAnimFromRadius;
    private float sizeAnimToRadius;
    private float sizeAnimTimer;
    private boolean sizeAnimActive;
    private boolean sizeAnimGrowing;

    public Puck(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.bulletAppState = bulletAppState;
    }

    public void initPuck() {
        puckNode = new Node("PuckNode");

        puckVisualModel = assetManager.loadModel("Models/puck.glb");
        puckVisualBaseScale = fitModelToRadius(puckVisualModel, BASE_RADIUS);
        alignVisualModelOnPlayPlane(puckVisualModel);
        makeGlbVisible(puckVisualModel);

        puckNode.attachChild(puckVisualModel);
        visualRadius = radius;
        setVisualRadius(visualRadius);

        puckNode.setLocalTranslation(position);
        this.rootNode.attachChild(puckNode);

        CylinderCollisionShape puckShape = new CylinderCollisionShape(
                new Vector3f(radius, COLLISION_HALF_HEIGHT, radius), 1);
        rigidbodyControl = new RigidBodyControl(puckShape, 5.0f);
        puckNode.addControl(rigidbodyControl);
        bulletAppState.getPhysicsSpace().add(rigidbodyControl);

        rigidbodyControl.setRestitution(1.0f);
        rigidbodyControl.setFriction(0f);
        rigidbodyControl.setDamping(0f, 0f);
        rigidbodyControl.setCcdMotionThreshold(1f);
        rigidbodyControl.setCcdSweptSphereRadius(radius * 0.9f);

        rigidbodyControl.setGravity(Vector3f.ZERO);
        rigidbodyControl.setAngularFactor(1f);
    }

    private void makeGlbVisible(Spatial model) {
        model.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                Material src = geom.getMaterial();
                Material dst = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");

                if (src != null) {
                    Object texVal = src.getParamValue("BaseColorMap");
                    Object colVal = src.getParamValue("BaseColor");
                    if (texVal instanceof Texture t) {
                        dst.setTexture("BaseColorMap", t);
                    } else if (colVal instanceof ColorRGBA c) {
                        dst.setColor("BaseColor", c);
                    }
                }
                dst.setFloat("Metallic",  0.5f);
                dst.setFloat("Roughness", 0.3f);

                dst.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                geom.setMaterial(dst);
                geom.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.CastAndReceive);
            }
        });
    }

    private float fitModelToRadius(Spatial model, float targetRadius) {
        BoundingBox bounds = getBounds(model);
        if (bounds == null) {
            return 1f;
        }

        float sourceRadius = Math.max(bounds.getXExtent(), bounds.getZExtent());
        if (sourceRadius <= 0.0001f) {
            return 1f;
        }

        float scale = targetRadius / sourceRadius;
        model.setLocalScale(scale);
        return scale;
    }

    private void alignVisualModelOnPlayPlane(Spatial model) {
        Node parent = model.getParent();
        if (parent != null) {
            parent.detachChild(model);
        }
        model.setLocalTranslation(0f, 0f, 0f);
        model.updateGeometricState();

        Vector3f boundsCenter = null;
        float boundsYExtent = 0f;
        if (model.getWorldBound() instanceof BoundingBox box) {
            boundsCenter = box.getCenter().clone();
            boundsYExtent = box.getYExtent();
        }

        if (parent != null) {
            parent.attachChild(model);
        }
        if (boundsCenter == null) {
            return;
        }

        float minY = boundsCenter.y - boundsYExtent;
        model.setLocalTranslation(
                -boundsCenter.x,
                -HALF_HEIGHT - minY,
                -boundsCenter.z);
    }

    private BoundingBox getBounds(Spatial model) {
        model.updateGeometricState();
        if (model.getWorldBound() instanceof BoundingBox box) {
            return box;
        }
        return null;
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
        visualRadius = Math.max(5f, newRadius);
        if (puckVisualModel != null) {
            float radiusScale = visualRadius / BASE_RADIUS;
            puckVisualModel.setLocalScale(
                    puckVisualBaseScale * radiusScale,
                    puckVisualBaseScale,
                    puckVisualBaseScale * radiusScale);
            alignVisualModelOnPlayPlane(puckVisualModel);
        }
    }

    private void cancelSizePowerupAnimation() {
        sizeAnimActive = false;
        sizeAnimTimer = 0f;
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

        emitter.setGravity(0f, -210f, 0f);
        emitter.getParticleInfluencer().setInitialVelocity(new Vector3f(0f, 290f, 0f));
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

    /** Move the puck by setting the spatial directly (for kinematic/display-only use). */
    public void setDisplayPosition(float x, float z) {
        puckNode.setLocalTranslation(x, HALF_HEIGHT, z);
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

        Vector3f angularVelocity = rigidbodyControl.getAngularVelocity();
        if (Math.abs(angularVelocity.x) > 0.0001f || Math.abs(angularVelocity.z) > 0.0001f) {
            rigidbodyControl.setAngularVelocity(new Vector3f(0f, angularVelocity.y, 0f));
        }

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