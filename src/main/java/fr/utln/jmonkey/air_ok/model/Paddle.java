package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;

import java.util.ArrayList;
import java.util.List;

public class Paddle {

    public static final float HALF_HEIGHT = 0.25f;
    private static final String MATERIAL_BASE_COLOR_PARAM = "BaseColor";
    private static final float BASE_RADIUS = 1.0f;
    private static final float SIZE_POWERUP_ANIM_DURATION = 1.35f;
    private static final float SIZE_POWERUP_FLASH_DURATION = 1.20f;
    private static final float SIZE_POWERUP_OVERSHOOT = 0.16f;
    private static final ColorRGBA MARIO_GROW_FLASH_COLOR = new ColorRGBA(1f, 0.96f, 0.35f, 1f);
    private static final ColorRGBA MARIO_SHRINK_FLASH_COLOR = new ColorRGBA(0.58f, 0.86f, 1f, 1f);
    /**
     * Collision shape is taller than the visual to prevent puck from tipping over
     * paddle.
     */
    private static final float COLLISION_HALF_HEIGHT = 1.25f;

    private Vector3f position;
    private float radius = BASE_RADIUS;
    private ColorRGBA color;

    private AssetManager assetManager;
    private Node rootNode;
    private BulletAppState bulletAppState;
    private Node paddleNode;
    private RigidBodyControl paddlePhysics;
    private final List<Material> tintedMaterials = new ArrayList<>();
    private float visualScale = 1f;
    private float sizeAnimFromScale;
    private float sizeAnimToScale;
    private float sizeAnimTimer;
    private float sizeFlashTimer;
    private boolean sizeAnimActive;
    private boolean sizeAnimGrowing;

    public Paddle(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this(assetManager, rootNode, bulletAppState, new Vector3f(0, HALF_HEIGHT, 12f), ColorRGBA.Cyan);
    }

