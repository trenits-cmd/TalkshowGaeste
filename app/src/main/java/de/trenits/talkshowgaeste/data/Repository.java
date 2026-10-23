package de.trenits.talkshowgaeste.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.trenits.talkshowgaeste.BuildConfig;

public class Repository {

    public interface Listener {
        void onDataChanged();
    }

    public interface RefreshCallback {
        void onFinished(boolean success, String message);
    }

    public static class Upcoming {
        public final Show show;
        public final Episode episode;

        public Upcoming(Show show, Episode episode) {
            this.show = show;
            this.episode = episode;
        }
    }

    private static final String PREFS = "talkshow_prefs";
    private static final String KEY_SEED = "seed_imported";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String ASSET_SEED = "talkshows.json";

    private static volatile Repository instance;

    private final Context context;
    private final DatabaseHelper helper;
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private Repository(Context context) {
        this.context = context.getApplicationContext();
        this.helper = new DatabaseHelper(this.context);
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ensureSeeded();
    }

    public static Repository get(Context context) {
        if (instance == null) {
            synchronized (Repository.class) {
                if (instance == null) {
                    instance = new Repository(context);
                }
            }
        }
        return instance;
    }

    public void addListener(Listener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        mainHandler.post(() -> {
            for (Listener listener : listeners) {
                listener.onDataChanged();
            }
        });
    }

    private void ensureSeeded() {
        if (prefs.getBoolean(KEY_SEED, false)) return;
        if (countShows() > 0) {
            prefs.edit().putBoolean(KEY_SEED, true).apply();
            return;
        }
        try {
            String json = readAsset();
            mergeJson(json);
            prefs.edit().putBoolean(KEY_SEED, true).apply();
        } catch (Exception ignored) {
            // Ohne Seed bleibt die App leer, bis ein Update erfolgreich war.
        }
    }

