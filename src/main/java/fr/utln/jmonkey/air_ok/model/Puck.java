package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Cylinder;

public  class Puck{

    private Vector3f position = new Vector3f(0, 0.5f, 0);
    private Vector3f velocity;
    private float radius = 0.8f;
    private float speedMultiplier = 1.0f;
    private float sizeMultiplier = 1.0f;

    private AssetManager assetManager;
    private Node rootNode;
    private BulletAppState bulletAppState;

    public Puck(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.bulletAppState = bulletAppState;
    }

    public void initPuck() {
        Cylinder puck = new Cylinder(2, 40, radius, 0.5f, true);
        Geometry puck_geo = new Geometry("Puck", puck);

        Material puck_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        puck_mat.setColor("Color", ColorRGBA.Red);
        puck_geo.setMaterial(puck_mat);

        puck_geo.setLocalTranslation(position);
        this.rootNode.attachChild(puck_geo);

        RigidBodyControl puck_phy = new RigidBodyControl(1.0f);
        puck_geo.addControl(puck_phy);
        bulletAppState.getPhysicsSpace().add(puck_phy);
    }
}