package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.SpotLight;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

public class Table {

    private static final float MODEL_SCALE = 0.01f;
    private static final float LEGACY_TABLE_WIDTH = 20f;
    private static final float LEGACY_TABLE_LENGTH = 30f;
    private static final float GOAL_WIDTH_RATIO = 6f / 20f;
    private static final float LEGACY_CENTER_NEUTRAL_RATIO = 2.2f / 30f;
    private static final float FALLBACK_WIDTH = 20f;
    private static final float FALLBACK_LENGTH = 30f;
    private static final float FLOOR_HALF_HEIGHT_LEGACY = 0.10f;
    private static final float SIDE_WALL_HALF_THICKNESS_LEGACY = 1.0f;
    private static final float SIDE_WALL_HALF_HEIGHT_LEGACY = 2.5f;
    private static final float END_BORDER_HALF_THICKNESS_LEGACY = 1.0f;
    private static final float WALL_CENTER_Y_LEGACY = 0.4f;

    private final AssetManager assetManager;
    private final Node rootNode;
    private final BulletAppState bulletAppState;

    private Spatial leftWallModel;
    private Spatial rightWallModel;
    private Spatial topDecorModel;
    private Spatial bottomDecorModel;

    private float width = FALLBACK_WIDTH;
    private float length = FALLBACK_LENGTH;
    private float goalWidth = FALLBACK_WIDTH * GOAL_WIDTH_RATIO;
    private float centerNeutralHalfDepth = FALLBACK_LENGTH * LEGACY_CENTER_NEUTRAL_RATIO;

    public Table(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.bulletAppState = bulletAppState;
    }

    public void initTable() {
        addArenaLights();

        Node arenaNode = new Node("ArenaModelNode");
        rootNode.attachChild(arenaNode);

        Spatial tableModel = attachScaledModel(arenaNode, "Models/table.glb", "TableModel");
        leftWallModel = attachScaledModel(arenaNode, "Models/wall1.glb", "LeftWallModel");
        rightWallModel = attachScaledModel(arenaNode, "Models/wall2.glb", "RightWallModel");

        topDecorModel = attachScaledModel(arenaNode, "Models/top.glb", "TopDecorModel");
        bottomDecorModel = attachScaledModel(arenaNode, "Models/bottom.glb", "BottomDecorModel");

        for (Spatial s : new Spatial[]{tableModel, leftWallModel, rightWallModel, topDecorModel, bottomDecorModel}) {
            makeGlbVisible(s);
        }

        alignArenaOnPlayPlane(arenaNode, tableModel);
        updateDimensionsFromTableModel(tableModel);
        addGameplayColliders();
    }

    private Spatial attachScaledModel(Node parent, String modelPath, String spatialName) {
        Spatial model = assetManager.loadModel(modelPath);
        model.setName(spatialName);
        model.setLocalScale(MODEL_SCALE);
        parent.attachChild(model);
        return model;
    }

    private void alignArenaOnPlayPlane(Node arenaNode, Spatial tableModel) {
        BoundingBox tableBounds = getBounds(tableModel);
        if (tableBounds == null) {
            return;
        }

        float centerX = tableBounds.getCenter().x;
        float centerZ = tableBounds.getCenter().z;
        float topY = tableBounds.getCenter().y + tableBounds.getYExtent();

        arenaNode.setLocalTranslation(-centerX, -topY, -centerZ);
        arenaNode.updateGeometricState();
    }

    private void updateDimensionsFromTableModel(Spatial tableModel) {
        BoundingBox tableBounds = getBounds(tableModel);
        if (tableBounds == null) {
            return;
        }

        width = tableBounds.getXExtent() * 2f;
        length = tableBounds.getZExtent() * 2f;
        goalWidth = width * GOAL_WIDTH_RATIO;
        centerNeutralHalfDepth = length * LEGACY_CENTER_NEUTRAL_RATIO;
    }

    private BoundingBox getBounds(Spatial spatial) {
        spatial.updateGeometricState();
        if (spatial.getWorldBound() instanceof BoundingBox box) {
            return box;
        }
        return null;
    }

