package com.example.naziur.androidchat.models;

/**
 * Created by Naziur on 01/09/2018.
 */

public class Chat {

    //profile name of the person the device owner is speaking to
    private String chatKey;

    private String speakingTo;
    private String usernameOfTheOneBeingSpokenTo;
    private String lastMsgInThisChat;
    private String timeOfMsg;
    private int isSeen;

    private String msgType;

    private String profilePic;

    public Chat(String speakingTo, String username, String lastMsg, String pic, String time, String chatKey, int received, String msgType){
        this.speakingTo = speakingTo;
        usernameOfTheOneBeingSpokenTo = username;
        lastMsgInThisChat = lastMsg;
        profilePic = pic;
        timeOfMsg = time;
        this.chatKey = chatKey;
        isSeen = received;
        this.msgType = msgType;

    }

    public String getChatKey() {
        return chatKey;
    }


    public String getTimeOfMsg() {
        return timeOfMsg;
    }

    public String getSpeakingTo() {
        return speakingTo;
    }

    public String getUsernameOfTheOneBeingSpokenTo() {
        return usernameOfTheOneBeingSpokenTo;
    }

    public String getLastMsgInThisChat() {
        return lastMsgInThisChat;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setSpeakingTo(String speakingTo) {
        this.speakingTo = speakingTo;
    }


    public int getIsSeen() {
        return isSeen;
    }

    public void setIsSeen(int isSeen) {
        this.isSeen = isSeen;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }
}
