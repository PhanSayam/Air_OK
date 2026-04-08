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

public class Puck {

    public static final float HALF_HEIGHT = 0.25f;
    /** Collision shape is much taller to prevent the puck from tipping over walls. */
    private static final float COLLISION_HALF_HEIGHT = 1.25f;

    private Vector3f position = new Vector3f(0, HALF_HEIGHT, 0);
    private float radius = 0.8f;

    private AssetManager assetManager;
    private Node rootNode;
    private BulletAppState bulletAppState;
    private RigidBodyControl rigidbodyControl;
    private Node puckNode;
    private Geometry puckGeo;

    public Puck(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.bulletAppState = bulletAppState;
    }

    public void initPuck() {
        puckNode = new Node("PuckNode");

        Cylinder puck = new Cylinder(2, 40, radius, 0.5f, true);
        puckGeo = new Geometry("Puck", puck);

        Material puckMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        puckMat.setColor("Color", ColorRGBA.Red);
        puckGeo.setMaterial(puckMat);

        // Visual marker: one half in a contrasting color to make spin visible.
        Cylinder halfMarker = new Cylinder(2, 40, radius * 0.52f, 0.52f, true);
        Geometry markerGeo = new Geometry("PuckSpinMarker", halfMarker);
        Material markerMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        markerMat.setColor("Color", ColorRGBA.White);
        markerGeo.setMaterial(markerMat);
        markerGeo.setLocalTranslation(radius * 0.48f, 0f, 0f);
        // Rotate the individual visual meshes so their local Z axis points up (Y).
        puckGeo.rotate(FastMath.HALF_PI, 0f, 0f);
        markerGeo.rotate(FastMath.HALF_PI, 0f, 0f);

        puckNode.attachChild(puckGeo);
        puckNode.attachChild(markerGeo);

        puckNode.setLocalTranslation(position);
        this.rootNode.attachChild(puckNode);

        // Collision shape is taller than the visual to prevent tipping at wall edges.
        // Axis 1 means the cylinder's height is along the Y axis.
        CylinderCollisionShape puckShape = new CylinderCollisionShape(new Vector3f(radius, COLLISION_HALF_HEIGHT, radius), 1);
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