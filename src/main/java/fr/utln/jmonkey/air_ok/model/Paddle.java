package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;



import javax.swing.*;

public class Paddle {

        private Vector3f position;
        private float radius = 1.0f;
        private float sizeMultiplier = 1.0f;

        private AssetManager assetManager;
        private Node rootNode;
        private BulletAppState bulletAppState;
        private Geometry paddle_geo;
        private RigidBodyControl paddle_phy;


        public Paddle(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState, Vector3f startPosition) {
            this.assetManager = assetManager;
            this.rootNode = rootNode;
            this.bulletAppState = bulletAppState;
            this.position = startPosition;
        }

    public Vector3f getPosition() {
        return this.position;
    }

    public void initPaddle() {
        Box paddleBox = new Box(radius, 0.2f, 0.5f);
        paddle_geo = new Geometry("Paddle", paddleBox);

        Material paddle_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        paddle_mat.setColor("Color", ColorRGBA.Cyan);
        paddle_geo.setMaterial(paddle_mat);

        paddle_geo.setLocalTranslation(position);

        this.rootNode.attachChild(paddle_geo);

        paddle_phy = new RigidBodyControl(0.0f);
        paddle_geo.addControl(paddle_phy);
        bulletAppState.getPhysicsSpace().add(paddle_phy);
    }


    public void move(Vector3f newPos) {
        this.position = newPos;
        paddle_geo.setLocalTranslation(newPos);
        paddle_phy.setPhysicsLocation(newPos);
    }
}