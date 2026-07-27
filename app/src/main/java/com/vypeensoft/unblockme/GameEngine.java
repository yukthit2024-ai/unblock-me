package com.vypeensoft.unblockme;

import java.util.List;
import java.util.Stack;

public class GameEngine {
    public static class MoveRecord {
        public Block block;
        public int oldX;
        public int oldY;
        public int newX;
        public int newY;
        public MoveRecord(Block block, int oldX, int oldY, int newX, int newY) {
            this.block = block;
            this.oldX = oldX;
            this.oldY = oldY;
            this.newX = newX;
            this.newY = newY;
        }
    }

    private List<Block> blocks;
    private int moves;
    private static final int GRID_SIZE = 6;
    private boolean isGameOver = false;
    private Stack<MoveRecord> moveHistory = new Stack<>();

    public GameEngine(List<Block> blocks) {
        this.blocks = blocks;
        this.moves = 0;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public int getMoves() {
        return moves;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public Block getBlockAt(int gridX, int gridY) {
        for (Block b : blocks) {
            if (b.contains(gridX, gridY)) return b;
        }
        return null;
    }

    public boolean canMoveTo(Block block, int newX, int newY) {
        if (block.isObstacle) return false;
        if (newX < 0 || newY < 0) return false;
        if (block.isHorizontal) {
            if (newX + block.length > GRID_SIZE) return false;
            if (newY != block.y) return false;
        } else {
            if (newY + block.length > GRID_SIZE) return false;
            if (newX != block.x) return false;
        }

        // Check collision with other blocks
        for (Block other : blocks) {
            if (other == block) continue;
            if (block.isHorizontal) {
                for (int i = 0; i < block.length; i++) {
                    if (other.contains(newX + i, newY)) return false;
                }
            } else {
                for (int i = 0; i < block.length; i++) {
                    if (other.contains(newX, newY + i)) return false;
                }
            }
        }
        return true;
    }

    public boolean moveBlock(Block block, int newX, int newY) {
        if (block.x == newX && block.y == newY) return false;
        if (canMoveTo(block, newX, newY)) {
            moveHistory.push(new MoveRecord(block, block.x, block.y, newX, newY));
            block.x = newX;
            block.y = newY;
            moves++;
            checkWin();
            return true;
        }
        return false;
    }

    public void recordFinalMove(Block block, int oldX, int oldY, int newX, int newY) {
        moveHistory.push(new MoveRecord(block, oldX, oldY, newX, newY));
    }

    public boolean undoMove() {
        if (moveHistory.isEmpty()) return false;
        MoveRecord record = moveHistory.pop();
        record.block.x = record.oldX;
        record.block.y = record.oldY;
        moves--;
        isGameOver = false;
        return true;
    }

    private void checkWin() {
        for (Block b : blocks) {
            if (b.isTarget) {
                if (b.x + b.length == GRID_SIZE) {
                    isGameOver = true;
                }
                break;
            }
        }
    }

    public void setGameOver(boolean over) {
        this.isGameOver = over;
    }

    public boolean isPathClearForTarget() {
        Block target = null;
        for (Block b : blocks) {
            if (b.isTarget) {
                target = b;
                break;
            }
        }
        if (target == null) return false;

        if (target.x + target.length >= GRID_SIZE) return false; // Already at the end

        for (int x = target.x + target.length; x < GRID_SIZE; x++) {
            for (Block other : blocks) {
                if (other == target) continue;
                if (other.contains(x, target.y)) return false;
            }
        }
        return true;
    }

    public List<SolutionManager.SolutionMove> getSolutionPath() {
        List<SolutionManager.SolutionMove> path = new java.util.ArrayList<>();
        for (MoveRecord record : moveHistory) {
            int distance = record.block.isHorizontal ? (record.newX - record.oldX) : (record.newY - record.oldY);
            path.add(new SolutionManager.SolutionMove(record.block.id, distance));
        }
        return path;
    }
}
