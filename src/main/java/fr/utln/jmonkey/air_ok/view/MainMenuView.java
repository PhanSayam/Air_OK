package fr.utln.jmonkey.air_ok.view;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

public class MainMenuView {
    private static final String MATERIAL_COLOR_PARAM = "Color";
    private static final ColorRGBA PANEL_COLOR                    = new ColorRGBA(0.88f, 0.88f, 0.88f, 0.68f);
    private static final ColorRGBA OPTION_TEXT_FRAME_COLOR         = new ColorRGBA(0.42f, 0.42f, 0.44f, 1f);
    private static final ColorRGBA OPTION_TEXT_FRAME_SELECTED_COLOR = new ColorRGBA(0.20f, 0.20f, 0.23f, 1f);
    private static final ColorRGBA OPTION_TEXT_COLOR               = ColorRGBA.Black;
    private static final ColorRGBA OPTION_SELECTED_TEXT_COLOR      = ColorRGBA.Black;
    private final Node rootNode;
    private final Geometry[] optionTextFrames;
    private final BitmapText[] optionTexts;
    private final float[] optionMinX;
    private final float[] optionMaxX;
    private final float[] optionMinY;
    private final float[] optionMaxY;
    private int selectedIndex;

    public MainMenuView(AssetManager assetManager, float screenWidth, float screenHeight) {
        rootNode = new Node("MainMenuView");

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        float menuColumnWidth = Math.min(430f, screenWidth * 0.36f);
        float menuLeft = Math.max(44f, screenWidth * 0.06f);
        float menuTop = screenHeight * 0.80f;

        //Geometry bg = createQuad(assetManager, "MenuBackground",
        //        new Vector2f(0f, 0f),
        //        new Vector2f(screenWidth, screenHeight),
        //        new ColorRGBA(0.02f, 0.04f, 0.07f, 0.18f), 0.1f);
        //rootNode.attachChild(bg);
        Geometry bg = createTexturedQuad(assetManager, "MenuBackground",
                screenWidth, screenHeight);
        rootNode.attachChild(bg);

        BitmapText title = new BitmapText(font);
        title.setSize(font.getCharSet().getRenderedSize() * 2.8f);
        title.setColor(ColorRGBA.Black);
        title.setText("AIR OK");
        float titleY = menuTop;
        title.setLocalTranslation(menuLeft + (menuColumnWidth - title.getLineWidth()) * 0.5f,
                titleY, 1f);
        rootNode.attachChild(title);

        BitmapText subtitle = new BitmapText(font);
        subtitle.setSize(font.getCharSet().getRenderedSize() * 0.92f);
        subtitle.setColor(ColorRGBA.Black);
        subtitle.setText("Selectionne un mode de jeu");
        float subtitleY = titleY - title.getLineHeight() - 14f;
        subtitle.setLocalTranslation(menuLeft + (menuColumnWidth - subtitle.getLineWidth()) * 0.5f,
                subtitleY, 1f);
        rootNode.attachChild(subtitle);

        // 4 options: 1 Joueur, 2 Joueurs, Tournoi, Quitter
        String[] entries = { "1 Joueur", "2 Joueurs", "Tournoi", "Quitter" };
        optionTextFrames = new Geometry[entries.length];
        optionTexts = new BitmapText[entries.length];
        optionMinX = new float[entries.length];
        optionMaxX = new float[entries.length];
        optionMinY = new float[entries.length];
        optionMaxY = new float[entries.length];

        float optionWidth = menuColumnWidth;
        float optionHeight = 58f;
        float optionX = menuLeft;
        float firstOptionY = subtitleY - subtitle.getLineHeight() - 48f;
        float optionSpacing = 74f;

        for (int i = 0; i < entries.length; i++) {
            float y = firstOptionY - i * optionSpacing;

            BitmapText optionText = new BitmapText(font);
            optionText.setSize(font.getCharSet().getRenderedSize() * 1.18f);
            optionText.setColor(OPTION_TEXT_COLOR);
            optionText.setText(entries[i]);
            optionText.setLocalTranslation(
                    optionX + (optionWidth - optionText.getLineWidth()) * 0.5f,
                    y + optionHeight * 0.62f,
                    1f);
            optionTexts[i] = optionText;
            rootNode.attachChild(optionText);

            float textFrameWidth = optionText.getLineWidth() + 56f;
            float textFrameHeight = optionText.getLineHeight() + 26f;
            float textFrameX = optionX + (optionWidth - textFrameWidth) * 0.5f;
            float textFrameY = y + (optionHeight - textFrameHeight) * 0.5f - 3f;
            Geometry optionTextFrame = createQuad(assetManager, "MainMenuOptionTextFrame_" + i,
                    new Vector2f(textFrameX, textFrameY),
                    new Vector2f(textFrameWidth, textFrameHeight),
                    OPTION_TEXT_FRAME_COLOR, 0.62f);
            optionTextFrames[i] = optionTextFrame;
            rootNode.attachChild(optionTextFrame);

            optionMinX[i] = textFrameX;
            optionMaxX[i] = textFrameX + textFrameWidth;
            optionMinY[i] = textFrameY;
            optionMaxY[i] = textFrameY + textFrameHeight;
        }

        BitmapText hint = new BitmapText(font);
        hint.setSize(font.getCharSet().getRenderedSize() * 0.75f);
        hint.setColor(ColorRGBA.Black);
        hint.setText("Fleches + Entree ou clic souris  |  1/2/T/Echap");
        float hintY = firstOptionY - optionSpacing * entries.length + 18f;
        hint.setLocalTranslation(menuLeft + (menuColumnWidth - hint.getLineWidth()) * 0.5f,
                hintY, 1f);
        rootNode.attachChild(hint);

        float panelPadding = 28f;
        float panelX = menuLeft - panelPadding;
        float panelBottom = hintY - hint.getLineHeight() - panelPadding;
        float panelTop = menuTop + title.getLineHeight() * 0.3f + panelPadding;
        float panelWidth = menuColumnWidth + panelPadding * 2f;
        float panelHeight = panelTop - panelBottom;
        Geometry panel = createQuad(assetManager, "MenuPanel",
                new Vector2f(panelX, panelBottom),
                new Vector2f(panelWidth, panelHeight),
                PANEL_COLOR, 0.3f);
        rootNode.attachChild(panel);

        setSelectedIndex(0);
    }

