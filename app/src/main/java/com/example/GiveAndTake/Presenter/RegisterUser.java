package com.example.giveandtake.Presenter;
import android.content.Intent;
import android.widget.Toast;

import com.example.giveandtake.Model.User;
import com.example.giveandtake.View.Login;
import com.example.giveandtake.View.VerifyPhone;
import com.google.firebase.database.DatabaseReference;

public class RegisterUser {

    public void registerUser(String emailTxt, String phoneTxt, String fullNameTxt, DatabaseReference databaseReference) {
        User user = new User();
        user.setEmail(emailTxt);
        user.setIsBlocked("0");
        user.setName(fullNameTxt);
        user.setIsPhoneVerified("1");
        if (emailTxt.endsWith("@manager.com") || emailTxt.endsWith("@msmail.ariel.ac.il")) {
            if (emailTxt.endsWith("@manager.com")) {
                user.setIsManager("1");
            } else {
                user.setIsManager("0");
            }
            databaseReference.child("users").child(phoneTxt).setValue(user);
        }
    }
}

