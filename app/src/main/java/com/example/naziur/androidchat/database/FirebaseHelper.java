package com.example.naziur.androidchat.database;

import android.util.Log;

import com.example.naziur.androidchat.models.Contact;
import com.example.naziur.androidchat.models.FirebaseUserModel;
import com.example.naziur.androidchat.models.User;
import com.example.naziur.androidchat.utils.Container;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

/**
 * Created by Hamidur and Naziur on 21/09/2018.
 */

public class FirebaseHelper {

    private static FirebaseDatabase database = FirebaseDatabase.getInstance();

    public interface FirebaseHelperListener {
        void onCompleteTask (int condition, Container container);
        void onFailureTask(DatabaseError databaseError);
        void onChange(int condition, Container container);
    }

    public static final int CONDITION_1 = 0;
    public static final int CONDITION_2 = 1;

    private static FirebaseHelperListener listener;

    private FirebaseHelper () {};

    public static void setFirebaseHelperListener (FirebaseHelperListener fbListener) {
        listener = fbListener;
    }

    public static void autoLogin(String node, final String currentDeviceId, final User user) {
        DatabaseReference usersRef = database.getReference(node);
        usersRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                boolean fail = true;
                for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    //Getting the data from snapshot
                    FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);

                    if (firebaseUserModel.getDeviceId().equals(currentDeviceId)) {
                        fail = false;
                        firebaseUserModel.setDeviceToken(FirebaseInstanceId.getInstance().getToken());
                        user.login(firebaseUserModel);
                        user.saveFirebaseKey(userSnapshot.getKey());
                        listener.onCompleteTask(CONDITION_1, null);
                        break;
                    }
                }

                if (fail) {
                    listener.onCompleteTask(CONDITION_2, null);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailureTask(databaseError);
            }
        });
    }


    public static void updateLocalContactsFromFirebase (String node, final FirebaseUserModel fbModel, final ContactDBHelper db) {
        DatabaseReference usersRef = database.getReference(node);
        Query query = usersRef.orderByChild("username").equalTo(fbModel.getUsername());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        FirebaseUserModel firebaseUserModel = userSnapshot.getValue(FirebaseUserModel.class);
                        if(firebaseUserModel.getUsername().equals(fbModel.getUsername())) {
                            db.updateProfile(firebaseUserModel.getUsername(), firebaseUserModel.getProfileName(), firebaseUserModel.getProfilePic());
                            Container container = new Container();
                            container.setContact(new Contact(firebaseUserModel));
                            listener.onChange(CONDITION_1, container);
                            break;
                        }
                    }
                } else {
                    Container container = new Container();
                    container.setContact(new Contact(fbModel, "", false));
                    listener.onCompleteTask(CONDITION_1, container);
                }

                listener.onCompleteTask(CONDITION_2, null);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
               listener.onFailureTask(databaseError);
            }
        });
    }

    public static ValueEventListener getMessageEventListener(final User user){
        //ValueEventListener valueEventListener =
    }

}
