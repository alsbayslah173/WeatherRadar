package com.weatherradar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class WindCompassView extends View {
    private Paint circlePaint, textPaint, arrowPaint;
    private float direction = 0f;

    public WindCompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#40FFFFFF"));
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(3f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#B0FFFFFF"));
        textPaint.setTextSize(20f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.WHITE);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setStrokeWidth(4f);
    }

    public void setDirection(float direction) {
        this.direction = direction;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int radius = Math.min(cx, cy) - 15;

        canvas.drawCircle(cx, cy, radius, circlePaint);

        canvas.drawText("ش", cx, cy - radius + 20, textPaint);
        canvas.drawText("ج", cx, cy + radius - 5, textPaint);
        canvas.drawText("ق", cx + radius - 10, cy + 6, textPaint);
        canvas.drawText("غ", cx - radius + 10, cy + 6, textPaint);

        canvas.save();
        canvas.rotate(direction, cx, cy);
        
        Path arrowPath = new Path();
        arrowPath.moveTo(cx, cy - radius + 25);
        arrowPath.lineTo(cx - 8, cy + 10);
        arrowPath.lineTo(cx + 8, cy + 10);
        arrowPath.close();
        
        canvas.drawPath(arrowPath, arrowPaint);
        canvas.drawLine(cx, cy + 10, cx, cy + radius - 20, arrowPaint);
        
        canvas.restore();
    }
}
