package com.cwnu.kitchenmood.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.cwnu.kitchenmood.model.Mood;

public final class MoodPreferences {
    private static final String MOOD_KEY = "mood";
    private final SharedPreferences preferences;

    public MoodPreferences(Context context) {
        Context application = context.getApplicationContext();
        preferences = (application != null ? application : context)
                .getSharedPreferences("kitchen_mood_radio", Context.MODE_PRIVATE);
    }

    public Mood loadMood() {
        try {
            return Mood.fromName(preferences.getString(MOOD_KEY, Mood.CALM.name()));
        } catch (ClassCastException exception) {
            return Mood.CALM;
        }
    }

    public void saveMood(Mood mood) {
        Mood selected = mood != null ? mood : Mood.CALM;
        preferences.edit().putString(MOOD_KEY, selected.name()).apply();
    }
}
