package de.trenits.talkshowgaeste.data;

import java.util.ArrayList;
import java.util.List;

public class Episode {
    public String showId;
    public String date;
    public String time;
    public String topic;
    public List<String> guests = new ArrayList<>();

    public Episode() {
    }

    public Episode(String showId, String date, String time, String topic, List<String> guests) {
        this.showId = showId;
        this.date = date;
        this.time = time;
        this.topic = topic;
        this.guests = guests;
    }
}
