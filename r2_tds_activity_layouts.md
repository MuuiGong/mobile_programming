# R² Android TDS Design System XML Layouts

## 📌 TDS 다크 모드 색상 체계

```xml
<!-- res/values-night/colors.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Background -->
    <color name="tds_bg_black">#050A0E</color>      <!-- Deep Dark -->
    <color name="tds_bg_dark">#0D1117</color>       <!-- Card Dark -->
    <color name="tds_bg_neutral">#1A1F26</color>    <!-- Neutral -->
    
    <!-- Text -->
    <color name="tds_text_primary">#FFFFFF</color>
    <color name="tds_text_secondary">#9CA3AF</color>
    <color name="tds_text_tertiary">#6B7280</color>
    
    <!-- Semantic -->
    <color name="tds_success">#10B981</color>       <!-- Green -->
    <color name="tds_error">#EF4444</color>         <!-- Red -->
    <color name="tds_warning">#F59E0B</color>       <!-- Yellow -->
    <color name="tds_info">#3B82F6</color>          <!-- Blue -->
    
    <!-- Risk Score -->
    <color name="risk_safe">#10B981</color>         <!-- Green (71-100) -->
    <color name="risk_caution">#F59E0B</color>      <!-- Yellow (31-70) -->
    <color name="risk_danger">#EF4444</color>       <!-- Red (0-30) -->
</resources>
```

---

