package com.thatgravyboat.skyblockhud.core.util.lerp;

public class LerpUtils {

  public static float clampZeroOne(float f) {
    return Math.max(0, Math.min(1, f));
  }

  public static float sigmoid(float val) {
    return (float) (1 / (1 + Math.exp(-val)));
  }

  private static final float sigmoidStr = 8;
  private static final float sigmoidA =
    -1 / (sigmoid(-0.5f * sigmoidStr) - sigmoid(0.5f * sigmoidStr));
  private static final float sigmoidB = sigmoidA * sigmoid(-0.5f * sigmoidStr);

  public static float sigmoidZeroOne(float f) {
    f = clampZeroOne(f);
    return sigmoidA * sigmoid(sigmoidStr * (f - 0.5f)) - sigmoidB;
  }

  public static float lerp(float a, float b, float amount) {
    return b + (a - b) * clampZeroOne(amount);
  }
}
