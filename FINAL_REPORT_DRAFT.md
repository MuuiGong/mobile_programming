# Final Report: RSquare - Advanced Crypto Trading Simulator

## 1. Introduction
**RSquare** is a comprehensive cryptocurrency trading simulation application designed to help users practice trading strategies and master risk management without financial risk. Built for Android, it provides a professional-grade trading environment that mimics real-world exchange features, including futures trading, leverage, and advanced charting.

The primary goal of RSquare is to educate users on the importance of **Risk Management**. Unlike typical trading simulators that focus solely on profit, RSquare integrates a sophisticated **Risk Scoring System** that evaluates the user's trading behavior, volatility, and drawdown, encouraging sustainable trading habits.

## 2. Main Features

### 2.1 Real-time Trading Dashboard
The dashboard serves as the command center for the user's trading activities.
- **Net Asset & PnL Tracking**: Real-time calculation of total equity and unrealized profit/loss.
- **Risk Score Gauge**: A dynamic visual indicator (0-100) representing the safety of the user's current portfolio.
- **Active Positions**: A summary list of all open trades with live price updates and PnL status.

### 2.2 Professional Trading Interface
A fully-featured trading screen designed for serious analysis.
- **Interactive Charting**: Powered by TradingView's Lightweight Charts, supporting candlestick patterns, volume data, and multiple timeframes (1m to 1d).
- **Visual Trade Management**: Users can visualize their Entry Price (EP), Take Profit (TP), Stop Loss (SL), and Liquidation Price (LP) directly on the chart.
- **Futures Trading Capabilities**:
  - **Long/Short Positions**: Profit from both rising and falling markets.
  - **Leverage**: Adjustable leverage up to 20x.
  - **Margin Modes**: Support for both Isolated and Cross margin modes.

### 2.3 Advanced Risk Management System
The core differentiator of RSquare.
- **Real-time Risk Calculation**: Automatically calculates Risk-to-Reward (R:R) ratios and position risk percentages before placing a trade.
- **Liquidation Engine**: Accurate calculation of liquidation prices based on leverage and margin mode.
- **Behavior Analysis**: Tracks metrics like Volatility, Maximum Drawdown (MDD), and Sharpe Ratio to generate a comprehensive Risk Score.

### 2.4 Portfolio Analysis & History
- **Trade History**: Detailed logs of all closed positions with reasons for closure (TP, SL, Manual).
- **Performance Analytics**: Visual breakdown of Win Rate, Total PnL, and trade frequency.

## 3. Running Examples

### Scenario 1: Placing a Long Position with Risk Management
1.  **Asset Selection**: User selects BTC/USDT from the market list.
2.  **Analysis**: User analyzes the 1h chart and identifies a support level.
3.  **Setup**:
    -   Selects **Cross Margin** and **5x Leverage**.
    -   Enters Entry Price.
    -   Sets Stop Loss below the support level (Risk < 2%).
    -   Sets Take Profit at the next resistance (R:R > 1:2).
4.  **Execution**: Clicks "Open Long". The position appears on the chart with visible EP, TP, and SL lines.
5.  **Monitoring**: The Dashboard updates to show the new active position and adjusts the global Risk Score.

### Scenario 2: Risk Alert & Liquidation
1.  **High Risk Setup**: User attempts to open a position with 20x leverage and no Stop Loss.
2.  **Warning**: The app displays a "High Risk" warning, showing a low Risk Score for this specific trade setup.
3.  **Liquidation**: If the price moves against the position and hits the calculated Liquidation Price, the system automatically closes the position to prevent negative balance, simulating a real exchange liquidation event.

## 4. Technical Implementation
-   **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles.
-   **UI/UX**: Custom XML layouts following the Toss Design System (TDS) for a premium, modern aesthetic.
-   **Charting**: Integration of `Lightweight Charts` via WebView with a two-way JavaScript Bridge for seamless Android-JS communication.
-   **Data Persistence**: Room Database for local storage of user data, positions, and trade history.
-   **Concurrency**: Usage of `WorkManager` and `Threads` for background price monitoring and order execution.

## 5. Conclusion
RSquare successfully bridges the gap between simple paper trading and professional exchange platforms. By prioritizing risk management and providing professional tools, it offers a unique educational value proposition for aspiring crypto traders.
