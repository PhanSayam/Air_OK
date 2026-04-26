package fr.utln.jmonkey.air_ok.view;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.LineWrapMode;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

public class EndScreenView {
    private static final String MATERIAL_COLOR_PARAM = "Color";
    private static final ColorRGBA PANEL_COLOR                    = new ColorRGBA(0.88f, 0.88f, 0.88f, 0.68f);
    private static final ColorRGBA OPTION_TEXT_FRAME_COLOR         = new ColorRGBA(0.42f, 0.42f, 0.44f, 1f);
    private static final ColorRGBA OPTION_TEXT_FRAME_SELECTED_COLOR = new ColorRGBA(0.20f, 0.20f, 0.23f, 1f);
    private static final ColorRGBA OPTION_TEXT_COLOR               = ColorRGBA.Black;
    private static final ColorRGBA OPTION_SELECTED_TEXT_COLOR      = ColorRGBA.Black;

    private final Node rootNode;
    private final BitmapText scoresText;
    private final Geometry[] optionTextFrames;
    private final BitmapText[] optionTexts;
    private final float[] optionMinX;
    private final float[] optionMaxX;
    private final float[] optionMinY;
    private final float[] optionMaxY;
    private int selectedIndex;

    public EndScreenView(AssetManager assetManager, float screenWidth, float screenHeight) {
        rootNode = new Node("EndScreenView");

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        float menuColumnWidth = Math.min(430f, screenWidth * 0.36f);
        float menuLeft = Math.max(44f, screenWidth * 0.06f);
        float menuTop = screenHeight * 0.74f;

        BitmapText title = new BitmapText(font);
        title.setSize(font.getCharSet().getRenderedSize() * 2.6f);
        title.setColor(ColorRGBA.Black);
        title.setText("Fin de partie");
        float titleY = menuTop;
        title.setLocalTranslation(menuLeft + (menuColumnWidth - title.getLineWidth()) * 0.5f,
                titleY, 1f);
        rootNode.attachChild(title);

        scoresText = new BitmapText(font);
        scoresText.setSize(font.getCharSet().getRenderedSize() * 0.95f);
        scoresText.setColor(ColorRGBA.Black);
        scoresText.setBox(new Rectangle(menuLeft, titleY - title.getLineHeight() - 132f, menuColumnWidth, 120f));
        scoresText.setLineWrapMode(LineWrapMode.Word);
        scoresText.setAlignment(BitmapFont.Align.Center);
        scoresText.setText("La partie est terminee.");
        scoresText.setLocalTranslation(0f, 0f, 1f);
        rootNode.attachChild(scoresText);

        String[] entries = { "Revenir au menu", "Quitter l'application" };
        optionTextFrames = new Geometry[entries.length];
        optionTexts = new BitmapText[entries.length];
        optionMinX = new float[entries.length];
        optionMaxX = new float[entries.length];
        optionMinY = new float[entries.length];
        optionMaxY = new float[entries.length];

        float optionWidth = menuColumnWidth;
        float optionHeight = 62f;
        float optionX = menuLeft;
        float firstOptionY = titleY - title.getLineHeight() - 255f;
        float optionSpacing = 82f;

        for (int i = 0; i < entries.length; i++) {
            float y = firstOptionY - i * optionSpacing;

            BitmapText optionText = new BitmapText(font);
            optionText.setSize(font.getCharSet().getRenderedSize() * 1.16f);
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
            Geometry optionTextFrame = createQuad(assetManager, "EndOptionTextFrame_" + i,
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
        hint.setSize(font.getCharSet().getRenderedSize() * 0.72f);
        hint.setColor(ColorRGBA.Black);
        hint.setText("Fleches + Entree ou clic souris");
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
        Geometry panel = createQuad(assetManager, "EndPanel",
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
            Material mat = optionTextFrames[i].getMaterial();
            if (i == selectedIndex) {
                mat.setColor(MATERIAL_COLOR_PARAM, OPTION_TEXT_FRAME_SELECTED_COLOR);
                optionTexts[i].setColor(OPTION_SELECTED_TEXT_COLOR);
            } else {
                mat.setColor(MATERIAL_COLOR_PARAM, OPTION_TEXT_FRAME_COLOR);
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

    public void setScoresText(String text) {
        if (text == null || text.isBlank()) {
            scoresText.setText("La partie est terminee.");
            return;
        }

        scoresText.setText(text);
    }
}