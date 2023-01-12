package com.example.giveandtake.Model;

import android.widget.Toast;

import com.example.giveandtake.View.VerifyPhone;

public class User {

    private String  fullName;

    private String  email;

    private String  isManager;

    private String  isPhoneVerified;

    private String  isBlocked;

    public String getName() {
        return fullName;
    }

    public void setName(String name) {
        fullName = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIsManager() {
        return isManager;
    }

    public void setIsManager(String isManager) {
        this.isManager = isManager;
    }

    public String getIsBlocked() {
        return isBlocked;
    }

    public void setIsBlocked(String isBlocked) {
        this.isBlocked = isBlocked;
    }

    public String getIsPhoneVerified() {
        return isPhoneVerified;
    }

    public void setIsPhoneVerified(String isPhoneVerified) {
        this.isPhoneVerified= isPhoneVerified;
    }

    @Override
    public String toString() {
        return "User [ Name=" + Name + ", email=" + email
                + ", isPhoneVerified=" + isPhoneVerified+ ", isBlocked=" + isBlocked + ", isManager=" + isManager
                + "]";
    }




}