    private String readAsset() throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStream input = context.getAssets().open(ASSET_SEED);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
        }
        return builder.toString();
    }

    public int countShows() {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM shows WHERE is_deleted = 0", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public List<Show> shows() {
        List<Show> result = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM shows WHERE is_deleted = 0 ORDER BY name COLLATE NOCASE", null)) {
            while (cursor.moveToNext()) result.add(readShow(cursor));
        }
        return result;
    }

    public List<Show> showsForDay(int isoDay) {
        List<Show> result = new ArrayList<>();
        for (Show show : shows()) {
            if (show.weekdays.contains(isoDay)) result.add(show);
        }
        Collections.sort(result, showTimeComparator());
        return result;
    }

    public Show showById(String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM shows WHERE id = ? AND is_deleted = 0", new String[]{id})) {
            return cursor.moveToFirst() ? readShow(cursor) : null;
        }
    }

    public Episode episode(String showId, String date) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM episodes WHERE show_id = ? AND date = ?",
                new String[]{showId, date})) {
            return cursor.moveToFirst() ? readEpisode(cursor) : null;
        }
    }

    public Episode nextEpisode(String showId, LocalDate fromDate) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM episodes WHERE show_id = ? AND date >= ? ORDER BY date, time LIMIT 1",
                new String[]{showId, fromDate.toString()})) {
            return cursor.moveToFirst() ? readEpisode(cursor) : null;
        }
    }

    public List<Upcoming> upcomingEpisodes(LocalDate fromDate, int limit) {
        List<Upcoming> result = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT e.show_id, e.date, e.time, e.topic, e.guests " +
                "FROM episodes e JOIN shows s ON s.id = e.show_id " +
                "WHERE s.is_deleted = 0 AND e.date >= ? ORDER BY e.date, e.time, s.name LIMIT ?";
        try (Cursor cursor = db.rawQuery(sql,
                new String[]{fromDate.toString(), String.valueOf(limit)})) {
            while (cursor.moveToNext()) {
                Episode episode = readEpisode(cursor);
                Show show = showById(episode.showId);
                if (show != null) result.add(new Upcoming(show, episode));
            }
        }
        return result;
    }

    public void saveShow(Show show, boolean isNew) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (isNew || show.id == null || show.id.isEmpty()) {
            show.id = "local-" + UUID.randomUUID();
        }
        values.put("id", show.id);
        values.put("name", show.name);
        values.put("broadcaster", show.broadcaster == null ? "" : show.broadcaster);
        values.put("weekdays", joinWeekdays(show.weekdays));
        values.put("time", show.time);
        values.put("frequency", show.frequency == null ? "Wöchentlich" : show.frequency);
        values.put("color", show.color == null ? "#E7B45B" : show.color);
        values.put("description", show.description == null ? "" : show.description);
        values.put("is_deleted", 0);

        if (isNew) {
            values.put("is_local", 1);
            values.put("is_customized", 0);
            db.insertWithOnConflict("shows", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } else {
            int local = 0;
            int customized = 0;
            try (Cursor cursor = db.rawQuery(
                    "SELECT is_local, is_customized FROM shows WHERE id = ?",
                    new String[]{show.id})) {
                if (cursor.moveToFirst()) {
                    local = cursor.getInt(0);
                    customized = cursor.getInt(1);
                }
            }
            values.put("is_local", local);
            values.put("is_customized", local == 1 ? customized : 1);
            db.update("shows", values, "id = ?", new String[]{show.id});
        }
        notifyListeners();
    }

    public void deleteShow(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int local = 0;
        try (Cursor cursor = db.rawQuery(
                "SELECT is_local FROM shows WHERE id = ?", new String[]{id})) {
            if (cursor.moveToFirst()) local = cursor.getInt(0);
        }
        if (local == 1) {
            db.delete("shows", "id = ?", new String[]{id});
        } else {
            ContentValues values = new ContentValues();
            values.put("is_deleted", 1);
            db.update("shows", values, "id = ?", new String[]{id});
        }
        db.delete("episodes", "show_id = ?", new String[]{id});
        notifyListeners();
    }

    public long lastUpdateMillis() {
        return prefs.getLong(KEY_LAST_UPDATE, 0L);
    }

    public void refresh(RefreshCallback callback) {
        executor.execute(() -> {
            try {
                String json = fetch(BuildConfig.DATA_URL);
                mergeJson(json);
                prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply();
                mainHandler.post(() -> {
                    if (callback != null) callback.onFinished(true, "Aktualisiert");
                });
            } catch (Exception exception) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onFinished(false, exception.getMessage());
                });
            }
        });
    }

    private String fetch(String url) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "TalkshowGaeste/1.0 (Android)");
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server antwortet mit " + status);
            }
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) builder.append(line);
            }
            if (builder.length() == 0) throw new IOException("Leere Antwort");
            return builder.toString();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void mergeJson(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray showsArray = root.optJSONArray("shows");
        JSONArray episodesArray = root.optJSONArray("episodes");

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (showsArray != null) {
                for (int i = 0; i < showsArray.length(); i++) {
                    JSONObject object = showsArray.optJSONObject(i);
                    if (object == null) continue;
                    String id = object.optString("id", "").trim();
                    String name = object.optString("name", "").trim();
                    if (id.isEmpty() || name.isEmpty()) continue;

                    boolean exists = false;
                    boolean local = false;
                    boolean customized = false;
                    boolean deleted = false;
                    try (Cursor cursor = db.rawQuery(
                            "SELECT is_local, is_customized, is_deleted FROM shows WHERE id = ?",
                            new String[]{id})) {
                        if (cursor.moveToFirst()) {
                            exists = true;
                            local = cursor.getInt(0) == 1;
                            customized = cursor.getInt(1) == 1;
                            deleted = cursor.getInt(2) == 1;
                        }
                    }

                    if (exists && (local || customized || deleted)) continue;

                    ContentValues values = new ContentValues();
                    values.put("id", id);
                    values.put("name", name);
                    values.put("broadcaster", object.optString("broadcaster", ""));
                    values.put("weekdays", joinWeekdays(parseWeekdays(object.optJSONArray("weekdays"))));
                    values.put("time", object.optString("time", "22:00"));
                    values.put("frequency", object.optString("frequency", "Wöchentlich"));
                    values.put("color", object.optString("color", "#E7B45B"));
                    values.put("description", object.optString("description", ""));
                    values.put("is_local", 0);
                    values.put("is_customized", 0);
                    values.put("is_deleted", 0);
                    db.insertWithOnConflict("shows", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            if (episodesArray != null) {
                for (int i = 0; i < episodesArray.length(); i++) {
                    JSONObject object = episodesArray.optJSONObject(i);
                    if (object == null) continue;
                    String showId = object.optString("showId", "").trim();
                    String date = object.optString("date", "").trim();
                    if (showId.isEmpty() || date.isEmpty()) continue;
                    if (!isShowActive(db, showId)) continue;

                    ContentValues values = new ContentValues();
                    values.put("show_id", showId);
                    values.put("date", date);
                    values.put("time", object.optString("time", ""));
                    values.put("topic", object.optString("topic", ""));
                    JSONArray guestsArray = object.optJSONArray("guests");
                    JSONArray guests = new JSONArray();
                    if (guestsArray != null) {
                        for (int g = 0; g < guestsArray.length(); g++) {
                            String guest = guestsArray.optString(g, "").trim();
                            if (!guest.isEmpty()) guests.put(guest);
                        }
                    }
                    values.put("guests", guests.toString());
                    db.insertWithOnConflict("episodes", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        notifyListeners();
    }

    private boolean isShowActive(SQLiteDatabase db, String showId) {
        try (Cursor cursor = db.rawQuery(
                "SELECT is_deleted FROM shows WHERE id = ?", new String[]{showId})) {
            return cursor.moveToFirst() && cursor.getInt(0) == 0;
        }
    }

    private List<Integer> parseWeekdays(JSONArray array) {
        List<Integer> days = new ArrayList<>();
        if (array == null) return days;
        for (int i = 0; i < array.length(); i++) {
            int day = array.optInt(i, 0);
            if (day >= 1 && day <= 7 && !days.contains(day)) days.add(day);
        }
        Collections.sort(days);
        return days;
    }

    private Show readShow(Cursor cursor) {
        return readShow(cursor, cursor.getColumnIndexOrThrow("id"));
    }

    private Show readShow(Cursor cursor, int idColumn) {
        Show show = new Show();
        show.id = cursor.getString(idColumn);
        show.name = getString(cursor, "name");
        show.broadcaster = getString(cursor, "broadcaster");
        show.weekdays = splitWeekdays(getString(cursor, "weekdays"));
        show.time = getString(cursor, "time");
        show.frequency = getString(cursor, "frequency");
        show.color = getString(cursor, "color");
        show.description = getString(cursor, "description");
        show.local = getInt(cursor, "is_local") == 1;
        show.customized = getInt(cursor, "is_customized") == 1;
        show.deleted = getInt(cursor, "is_deleted") == 1;
        return show;
    }

    private Episode readEpisode(Cursor cursor) {
        Episode episode = new Episode();
        episode.showId = getString(cursor, "show_id");
        episode.date = getString(cursor, "date");
        episode.time = getString(cursor, "time");
        episode.topic = getString(cursor, "topic");
        episode.guests = parseGuests(getString(cursor, "guests"));
        return episode;
    }

    private List<String> parseGuests(String json) {
        List<String> guests = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json == null ? "[]" : json);
            for (int i = 0; i < array.length(); i++) {
                String guest = array.optString(i, "").trim();
                if (!guest.isEmpty()) guests.add(guest);
            }
        } catch (Exception ignored) {
        }
        return guests;
    }

    private String joinWeekdays(List<Integer> days) {
        StringBuilder builder = new StringBuilder();
        if (days != null) {
            for (int i = 0; i < days.size(); i++) {
                if (i > 0) builder.append(",");
                builder.append(days.get(i));
            }
        }
        return builder.toString();
    }

    private List<Integer> splitWeekdays(String value) {
        List<Integer> days = new ArrayList<>();
        if (value == null || value.isEmpty()) return days;
        for (String part : value.split(",")) {
            try {
                int day = Integer.parseInt(part.trim());
                if (day >= 1 && day <= 7) days.add(day);
            } catch (NumberFormatException ignored) {
            }
        }
        Collections.sort(days);
        return days;
    }

    private String getString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index >= 0 && !cursor.isNull(index) ? cursor.getString(index) : "";
    }

    private int getInt(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index >= 0 && !cursor.isNull(index) ? cursor.getInt(index) : 0;
    }

    private Comparator<Show> showTimeComparator() {
        return (a, b) -> {
            String left = a.time == null ? "" : a.time;
            String right = b.time == null ? "" : b.time;
            int result = left.compareTo(right);
            if (result != 0) return result;
            return a.name.toLowerCase(Locale.GERMAN).compareTo(b.name.toLowerCase(Locale.GERMAN));
        };
    }
}
