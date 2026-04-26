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

import fr.utln.jmonkey.air_ok.model.TournamentManager;
import fr.utln.jmonkey.air_ok.view.TournamentInterstitialView;

/**
 * Displayed between tournament rounds (win/loss result + navigation options).
 */
public class TournamentInterstitialState extends BaseAppState {

    private static final String T_UP     = "TournIUp";
    private static final String T_DOWN   = "TournIDown";
    private static final String T_SELECT = "TournISelect";
    private static final String T_CLICK  = "TournIClick";
    private static final String T_ESC    = "TournIEsc";

    private SimpleApplication app;
    private TournamentInterstitialView view;

    private final TournamentManager tournament;
    private final boolean playerWon;
    private final String matchSummary;

    private int selectedIndex;
    private float selectionPulseTimer;
    private ColorRGBA previousBackgroundColor;

    private final ActionListener inputListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        switch (name) {
            case T_UP     -> moveSelection(-1);
            case T_DOWN   -> moveSelection(1);
            case T_SELECT -> activateSelection(selectedIndex);
            case T_CLICK  -> {
                int h = view.pickOption(app.getInputManager().getCursorPosition());
                if (h >= 0) { selectedIndex = h; view.setSelectedIndex(h); activateSelection(h); }
            }
            case T_ESC    -> activateSelection(view.getOptionCount() - 1);
        }
    };

    public TournamentInterstitialState(TournamentManager tournament, boolean playerWon, String matchSummary) {
        this.tournament = tournament;
        this.playerWon = playerWon;
        this.matchSummary = matchSummary;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;

        String titleText;
        ColorRGBA titleColor;
        String infoText;
        String[] options;

        if (!playerWon) {
            titleText  = "Defaite...";
            titleColor = new ColorRGBA(0.95f, 0.35f, 0.25f, 1f);
            infoText   = matchSummary + "\n\nVous avez ete elimine du tournoi.";
            options    = new String[]{"Retour au menu", "Quitter"};
        } else if (tournament.isTournamentComplete()) {
            titleText  = "CHAMPION !";
            titleColor = new ColorRGBA(1f, 0.85f, 0.20f, 1f);
            infoText   = matchSummary + "\n\nVous avez vaincu les " + TournamentManager.TOTAL_ROUNDS
                        + " adversaires du tournoi !";
            options    = new String[]{"Retour au menu", "Quitter"};
        } else {
            String nextName = tournament.getCurrentOpponentName();
            titleText  = "Victoire !";
            titleColor = new ColorRGBA(0.40f, 0.90f, 0.45f, 1f);
            infoText   = matchSummary + "\n\nProchain adversaire : " + nextName
                        + "  (Tour " + (tournament.getCurrentRound() + 1)
                        + "/" + TournamentManager.TOTAL_ROUNDS + ")";
            options    = new String[]{"Prochain adversaire", "Abandonner le tournoi"};
        }

        view = new TournamentInterstitialView(app.getAssetManager(),
                app.getCamera().getWidth(), app.getCamera().getHeight(),
                titleText, titleColor, infoText, options);

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
        int h = view.pickOption(app.getInputManager().getCursorPosition());
        if (h >= 0 && h != selectedIndex) {
            selectedIndex = h;
            view.setSelectedIndex(h);
        }
    }

    private void activateSelection(int optionIndex) {
        if (!playerWon || tournament.isTournamentComplete()) {
            // Only "return to menu" or "quit"
            if (optionIndex == 0) returnToMenu();
            else                  app.stop();
        } else {
            // Won, more rounds ahead
            if (optionIndex == 0) startNextRound();
            else                  returnToMenu();
        }
    }

    private void startNextRound() {
        GameState.GameConfig cfg = new GameState.GameConfig(GameState.GameMode.SINGLE_PLAYER);
        cfg.tournament         = tournament;
        cfg.aiSpeedMultiplier  = tournament.getCurrentAiSpeedMultiplier();
        cfg.aiReactionDelay    = tournament.getCurrentAiReactionDelay();
        cfg.opponentName       = tournament.getCurrentOpponentName();
        cfg.playerOneName      = "Joueur 1";

        getStateManager().detach(this);
        getStateManager().attach(new GameState(cfg));
    }

    private void returnToMenu() {
        getStateManager().detach(this);
        getStateManager().attach(new MainMenuState());
    }

    @Override
    protected void onEnable() {
        previousBackgroundColor = app.getViewPort().getBackgroundColor().clone();
        app.getViewPort().setBackgroundColor(ColorRGBA.Black);
        app.getGuiNode().attachChild(view.getRootNode());
        app.getInputManager().setCursorVisible(true);
        app.getInputManager().addListener(inputListener, T_UP, T_DOWN, T_SELECT, T_CLICK, T_ESC);
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
        deleteIfPresent(T_UP);
        deleteIfPresent(T_DOWN);
        deleteIfPresent(T_SELECT);
        deleteIfPresent(T_CLICK);
        deleteIfPresent(T_ESC);
    }

    private void deleteIfPresent(String mapping) {
        if (app.getInputManager().hasMapping(mapping)) {
            app.getInputManager().deleteMapping(mapping);
        }
    }

    private void registerInputMappings() {
        if (!app.getInputManager().hasMapping(T_UP)) {
            app.getInputManager().addMapping(T_UP,
                    new KeyTrigger(KeyInput.KEY_UP),
                    new KeyTrigger(KeyInput.KEY_Z),
                    new KeyTrigger(KeyInput.KEY_W));
        }
        if (!app.getInputManager().hasMapping(T_DOWN)) {
            app.getInputManager().addMapping(T_DOWN,
                    new KeyTrigger(KeyInput.KEY_DOWN),
                    new KeyTrigger(KeyInput.KEY_S));
        }
        if (!app.getInputManager().hasMapping(T_SELECT)) {
            app.getInputManager().addMapping(T_SELECT,
                    new KeyTrigger(KeyInput.KEY_RETURN),
                    new KeyTrigger(KeyInput.KEY_SPACE));
        }
        if (!app.getInputManager().hasMapping(T_CLICK)) {
            app.getInputManager().addMapping(T_CLICK,
                    new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        }
        if (!app.getInputManager().hasMapping(T_ESC)) {
            app.getInputManager().addMapping(T_ESC, new KeyTrigger(KeyInput.KEY_ESCAPE));
        }
    }
}
