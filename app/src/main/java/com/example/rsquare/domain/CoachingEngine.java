package com.example.rsquare.domain;

import com.example.rsquare.data.local.entity.Challenge;
import com.example.rsquare.data.local.entity.Journal;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 코칭 엔진
 * AI 기반 행동 패턴 분석 및 맞춤 피드백 제공
 */
public class CoachingEngine {
    
    /**
     * 거래 세션 분석
     * 최근 거래 및 저널을 분석하여 전반적인 피드백 생성
     */
    public static List<CoachingMessage> analyzeTradingSession(List<Position> positions, 
                                                               List<Journal> journals,
                                                               RiskMetrics riskMetrics) {
        List<CoachingMessage> messages = new ArrayList<>();
        
        if (positions == null || positions.isEmpty()) {
            CoachingMessage welcome = new CoachingMessage(
                "첫 거래를 시작해보세요! R² 앱과 함께 리스크 관리 능력을 키워나가세요.",
                CoachingMessage.MessageType.SUGGESTION
            );
            welcome.addActionItem("대시보드에서 암호화폐를 선택하여 첫 모의 거래 시작하기");
            messages.add(welcome);
            return messages;
        }
        
        // 행동 패턴 분석
        List<BehaviorPattern> patterns = BehaviorAnalyzer.analyzeAllPatterns(positions, journals);
        
        // 패턴 기반 메시지 생성
        for (BehaviorPattern pattern : patterns) {
            CoachingMessage message = generatePatternMessage(pattern);
            if (message != null) {
                messages.add(message);
            }
        }
        
        // 리스크 스코어 기반 메시지
        if (riskMetrics != null) {
            CoachingMessage riskMessage = generateRiskMessage(riskMetrics);
            if (riskMessage != null) {
                messages.add(riskMessage);
            }
        }
        
        // 거래 통계 기반 메시지
        CoachingMessage statsMessage = generateStatisticsMessage(positions);
        if (statsMessage != null) {
            messages.add(statsMessage);
        }
        
        // 긍정적 피드백
        CoachingMessage positiveMessage = generatePositiveFeedback(positions, patterns);
        if (positiveMessage != null) {
            messages.add(positiveMessage);
        }
        
        return messages;
    }
    
    /**
     * 패턴 기반 메시지 생성
     */
    private static CoachingMessage generatePatternMessage(BehaviorPattern pattern) {
        CoachingMessage message = new CoachingMessage();
        message.setType(CoachingMessage.MessageType.WARNING);
        
        String patternName = pattern.getType().getDisplayName();
        message.setMessage("⚠️ " + patternName + " 패턴이 감지되었습니다");
        
        message.addActionItem(pattern.getDescription());
        message.addActionItem("💡 " + pattern.getRecommendation());
        
        return message;
    }
    
    /**
     * 리스크 스코어 기반 메시지
     */
    private static CoachingMessage generateRiskMessage(RiskMetrics metrics) {
        CoachingMessage message = new CoachingMessage();
        
        if (metrics.getRiskScore() >= 70) {
            message.setType(CoachingMessage.MessageType.POSITIVE);
            message.setMessage("✅ 우수한 리스크 관리! 현재 리스크 스코어: " + 
                String.format("%.0f", metrics.getRiskScore()));
            message.addActionItem("현재의 신중한 접근 방식을 유지하세요");
        } else if (metrics.getRiskScore() >= 50) {
            message.setType(CoachingMessage.MessageType.SUGGESTION);
            message.setMessage("⚡ 리스크 관리를 개선할 여지가 있습니다 (스코어: " + 
                String.format("%.0f", metrics.getRiskScore()) + ")");
            message.addActionItem("변동성과 MDD를 줄이기 위해 포지션 크기 조정 고려");
            message.addActionItem("R:R 비율 2:1 이상 유지하기");
        } else {
            message.setType(CoachingMessage.MessageType.WARNING);
            message.setMessage("🚨 주의! 리스크 수준이 높습니다 (스코어: " + 
                String.format("%.0f", metrics.getRiskScore()) + ")");
            message.addActionItem("즉시 포지션 규모를 줄이세요");
            message.addActionItem("손절 설정을 더 보수적으로 조정하세요");
            message.addActionItem("새로운 거래 전 전략을 재검토하세요");
        }
        
        return message;
    }
    
