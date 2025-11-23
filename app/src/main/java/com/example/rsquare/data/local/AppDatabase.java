package com.example.rsquare.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
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
import com.example.rsquare.data.local.dao.UserSettingsDao;
import com.example.rsquare.data.local.entity.Badge;
import com.example.rsquare.data.local.entity.Challenge;
import com.example.rsquare.data.local.entity.Journal;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.local.entity.TradeHistory;
import com.example.rsquare.data.local.entity.User;
import com.example.rsquare.data.local.entity.UserSettings;
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
        Badge.class,
        UserSettings.class
    },
    version = 4,
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
    public abstract UserSettingsDao userSettingsDao();
    
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
                    .addMigrations(
                        // Migration 1 -> 2: 새 컬럼 추가
                        new androidx.room.migration.Migration(1, 2) {
                            @Override
                            public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase database) {
                                // Position 테이블에 새 컬럼 추가
                                database.execSQL("ALTER TABLE positions ADD COLUMN tradeType TEXT DEFAULT 'SPOT'");
                                database.execSQL("ALTER TABLE positions ADD COLUMN leverage INTEGER DEFAULT 1");
                                database.execSQL("ALTER TABLE positions ADD COLUMN riskAmount REAL DEFAULT 0");
                                database.execSQL("ALTER TABLE positions ADD COLUMN timeframe TEXT DEFAULT '1H'");
                                database.execSQL("ALTER TABLE positions ADD COLUMN exitReason TEXT");
                                database.execSQL("ALTER TABLE positions ADD COLUMN maxDrawdown REAL DEFAULT 0");
                                database.execSQL("ALTER TABLE positions ADD COLUMN rrRatio REAL DEFAULT 0");
                                
                                // UserSettings 테이블 생성
                                database.execSQL("CREATE TABLE IF NOT EXISTS user_settings (" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "userId INTEGER NOT NULL, " +
                                    "initialCapital REAL NOT NULL DEFAULT 10000.0, " +
                                    "tradeMode TEXT DEFAULT 'FUTURES', " +
                                    "defaultLeverage INTEGER NOT NULL DEFAULT 1, " +
                                    "useFixedRiskAmount INTEGER NOT NULL DEFAULT 0, " +
                                    "fixedRiskAmount REAL NOT NULL DEFAULT 100.0, " +
                                    "riskPercentage REAL NOT NULL DEFAULT 2.0, " +
                                    "maxPositions INTEGER NOT NULL DEFAULT 3, " +
                                    "maxLossPerTrade REAL NOT NULL DEFAULT 5.0, " +
                                    "dailyLossLimit REAL NOT NULL DEFAULT 10.0, " +
                                    "maxPositionDuration TEXT DEFAULT 'UNLIMITED', " +
                                    "defaultSymbol TEXT DEFAULT 'BTCUSDT', " +
                                    "availableSymbols TEXT DEFAULT '[\"BTCUSDT\",\"ETHUSDT\"]', " +
                                    "defaultTimeframe TEXT DEFAULT '1H', " +
                                    "chartType TEXT DEFAULT 'Candlestick', " +
                                    "createdAt INTEGER, " +
                                    "updatedAt INTEGER, " +
                                    "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE)");
                                
                                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_settings_userId ON user_settings(userId)");
                            }
                        },
                        // Migration 2 -> 3: NOT NULL 제약조건 수정
                        new androidx.room.migration.Migration(2, 3) {
                            @Override
                            public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase database) {
                                // 1. positions 테이블 재생성 (NOT NULL 제약조건 수정)
                                database.execSQL("CREATE TABLE positions_new (" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "userId INTEGER NOT NULL, " +
                                    "symbol TEXT, " +
                                    "quantity REAL NOT NULL, " +
                                    "entryPrice REAL NOT NULL, " +
                                    "takeProfit REAL NOT NULL, " +
                                    "stopLoss REAL NOT NULL, " +
                                    "isLong INTEGER NOT NULL, " +
                                    "openTime INTEGER, " +
                                    "closeTime INTEGER, " +
                                    "isClosed INTEGER NOT NULL, " +
                                    "pnl REAL NOT NULL, " +
                                    "closedPrice REAL, " +
                                    "tradeType TEXT DEFAULT 'SPOT', " +
                                    "leverage INTEGER NOT NULL DEFAULT 1, " +
                                    "riskAmount REAL NOT NULL DEFAULT 0, " +
                                    "timeframe TEXT DEFAULT '1H', " +
                                    "exitReason TEXT, " +
                                    "maxDrawdown REAL NOT NULL DEFAULT 0, " +
                                    "rrRatio REAL NOT NULL DEFAULT 0, " +
                                    "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE)");
                                
                                database.execSQL("INSERT INTO positions_new SELECT " +
                                    "id, userId, symbol, quantity, entryPrice, takeProfit, stopLoss, " +
                                    "isLong, openTime, closeTime, isClosed, pnl, closedPrice, " +
                                    "COALESCE(tradeType, 'SPOT'), " +
                                    "COALESCE(leverage, 1), " +
                                    "COALESCE(riskAmount, 0), " +
                                    "COALESCE(timeframe, '1H'), " +
                                    "exitReason, " +
                                    "COALESCE(maxDrawdown, 0), " +
                                    "COALESCE(rrRatio, 0) " +
                                    "FROM positions");
                                
                                database.execSQL("DROP TABLE positions");
                                database.execSQL("ALTER TABLE positions_new RENAME TO positions");
                                database.execSQL("CREATE INDEX IF NOT EXISTS index_positions_userId ON positions(userId)");
                                database.execSQL("CREATE INDEX IF NOT EXISTS index_positions_isClosed ON positions(isClosed)");
                                
                                // 2. user_settings 테이블 재생성 (nullable 필드 수정)
                                database.execSQL("CREATE TABLE user_settings_new (" +
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "userId INTEGER NOT NULL, " +
                                    "initialCapital REAL NOT NULL DEFAULT 10000.0, " +
                                    "tradeMode TEXT DEFAULT 'FUTURES', " +
                                    "defaultLeverage INTEGER NOT NULL DEFAULT 1, " +
                                    "useFixedRiskAmount INTEGER NOT NULL DEFAULT 0, " +
                                    "fixedRiskAmount REAL NOT NULL DEFAULT 100.0, " +
                                    "riskPercentage REAL NOT NULL DEFAULT 2.0, " +
                                    "maxPositions INTEGER NOT NULL DEFAULT 3, " +
                                    "maxLossPerTrade REAL NOT NULL DEFAULT 5.0, " +
                                    "dailyLossLimit REAL NOT NULL DEFAULT 10.0, " +
                                    "maxPositionDuration TEXT DEFAULT 'UNLIMITED', " +
                                    "defaultSymbol TEXT DEFAULT 'BTCUSDT', " +
                                    "availableSymbols TEXT DEFAULT '[\"BTCUSDT\",\"ETHUSDT\"]', " +
                                    "defaultTimeframe TEXT DEFAULT '1H', " +
                                    "chartType TEXT DEFAULT 'Candlestick', " +
                                    "createdAt INTEGER, " +
                                    "updatedAt INTEGER, " +
                                    "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE)");
                                
                                database.execSQL("INSERT INTO user_settings_new SELECT " +
                                    "id, userId, initialCapital, tradeMode, defaultLeverage, " +
                                    "useFixedRiskAmount, fixedRiskAmount, riskPercentage, maxPositions, " +
                                    "maxLossPerTrade, dailyLossLimit, maxPositionDuration, defaultSymbol, " +
                                    "availableSymbols, defaultTimeframe, chartType, createdAt, updatedAt " +
                                    "FROM user_settings");
                                
                                database.execSQL("DROP TABLE user_settings");
                                database.execSQL("ALTER TABLE user_settings_new RENAME TO user_settings");
                                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_settings_userId ON user_settings(userId)");
                            }
                        },
                        // Migration 3 -> 4: marginMode 필드 추가
                        new androidx.room.migration.Migration(3, 4) {
                            @Override
                            public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase database) {
                                // Position 테이블에 marginMode 컬럼 추가
                                database.execSQL("ALTER TABLE positions ADD COLUMN marginMode TEXT DEFAULT 'CROSS'");
                                
                                // UserSettings 테이블에 defaultMarginMode 컬럼 추가
                                database.execSQL("ALTER TABLE user_settings ADD COLUMN defaultMarginMode TEXT DEFAULT 'CROSS'");
                            }
                        }
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

