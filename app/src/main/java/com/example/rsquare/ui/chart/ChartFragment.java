package com.example.rsquare.ui.chart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.rsquare.R;
import com.example.rsquare.data.local.entity.Position;
import com.example.rsquare.data.remote.model.CoinMarketChart;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Chart Fragment
 * WebView 기반 인터랙티브 차트 및 거래 UI
 */
public class ChartFragment extends Fragment implements ChartWebViewInterface.ChartCallback {
    
    private ChartViewModel viewModel;
    private WebView chartWebView;
    private EditText etQuantity, etEntryPrice, etTakeProfit, etStopLoss;
    private TextView tvRiskReward, tvSymbol, tvCurrentPrice;
    private LinearLayout togglePositionType;
    private MaterialButton btnLong, btnShort;
    private Button btnOpenPosition;
    private Spinner spinnerCoin;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart_tds, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(ChartViewModel.class);
        
        initViews(view);
        setupWebView();
        setupObservers();
        setupListeners();
        
        // 초기 데이터 로드
        viewModel.loadMarketData();
    }
    
    /**
     * View 초기화
     */
    private void initViews(View view) {
        chartWebView = view.findViewById(R.id.chart_webview);
        etQuantity = view.findViewById(R.id.et_quantity);
        etEntryPrice = view.findViewById(R.id.et_entry_price);
        etTakeProfit = view.findViewById(R.id.et_take_profit);
        etStopLoss = view.findViewById(R.id.et_stop_loss);
        tvRiskReward = view.findViewById(R.id.tv_risk_reward);
        tvSymbol = view.findViewById(R.id.tv_symbol);
        tvCurrentPrice = view.findViewById(R.id.tv_current_price);
        togglePositionType = view.findViewById(R.id.toggle_position_type);
        btnLong = view.findViewById(R.id.btn_long);
        btnShort = view.findViewById(R.id.btn_short);
        btnOpenPosition = view.findViewById(R.id.btn_open_position);
        spinnerCoin = view.findViewById(R.id.spinner_coin);
        
        // 초기 선택 상태 설정 (롱이 기본 선택)
        btnLong.setChecked(true);
    }
    
    /**
     * WebView 설정
     */
    private void setupWebView() {
        WebSettings webSettings = chartWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        
        // JavaScript Interface 추가
        chartWebView.addJavascriptInterface(new ChartWebViewInterface(this), "Android");
        
        // HTML 로드
        chartWebView.loadUrl("file:///android_asset/chart.html");
    }
    
    /**
     * Observer 설정
     */
    private void setupObservers() {
        // 차트 데이터 관찰
        viewModel.getChartData().observe(getViewLifecycleOwner(), chartData -> {
            if (chartData != null) {
                loadChartData(chartData);
            }
        });
        
        // 현재 가격 관찰
        viewModel.getCurrentPrice().observe(getViewLifecycleOwner(), price -> {
            if (price != null) {
                tvCurrentPrice.setText("$" + String.format(Locale.US, "%.2f", price));
                
                // 진입가가 설정되지 않았으면 현재 가격으로 설정
                if (etEntryPrice.getText().toString().isEmpty()) {
                    etEntryPrice.setText(String.valueOf(price));
                    callJavaScript("setEntryPrice(" + price + ")");
                }
            }
        });
        
        // R:R 비율 관찰
        viewModel.getRiskRewardRatio().observe(getViewLifecycleOwner(), rr -> {
            if (rr != null) {
                tvRiskReward.setText("R:R " + String.format(Locale.US, "%.2f", rr) + ":1");
                
                // 색상 설정 (TDS 색상 사용)
                if (rr >= 2.0) {
                    tvRiskReward.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_success));
                } else if (rr >= 1.0) {
                    tvRiskReward.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_warning));
                } else {
                    tvRiskReward.setTextColor(ContextCompat.getColor(requireContext(), R.color.tds_error));
                }
            }
        });
        
        // 선택된 코인 관찰
        viewModel.getSelectedCoinId().observe(getViewLifecycleOwner(), coinId -> {
            if (coinId != null) {
                tvSymbol.setText(coinId.toUpperCase());
                callJavaScript("setSymbol('" + coinId.toUpperCase() + "')");
            }
        });
        
        // 에러 메시지 관찰
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Listener 설정
     */
    private void setupListeners() {
        // 포지션 타입 토글
        btnLong.setOnClickListener(v -> {
            if (!btnLong.isChecked()) {
                btnLong.setChecked(true);
                btnShort.setChecked(false);
                viewModel.setIsLong(true);
                callJavaScript("setPositionType(true)");
            }
        });
        
        btnShort.setOnClickListener(v -> {
            if (!btnShort.isChecked()) {
                btnShort.setChecked(true);
                btnLong.setChecked(false);
                viewModel.setIsLong(false);
                callJavaScript("setPositionType(false)");
            }
        });
        
        // 가격 입력 리스너
        etEntryPrice.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateEntryPrice();
            }
        });
        
        etTakeProfit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateTakeProfit();
            }
        });
        
        etStopLoss.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateStopLoss();
            }
        });
        
        // 포지션 열기 버튼
        btnOpenPosition.setOnClickListener(v -> openPosition());
    }
    
    /**
     * 차트 데이터 로드
     */
    private void loadChartData(CoinMarketChart chartData) {
        try {
            JSONArray dataArray = new JSONArray();
            List<CoinMarketChart.PricePoint> points = chartData.getPricePoints();
            
            if (points != null && !points.isEmpty()) {
                for (CoinMarketChart.PricePoint point : points) {
                    JSONObject candle = new JSONObject();
                    candle.put("time", point.getTimestamp() / 1000); // 초 단위로 변환
                    candle.put("open", point.getPrice());
                    candle.put("high", point.getPrice() * 1.01);
                    candle.put("low", point.getPrice() * 0.99);
                    candle.put("close", point.getPrice());
                    dataArray.put(candle);
                }
                
                String dataString = dataArray.toString();
                callJavaScript("setChartData('" + dataString + "')");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 진입가 업데이트
     */
    private void updateEntryPrice() {
        try {
            double price = Double.parseDouble(etEntryPrice.getText().toString());
            viewModel.updateEntryPrice(price);
            callJavaScript("setEntryPrice(" + price + ")");
        } catch (NumberFormatException e) {
            // 잘못된 입력 무시
        }
    }
    
    /**
     * 익절가 업데이트
     */
    private void updateTakeProfit() {
        try {
            double price = Double.parseDouble(etTakeProfit.getText().toString());
            viewModel.updateTakeProfit(price);
            callJavaScript("setTakeProfit(" + price + ")");
        } catch (NumberFormatException e) {
            // 잘못된 입력 무시
        }
    }
    
    /**
     * 손절가 업데이트
     */
    private void updateStopLoss() {
        try {
            double price = Double.parseDouble(etStopLoss.getText().toString());
            viewModel.updateStopLoss(price);
            callJavaScript("setStopLoss(" + price + ")");
        } catch (NumberFormatException e) {
            // 잘못된 입력 무시
        }
    }
    
    /**
     * 포지션 열기
     */
    private void openPosition() {
        try {
            double quantity = Double.parseDouble(etQuantity.getText().toString());
            double entryPrice = Double.parseDouble(etEntryPrice.getText().toString());
            double takeProfit = Double.parseDouble(etTakeProfit.getText().toString());
            double stopLoss = Double.parseDouble(etStopLoss.getText().toString());
            
            Position position = new Position();
            position.setUserId(1);
            position.setSymbol(viewModel.getSelectedCoinId().getValue());
            position.setQuantity(quantity);
            position.setEntryPrice(entryPrice);
            position.setTakeProfit(takeProfit);
            position.setStopLoss(stopLoss);
            position.setLong(viewModel.getIsLong().getValue());
            
            // ViewModel을 통해 포지션 열기 (TradeViewModel 사용 필요)
            Toast.makeText(requireContext(), "포지션이 열렸습니다!", Toast.LENGTH_SHORT).show();
            
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "모든 필드를 올바르게 입력하세요", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * JavaScript 호출
     */
    private void callJavaScript(String script) {
        if (chartWebView != null) {
            chartWebView.post(() -> chartWebView.evaluateJavascript(script, null));
        }
    }
    
    // ChartCallback 구현
    @Override
    public void onPriceChanged(double price) {
        requireActivity().runOnUiThread(() -> {
            tvCurrentPrice.setText("$" + String.format(Locale.US, "%.2f", price));
        });
    }
    
    @Override
    public void onEntryPriceChanged(double price) {
        requireActivity().runOnUiThread(() -> {
            etEntryPrice.setText(String.valueOf(price));
            viewModel.updateEntryPrice(price);
        });
    }
    
    @Override
    public void onTakeProfitChanged(double price) {
        requireActivity().runOnUiThread(() -> {
            etTakeProfit.setText(String.valueOf(price));
            viewModel.updateTakeProfit(price);
        });
    }
    
    @Override
    public void onStopLossChanged(double price) {
        requireActivity().runOnUiThread(() -> {
            etStopLoss.setText(String.valueOf(price));
            viewModel.updateStopLoss(price);
        });
    }
}

