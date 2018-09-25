package com.example.naziur.androidchat.models;

import com.example.naziur.androidchat.utils.Constants;

/**
 * Created by Naziur on 01/09/2018.
 */

public class Chat {

    //profile name of the person the device owner is speaking to
    private String chatKey;

    private String speakingTo, title; // profile name/ group name
    private String usernameOfTheOneBeingSpokenTo, senderName; // username of that profile name/ sender of the chat in group chat
    private String lastMsgInThisChat;
    private String timeOfMsg;
    private int isSeen;

    private String admin;

    private boolean isGroup;

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
        isGroup = false;
    }

    public Chat(String groupTitle, String username, String lastMsg, String pic, String time, String chatKey, String msgType, String admin){
        this.title = groupTitle;
        senderName = username;
        lastMsgInThisChat = lastMsg;
        profilePic = pic;
        timeOfMsg = time;
        this.chatKey = chatKey;
        isSeen = Constants.MESSAGE_RECEIVED;
        this.msgType = msgType;
        this.admin = admin;
        isGroup = true;

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

    public boolean isGroup () {return isGroup;}

    public String getTitle (){return title;}

    public String getSenderName (){return senderName;}

    public String getAdmin() {
        return admin;
    }
}
