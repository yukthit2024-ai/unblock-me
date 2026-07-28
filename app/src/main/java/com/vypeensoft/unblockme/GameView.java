package com.vypeensoft.unblockme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.animation.ValueAnimator;
import android.animation.AnimatorListenerAdapter;
import android.animation.Animator;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

public class GameView extends View {
    private GameEngine engine;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float cellSize;
    private Block selectedBlock = null;
    private float lastTouchX, lastTouchY;
    private OnGameListener listener;

    private boolean isAnimatingWin = false;
    private float animatedTargetX = -1f;

    private boolean isPlayingSolution = false;
    private Block animatedSolutionBlock = null;
    private float solutionAnimatedX = -1f;
    private float solutionAnimatedY = -1f;

    private Bitmap bmpTarget;
    private Bitmap bmpWoodH2;
    private Bitmap bmpWoodH3;
    private Bitmap bmpWoodV2;
    private Bitmap bmpWoodV3;
    private Bitmap bmpObstacle;

    public interface OnGameListener {
        void onMove();
        void onWin();
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        loadBitmaps(context);
    }

    private void loadBitmaps(Context context) {
        try {
            bmpTarget = BitmapFactory.decodeStream(context.getAssets().open("tiles/wood/red_block_1.png"));
            bmpWoodH2 = BitmapFactory.decodeStream(context.getAssets().open("tiles/wood/wood_block_h2.png"));
            bmpWoodH3 = BitmapFactory.decodeStream(context.getAssets().open("tiles/wood/wood_block_h3.png"));
            bmpWoodV2 = BitmapFactory.decodeStream(context.getAssets().open("tiles/wood/wood_block_v2.png"));
            bmpWoodV3 = BitmapFactory.decodeStream(context.getAssets().open("tiles/wood/wood_block_v3.png"));
            bmpObstacle = BitmapFactory.decodeStream(context.getAssets().open("tiles/wood/obstacle.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setEngine(GameEngine engine) {
        this.engine = engine;
        invalidate();
    }

    public void setOnGameListener(OnGameListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cellSize = Math.min(w, h) / 6.0f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (engine == null) return;

        // Draw grid lines to reveal the background
        paint.setColor(Color.parseColor("#40000000")); // semi-transparent black
        paint.setStrokeWidth(2);
        for (int i = 1; i < 6; i++) {
            canvas.drawLine(i * cellSize, 0, i * cellSize, getHeight(), paint);
            canvas.drawLine(0, i * cellSize, getWidth(), i * cellSize, paint);
        }

        // Draw board border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(Color.parseColor("#8D6E63"));
        canvas.drawRect(0, 0, 6 * cellSize, 6 * cellSize, paint);
        paint.setStyle(Paint.Style.FILL);

        // Draw exit indicator
        paint.setColor(Color.RED);
        paint.setAlpha(80);
        canvas.drawRect(5 * cellSize, 2 * cellSize, 6 * cellSize, 3 * cellSize, paint);
        paint.setAlpha(255);

        // Draw blocks
        for (Block b : engine.getBlocks()) {
            float blockX = b.x;
            float blockY = b.y;

            if (b == animatedSolutionBlock && isPlayingSolution) {
                blockX = solutionAnimatedX;
                blockY = solutionAnimatedY;
            } else if (b.isTarget && isAnimatingWin && animatedTargetX >= 0) {
                blockX = animatedTargetX;
            }

            float left = blockX * cellSize + 8;
            float top = blockY * cellSize + 8;
            float right = (b.isHorizontal ? blockX + b.length : blockX + 1) * cellSize - 8;
            float bottom = (b.isHorizontal ? blockY + 1 : blockY + b.length) * cellSize - 8;
            
            RectF rect = new RectF(left, top, right, bottom);

            Bitmap bmpToDraw = null;
            if (b.isTarget) {
                bmpToDraw = bmpTarget;
            } else if (b.isObstacle) {
                bmpToDraw = bmpObstacle;
            } else if (b.isHorizontal) {
                bmpToDraw = (b.length == 3) ? bmpWoodH3 : bmpWoodH2;
            } else {
                bmpToDraw = (b.length == 3) ? bmpWoodV3 : bmpWoodV2;
            }

            if (bmpToDraw != null) {
                if (b.isObstacle) {
                    for (int i = 0; i < b.length; i++) {
                        float cellLeft = (blockX + (b.isHorizontal ? i : 0)) * cellSize + 8;
                        float cellTop = (blockY + (b.isHorizontal ? 0 : i)) * cellSize + 8;
                        float cellRight = (blockX + (b.isHorizontal ? i + 1 : 1)) * cellSize - 8;
                        float cellBottom = (blockY + (b.isHorizontal ? 1 : i + 1)) * cellSize - 8;
                        RectF cellRect = new RectF(cellLeft, cellTop, cellRight, cellBottom);
                        canvas.drawBitmap(bmpToDraw, null, cellRect, paint);
                    }
                } else {
                    canvas.drawBitmap(bmpToDraw, null, rect, paint);
                }
            } else {
                // Fallback
                paint.setColor(b.color);
                canvas.drawRoundRect(rect, 20, 20, paint);
            }

            // Draw highlight for selected
            if (b == selectedBlock) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(8);
                paint.setColor(Color.YELLOW);
                canvas.drawRoundRect(rect, 20, 20, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (engine == null || isAnimatingWin || isPlayingSolution) return false;
        
        // If game is already over and they try a new touch, ignore
        if (engine.isGameOver() && event.getAction() == MotionEvent.ACTION_DOWN) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        int gridX = (int) (x / cellSize);
        int gridY = (int) (y / cellSize);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                selectedBlock = engine.getBlockAt(gridX, gridY);
                lastTouchX = x;
                lastTouchY = y;
                invalidate();
                return selectedBlock != null;

            case MotionEvent.ACTION_MOVE:
                if (selectedBlock != null && !engine.isGameOver()) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;

                    if (selectedBlock.isHorizontal) {
                        if (Math.abs(dx) >= cellSize) {
                            int direction = dx > 0 ? 1 : -1;
                            if (engine.moveBlock(selectedBlock, selectedBlock.x + direction, selectedBlock.y)) {
                                lastTouchX += direction * cellSize;
                                if (listener != null) listener.onMove();
                                if (engine.isPathClearForTarget()) {
                                    startWinAnimation();
                                    selectedBlock = null;
                                } else if (engine.isGameOver() && listener != null) {
                                    listener.onWin();
                                    selectedBlock = null;
                                }
                            }
                        }
                    } else {
                        if (Math.abs(dy) >= cellSize) {
                            int direction = dy > 0 ? 1 : -1;
                            if (engine.moveBlock(selectedBlock, selectedBlock.x, selectedBlock.y + direction)) {
                                lastTouchY += direction * cellSize;
                                if (listener != null) listener.onMove();
                                if (engine.isPathClearForTarget()) {
                                    startWinAnimation();
                                    selectedBlock = null;
                                } else if (engine.isGameOver() && listener != null) {
                                    listener.onWin();
                                    selectedBlock = null;
                                }
                            }
                        }
                    }
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                selectedBlock = null;
                invalidate();
                break;
        }
        return true;
    }

    private void startWinAnimation() {
        isAnimatingWin = true;
        Block target = null;
        for (Block b : engine.getBlocks()) {
            if (b.isTarget) {
                target = b;
                break;
            }
        }
        if (target == null) return;
        
        final Block finalTarget = target;
        float startX = target.x;
        float endX = 6f - target.length;
        
        ValueAnimator animator = ValueAnimator.ofFloat(startX, endX);
        animator.setDuration(400);
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animatedTargetX = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                engine.recordFinalMove(finalTarget, (int)startX, finalTarget.y, (int)endX, finalTarget.y);
                finalTarget.x = (int) endX;
                engine.setGameOver(true);
                isAnimatingWin = false;
                if (listener != null) {
                    listener.onWin();
                }
            }
        });
        animator.start();
    }

    public void playSolution(List<SolutionManager.SolutionMove> moves) {
        if (engine == null || moves == null || moves.isEmpty()) return;
        isPlayingSolution = true;
        animateNextSolutionMove(0, moves);
    }

    private void animateNextSolutionMove(int index, List<SolutionManager.SolutionMove> moves) {
        if (index >= moves.size() || engine == null) {
            isPlayingSolution = false;
            animatedSolutionBlock = null;
            invalidate();
            return;
        }

        SolutionManager.SolutionMove move = moves.get(index);
        Block blockToMove = null;
        for (Block b : engine.getBlocks()) {
            if (b.id.equals(move.car)) {
                blockToMove = b;
                break;
            }
        }

        if (blockToMove == null) {
            animateNextSolutionMove(index + 1, moves);
            return;
        }

        final Block finalBlock = blockToMove;
        float startVal = finalBlock.isHorizontal ? finalBlock.x : finalBlock.y;
        float endVal = startVal + move.distance;

        animatedSolutionBlock = finalBlock;
        if (finalBlock.isHorizontal) {
            solutionAnimatedX = startVal;
            solutionAnimatedY = finalBlock.y;
        } else {
            solutionAnimatedX = finalBlock.x;
            solutionAnimatedY = startVal;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(startVal, endVal);
        animator.setDuration(250 * Math.abs(move.distance));
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            if (finalBlock.isHorizontal) {
                solutionAnimatedX = val;
            } else {
                solutionAnimatedY = val;
            }
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Force update block coordinates
                if (finalBlock.isHorizontal) {
                    finalBlock.x = (int) endVal;
                } else {
                    finalBlock.y = (int) endVal;
                }
                
                // If this move clears the path, trigger win automatically?
                // The solution should ideally just move the red block out.
                if (engine.isPathClearForTarget() && finalBlock.isTarget && finalBlock.x + finalBlock.length == 6) {
                     engine.setGameOver(true);
                     if (listener != null) listener.onWin();
                     isPlayingSolution = false;
                     animatedSolutionBlock = null;
                     return;
                }

                postDelayed(() -> animateNextSolutionMove(index + 1, moves), 150);
            }
        });
        animator.start();
    }
}
