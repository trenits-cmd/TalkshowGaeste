package de.trenits.talkshowgaeste;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.trenits.talkshowgaeste.data.Episode;
import de.trenits.talkshowgaeste.data.Repository;
import de.trenits.talkshowgaeste.data.Show;
import de.trenits.talkshowgaeste.ui.ScheduleAdapter;
import de.trenits.talkshowgaeste.ui.ShowsAdapter;
import de.trenits.talkshowgaeste.ui.TodayAdapter;
import de.trenits.talkshowgaeste.ui.ArchiveAdapter;
import de.trenits.talkshowgaeste.ui.ArchiveItem;
import de.trenits.talkshowgaeste.util.Formats;

public class MainActivity extends AppCompatActivity implements Repository.Listener {

    private static final String[] PALETTE = {
            "#E7B45B", "#6EA8FE", "#6FCF97", "#E45C5C", "#B48EAD", "#4FB3BF", "#E08A5B", "#D4C56A"
    };

    private static final int[] DAY_IDS = {1, 2, 3, 4, 5, 6, 7};
    private static final int[] EDIT_CHIP_IDS = {
            R.id.chipMon, R.id.chipTue, R.id.chipWed, R.id.chipThu,
            R.id.chipFri, R.id.chipSat, R.id.chipSun
    };
    private static final int[] PLAN_CHIP_IDS = {
            R.id.chipPlanMon, R.id.chipPlanTue, R.id.chipPlanWed, R.id.chipPlanThu,
            R.id.chipPlanFri, R.id.chipPlanSat, R.id.chipPlanSun
    };

    private Repository repository;
    private TodayAdapter todayAdapter;
    private ScheduleAdapter planAdapter;
    private ShowsAdapter showsAdapter;

    private RecyclerView todayList;
    private RecyclerView planList;
    private RecyclerView showsList;
    private View todayEmpty;
    private View planEmpty;
    private View upcomingSection;
    private LinearLayout upcomingContainer;
    private TextView tvWeekday;
    private TextView tvDate;
    private TextView tvTodayCount;
    private TextView tvUpdated;
    private TextView tvPlanDay;
    private TextView tvShowCount;
    private View todayScreen;
    private View planScreen;
    private View showsScreen;
    private View archivesScreen;

