package com.cwnu.kitchenmood;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cwnu.kitchenmood.audio.MoodAudioEngine;
import com.cwnu.kitchenmood.model.Mood;
import com.cwnu.kitchenmood.storage.MoodPreferences;
import com.cwnu.kitchenmood.timer.KitchenTimer;
import com.cwnu.kitchenmood.ui.RadioDialView;

import java.util.Locale;
import java.util.Random;

/** A small, offline radio and kitchen break companion. */
public final class MainActivity extends Activity {
    private static final int CREAM = Color.rgb(247, 242, 228);
    private static final int MUTED = Color.rgb(181, 198, 186);
    private static final int DARK_TEXT = Color.rgb(31, 51, 43);
    private static final String STATE_MOOD = "mood";
    private static final String STATE_MINUTES = "minutes";
    private static final String STATE_REMAINING = "remaining";
    private static final String STATE_RUNNING = "running";
    private static final String STATE_DEADLINE = "deadline";
    private static final String STATE_NOTE = "note";
    private static final String STATE_VOLUME = "volume";

    private Mood mood;
    private MoodAudioEngine audio;
    private KitchenTimer timer;
    private MoodPreferences preferences;
    private final Random random = new Random();
    private Handler tickerHandler;
    private boolean foreground;
    private int noteIndex;
    private float volume = 0.2f;
    private long lastDisplayedSecond = Long.MIN_VALUE;
    private Mood lastTimerStyleMood;
    private int lastTimerStyleMinutes = -1;
    private boolean lastTimerStyleRunning;
    private boolean lastTimerStyleComplete;

