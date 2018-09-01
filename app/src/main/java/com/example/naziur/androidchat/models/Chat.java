package com.example.naziur.androidchat.models;

/**
 * Created by Naziur on 01/09/2018.
 */

public class Chat {

    private MessageCell messageCell;
    private String speakingTo;

    public Chat(String speakingTo, MessageCell messageCell){
        this.speakingTo = speakingTo;
        this.messageCell = messageCell;
    }

    public MessageCell getMessageCell() {
        return messageCell;
    }

    public void setMessageCell(MessageCell messageCell) {
        this.messageCell = messageCell;
    }

    public String getSpeakingTo() {
        return speakingTo;
    }

    public void setSpeakingTo(String speakingTo) {
        this.speakingTo = speakingTo;
    }





}
