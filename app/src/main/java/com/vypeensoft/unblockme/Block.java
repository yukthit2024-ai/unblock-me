package com.vypeensoft.unblockme;

import android.graphics.Color;

public class Block {
    public String id;
    public int x, y; // Top-left position
    public int length;
    public boolean isHorizontal;
    public int color;
    public boolean isTarget;
    public boolean isObstacle;

    public Block(String id, int x, int y, int length, boolean isHorizontal, int color, boolean isTarget, boolean isObstacle) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.length = length;
        this.isHorizontal = isHorizontal;
        this.color = color;
        this.isTarget = isTarget;
        this.isObstacle = isObstacle;
    }

    public boolean contains(int gridX, int gridY) {
        if (isHorizontal) {
            return gridY == y && gridX >= x && gridX < x + length;
        } else {
            return gridX == x && gridY >= y && gridY < y + length;
        }
    }

    public Block copy() {
        return new Block(id, x, y, length, isHorizontal, color, isTarget, isObstacle);
    }
}
