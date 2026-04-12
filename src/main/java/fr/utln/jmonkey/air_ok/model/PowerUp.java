package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
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

        Type(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static final float HALF_HEIGHT = 0.08f;
    private static final float RADIUS = 0.55f;
    // Assuming Puck.HALF_HEIGHT is accessible, e.g., 0.25f
    private static final float POSITION_Y = 0.25f + HALF_HEIGHT;

    private final Type type;
    private final Vector3f position;
    private final Node powerUpNode;

    // Timer for the bobbing animation
    private float timeOffset;

    public PowerUp(Type type, Vector3f position, AssetManager assetManager) {
        this.type = type;
        this.position = new Vector3f(position.x, POSITION_Y, position.z);
        this.powerUpNode = createVisual(assetManager);
        this.powerUpNode.setLocalTranslation(this.position);

        // Randomize start time so multiple power-ups don't bob in perfect sync
        this.timeOffset = (float) (Math.random() * FastMath.PI);
    }

    private Node createVisual(AssetManager assetManager) {
        Node node = new Node("PowerUp_" + type.name());

        Cylinder body = new Cylinder(16, 24, RADIUS, HALF_HEIGHT * 2f, true);
        Geometry bodyGeo = new Geometry("PowerUpBody", body);
        Material bodyMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        bodyMat.setColor("Color", getColor(type));
        bodyGeo.setMaterial(bodyMat);
        // Rotate flat to lie on the table
        bodyGeo.rotate(FastMath.HALF_PI, 0f, 0f);

        Cylinder ring = new Cylinder(16, 24, RADIUS * 0.65f, HALF_HEIGHT * 0.6f, true);
        Geometry ringGeo = new Geometry("PowerUpRing", ring);
        Material ringMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        ringMat.setColor("Color", ColorRGBA.White);
        ringGeo.setMaterial(ringMat);
        // Rotate flat and move slightly up to sit inside the body
        ringGeo.rotate(FastMath.HALF_PI, 0f, 0f);
        ringGeo.setLocalTranslation(0f, HALF_HEIGHT * 0.45f, 0f);

        node.attachChild(bodyGeo);
        node.attachChild(ringGeo);
        return node;
    }

    private ColorRGBA getColor(Type powerUpType) {
        return switch (powerUpType) {
            case SPEED_PLUS -> new ColorRGBA(1f, 0.78f, 0.15f, 1f);
            case SIZE_PLUS -> new ColorRGBA(0.38f, 0.90f, 0.45f, 1f);
            case SIZE_MINUS -> new ColorRGBA(0.98f, 0.40f, 0.40f, 1f);
            case SHOT_ON_GOAL -> new ColorRGBA(0.30f, 0.78f, 1f, 1f);
            case PADDLE_PLUS -> new ColorRGBA(0.58f, 0.58f, 1f, 1f);
            case PADDLE_MINUS -> new ColorRGBA(1f, 0.56f, 0.22f, 1f);
        };
    }

    /**
     * Updates the floating and spinning animation.
     * MUST be called from the main update loop.
     */
    public void updateAnimation(float tpf) {
        timeOffset += tpf;

        // Spin around Y axis
        powerUpNode.rotate(0f, 2.5f * tpf, 0f);

        // Bob up and down using a Sine wave
        float floatOffset = FastMath.sin(timeOffset * 4f) * 0.05f;
        powerUpNode.setLocalTranslation(position.x, POSITION_Y + floatOffset, position.z);
    }

    public void attachTo(Node parentNode) {
        parentNode.attachChild(powerUpNode);
    }

    public void removeFromParent() {
        powerUpNode.removeFromParent();
    }

    public Type getType() {
        return type;
    }

    public float getRadius() {
        return RADIUS;
    }

    public Vector3f getPosition() {
        return powerUpNode.getLocalTranslation();
    }
}