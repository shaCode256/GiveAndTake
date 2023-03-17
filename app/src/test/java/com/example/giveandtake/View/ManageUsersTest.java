package com.example.giveandtake.View;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class ManageUsersTest {

    ArrayList<String> listItems= new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        listItems.add("String1");
        listItems.add("String2");
    }

    @Test
    public void addItems() {
        listItems.add("String3");
        assertEquals(3, listItems.size());
        assertEquals("String3", listItems.get(2));
    }
}