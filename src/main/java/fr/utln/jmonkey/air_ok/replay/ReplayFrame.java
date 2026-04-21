package fr.utln.jmonkey.air_ok.replay;

import com.jme3.math.Vector3f;

public final class ReplayFrame {
    public final float    timestamp;    // absolute game time (seconds) when frame was recorded
    public final Vector3f puckPosition;
    public final Vector3f p1Position;
    public final Vector3f p2Position;

    public ReplayFrame(float timestamp, Vector3f puckPos, Vector3f p1Pos, Vector3f p2Pos) {
        this.timestamp    = timestamp;
        this.puckPosition = puckPos.clone();
        this.p1Position   = p1Pos.clone();
        this.p2Position   = p2Pos.clone();
    }
}
