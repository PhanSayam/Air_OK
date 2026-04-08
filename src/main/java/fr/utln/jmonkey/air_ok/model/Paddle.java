package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;

public class Paddle {

    public static final float HALF_HEIGHT = 0.25f;
    /** Collision shape is taller than the visual to prevent puck from tipping over paddle. */
    private static final float COLLISION_HALF_HEIGHT = 1.25f;

    private Vector3f position;
    private float radius = 1.0f;
    private ColorRGBA color;

    private AssetManager assetManager;
    private Node rootNode;
    private BulletAppState bulletAppState;
    private Node paddleNode;
    private Geometry paddle_geo;
    private RigidBodyControl paddlePhysics;

    public Paddle(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this(assetManager, rootNode, bulletAppState, new Vector3f(0, HALF_HEIGHT, 12f), ColorRGBA.Cyan);
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

        Cylinder paddleMesh = new Cylinder(2, 40, radius, HALF_HEIGHT * 2f, true);
        paddle_geo = new Geometry("Paddle", paddleMesh);

        Material paddle_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        paddle_mat.setColor("Color", color);
        paddle_geo.setMaterial(paddle_mat);
        // Same orientation as puck: thickness on Y axis.
        // We rotate the visual mesh, not the physics node.
        paddle_geo.rotate(FastMath.HALF_PI, 0f, 0f);

        paddleNode.attachChild(paddle_geo);
        paddleNode.setLocalTranslation(position);

        this.rootNode.attachChild(paddleNode);

        // Collision shape is taller than the visual to prevent puck tipping.
        // Axis 1 means the cylinder's height is along the Y axis.
        CylinderCollisionShape paddleShape = new CylinderCollisionShape(new Vector3f(radius, COLLISION_HALF_HEIGHT, radius), 1);
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