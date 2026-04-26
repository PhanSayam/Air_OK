
package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.util.SkyFactory;

public class ArcadeEnvironment {

    private final AssetManager assetManager;
    private final Node rootNode;
    private Node envNode;

    private static final float GRID_Y = -0.5f;
    private static final float GRID_SIZE = 1000f;

    public ArcadeEnvironment(AssetManager assetManager, Node rootNode) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
    }

    public void initEnvironment() {
        envNode = new Node("ArcadeEnvironmentNode");
        rootNode.attachChild(envNode);

        try {
            Spatial sky = SkyFactory.createSky(assetManager, "Textures/arcade.png",
                    SkyFactory.EnvMapType.SphereMap);
            envNode.attachChild(sky);
        } catch (Exception e) {
            System.err.println("Image non trouvee, on reste sur le fond par defaut.");
        }

        addFloorGrid();
    }

    private void addFloorGrid() {
        Material matGrid = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matGrid.setColor("Color", ColorRGBA.Cyan);

        int lines = 20;
        float spacing = GRID_SIZE / lines;

        for (int i = -lines; i <= lines; i++) {
            Box b1 = new Box(0.1f, 0.05f, GRID_SIZE);
            Geometry g1 = new Geometry("GridLineX", b1);
            g1.setMaterial(matGrid);
            g1.setLocalTranslation(i * spacing, GRID_Y, 0);
            envNode.attachChild(g1);

            Box b2 = new Box(GRID_SIZE, 0.05f, 0.1f);
            Geometry g2 = new Geometry("GridLineZ", b2);
            g2.setMaterial(matGrid);
            g2.setLocalTranslation(0, GRID_Y, i * spacing);
            envNode.attachChild(g2);
        }
    }

    public void cleanup() {
        if (envNode != null) envNode.removeFromParent();
    }
}