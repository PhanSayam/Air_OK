package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;

public class Table {

    private static final float WIDTH = 20f;
    private static final float LENGTH = 30f;
    private static final float GOAL_WIDTH = 6f;
    private static final float CENTER_NEUTRAL_HALF_DEPTH = 2.2f;
    private static final float MARKING_HEIGHT = 0.015f;
    /**
     * Collision shapes for borders are much taller than their visuals to prevent
     * puck tipping.
     */
    private static final float COLLISION_BORDER_HALF_HEIGHT = 2.5f;

    private AssetManager assetManager;
    private Node rootNode;
    private BulletAppState bulletAppState;
    private Material sideWallMat;
    private Material goalWallMat;

    public Table(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.bulletAppState = bulletAppState;
    }

    public void initTable() {

        Material tableMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        tableMat.setColor("Color", new ColorRGBA(0.08f, 0.14f, 0.19f, 1f));

        sideWallMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        sideWallMat.setColor("Color", new ColorRGBA(0.07f, 0.46f, 0.84f, 1f));

        goalWallMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        goalWallMat.setColor("Color", new ColorRGBA(0.97f, 0.55f, 0.10f, 1f));

        Box box = new Box(WIDTH / 2, 0.1f, LENGTH / 2);
        Geometry table_geo = new Geometry("Box", box);
        table_geo.setLocalTranslation(0, -0.1f, 0);
        table_geo.setMaterial(tableMat);
        this.rootNode.attachChild(table_geo);

        addTableMarkings();

        RigidBodyControl table_phy = new RigidBodyControl(0.0f);
        table_geo.addControl(table_phy);
        bulletAppState.getPhysicsSpace().add(table_phy);
        table_phy.setRestitution(1.0f);
        table_phy.setFriction(0f);

        Box long_border = new Box(1f, 0.5f, LENGTH / 2);

        Geometry Lborder_geo = new Geometry("leftBorder", long_border);
        Lborder_geo.setMaterial(sideWallMat);
        Lborder_geo.setLocalTranslation(-11f, 0.4f, 0);
        this.rootNode.attachChild(Lborder_geo);

        // Collision shape is taller than the visual border to prevent puck from
        // tipping.
        BoxCollisionShape leftBorderShape = new BoxCollisionShape(
                new Vector3f(1f, COLLISION_BORDER_HALF_HEIGHT, LENGTH / 2));
        RigidBodyControl leftBorderPhysics = new RigidBodyControl(leftBorderShape, 0.0f);
        Lborder_geo.addControl(leftBorderPhysics);
        bulletAppState.getPhysicsSpace().add(leftBorderPhysics);
        leftBorderPhysics.setRestitution(1.0f);
        leftBorderPhysics.setFriction(0f);

        Geometry Rborder_geo = new Geometry("rightBorder", long_border);
        Rborder_geo.setMaterial(sideWallMat);
        Rborder_geo.setLocalTranslation(11f, 0.4f, 0f);
        this.rootNode.attachChild(Rborder_geo);

        // Collision shape is taller than the visual border to prevent puck from
        // tipping.
        BoxCollisionShape rightBorderShape = new BoxCollisionShape(
                new Vector3f(1f, COLLISION_BORDER_HALF_HEIGHT, LENGTH / 2));
        RigidBodyControl rightBorderPhysics = new RigidBodyControl(rightBorderShape, 0.0f);
        Rborder_geo.addControl(rightBorderPhysics);
        bulletAppState.getPhysicsSpace().add(rightBorderPhysics);
        rightBorderPhysics.setRestitution(1.0f);
        rightBorderPhysics.setFriction(0f);

        addGoalBordersAndFrames();
    }

