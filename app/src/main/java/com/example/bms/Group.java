package com.example.bms;

public class Group {
    private String name;
    private long id;

    public Group(String name, long id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

}