package com.example.rsquare.data.remote.model;

import java.util.List;

public class ExchangeInfoResponse {
    private List<SymbolInfo> symbols;

    public List<SymbolInfo> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<SymbolInfo> symbols) {
        this.symbols = symbols;
    }
}
