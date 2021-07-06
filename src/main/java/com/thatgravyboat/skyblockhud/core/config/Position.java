package com.thatgravyboat.skyblockhud.core.config;

import com.google.gson.annotations.Expose;
import net.minecraft.client.gui.ScaledResolution;

public class Position {

    @Expose
    private int x;

    @Expose
    private int y;

    @Expose
    private boolean centerX;

    @Expose
    private boolean centerY;

    private static final int EDGE_OFFSET = 0;

    public Position(int x, int y) {
        this(x, y, false, false);
    }

    public Position(int x, int y, boolean centerX, boolean centerY) {
        this.x = x;
        this.y = y;
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public void set(Position other) {
        this.x = other.x;
        this.y = other.y;
        this.centerX = other.centerX;
        this.centerY = other.centerY;
    }

    public Position clone() {
        return new Position(x, y, centerX, centerY);
    }

    public boolean isCenterX() {
        return centerX;
    }

    public boolean isCenterY() {
        return centerY;
    }

    public int getRawX() {
        return x;
    }

    public int getRawY() {
        return y;
    }

    public int getAbsX(ScaledResolution scaledResolution, int objWidth) {
        int width = scaledResolution.getScaledWidth();

        if (centerX) {
            return width / 2 + x;
        }

        int ret = x;
        if (x < 0) {
            ret = width + x - objWidth;
        }

        if (ret < 0) ret = 0;
        if (ret > width - objWidth) ret = width - objWidth;

        return ret;
    }

    public int getAbsY(ScaledResolution scaledResolution, int objHeight) {
        int height = scaledResolution.getScaledHeight();

        if (centerY) {
            return height / 2 + y;
        }

        int ret = y;
        if (y < 0) {
            ret = height + y - objHeight;
        }

        if (ret < 0) ret = 0;
        if (ret > height - objHeight) ret = height - objHeight;

        return ret;
    }

    public int moveX(int deltaX, int objWidth, ScaledResolution scaledResolution) {
        int screenWidth = scaledResolution.getScaledWidth();
        boolean wasPositiveX = this.x >= 0;
        this.x += deltaX;

        if (centerX) {
            if (wasPositiveX) {
                if (this.x > screenWidth / 2 - objWidth / 2) {
                    deltaX += screenWidth / 2 - objWidth / 2 - this.x;
                    this.x = screenWidth / 2 - objWidth / 2;
                }
            } else {
                if (this.x < -screenWidth / 2 + objWidth / 2) {
                    deltaX += -screenWidth / 2 + objWidth / 2 - this.x;
                    this.x = -screenWidth / 2 + objWidth / 2;
                }
            }
            return deltaX;
        }

        if (wasPositiveX) {
            if (this.x < EDGE_OFFSET) {
                deltaX += EDGE_OFFSET - this.x;
                this.x = EDGE_OFFSET;
            }
            if (this.x > screenWidth - EDGE_OFFSET) {
                deltaX += screenWidth - EDGE_OFFSET - this.x;
                this.x = screenWidth - EDGE_OFFSET;
            }
        } else {
            if (this.x + 1 > -EDGE_OFFSET) {
                deltaX += -EDGE_OFFSET - 1 - this.x;
                this.x = -EDGE_OFFSET - 1;
            }
            if (this.x + screenWidth < EDGE_OFFSET) {
                deltaX += EDGE_OFFSET - screenWidth - this.x;
                this.x = EDGE_OFFSET - screenWidth;
            }
        }

        if (this.x >= 0 && this.x + objWidth / 2 > screenWidth / 2) {
            this.x -= screenWidth - objWidth;
        }
        if (this.x < 0 && this.x + objWidth / 2 <= -screenWidth / 2) {
            this.x += screenWidth - objWidth;
        }
        return deltaX;
    }

    public int moveY(int deltaY, int objHeight, ScaledResolution scaledResolution) {
        int screenHeight = scaledResolution.getScaledHeight();
        boolean wasPositiveY = this.y >= 0;
        this.y += deltaY;

        if (centerY) {
            if (wasPositiveY) {
                if (this.y > screenHeight / 2 - objHeight / 2) {
                    deltaY += screenHeight / 2 - objHeight / 2 - this.y;
                    this.y = screenHeight / 2 - objHeight / 2;
                }
            } else {
                if (this.y < -screenHeight / 2 + objHeight / 2) {
                    deltaY += -screenHeight / 2 + objHeight / 2 - this.y;
                    this.y = -screenHeight / 2 + objHeight / 2;
                }
            }
            return deltaY;
        }

        if (wasPositiveY) {
            if (this.y < EDGE_OFFSET) {
                deltaY += EDGE_OFFSET - this.y;
                this.y = EDGE_OFFSET;
            }
            if (this.y > screenHeight - EDGE_OFFSET) {
                deltaY += screenHeight - EDGE_OFFSET - this.y;
                this.y = screenHeight - EDGE_OFFSET;
            }
        } else {
            if (this.y + 1 > -EDGE_OFFSET) {
                deltaY += -EDGE_OFFSET - 1 - this.y;
                this.y = -EDGE_OFFSET - 1;
            }
            if (this.y + screenHeight < EDGE_OFFSET) {
                deltaY += EDGE_OFFSET - screenHeight - this.y;
                this.y = EDGE_OFFSET - screenHeight;
            }
        }

        if (this.y >= 0 && this.y - objHeight / 2 > screenHeight / 2) {
            this.y -= screenHeight - objHeight;
        }
        if (this.y < 0 && this.y - objHeight / 2 <= -screenHeight / 2) {
            this.y += screenHeight - objHeight;
        }
        return deltaY;
    }

    public boolean rightAligned(ScaledResolution scaledResolution, int objWidth) {
        return (this.getAbsX(scaledResolution, objWidth) > (scaledResolution.getScaledWidth() / 2));
    }
}
