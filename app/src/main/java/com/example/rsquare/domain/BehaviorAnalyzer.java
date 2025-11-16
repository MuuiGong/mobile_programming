package com.example.rsquare.domain;

import com.example.rsquare.data.local.entity.Journal;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 행동 분석 엔진
 * 사용자의 거래 패턴 감지 및 분석
 */
public class BehaviorAnalyzer {
    
    /**
     * 손실 회피 패턴 감지
     * 손실 후 거래 규모가 현저히 감소하는 패턴
     */
    public static BehaviorPattern detectLossAversion(List<Position> positions) {
        if (positions == null || positions.size() < 5) {
            return null;
        }
        
        List<Position> sortedPositions = new ArrayList<>(positions);
        Collections.sort(sortedPositions, (p1, p2) -> 
            p1.getOpenTime().compareTo(p2.getOpenTime()));
        
        int lossAversionCount = 0;
        
        for (int i = 1; i < sortedPositions.size(); i++) {
            Position prev = sortedPositions.get(i - 1);
            Position current = sortedPositions.get(i);
            
            // 이전 거래가 손실이고, 현재 거래 규모가 크게 감소한 경우
            if (prev.getPnl() < 0) {
                double prevSize = prev.getQuantity() * prev.getEntryPrice();
                double currentSize = current.getQuantity() * current.getEntryPrice();
                double sizeReduction = (prevSize - currentSize) / prevSize;
                
                if (sizeReduction > Constants.LOSS_AVERSION_THRESHOLD) {
                    lossAversionCount++;
                }
            }
        }
        
        if (lossAversionCount >= 3) {
            BehaviorPattern pattern = new BehaviorPattern(
                BehaviorPattern.PatternType.LOSS_AVERSION,
                lossAversionCount >= 5 ? BehaviorPattern.Severity.HIGH : BehaviorPattern.Severity.MEDIUM,
                lossAversionCount
            );
            pattern.setDescription("손실 후 거래 규모를 과도하게 줄이는 패턴이 감지되었습니다.");
            pattern.setRecommendation("손실은 정상적인 거래의 일부입니다. 일관된 포지션 크기를 유지하세요.");
            return pattern;
        }
        
        return null;
    }
    
    /**
     * 복수 매매 패턴 감지
     * 손실 직후 짧은 시간 내 더 큰 규모로 재진입
     */
    public static BehaviorPattern detectRevengeTrading(List<Position> positions) {
        if (positions == null || positions.size() < 3) {
            return null;
        }
        
        List<Position> sortedPositions = new ArrayList<>(positions);
        Collections.sort(sortedPositions, (p1, p2) -> 
            p1.getOpenTime().compareTo(p2.getOpenTime()));
        
        int revengeTradingCount = 0;
        
        for (int i = 1; i < sortedPositions.size(); i++) {
            Position prev = sortedPositions.get(i - 1);
            Position current = sortedPositions.get(i);
            
            // 이전 거래가 손실이고
            if (prev.getPnl() < 0 && prev.getCloseTime() != null) {
                long timeDiff = current.getOpenTime().getTime() - prev.getCloseTime().getTime();
                
                // 짧은 시간 내에 재진입
                if (timeDiff < Constants.REVENGE_TRADING_WINDOW) {
                    double prevSize = prev.getQuantity() * prev.getEntryPrice();
                    double currentSize = current.getQuantity() * current.getEntryPrice();
                    
                    // 더 큰 규모로 진입
                    if (currentSize > prevSize * 1.2) {
                        revengeTradingCount++;
                    }
                }
            }
        }
        
        if (revengeTradingCount >= 2) {
            BehaviorPattern pattern = new BehaviorPattern(
                BehaviorPattern.PatternType.REVENGE_TRADING,
                revengeTradingCount >= 4 ? BehaviorPattern.Severity.CRITICAL : BehaviorPattern.Severity.HIGH,
                revengeTradingCount
            );
            pattern.setDescription("손실 직후 감정적으로 더 큰 거래를 시도하는 패턴이 감지되었습니다.");
            pattern.setRecommendation("손실 후에는 잠시 휴식을 취하고 차분히 전략을 재검토하세요.");
            return pattern;
        }
        
        return null;
    }
    
    /**
     * 과매매 패턴 감지
     * 짧은 시간 내 과도한 거래 횟수
     */
    public static BehaviorPattern detectOvertrading(List<Position> positions, long timeWindow) {
        if (positions == null || positions.isEmpty()) {
            return null;
        }
        
        long currentTime = System.currentTimeMillis();
        int recentTradeCount = 0;
        
        for (Position position : positions) {
            long timeDiff = currentTime - position.getOpenTime().getTime();
            if (timeDiff <= timeWindow) {
                recentTradeCount++;
            }
        }
        
        // 하루에 10회 이상 거래
        if (recentTradeCount >= Constants.OVERTRADING_THRESHOLD) {
            BehaviorPattern pattern = new BehaviorPattern(
                BehaviorPattern.PatternType.OVERTRADING,
                recentTradeCount >= 15 ? BehaviorPattern.Severity.CRITICAL : BehaviorPattern.Severity.HIGH,
                recentTradeCount
            );
            pattern.setDescription("과도하게 많은 거래를 하고 있습니다 (최근 " + recentTradeCount + "회).");
            pattern.setRecommendation("거래 빈도를 줄이고 각 거래를 신중히 분석하세요. 질이 양보다 중요합니다.");
            return pattern;
        }
        
        return null;
    }
    
