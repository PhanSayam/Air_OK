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

public class TournamentInterstitialView {

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

    public TournamentInterstitialView(AssetManager assetManager, float screenWidth, float screenHeight,
            String titleText, ColorRGBA titleColor, String infoText, String[] options) {

        rootNode = new Node("TournamentInterstitialView");

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        float menuColumnWidth = Math.min(480f, screenWidth * 0.40f);
        float menuLeft = Math.max(44f, screenWidth * 0.06f);
        float menuTop = screenHeight * 0.76f;

        BitmapText title = new BitmapText(font);
        title.setSize(font.getCharSet().getRenderedSize() * 2.4f);
        title.setColor(titleColor);
        title.setText(titleText);
        float titleY = menuTop;
        title.setLocalTranslation(menuLeft + (menuColumnWidth - title.getLineWidth()) * 0.5f, titleY, 1f);
        rootNode.attachChild(title);

        BitmapText info = new BitmapText(font);
        info.setSize(font.getCharSet().getRenderedSize() * 0.95f);
        info.setColor(ColorRGBA.Black);
        info.setBox(new Rectangle(menuLeft, titleY - title.getLineHeight() - 110f, menuColumnWidth, 100f));
        info.setLineWrapMode(LineWrapMode.Word);
        info.setAlignment(BitmapFont.Align.Center);
        info.setText(infoText);
        info.setLocalTranslation(0f, 0f, 1f);
        rootNode.attachChild(info);

        optionTextFrames = new Geometry[options.length];
        optionTexts = new BitmapText[options.length];
        optionMinX = new float[options.length];
        optionMaxX = new float[options.length];
        optionMinY = new float[options.length];
        optionMaxY = new float[options.length];

        float optionWidth = menuColumnWidth;
        float optionHeight = 62f;
        float optionX = menuLeft;
        float firstOptionY = titleY - title.getLineHeight() - 220f;
        float optionSpacing = 82f;

        float lastFrameBottomY = firstOptionY;
        for (int i = 0; i < options.length; i++) {
            float y = firstOptionY - i * optionSpacing;

            BitmapText optionText = new BitmapText(font);
            optionText.setSize(font.getCharSet().getRenderedSize() * 1.16f);
            optionText.setColor(OPTION_TEXT_COLOR);
            optionText.setText(options[i]);
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
            Geometry optionTextFrame = createQuad(assetManager, "TournOptFrame_" + i,
                    new Vector2f(textFrameX, textFrameY),
                    new Vector2f(textFrameWidth, textFrameHeight),
                    OPTION_TEXT_FRAME_COLOR, 0.62f);
            optionTextFrames[i] = optionTextFrame;
            rootNode.attachChild(optionTextFrame);

            optionMinX[i] = textFrameX;
            optionMaxX[i] = textFrameX + textFrameWidth;
            optionMinY[i] = textFrameY;
            optionMaxY[i] = textFrameY + textFrameHeight;
            lastFrameBottomY = textFrameY;
        }

        float panelPadding = 28f;
        float panelX = menuLeft - panelPadding;
        float panelBottom = lastFrameBottomY - panelPadding;
        float panelTop = menuTop + title.getLineHeight() * 0.3f + panelPadding;
        float panelWidth = menuColumnWidth + panelPadding * 2f;
        float panelHeight = panelTop - panelBottom;
        Geometry panel = createQuad(assetManager, "TournPanel",
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

    public Node getRootNode() { return rootNode; }
    public int getOptionCount() { return optionTextFrames.length; }

    public void setSelectedIndex(int index) {
        selectedIndex = Math.max(0, Math.min(optionTextFrames.length - 1, index));
        updateSelectionPulse(0f);
    }

    public void updateSelectionPulse(float timer) {
        if (Float.isNaN(timer)) return;
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
            if (x >= optionMinX[i] && x <= optionMaxX[i]
                    && y >= optionMinY[i] && y <= optionMaxY[i]) {
                return i;
            }
        }
        return -1;
    }
}
