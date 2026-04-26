package fr.utln.jmonkey.air_ok.controller.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;

import fr.utln.jmonkey.air_ok.view.EndScreenView;

public class EndScreenState extends BaseAppState {

    private static final String END_UP = "EndMenuUp";
    private static final String END_DOWN = "EndMenuDown";
    private static final String END_SELECT = "EndMenuSelect";
    private static final String END_CLICK = "EndMenuClick";
    private static final String END_RETURN_SHORTCUT = "EndMenuReturnShortcut";
    private static final String END_QUIT_SHORTCUT = "EndMenuQuitShortcut";

    private SimpleApplication app;
    private EndScreenView view;
    private final String resultText;
    private int selectedIndex;
    private float selectionPulseTimer;
    private ColorRGBA previousBackgroundColor;

    private final ActionListener inputListener = (name, isPressed, tpf) -> {
        if (!isPressed) {
            return;
        }

        if (END_UP.equals(name)) {
            moveSelection(-1);
        } else if (END_DOWN.equals(name)) {
            moveSelection(1);
        } else if (END_SELECT.equals(name)) {
            activateSelection(selectedIndex);
        } else if (END_CLICK.equals(name)) {
            int hovered = view.pickOption(app.getInputManager().getCursorPosition());
            if (hovered >= 0) {
                selectedIndex = hovered;
                view.setSelectedIndex(selectedIndex);
                activateSelection(selectedIndex);
            }
        } else if (END_RETURN_SHORTCUT.equals(name)) {
            activateSelection(0);
        } else if (END_QUIT_SHORTCUT.equals(name)) {
            activateSelection(1);
        }
    };

    public EndScreenState() {
        this(null);
    }

    public EndScreenState(String resultText) {
        this.resultText = resultText;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        view = new EndScreenView(app.getAssetManager(), app.getCamera().getWidth(), app.getCamera().getHeight());

        if (resultText != null && !resultText.isBlank()) {
            view.setScoresText(resultText);
        }

        selectedIndex = 0;
        selectionPulseTimer = 0f;
        view.setSelectedIndex(selectedIndex);
        registerInputMappings();
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        updateHoverSelection();
        selectionPulseTimer += tpf;
        view.updateSelectionPulse(selectionPulseTimer);
    }

    private void moveSelection(int delta) {
        int count = view.getOptionCount();
        selectedIndex = (selectedIndex + delta + count) % count;
        view.setSelectedIndex(selectedIndex);
    }

    private void updateHoverSelection() {
        int hovered = view.pickOption(app.getInputManager().getCursorPosition());
        if (hovered >= 0 && hovered != selectedIndex) {
            selectedIndex = hovered;
            view.setSelectedIndex(selectedIndex);
        }
    }

    private void activateSelection(int optionIndex) {
        switch (optionIndex) {
            case 0 -> returnToMenu();
            case 1 -> app.stop();
            default -> {
                // Ignore invalid option.
            }
        }
    }

    private void returnToMenu() {
        getStateManager().detach(this);
        getStateManager().attach(new MainMenuState());
    }

    private void registerInputMappings() {
        if (!app.getInputManager().hasMapping(END_UP)) {
            app.getInputManager().addMapping(END_UP,
                    new KeyTrigger(KeyInput.KEY_UP),
                    new KeyTrigger(KeyInput.KEY_Z),
                    new KeyTrigger(KeyInput.KEY_W));
        }
        if (!app.getInputManager().hasMapping(END_DOWN)) {
            app.getInputManager().addMapping(END_DOWN,
                    new KeyTrigger(KeyInput.KEY_DOWN),
                    new KeyTrigger(KeyInput.KEY_S));
        }
        if (!app.getInputManager().hasMapping(END_SELECT)) {
            app.getInputManager().addMapping(END_SELECT,
                    new KeyTrigger(KeyInput.KEY_RETURN),
                    new KeyTrigger(KeyInput.KEY_SPACE));
        }
        if (!app.getInputManager().hasMapping(END_CLICK)) {
            app.getInputManager().addMapping(END_CLICK,
                    new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        }
        if (!app.getInputManager().hasMapping(END_RETURN_SHORTCUT)) {
            app.getInputManager().addMapping(END_RETURN_SHORTCUT, new KeyTrigger(KeyInput.KEY_M));
        }
        if (!app.getInputManager().hasMapping(END_QUIT_SHORTCUT)) {
            app.getInputManager().addMapping(END_QUIT_SHORTCUT, new KeyTrigger(KeyInput.KEY_ESCAPE));
        }
    }

    @Override
    protected void onEnable() {
        previousBackgroundColor = app.getViewPort().getBackgroundColor().clone();
        app.getViewPort().setBackgroundColor(ColorRGBA.Black);
        app.getGuiNode().attachChild(view.getRootNode());
        app.getInputManager().setCursorVisible(true);
        app.getInputManager().addListener(
                inputListener,
                END_UP,
                END_DOWN,
                END_SELECT,
                END_CLICK,
                END_RETURN_SHORTCUT,
                END_QUIT_SHORTCUT);
    }

    @Override
    protected void onDisable() {
        if (previousBackgroundColor != null) {
            app.getViewPort().setBackgroundColor(previousBackgroundColor);
        }
        view.getRootNode().removeFromParent();
        app.getInputManager().removeListener(inputListener);
    }

    @Override
    protected void cleanup(Application app) {
        if (this.app.getInputManager().hasMapping(END_UP)) {
            this.app.getInputManager().deleteMapping(END_UP);
        }
        if (this.app.getInputManager().hasMapping(END_DOWN)) {
            this.app.getInputManager().deleteMapping(END_DOWN);
        }
        if (this.app.getInputManager().hasMapping(END_SELECT)) {
            this.app.getInputManager().deleteMapping(END_SELECT);
        }
        if (this.app.getInputManager().hasMapping(END_CLICK)) {
            this.app.getInputManager().deleteMapping(END_CLICK);
        }
        if (this.app.getInputManager().hasMapping(END_RETURN_SHORTCUT)) {
            this.app.getInputManager().deleteMapping(END_RETURN_SHORTCUT);
        }
        if (this.app.getInputManager().hasMapping(END_QUIT_SHORTCUT)) {
            this.app.getInputManager().deleteMapping(END_QUIT_SHORTCUT);
        }
    }
}