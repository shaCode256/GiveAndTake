package com.example.giveandtake.Model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class UserTest {
    User user1= new User();

    @Before
    public void setUp() throws Exception {
        user1.setEmail("user1@gmail.com");
        user1.setFullName("user1 name");
        user1.setIsManager("1");
        user1.setIsBlocked("0");
        user1.setIsPhoneVerified("1");
    }

    @Test
    public void getFullName() {
        assertEquals("user1 name", user1.getFullName());
    }

    @Test
    public void setFullName() {
    }

    @Test
    public void getEmail() {
        assertEquals("user1@gmail.com", user1.getEmail());
    }

    @Test
    public void setEmail() {
        user1.setEmail("user2@gmail.com");
        assertEquals("user2@gmail.com", user1.getEmail());
    }

    @Test
    public void getIsManager() {
        assertEquals("1", user1.getIsManager());
    }

    @Test
    public void setIsManager() {
        user1.setIsManager("0");
        assertEquals("0", user1.getIsManager());
    }

    @Test
    public void getIsBlocked() {
        assertEquals("0", user1.getIsBlocked());
    }

    @Test
    public void setIsBlocked() {
        user1.setIsBlocked("1");
        assertEquals("1", user1.getIsBlocked());
    }

    @Test
    public void getIsPhoneVerified() {
        assertEquals("1", user1.getIsPhoneVerified());
    }

    @Test
    public void setIsPhoneVerified() {
        user1.setIsPhoneVerified("0");
        assertEquals("0", user1.getIsPhoneVerified());
    }

    @Test
    public void testToString() {
        assertEquals("User [ fullName=" + user1.getFullName() + ", email=" + user1.getEmail()
                + ", isPhoneVerified=" + user1.getIsPhoneVerified()+ ", isBlocked=" + user1.getIsBlocked() + ", isManager=" + user1.getIsManager()
                + "]", user1.toString() );
    }
}