package com.AI_assistant.Models;

import java.util.ArrayList;
import java.util.List;

public class ChatSessionState {

    private List<ChatMessage> messages = new ArrayList<>();
    private String summary = "";

    public List<ChatMessage> getMessages() { return messages; }
    public void addMessage(ChatMessage message) { messages.add(message); }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

}
