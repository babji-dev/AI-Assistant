package com.AI_assistant.Models;

public class ChatMessage {

    public String sender;
    public String text;
    public String type;
    public String codeSnippet;

    public ChatMessage(String sender, String text,String type) {
        this.sender = sender;
        this.text = text;
        this.type = type;
    }

    public ChatMessage(){

    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }

    //    @Override
//    public String toString() {
//        return "ChatMessage{" +
//                "sender='" + sender + '\'' +
//                ", text='" + text + '\'' +
//                '}';
//    }


    @Override
    public String toString() {
        return "ChatMessage{" +
                "sender='" + sender + '\'' +
                ", text='" + text + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
