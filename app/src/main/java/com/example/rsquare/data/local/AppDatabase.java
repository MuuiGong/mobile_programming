package com.example.rsquare.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.rsquare.data.local.dao.BadgeDao;
import com.example.rsquare.data.local.dao.ChallengeDao;
import com.example.rsquare.data.local.dao.JournalDao;
import com.example.rsquare.data.local.dao.PositionDao;
import com.example.rsquare.data.local.dao.TradeHistoryDao;
import com.example.rsquare.data.local.dao.UserDao;
import com.example.rsquare.data.local.entity.Badge;
import com.example.rsquare.data.local.entity.Challenge;
import com.example.rsquare.data.local.entity.Journal;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.TradeHistory;
import com.example.rsquare.data.local.entity.User;
import com.example.rsquare.util.Constants;
import com.example.rsquare.util.DateConverter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room 데이터베이스 클래스
 * 싱글톤 패턴으로 구현
 */
@Database(
    entities = {
        User.class,
        Position.class,
        TradeHistory.class,
        Journal.class,
        Challenge.class,
        Badge.class
    },
    version = 1,
    exportSchema = true
)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    
    // DAO 추상 메서드
    public abstract UserDao userDao();
    public abstract PositionDao positionDao();
    public abstract TradeHistoryDao tradeHistoryDao();
    public abstract JournalDao journalDao();
    public abstract ChallengeDao challengeDao();
    public abstract BadgeDao badgeDao();
    
    // 싱글톤 인스턴스
    private static volatile AppDatabase INSTANCE;
    
    // 백그라운드 작업용 ExecutorService
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
        Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    
    /**
     * 데이터베이스 인스턴스 가져오기
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        Constants.DATABASE_NAME
                    )
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // 데이터베이스 생성 시 초기 데이터 삽입
                            databaseWriteExecutor.execute(() -> {
                                initializeData(INSTANCE);
                            });
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 초기 데이터 삽입
     */
    private static void initializeData(AppDatabase database) {
        // 기본 사용자 생성
        User user = new User();
        user.setNickname("트레이더");
        user.setBalance(Constants.INITIAL_BALANCE);
        
        long userId = database.userDao().insert(user);
        
        // 초기 챌린지 생성
        createInitialChallenges(database, userId);
    }
    
    /**
     * 초기 챌린지 생성
     */
    private static void createInitialChallenges(AppDatabase database, long userId) {
        // 첫 거래 챌린지
        Challenge firstTrade = new Challenge();
        firstTrade.setUserId(userId);
        firstTrade.setTitle("첫 거래 완료하기");
        firstTrade.setDescription("첫 번째 모의 거래를 완료하세요");
        firstTrade.setTargetValue(1);
        firstTrade.setTargetType(Constants.CHALLENGE_TYPE_CONSISTENT);
        firstTrade.setDifficulty(Challenge.Difficulty.EASY);
        database.challengeDao().insert(firstTrade);
        
        // 리스크 관리 챌린지
        Challenge riskManagement = new Challenge();
        riskManagement.setUserId(userId);
        riskManagement.setTitle("리스크 관리 마스터");
        riskManagement.setDescription("R:R 비율 2.0 이상으로 5회 거래");
        riskManagement.setTargetValue(5);
        riskManagement.setTargetType(Constants.CHALLENGE_TYPE_RR);
        riskManagement.setDifficulty(Challenge.Difficulty.MEDIUM);
        database.challengeDao().insert(riskManagement);
        
        // 3연승 챌린지
        Challenge winStreak = new Challenge();
        winStreak.setUserId(userId);
        winStreak.setTitle("3연승 달성");
        winStreak.setDescription("연속으로 3번 수익을 내세요");
        winStreak.setTargetValue(3);
        winStreak.setTargetType(Constants.CHALLENGE_TYPE_WIN_STREAK);
        winStreak.setDifficulty(Challenge.Difficulty.MEDIUM);
        database.challengeDao().insert(winStreak);
    }
}

