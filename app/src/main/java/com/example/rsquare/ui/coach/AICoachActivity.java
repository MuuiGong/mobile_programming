package com.example.rsquare.ui.coach;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rsquare.R;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 코치 Activity
 */
public class AICoachActivity extends AppCompatActivity {
    
    private RecyclerView chatRecycler;
    private EditText chatInput;
    private Button btnSend;
    private Button btnQuestion1;
    private Button btnQuestion2;
    private Button btnQuestion3;
    
    private List<String> chatMessages = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_coach);
        
        initViews();
        setupToolbar();
        setupListeners();
        setupRecyclerView();
    }
    
    private void initViews() {
        chatRecycler = findViewById(R.id.chat_recycler);
        chatInput = findViewById(R.id.chat_input);
        btnSend = findViewById(R.id.btn_send);
        btnQuestion1 = findViewById(R.id.btn_question_1);
        btnQuestion2 = findViewById(R.id.btn_question_2);
        btnQuestion3 = findViewById(R.id.btn_question_3);
    }
    
    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI 코치");
        }
    }
    
    private void setupRecyclerView() {
        chatRecycler.setLayoutManager(new LinearLayoutManager(this));
        // TODO: ChatAdapter 구현
    }
    
    private void setupListeners() {
        btnSend.setOnClickListener(v -> {
            String message = chatInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                chatInput.setText("");
            }
        });
        
        btnQuestion1.setOnClickListener(v -> {
            String question = btnQuestion1.getText().toString();
            sendMessage(question);
        });
        
        btnQuestion2.setOnClickListener(v -> {
            String question = btnQuestion2.getText().toString();
            sendMessage(question);
        });
        
        btnQuestion3.setOnClickListener(v -> {
            String question = btnQuestion3.getText().toString();
            sendMessage(question);
        });
        
        // Enter 키로 전송
        chatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                btnSend.performClick();
                return true;
            }
            return false;
        });
    }
    
    private void sendMessage(String message) {
        chatMessages.add("사용자: " + message);
        
        // AI 응답 시뮬레이션
        String response = generateAIResponse(message);
        chatMessages.add("AI 코치: " + response);
        
        // RecyclerView 업데이트
        // TODO: Adapter에 메시지 추가
        
        Toast.makeText(this, "AI 코치: " + response, Toast.LENGTH_LONG).show();
    }
    
    private String generateAIResponse(String question) {
        // 간단한 응답 생성 (실제로는 AI 엔진 호출)
        if (question.contains("위험")) {
            return "이번 주 가장 위험했던 포지션은 ETHUSDT 롱 포지션이었습니다. 마진 비율이 15%까지 떨어졌던 상황이었습니다.";
        } else if (question.contains("손절")) {
            return "손절을 지키지 않은 비율은 약 35%입니다. 손절을 더 엄격하게 지키시면 리스크 관리가 개선될 것입니다.";
        } else if (question.contains("리스크 성향")) {
            return "귀하의 리스크 성향은 중간 수준입니다. 평균보다 약간 공격적인 편이지만, 적절한 수준입니다.";
        } else {
            return "질문을 이해했습니다. 거래 데이터를 분석하여 답변을 준비하겠습니다.";
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