    public Paddle(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState,
            Vector3f startPosition, ColorRGBA color) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.bulletAppState = bulletAppState;
        this.position = startPosition.clone();
        this.color = color.clone();
    }

    public void initPaddle() {
        paddleNode = new Node("PaddleNode");

        // --- VISUAL LOADING (Replaces Cylinder & Material) ---
        // Load the 3D model from the assets folder
        com.jme3.scene.Spatial visualModel = assetManager.loadModel("Models/paddle.glb");

        // IMPORTANT OFFSET:
        // In jME3, the physics CylinderCollisionShape is centered.
        // But in Blender, we placed the origin at the very bottom of the mesh!
        // So we shift the visual model down by HALF_HEIGHT so it perfectly wraps the
        // physics collider.
        visualModel.setLocalTranslation(0, -HALF_HEIGHT, 0);

        visualModel.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                // On récupère le matériau exporté depuis Blender
                Material mat = geom.getMaterial();
                // On écrase la couleur d'origine avec la couleur du joueur (this.color)
                if (mat.getParam(MATERIAL_BASE_COLOR_PARAM) != null) {
                    mat.setColor(MATERIAL_BASE_COLOR_PARAM, color);
                    if (!tintedMaterials.contains(mat)) {
                        tintedMaterials.add(mat);
                    }
                }
            }
        });

        paddleNode.attachChild(visualModel);
        // -----------------------------------------------------

        paddleNode.setLocalTranslation(position);
        this.rootNode.attachChild(paddleNode);

        // --- PHYSICS (Unchanged) ---
        // Collision shape is taller than the visual to prevent puck tipping.
        // Axis 1 means the cylinder's height is along the Y axis.
        CylinderCollisionShape paddleShape = new CylinderCollisionShape(
                new Vector3f(radius, COLLISION_HALF_HEIGHT, radius), 1);
        paddlePhysics = new RigidBodyControl(paddleShape, 20.0f);

        paddleNode.addControl(paddlePhysics);
        bulletAppState.getPhysicsSpace().add(paddlePhysics);

        paddlePhysics.setKinematic(true);
        paddlePhysics.setGravity(Vector3f.ZERO);
        paddlePhysics.setAngularFactor(0f);
        paddlePhysics.setFriction(0f);
        paddlePhysics.setRestitution(1.0f);
        paddlePhysics.setCcdMotionThreshold(0.005f);
        paddlePhysics.setCcdSweptSphereRadius(radius * 0.9f);
        paddlePhysics.setPhysicsLocation(position);
        visualScale = 1f;
    }

    public void setPosition(Vector3f newPosition) {
        position = new Vector3f(newPosition.x, HALF_HEIGHT, newPosition.z);
        paddleNode.setLocalTranslation(position);
        paddlePhysics.setPhysicsLocation(position);
        paddlePhysics.activate();
    }

    public Vector3f getPosition() {
        return paddlePhysics != null ? paddlePhysics.getPhysicsLocation() : position;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadiusScale(float scale) {
        float targetScale = applyRadiusScalePhysics(scale);
        cancelSizePowerupAnimation();
        setVisualScale(targetScale);
    }

    public void setRadiusScaleWithMarioAnimation(float scale) {
        float targetScale = applyRadiusScalePhysics(scale);

        if (paddleNode == null) {
            visualScale = targetScale;
            return;
        }

        sizeAnimFromScale = visualScale;
        sizeAnimToScale = targetScale;
        sizeAnimTimer = 0f;
        sizeFlashTimer = SIZE_POWERUP_FLASH_DURATION;
        sizeAnimGrowing = targetScale >= visualScale;
        sizeAnimActive = Math.abs(sizeAnimToScale - sizeAnimFromScale) > 0.0001f;

        if (!sizeAnimActive) {
            setVisualScale(targetScale);
        }
    }

    public void updateSizePowerUpAnimation(float tpf) {
        if (sizeAnimActive) {
            sizeAnimTimer = Math.min(SIZE_POWERUP_ANIM_DURATION, sizeAnimTimer + tpf);
            float alpha = sizeAnimTimer / SIZE_POWERUP_ANIM_DURATION;
            float easeOut = 1f - (float) Math.pow(1f - alpha, 3);
            float overshoot = (float) Math.sin(alpha * Math.PI) * SIZE_POWERUP_OVERSHOOT * (1f - alpha);
            float blend = Math.max(0f, Math.min(1.1f, easeOut + (sizeAnimGrowing ? overshoot : -overshoot)));
            float animatedScale = sizeAnimFromScale + (sizeAnimToScale - sizeAnimFromScale) * blend;
            setVisualScale(animatedScale);

            if (alpha >= 1f) {
                sizeAnimActive = false;
                setVisualScale(sizeAnimToScale);
            }
        }

        updateSizePowerUpFlash(tpf);
    }

    private float applyRadiusScalePhysics(float scale) {
        float safeScale = Math.max(0.45f, scale);
        radius = BASE_RADIUS * safeScale;

        if (paddlePhysics != null) {
            CylinderCollisionShape paddleShape = new CylinderCollisionShape(
                    new Vector3f(radius, COLLISION_HALF_HEIGHT, radius), 1);
            paddlePhysics.setCollisionShape(paddleShape);
            paddlePhysics.setCcdSweptSphereRadius(radius * 0.9f);
            paddlePhysics.activate();
        }

        return safeScale;
    }

    private void setVisualScale(float scale) {
        visualScale = Math.max(0.30f, scale);
        if (paddleNode != null) {
            paddleNode.setLocalScale(visualScale, 1f, visualScale);
        }
    }

    private void updateSizePowerUpFlash(float tpf) {
        if (tintedMaterials.isEmpty()) {
            return;
        }

        if (sizeFlashTimer <= 0f) {
            for (Material mat : tintedMaterials) {
                mat.setColor(MATERIAL_BASE_COLOR_PARAM, color);
            }
            return;
        }

        sizeFlashTimer = Math.max(0f, sizeFlashTimer - tpf);
        float elapsed = SIZE_POWERUP_FLASH_DURATION - sizeFlashTimer;
        float pulse = 0.5f + 0.5f * (float) Math.sin(elapsed * 30f);
        float fade = Math.max(0f, sizeFlashTimer / SIZE_POWERUP_FLASH_DURATION);
        float intensity = pulse * fade;

        ColorRGBA target = sizeAnimGrowing ? MARIO_GROW_FLASH_COLOR : MARIO_SHRINK_FLASH_COLOR;
        float inv = 1f - intensity;
        ColorRGBA blended = new ColorRGBA(
                color.r * inv + target.r * intensity,
                color.g * inv + target.g * intensity,
                color.b * inv + target.b * intensity,
                1f);

        for (Material mat : tintedMaterials) {
            mat.setColor(MATERIAL_BASE_COLOR_PARAM, blended);
        }
    }

    private void cancelSizePowerupAnimation() {
        sizeAnimActive = false;
        sizeAnimTimer = 0f;
        sizeFlashTimer = 0f;
        if (!tintedMaterials.isEmpty()) {
            for (Material mat : tintedMaterials) {
                mat.setColor(MATERIAL_BASE_COLOR_PARAM, color);
            }
        }
    }

    public void resetRadiusScale() {
        setRadiusScale(1f);
    }

    public void constrainToTablePlane(float yValue) {
        Vector3f currentPos = paddlePhysics.getPhysicsLocation();
        if (Math.abs(currentPos.y - yValue) > 0.0001f) {
            setPosition(new Vector3f(currentPos.x, yValue, currentPos.z));
        }
    }

    public RigidBodyControl getPhysicsControl() {
        return paddlePhysics;
    }
}