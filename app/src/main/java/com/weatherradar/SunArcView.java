package com.weatherradar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SunArcView extends View {
    private Paint arcPaint, sunPaint, glowPaint;
    private float progress = 0.5f;

    public SunArcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // القوس
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(Color.parseColor("#60FFFFFF")); // أبيض شفاف
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(3f);

        // النقطة الصلبة (الشمس)
        sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunPaint.setColor(Color.WHITE);
        sunPaint.setStyle(Paint.Style.FILL);

        // التوهج (لون أصفر فاتح ليكون واضحاً فوق الأزرق)
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(Color.parseColor("#A0FFD54F")); 
        glowPaint.setStyle(Paint.Style.FILL);
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(1, progress));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        float centerX = width / 2f;
        float centerY = height - 10f;

        // 🌟 الحل: حساب نصف القطر ليتناسب مع العرض والارتفاع معاً
        float maxRadiusWidth = (width - 30) / 2f;
        float maxRadiusHeight = height - 20f;
        float radius = Math.min(maxRadiusWidth, maxRadiusHeight);

        // رسم القوس
        RectF oval = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        canvas.drawArc(oval, 180, 180, false, arcPaint);

        // حساب موقع الشمس بدقة على القوس
        double angleRad = Math.toRadians(180 + (progress * 180));
        float dotX = centerX + radius * (float) Math.cos(angleRad);
        float dotY = centerY + radius * (float) Math.sin(angleRad);

        // رسم التوهج ثم النواة البيضاء
        canvas.drawCircle(dotX, dotY, 15f, glowPaint);
        canvas.drawCircle(dotX, dotY, 6f, sunPaint);
    }
}
