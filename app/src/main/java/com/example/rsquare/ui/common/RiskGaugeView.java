package com.example.rsquare.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Risk Gauge Custom View
 * 리스크 스코어를 시각적으로 표시하는 원형 게이지
 */
public class RiskGaugeView extends View {
    
    private Paint backgroundPaint;
    private Paint gaugePaint;
    private Paint textPaint;
    private Paint labelPaint;
    
    private RectF gaugeRect;
    
    private float riskScore = 0f; // 0-100
    private String label = "리스크 스코어";
    
    private static final float START_ANGLE = 135f;
    private static final float SWEEP_ANGLE = 270f;
    private static final float STROKE_WIDTH = 40f;
    
    public RiskGaugeView(Context context) {
        super(context);
        init();
    }
    
    public RiskGaugeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public RiskGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 배경 페인트
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(STROKE_WIDTH);
        backgroundPaint.setColor(0xFF2b2b43);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        
        // 게이지 페인트
        gaugePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gaugePaint.setStyle(Paint.Style.STROKE);
        gaugePaint.setStrokeWidth(STROKE_WIDTH);
        gaugePaint.setStrokeCap(Paint.Cap.ROUND);
        
        // 텍스트 페인트 (점수)
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(80f);
        textPaint.setColor(0xFFd1d4dc);
        
        // 레이블 페인트
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(28f);
        labelPaint.setColor(0xFF787b86);
        
        gaugeRect = new RectF();
    }
    
    /**
     * 리스크 스코어 설정
     */
    public void setRiskScore(float score) {
        this.riskScore = Math.max(0, Math.min(100, score));
        invalidate();
    }
    
    /**
     * 레이블 설정
     */
    public void setLabel(String label) {
        this.label = label;
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = (size - STROKE_WIDTH * 2 - 40) / 2f;
        
        gaugeRect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        );
        
        // 배경 원 그리기
        canvas.drawArc(gaugeRect, START_ANGLE, SWEEP_ANGLE, false, backgroundPaint);
        
        // 게이지 색상 결정
        int gaugeColor = getGaugeColor(riskScore);
        gaugePaint.setColor(gaugeColor);
        
        // 게이지 그리기
        float sweepAngle = (riskScore / 100f) * SWEEP_ANGLE;
        canvas.drawArc(gaugeRect, START_ANGLE, sweepAngle, false, gaugePaint);
        
        // 점수 텍스트 그리기
        String scoreText = String.format("%.0f", riskScore);
        float textY = centerY + (textPaint.descent() - textPaint.ascent()) / 2 - textPaint.descent();
        canvas.drawText(scoreText, centerX, textY - 20, textPaint);
        
        // 레이블 그리기
        canvas.drawText(label, centerX, textY + 50, labelPaint);
    }
    
    /**
     * 리스크 스코어에 따른 색상 반환
     */
    private int getGaugeColor(float score) {
        if (score >= 70) {
            return 0xFF26a69a; // 안전 (초록)
        } else if (score >= 50) {
            return 0xFFFFC107; // 주의 (노랑)
        } else if (score >= 30) {
            return 0xFFFF9800; // 경고 (주황)
        } else {
            return 0xFFef5350; // 위험 (빨강)
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        );
        
        setMeasuredDimension(size, size);
    }
}

