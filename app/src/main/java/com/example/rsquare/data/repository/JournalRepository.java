package com.example.rsquare.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.rsquare.data.local.AppDatabase;
import com.example.rsquare.data.local.dao.JournalDao;
import com.example.rsquare.data.local.entity.Journal;

import java.util.List;

/**
 * Journal Repository
 * 감정 기록 및 패턴 분석
 */
public class JournalRepository {
    
    private final JournalDao journalDao;
    
    public JournalRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.journalDao = database.journalDao();
    }
    
    /**
     * 모든 저널 조회
     */
    public LiveData<List<Journal>> getAllJournals() {
        return journalDao.getAllJournals();
    }
    
    /**
     * 포지션별 저널 조회
     */
    public LiveData<List<Journal>> getJournalsByPosition(long positionId) {
        return journalDao.getJournalsByPosition(positionId);
    }
    
    /**
     * 최근 저널 조회
     */
    public LiveData<List<Journal>> getRecentJournals(int limit) {
        return journalDao.getRecentJournals(limit);
    }
    
    /**
     * 저널 추가
     */
    public void addJournal(Journal journal, OnJournalAddedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long journalId = journalDao.insert(journal);
            if (listener != null) {
                listener.onJournalAdded(journalId);
            }
        });
    }
    
    /**
     * 저널 업데이트
     */
    public void updateJournal(Journal journal) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            journalDao.update(journal);
        });
    }
    
    /**
     * 특정 기간 이후 저널 조회
     */
    public void getJournalsSince(long startTime, OnJournalsLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Journal> journals = journalDao.getJournalsSince(startTime);
            if (listener != null) {
                listener.onJournalsLoaded(journals);
            }
        });
    }
    
    /**
     * 감정 분포 조회
     */
    public void getEmotionDistribution(long startTime, OnEmotionDistributionLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<JournalDao.EmotionCount> distribution = journalDao.getEmotionDistribution(startTime);
            if (listener != null) {
                listener.onEmotionDistributionLoaded(distribution);
            }
        });
    }
    
    /**
     * 포지션별 저널 조회 (동기)
     */
    public void getJournalsByPositionSync(long positionId, OnJournalsLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Journal> journals = journalDao.getJournalsByPositionSync(positionId);
            if (listener != null) {
                listener.onJournalsLoaded(journals);
            }
        });
    }
    
    public interface OnJournalAddedListener {
        void onJournalAdded(long journalId);
    }
    
    public interface OnJournalsLoadedListener {
        void onJournalsLoaded(List<Journal> journals);
    }
    
    public interface OnEmotionDistributionLoadedListener {
        void onEmotionDistributionLoaded(List<JournalDao.EmotionCount> distribution);
    }
}