    private Geometry createQuad(AssetManager assetManager, String name, Vector2f position, Vector2f size,
            ColorRGBA color, float z) {
        Geometry geo = new Geometry(name, new Quad(size.x, size.y));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor(MATERIAL_COLOR_PARAM, color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setDepthWrite(false);
        geo.setMaterial(mat);
        geo.setLocalTranslation(position.x, position.y, z);
        geo.setQueueBucket(RenderQueue.Bucket.Gui);
        return geo;
    }

    private Geometry createTexturedQuad(AssetManager assetManager, String name,
                                        float width, float height) {
        Geometry geo = new Geometry(name, new Quad(width, height));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            com.jme3.texture.Texture tex = assetManager.loadTexture("Textures/arcade.png");
            tex.setWrap(com.jme3.texture.Texture.WrapMode.Clamp);
            mat.setTexture("ColorMap", tex);
        } catch (Exception e) {
            // fallback : fond sombre si l'image est absente
            mat.setColor("Color", new ColorRGBA(0.02f, 0.04f, 0.10f, 1f));
        }
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Off);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setDepthWrite(false);
        geo.setMaterial(mat);
        geo.setLocalTranslation(0f, 0f, 0f); // z=0, derrière tout le reste
        geo.setQueueBucket(RenderQueue.Bucket.Gui);
        return geo;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public int getOptionCount() {
        return optionTextFrames.length;
    }

    public void setSelectedIndex(int index) {
        selectedIndex = Math.max(0, Math.min(optionTextFrames.length - 1, index));
        updateSelectionPulse(0f);
    }

    public void updateSelectionPulse(float timer) {
        if (Float.isNaN(timer)) {
            return;
        }

        for (int i = 0; i < optionTextFrames.length; i++) {
            Material textFrameMat = optionTextFrames[i].getMaterial();
            if (i == selectedIndex) {
                textFrameMat.setColor(MATERIAL_COLOR_PARAM, OPTION_TEXT_FRAME_SELECTED_COLOR);
                optionTexts[i].setColor(OPTION_SELECTED_TEXT_COLOR);
            } else {
                textFrameMat.setColor(MATERIAL_COLOR_PARAM, OPTION_TEXT_FRAME_COLOR);
                optionTexts[i].setColor(OPTION_TEXT_COLOR);
            }
        }
    }

    public int pickOption(Vector2f cursorPosition) {
        float x = cursorPosition.x;
        float y = cursorPosition.y;
        for (int i = 0; i < optionTextFrames.length; i++) {
            if (x >= optionMinX[i] && x <= optionMaxX[i] && y >= optionMinY[i] && y <= optionMaxY[i]) {
                return i;
            }
        }
        return -1;
    }
}
