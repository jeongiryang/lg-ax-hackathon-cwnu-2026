package com.cwnu.kitchenmood.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/** A rotating record drawn entirely on the device. */
public final class RadioDialView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private int accentColor = Color.rgb(223, 209, 137);
    private boolean playing;
    private boolean attached;
    private boolean animating;
    private float rotation;
    private long lastFrame;
    private final Runnable animation = new Runnable() {
        @Override public void run() {
            if (!animating) return;
            long now = SystemClock.uptimeMillis();
            if (lastFrame != 0L) rotation = (rotation + Math.min(100L, now - lastFrame) * 0.014f) % 360f;
            lastFrame = now;
            invalidate();
            postOnAnimation(this);
        }
    };

    public RadioDialView(Context context) { super(context); }
    public RadioDialView(Context context, AttributeSet attributes) { super(context, attributes); }

    public void setAccentColor(int color) {
        if (accentColor != color) { accentColor = color; invalidate(); }
    }

    public void setPlaying(boolean value) {
        playing = value;
        syncAnimation();
    }

    private void syncAnimation() {
        boolean enabled = playing && attached && ValueAnimator.areAnimatorsEnabled();
        if (animating == enabled) return;
        animating = enabled;
        removeCallbacks(animation);
        lastFrame = 0L;
        if (animating) postOnAnimation(animation);
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        syncAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        syncAnimation();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() * 0.5f;
        float cy = getHeight() * 0.5f;
        float radius = Math.min(getWidth(), getHeight()) * 0.42f;
        if (radius <= 0f) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(40, 0, 0, 0));
        canvas.drawCircle(cx, cy + radius * 0.055f, radius * 1.05f, paint);
        paint.setColor(Color.rgb(16, 26, 22));
        canvas.drawCircle(cx, cy, radius, paint);
        canvas.save();
        canvas.rotate(rotation, cx, cy);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, radius * 0.009f));
        for (int i = 0; i < 13; i++) {
            paint.setColor(i % 2 == 0 ? Color.rgb(48, 60, 50) : Color.rgb(34, 45, 37));
            canvas.drawCircle(cx, cy, radius * (0.43f + i * 0.039f), paint);
        }
        paint.setStrokeWidth(radius * 0.09f);
        paint.setColor(Color.argb(13, 247, 242, 228));
        arcBounds.set(cx - radius * 0.78f, cy - radius * 0.78f, cx + radius * 0.78f, cy + radius * 0.78f);
        canvas.drawArc(arcBounds, -55f, 36f, false, paint);
        canvas.drawArc(arcBounds, 125f, 36f, false, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accentColor);
        canvas.drawCircle(cx, cy, radius * 0.355f, paint);
        paint.setColor(Color.rgb(37, 53, 43));
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(radius * 0.113f);
        canvas.drawText("KITCHEN", cx, cy - radius * 0.095f, paint);
        paint.setTextSize(radius * 0.081f);
        canvas.drawText("MOOD RADIO", cx, cy + radius * 0.145f, paint);
        paint.setColor(Color.rgb(20, 35, 28));
        canvas.drawCircle(cx, cy, radius * 0.045f, paint);
        paint.setColor(Color.rgb(220, 225, 205));
        canvas.drawCircle(cx, cy, radius * 0.014f, paint);
        canvas.restore();
        // The stationary arm anchors the movement visually.
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.038f);
        paint.setColor(Color.rgb(185, 193, 169));
        float pivotX = cx + radius * 0.94f;
        float pivotY = cy - radius * 0.86f;
        canvas.drawLine(pivotX, pivotY, cx + radius * 0.77f, cy + radius * 0.1f, paint);
        canvas.drawLine(cx + radius * 0.77f, cy + radius * 0.1f, cx + radius * 0.56f, cy + radius * 0.27f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(85, 107, 86));
        canvas.drawCircle(pivotX, pivotY, radius * 0.082f, paint);
        paint.setColor(accentColor);
        canvas.drawCircle(pivotX, pivotY, radius * 0.032f, paint);
        paint.setColor(Color.rgb(214, 215, 189));
        canvas.drawRoundRect(cx + radius * 0.47f, cy + radius * 0.23f,
                cx + radius * 0.61f, cy + radius * 0.32f, radius * 0.016f, radius * 0.016f, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }
}
