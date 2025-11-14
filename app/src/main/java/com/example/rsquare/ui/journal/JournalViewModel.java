package com.example.rsquare.ui.journal;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rsquare.data.local.entity.Journal;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.repository.JournalRepository;
import com.example.rsquare.data.repository.TradingRepository;

import java.util.List;

/**
 * Journal ViewModel
 * 거래 기록 및 감정 저널 관리
 */
public class JournalViewModel extends AndroidViewModel {
    
    private final JournalRepository journalRepository;
    private final TradingRepository tradingRepository;
    
    private final LiveData<List<Journal>> allJournals;
    private final LiveData<List<Position>> allPositions;
    private final MutableLiveData<List<Journal>> selectedPositionJournals = new MutableLiveData<>();
    private final MutableLiveData<Position> selectedPosition = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    public JournalViewModel(@NonNull Application application) {
        super(application);
        
        journalRepository = new JournalRepository(application);
        tradingRepository = new TradingRepository(application);
        
        allJournals = journalRepository.getAllJournals();
        allPositions = tradingRepository.getAllPositions(1);
    }
    
    /**
     * 모든 저널 조회
     */
    public LiveData<List<Journal>> getAllJournals() {
        return allJournals;
    }
    
    /**
     * 모든 포지션 조회
     */
    public LiveData<List<Position>> getAllPositions() {
        return allPositions;
    }
    
    /**
     * 선택된 포지션의 저널
     */
    public LiveData<List<Journal>> getSelectedPositionJournals() {
        return selectedPositionJournals;
    }
    
    /**
     * 선택된 포지션
     */
    public LiveData<Position> getSelectedPosition() {
        return selectedPosition;
    }
    
    /**
     * 성공 메시지
     */
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    /**
     * 포지션 선택
     */
    public void selectPosition(long positionId) {
        tradingRepository.getPositionById(positionId).observeForever(position -> {
            selectedPosition.setValue(position);
        });
        
        journalRepository.getJournalsByPositionSync(positionId, journals -> {
            selectedPositionJournals.postValue(journals);
        });
    }
    
    /**
     * 저널 추가
     */
    public void addJournal(Journal journal) {
        journalRepository.addJournal(journal, journalId -> {
            successMessage.postValue("저널이 추가되었습니다");
            
            // 선택된 포지션의 저널 새로고침
            if (selectedPosition.getValue() != null) {
                selectPosition(selectedPosition.getValue().getId());
            }
        });
    }
    
    /**
     * 저널 업데이트
     */
    public void updateJournal(Journal journal) {
        journalRepository.updateJournal(journal);
        successMessage.setValue("저널이 업데이트되었습니다");
    }
    
    /**
     * 최근 일주일 저널 조회
     */
    public void getRecentWeekJournals(OnJournalsLoadedListener listener) {
        long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        journalRepository.getJournalsSince(weekAgo, journals -> {
            if (listener != null) {
                listener.onJournalsLoaded(journals);
            }
        });
    }
    
    /**
     * 감정 분포 조회
     */
    public void getEmotionDistribution(OnEmotionDistributionLoadedListener listener) {
        long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        journalRepository.getEmotionDistribution(weekAgo, distribution -> {
            if (listener != null) {
                listener.onDistributionLoaded(distribution);
            }
        });
    }
    
    public interface OnJournalsLoadedListener {
        void onJournalsLoaded(List<Journal> journals);
    }
    
    public interface OnEmotionDistributionLoadedListener {
        void onDistributionLoaded(List<com.example.rsquare.data.local.dao.JournalDao.EmotionCount> distribution);
    }
}

