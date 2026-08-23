package com.example.ime;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.example.clipboard.ClipboardHistoryManager;
import com.example.database.DatabaseHelper;
import com.example.prediction.PredictionEngine;
import com.example.ui.theme.KeyboardTheme;

import java.util.List;

public class KeyboardIME extends InputMethodService implements KeyboardViewJava.OnKeyboardActionListener {

    private KeyboardViewJava keyboardView;
    private DatabaseHelper dbHelper;
    private PredictionEngine predictionEngine;
    private ClipboardHistoryManager clipboardHistoryManager;
    private Vibrator vibrator;
    private StringBuilder composingWord = new StringBuilder();
    private String lastWordContext = null;

    private KeyboardTheme.ThemeStyle activeTheme = KeyboardTheme.ThemeStyle.DARK;
    private KeyboardTheme.HeightStyle heightStyle = KeyboardTheme.HeightStyle.NORMAL;
    private int customKeyHeightDp = 52;
    private KeyboardTheme.KeyShapeStyle shapeStyle = KeyboardTheme.KeyShapeStyle.ROUNDED;

    private boolean autocorrectEnabled = false; // Auto-correct disabled per requirement
    private boolean predictionEnabled = true;
    private boolean hapticEnabled = true;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = DatabaseHelper.getInstance(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        clipboardHistoryManager = new ClipboardHistoryManager(this);
        predictionEngine = PredictionEngine.getInstance();
        predictionEngine.initAsync(dbHelper, new Runnable() {
            @Override
            public void run() {
                updatePredictions();
            }
        });
    }

    @Override
    public View onCreateInputView() {
        setExtractViewShown(false);
        loadSettings();
        if (keyboardView == null) {
            keyboardView = new KeyboardViewJava(this);
            keyboardView.setOnKeyboardActionListener(this);
        }
        applyConfiguration();
        if (clipboardHistoryManager != null) {
            keyboardView.setClipboardClips(clipboardHistoryManager.getClips());
            clipboardHistoryManager.setOnClipboardUpdatedListener(new ClipboardHistoryManager.OnClipboardUpdatedListener() {
                @Override
                public void OnClipboardUpdated(List<String> clips) {
                    if (keyboardView != null) {
                        keyboardView.setClipboardClips(clips);
                    }
                }
            });
        }
        return keyboardView;
    }

    @Override
    public boolean onEvaluateInputViewShown() {
        super.onEvaluateInputViewShown();
        return true;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        return true;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        loadSettings();
        composingWord.setLength(0);
        lastWordContext = null;
        applyConfiguration();
        if (clipboardHistoryManager != null) {
            clipboardHistoryManager.checkCurrentClip();
            if (keyboardView != null) {
                keyboardView.setClipboardClips(clipboardHistoryManager.getClips());
            }
        }
        updatePredictions();
    }

    private void loadSettings() {
        if (dbHelper == null) return;
        try {
            String t = dbHelper.getSetting("theme", "DARK");
            activeTheme = KeyboardTheme.ThemeStyle.valueOf(t);
        } catch (Exception e) { activeTheme = KeyboardTheme.ThemeStyle.DARK; }

        try {
            String h = dbHelper.getSetting("height", "NORMAL");
            heightStyle = KeyboardTheme.HeightStyle.valueOf(h);
        } catch (Exception e) { heightStyle = KeyboardTheme.HeightStyle.NORMAL; }

        try {
            String customH = dbHelper.getSetting("custom_key_height", "");
            if (!customH.isEmpty()) {
                customKeyHeightDp = Integer.parseInt(customH);
            } else {
                customKeyHeightDp = heightStyle.getKeyHeightDp();
            }
        } catch (Exception e) {
            customKeyHeightDp = heightStyle.getKeyHeightDp();
        }

        try {
            String s = dbHelper.getSetting("shape", "ROUNDED");
            shapeStyle = KeyboardTheme.KeyShapeStyle.valueOf(s);
        } catch (Exception e) { shapeStyle = KeyboardTheme.KeyShapeStyle.ROUNDED; }

        autocorrectEnabled = "1".equals(dbHelper.getSetting("autocorrect", "1"));
        predictionEnabled = "1".equals(dbHelper.getSetting("prediction", "1"));
        hapticEnabled = "1".equals(dbHelper.getSetting("haptic", "1"));
    }

    private void applyConfiguration() {
        if (keyboardView != null) {
            keyboardView.setConfiguration(activeTheme, heightStyle, customKeyHeightDp, shapeStyle);
        }
    }

    private void updatePredictions() {
        if (keyboardView == null) return;
        if (!predictionEnabled) {
            keyboardView.setPredictions(null);
            return;
        }
        final String prefix = composingWord.toString();
        final String prevContext = lastWordContext;
        predictionEngine.predictAsync(prefix, prevContext, 4, new PredictionEngine.PredictionCallback() {
            @Override
            public void onPredictionsReady(String query, List<String> predictions) {
                if (keyboardView != null && composingWord.toString().equals(query)) {
                    keyboardView.setPredictions(predictions);
                }
            }
        });
    }

