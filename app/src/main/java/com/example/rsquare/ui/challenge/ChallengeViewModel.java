package com.example.rsquare.ui.challenge;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rsquare.data.local.entity.Badge;
import com.example.rsquare.data.local.entity.Challenge;
import com.example.rsquare.data.repository.ChallengeRepository;

import java.util.List;

/**
 * Challenge ViewModel
 * 챌린지 및 뱃지 관리
 */
public class ChallengeViewModel extends AndroidViewModel {
    
    private final ChallengeRepository challengeRepository;
    
    private final LiveData<List<Challenge>> activeChallenges;
    private final LiveData<List<Challenge>> completedChallenges;
    private final LiveData<List<Badge>> userBadges;
    private final MutableLiveData<Challenge> selectedChallenge = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    public ChallengeViewModel(@NonNull Application application) {
        super(application);
        
        challengeRepository = new ChallengeRepository(application);
        
        activeChallenges = challengeRepository.getActiveChallenges(1);
        completedChallenges = challengeRepository.getCompletedChallenges(1);
        userBadges = challengeRepository.getUserBadges(1);
    }
    
    /**
     * 활성 챌린지 조회
     */
    public LiveData<List<Challenge>> getActiveChallenges() {
        return activeChallenges;
    }
    
    /**
     * 완료된 챌린지 조회
     */
    public LiveData<List<Challenge>> getCompletedChallenges() {
        return completedChallenges;
    }
    
    /**
     * 사용자 뱃지 조회
     */
    public LiveData<List<Badge>> getUserBadges() {
        return userBadges;
    }
    
    /**
     * 선택된 챌린지
     */
    public LiveData<Challenge> getSelectedChallenge() {
        return selectedChallenge;
    }
    
    /**
     * 성공 메시지
     */
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    /**
     * 챌린지 선택
     */
    public void selectChallenge(long challengeId) {
        challengeRepository.getChallengeById(challengeId).observeForever(challenge -> {
            selectedChallenge.setValue(challenge);
        });
    }
    
    /**
     * 챌린지 추가
     */
    public void addChallenge(Challenge challenge) {
        challengeRepository.addChallenge(challenge, challengeId -> {
            successMessage.postValue("새 챌린지가 추가되었습니다!");
        });
    }
    
    /**
     * 챌린지 진행률 업데이트
     */
    public void updateChallengeProgress(long challengeId, double progress) {
        challengeRepository.updateChallengeProgress(challengeId, progress);
        
        // 100% 달성 시 뱃지 부여
        if (progress >= 100) {
            awardBadge(challengeId);
        }
    }
    
    /**
     * 챌린지 완료 시 뱃지 부여
     */
    private void awardBadge(long challengeId) {
        challengeRepository.getActiveChallengeByType(1, null, challenge -> {
            if (challenge != null) {
                Badge badge = new Badge();
                badge.setUserId(1);
                badge.setBadgeType(Badge.BadgeType.CHALLENGE_HERO);
                badge.setName(challenge.getTitle() + " 완료");
                badge.setDescription(challenge.getDescription());
                
                challengeRepository.addBadge(badge, (badgeId, earnedBadge) -> {
                    successMessage.postValue("🎉 새 뱃지 획득: " + earnedBadge.getName());
                });
            }
        });
    }
    
    /**
     * 특정 타입의 활성 챌린지 진행률 증가
     */
    public void incrementChallengeProgress(String targetType, double increment) {
        challengeRepository.getActiveChallengeByType(1, targetType, challenge -> {
            if (challenge != null) {
                double newProgress = challenge.getProgress() + increment;
                challenge.updateProgress((newProgress / challenge.getTargetValue()) * 100);
                challengeRepository.updateChallenge(challenge);
                
                if (challenge.getStatus() == Challenge.Status.COMPLETED) {
                    successMessage.postValue("🎉 챌린지 완료: " + challenge.getTitle());
                    awardBadge(challenge.getId());
                }
            }
        });
    }
    
    /**
     * 뱃지 수 조회
     */
    public void getBadgeCount(OnCountLoadedListener listener) {
        challengeRepository.getBadgeCount(1, count -> {
            if (listener != null) {
                listener.onCountLoaded(count);
            }
        });
    }
    
    /**
     * 완료된 챌린지 수 조회
     */
    public void getCompletedChallengeCount(OnCountLoadedListener listener) {
        challengeRepository.getCompletedChallengeCount(1, count -> {
            if (listener != null) {
                listener.onCountLoaded(count);
            }
        });
    }
    
    public interface OnCountLoadedListener {
        void onCountLoaded(int count);
    }
    
    /**
     * 챌린지 로드 (이미 LiveData로 자동 업데이트됨)
     */
    public void loadChallenges() {
        // LiveData가 자동으로 업데이트되므로 별도 작업 불필요
    }
}

