package com.example.giveandtake.Presenter;

import com.example.giveandtake.Model.User;

public class RegisterUser {
    public void registerUser(String emailTxt, String phoneTxt, String fullNameTxt) {
        User user = new User();
        user.setEmail(emailTxt);
        user.setIsBlocked("0");
        user.setFullName(fullNameTxt);
        user.setIsPhoneVerified("1");
            if (emailTxt.endsWith("@manager.com")) {
                user.setIsManager("1");
            } else {
                user.setIsManager("0");
            }
    }

}

