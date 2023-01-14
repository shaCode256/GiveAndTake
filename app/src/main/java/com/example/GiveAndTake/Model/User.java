package com.example.giveandtake.Model;

public class User {

    private String  fullName;

    private String  email;

    private String  isManager;

    private String  isPhoneVerified;

    private String  isBlocked;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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
        return "User [ fullName=" + fullName + ", email=" + email
                + ", isPhoneVerified=" + isPhoneVerified+ ", isBlocked=" + isBlocked + ", isManager=" + isManager
                + "]";
    }




}
