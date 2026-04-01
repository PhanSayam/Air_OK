package fr.utln.jmonkey.air_ok.model;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;




public class Table {

    private float width = 20f;
    private float length = 30f;
    private float neutralZoneRatio = 0.2f;

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
        //Texture tex = assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg");
        //tex.setWrap(Texture.WrapMode.Repeat);
        //table_mat.setTexture("ColorMap", tex);
        table_mat.setColor("Color", ColorRGBA.DarkGray);

        border_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        //Texture tex = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg");
        //border_mat.setTexture("ColorMap", tex);
        border_mat.setColor("Color", ColorRGBA.Blue);

        Box box = new Box(width/2, 0.1f, length/2);
        Geometry table_geo = new Geometry("Box", box);
        table_geo.setLocalTranslation(0, -0.1f, 0);
        table_geo.setMaterial(table_mat);
        this.rootNode.attachChild(table_geo);

        RigidBodyControl table_phy = new RigidBodyControl(0.0f);
        table_geo.addControl(table_phy);
        bulletAppState.getPhysicsSpace().add(table_phy);

        Box long_border = new Box(1f, 0.5f, 15f);

        Geometry Lborder_geo = new Geometry("leftBorder", long_border);
        Lborder_geo.setMaterial(border_mat);
        Lborder_geo.setLocalTranslation(-10f, 0.4f, 0);
        this.rootNode.attachChild(Lborder_geo);

        Geometry Rborder_geo = new Geometry("rightBorder", long_border);
        Rborder_geo.setMaterial(border_mat);
        Rborder_geo.setLocalTranslation(10f, 0.4f, 0f);
        this.rootNode.attachChild(Rborder_geo);
    }
}