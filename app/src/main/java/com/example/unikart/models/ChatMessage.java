package com.example.unikart.models;

public class ChatMessage {
    private String messageId;
    private String chatId;
    private String senderId;
    private String text;
    private long sentAt;

    public ChatMessage() {}

    public String getMessageId()            { return messageId != null ? messageId : ""; }
    public void setMessageId(String id)     { this.messageId = id; }

    public String getChatId()               { return chatId != null ? chatId : ""; }
    public void setChatId(String id)        { this.chatId = id; }

    public String getSenderId()             { return senderId != null ? senderId : ""; }
    public void setSenderId(String id)      { this.senderId = id; }

    public String getText()                 { return text != null ? text : ""; }
    public void setText(String t)           { this.text = t; }

    public long getSentAt()                 { return sentAt; }
    public void setSentAt(long t)           { this.sentAt = t; }
}