    /**
     * 충동적 행동 분석
     * 감정 저널 기반 분석
     */
    public static BehaviorPattern detectImpulsiveBehavior(List<Journal> journals) {
        if (journals == null || journals.isEmpty()) {
            return null;
        }
        
        int impulsiveCount = 0;
        
        for (Journal journal : journals) {
            if (journal.getEmotion() == Journal.Emotion.FOMO ||
                journal.getEmotion() == Journal.Emotion.REVENGE ||
                journal.getEmotion() == Journal.Emotion.GREEDY) {
                impulsiveCount++;
            }
        }
        
        double impulsiveRatio = (double) impulsiveCount / journals.size();
        
        if (impulsiveRatio > 0.5) {
            BehaviorPattern pattern = new BehaviorPattern(
                BehaviorPattern.PatternType.IMPULSIVE_BEHAVIOR,
                impulsiveRatio > 0.7 ? BehaviorPattern.Severity.HIGH : BehaviorPattern.Severity.MEDIUM,
                impulsiveCount
            );
            pattern.setDescription("거래 중 충동적인 감정이 자주 나타나고 있습니다.");
            pattern.setRecommendation("거래 전 체크리스트를 만들고, 감정이 고조될 때는 잠시 휴식을 취하세요.");
            return pattern;
        }
        
        return null;
    }
    
    /**
     * 부실한 리스크 관리 패턴 감지
     * 낮은 R:R 비율의 거래가 많은 경우
     */
    public static BehaviorPattern detectPoorRiskManagement(List<Position> positions) {
        if (positions == null || positions.size() < 5) {
            return null;
        }
        
        int poorRRCount = 0;
        
        for (Position position : positions) {
            double rr = RiskCalculator.calculateRiskRewardRatio(
                position.getEntryPrice(),
                position.getTakeProfit(),
                position.getStopLoss(),
                position.isLong()
            );
            
            if (rr < Constants.MIN_RISK_REWARD_RATIO) {
                poorRRCount++;
            }
        }
        
        double poorRRRatio = (double) poorRRCount / positions.size();
        
        if (poorRRRatio > 0.4) {
            BehaviorPattern pattern = new BehaviorPattern(
                BehaviorPattern.PatternType.POOR_RISK_MANAGEMENT,
                poorRRRatio > 0.6 ? BehaviorPattern.Severity.CRITICAL : BehaviorPattern.Severity.HIGH,
                poorRRCount
            );
            pattern.setDescription("R:R 비율이 낮은 거래가 많습니다 (" + 
                String.format("%.0f%%", poorRRRatio * 100) + ").");
            pattern.setRecommendation("최소 2:1의 R:R 비율을 목표로 하세요. 잠재적 이익이 리스크보다 커야 합니다.");
            return pattern;
        }
        
        return null;
    }
    
    /**
     * 연속 손실 패턴 감지 (명세서 요구사항)
     */
    public static BehaviorPattern detectConsecutiveLosses(List<Position> positions) {
        if (positions == null || positions.size() < 3) {
            return null;
        }
        
        List<Position> sortedPositions = new ArrayList<>(positions);
        Collections.sort(sortedPositions, (p1, p2) -> 
            p1.getOpenTime().compareTo(p2.getOpenTime()));
        
        int consecutiveLosses = 0;
        int maxConsecutive = 0;
        
        for (Position position : sortedPositions) {
            if (position.isClosed()) {
                if (position.getPnl() < 0) {
                    consecutiveLosses++;
                    maxConsecutive = Math.max(maxConsecutive, consecutiveLosses);
                } else {
                    consecutiveLosses = 0;
                }
            }
        }
        
        if (maxConsecutive >= 3) {
            BehaviorPattern pattern = new BehaviorPattern(
                BehaviorPattern.PatternType.CONSECUTIVE_LOSSES,
                maxConsecutive >= 5 ? BehaviorPattern.Severity.CRITICAL : BehaviorPattern.Severity.HIGH,
                maxConsecutive
            );
            pattern.setDescription("연속 " + maxConsecutive + "회 손실이 발생했습니다.");
            pattern.setRecommendation("연속 손실이 발생했습니다. 규칙을 재검토하세요. 거래를 잠시 중단하고 전략을 점검하세요.");
            return pattern;
        }
        
        return null;
    }
    
