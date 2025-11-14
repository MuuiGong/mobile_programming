package com.example.rsquare.util;

import com.example.rsquare.data.local.entity.Journal;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * 테스트 데이터 생성 유틸리티
 * 개발 및 테스트용 목 데이터 생성
 */
public class TestDataGenerator {
    
    private static final Random random = new Random();
    
    /**
     * 테스트 사용자 생성
     */
    public static User createTestUser() {
        User user = new User();
        user.setNickname("트레이더");
        user.setBalance(Constants.INITIAL_BALANCE);
        return user;
    }
    
    /**
     * 랜덤 포지션 생성
     */
    public static Position createRandomPosition(long userId, boolean closed) {
        Position position = new Position();
        position.setUserId(userId);
        
        String[] symbols = {"BTC", "ETH", "ADA", "SOL", "XRP"};
        position.setSymbol(symbols[random.nextInt(symbols.length)]);
        
        double entryPrice = 30000 + random.nextDouble() * 20000;
        position.setEntryPrice(entryPrice);
        position.setQuantity(0.1 + random.nextDouble() * 0.9);
        position.setLong(random.nextBoolean());
        
        if (position.isLong()) {
            position.setTakeProfit(entryPrice * (1.02 + random.nextDouble() * 0.03));
            position.setStopLoss(entryPrice * (0.98 - random.nextDouble() * 0.02));
        } else {
            position.setTakeProfit(entryPrice * (0.98 - random.nextDouble() * 0.02));
            position.setStopLoss(entryPrice * (1.02 + random.nextDouble() * 0.03));
        }
        
        if (closed) {
            position.setClosed(true);
            position.setCloseTime(new Date());
            
            // 랜덤 손익
            double pnl = (random.nextDouble() - 0.4) * 1000;
            position.setPnl(pnl);
            
            if (pnl > 0) {
                position.setClosedPrice(position.getTakeProfit());
            } else {
                position.setClosedPrice(position.getStopLoss());
            }
        }
        
        return position;
    }
    
    /**
     * 여러 테스트 포지션 생성
     */
    public static List<Position> createTestPositions(long userId, int count, boolean closed) {
        List<Position> positions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            positions.add(createRandomPosition(userId, closed));
        }
        return positions;
    }
    
    /**
     * 랜덤 저널 생성
     */
    public static Journal createRandomJournal(long positionId) {
        Journal journal = new Journal();
        journal.setPositionId(positionId);
        
        Journal.Emotion[] emotions = Journal.Emotion.values();
        journal.setEmotion(emotions[random.nextInt(emotions.length)]);
        
        String[] notes = {
            "신중하게 진입했다",
            "시장 분석을 충분히 했다",
            "약간 불안했지만 계획대로 진행",
            "리스크 관리를 철저히 했다",
            "감정적으로 진입했을 수도",
            "계획에 따라 실행했다"
        };
        journal.setNote(notes[random.nextInt(notes.length)]);
        
        return journal;
    }
}

