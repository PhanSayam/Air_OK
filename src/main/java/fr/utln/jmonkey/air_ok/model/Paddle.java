package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;

public class Paddle {

    public static final float HALF_HEIGHT = 25f;
    private static final float BASE_RADIUS = 100f;
    private static final float MIN_RADIUS_SCALE = 0.45f;

    /**
     * Collision shape is taller than the visual to prevent puck from tipping over
     * paddle.
     */
    private static final float COLLISION_HALF_HEIGHT = 125f;

    private Vector3f position;
    private float radius = BASE_RADIUS;
    private ColorRGBA color;

    private AssetManager assetManager;
    private Node rootNode;
    private BulletAppState bulletAppState;
    private Node paddleNode;
    private Spatial paddleVisualModel;
    private float paddleVisualBaseScale = 1f;
    private RigidBodyControl paddlePhysics;

    public Paddle(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this(assetManager, rootNode, bulletAppState, new Vector3f(0, HALF_HEIGHT, 1200f), ColorRGBA.Cyan);
    }

    public Paddle(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState,
            Vector3f startPosition, ColorRGBA color) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.bulletAppState = bulletAppState;
        this.position = startPosition.clone();
        this.color = color;
    }

    public void initPaddle() {
        paddleNode = new Node("PaddleNode");

        paddleVisualModel = assetManager.loadModel("Models/paddle2.glb");
        paddleVisualBaseScale = fitModelToRadius(paddleVisualModel, BASE_RADIUS);
        alignVisualModelOnPlayPlane(paddleVisualModel);
        applyPlayerColor(paddleVisualModel, color);

        paddleNode.attachChild(paddleVisualModel);
        paddleNode.setLocalTranslation(position);
        this.rootNode.attachChild(paddleNode);

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
        paddlePhysics.setCcdMotionThreshold(1f);
        paddlePhysics.setCcdSweptSphereRadius(radius * 0.9f);
        paddlePhysics.setPhysicsLocation(position);
    }

    private float fitModelToRadius(Spatial model, float targetRadius) {
        model.updateGeometricState();
        if (!(model.getWorldBound() instanceof BoundingBox bounds)) {
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
        model.setLocalTranslation(-boundsCenter.x, -HALF_HEIGHT - minY, -boundsCenter.z);
    }

    /**
     * Applies a flat Unshaded material in the player's colour to every geometry
     * of the GLB model, making the paddle clearly identifiable per player.
     */
    private void applyPlayerColor(Spatial model, ColorRGBA playerColor) {
        model.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                mat.setColor("Color", playerColor);
                mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                geom.setMaterial(mat);
            }
        });
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

    public void setRadiusScaleWithMarioAnimation(float scale) {
        setRadiusScale(scale);
    }

    public void updateSizePowerUpAnimation(float tpf) {
        // No-op; kept for API compatibility.
    }

    public void resetRadiusScale() {
        setRadiusScale(1f);
    }

    private void setRadiusScale(float scale) {
        float safeScale = Math.max(MIN_RADIUS_SCALE, scale);
        radius = BASE_RADIUS * safeScale;

        if (paddleVisualModel != null) {
            paddleVisualModel.setLocalScale(
                    paddleVisualBaseScale * safeScale,
                    paddleVisualBaseScale,
                    paddleVisualBaseScale * safeScale);
            alignVisualModelOnPlayPlane(paddleVisualModel);
        }

        if (paddlePhysics != null) {
            CylinderCollisionShape paddleShape = new CylinderCollisionShape(
                    new Vector3f(radius, COLLISION_HALF_HEIGHT, radius), 1);
            paddlePhysics.setCollisionShape(paddleShape);
            paddlePhysics.setCcdSweptSphereRadius(radius * 0.9f);
            paddlePhysics.activate();
        }
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