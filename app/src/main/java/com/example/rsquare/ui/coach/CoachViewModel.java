package com.example.rsquare.ui.coach;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.rsquare.data.local.entity.Challenge;
import com.example.rsquare.data.local.entity.Journal;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.repository.ChallengeRepository;
import com.example.rsquare.data.repository.JournalRepository;
import com.example.rsquare.data.repository.TradingRepository;
import com.example.rsquare.domain.BehaviorPattern;
import com.example.rsquare.domain.CoachingEngine;
import com.example.rsquare.domain.CoachingMessage;
import com.example.rsquare.domain.RiskCalculator;
import com.example.rsquare.domain.RiskMetrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Coach ViewModel
 * AI 코치 피드백 및 행동 패턴 분석
 */
public class CoachViewModel extends AndroidViewModel {
    
    private final TradingRepository tradingRepository;
    private final JournalRepository journalRepository;
    private final ChallengeRepository challengeRepository;
    
    private final MutableLiveData<List<CoachingMessage>> coachingMessages = new MutableLiveData<>();
    private final MutableLiveData<List<BehaviorPattern>> behaviorPatterns = new MutableLiveData<>();
    private final MutableLiveData<CoachingEngine.WeeklyReport> weeklyReport = new MutableLiveData<>();
    private final MutableLiveData<Challenge> recommendedChallenge = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    
    public CoachViewModel(@NonNull Application application) {
        super(application);
        
        tradingRepository = new TradingRepository(application);
        journalRepository = new JournalRepository(application);
        challengeRepository = new ChallengeRepository(application);
        
        // 초기 분석 실행
        analyzeTradingSession();
    }
    
    /**
     * 거래 세션 분석
     */
    public void analyzeTradingSession() {
        loading.setValue(true);
        
        // 최근 30일 데이터 조회
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        tradingRepository.getPositionsSince(1, thirtyDaysAgo, positions -> {
            journalRepository.getJournalsSince(thirtyDaysAgo, journals -> {
                // 종료된 포지션만 필터링
                List<Position> closedPositions = new ArrayList<>();
                for (Position position : positions) {
                    if (position.isClosed()) {
                        closedPositions.add(position);
                    }
                }
                
                // 리스크 메트릭스 계산
                RiskMetrics metrics = RiskCalculator.calculateRiskScore(closedPositions);
                
                // 코칭 메시지 생성
                List<CoachingMessage> messages = CoachingEngine.analyzeTradingSession(
                    positions, journals, metrics
                );
                coachingMessages.postValue(messages);
                
                // 행동 패턴 분석
                List<BehaviorPattern> patterns = com.example.rsquare.domain.BehaviorAnalyzer.analyzeAllPatterns(
                    closedPositions, journals
                );
                behaviorPatterns.postValue(patterns);
                
                // 챌린지 추천
                Challenge recommended = CoachingEngine.recommendChallenge(1, patterns, metrics);
                recommendedChallenge.postValue(recommended);
                
                loading.postValue(false);
            });
        });
    }
    
    /**
     * 주간 리포트 생성
     */
    public void generateWeeklyReport() {
        loading.setValue(true);
        
        long weekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        
        tradingRepository.getPositionsSince(1, weekAgo, positions -> {
            journalRepository.getJournalsSince(weekAgo, journals -> {
                // 종료된 포지션만 필터링
                List<Position> closedPositions = new ArrayList<>();
                for (Position position : positions) {
                    if (position.isClosed()) {
                        closedPositions.add(position);
                    }
                }
                
                // 리스크 메트릭스 계산
                RiskMetrics metrics = RiskCalculator.calculateRiskScore(closedPositions);
                
                // 주간 리포트 생성
                CoachingEngine.WeeklyReport report = CoachingEngine.generateWeeklyReport(
                    closedPositions, journals, metrics
                );
                weeklyReport.postValue(report);
                
                loading.postValue(false);
            });
        });
    }
    
    /**
     * 추천 챌린지 수락
     */
    public void acceptRecommendedChallenge() {
        Challenge challenge = recommendedChallenge.getValue();
        if (challenge != null) {
            challengeRepository.addChallenge(challenge, challengeId -> {
                coachingMessages.postValue(java.util.Collections.singletonList(
                    new CoachingMessage("챌린지가 추가되었습니다! 목표를 향해 도전해보세요.", 
                        CoachingMessage.MessageType.POSITIVE)
                ));
            });
        }
    }
    
    // Getters
    public MutableLiveData<List<CoachingMessage>> getCoachingMessages() {
        return coachingMessages;
    }
    
    public MutableLiveData<List<BehaviorPattern>> getBehaviorPatterns() {
        return behaviorPatterns;
    }
    
    public MutableLiveData<CoachingEngine.WeeklyReport> getWeeklyReport() {
        return weeklyReport;
    }
    
    public MutableLiveData<Challenge> getRecommendedChallenge() {
        return recommendedChallenge;
    }
    
    public MutableLiveData<Boolean> getLoading() {
        return loading;
    }
}

