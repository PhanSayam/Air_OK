package fr.utln.jmonkey.air_ok.controller.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;
import fr.utln.jmonkey.air_ok.model.Paddle;
import fr.utln.jmonkey.air_ok.model.Player;
import fr.utln.jmonkey.air_ok.model.Puck;
import fr.utln.jmonkey.air_ok.model.Table;
import fr.utln.jmonkey.air_ok.model.rules.ScoreRules;
import fr.utln.jmonkey.air_ok.view.ScoreView;

import java.util.List;

public class GameState extends BaseAppState {

    private final GameMode mode;

    private SimpleApplication simpleApplication;
    private BulletAppState bulletAppState;

    private Puck puck;
    private Table table;
    private List<Paddle> paddles;
    private List<Player> players;

    private ScoreView scoreView;
    private boolean gameFinished;

    public GameState(GameMode mode) {
        this.mode = mode;
    }

    @Override
    protected void initialize(Application app) {
        simpleApplication = (SimpleApplication) app;
        gameFinished = false;

        players = List.of(new Player("Player 1"), new Player("Player 2"));

        scoreView = new ScoreView();
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        scoreView.getContainer().setLocalTranslation(4*screenWidth/5, 4*screenHeight/5, 0);
        updateScoreView();

        bulletAppState = new BulletAppState();
        getStateManager().attach(bulletAppState);

        app.getCamera().setLocation(new Vector3f(0, 30f, 30f));
        app.getCamera().lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);

        table = new Table(simpleApplication.getAssetManager(), simpleApplication.getRootNode(), bulletAppState);
        table.initTable();

        puck = new Puck(simpleApplication.getAssetManager(), simpleApplication.getRootNode(), bulletAppState);
        puck.initPuck();

        paddles = List.of(
                new Paddle(simpleApplication.getAssetManager(), simpleApplication.getRootNode(), bulletAppState),
                new Paddle(simpleApplication.getAssetManager(), simpleApplication.getRootNode(), bulletAppState)
        );
        paddles.forEach(Paddle::initPaddle);
    }

    @Override
    public void update(float tpf) {
        if (gameFinished) {
            return;
        }

        int goalSide = detectGoalSide();
        if (goalSide >= 0) {
            handleGoal(goalSide);
        }
    }

    public void handleGoal(int side) {
        int scorerIndex = switch (side) {
            case 0 -> 1;
            case 1 -> 0;
            default -> throw new IllegalArgumentException("Invalid goal side: " + side);
        };

        applyPointToPlayer(scorerIndex);
        updateScoreView();

        if (ScoreRules.hasWon(players.get(scorerIndex).getScore())) {
            gameFinished = true;
            getStateManager().detach(this);
            getStateManager().attach(new EndScreenState());
            return;
        }

        resetRound();
    }

    public void handleOwnGoal(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= players.size()) {
            throw new IllegalArgumentException("Invalid player index: " + playerIndex);
        }

        Player player = players.get(playerIndex);
        player.setScore(ScoreRules.ownGoal(player.getScore()));
        updateScoreView();
        resetRound();
    }

    private void applyPointToPlayer(int playerIndex) {
        Player player = players.get(playerIndex);
        player.setScore(ScoreRules.addPointTo(player.getScore()));
    }

    private int detectGoalSide() {
        return -1;
    }

    private void resetRound() {
    }

    private void updateScoreView() {
        scoreView.getScoreP1().setText(players.get(0).getName() + ": " + players.get(0).getScore());
        scoreView.getScoreP2().setText(players.get(1).getName() + ": " + players.get(1).getScore());
    }

    @Override
    protected void cleanup(Application application) {
        onDisable();
        if (bulletAppState != null) {
            getStateManager().detach(bulletAppState);
            bulletAppState = null;
        }
    }

    @Override
    protected void onEnable() {
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(scoreView.getContainer());
    }

    @Override
    protected void onDisable() {
        if (scoreView != null && scoreView.getContainer() != null) {
            scoreView.getContainer().removeFromParent();
        }
    }
}