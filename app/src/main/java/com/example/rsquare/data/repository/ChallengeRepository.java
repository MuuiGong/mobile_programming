package com.example.rsquare.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.rsquare.data.local.AppDatabase;
import com.example.rsquare.data.local.dao.BadgeDao;
import com.example.rsquare.data.local.dao.ChallengeDao;
import com.example.rsquare.data.local.entity.Badge;
import com.example.rsquare.data.local.entity.Challenge;

import java.util.List;

/**
 * Challenge Repository
 * 챌린지 및 뱃지 관리
 */
public class ChallengeRepository {
    
    private final ChallengeDao challengeDao;
    private final BadgeDao badgeDao;
    
    public ChallengeRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.challengeDao = database.challengeDao();
        this.badgeDao = database.badgeDao();
    }
    
    /**
     * 활성 챌린지 조회
     */
    public LiveData<List<Challenge>> getActiveChallenges(long userId) {
        return challengeDao.getActiveChallenges(userId);
    }
    
    /**
     * 완료된 챌린지 조회
     */
    public LiveData<List<Challenge>> getCompletedChallenges(long userId) {
        return challengeDao.getCompletedChallenges(userId);
    }
    
    /**
     * 모든 챌린지 조회
     */
    public LiveData<List<Challenge>> getAllChallenges(long userId) {
        return challengeDao.getAllChallenges(userId);
    }
    
    /**
     * 챌린지 ID로 조회
     */
    public LiveData<Challenge> getChallengeById(long challengeId) {
        return challengeDao.getChallengeById(challengeId);
    }
    
    /**
     * 챌린지 추가
     */
    public void addChallenge(Challenge challenge, OnChallengeAddedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long challengeId = challengeDao.insert(challenge);
            if (listener != null) {
                listener.onChallengeAdded(challengeId);
            }
        });
    }
    
    /**
     * 챌린지 진행률 업데이트
     */
    public void updateChallengeProgress(long challengeId, double progress) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            challengeDao.updateProgress(challengeId, progress);
            
            // 100% 달성 시 상태 변경
            if (progress >= 100) {
                challengeDao.updateStatus(challengeId, Challenge.Status.COMPLETED, 
                    System.currentTimeMillis());
            }
        });
    }
    
    /**
     * 챌린지 상태 업데이트
     */
    public void updateChallengeStatus(long challengeId, Challenge.Status status) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Long completedAt = (status == Challenge.Status.COMPLETED) ? 
                System.currentTimeMillis() : null;
            challengeDao.updateStatus(challengeId, status, completedAt);
        });
    }
    
    /**
     * 챌린지 업데이트
     */
    public void updateChallenge(Challenge challenge) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            challengeDao.update(challenge);
        });
    }
    
    /**
     * 타입별 활성 챌린지 조회
     */
    public void getActiveChallengeByType(long userId, String targetType, 
                                         OnChallengeLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Challenge challenge = challengeDao.getActiveChallengeByType(userId, targetType);
            if (listener != null) {
                listener.onChallengeLoaded(challenge);
            }
        });
    }
    
    /**
     * 완료된 챌린지 수 조회
     */
    public void getCompletedChallengeCount(long userId, OnCountLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = challengeDao.getCompletedChallengeCount(userId);
            if (listener != null) {
                listener.onCountLoaded(count);
            }
        });
    }
    
    // Badge 관련 메서드
    
    /**
     * 사용자 뱃지 조회
     */
    public LiveData<List<Badge>> getUserBadges(long userId) {
        return badgeDao.getUserBadges(userId);
    }
    
    /**
     * 뱃지 추가
     */
    public void addBadge(Badge badge, OnBadgeEarnedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 이미 획득한 뱃지인지 확인
            Badge existing = badgeDao.getBadgeByType(badge.getUserId(), badge.getBadgeType());
            if (existing == null) {
                long badgeId = badgeDao.insert(badge);
                if (listener != null) {
                    listener.onBadgeEarned(badgeId, badge);
                }
            }
        });
    }
    
    /**
     * 최근 뱃지 조회
     */
    public LiveData<List<Badge>> getRecentBadges(long userId, int limit) {
        return badgeDao.getRecentBadges(userId, limit);
    }
    
    /**
     * 뱃지 수 조회
     */
    public void getBadgeCount(long userId, OnCountLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = badgeDao.getBadgeCount(userId);
            if (listener != null) {
                listener.onCountLoaded(count);
            }
        });
    }
    
    public interface OnChallengeAddedListener {
        void onChallengeAdded(long challengeId);
    }
    
    public interface OnChallengeLoadedListener {
        void onChallengeLoaded(Challenge challenge);
    }
    
    public interface OnBadgeEarnedListener {
        void onBadgeEarned(long badgeId, Badge badge);
    }
    
    public interface OnCountLoadedListener {
        void onCountLoaded(int count);
    }
}

