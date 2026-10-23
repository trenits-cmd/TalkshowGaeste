package de.trenits.talkshowgaeste.data;

import java.util.ArrayList;
import java.util.List;

public class Show {
    public String id;
    public String name;
    public String broadcaster;
    public List<Integer> weekdays = new ArrayList<>();
    public String time;
    public String frequency;
    public String color;
    public String description;
    public boolean local;
    public boolean customized;
    public boolean deleted;

    public Show() {
    }

    public Show(String id, String name, String broadcaster, List<Integer> weekdays,
                String time, String frequency, String color, String description) {
        this.id = id;
        this.name = name;
        this.broadcaster = broadcaster;
        this.weekdays = weekdays;
        this.time = time;
        this.frequency = frequency;
        this.color = color;
        this.description = description;
    }
}
