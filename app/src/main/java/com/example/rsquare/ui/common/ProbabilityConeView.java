package com.example.rsquare.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Probability Cone Custom View
 * Monte Carlo 시뮬레이션 결과의 확률 분포를 시각화
 */
public class ProbabilityConeView extends View {
    
    private Paint linePaint;
    private Paint fill25Paint;
    private Paint fill50Paint;
    private Paint fill75Paint;
    private Paint gridPaint;
    private Paint textPaint;
    
    private List<Double> cumulativeReturns = new ArrayList<>();
    private double percentile25 = 0;
    private double percentile50 = 0;
    private double percentile75 = 0;
    
    private float padding = 60f;
    
    public ProbabilityConeView(Context context) {
        super(context);
        init();
    }
    
    public ProbabilityConeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public ProbabilityConeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 중앙선 페인트
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);
        linePaint.setColor(0xFF2196F3);
        
        // 25% 영역 페인트
        fill25Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill25Paint.setStyle(Paint.Style.FILL);
        fill25Paint.setColor(0x1026a69a);
        
        // 50% 영역 페인트
        fill50Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill50Paint.setStyle(Paint.Style.FILL);
        fill50Paint.setColor(0x2026a69a);
        
        // 75% 영역 페인트
        fill75Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill75Paint.setStyle(Paint.Style.FILL);
        fill75Paint.setColor(0x3026a69a);
        
        // 그리드 페인트
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(0x40787b86);
        
        // 텍스트 페인트
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(24f);
        textPaint.setColor(0xFF787b86);
    }
    
    /**
     * 데이터 설정
     */
    public void setData(List<Double> cumulativeReturns, double p25, double p50, double p75) {
        this.cumulativeReturns = new ArrayList<>(cumulativeReturns);
        this.percentile25 = p25;
        this.percentile50 = p50;
        this.percentile75 = p75;
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (cumulativeReturns.isEmpty()) {
            // 데이터 없음 표시
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("데이터 없음", getWidth() / 2f, getHeight() / 2f, textPaint);
            return;
        }
        
        float width = getWidth() - 2 * padding;
        float height = getHeight() - 2 * padding;
        
        // 데이터 범위 계산
        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;
        
        for (Double value : cumulativeReturns) {
            minValue = Math.min(minValue, value);
            maxValue = Math.max(maxValue, value);
        }
        
        // 백분위수도 포함
        minValue = Math.min(minValue, Math.min(percentile25, percentile75));
        maxValue = Math.max(maxValue, Math.max(percentile25, percentile75));
        
        double range = maxValue - minValue;
        if (range == 0) range = 1;
        
        // 그리드 그리기
        drawGrid(canvas, width, height, minValue, maxValue);
        
        // Probability Cone 그리기 (백분위수 기반)
        drawProbabilityCone(canvas, width, height, minValue, range);
        
        // 중앙선 (평균 누적 수익률) 그리기
        drawCenterLine(canvas, width, height, minValue, range);
    }
    
    /**
     * 그리드 그리기
     */
    private void drawGrid(Canvas canvas, float width, float height, double minValue, double maxValue) {
        // 수평 그리드 (5개)
        for (int i = 0; i <= 5; i++) {
            float y = padding + (height / 5f) * i;
            canvas.drawLine(padding, y, padding + width, y, gridPaint);
            
            // Y축 레이블
            double value = maxValue - (maxValue - minValue) * (i / 5.0);
            String label = String.format("%.0f%%", value);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(label, padding - 10, y + 8, textPaint);
        }
        
        // 수직 그리드
        int gridCount = Math.min(cumulativeReturns.size(), 10);
        for (int i = 0; i <= gridCount; i++) {
            float x = padding + (width / gridCount) * i;
            canvas.drawLine(x, padding, x, padding + height, gridPaint);
        }
    }
    
    /**
     * Probability Cone 그리기
     */
    private void drawProbabilityCone(Canvas canvas, float width, float height, 
                                     double minValue, double range) {
        int dataSize = cumulativeReturns.size();
        if (dataSize < 2) return;
        
        // 75% 영역
        Path path75 = new Path();
        path75.moveTo(padding, padding + height / 2f);
        
        for (int i = 0; i < dataSize; i++) {
            float x = padding + (width / (dataSize - 1)) * i;
            double spread = (percentile75 - percentile25) * (i / (double) dataSize);
            float yTop = padding + (float) ((1 - (percentile50 + spread - minValue) / range) * height);
            path75.lineTo(x, yTop);
        }
        
        for (int i = dataSize - 1; i >= 0; i--) {
            float x = padding + (width / (dataSize - 1)) * i;
            double spread = (percentile75 - percentile25) * (i / (double) dataSize);
            float yBottom = padding + (float) ((1 - (percentile50 - spread - minValue) / range) * height);
            path75.lineTo(x, yBottom);
        }
        
        path75.close();
        canvas.drawPath(path75, fill75Paint);
    }
    
    /**
     * 중앙선 그리기
     */
    private void drawCenterLine(Canvas canvas, float width, float height, 
                                double minValue, double range) {
        Path centerPath = new Path();
        
        for (int i = 0; i < cumulativeReturns.size(); i++) {
            float x = padding + (width / (cumulativeReturns.size() - 1)) * i;
            double value = cumulativeReturns.get(i);
            float y = padding + (float) ((1 - (value - minValue) / range) * height);
            
            if (i == 0) {
                centerPath.moveTo(x, y);
            } else {
                centerPath.lineTo(x, y);
            }
        }
        
        canvas.drawPath(centerPath, linePaint);
    }
}

