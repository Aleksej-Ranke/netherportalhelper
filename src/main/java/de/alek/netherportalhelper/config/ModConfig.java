package de.alek.netherportalhelper.config;

public class ModConfig {

    public boolean hudVisible = true;
    public int hudPanelX = 6;
    public int hudPanelY = 6;

    public int compassCenterY = 56;
    public float compassArrowScale = 3.0f;
    public float compassTextScale = 1.0f;
    public float compassSmoothing = 0.28f;
    public float compassBackzoneDegrees = 165.0f;
    public boolean showTurnAroundHint = true;
    public boolean showYLevelIndicator = true;

    public void normalize() {
        hudPanelX = Math.max(0, hudPanelX);
        hudPanelY = Math.max(0, hudPanelY);
        compassCenterY = clampInt(compassCenterY, 20, 400);
        compassArrowScale = clampFloat(compassArrowScale, 1.0f, 6.0f);
        compassTextScale = clampFloat(compassTextScale, 0.6f, 2.5f);
        compassSmoothing = clampFloat(compassSmoothing, 0.0f, 1.0f);
        compassBackzoneDegrees = clampFloat(compassBackzoneDegrees, 90.0f, 179.0f);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