    /**
     * 통계 기반 메시지
     */
    private static CoachingMessage generateStatisticsMessage(List<Position> positions) {
        int winCount = 0;
        int totalCount = 0;
        double totalPnL = 0;
        
        for (Position position : positions) {
            if (position.isClosed()) {
                totalCount++;
                totalPnL += position.getPnl();
                if (position.getPnl() > 0) {
                    winCount++;
                }
            }
        }
        
        if (totalCount < 5) {
            return null; // 충분한 데이터 없음
        }
        
        double winRate = ((double) winCount / totalCount) * 100;
        
        CoachingMessage message = new CoachingMessage();
        
        if (totalPnL > 0 && winRate >= 50) {
            message.setType(CoachingMessage.MessageType.ACHIEVEMENT);
            message.setMessage("🎉 훌륭합니다! 총 " + totalCount + "회 거래 중 승률 " + 
                String.format("%.1f%%", winRate));
            message.addActionItem("현재 전략이 효과적입니다. 계속 유지하세요!");
        } else if (winRate < 40) {
            message.setType(CoachingMessage.MessageType.SUGGESTION);
            message.setMessage("📊 승률이 낮습니다 (" + String.format("%.1f%%", winRate) + 
                "). 진입 기준을 재검토해보세요");
            message.addActionItem("거래 전 체크리스트 만들기");
            message.addActionItem("과거 거래 리플레이를 통해 실수 분석하기");
        }
        
        return message;
    }
    
    /**
     * 긍정적 피드백 생성
     */
    private static CoachingMessage generatePositiveFeedback(List<Position> positions, 
                                                            List<BehaviorPattern> patterns) {
        // 패턴이 감지되지 않았고 거래가 충분히 있으면 긍정적 피드백
        if (patterns.isEmpty() && positions.size() >= 10) {
            CoachingMessage message = new CoachingMessage(
                "🌟 좋은 거래 습관을 유지하고 있습니다!",
                CoachingMessage.MessageType.POSITIVE
            );
            message.addActionItem("위험한 행동 패턴이 감지되지 않았습니다");
            message.addActionItem("계속해서 체계적인 거래를 이어가세요");
            return message;
        }
        
        return null;
    }
    
    /**
     * 챌린지 추천
     * 사용자의 행동 패턴에 맞는 챌린지 생성
     */
    public static Challenge recommendChallenge(long userId, List<BehaviorPattern> patterns, 
                                               RiskMetrics metrics) {
        Challenge challenge = new Challenge();
        challenge.setUserId(userId);
        
        // 패턴에 따른 맞춤 챌린지
        if (patterns != null && !patterns.isEmpty()) {
            BehaviorPattern mainPattern = patterns.get(0);
            
            switch (mainPattern.getType()) {
                case POOR_RISK_MANAGEMENT:
                    challenge.setTitle("리스크 마스터 챌린지");
                    challenge.setDescription("R:R 비율 2.0 이상으로 5회 연속 거래하기");
                    challenge.setTargetValue(5);
                    challenge.setTargetType(Constants.CHALLENGE_TYPE_RR);
                    challenge.setDifficulty(Challenge.Difficulty.MEDIUM);
                    break;
                    
                case REVENGE_TRADING:
                case IMPULSIVE_BEHAVIOR:
                    challenge.setTitle("감정 조절 챌린지");
                    challenge.setDescription("모든 거래에 감정 저널 작성하고 충동적 감정 0회 유지");
                    challenge.setTargetValue(10);
                    challenge.setTargetType(Constants.CHALLENGE_TYPE_EMOTION);
                    challenge.setDifficulty(Challenge.Difficulty.HARD);
                    break;
                    
                case OVERTRADING:
                    challenge.setTitle("절제력 챌린지");
                    challenge.setDescription("하루 최대 3회 거래로 제한하며 7일 달성");
                    challenge.setTargetValue(7);
                    challenge.setTargetType(Constants.CHALLENGE_TYPE_CONSISTENT);
                    challenge.setDifficulty(Challenge.Difficulty.MEDIUM);
                    break;
                    
                default:
                    return createDefaultChallenge(userId);
            }
        } else {
            return createDefaultChallenge(userId);
        }
        
        return challenge;
    }
    
    /**
     * 기본 챌린지 생성
     */
    private static Challenge createDefaultChallenge(long userId) {
        Challenge challenge = new Challenge();
        challenge.setUserId(userId);
        challenge.setTitle("일관성 챌린지");
        challenge.setDescription("5회 연속으로 계획된 TP/SL 준수하기");
        challenge.setTargetValue(5);
        challenge.setTargetType(Constants.CHALLENGE_TYPE_CONSISTENT);
        challenge.setDifficulty(Challenge.Difficulty.EASY);
        return challenge;
    }
    
