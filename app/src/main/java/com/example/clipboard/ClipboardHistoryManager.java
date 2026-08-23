package com.example.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages clipboard history for Keyboard Hemat.
 */
public class ClipboardHistoryManager {

    private static final String PREF_NAME = "keyboard_clipboard_history";
    private static final String KEY_CLIPS_COUNT = "clips_count";
    private static final String KEY_CLIP_PREFIX = "clip_item_";
    private static final int MAX_CLIPS = 15;

    private final Context context;
    private final ClipboardManager clipboardManager;
    private final List<String> clipHistory = new ArrayList<>();
    private ClipboardManager.OnPrimaryClipChangedListener clipListener;

    public interface OnClipboardUpdatedListener {
        void OnClipboardUpdated(List<String> clips);
    }

    private OnClipboardUpdatedListener updateListener;

    public ClipboardHistoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.clipboardManager = (ClipboardManager) this.context.getSystemService(Context.CLIPBOARD_SERVICE);
        loadHistory();
        checkCurrentClip();
        initListener();
    }

    public void setOnClipboardUpdatedListener(OnClipboardUpdatedListener listener) {
        this.updateListener = listener;
    }

    private void initListener() {
        if (clipboardManager == null) return;
        clipListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                checkCurrentClip();
            }
        };
        try {
            clipboardManager.addPrimaryClipChangedListener(clipListener);
        } catch (Exception ignored) {}
    }

    public void checkCurrentClip() {
        if (clipboardManager == null) return;
        try {
            if (clipboardManager.hasPrimaryClip() && clipboardManager.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = clipboardManager.getPrimaryClip().getItemAt(0).getText();
                if (text != null) {
                    addClip(text.toString());
                }
            }
        } catch (Exception ignored) {}
    }

    public synchronized void addClip(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (isSensitiveData(text)) return; // Exclude sensitive data (passwords, tokens, cards)
        String trimmed = text.trim();

        // Remove duplicate if exists
        clipHistory.remove(trimmed);

        // Add to front (newest first)
        clipHistory.add(0, trimmed);

        // Limit size
        while (clipHistory.size() > MAX_CLIPS) {
            clipHistory.remove(clipHistory.size() - 1);
        }

        saveHistory();

        if (updateListener != null) {
            updateListener.OnClipboardUpdated(new ArrayList<>(clipHistory));
        }
    }

    private boolean isSensitiveData(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        if (lower.contains("password") || lower.contains("passwd") || lower.contains("sandi") ||
            lower.contains("token") || lower.contains("bearer") || lower.contains("secret") ||
            lower.contains("api_key") || lower.contains("apikey") || lower.contains("auth") ||
            lower.contains("cvv") || lower.contains("otp") || lower.contains("pin ")) {
            return true;
        }
        if (text.matches(".*\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b.*")) {
            return true;
        }
        if (text.length() > 32 && !text.contains(" ") && text.matches("^[A-Za-z0-9_\\-\\./=]+$")) {
            return true;
        }
        return false;
    }

    public synchronized List<String> getClips() {
        return new ArrayList<>(clipHistory);
    }

    public synchronized void clearClips() {
        clipHistory.clear();
        saveHistory();
        if (updateListener != null) {
            updateListener.OnClipboardUpdated(new ArrayList<>(clipHistory));
        }
    }

    public synchronized void removeClip(String text) {
        if (clipHistory.remove(text)) {
            saveHistory();
            if (updateListener != null) {
                updateListener.OnClipboardUpdated(new ArrayList<>(clipHistory));
            }
        }
    }

    private void loadHistory() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int count = prefs.getInt(KEY_CLIPS_COUNT, 0);
        clipHistory.clear();
        for (int i = 0; i < count; i++) {
            String clip = prefs.getString(KEY_CLIP_PREFIX + i, null);
            if (clip != null && !clip.trim().isEmpty()) {
                clipHistory.add(clip);
            }
        }
    }

    private void saveHistory() {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_CLIPS_COUNT, clipHistory.size());
        for (int i = 0; i < clipHistory.size(); i++) {
            editor.putString(KEY_CLIP_PREFIX + i, clipHistory.get(i));
        }
        editor.apply();
    }
}