    private int planDayIso = LocalDate.now().getDayOfWeek().getValue();
    private boolean refreshing;
    private ObjectAnimator refreshAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TalkshowGaeste);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = Repository.get(this);
        repository.addListener(this);

        bindViews();
        setupLists();
        setupNavigation();
        setupPlanDays();
        setupActions();

        renderAll();
        refresh(true);
    }

    private void bindViews() {
        todayList = findViewById(R.id.todayList);
        planList = findViewById(R.id.planList);
        showsList = findViewById(R.id.showsList);
        todayEmpty = findViewById(R.id.todayEmpty);
        planEmpty = findViewById(R.id.planEmpty);
        upcomingSection = findViewById(R.id.upcomingSection);
        upcomingContainer = findViewById(R.id.upcomingContainer);
        tvWeekday = findViewById(R.id.tvWeekday);
        tvDate = findViewById(R.id.tvDate);
        tvTodayCount = findViewById(R.id.tvTodayCount);
        tvUpdated = findViewById(R.id.tvUpdated);
        tvPlanDay = findViewById(R.id.tvPlanDay);
        tvShowCount = findViewById(R.id.tvShowCount);
        todayScreen = findViewById(R.id.todayScreen);
        planScreen = findViewById(R.id.planScreen);
        showsScreen = findViewById(R.id.showsScreen);
        archivesScreen = findViewById(R.id.archivesScreen);
        archiveList = findViewById(R.id.archiveList);
        archiveEmpty = findViewById(R.id.archiveEmpty);
    }

    private void setupLists() {
        todayList.setLayoutManager(new LinearLayoutManager(this));
        todayAdapter = new TodayAdapter(this::showDetails);
        todayList.setAdapter(todayAdapter);
        todayList.setItemAnimator(null);

        planList.setLayoutManager(new LinearLayoutManager(this));
        planAdapter = new ScheduleAdapter(repository, this::showDetails);
        planList.setAdapter(planAdapter);
        planList.setItemAnimator(null);

        showsList.setLayoutManager(new LinearLayoutManager(this));
        showsAdapter = new ShowsAdapter(new ShowsAdapter.Listener() {
            @Override
            public void onShowClick(Show show) {
                showDetails(show);
            }

            @Override
            public void onEdit(Show show) {
                showEditDialog(show);
            }

            @Override
            public void onDelete(Show show) {
                confirmDelete(show);
            }
        });
        showsList.setAdapter(showsAdapter);
        showsList.setItemAnimator(null);
    }

    private void setupNavigation() {
        BottomNavigationView navigation = findViewById(R.id.bottomNav);
        navigation.setOnItemSelectedListener(item -> {
            showScreen(item.getItemId());
            if (item.getItemId() == R.id.nav_archive) {
                showGuestArchive();
            }
            return true;
        });
        showScreen(R.id.nav_today);
    }

    private void showScreen(int itemId) {
        todayScreen.setVisibility(itemId == R.id.nav_today ? View.VISIBLE : View.GONE);
        planScreen.setVisibility(itemId == R.id.nav_plan ? View.VISIBLE : View.GONE);
        showsScreen.setVisibility(itemId == R.id.nav_shows ? View.VISIBLE : View.GONE);
        archivesScreen.setVisibility(itemId == R.id.nav_archive ? View.VISIBLE : View.GONE);
        if (itemId == R.id.nav_plan) renderPlan();
        if (itemId == R.id.nav_shows) renderShows();
        if (itemId == R.id.nav_archive) renderArchive();
    }

    private void setupPlanDays() {
        ChipGroup group = findViewById(R.id.dayChips);
        Chip todayChip = findViewById(PLAN_CHIP_IDS[planDayIso - 1]);
        if (todayChip != null) todayChip.setChecked(true);
        group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            for (int i = 0; i < PLAN_CHIP_IDS.length; i++) {
                if (PLAN_CHIP_IDS[i] == checkedId) {
                    planDayIso = i + 1;
                    break;
                }
            }
            renderPlan();
        });
    }

    private void setupActions() {
        ImageButton refreshButton = findViewById(R.id.buttonRefresh);
        refreshButton.setOnClickListener(v -> refresh(false));
        findViewById(R.id.buttonInfo).setOnClickListener(v -> showAbout());
        findViewById(R.id.fabAdd).setOnClickListener(v -> showEditDialog(null));
    }

    @Override
    protected void onDestroy() {
        if (refreshAnimator != null) refreshAnimator.cancel();
        repository.removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onDataChanged() {
        runOnUiThread(this::renderAll);
    }

    private void renderAll() {
        renderToday();
        renderPlan();
        renderShows();
    }

    private void renderToday() {
        LocalDate today = LocalDate.now();
        Locale german = Locale.GERMAN;
        tvWeekday.setText(Formats.weekdayLong(today.getDayOfWeek().getValue()).toUpperCase(german));
        tvDate.setText(Formats.dayMonth(today));

        int isoDay = today.getDayOfWeek().getValue();
        String todayString = today.toString();
        List<TodayAdapter.TodayItem> items = new ArrayList<>();

        for (Show show : repository.shows()) {
            Episode episode = repository.episode(show.id, todayString);
            boolean scheduled = show.weekdays.contains(isoDay);
            boolean monthly = show.frequency != null
                    && show.frequency.toLowerCase(german).contains("monat");
            if (episode != null || (scheduled && !monthly)) {
                items.add(new TodayAdapter.TodayItem(show, episode));
            }
        }

        items.sort(Comparator
                .comparing((TodayAdapter.TodayItem item) -> item.episode != null
                        && item.episode.time != null && !item.episode.time.isEmpty()
                        ? item.episode.time : item.show.time)
                .thenComparing(item -> item.show.name.toLowerCase(german)));

        todayAdapter.submit(items);
        todayList.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        todayEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        tvTodayCount.setText(items.size() == 1
                ? getString(R.string.today_count_one)
                : getString(R.string.today_count, items.size()));

        long lastUpdate = repository.lastUpdateMillis();
        if (lastUpdate == 0) {
            tvUpdated.setText(R.string.not_updated);
        } else {
            String time = new SimpleDateFormat("HH:mm", german).format(new Date(lastUpdate));
            tvUpdated.setText(getString(R.string.last_update, time));
        }

        renderUpcoming(today);
    }

    private void renderUpcoming(LocalDate today) {
        upcomingContainer.removeAllViews();
        List<Coming> coming = new ArrayList<>();

        for (int offset = 1; offset <= 8; offset++) {
            LocalDate date = today.plusDays(offset);
            int isoDay = date.getDayOfWeek().getValue();
            for (Show show : repository.shows()) {
                if (!show.weekdays.contains(isoDay)) continue;
                Episode episode = repository.episode(show.id, date.toString());
                boolean monthly = show.frequency != null
                        && show.frequency.toLowerCase(Locale.GERMAN).contains("monat");
                if (monthly && episode == null) continue;
                coming.add(new Coming(show, episode, date));
            }
        }

        coming.sort(Comparator
                .comparing((Coming c) -> c.date)
                .thenComparing(c -> c.episode != null && c.episode.time != null
                        && !c.episode.time.isEmpty() ? c.episode.time : c.show.time)
                .thenComparing(c -> c.show.name));

        int count = Math.min(4, coming.size());
        upcomingSection.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        for (int i = 0; i < count; i++) {
            Coming entry = coming.get(i);
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_upcoming_show, upcomingContainer, false);
            TextView dateView = row.findViewById(R.id.tvDate);
            TextView nameView = row.findViewById(R.id.tvName);
            TextView metaView = row.findViewById(R.id.tvMeta);

            String dateText = Formats.weekdayShort(entry.date.getDayOfWeek().getValue())
                    + "\n" + Formats.shortDate(entry.date);
            dateView.setText(dateText);
            nameView.setText(entry.show.name);
            String time = entry.episode != null && entry.episode.time != null
                    && !entry.episode.time.isEmpty() ? entry.episode.time : entry.show.time;
            String meta = (TextUtils.isEmpty(entry.show.broadcaster) ? "TV" : entry.show.broadcaster)
                    + " · " + time + " Uhr";
            if (entry.episode != null && !entry.episode.guests.isEmpty()) {
                meta += " · Gäste bekannt";
            }
            metaView.setText(meta);
            row.setOnClickListener(v -> showDetails(entry.show));
            upcomingContainer.addView(row);
        }
    }

    private void renderPlan() {
        String dayName = Formats.weekdayLong(planDayIso);
        List<Show> shows = repository.showsForDay(planDayIso);
        planAdapter.submit(shows);
        tvPlanDay.setText(dayName + " · " + (shows.size() == 1
                ? "1 Show" : shows.size() + " Shows"));
        planList.setVisibility(shows.isEmpty() ? View.GONE : View.VISIBLE);
        planEmpty.setVisibility(shows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderShows() {
        List<Show> shows = repository.shows();
        showsAdapter.submit(shows);
        String count = shows.size() == 1 ? "1 Show in deiner Datenbank"
                : shows.size() + " Shows in deiner Datenbank";
        tvShowCount.setText(count + " · " + getString(R.string.database_hint));
    }

    private void renderArchive() {
        Map<String, List<Repository.RecentArchiveEntry>> archive = repository.getRecentGuestArchive();

        if (archive.isEmpty()) {
            archiveList.setVisibility(View.GONE);
            archiveEmpty.setVisibility(View.VISIBLE);
            return;
        }

        archiveList.setVisibility(View.VISIBLE);
        archiveEmpty.setVisibility(View.GONE);

        List<ArchiveItem> items = new ArrayList<>();
        for (var showEntry : archive.entrySet()) {
            String showName = showEntry.getKey();
            for (var dateEntry : showEntry.getValue()) {
                String formattedDate = dateEntry.getDate();
                List<String> guests = dateEntry.getGuests();
                String guestText;
                if (guests.isEmpty()) {
                    guestText = "keine Gäste";
                } else if (guests.size() == 1) {
                    guestText = guests.get(0);
                } else {
                    guestText = guests.get(0) + ", " + guests.get(1) + "+" + (guests.size() - 2 + " weitere");
                }
                items.add(new ArchiveItem(showName, formattedDate, guestText));
            }
        }

        ArchiveAdapter adapter = new ArchiveAdapter(items);
        archiveList.setAdapter(adapter);
        archiveList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_archive) {
            showGuestArchive();
            return true;
        }
        if (item.getItemId() == R.id.nav_archive) {
            showGuestArchive();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Gästarchiv-Funktion: Zeigt Gäste der letzten 7 Tage pro Sendung
    private void showGuestArchive() {
        Map<String, List<Repository.RecentArchiveEntry>> archive = repository.getRecentGuestArchive();
        StringBuilder sb = new StringBuilder();
        sb.append("Gästearchiv (letzte 7 Tage)\n\n");
        if (archive.isEmpty()) {
            sb.append("Keine Gäste-Daten der letzten 7 Tage verfügbar.");
        } else {
            for (var showEntry : archive.entrySet()) {
                sb.append(showEntry.getKey()).append(":\n");
                for (var dayEntry : showEntry.getValue()) {
                    sb.append("  " + dayEntry.date + " - ");
                    List<String> guests = dayEntry.guests;
                    for (int i = 0; i < guests.size() && i < 3; i++) {
                        sb.append(guests.get(i));
                        if (i < 2 && i < guests.size() - 1) sb.append(", ");
                    }
                    if (guests.size() > 3) sb.append(" + " + (guests.size() - 3) + " weitere");
                    sb.append("\n");
                }
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Gästearchiv")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showDetails(Show show) {
        LocalDate today = LocalDate.now();
        LocalDate episodeDate = today;
        Episode episode = repository.episode(show.id, today.toString());
        if (episode == null) {
            episode = repository.nextEpisode(show.id, today);
            if (episode != null) episodeDate = LocalDate.parse(episode.date);
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_show_detail, null);
        TextView name = view.findViewById(R.id.detailName);
        TextView meta = view.findViewById(R.id.detailMeta);
        TextView topic = view.findViewById(R.id.detailTopic);
        TextView guestsLabel = view.findViewById(R.id.detailGuestsLabel);
        LinearLayout guests = view.findViewById(R.id.detailGuests);
        TextView description = view.findViewById(R.id.detailDescription);

        name.setText(show.name);
        StringBuilder metaText = new StringBuilder();
        if (!TextUtils.isEmpty(show.broadcaster)) metaText.append(show.broadcaster).append(" · ");
        metaText.append(Formats.weekdays(show.weekdays)).append(" · ")
                .append(show.time).append(" Uhr");
        if (episode != null && episodeDate != null && !episodeDate.equals(today)) {
            metaText.append(" · Nächste Sendung: ").append(Formats.shortDate(episodeDate));
        }
        meta.setText(metaText.toString());

        if (episode != null && !TextUtils.isEmpty(episode.topic)) {
            topic.setVisibility(View.VISIBLE);
            topic.setText(episode.topic);
        } else {
            topic.setVisibility(View.GONE);
        }

        guests.removeAllViews();
        if (episodeDate != null && !episodeDate.equals(today)) {
            guestsLabel.setText("Gäste am " + Formats.shortDate(episodeDate));
        } else {
            guestsLabel.setText(R.string.guests);
        }
        if (episode != null && !episode.guests.isEmpty()) {
            for (String guest : episode.guests) {
                View line = LayoutInflater.from(this)
                        .inflate(R.layout.item_guest_line, guests, false);
                TextView guestName = line.findViewById(R.id.tvGuest);
                guestName.setText(guest);
                guests.addView(line);
            }
        } else {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_guests);
            empty.setTextColor(getColor(R.color.text_faint));
            empty.setTextSize(13);
            empty.setPadding(0, dp(4), 0, 0);
            guests.addView(empty);
        }

        if (!TextUtils.isEmpty(show.description)) {
            description.setVisibility(View.VISIBLE);
            description.setText(show.description);
        } else {
            description.setVisibility(View.GONE);
        }

        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void showEditDialog(Show existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_show_edit, null);
        TextInputLayout layoutName = view.findViewById(R.id.layoutName);
        TextInputLayout layoutTime = view.findViewById(R.id.layoutTime);
        TextInputEditText inputName = view.findViewById(R.id.inputName);
        TextInputEditText inputBroadcaster = view.findViewById(R.id.inputBroadcaster);
        TextInputEditText inputTime = view.findViewById(R.id.inputTime);
        TextInputEditText inputDescription = view.findViewById(R.id.inputDescription);
        MaterialAutoCompleteTextView inputFrequency = view.findViewById(R.id.inputFrequency);

        String[] frequencies = {"3× pro Woche", "2× pro Woche", "Wöchentlich", "Monatlich"};
        android.widget.ArrayAdapter<String> frequencyAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, frequencies);
        inputFrequency.setAdapter(frequencyAdapter);

        if (existing != null) {
            inputName.setText(existing.name);
            inputBroadcaster.setText(existing.broadcaster);
            inputTime.setText(existing.time);
            inputDescription.setText(existing.description);
            inputFrequency.setText(existing.frequency, false);
            for (int i = 0; i < DAY_IDS.length; i++) {
                Chip chip = view.findViewById(EDIT_CHIP_IDS[i]);
                chip.setChecked(existing.weekdays.contains(DAY_IDS[i]));
            }
        } else {
            inputFrequency.setText("Wöchentlich", false);
            inputTime.setText("22:00");
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.add_show_title : R.string.edit_show_title)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    layoutName.setError(null);
                    layoutTime.setError(null);
                    String name = textOf(inputName);
                    String time = textOf(inputTime);

                    if (name.isEmpty()) {
                        layoutName.setError(getString(R.string.error_name));
                        return;
                    }
                    if (!Formats.validTime(time)) {
                        layoutTime.setError(getString(R.string.error_time));
                        return;
                    }

                    List<Integer> days = new ArrayList<>();
                    for (int i = 0; i < EDIT_CHIP_IDS.length; i++) {
                        Chip chip = view.findViewById(EDIT_CHIP_IDS[i]);
                        if (chip.isChecked()) days.add(DAY_IDS[i]);
                    }
                    if (days.isEmpty()) {
                        Toast.makeText(this, R.string.error_days, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Show show = existing != null ? existing : new Show();
                    show.name = name;
                    show.broadcaster = textOf(inputBroadcaster);
                    show.time = Formats.normalizeTime(time);
                    show.weekdays = days;
                    String frequency = inputFrequency.getText().toString().trim();
                    show.frequency = frequency.isEmpty() ? "Wöchentlich" : frequency;
                    show.description = textOf(inputDescription);
                    if (TextUtils.isEmpty(show.color)) {
                        show.color = PALETTE[repository.countShows() % PALETTE.length];
                    }

                    repository.saveShow(show, existing == null);
                    dialog.dismiss();
                    renderAll();
                    snackbar(existing == null ? "Show hinzugefügt" : "Show gespeichert");
                }));

        dialog.show();
    }

    private void confirmDelete(Show show) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, show.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.deleteShow(show.id);
                    renderAll();
                    snackbar("„" + show.name + "“ gelöscht");
                })
                .show();
    }

    private void showAbout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_title)
                .setMessage(getString(R.string.about_text,
                        BuildConfig.DATA_URL, BuildConfig.VERSION_NAME))
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void refresh(boolean silent) {
        if (refreshing) return;
        refreshing = true;
        ImageButton button = findViewById(R.id.buttonRefresh);
        button.setEnabled(false);
        button.setAlpha(0.55f);
        if (!silent) {
            tvUpdated.setText(R.string.updating);
        }
        startRefreshAnimation(button);

        repository.refresh((success, message) -> {
            refreshing = false;
            button.setEnabled(true);
            button.setAlpha(1f);
            stopRefreshAnimation(button);
            renderAll();
            if (!silent) {
                snackbar(success ? getString(R.string.update_success)
                        : getString(R.string.update_offline));
            }
            if (!success && repository.countShows() == 0) {
                snackbar(getString(R.string.update_offline));
            }
        });
    }

    private void startRefreshAnimation(View view) {
        if (refreshAnimator != null) refreshAnimator.cancel();
        refreshAnimator = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, 360f);
        refreshAnimator.setDuration(900);
        refreshAnimator.setRepeatCount(ValueAnimator.INFINITE);
        refreshAnimator.setInterpolator(new LinearInterpolator());
        refreshAnimator.start();
    }

    private void stopRefreshAnimation(View view) {
        if (refreshAnimator != null) {
            refreshAnimator.cancel();
            refreshAnimator = null;
        }
        view.setRotation(0f);
    }

    private void snackbar(String message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_LONG).show();
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class Coming {
        final Show show;
        final Episode episode;
        final LocalDate date;

        Coming(Show show, Episode episode, LocalDate date) {
            this.show = show;
            this.episode = episode;
            this.date = date;
        }
    }
}
