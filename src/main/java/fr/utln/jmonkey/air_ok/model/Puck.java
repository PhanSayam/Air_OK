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

    private Vector3f position = new Vector3f(0, HALF_HEIGHT, 0);
    private float radius = 0.8f;

    private AssetManager assetManager;
    private Node rootNode;
    private BulletAppState bulletAppState;
    private RigidBodyControl rigidbodyControl;
    private Geometry puckGeo;

    public Puck(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.bulletAppState = bulletAppState;
    }

    public void initPuck() {
        Cylinder puck = new Cylinder(2, 40, radius, 0.5f, true);
        puckGeo = new Geometry("Puck", puck);

        Material puckMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        puckMat.setColor("Color", ColorRGBA.Red);
        puckGeo.setMaterial(puckMat);
        // jME cylinder axis is Z by default, rotate to keep puck thickness on Y.
        puckGeo.rotate(FastMath.HALF_PI, 0f, 0f);

        puckGeo.setLocalTranslation(position);
        this.rootNode.attachChild(puckGeo);

        CylinderCollisionShape puckShape = new CylinderCollisionShape(new Vector3f(radius, HALF_HEIGHT, radius));
        rigidbodyControl = new RigidBodyControl(puckShape, 5.0f);
        puckGeo.addControl(rigidbodyControl);
        bulletAppState.getPhysicsSpace().add(rigidbodyControl);

        rigidbodyControl.setRestitution(1.0f);
        rigidbodyControl.setFriction(0f);
        rigidbodyControl.setDamping(0f, 0f);
        rigidbodyControl.setCcdMotionThreshold(0.01f);
        rigidbodyControl.setCcdSweptSphereRadius(radius * 0.9f);

        rigidbodyControl.setGravity(Vector3f.ZERO);
        rigidbodyControl.setAngularFactor(0f);
    }

    public RigidBodyControl getPhysicsControl() {
        return rigidbodyControl;
    }

    public Vector3f getPosition() {
        return puckGeo.getLocalTranslation();
    }

    public Vector3f getVelocity() {
        return rigidbodyControl.getLinearVelocity();
    }

    public float getRadius() {
        return radius;
    }

    public void resetPosition(Vector3f newPosition) {
        position = new Vector3f(newPosition.x, HALF_HEIGHT, newPosition.z);
        puckGeo.setLocalTranslation(position);
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
    }
}