    private void addTableMarkings() {
        Material centerLineMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        centerLineMat.setColor("Color", new ColorRGBA(0.90f, 0.94f, 0.97f, 1f));

        Material neutralZoneMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        neutralZoneMat.setColor("Color", new ColorRGBA(0.14f, 0.22f, 0.30f, 1f));

        Material ringMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        ringMat.setColor("Color", new ColorRGBA(0.26f, 0.80f, 0.95f, 1f));

        // Subtle neutral-zone strip in the center.
        Box neutralStrip = new Box(WIDTH / 2f, MARKING_HEIGHT / 2f, CENTER_NEUTRAL_HALF_DEPTH);
        Geometry neutralStripGeo = new Geometry("neutralStrip", neutralStrip);
        neutralStripGeo.setMaterial(neutralZoneMat);
        neutralStripGeo.setLocalTranslation(0f, MARKING_HEIGHT / 2f, 0f);
        rootNode.attachChild(neutralStripGeo);

        // Two delimitation lines for the no-player center zone.
        Box centerLine = new Box(WIDTH / 2f, MARKING_HEIGHT / 2f, 0.08f);
        Geometry upperCenterLine = new Geometry("upperCenterNeutralLine", centerLine);
        upperCenterLine.setMaterial(centerLineMat);
        upperCenterLine.setLocalTranslation(0f, MARKING_HEIGHT / 2f + 0.001f, CENTER_NEUTRAL_HALF_DEPTH);
        rootNode.attachChild(upperCenterLine);

        Geometry lowerCenterLine = new Geometry("lowerCenterNeutralLine", centerLine);
        lowerCenterLine.setMaterial(centerLineMat);
        lowerCenterLine.setLocalTranslation(0f, MARKING_HEIGHT / 2f + 0.001f, -CENTER_NEUTRAL_HALF_DEPTH);
        rootNode.attachChild(lowerCenterLine);

        // Decorative center ring and dot.
        Box ringOuter = new Box(1.4f, MARKING_HEIGHT / 2f, 1.4f);
        Geometry ringOuterGeo = new Geometry("centerRingOuter", ringOuter);
        ringOuterGeo.setMaterial(ringMat);
        ringOuterGeo.setLocalTranslation(0f, MARKING_HEIGHT / 2f + 0.002f, 0f);
        rootNode.attachChild(ringOuterGeo);

        Box ringInnerMask = new Box(1.0f, MARKING_HEIGHT, 1.0f);
        Geometry ringInnerMaskGeo = new Geometry("centerRingInnerMask", ringInnerMask);
        ringInnerMaskGeo.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        ringInnerMaskGeo.getMaterial().setColor("Color", new ColorRGBA(0.08f, 0.14f, 0.19f, 1f));
        ringInnerMaskGeo.setLocalTranslation(0f, MARKING_HEIGHT, 0f);
        rootNode.attachChild(ringInnerMaskGeo);

        Box centerDot = new Box(0.18f, MARKING_HEIGHT / 2f, 0.18f);
        Geometry centerDotGeo = new Geometry("centerDot", centerDot);
        centerDotGeo.setMaterial(centerLineMat);
        centerDotGeo.setLocalTranslation(0f, MARKING_HEIGHT / 2f + 0.003f, 0f);
        rootNode.attachChild(centerDotGeo);
    }

    private void addGoalBordersAndFrames() {
        float endBorderThickness = 1f;
        float borderHeight = 0.5f;
        float playableHalfWidth = WIDTH / 2f;
        float playableHalfLength = LENGTH / 2f;
        // Extend each segment toward side walls by half the wall thickness to avoid
        // corner trapping.
        float sideSegmentHalf = (WIDTH - GOAL_WIDTH) / 4f + endBorderThickness / 2f;
        float sideSegmentOffset = GOAL_WIDTH / 2f + sideSegmentHalf;

        Box sideSegment = new Box(sideSegmentHalf, borderHeight, endBorderThickness);

        createStaticBorder("topLeftSegment", sideSegment, -sideSegmentOffset, 0.4f, playableHalfLength + 1f,
                goalWallMat);
        createStaticBorder("topRightSegment", sideSegment, sideSegmentOffset, 0.4f, playableHalfLength + 1f,
                goalWallMat);
        createStaticBorder("bottomLeftSegment", sideSegment, -sideSegmentOffset, 0.4f, -(playableHalfLength + 1f),
                goalWallMat);
        createStaticBorder("bottomRightSegment", sideSegment, sideSegmentOffset, 0.4f, -(playableHalfLength + 1f),
                goalWallMat);

        Material goalFrameMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        goalFrameMat.setColor("Color", new ColorRGBA(0.96f, 0.98f, 1f, 1f));

        Box topCrossbar = new Box(GOAL_WIDTH / 2f, 0.2f, 0.1f);
        Geometry topGoalGeo = new Geometry("topGoalFrame", topCrossbar);
        topGoalGeo.setMaterial(goalFrameMat);
        topGoalGeo.setLocalTranslation(0, 0.4f, playableHalfLength + 1.2f);
        rootNode.attachChild(topGoalGeo);

        Box bottomCrossbar = new Box(GOAL_WIDTH / 2f, 0.2f, 0.1f);
        Geometry bottomGoalGeo = new Geometry("bottomGoalFrame", bottomCrossbar);
        bottomGoalGeo.setMaterial(goalFrameMat);
        bottomGoalGeo.setLocalTranslation(0, 0.4f, -(playableHalfLength + 1.2f));
        rootNode.attachChild(bottomGoalGeo);
    }

    private void createStaticBorder(String name, Box box, float x, float y, float z, Material visualMaterial) {
        Geometry borderGeo = new Geometry(name, box);
        borderGeo.setMaterial(visualMaterial);
        borderGeo.setLocalTranslation(x, y, z);
        rootNode.attachChild(borderGeo);

        // Use a taller collision shape than the visual box to prevent puck from
        // tipping.
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