package com.example.rsquare.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;

/**
 * Onboarding ViewPager Adapter
 */
public class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.SlideViewHolder> {
    
    private static final int[] SLIDE_LAYOUTS = {
        R.layout.slide_onboarding_welcome,
        R.layout.slide_onboarding_auto_calc,
        R.layout.slide_onboarding_monitoring,
        R.layout.slide_onboarding_start
    };
    
    @Override
    public int getItemCount() {
        return SLIDE_LAYOUTS.length;
    }
    
    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(SLIDE_LAYOUTS[viewType], parent, false);
        return new SlideViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        // No binding needed for static slides
    }
    
    @Override
    public int getItemViewType(int position) {
        return position;
    }
    
    static class SlideViewHolder extends RecyclerView.ViewHolder {
        public SlideViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
