package com.thatgravyboat.skyblockhud.core.util.lerp;

public class LerpingFloat {

    private int timeSpent;
    private long lastMillis;
    private int timeToReachTarget;

    private float targetValue;
    private float lerpValue;

    public LerpingFloat(float initialValue, int timeToReachTarget) {
        this.targetValue = this.lerpValue = initialValue;
        this.timeToReachTarget = timeToReachTarget;
    }

    public LerpingFloat(int initialValue) {
        this(initialValue, 200);
    }

    public void tick() {
        int lastTimeSpent = timeSpent;
        this.timeSpent += System.currentTimeMillis() - lastMillis;

        float lastDistPercentToTarget = lastTimeSpent / (float) timeToReachTarget;
        float distPercentToTarget = timeSpent / (float) timeToReachTarget;
        float fac = (1 - lastDistPercentToTarget) / lastDistPercentToTarget;

        float startValue = lerpValue - (targetValue - lerpValue) / fac;

        float dist = targetValue - startValue;
        if (dist == 0) return;

        float oldLerpValue = lerpValue;
        if (distPercentToTarget >= 1) {
            lerpValue = targetValue;
        } else {
            lerpValue = startValue + dist * distPercentToTarget;
        }

        if (lerpValue == oldLerpValue) {
            timeSpent = lastTimeSpent;
        } else {
            this.lastMillis = System.currentTimeMillis();
        }
    }

    public void resetTimer() {
        this.timeSpent = 0;
        this.lastMillis = System.currentTimeMillis();
    }

    public void setTarget(float targetValue) {
        this.targetValue = targetValue;
    }

    public void setValue(float value) {
        this.targetValue = this.lerpValue = value;
    }

    public float getValue() {
        return lerpValue;
    }

    public float getTarget() {
        return targetValue;
    }
}