## 1️⃣ activity_main.xml (대시보드)

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/tds_bg_black">

    <!-- Header -->
    <LinearLayout
        android:id="@+id/header"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="@color/tds_bg_dark"
        android:padding="16dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="R² 리스크 트레이닝"
            android:textSize="24sp"
            android:textColor="@color/tds_text_primary"
            android:textStyle="bold" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="수익이 아닌, 리스크 감각을 키우세요"
            android:textSize="12sp"
            android:textColor="@color/tds_text_secondary"
            android:layout_marginTop="4dp" />
    </LinearLayout>

    <!-- ScrollView (Content) -->
    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/header"
        app:layout_constraintBottom_toTopOf="@id/bottom_nav"
        app:layout_constraintStart_toStartOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- 1. 계정 현황 카드 -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                app:cardBackgroundColor="@color/tds_bg_dark"
                app:cardCornerRadius="12dp"
                app:cardElevation="0dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="💰 계정 현황"
                        android:textSize="14sp"
                        android:textColor="@color/tds_text_secondary"
                        android:textStyle="bold" />

                    <TextView
                        android:id="@+id/balance_text"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="$10,000.00"
                        android:textSize="32sp"
                        android:textColor="@color/tds_text_primary"
                        android:textStyle="bold"
                        android:layout_marginTop="8dp" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginTop="12dp">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="순자산"
                            android:textSize="12sp"
                            android:textColor="@color/tds_text_secondary" />

                        <TextView
                            android:id="@+id/net_asset_text"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="$10,250.50"
                            android:textSize="16sp"
                            android:textColor="@color/tds_success"
                            android:textStyle="bold" />
                    </LinearLayout>

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginTop="8dp">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="변화"
                            android:textSize="12sp"
                            android:textColor="@color/tds_text_secondary" />

                        <TextView
                            android:id="@+id/change_text"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="+$250.50 (+2.5%)"
                            android:textSize="14sp"
                            android:textColor="@color/tds_success"
                            android:textStyle="bold" />
                    </LinearLayout>
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- 2. Risk Score 카드 -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                app:cardBackgroundColor="@color/tds_bg_dark"
                app:cardCornerRadius="12dp"
                app:cardElevation="0dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="⚠️ Risk Score"
                        android:textSize="14sp"
                        android:textColor="@color/tds_text_secondary"
                        android:textStyle="bold" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginTop="12dp"
                        android:gravity="center_vertical">

                        <TextView
                            android:id="@+id/risk_score_text"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="75/100"
                            android:textSize="28sp"
                            android:textColor="@color/risk_safe"
                            android:textStyle="bold" />

                        <!-- 리스크 게이지 (ProgressBar) -->
                        <ProgressBar
                            android:id="@+id/risk_gauge"
                            android:layout_width="80dp"
                            android:layout_height="80dp"
                            style="?android:attr/progressBarStyleHorizontal"
                            android:progress="75"
                            android:progressDrawable="@drawable/risk_gauge_drawable" />
                    </LinearLayout>

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="0-30: 🔴 위험 | 31-70: 🟡 주의 | 71-100: 🟢 안정"
                        android:textSize="11sp"
                        android:textColor="@color/tds_text_tertiary"
                        android:layout_marginTop="8dp" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- 3. 활성 포지션 카드 -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                app:cardBackgroundColor="@color/tds_bg_dark"
                app:cardCornerRadius="12dp"
                app:cardElevation="0dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="📈 활성 포지션"
                            android:textSize="14sp"
                            android:textColor="@color/tds_text_secondary"
                            android:textStyle="bold" />

                        <TextView
                            android:id="@+id/active_positions_count"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="2개"
                            android:textSize="14sp"
                            android:textColor="@color/tds_text_primary"
                            android:background="@drawable/badge_background"
                            android:paddingHorizontal="8dp"
                            android:paddingVertical="4dp" />
                    </LinearLayout>

                    <!-- RecyclerView for Active Positions -->
                    <androidx.recyclerview.widget.RecyclerView
                        android:id="@+id/active_positions_recycler"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="12dp"
                        android:orientation="vertical" />
                </LinearLayout>
            </androidx.cardview.widget.CardView>

            <!-- 4. 오늘의 성과 카드 -->
            <androidx.cardview.widget.CardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                app:cardBackgroundColor="@color/tds_bg_dark"
                app:cardCornerRadius="12dp"
                app:cardElevation="0dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="🎯 오늘의 성과"
                        android:textSize="14sp"
                        android:textColor="@color/tds_text_secondary"
                        android:textStyle="bold" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginTop="12dp">

                        <!-- 거래 횟수 -->
                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="거래"
                                android:textSize="12sp"
                                android:textColor="@color/tds_text_secondary" />

                            <TextView
                                android:id="@+id/today_trades_count"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="5회"
                                android:textSize="18sp"
                                android:textColor="@color/tds_text_primary"
                                android:textStyle="bold" />
                        </LinearLayout>

                        <!-- 승률 -->
                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="승률"
                                android:textSize="12sp"
                                android:textColor="@color/tds_text_secondary" />

                            <TextView
                                android:id="@+id/today_win_rate"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="60%"
                                android:textSize="18sp"
                                android:textColor="@color/tds_success"
                                android:textStyle="bold" />
                        </LinearLayout>

                        <!-- 총 수익 -->
                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="총 수익"
                                android:textSize="12sp"
                                android:textColor="@color/tds_text_secondary" />

                            <TextView
                                android:id="@+id/today_pnl"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="+$250.50"
                                android:textSize="18sp"
                                android:textColor="@color/tds_success"
                                android:textStyle="bold" />
                        </LinearLayout>
                    </LinearLayout>
                </LinearLayout>
            </androidx.cardview.widget.CardView>
        </LinearLayout>
    </ScrollView>

    <!-- Bottom Navigation -->
    <LinearLayout
        android:id="@+id/bottom_nav"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:orientation="horizontal"
        android:background="@color/tds_bg_dark"
        android:gravity="center_vertical"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent">

        <!-- 새 거래 -->
        <Button
            android:id="@+id/btn_new_trade"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="+ 새 거래"
            android:textColor="@color/tds_text_primary"
            android:background="@drawable/btn_primary"
            android:textSize="14sp"
            android:layout_margin="8dp" />

        <!-- 기록 -->
        <Button
            android:id="@+id/btn_history"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="기록"
            android:textColor="@color/tds_text_primary"
            android:background="@drawable/btn_secondary"
            android:textSize="14sp"
            android:layout_margin="8dp" />

        <!-- 분석 -->
        <Button
            android:id="@+id/btn_analysis"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="분석"
            android:textColor="@color/tds_text_primary"
            android:background="@drawable/btn_secondary"
            android:textSize="14sp"
            android:layout_margin="8dp" />
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## 2️⃣ activity_trading.xml (거래 실행)

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/tds_bg_black">

    <!-- Toolbar -->
    <androidx.appcompat.widget.Toolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:background="@color/tds_bg_dark"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent">

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="match_parent"
            android:orientation="vertical"
            android:gravity="center">

            <TextView
                android:id="@+id/symbol_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="BTCUSDT | 1H"
                android:textSize="16sp"
                android:textColor="@color/tds_text_primary"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/timeframe_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="5x 레버리지"
                android:textSize="12sp"
                android:textColor="@color/tds_text_secondary" />
        </LinearLayout>
    </androidx.appcompat.widget.Toolbar>

    <!-- TradingView WebView -->
    <WebView
        android:id="@+id/trading_chart"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toTopOf="@id/control_panel"
        app:layout_constraintStart_toStartOf="parent" />

    <!-- Control Panel -->
    <androidx.cardview.widget.CardView
        android:id="@+id/control_panel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="12dp"
        app:cardBackgroundColor="@color/tds_bg_dark"
        app:cardCornerRadius="12dp"
        app:cardElevation="0dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Entry Price -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="12dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Entry"
                    android:textSize="14sp"
                    android:textColor="@color/tds_text_secondary" />

                <EditText
                    android:id="@+id/entry_price_input"
                    android:layout_width="140dp"
                    android:layout_height="wrap_content"
                    android:text="$95,836.00"
                    android:textSize="14sp"
                    android:textColor="@color/tds_text_primary"
                    android:inputType="numberDecimal"
                    android:background="@drawable/edit_text_background"
                    android:paddingHorizontal="12dp"
                    android:paddingVertical="8dp" />
            </LinearLayout>

            <!-- TP Price -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="12dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="TP"
                    android:textSize="14sp"
                    android:textColor="@color/tds_text_secondary" />

                <EditText
                    android:id="@+id/tp_price_input"
                    android:layout_width="140dp"
                    android:layout_height="wrap_content"
                    android:text="$97,752.72"
                    android:textSize="14sp"
                    android:textColor="@color/tds_success"
                    android:inputType="numberDecimal"
                    android:background="@drawable/edit_text_background"
                    android:paddingHorizontal="12dp"
                    android:paddingVertical="8dp" />
            </LinearLayout>

            <!-- SL Price -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="12dp">

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="SL"
                    android:textSize="14sp"
                    android:textColor="@color/tds_text_secondary" />

                <EditText
                    android:id="@+id/sl_price_input"
                    android:layout_width="140dp"
                    android:layout_height="wrap_content"
                    android:text="$93,919.28"
                    android:textSize="14sp"
                    android:textColor="@color/tds_error"
                    android:inputType="numberDecimal"
                    android:background="@drawable/edit_text_background"
                    android:paddingHorizontal="12dp"
                    android:paddingVertical="8dp" />
            </LinearLayout>

            <!-- R:R Ratio & Risk Score -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="16dp">

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="R:R Ratio"
                        android:textSize="12sp"
                        android:textColor="@color/tds_text_secondary" />

                    <TextView
                        android:id="@+id/rr_ratio_text"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="1.24:1"
                        android:textSize="18sp"
                        android:textColor="@color/tds_text_primary"
                        android:textStyle="bold" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Risk Score"
                        android:textSize="12sp"
                        android:textColor="@color/tds_text_secondary" />

                    <TextView
                        android:id="@+id/risk_score_trading"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="75/100 🟢"
                        android:textSize="18sp"
                        android:textColor="@color/risk_safe"
                        android:textStyle="bold" />
                </LinearLayout>
            </LinearLayout>

            <!-- Entry Button -->
            <Button
                android:id="@+id/btn_enter_trade"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="거래 진입"
                android:textColor="@color/tds_text_primary"
                android:textSize="16sp"
                android:textStyle="bold"
                android:background="@drawable/btn_primary"
                android:layout_marginTop="8dp" />
        </LinearLayout>
    </androidx.cardview.widget.CardView>
</androidx.constraintlayout.widget.ConstraintLayout>
```

이제 **res/drawable/** 파일들도 필요합니다. 계속하겠습니까?
