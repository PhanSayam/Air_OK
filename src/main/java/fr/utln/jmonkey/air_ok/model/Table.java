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
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

public class Table {

    private static final float GOAL_WIDTH_RATIO = 6f / 20f;

    private static final float FALLBACK_WIDTH = 20f;
    private static final float FALLBACK_LENGTH = 30f;
    private static final float FLOOR_HALF_HEIGHT = 0.10f;
    private static final float CENTER_NEUTRAL_HALF_DEPTH = 352f;

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
    private DirectionalLight shadowKeyLight;

    public Table(AssetManager assetManager, Node rootNode, BulletAppState bulletAppState) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
        this.bulletAppState = bulletAppState;
    }

    public void initTable() {
        addArenaLights();

        Node arenaNode = new Node("ArenaModelNode");
        rootNode.attachChild(arenaNode);

        Spatial tableModel = attachModel(arenaNode, "Models/table.glb", "TableModel");
        leftWallModel = attachModel(arenaNode, "Models/wall1.glb", "LeftWallModel");
        rightWallModel = attachModel(arenaNode, "Models/wall2.glb", "RightWallModel");
        topDecorModel = attachModel(arenaNode, "Models/top.glb", "TopDecorModel");
        bottomDecorModel = attachModel(arenaNode, "Models/bottom.glb", "BottomDecorModel");

        // Table surface slightly darkened so coloured power-ups stand out against the white.
        makeGlbVisible(tableModel,       new ColorRGBA(0.55f, 0.55f, 0.55f, 1f));
        makeGlbVisible(bottomDecorModel, new ColorRGBA(0.7f,  0.7f,  0.7f,  1f));

        // Walls and top border are intentionally black regardless of GLB material.
        paintModelColor(leftWallModel, ColorRGBA.Black);
        paintModelColor(rightWallModel, ColorRGBA.Black);
        paintModelColor(topDecorModel, ColorRGBA.Black);

        alignArenaOnPlayPlane(arenaNode, tableModel);
        updateDimensionsFromTableModel(tableModel);

        addGameplayColliders();
    }

    private Spatial attachModel(Node parent, String modelPath, String spatialName) {
        Spatial model = assetManager.loadModel(modelPath);
        model.setName(spatialName);
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
    }

    private BoundingBox getBounds(Spatial spatial) {
        spatial.updateGeometricState();
        if (spatial.getWorldBound() instanceof BoundingBox box) {
            return box;
        }
        return null;
    }

    private void addGameplayColliders() {
        addStaticBoxCollider(
                "tableFloorCollider",
                new Vector3f(width / 2f, FLOOR_HALF_HEIGHT, length / 2f),
                new Vector3f(0f, -FLOOR_HALF_HEIGHT, 0f));

        addStaticMeshCollider(leftWallModel);
        addStaticMeshCollider(rightWallModel);
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

    private void paintModelColor(Spatial model, ColorRGBA color) {
        model.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                mat.setBoolean("UseMaterialColors", true);
                mat.setColor("Ambient",  color.mult(0.3f));
                mat.setColor("Diffuse",  color);
                mat.setColor("Specular", new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));
                mat.setFloat("Shininess", 16f);
                mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                geom.setMaterial(mat);
                geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            }
        });
    }

    private void makeGlbVisible(Spatial model, ColorRGBA tint) {
        model.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Geometry geom) {
                Material src = geom.getMaterial();
                Material dst = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

                Texture baseColorTexture = null;
                if (src != null) {
                    Object texVal = src.getParamValue("BaseColorMap");
                    if (texVal instanceof Texture t) {
                        baseColorTexture = t;
                    }
                }

                if (baseColorTexture != null) {
                    dst.setTexture("ColorMap", baseColorTexture);
                    dst.setColor("Color", tint);
                } else {
                    ColorRGBA color = tint.clone();
                    if (src != null) {
                        Object colVal = src.getParamValue("BaseColor");
                        if (colVal instanceof ColorRGBA c) {
                            color = new ColorRGBA(
                                    c.r * tint.r,
                                    c.g * tint.g,
                                    c.b * tint.b,
                                    c.a);
                        }
                    }
                    dst.setColor("Color", color);
                }

                dst.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                geom.setMaterial(dst);
                geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
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
        this.shadowKeyLight = keyLight;

        SpotLight overheadLight = new SpotLight();
        overheadLight.setColor(new ColorRGBA(1.2f, 1.15f, 1.1f, 1f));
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

    public DirectionalLight getShadowKeyLight() {
        return shadowKeyLight;
    }

    public float getGoalWidth() {
        return goalWidth;
    }

    public float getCenterNeutralHalfDepth() {
        return CENTER_NEUTRAL_HALF_DEPTH;
    }
}