    /**
     * 주간 리포트 생성
     */
    public static WeeklyReport generateWeeklyReport(List<Position> positions, 
                                                    List<Journal> journals,
                                                    RiskMetrics metrics) {
        WeeklyReport report = new WeeklyReport();
        
        // 거래 통계
        int totalTrades = 0;
        int winCount = 0;
        double totalPnL = 0;
        double bestTrade = Double.MIN_VALUE;
        double worstTrade = Double.MAX_VALUE;
        
        for (Position position : positions) {
            if (position.isClosed()) {
                totalTrades++;
                totalPnL += position.getPnl();
                
                if (position.getPnl() > 0) {
                    winCount++;
                }
                
                if (position.getPnl() > bestTrade) {
                    bestTrade = position.getPnl();
                }
                
                if (position.getPnl() < worstTrade) {
                    worstTrade = position.getPnl();
                }
            }
        }
        
        report.setTotalTrades(totalTrades);
        report.setWinCount(winCount);
        report.setTotalPnL(totalPnL);
        report.setBestTrade(bestTrade != Double.MIN_VALUE ? bestTrade : 0);
        report.setWorstTrade(worstTrade != Double.MAX_VALUE ? worstTrade : 0);
        
        // 승률
        if (totalTrades > 0) {
            report.setWinRate(((double) winCount / totalTrades) * 100);
        }
        
        // 리스크 메트릭스
        report.setRiskMetrics(metrics);
        
        // 행동 패턴
        List<BehaviorPattern> patterns = BehaviorAnalyzer.analyzeAllPatterns(positions, journals);
        report.setDetectedPatterns(patterns);
        
        // 감정 분석
        if (journals != null && !journals.isEmpty()) {
            int negativeEmotions = 0;
            for (Journal journal : journals) {
                if (journal.getEmotion() == Journal.Emotion.ANXIOUS ||
                    journal.getEmotion() == Journal.Emotion.FEAR ||
                    journal.getEmotion() == Journal.Emotion.REVENGE) {
                    negativeEmotions++;
                }
            }
            report.setNegativeEmotionRate(((double) negativeEmotions / journals.size()) * 100);
        }
        
        // 개선 제안
        report.setImprovementSuggestions(generateImprovementSuggestions(report));
        
        return report;
    }
    
    /**
     * 개선 제안 생성
     */
    private static List<String> generateImprovementSuggestions(WeeklyReport report) {
        List<String> suggestions = new ArrayList<>();
        
        if (report.getWinRate() < 50) {
            suggestions.add("승률이 50% 미만입니다. 진입 기준을 더 엄격하게 설정하세요.");
        }
        
        if (report.getTotalPnL() < 0) {
            suggestions.add("주간 손실이 발생했습니다. 포지션 크기를 줄이고 리스크를 재평가하세요.");
        }
        
        if (report.getRiskMetrics() != null && report.getRiskMetrics().getRiskScore() < 60) {
            suggestions.add("리스크 관리를 강화하세요. R:R 비율을 높이고 손절을 철저히 지키세요.");
        }
        
        if (report.getNegativeEmotionRate() > 50) {
            suggestions.add("감정적 거래가 많습니다. 거래 전 명상이나 휴식을 취하세요.");
        }
        
        if (!report.getDetectedPatterns().isEmpty()) {
            suggestions.add("감지된 행동 패턴을 주의깊게 검토하고 개선 방안을 마련하세요.");
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("훌륭한 한 주였습니다! 현재의 접근 방식을 유지하세요.");
        }
        
        return suggestions;
    }
    
    /**
     * 주간 리포트 데이터 클래스
     */
    public static class WeeklyReport {
        private int totalTrades;
        private int winCount;
        private double winRate;
        private double totalPnL;
        private double bestTrade;
        private double worstTrade;
        private RiskMetrics riskMetrics;
        private List<BehaviorPattern> detectedPatterns;
        private double negativeEmotionRate;
        private List<String> improvementSuggestions;
        
        public WeeklyReport() {
            this.detectedPatterns = new ArrayList<>();
            this.improvementSuggestions = new ArrayList<>();
        }
        
        // Getters and Setters
        public int getTotalTrades() {
            return totalTrades;
        }
        
        public void setTotalTrades(int totalTrades) {
            this.totalTrades = totalTrades;
        }
        
        public int getWinCount() {
            return winCount;
        }
        
        public void setWinCount(int winCount) {
            this.winCount = winCount;
        }
        
        public double getWinRate() {
            return winRate;
        }
        
        public void setWinRate(double winRate) {
            this.winRate = winRate;
        }
        
        public double getTotalPnL() {
            return totalPnL;
        }
        
        public void setTotalPnL(double totalPnL) {
            this.totalPnL = totalPnL;
        }
        
        public double getBestTrade() {
            return bestTrade;
        }
        
        public void setBestTrade(double bestTrade) {
            this.bestTrade = bestTrade;
        }
        
        public double getWorstTrade() {
            return worstTrade;
        }
        
        public void setWorstTrade(double worstTrade) {
            this.worstTrade = worstTrade;
        }
        
        public RiskMetrics getRiskMetrics() {
            return riskMetrics;
        }
        
        public void setRiskMetrics(RiskMetrics riskMetrics) {
            this.riskMetrics = riskMetrics;
        }
        
        public List<BehaviorPattern> getDetectedPatterns() {
            return detectedPatterns;
        }
        
        public void setDetectedPatterns(List<BehaviorPattern> detectedPatterns) {
            this.detectedPatterns = detectedPatterns;
        }
        
        public double getNegativeEmotionRate() {
            return negativeEmotionRate;
        }
        
        public void setNegativeEmotionRate(double negativeEmotionRate) {
            this.negativeEmotionRate = negativeEmotionRate;
        }
        
        public List<String> getImprovementSuggestions() {
            return improvementSuggestions;
        }
        
        public void setImprovementSuggestions(List<String> improvementSuggestions) {
            this.improvementSuggestions = improvementSuggestions;
        }
    }
}

