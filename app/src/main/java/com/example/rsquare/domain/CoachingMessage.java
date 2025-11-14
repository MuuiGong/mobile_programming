package com.example.rsquare.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 코칭 메시지 데이터 클래스
 */
public class CoachingMessage {
    
    private String message;
    private MessageType type;
    private List<String> actionItems;
    private List<Long> relatedChallengeIds;
    private long timestamp;
    
    public enum MessageType {
        POSITIVE("긍정적 피드백"),
        WARNING("경고"),
        SUGGESTION("제안"),
        ACHIEVEMENT("성취");
        
        private final String label;
        
        MessageType(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }
    
    public CoachingMessage() {
        this.actionItems = new ArrayList<>();
        this.relatedChallengeIds = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public CoachingMessage(String message, MessageType type) {
        this();
        this.message = message;
        this.type = type;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public List<String> getActionItems() {
        return actionItems;
    }
    
    public void setActionItems(List<String> actionItems) {
        this.actionItems = actionItems;
    }
    
    public void addActionItem(String item) {
        this.actionItems.add(item);
    }
    
    public List<Long> getRelatedChallengeIds() {
        return relatedChallengeIds;
    }
    
    public void setRelatedChallengeIds(List<Long> relatedChallengeIds) {
        this.relatedChallengeIds = relatedChallengeIds;
    }
    
    public void addRelatedChallenge(long challengeId) {
        this.relatedChallengeIds.add(challengeId);
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

