package de.trenits.talkshowgaeste.ui;

public class ArchiveItem {
    public final String showName;
    public final String date;
    public final String guestText;

    public ArchiveItem(String showName, String date, String guestText) {
        this.showName = showName;
        this.date = date;
        this.guestText = guestText;
    }
}