    private ScrollView scroll;
    private LinearLayout page;
    private LinearLayout stationCard;
    private LinearLayout timerCard;
    private LinearLayout noteCard;
    private final Button[] moodButtons = new Button[Mood.values().length];
    private final Button[] presetButtons = new Button[3];
    private TextView stationTitle;
    private TextView stationDescription;
    private TextView playbackStatus;
    private TextView volumeLabel;
    private TextView timerDigits;
    private TextView timerStatus;
    private TextView noteText;
    private TextView noteCount;
    private Button playbackButton;
    private Button timerActionButton;
    private Button timerResetButton;
    private Button customApplyButton;
    private Button shuffleButton;
    private SeekBar volumeSeek;
    private EditText customMinutes;
    private RadioDialView radioDial;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!foreground || tickerHandler == null) return;
            refreshTime();
            renderPlayback();
            tickerHandler.postDelayed(this, 250L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        configureWindow();
        preferences = new MoodPreferences(this);
        mood = state == null ? preferences.loadMood() : Mood.fromName(state.getString(STATE_MOOD));
        if (mood == null) mood = Mood.CALM;
        timer = new KitchenTimer(SystemClock::elapsedRealtime);
        timer.setMinutes(5);
        if (state != null) {
            timer.restore(state.getInt(STATE_MINUTES, 5), state.getLong(STATE_REMAINING, 300_000L),
                    state.getBoolean(STATE_RUNNING, false), state.getLong(STATE_DEADLINE, 0L));
            noteIndex = state.getInt(STATE_NOTE, 0);
            volume = Math.max(0f, Math.min(1f, state.getFloat(STATE_VOLUME, 0.2f)));
        }
        audio = new MoodAudioEngine(this);
        audio.setMood(mood);
        audio.setVolume(volume);
        buildContent();
        tickerHandler = new Handler(Looper.getMainLooper());
        applyMood();
        renderTimer();
        renderPlayback();
    }

    private void configureWindow() {
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void buildContent() {
        FrameLayout root = new FrameLayout(this);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setPadding(dp(20), dp(22), dp(20), dp(28));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                Insets keyboard = insets.getInsets(WindowInsets.Type.ime());
                left = bars.left; top = bars.top; right = bars.right;
                bottom = Math.max(bars.bottom, keyboard.bottom);
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(dp(20) + left, dp(22) + top, dp(20) + right, dp(28) + bottom);
            return insets;
        });
        page = column();
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        setContentView(root);
        scroll.requestApplyInsets();

        TextView brand = text("LG AX × CWNU  ·  THEME B", 10, MUTED, true);
        brand.setLetterSpacing(0.15f);
        page.addView(brand);
        addSpace(page, 10);
        TextView title = text("Kitchen Mood\nRadio", 34, CREAM, true);
        title.setLineSpacing(0, 0.98f);
        title.setContentDescription("Kitchen Mood Radio, 키친 무드 라디오");
        page.addView(title);
        addSpace(page, 10);
        page.addView(text("주방에 흐르는, 오늘의 리듬.", 14, MUTED, false));
        addSpace(page, 26);

        page.addView(eyebrow("01  오늘의 분위기"));
        addSpace(page, 10);
        LinearLayout moodRow = row();
        Mood[] moods = Mood.values();
        for (int i = 0; i < moods.length; i++) {
            final Mood item = moods[i];
            Button button = button(item.icon + "\n" + item.label, 12);
            button.setMinHeight(dp(70));
            button.setMaxLines(2);
            button.setOnClickListener(view -> selectMood(item));
            moodButtons[i] = button;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(76), 1f);
            if (i > 0) params.leftMargin = dp(8);
            moodRow.addView(button, params);
        }
        page.addView(moodRow);
        addSpace(page, 18);

        stationCard = card();
        stationCard.addView(eyebrow("YOUR LITTLE KITCHEN STATION"));
        addSpace(stationCard, 8);
        stationTitle = text("", 22, CREAM, true);
        if (Build.VERSION.SDK_INT >= 28) stationTitle.setAccessibilityHeading(true);
        stationCard.addView(stationTitle);
        addSpace(stationCard, 6);
        stationDescription = text("", 13, MUTED, false);
        stationDescription.setLineSpacing(dp(3), 1f);
        stationCard.addView(stationDescription);
        radioDial = new RadioDialView(this);
        radioDial.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams dialParams = new LinearLayout.LayoutParams(-1, dp(232));
        dialParams.topMargin = dp(10);
        stationCard.addView(radioDial, dialParams);
        playbackStatus = text("", 11, MUTED, false);
        playbackStatus.setGravity(Gravity.CENTER);
        stationCard.addView(playbackStatus);
        addSpace(stationCard, 14);
        playbackButton = button("▶  라디오 재생", 15);
        playbackButton.setOnClickListener(view -> togglePlayback());
        stationCard.addView(playbackButton, new LinearLayout.LayoutParams(-1, dp(52)));
        addSpace(stationCard, 18);
        LinearLayout volumeRow = row();
        TextView volumeTitle = text("볼륨", 12, MUTED, false);
        volumeRow.addView(volumeTitle, new LinearLayout.LayoutParams(0, -2, 1));
        volumeLabel = text(getString(R.string.volume_percent, Math.round(volume * 100)), 12, CREAM, true);
        volumeRow.addView(volumeLabel);
        stationCard.addView(volumeRow);
        volumeSeek = new SeekBar(this);
        volumeSeek.setMax(100);
        volumeSeek.setProgress(Math.round(volume * 100));
        volumeSeek.setContentDescription("라디오 볼륨");
        volumeSeek.setPadding(dp(2), 0, dp(2), 0);
        volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                volume = progress / 100f;
                audio.setVolume(volume);
                volumeLabel.setText(getString(R.string.volume_percent, progress));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        stationCard.addView(volumeSeek, new LinearLayout.LayoutParams(-1, dp(44)));
        page.addView(stationCard);
        addSpace(page, 18);

        buildTimerCard();
        page.addView(timerCard);
        addSpace(page, 18);
        buildNoteCard();
        page.addView(noteCard);
        addSpace(page, 24);
        TextView footer = text("A small pause. A softer kitchen.\n인터넷 없이, 내 휴대폰에서.", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(dp(5), 1f);
        page.addView(footer);
    }

    private void buildTimerCard() {
        timerCard = card();
        timerCard.addView(eyebrow("02  작은 쉼표 타이머"));
        addSpace(timerCard, 14);
        timerDigits = text("05:00", 48, CREAM, true);
        timerDigits.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        timerDigits.setGravity(Gravity.CENTER);
        timerDigits.setFontFeatureSettings("tnum");
        timerCard.addView(timerDigits);
        timerStatus = text("준비되면 시작해요", 12, MUTED, false);
        timerStatus.setGravity(Gravity.CENTER);
        timerCard.addView(timerStatus);
        addSpace(timerCard, 18);
        int[] minutes = {3, 5, 10};
        LinearLayout presets = row();
        for (int i = 0; i < minutes.length; i++) {
            final int value = minutes[i];
            Button preset = button(value + "분", 13);
            preset.setOnClickListener(view -> setTimerMinutes(value));
            preset.setContentDescription(value + "분 타이머 설정");
            presetButtons[i] = preset;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1);
            if (i > 0) params.leftMargin = dp(8);
            presets.addView(preset, params);
        }
        timerCard.addView(presets);
        addSpace(timerCard, 16);
        TextView inputLabel = text("직접 설정 · 1–60분", 11, MUTED, false);
        timerCard.addView(inputLabel);
        addSpace(timerCard, 8);
        LinearLayout customRow = row();
        customMinutes = new EditText(this);
        customMinutes.setId(View.generateViewId());
        inputLabel.setLabelFor(customMinutes.getId());
        customMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        customMinutes.setSingleLine(true);
        customMinutes.setImeOptions(EditorInfo.IME_ACTION_DONE);
        customMinutes.setTextSize(16);
        customMinutes.setTextColor(CREAM);
        customMinutes.setHintTextColor(MUTED);
        customMinutes.setHint("분 입력");
        customMinutes.setContentDescription("타이머 시간, 1분부터 60분까지");
        customMinutes.setPadding(dp(14), 0, dp(14), 0);
        customMinutes.setBackground(shape(Color.rgb(25, 46, 37), dp(12), 1, Color.rgb(70, 90, 76)));
        customMinutes.setSelectAllOnFocus(true);
        customMinutes.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) { applyCustomMinutes(); return true; }
            return false;
        });
        customRow.addView(customMinutes, new LinearLayout.LayoutParams(0, dp(48), 1));
        customApplyButton = button("적용", 13);
        customApplyButton.setOnClickListener(view -> applyCustomMinutes());
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(dp(72), dp(48));
        applyParams.leftMargin = dp(8);
        customRow.addView(customApplyButton, applyParams);
        timerCard.addView(customRow);
        addSpace(timerCard, 18);
        LinearLayout actionRow = row();
        timerActionButton = button("시작", 14);
        timerActionButton.setOnClickListener(view -> {
            if (timer.isRunning()) timer.pause(); else timer.start();
            lastDisplayedSecond = Long.MIN_VALUE;
            renderTimer();
        });
        actionRow.addView(timerActionButton, new LinearLayout.LayoutParams(0, dp(50), 1));
        timerResetButton = button("다시 설정", 13);
        timerResetButton.setOnClickListener(view -> {
            timer.reset();
            lastDisplayedSecond = Long.MIN_VALUE;
            renderTimer();
        });
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0, dp(50), 1);
        resetParams.leftMargin = dp(8);
        actionRow.addView(timerResetButton, resetParams);
        timerCard.addView(actionRow);
        addSpace(timerCard, 12);
        TextView timerHelp = text("화면을 나가면 음악은 멈추고, 시간은 계속 흘러요.\n돌아오면 남은 시간을 확인할 수 있어요.", 11, MUTED, false);
        timerHelp.setLineSpacing(dp(3), 1f);
        timerCard.addView(timerHelp);
    }

    private void buildNoteCard() {
        noteCard = card();
        LinearLayout heading = row();
        TextView noteTitle = text("03  오늘의 한 문장", 11, DARK_TEXT, true);
        noteTitle.setLetterSpacing(0.05f);
        heading.addView(noteTitle, new LinearLayout.LayoutParams(0, -2, 1));
        noteCount = text("", 11, DARK_TEXT, false);
        heading.addView(noteCount);
        noteCard.addView(heading);
        addSpace(noteCard, 18);
        noteText = text("", 21, DARK_TEXT, true);
        noteText.setLineSpacing(dp(5), 1.06f);
        noteText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        noteCard.addView(noteText);
        addSpace(noteCard, 18);
        shuffleButton = button("↻  다른 문장", 13);
        shuffleButton.setOnClickListener(view -> {
            int length = mood.notes.length;
            if (length > 1) noteIndex = (noteIndex + 1 + random.nextInt(length - 1)) % length;
            renderNote();
        });
        noteCard.addView(shuffleButton, new LinearLayout.LayoutParams(-1, dp(46)));
    }

    private void selectMood(Mood selected) {
        if (mood != selected) {
            mood = selected;
            noteIndex = 0;
            preferences.saveMood(mood);
            audio.setMood(mood);
        }
        applyMood();
        renderPlayback();
    }

    private void applyMood() {
        scroll.getRootView().setBackgroundColor(mood.backgroundColor);
        scroll.setBackgroundColor(mood.backgroundColor);
        stationCard.setBackground(shape(mood.cardColor, dp(24), 1, Color.rgb(66, 84, 70)));
        timerCard.setBackground(shape(mood.cardColor, dp(24), 1, Color.rgb(66, 84, 70)));
        noteCard.setBackground(shape(mood.noteColor, dp(24), 0, 0));
        for (int i = 0; i < moodButtons.length; i++) {
            Mood item = Mood.values()[i];
            Button button = moodButtons[i];
            boolean selected = item == mood;
            button.setSelected(selected);
            button.setActivated(selected);
            button.setTextColor(selected ? DARK_TEXT : CREAM);
            button.setBackground(shape(selected ? mood.accentColor : mood.cardColor, dp(16),
                    selected ? 0 : 1, Color.rgb(66, 84, 70)));
            button.setContentDescription(item.label + (selected ? ", 선택됨" : ", 선택하기"));
        }
        stationTitle.setText(mood.stationTitle);
        stationDescription.setText(mood.description);
        radioDial.setAccentColor(mood.accentColor);
        tintPrimary(playbackButton);
        tintPrimary(timerActionButton);
        tintSecondary(timerResetButton);
        tintSecondary(customApplyButton);
        shuffleButton.setTextColor(DARK_TEXT);
        shuffleButton.setBackground(shape(withAlpha(DARK_TEXT, 20), dp(12), 1, withAlpha(DARK_TEXT, 50)));
        volumeSeek.setProgressTintList(ColorStateList.valueOf(mood.accentColor));
        volumeSeek.setThumbTintList(ColorStateList.valueOf(mood.accentColor));
        volumeSeek.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(66, 84, 70)));
        renderNote();
        renderTimer();
    }

    private void togglePlayback() {
        if (audio.isPlaying()) {
            audio.stop();
        } else if (!audio.start(mood)) {
            Toast.makeText(this, "지금은 소리를 재생할 수 없어요. 잠시 후 다시 눌러주세요.", Toast.LENGTH_LONG).show();
        }
        renderPlayback();
    }

    private void renderPlayback() {
        boolean playing = audio.isPlaying();
        String label = playing ? "■  라디오 멈추기" : "▶  라디오 재생";
        if (!label.contentEquals(playbackButton.getText())) playbackButton.setText(label);
        String description = playing ? mood.label + " 라디오 멈추기" : mood.label + " 라디오 재생하기";
        if (!description.equals(playbackButton.getContentDescription())) playbackButton.setContentDescription(description);
        if (playbackButton.isSelected() != playing) playbackButton.setSelected(playing);
        String status = playing ? "●  ON AIR · 지금, 작은 리듬이 흐릅니다" : "READY · 재생 버튼을 눌러주세요";
        if (!status.contentEquals(playbackStatus.getText())) playbackStatus.setText(status);
        int statusColor = playing ? mood.accentColor : MUTED;
        if (playbackStatus.getCurrentTextColor() != statusColor) playbackStatus.setTextColor(statusColor);
        radioDial.setPlaying(playing);
    }

    private void setTimerMinutes(int minutes) {
        timer.setMinutes(minutes);
        customMinutes.setText("");
        customMinutes.setError(null);
        lastDisplayedSecond = Long.MIN_VALUE;
        renderTimer();
    }

    private void applyCustomMinutes() {
        String raw = customMinutes.getText().toString().trim();
        int minutes;
        try {
            minutes = Integer.parseInt(raw);
            if (minutes < 1 || minutes > 60) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            customMinutes.setError("1부터 60 사이의 분을 입력해주세요");
            Toast.makeText(this, "타이머는 1분부터 60분까지 설정할 수 있어요.", Toast.LENGTH_SHORT).show();
            return;
        }
        setTimerMinutes(minutes);
        customMinutes.clearFocus();
        InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(customMinutes.getWindowToken(), 0);
        Toast.makeText(this, minutes + "분으로 설정했어요", Toast.LENGTH_SHORT).show();
    }

    private void refreshTime() {
        if (timer.tick()) {
            audio.stop();
            timerDigits.announceForAccessibility("타이머가 끝났어요");
            Toast.makeText(this, "작은 쉼표가 끝났어요. 편안한 하루 보내세요.", Toast.LENGTH_LONG).show();
        }
        renderTimer();
    }

    private void renderTimer() {
        long seconds = Math.max(0L, (timer.getRemainingMillis() + 999L) / 1_000L);
        if (seconds != lastDisplayedSecond) {
            timerDigits.setText(String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L));
            timerDigits.setContentDescription("남은 시간 " + seconds / 60L + "분 " + seconds % 60L + "초");
            lastDisplayedSecond = seconds;
        }
        boolean running = timer.isRunning();
        boolean complete = timer.isComplete();
        int minutes = timer.getMinutes();
        boolean started = timer.getRemainingMillis() < minutes * 60_000L;
        String action = running ? "일시정지" : complete ? "다시 시작" : started ? "이어하기" : "시작";
        if (!action.contentEquals(timerActionButton.getText())) timerActionButton.setText(action);
        String description = "타이머 " + action;
        if (!description.equals(timerActionButton.getContentDescription())) timerActionButton.setContentDescription(description);
        if (timerActionButton.isSelected() != running) timerActionButton.setSelected(running);
        String status = complete ? "작은 쉼표가 끝났어요" : running ? "잠깐, 나만의 시간을 보내요" : started ? "잠시 멈췄어요" : "준비되면 시작해요";
        if (!status.contentEquals(timerStatus.getText())) timerStatus.setText(status);
        if (lastTimerStyleMood == mood && lastTimerStyleMinutes == minutes
                && lastTimerStyleRunning == running && lastTimerStyleComplete == complete) return;
        int[] values = {3, 5, 10};
        for (int i = 0; i < presetButtons.length; i++) {
            Button button = presetButtons[i];
            boolean selected = minutes == values[i];
            if (button.isSelected() != selected) button.setSelected(selected);
            button.setTextColor(selected ? DARK_TEXT : CREAM);
            button.setBackground(shape(selected ? mood.accentColor : withAlpha(CREAM, 12), dp(12),
                    selected ? 0 : 1, Color.rgb(66, 84, 70)));
            String presetDescription = values[i] + "분 타이머" + (selected ? ", 선택됨" : ", 설정하기");
            if (!presetDescription.equals(button.getContentDescription())) button.setContentDescription(presetDescription);
        }
        lastTimerStyleMood = mood;
        lastTimerStyleMinutes = minutes;
        lastTimerStyleRunning = running;
        lastTimerStyleComplete = complete;
    }

    private void renderNote() {
        if (mood.notes.length == 0) { noteText.setText("오늘도 편안한 리듬으로."); noteCount.setText(""); return; }
        noteIndex = Math.floorMod(noteIndex, mood.notes.length);
        noteText.setText(getString(R.string.quoted_note, mood.notes[noteIndex]));
        noteCount.setText(String.format(Locale.US, "%02d / %02d", noteIndex + 1, mood.notes.length));
    }

    @Override protected void onResume() {
        super.onResume();
        foreground = true;
        if (tickerHandler != null) {
            tickerHandler.removeCallbacks(ticker);
            tickerHandler.post(ticker);
        }
    }

    @Override protected void onPause() {
        foreground = false;
        if (tickerHandler != null) tickerHandler.removeCallbacks(ticker);
        if (audio != null) audio.stop();
        if (playbackButton != null) renderPlayback();
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString(STATE_MOOD, mood.name());
        state.putInt(STATE_MINUTES, timer.getMinutes());
        state.putLong(STATE_REMAINING, timer.getRemainingMillis());
        state.putBoolean(STATE_RUNNING, timer.isRunning());
        state.putLong(STATE_DEADLINE, timer.getDeadline());
        state.putInt(STATE_NOTE, noteIndex);
        state.putFloat(STATE_VOLUME, volume);
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        foreground = false;
        if (tickerHandler != null) tickerHandler.removeCallbacksAndMessages(null);
        if (audio != null) audio.release();
        super.onDestroy();
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private LinearLayout card() {
        LinearLayout layout = column();
        layout.setPadding(dp(20), dp(22), dp(20), dp(20));
        return layout;
    }

    private TextView eyebrow(String value) {
        TextView view = text(value, 10, MUTED, true);
        view.setLetterSpacing(0.08f);
        return view;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return view;
    }

    private Button button(String value, float size) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(size);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(44));
        button.setMinimumHeight(dp(44));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(8), dp(4), dp(8), dp(4));
        button.setStateListAnimator(null);
        return button;
    }

    private void tintPrimary(Button button) {
        button.setTextColor(DARK_TEXT);
        button.setBackground(shape(mood.accentColor, dp(13), 0, 0));
    }

    private void tintSecondary(Button button) {
        button.setTextColor(CREAM);
        button.setBackground(shape(withAlpha(CREAM, 12), dp(13), 1, Color.rgb(66, 84, 70)));
    }

    private GradientDrawable shape(int color, int radius, int stroke, int strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(radius);
        if (stroke > 0) background.setStroke(dp(stroke), strokeColor);
        return background;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void addSpace(LinearLayout layout, int height) {
        layout.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
