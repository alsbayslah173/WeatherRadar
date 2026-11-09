package com.weatherradar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.List;
import java.util.Locale;

public class HourlyForecastView extends View {

    private List<HourlyItem> data;
    private Paint linePaint, pointPaint, textPaint, tempPaint, fillPaint, windPaint, glowPaint, nowLinePaint;
    private Path linePath, fillPath, nowIndicatorPath;
    private int paddingX = 50; 
    private float animationProgress = 0f; 

    public static class HourlyItem {
        public String time;
        public double temp;
        public int weatherCode;
        public double windSpeed;

        public HourlyItem(String time, double temp, int weatherCode, double windSpeed) {
            this.time = time;
            this.temp = temp;
            this.weatherCode = weatherCode;
            this.windSpeed = windSpeed;
        }
    }

    public HourlyForecastView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(Color.parseColor("#FFD54F"));
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(12f);
        glowPaint.setAlpha(60);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#FFD54F")); 
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.WHITE);
        pointPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#B0FFFFFF"));
        textPaint.setTextAlign(Paint.Align.CENTER);

        tempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempPaint.setColor(Color.WHITE);
        tempPaint.setTextSize(28f);
        tempPaint.setFakeBoldText(true);
        tempPaint.setTextAlign(Paint.Align.CENTER);

        windPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        windPaint.setColor(Color.parseColor("#90CAF9")); 
        windPaint.setTextSize(16f); 
        windPaint.setTextAlign(Paint.Align.CENTER);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        nowLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nowLinePaint.setColor(Color.parseColor("#80FFFFFF"));
        nowLinePaint.setStyle(Paint.Style.STROKE);
        nowLinePaint.setStrokeWidth(2f);
        nowLinePaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        linePath = new Path();
        fillPath = new Path();
        nowIndicatorPath = new Path();
    }

    public void setData(List<HourlyItem> data) {
        this.data = data;
        requestLayout();
        
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1500); 
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (data != null && !data.isEmpty()) {
            int desiredWidth = (data.size() - 1) * 140 + (2 * paddingX);
            int width = resolveSize(desiredWidth, widthMeasureSpec);
            int height = (int) (220 * getResources().getDisplayMetrics().density);
            setMeasuredDimension(width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.isEmpty() || animationProgress == 0f) return;

        int width = getWidth();
        int height = getHeight();
        
        float stepX = (width - 2 * paddingX) / (float) (data.size() - 1);

        double maxTemp = -100, minTemp = 100;
        for (HourlyItem item : data) {
            if (item.temp > maxTemp) maxTemp = item.temp;
            if (item.temp < minTemp) minTemp = item.temp;
        }
        
        maxTemp += 1;
        minTemp -= 1;
        double tempRange = maxTemp - minTemp;
        if (tempRange == 0) tempRange = 1;

        float graphTop = height * 0.25f; 
        float graphBottom = height * 0.55f;
        float graphHeight = graphBottom - graphTop;

        linePath.reset();
        fillPath.reset();
        nowIndicatorPath.reset();

        float firstX = width - paddingX;
        nowIndicatorPath.moveTo(firstX, graphTop - 20);
        nowIndicatorPath.lineTo(firstX, graphBottom + 45);
        canvas.drawPath(nowIndicatorPath, nowLinePaint);

        int pointsToDraw = (int) (animationProgress * (data.size() - 1));
        
        for (int i = 0; i <= pointsToDraw; i++) {
            HourlyItem item = data.get(i);
            float x = (width - paddingX) - (i * stepX); 
            
            float y = (float) (graphTop + ((maxTemp - item.temp) / tempRange) * graphHeight);

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, y);
            } else {
                float prevX = (width - paddingX) - ((i - 1) * stepX);
                float prevY = (float) (graphTop + ((maxTemp - data.get(i - 1).temp) / tempRange) * graphHeight);
                
                float midX = (prevX + x) / 2;
                linePath.cubicTo(midX, prevY, midX, y, x, y);
                fillPath.cubicTo(midX, prevY, midX, y, x, y);
            }

            if (i == 0) {
                Paint p = new Paint(glowPaint);
                p.setAlpha(150);
                canvas.drawCircle(x, y, 16f, p);
            }
            
            canvas.drawCircle(x, y, 6f, pointPaint);

            canvas.drawText(String.format(Locale.getDefault(), "%.0f°", item.temp), x, y - 20, tempPaint);

            String icon = getWeatherIcon(item.weatherCode);
            textPaint.setTextSize(35f);
            canvas.drawText(icon, x, height * 0.72f, textPaint);

            canvas.drawText(String.format(Locale.getDefault(), "%.0f كم/س", item.windSpeed), x, height * 0.84f, windPaint);

            textPaint.setTextSize(18f);
            canvas.drawText(item.time, x, height * 0.94f, textPaint);
        }

        if (pointsToDraw > 0) {
            float lastDrawnX = (width - paddingX) - (pointsToDraw * stepX);
            float lastY = graphBottom;
            fillPath.lineTo(lastDrawnX, lastY);
            fillPath.lineTo(width - paddingX, lastY); 
            fillPath.close();

            fillPaint.setShader(new LinearGradient(0, graphTop, 0, graphBottom, 
                    Color.parseColor("#40FFD54F"), Color.TRANSPARENT, Shader.TileMode.CLAMP));

            canvas.drawPath(fillPath, fillPaint);
            canvas.drawPath(linePath, glowPaint); 
            canvas.drawPath(linePath, linePaint); // تم إصلاح الخطأ هنا
        }
    }

    private String getWeatherIcon(int code) {
        if (code == 0) return "☀️";
        if (code <= 3) return "⛅";
        if (code <= 48) return "🌫️";
        if (code <= 67) return "🌧️";
        if (code <= 77) return "❄️";
        if (code <= 82) return "🌦️";
        return "⛈️";
    }
}
