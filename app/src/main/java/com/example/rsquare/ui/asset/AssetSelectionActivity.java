package com.example.rsquare.ui.asset;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.ui.adapter.AssetCardAdapter;
import com.example.rsquare.ui.trading.TradingActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 자산·모드 선택 화면 Activity
 */
public class AssetSelectionActivity extends AppCompatActivity {
    
    private androidx.cardview.widget.CardView cardFutures;
    private androidx.cardview.widget.CardView cardStock;
    private RecyclerView assetRecycler;
    private Button btnStartTrading;
    
    private String selectedMode = null; // "FUTURES" or "STOCK"
    private Position selectedAsset = null;
    
    private AssetCardAdapter adapter;
    
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
    }
    
    private void setupListeners() {
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
        
        // 주식 모의투자 모드 선택
        cardStock.setOnClickListener(v -> {
            selectedMode = "STOCK";
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
        } else if ("STOCK".equals(selectedMode)) {
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
        Toast.makeText(this, selectedMode.equals("FUTURES") ? "코인 선물 모드 선택됨" : "주식 모의투자 모드 선택됨", 
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
    
    private void loadAssets() {
        // 샘플 자산 데이터 생성
        List<Position> assets = new ArrayList<>();
        
        // 코인 선물 모드인 경우
        if ("FUTURES".equals(selectedMode)) {
            Position btc = new Position();
            btc.setSymbol("BTCUSDT");
            btc.setEntryPrice(95836.0);
            assets.add(btc);
            
            Position eth = new Position();
            eth.setSymbol("ETHUSDT");
            eth.setEntryPrice(3250.0);
            assets.add(eth);
            
            Position ada = new Position();
            ada.setSymbol("ADAUSDT");
            ada.setEntryPrice(0.45);
            assets.add(ada);
        } 
        // 주식 모드인 경우
        else if ("STOCK".equals(selectedMode)) {
            Position apple = new Position();
            apple.setSymbol("AAPL");
            apple.setEntryPrice(175.50);
            assets.add(apple);
            
            Position tesla = new Position();
            tesla.setSymbol("TSLA");
            tesla.setEntryPrice(245.30);
            assets.add(tesla);
            
            Position msft = new Position();
            msft.setSymbol("MSFT");
            msft.setEntryPrice(380.20);
            assets.add(msft);
        }
        
        adapter.setAssets(assets);
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
