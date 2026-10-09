package com.cwnu.kitchenmood.model;

import static org.junit.Assert.*;
import org.junit.Test;

public class MoodTest {
    @Test public void invalidSavedPreferenceFallsBackToCalm() {
        assertSame(Mood.CALM, Mood.fromName(null));
        assertSame(Mood.CALM, Mood.fromName("missing"));
        assertSame(Mood.GROOVE, Mood.fromName("GROOVE"));
    }

    @Test public void allModesHaveNotesForTheShuffleControl() {
        for (Mood mood : Mood.values()) {
            assertTrue(mood.notes.length > 1);
            for (String note : mood.notes) assertFalse(note.trim().isEmpty());
        }
    }
}
