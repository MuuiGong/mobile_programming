package com.example.rsquare.ui.asset;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.rsquare.ui.BaseActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.ui.adapter.AssetCardAdapter;
import com.example.rsquare.ui.trading.TradingActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 자산·모드 선택 화면 Activity
 */
public class AssetSelectionActivity extends BaseActivity {
    
    private androidx.cardview.widget.CardView cardFutures;
    private androidx.cardview.widget.CardView cardStock;
    private RecyclerView assetRecycler;
    private Button btnStartTrading;
    
    private String selectedMode = null; // "FUTURES" or "SPOT"
    private Position selectedAsset = null;
    
    private AssetCardAdapter adapter;
    
    private android.widget.ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_selection);
        
        initViews();
        setupListeners();
        setupRecyclerView();
        loadAssets();
    }
    
    private void initViews() {
        View futuresView = findViewById(R.id.card_futures);
        View stockView = findViewById(R.id.card_stock);
        
        if (futuresView instanceof CardView) {
            cardFutures = (CardView) futuresView;
        } else {
            throw new IllegalStateException("card_futures must be a CardView");
        }
        
        if (stockView instanceof CardView) {
            cardStock = (CardView) stockView;
        } else {
            throw new IllegalStateException("card_stock must be a CardView");
        }
        
        assetRecycler = findViewById(R.id.asset_recycler);
        btnStartTrading = findViewById(R.id.bottom_action);
        btnBack = findViewById(R.id.btn_back);
    }
    
    private void setupListeners() {
        // 뒤로가기 버튼
        btnBack.setOnClickListener(v -> finish());

        // CardView를 클릭 가능하게 설정
        cardFutures.setClickable(true);
        cardFutures.setFocusable(true);
        cardStock.setClickable(true);
        cardStock.setFocusable(true);
        
        // 코인 선물 모드 선택
        cardFutures.setOnClickListener(v -> {
            selectedMode = "FUTURES";
            updateModeSelection();
        });
        
        // 코인 현물 모드 선택
        cardStock.setOnClickListener(v -> {
            selectedMode = "SPOT";
            updateModeSelection();
        });
        
        // 트레이딩 시작 버튼
        btnStartTrading.setOnClickListener(v -> {
            if (selectedMode == null) {
                Toast.makeText(this, "거래 모드를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedAsset == null) {
                Toast.makeText(this, "자산을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent(this, TradingActivity.class);
            intent.putExtra("mode", selectedMode);
            intent.putExtra("symbol", selectedAsset.getSymbol());
            startActivity(intent);
        });
    }
    
    private void updateModeSelection() {
        // 카드 선택 상태 업데이트
        if ("FUTURES".equals(selectedMode)) {
            cardFutures.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                getColor(R.color.tds_blue_400)));
            cardStock.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                getColor(R.color.tds_bg_dark)));
        } else if ("SPOT".equals(selectedMode)) {
            cardFutures.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                getColor(R.color.tds_bg_dark)));
            cardStock.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                getColor(R.color.tds_blue_400)));
        }
        
        // 자산 목록 업데이트
        loadAssets();
        
        // 버튼 활성화 확인
        updateStartButton();
        
        // 선택 피드백
        Toast.makeText(this, selectedMode.equals("FUTURES") ? "코인 선물 모드 선택됨" : "코인 현물 모드 선택됨", 
            Toast.LENGTH_SHORT).show();
    }
    
    private void setupRecyclerView() {
        assetRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssetCardAdapter();
        adapter.setOnAssetClickListener(asset -> {
            selectedAsset = asset;
            updateStartButton();
            Toast.makeText(this, asset.getSymbol() + " 선택됨", Toast.LENGTH_SHORT).show();
        });
        assetRecycler.setAdapter(adapter);
    }
    
    private com.example.rsquare.data.repository.MarketDataRepository repository;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.stopWebSocket();
        }
    }

    private void loadAssets() {
        // 로딩 표시 (필요시 추가)
        
        if (repository == null) {
            repository = new com.example.rsquare.data.repository.MarketDataRepository();
        }
        
        // 1. 유효한 Binance 심볼 목록 먼저 조회
        repository.getValidBinanceSymbols(new com.example.rsquare.data.repository.MarketDataRepository.OnValidSymbolsLoadedListener() {
            @Override
            public void onValidSymbolsLoaded(Set<String> validSymbols) {
                if (isFinishing() || isDestroyed()) return;
                
                // 2. 코인 목록 조회
                repository.getTopCoins(50, new com.example.rsquare.data.repository.MarketDataRepository.OnMarketDataLoadedListener() {
                    @Override
                    public void onMarketDataLoaded(List<com.example.rsquare.data.remote.model.CoinPrice> prices) {
                        if (isFinishing() || isDestroyed()) return;

                        List<Position> assets = new ArrayList<>();
                        List<String> symbols = new ArrayList<>();
                        
                        for (com.example.rsquare.data.remote.model.CoinPrice price : prices) {
                            // 심볼 변환 (예: btc -> BTCUSDT)
                            String symbol = price.getSymbol().toUpperCase();
                            
                            // 스테이블 코인 제외
                            if (symbol.equals("USDT") || symbol.equals("USDC") || 
                                symbol.equals("DAI") || symbol.equals("FDUSD")) {
                                continue;
                            }
                            
                            if (!symbol.endsWith("USDT")) {
                                symbol += "USDT";
                            }
                            
                            // Binance에서 거래 가능한지 확인
                            if (validSymbols != null && !validSymbols.contains(symbol)) {
                                // android.util.Log.d("AssetSelection", "Skipping unsupported symbol: " + symbol);
                                continue;
                            }
                            
                            Position asset = new Position();
                            asset.setSymbol(symbol);
                            asset.setEntryPrice(price.getCurrentPrice());
                            asset.setLogoUrl(price.getImage());
                            
                            assets.add(asset);
                            symbols.add(symbol);
                        }
                        
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;

                            if (adapter != null) {
                                // 초기 가격 변동률 맵 생성
                                java.util.Map<String, Double> initialChanges = new java.util.HashMap<>();
                                for (com.example.rsquare.data.remote.model.CoinPrice price : prices) {
                                    String symbol = price.getSymbol().toUpperCase();
                                    if (!symbol.endsWith("USDT")) symbol += "USDT";
                                    // 필터링된 심볼만 추가
                                    if (symbols.contains(symbol)) {
                                        initialChanges.put(symbol, price.getPriceChangePercentage24h());
                                    }
                                }
                                
                                adapter.setInitialPrices(initialChanges);
                                adapter.setAssets(assets);
                            }
                            
                            // 실시간 업데이트 구독
                            if (!symbols.isEmpty()) {
                                repository.subscribeToRealtimeUpdates(symbols, new com.example.rsquare.data.repository.MarketDataRepository.OnRealtimeUpdateListener() {
                                    // Throttling을 위한 변수들
                                    private final java.util.Map<String, Double> pendingPrices = new java.util.concurrent.ConcurrentHashMap<>();
                                    private final java.util.Map<String, Double> pendingChanges = new java.util.concurrent.ConcurrentHashMap<>();
                                    private final android.os.Handler updateHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                                    private static final long UPDATE_INTERVAL_MS = 500; // 0.5초마다 업데이트
                                    private final java.util.concurrent.atomic.AtomicBoolean isUpdateScheduled = new java.util.concurrent.atomic.AtomicBoolean(false);
                                    
                                    private final Runnable updateRunnable = new Runnable() {
                                        @Override
                                        public void run() {
                                            if (isFinishing() || isDestroyed()) {
                                                isUpdateScheduled.set(false);
                                                return;
                                            }

                                            if (adapter != null) {
                                                for (String symbol : pendingPrices.keySet()) {
                                                    Double price = pendingPrices.get(symbol);
                                                    Double change = pendingChanges.get(symbol);
                                                    if (price != null && change != null) {
                                                        adapter.updatePrice(symbol, price, change);
                                                    }
                                                }
                                                pendingPrices.clear();
                                                pendingChanges.clear();
                                            }
                                            isUpdateScheduled.set(false);
                                        }
                                    };

                                    @Override
                                    public void onRealtimeUpdate(String coinId, double price, double changePercent) {
                                        if (isFinishing() || isDestroyed()) return;

                                        String symbol = coinId.toUpperCase();
                                        if (!symbol.endsWith("USDT")) symbol += "USDT";
                                        
                                        pendingPrices.put(symbol, price);
                                        pendingChanges.put(symbol, changePercent);
                                        
                                        if (isUpdateScheduled.compareAndSet(false, true)) {
                                            updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
                                        }
                                    }
                                });
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        if (isFinishing() || isDestroyed()) return;
                        runOnUiThread(() -> {
                            Toast.makeText(AssetSelectionActivity.this, 
                                "자산 목록 로드 실패: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                if (isFinishing() || isDestroyed()) return;
                runOnUiThread(() -> {
                    Toast.makeText(AssetSelectionActivity.this, 
                        "거래소 정보 로드 실패: " + error, Toast.LENGTH_SHORT).show();
                    // 실패해도 일단 코인 목록은 로드 시도 (필터링 없이)
                    // ... (fallback logic could be added here, but keeping it simple for now)
                });
            }
        });
    }
    
    private void updateStartButton() {
        btnStartTrading.setEnabled(selectedMode != null && selectedAsset != null);
        if (btnStartTrading.isEnabled()) {
            btnStartTrading.setAlpha(1.0f);
        } else {
            btnStartTrading.setAlpha(0.5f);
        }
    }
}
