package com.example.rsquare.ui.position;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rsquare.data.local.AppDatabase;
import com.example.rsquare.data.local.entity.Position;

/**
 * 포지션 상세 ViewModel
 */
public class PositionDetailViewModel extends AndroidViewModel {
    
    private final MutableLiveData<Position> position = new MutableLiveData<>();
    
    public PositionDetailViewModel(Application application) {
        super(application);
    }
    
    public LiveData<Position> getPosition() {
        return position;
    }
    
    public void loadPosition(long positionId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Position pos = AppDatabase.getInstance(getApplication())
                .positionDao()
                .getPositionByIdSync(positionId);
            
            if (pos != null) {
                position.postValue(pos);
            }
        });
    }
}