    private void addGameplayColliders() {
        // Flat floor: a simple box is exact.
        float floorHalfHeight = scaleLength(FLOOR_HALF_HEIGHT_LEGACY);
        addStaticBoxCollider(
                "tableFloorCollider",
                new Vector3f(width / 2f, floorHalfHeight, length / 2f),
                new Vector3f(0f, -floorHalfHeight, 0f));

        // Side walls: use the actual mesh geometry so the rounded corners at each
        // end of the wall are matched precisely. Top/bottom decorative models are
        // intentionally left without a physics body (they only frame the goal area).
        addStaticMeshCollider(leftWallModel);
        addStaticMeshCollider(rightWallModel);

        // End walls: two box segments per end, one on each side of the goal opening.
        // Box colliders are safe here because the puck cannot get trapped in them.
        float wallHalfThickness = scaleWidth(SIDE_WALL_HALF_THICKNESS_LEGACY);
        float wallHalfHeight = scaleLength(SIDE_WALL_HALF_HEIGHT_LEGACY);
        float wallCenterY = scaleLength(WALL_CENTER_Y_LEGACY);
        float endBorderHalfThickness = scaleLength(END_BORDER_HALF_THICKNESS_LEGACY);
        float sideSegmentHalf = (width - goalWidth) / 4f + wallHalfThickness / 2f;
        float sideSegmentOffset = goalWidth / 2f + sideSegmentHalf;
        float endSegmentZ = length / 2f + endBorderHalfThickness;

        addStaticBoxCollider(
                "topLeftGoalSegmentCollider",
                new Vector3f(sideSegmentHalf, wallHalfHeight, endBorderHalfThickness),
                new Vector3f(-sideSegmentOffset, wallCenterY, endSegmentZ));
        addStaticBoxCollider(
                "topRightGoalSegmentCollider",
                new Vector3f(sideSegmentHalf, wallHalfHeight, endBorderHalfThickness),
                new Vector3f(sideSegmentOffset, wallCenterY, endSegmentZ));
        addStaticBoxCollider(
                "bottomLeftGoalSegmentCollider",
                new Vector3f(sideSegmentHalf, wallHalfHeight, endBorderHalfThickness),
                new Vector3f(-sideSegmentOffset, wallCenterY, -endSegmentZ));
        addStaticBoxCollider(
                "bottomRightGoalSegmentCollider",
                new Vector3f(sideSegmentHalf, wallHalfHeight, endBorderHalfThickness),
                new Vector3f(sideSegmentOffset, wallCenterY, -endSegmentZ));
    }

    private void addStaticMeshCollider(Spatial model) {
        model.updateGeometricState();
        CollisionShape shape = CollisionShapeFactory.createMeshShape(model);
        RigidBodyControl physicsControl = new RigidBodyControl(shape, 0.0f);
        model.addControl(physicsControl);
        bulletAppState.getPhysicsSpace().add(physicsControl);
        physicsControl.setRestitution(1.0f);
        physicsControl.setFriction(0f);
    }

    private void addStaticBoxCollider(String name, Vector3f halfExtents, Vector3f center) {
        Node colliderNode = new Node(name);
        colliderNode.setLocalTranslation(center);
        rootNode.attachChild(colliderNode);

        BoxCollisionShape collisionShape = new BoxCollisionShape(halfExtents);
        RigidBodyControl physicsControl = new RigidBodyControl(collisionShape, 0.0f);
        colliderNode.addControl(physicsControl);
        bulletAppState.getPhysicsSpace().add(physicsControl);
        physicsControl.setRestitution(1.0f);
        physicsControl.setFriction(0f);
    }

    private float scaleWidth(float legacyValue) {
        return legacyValue * (width / LEGACY_TABLE_WIDTH);
    }

    private float scaleLength(float legacyValue) {
        return legacyValue * (length / LEGACY_TABLE_LENGTH);
    }

    /**
     * Replaces GLTF/PBR materials with Unshaded materials, extracting the
     * embedded texture or base colour so the model is always visible regardless
     * of lighting setup or face-normal orientation.
     */
    private void makeGlbVisible(Spatial model) {
        model.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                Material src = geom.getMaterial();
                Material dst = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

                if (src != null) {
                    Object texVal = src.getParamValue("BaseColorMap");
                    Object colVal = src.getParamValue("BaseColor");
                    if (texVal instanceof Texture) {
                        dst.setTexture("ColorMap", (Texture) texVal);
                    } else if (colVal instanceof ColorRGBA) {
                        dst.setColor("Color", (ColorRGBA) colVal);
                    } else {
                        dst.setColor("Color", new ColorRGBA(0.7f, 0.7f, 0.7f, 1f));
                    }
                } else {
                    dst.setColor("Color", new ColorRGBA(0.7f, 0.7f, 0.7f, 1f));
                }

                dst.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                geom.setMaterial(dst);
            }
        });
    }

    private void addArenaLights() {
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.55f, 0.60f, 0.70f, 1f));
        rootNode.addLight(ambient);

        DirectionalLight keyLight = new DirectionalLight();
        keyLight.setDirection(new Vector3f(-0.5f, -1f, -0.25f).normalizeLocal());
        keyLight.setColor(new ColorRGBA(0.72f, 0.76f, 0.84f, 1f));
        rootNode.addLight(keyLight);

        SpotLight overheadLight = new SpotLight();
        overheadLight.setColor(new ColorRGBA(2.2f, 2.1f, 2.0f, 1f));
        overheadLight.setPosition(new Vector3f(0f, 18f, 0f));
        overheadLight.setDirection(new Vector3f(0f, -1f, 0f));
        overheadLight.setSpotRange(80f);
        overheadLight.setSpotInnerAngle(0.35f);
        overheadLight.setSpotOuterAngle(0.70f);
        rootNode.addLight(overheadLight);
    }

    public float getWidth() {
        return width;
    }

    public float getLength() {
        return length;
    }

    public float getGoalWidth() {
        return goalWidth;
    }

    public float getCenterNeutralHalfDepth() {
        return centerNeutralHalfDepth;
    }
}