    private void triggerHaptic() {
        if (hapticEnabled && vibrator != null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(15);
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onKeyPress(char c) {
        triggerHaptic();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        CharSequence selectedText = null;
        try {
            selectedText = ic.getSelectedText(0);
        } catch (Exception ignored) {}

        if (!TextUtils.isEmpty(selectedText)) {
            composingWord.setLength(0);
        }

        if (Character.isLetterOrDigit(c) || c == '\'' || c == '-' || c == '_') {
            composingWord.append(c);
            ic.commitText(String.valueOf(c), 1);
            updatePredictions();
        } else {
            if (composingWord.length() > 0) {
                lastWordContext = composingWord.toString();
                predictionEngine.learnWordAsync(lastWordContext, false, dbHelper);
                composingWord.setLength(0);
            }
            ic.commitText(String.valueOf(c), 1);
            updatePredictions();
        }
    }

    @Override
    public void onSpecialPress(String action) {
        triggerHaptic();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        if ("BACKSPACE".equals(action)) {
            CharSequence selectedText = null;
            try {
                selectedText = ic.getSelectedText(0);
            } catch (Exception ignored) {}

            if (!TextUtils.isEmpty(selectedText)) {
                composingWord.setLength(0);
                ic.commitText("", 1);
            } else if (composingWord.length() > 0) {
                composingWord.deleteCharAt(composingWord.length() - 1);
                ic.deleteSurroundingText(1, 0);
            } else {
                lastWordContext = null;
                sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DEL);
            }
            updatePredictions();

        } else if ("DELETE_ALL".equals(action)) {
            composingWord.setLength(0);
            lastWordContext = null;
            ic.performContextMenuAction(android.R.id.selectAll);
            ic.commitText("", 1);
            ic.deleteSurroundingText(10000, 10000); // Ekstra pembersihan cadangan
            updatePredictions();

        } else if ("SPACE".equals(action)) {
            if (composingWord.length() > 0) {
                String rawWord = composingWord.toString();
                String corrected = autocorrectEnabled ? predictionEngine.getAutoCorrection(rawWord) : null;
                if (corrected != null && !corrected.equalsIgnoreCase(rawWord)) {
                    ic.deleteSurroundingText(rawWord.length(), 0);
                    ic.commitText(corrected + " ", 1);
                    predictionEngine.learnWordAsync(corrected, false, dbHelper);
                    lastWordContext = corrected;
                } else {
                    lastWordContext = rawWord;
                    predictionEngine.learnWordAsync(lastWordContext, false, dbHelper);
                    ic.commitText(" ", 1);
                }
                composingWord.setLength(0);
            } else {
                ic.commitText(" ", 1);
            }
            updatePredictions();

        } else if ("ENTER".equals(action)) {
            if (composingWord.length() > 0) {
                lastWordContext = composingWord.toString();
                predictionEngine.learnWordAsync(lastWordContext, false, dbHelper);
                composingWord.setLength(0);
            }
            lastWordContext = null;
            ic.commitText("\n", 1);
            updatePredictions();

        } else if ("TAB".equals(action)) {
            ic.commitText("\t", 1);
        } else if ("CUT".equals(action)) {
            ic.performContextMenuAction(android.R.id.cut);
        } else if ("COPY".equals(action)) {
            ic.performContextMenuAction(android.R.id.copy);
        } else if ("PASTE".equals(action)) {
            ic.performContextMenuAction(android.R.id.paste);
        } else if ("SELECT_ALL".equals(action)) {
            ic.performContextMenuAction(android.R.id.selectAll);
        } else if ("REDO".equals(action)) {
            ic.performContextMenuAction(16908339); // android.R.id.redo requires API 23+, our min is 30, it is safe
        } else if ("UNDO".equals(action)) {
            ic.performContextMenuAction(16908338); // android.R.id.undo
        } else if ("HIDE".equals(action)) {
            requestHideSelf(0);
        } else if ("DPAD_UP".equals(action)) {
            sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DPAD_UP);
        } else if ("DPAD_DOWN".equals(action)) {
            sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DPAD_DOWN);
        } else if ("DPAD_LEFT".equals(action)) {
            sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DPAD_LEFT);
        } else if ("DPAD_RIGHT".equals(action)) {
            sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DPAD_RIGHT);
        } else if ("MOVE_HOME".equals(action)) {
            sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_MOVE_HOME);
        } else if ("MOVE_END".equals(action)) {
            sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_MOVE_END);
        } else if ("PAGE_UP".equals(action)) {
            sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_PAGE_UP);
        } else if ("PAGE_DOWN".equals(action)) {
            sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_PAGE_DOWN);
        } else if ("TOP".equals(action)) {
            android.view.inputmethod.ExtractedText extText = ic.getExtractedText(new android.view.inputmethod.ExtractedTextRequest(), 0);
            if (extText != null) {
                ic.setSelection(0, 0);
            }
        }
    }

    @Override
    public void onPredictionClick(String word) {
        triggerHaptic();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        if (composingWord.length() > 0) {
            ic.deleteSurroundingText(composingWord.length(), 0);
        }
        ic.commitText(word + " ", 1);
        predictionEngine.learnWordAsync(word, false, dbHelper);
        if (word.contains(" ")) {
            lastWordContext = word.substring(word.lastIndexOf(' ') + 1);
        } else {
            lastWordContext = word;
        }
        composingWord.setLength(0);
        updatePredictions();
    }

    @Override
    public void onPasteClip(String text) {
        triggerHaptic();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || text == null) return;
        ic.commitText(text, 1);
        composingWord.setLength(0);
        updatePredictions();
    }

    @Override
    public void onClearClipboard() {
        triggerHaptic();
        if (clipboardHistoryManager != null) {
            clipboardHistoryManager.clearClips();
        }
    }

    @Override
    public void onEmojiClick(String emoji) {
        triggerHaptic();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || emoji == null) return;
        if (composingWord.length() > 0) {
            String word = composingWord.toString();
            predictionEngine.learnWordAsync(word, false, dbHelper);
            composingWord.setLength(0);
        }
        ic.commitText(emoji, 1);
        updatePredictions();
    }
}
