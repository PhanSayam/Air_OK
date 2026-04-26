package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;

public class PowerUp {

    public enum Type {
        SPEED_PLUS("Speed +"),
        SIZE_PLUS("Size +"),
        SIZE_MINUS("Size -"),
        SHOT_ON_GOAL("Shot on Goal"),
        PADDLE_PLUS("Paddle +"),
        PADDLE_MINUS("Paddle -");

        private final String label;
        private final String modelPath;

        Type(String label) {
            this.label = label;
            this.modelPath = "Models/" + name().toLowerCase() + ".glb";
        }

        public String getLabel()     { return label; }
        public String getModelPath() { return modelPath; }
    }

    public static final float DISC_RADIUS = 100f;
    private static final float BOB_AMPLITUDE = 6f;

    private final Type type;
    private final Vector3f position;
    private final Node powerUpNode;
    private Geometry hitboxGeo;
    private float timeOffset;

    public PowerUp(Type type, Vector3f position, AssetManager assetManager) {
        this.type = type;
        this.position = position.clone();
        this.powerUpNode = createVisual(assetManager);
        this.powerUpNode.setLocalTranslation(this.position);
        this.timeOffset = (float) (Math.random() * FastMath.PI);

        hitboxGeo = buildHitboxDisc(assetManager);
        powerUpNode.attachChild(hitboxGeo);
    }

    private Node createVisual(AssetManager assetManager) {
        Node node = new Node("PowerUp_" + type.name());

        Spatial model = assetManager.loadModel(type.getModelPath());
        fitModelToRadius(model, DISC_RADIUS);
        alignModelBase(model);
        node.attachChild(model);

        return node;
    }

    private void fitModelToRadius(Spatial model, float targetRadius) {
        model.updateGeometricState();
        if (!(model.getWorldBound() instanceof BoundingBox bounds)) return;
        float r = Math.max(bounds.getXExtent(), bounds.getZExtent());
        if (r > 0.0001f) model.setLocalScale(targetRadius / r);
    }

    private void alignModelBase(Spatial model) {
        model.updateGeometricState();
        if (!(model.getWorldBound() instanceof BoundingBox bounds)) return;
        Vector3f c = bounds.getCenter();
        model.setLocalTranslation(-c.x, -(c.y - bounds.getYExtent()), -c.z);
    }

    private Geometry buildHitboxDisc(AssetManager assetManager) {
        Geometry geo = new Geometry("PowerUpHitbox",
                new Cylinder(2, 48, DISC_RADIUS, 2f, true));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Yellow);
        mat.getAdditionalRenderState().setWireframe(true);
        geo.setMaterial(mat);
        geo.rotate(FastMath.HALF_PI, 0f, 0f);
        geo.setCullHint(Spatial.CullHint.Always);
        return geo;
    }

    public void updateAnimation(float tpf) {
        timeOffset += tpf;
        powerUpNode.rotate(0f, 2.5f * tpf, 0f);
        float floatOffset = FastMath.sin(timeOffset * 4f) * BOB_AMPLITUDE;
        powerUpNode.setLocalTranslation(position.x, position.y + floatOffset, position.z);
    }

    public void setHitboxVisible(boolean visible) {
        if (hitboxGeo != null) {
            hitboxGeo.setCullHint(visible
                    ? Spatial.CullHint.Dynamic
                    : Spatial.CullHint.Always);
        }
    }

    public void attachTo(Node parentNode)  { parentNode.attachChild(powerUpNode); }
    public void removeFromParent()         { powerUpNode.removeFromParent(); }
    public Type getType()                  { return type; }
    public float getRadius()               { return DISC_RADIUS; }
    public Vector3f getPosition()          { return powerUpNode.getLocalTranslation(); }
}