    /**
     * 손절 변경 패턴 감지 (명세서 요구사항 - Moving Stop-Loss)
     * 실제로는 포지션 히스토리를 추적해야 하지만, 여기서는 간단히 구현
     */
    public static BehaviorPattern detectMovingStopLoss(List<Position> positions) {
        // 실제 구현에서는 포지션의 SL 변경 이력을 추적해야 함
        // 여기서는 간단히 TP/SL 비율이 비정상적으로 변경된 경우를 감지
        if (positions == null || positions.size() < 3) {
            return null;
        }
        
        int movingSLCount = 0;
        for (Position position : positions) {
            if (position.isClosed()) {
                // TP와 SL의 거리 비율이 비정상적이면 감지
                double tpDistance = Math.abs(position.getTakeProfit() - position.getEntryPrice());
                double slDistance = Math.abs(position.getEntryPrice() - position.getStopLoss());
                
                if (slDistance > 0) {
                    double ratio = tpDistance / slDistance;
                    // R:R 비율이 0.5 미만이면 손절을 너무 넓게 설정한 것으로 간주
                    if (ratio < 0.5) {
                        movingSLCount++;
                    }
                }
            }
        }
        
        if (movingSLCount >= 3) {
            BehaviorPattern pattern = new BehaviorPattern(
                BehaviorPattern.PatternType.MOVING_STOP_LOSS,
                BehaviorPattern.Severity.MEDIUM,
                movingSLCount
            );
            pattern.setDescription("손절을 변경하고 있습니다. 규칙을 지키세요.");
            pattern.setRecommendation("손절을 변경하고 있습니다. 규칙을 지키세요. 설정한 SL을 그대로 유지하세요.");
            return pattern;
        }
        
        return null;
    }
    
    /**
     * 충동적 진입 패턴 감지 (명세서 요구사항 - Impulsive Entry)
     * 차트 확인 후 3초 내 거래 진입
     */
    public static BehaviorPattern detectImpulsiveEntry(List<Position> positions, List<Journal> journals) {
        if (positions == null || positions.size() < 3) {
            return null;
        }
        
        // 저널에서 차트 확인 시간과 거래 진입 시간 비교
        // 실제로는 더 정교한 추적이 필요하지만, 여기서는 간단히 구현
        int impulsiveCount = 0;
        
        for (Position position : positions) {
            // 진입 시간이 너무 빠르면 충동적 진입으로 간주
            // 실제로는 차트 열람 시간과 비교해야 함
            if (position.getOpenTime() != null) {
                // 간단히 거래 간격이 너무 짧으면 충동적 진입으로 간주
                // 실제 구현에서는 차트 열람 이벤트를 추적해야 함
            }
        }
        
        // 저널 기반으로 감정적 진입 감지
        if (journals != null) {
            for (Journal journal : journals) {
                if (journal.getEmotion() == Journal.Emotion.FOMO ||
                    journal.getEmotion() == Journal.Emotion.GREEDY) {
                    impulsiveCount++;
                }
            }
        }
        
        if (impulsiveCount >= 3) {
            BehaviorPattern pattern = new BehaviorPattern(
                BehaviorPattern.PatternType.IMPULSIVE_ENTRY,
                BehaviorPattern.Severity.MEDIUM,
                impulsiveCount
            );
            pattern.setDescription("너무 빠르게 진입하고 있습니다.");
            pattern.setRecommendation("너무 빠르게 진입하고 있습니다. 차트를 충분히 분석한 후 진입하세요.");
            return pattern;
        }
        
        return null;
    }
    
    /**
     * 모든 패턴 감지 (명세서의 모든 패턴 포함)
     */
    public static List<BehaviorPattern> analyzeAllPatterns(List<Position> positions, 
                                                           List<Journal> journals) {
        List<BehaviorPattern> patterns = new ArrayList<>();
        
        // 명세서의 위험한 패턴들
        BehaviorPattern overtrading = detectOvertrading(positions, 60 * 60 * 1000); // 1시간 내 5회 이상
        if (overtrading != null) patterns.add(overtrading);
        
        BehaviorPattern lossAversion = detectLossAversion(positions);
        if (lossAversion != null) patterns.add(lossAversion);
        
        BehaviorPattern movingSL = detectMovingStopLoss(positions);
        if (movingSL != null) patterns.add(movingSL);
        
        BehaviorPattern impulsiveEntry = detectImpulsiveEntry(positions, journals);
        if (impulsiveEntry != null) patterns.add(impulsiveEntry);
        
        BehaviorPattern consecutiveLosses = detectConsecutiveLosses(positions);
        if (consecutiveLosses != null) patterns.add(consecutiveLosses);
        
        // 기존 패턴들
        BehaviorPattern revengeTrading = detectRevengeTrading(positions);
        if (revengeTrading != null) patterns.add(revengeTrading);
        
        BehaviorPattern impulsive = detectImpulsiveBehavior(journals);
        if (impulsive != null) patterns.add(impulsive);
        
        BehaviorPattern poorRisk = detectPoorRiskManagement(positions);
        if (poorRisk != null) patterns.add(poorRisk);
        
        return patterns;
    }
}

