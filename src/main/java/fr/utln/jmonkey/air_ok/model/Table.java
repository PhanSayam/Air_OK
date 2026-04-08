package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;

public class Table {

    private static final float WIDTH = 20f;
    private static final float LENGTH = 30f;
    private static final float GOAL_WIDTH = 6f;
    /** Collision shapes for borders are much taller than their visuals to prevent puck tipping. */
    private static final float COLLISION_BORDER_HALF_HEIGHT = 2.5f;

    private AssetManager assetManager;
    private Node rootNode;
    private BulletAppState bulletAppState;
    private Material border_mat;

    public Table(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.bulletAppState = bulletAppState;
    }

    public void initTable() {

        Material table_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        // Texture tex = assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg");
        // tex.setWrap(Texture.WrapMode.Repeat);
        // table_mat.setTexture("ColorMap", tex);
        table_mat.setColor("Color", ColorRGBA.DarkGray);

        border_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        // Texture tex =
        // assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg");
        // border_mat.setTexture("ColorMap", tex);
        border_mat.setColor("Color", ColorRGBA.Blue);

        Box box = new Box(WIDTH / 2, 0.1f, LENGTH / 2);
        Geometry table_geo = new Geometry("Box", box);
        table_geo.setLocalTranslation(0, -0.1f, 0);
        table_geo.setMaterial(table_mat);
        this.rootNode.attachChild(table_geo);

        RigidBodyControl table_phy = new RigidBodyControl(0.0f);
        table_geo.addControl(table_phy);
        bulletAppState.getPhysicsSpace().add(table_phy);
        table_phy.setRestitution(1.0f);
        table_phy.setFriction(0f);

        Box long_border = new Box(1f, 0.5f, LENGTH / 2);

        Geometry Lborder_geo = new Geometry("leftBorder", long_border);
        Lborder_geo.setMaterial(border_mat);
        Lborder_geo.setLocalTranslation(-10f, 0.4f, 0);
        this.rootNode.attachChild(Lborder_geo);

        // Collision shape is taller than the visual border to prevent puck from tipping.
        BoxCollisionShape leftBorderShape = new BoxCollisionShape(new Vector3f(1f, COLLISION_BORDER_HALF_HEIGHT, LENGTH / 2));
        RigidBodyControl leftBorderPhysics = new RigidBodyControl(leftBorderShape, 0.0f);
        Lborder_geo.addControl(leftBorderPhysics);
        bulletAppState.getPhysicsSpace().add(leftBorderPhysics);
        leftBorderPhysics.setRestitution(1.0f);
        leftBorderPhysics.setFriction(0f);

        Geometry Rborder_geo = new Geometry("rightBorder", long_border);
        Rborder_geo.setMaterial(border_mat);
        Rborder_geo.setLocalTranslation(10f, 0.4f, 0f);
        this.rootNode.attachChild(Rborder_geo);

        // Collision shape is taller than the visual border to prevent puck from tipping.
        BoxCollisionShape rightBorderShape = new BoxCollisionShape(new Vector3f(1f, COLLISION_BORDER_HALF_HEIGHT, LENGTH / 2));
        RigidBodyControl rightBorderPhysics = new RigidBodyControl(rightBorderShape, 0.0f);
        Rborder_geo.addControl(rightBorderPhysics);
        bulletAppState.getPhysicsSpace().add(rightBorderPhysics);
        rightBorderPhysics.setRestitution(1.0f);
        rightBorderPhysics.setFriction(0f);

        addGoalBordersAndFrames();
    }

    private void addGoalBordersAndFrames() {
        float endBorderThickness = 1f;
        float borderHeight = 0.5f;
        float playableHalfWidth = WIDTH / 2f;
        float playableHalfLength = LENGTH / 2f;
        float sideSegmentHalf = (WIDTH - GOAL_WIDTH) / 4f;
        float sideSegmentOffset = GOAL_WIDTH / 2f + sideSegmentHalf;

        Box sideSegment = new Box(sideSegmentHalf, borderHeight, endBorderThickness);

        createStaticBorder("topLeftSegment", sideSegment, -sideSegmentOffset, 0.4f, playableHalfLength);
        createStaticBorder("topRightSegment", sideSegment, sideSegmentOffset, 0.4f, playableHalfLength);
        createStaticBorder("bottomLeftSegment", sideSegment, -sideSegmentOffset, 0.4f, -playableHalfLength);
        createStaticBorder("bottomRightSegment", sideSegment, sideSegmentOffset, 0.4f, -playableHalfLength);

        Material goalFrameMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        goalFrameMat.setColor("Color", ColorRGBA.White);

        Box topCrossbar = new Box(GOAL_WIDTH / 2f, 0.2f, 0.1f);
        Geometry topGoalGeo = new Geometry("topGoalFrame", topCrossbar);
        topGoalGeo.setMaterial(goalFrameMat);
        topGoalGeo.setLocalTranslation(0, 0.4f, playableHalfLength + 0.2f);
        rootNode.attachChild(topGoalGeo);

        Box bottomCrossbar = new Box(GOAL_WIDTH / 2f, 0.2f, 0.1f);
        Geometry bottomGoalGeo = new Geometry("bottomGoalFrame", bottomCrossbar);
        bottomGoalGeo.setMaterial(goalFrameMat);
        bottomGoalGeo.setLocalTranslation(0, 0.4f, -playableHalfLength - 0.2f);
        rootNode.attachChild(bottomGoalGeo);
    }

    private void createStaticBorder(String name, Box box, float x, float y, float z) {
        Geometry borderGeo = new Geometry(name, box);
        borderGeo.setMaterial(border_mat);
        borderGeo.setLocalTranslation(x, y, z);
        rootNode.attachChild(borderGeo);

        // Use a taller collision shape than the visual box to prevent puck from tipping.
        Vector3f halfExtents = new Vector3f(box.getXExtent(), COLLISION_BORDER_HALF_HEIGHT, box.getZExtent());
        BoxCollisionShape borderShape = new BoxCollisionShape(halfExtents);
        RigidBodyControl borderPhysics = new RigidBodyControl(borderShape, 0.0f);
        borderGeo.addControl(borderPhysics);
        bulletAppState.getPhysicsSpace().add(borderPhysics);
        borderPhysics.setRestitution(1.0f);
        borderPhysics.setFriction(0f);
    }

    public float getWidth() {
        return WIDTH;
    }

    public float getLength() {
        return LENGTH;
    }

    public float getGoalWidth() {
        return GOAL_WIDTH;
    }
}