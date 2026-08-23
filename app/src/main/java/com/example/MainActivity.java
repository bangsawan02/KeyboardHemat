package com.example;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.database.DatabaseHelper;
import com.example.ime.KeyboardViewJava;
import com.example.ui.theme.KeyboardTheme;

import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    private KeyboardViewJava previewKeyboardView;
    private EditText previewEditText;
    private EditText editorEditText;
    private ClipboardManager.OnPrimaryClipChangedListener clipListener;

    private TextView statusTextView;
    private Button enableButton;
    private Button selectButton;

    private Switch predictionSwitch;
    private Switch autocorrectSwitch;
    private Switch hapticSwitch;

    private TextView totalWordCountTextView;
    private TextView customWordCountTextView;
    private LinearLayout wordListContainer;
    private EditText searchEditText;

    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = DatabaseHelper.getInstance(this);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        rootLayout.setBackgroundColor(Color.parseColor("#F5F5F7"));

        // Header Title
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(0, dpToPx(8), 0, dpToPx(16));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("⌨️ Keyboard Hemat");
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        headerTitle.setTypeface(Typeface.DEFAULT_BOLD);
        headerTitle.setTextColor(Color.parseColor("#1A237E"));
        titleBlock.addView(headerTitle);

        TextView headerSub = new TextView(this);
        headerSub.setText("Keyboard Ringan, Presisi, hemat Memori & Cerdas");
        headerSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        headerSub.setTextColor(Color.parseColor("#5C6BC0"));
        headerSub.setPadding(0, dpToPx(2), 0, 0);
        titleBlock.addView(headerSub);

        headerLayout.addView(titleBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button openSettingsBtn = new Button(this);
        openSettingsBtn.setText("⚙️ Pengaturan");
        openSettingsBtn.setAllCaps(false);
        openSettingsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        openSettingsBtn.setTypeface(Typeface.DEFAULT_BOLD);
        openSettingsBtn.setTextColor(Color.parseColor("#1A237E"));
        GradientDrawable settingsBtnBg = new GradientDrawable();
        settingsBtnBg.setColor(Color.parseColor("#E8EAF6"));
        settingsBtnBg.setCornerRadius(dpToPx(8));
        openSettingsBtn.setBackground(settingsBtnBg);
        openSettingsBtn.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        openSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        headerLayout.addView(openSettingsBtn);

        rootLayout.addView(headerLayout);

        // 1. Activation Status Card
        rootLayout.addView(createStatusCard());

        // 2. Live Interactive Preview Card
        rootLayout.addView(createPreviewCard());

        // 3. Text Editor Card (Fitur Editor Teks)
        rootLayout.addView(createEditorCard());

        // 3. Height Style Card
        rootLayout.addView(createHeightCard());

        // 5. Shape Style Card
        rootLayout.addView(createShapeCard());

        // 6. Theme Selector Card
        rootLayout.addView(createThemeCard());

        // 7. Intelligence Settings Card
        rootLayout.addView(createIntelligenceCard());

        // 8. Dictionary Manager Card
        rootLayout.addView(createDictionaryCard());

        scrollView.addView(rootLayout);
        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Platform sync refresh
        checkKeyboardStatus();
        refreshDictionaryList();
        syncEditorWithClipboard();
        registerClipboardListener();
        if (predictionSwitch != null) {
            predictionSwitch.setChecked("1".equals(dbHelper.getSetting("prediction", "1")));
        }
        if (autocorrectSwitch != null) {
            autocorrectSwitch.setChecked("1".equals(dbHelper.getSetting("autocorrect", "1")));
        }
        if (hapticSwitch != null) {
            hapticSwitch.setChecked("1".equals(dbHelper.getSetting("haptic", "1")));
        }
        updatePreviewConfig();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterClipboardListener();
    }

    private void registerClipboardListener() {
        try {
            final ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                if (clipListener == null) {
                    clipListener = new ClipboardManager.OnPrimaryClipChangedListener() {
                        @Override
                        public void onPrimaryClipChanged() {
                            syncEditorWithClipboard();
                        }
                    };
                }
                cm.addPrimaryClipChangedListener(clipListener);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unregisterClipboardListener() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && clipListener != null) {
                cm.removePrimaryClipChangedListener(clipListener);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void syncEditorWithClipboard() {
        if (editorEditText == null) return;
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                if (text != null && !text.toString().equals(editorEditText.getText().toString())) {
                    editorEditText.setText(text);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkKeyboardStatus() {
        String packageId = getPackageName();
        boolean isEnabled = false;
        boolean isSelected = false;

        try {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                java.util.List<android.view.inputmethod.InputMethodInfo> enabledImis = imm.getEnabledInputMethodList();
                if (enabledImis != null) {
                    for (android.view.inputmethod.InputMethodInfo imi : enabledImis) {
                        if (imi.getPackageName().equals(packageId)) {
                            isEnabled = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String defaultIme = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            isSelected = defaultIme != null && defaultIme.contains(packageId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isEnabled && isSelected) {
            statusTextView.setText("✓ Status: Keyboard Hemat Aktif & Siap Digunakan!");
            statusTextView.setTextColor(Color.parseColor("#2E7D32"));
            enableButton.setVisibility(View.GONE);
            selectButton.setVisibility(View.GONE);
        } else if (isEnabled) {
            statusTextView.setText("⚠ Status: Diaktifkan tapi belum dipilih sebagai keyboard default.");
            statusTextView.setTextColor(Color.parseColor("#EF6C00"));
            enableButton.setVisibility(View.GONE);
            selectButton.setVisibility(View.VISIBLE);
        } else {
            statusTextView.setText("✖ Status: Keyboard belum diaktifkan di setelan sistem.");
            statusTextView.setTextColor(Color.parseColor("#C62828"));
            enableButton.setVisibility(View.VISIBLE);
            selectButton.setVisibility(View.GONE);
        }
    }

    private LinearLayout createCardContainer() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dpToPx(12));
        bg.setStroke(dpToPx(1), Color.parseColor("#E0E0E0"));
        layout.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(16));
        layout.setLayoutParams(lp);

        return layout;
    }

    private View createStatusCard() {
        LinearLayout layout = createCardContainer();

        TextView title = new TextView(this);
        title.setText("Status Aktivasi Keyboard");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(title);

        statusTextView = new TextView(this);
        statusTextView.setPadding(0, dpToPx(8), 0, dpToPx(12));
        statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        layout.addView(statusTextView);

        enableButton = new Button(this);
        enableButton.setText("1. Aktifkan di Setelan Sistem");
        enableButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        layout.addView(enableButton);

        selectButton = new Button(this);
        selectButton.setText("2. Pilih Sebagai Keyboard Aktif");
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        });
        layout.addView(selectButton);

        return layout;
    }

    private View createPreviewCard() {
        LinearLayout layout = createCardContainer();

        TextView title = new TextView(this);
        title.setText("Pratinjau Langsung Keyboard");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(title);

        previewEditText = new EditText(this);
        previewEditText.setHint("Ketik di sini untuk mencoba keyboard...");
        previewEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        previewEditText.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        previewEditText.setBackgroundColor(Color.parseColor("#F0F0F0"));
        layout.addView(previewEditText);

        previewKeyboardView = new KeyboardViewJava(this);
        updatePreviewConfig();

        previewEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (previewKeyboardView == null) return;
                String text = s != null ? s.toString() : "";
                String lastWord = "";
                int lastSpace = Math.max(text.lastIndexOf(' '), text.lastIndexOf('\n'));
                if (lastSpace >= 0 && lastSpace < text.length() - 1) {
                    lastWord = text.substring(lastSpace + 1);
                } else if (lastSpace < 0) {
                    lastWord = text;
                }
                final String query = lastWord;
                com.example.prediction.PredictionEngine.getInstance().predictAsync(query, 3, new com.example.prediction.PredictionEngine.PredictionCallback() {
                    @Override
                    public void onPredictionsReady(String q, List<String> predictions) {
                        if (previewKeyboardView != null) {
                            previewKeyboardView.setPredictions(predictions);
                        }
                    }
                });
            }
        });

        previewKeyboardView.setOnKeyboardActionListener(new KeyboardViewJava.OnKeyboardActionListener() {
            @Override
            public void onKeyPress(char c) {
                previewEditText.append(String.valueOf(c));
            }

            @Override
            public void onSpecialPress(String action) {
                if ("BACKSPACE".equals(action)) {
                    Editable editable = previewEditText.getText();
                    if (editable != null && editable.length() > 0) {
                        int selStart = previewEditText.getSelectionStart();
                        int selEnd = previewEditText.getSelectionEnd();
                        if (selStart >= 0 && selEnd > selStart) {
                            editable.delete(selStart, selEnd);
                        } else if (selStart > 0) {
                            editable.delete(selStart - 1, selStart);
                        } else {
                            editable.delete(editable.length() - 1, editable.length());
                        }
                    }
                } else if ("DELETE_ALL".equals(action)) {
                    previewEditText.setText("");
                } else if ("SPACE".equals(action)) {
                    previewEditText.append(" ");
                } else if ("ENTER".equals(action)) {
                    previewEditText.append("\n");
                } else if ("TAB".equals(action)) {
                    boolean spaces = "1".equals(dbHelper.getSetting("tabUsesSpaces", "1"));
                    previewEditText.append(spaces ? "    " : "\t");
                } else if ("SELECT_ALL".equals(action)) {
                    previewEditText.selectAll();
                } else if ("CUT".equals(action)) {
                    int selStart = previewEditText.getSelectionStart();
                    int selEnd = previewEditText.getSelectionEnd();
                    if (selStart >= 0 && selEnd > selStart) {
                        String cutText = previewEditText.getText().subSequence(selStart, selEnd).toString();
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("IME", cutText));
                        previewEditText.getText().delete(selStart, selEnd);
                    }
                } else if ("COPY".equals(action)) {
                    int selStart = previewEditText.getSelectionStart();
                    int selEnd = previewEditText.getSelectionEnd();
                    if (selStart >= 0 && selEnd > selStart) {
                        String copyText = previewEditText.getText().subSequence(selStart, selEnd).toString();
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("IME", copyText));
                    }
                } else if ("PASTE".equals(action)) {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                        CharSequence clip = cm.getPrimaryClip().getItemAt(0).getText();
                        if (clip != null) previewEditText.append(clip);
                    }
                } else if ("MOVE_HOME".equals(action) || "TOP".equals(action)) {
                    previewEditText.setSelection(0);
                } else if ("MOVE_END".equals(action)) {
                    previewEditText.setSelection(previewEditText.getText().length());
                } else if (action.startsWith("PAIR:")) {
                    previewEditText.append(action.substring(5));
                } else if (action.startsWith("COMMIT:")) {
                    previewEditText.append(action.substring(7));
                }
            }

            @Override
            public void onPredictionClick(String word) {
                previewEditText.append(word + " ");
            }

            @Override
            public void onPasteClip(String text) {
                previewEditText.append(text);
            }

            @Override
            public void onClearClipboard() {
                // No-op for preview
            }

            @Override
            public void onEmojiClick(String emoji) {
                previewEditText.append(emoji);
            }
        });

        layout.addView(previewKeyboardView);
        return layout;
    }

    private View createEditorCard() {
        LinearLayout layout = createCardContainer();

        TextView title = new TextView(this);
        title.setText("Editor Teks & Alat Pengeditan");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(title);

        editorEditText = new EditText(this);
        editorEditText.setHint("Ketik atau tempel teks di sini untuk menguji editor...");
        editorEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        editorEditText.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        editorEditText.setMinLines(4);
        editorEditText.setGravity(Gravity.TOP | Gravity.START);

        GradientDrawable editorBg = new GradientDrawable();
        editorBg.setColor(Color.parseColor("#FAFAFA"));
        editorBg.setCornerRadius(dpToPx(8));
        editorBg.setStroke(dpToPx(1), Color.parseColor("#CCCCCC"));
        editorEditText.setBackground(editorBg);

        LinearLayout.LayoutParams editorLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editorLp.setMargins(0, dpToPx(8), 0, dpToPx(8));
        editorEditText.setLayoutParams(editorLp);
        layout.addView(editorEditText);

        final TextView statsTextView = new TextView(this);
        statsTextView.setText("Karakter: 0 | Kata: 0 | Baris: 0");
        statsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statsTextView.setTextColor(Color.parseColor("#666666"));
        statsTextView.setPadding(0, 0, 0, dpToPx(8));
        layout.addView(statsTextView);

        editorEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                int chars = str.length();
                int words = str.trim().isEmpty() ? 0 : str.trim().split("\\s+").length;
                int lines = str.isEmpty() ? 0 : str.split("\n", -1).length;
                statsTextView.setText("Karakter: " + chars + " | Kata: " + words + " | Baris: " + lines);
            }
        });

        HorizontalScrollView scrollTools1 = new HorizontalScrollView(this);
        scrollTools1.setHorizontalScrollBarEnabled(false);

        LinearLayout tools1Layout = new LinearLayout(this);
        tools1Layout.setOrientation(LinearLayout.HORIZONTAL);
        tools1Layout.setPadding(0, dpToPx(4), 0, dpToPx(4));

        Button btnCopy = createToolButton("Salin");
        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("EditorText", editorEditText.getText().toString()));
                    Toast.makeText(MainActivity.this, "Teks disalin ke papan klip", Toast.LENGTH_SHORT).show();
                }
            }
        });
        tools1Layout.addView(btnCopy);

        Button btnPaste = createToolButton("Tempel");
        btnPaste.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                    if (text != null) {
                        int start = editorEditText.getSelectionStart();
                        int end = editorEditText.getSelectionEnd();
                        if (start < 0) start = editorEditText.length();
                        if (end < 0) end = editorEditText.length();
                        editorEditText.getText().replace(Math.min(start, end), Math.max(start, end), text);
                    }
                }
            }
        });
        tools1Layout.addView(btnPaste);

        Button btnSelectAll = createToolButton("Pilih Semua");
        btnSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                editorEditText.selectAll();
            }
        });
        tools1Layout.addView(btnSelectAll);

        Button btnClear = createToolButton("Hapus Semua");
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                editorEditText.setText("");
            }
        });
        tools1Layout.addView(btnClear);

        Button btnShare = createToolButton("Bagikan");
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String text = editorEditText.getText().toString();
                if (text.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Tidak ada teks untuk dibagikan", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Bagikan Teks Via"));
            }
        });
        tools1Layout.addView(btnShare);

        scrollTools1.addView(tools1Layout);
        layout.addView(scrollTools1);

        HorizontalScrollView scrollTools2 = new HorizontalScrollView(this);
        scrollTools2.setHorizontalScrollBarEnabled(false);

        LinearLayout tools2Layout = new LinearLayout(this);
        tools2Layout.setOrientation(LinearLayout.HORIZONTAL);
        tools2Layout.setPadding(0, dpToPx(4), 0, dpToPx(4));

        Button btnUpper = createToolButton("KAPITAL");
        btnUpper.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String text = editorEditText.getText().toString();
                editorEditText.setText(text.toUpperCase());
            }
        });
        tools2Layout.addView(btnUpper);

        Button btnLower = createToolButton("kecil");
        btnLower.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String text = editorEditText.getText().toString();
                editorEditText.setText(text.toLowerCase());
            }
        });
        tools2Layout.addView(btnLower);

        Button btnTitleCase = createToolButton("Kapital Judul");
        btnTitleCase.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String text = editorEditText.getText().toString();
                StringBuilder sb = new StringBuilder();
                boolean capitalize = true;
                for (char c : text.toCharArray()) {
                    if (Character.isWhitespace(c)) {
                        capitalize = true;
                        sb.append(c);
                    } else if (capitalize) {
                        sb.append(Character.toUpperCase(c));
                        capitalize = false;
                    } else {
                        sb.append(Character.toLowerCase(c));
                    }
                }
                editorEditText.setText(sb.toString());
            }
        });
        tools2Layout.addView(btnTitleCase);

        Button btnLeft = createToolButton("◀ Kiri");
        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                int pos = editorEditText.getSelectionStart();
                if (pos > 0) editorEditText.setSelection(pos - 1);
            }
        });
        tools2Layout.addView(btnLeft);

        Button btnRight = createToolButton("▶ Kanan");
        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                int pos = editorEditText.getSelectionStart();
                if (pos < editorEditText.length()) editorEditText.setSelection(pos + 1);
            }
        });
        tools2Layout.addView(btnRight);

        scrollTools2.addView(tools2Layout);
        layout.addView(scrollTools2);

        // Tool Row 3: Draft and Font style controls
        HorizontalScrollView scrollTools3 = new HorizontalScrollView(this);
        scrollTools3.setHorizontalScrollBarEnabled(false);

        LinearLayout tools3Layout = new LinearLayout(this);
        tools3Layout.setOrientation(LinearLayout.HORIZONTAL);
        tools3Layout.setPadding(0, dpToPx(4), 0, dpToPx(4));

        Button btnSaveDraft = createToolButton("💾 Simpan");
        btnSaveDraft.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String text = editorEditText.getText().toString();
                dbHelper.setSetting("editor_draft", text);
                Toast.makeText(MainActivity.this, "Draft teks berhasil disimpan!", Toast.LENGTH_SHORT).show();
            }
        });
        tools3Layout.addView(btnSaveDraft);

        Button btnLoadDraft = createToolButton("📂 Muat");
        btnLoadDraft.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String draft = dbHelper.getSetting("editor_draft", "");
                editorEditText.setText(draft);
                if (draft.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Tidak ada draft tersimpan.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Draft berhasil dimuat!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        tools3Layout.addView(btnLoadDraft);

        Button btnFontMinus = createToolButton("A-");
        btnFontMinus.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                float px = editorEditText.getTextSize();
                float sp = px / getResources().getDisplayMetrics().scaledDensity;
                if (sp > 10f) {
                    editorEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp - 2);
                }
            }
        });
        tools3Layout.addView(btnFontMinus);

        Button btnFontPlus = createToolButton("A+");
        btnFontPlus.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                float px = editorEditText.getTextSize();
                float sp = px / getResources().getDisplayMetrics().scaledDensity;
                if (sp < 24f) {
                    editorEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp + 2);
                }
            }
        });
        tools3Layout.addView(btnFontPlus);

        final Button btnFontType = createToolButton("Font: Sans");
        btnFontType.setOnClickListener(new View.OnClickListener() {
            private int fontState = 0; // 0=Sans, 1=Serif, 2=Monospace
            @Override public void onClick(View v) {
                fontState = (fontState + 1) % 3;
                if (fontState == 0) {
                    editorEditText.setTypeface(Typeface.SANS_SERIF);
                    btnFontType.setText("Font: Sans");
                } else if (fontState == 1) {
                    editorEditText.setTypeface(Typeface.SERIF);
                    btnFontType.setText("Font: Serif");
                } else {
                    editorEditText.setTypeface(Typeface.MONOSPACE);
                    btnFontType.setText("Font: Mono");
                }
            }
        });
        tools3Layout.addView(btnFontType);

        scrollTools3.addView(tools3Layout);
        layout.addView(scrollTools3);

        return layout;
    }

    private Button createToolButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#E8EAF6"));
        bg.setCornerRadius(dpToPx(6));
        btn.setBackground(bg);
        btn.setTextColor(Color.parseColor("#1A237E"));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(36));
        lp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void updatePreviewConfig() {
        if (previewKeyboardView == null) return;
        try {
            KeyboardTheme.ThemeStyle theme = KeyboardTheme.ThemeStyle.valueOf(dbHelper.getSetting("theme", "DARK"));
            KeyboardTheme.HeightStyle height = KeyboardTheme.HeightStyle.valueOf(dbHelper.getSetting("height", "NORMAL"));
            int customHeight = 52;
            try {
                String customH = dbHelper.getSetting("custom_key_height", "");
                if (!customH.isEmpty()) {
                    customHeight = Integer.parseInt(customH);
                } else {
                    customHeight = height.getKeyHeightDp();
                }
            } catch (Exception ignored) {
                customHeight = height.getKeyHeightDp();
            }
            KeyboardTheme.KeyShapeStyle shape = KeyboardTheme.KeyShapeStyle.valueOf(dbHelper.getSetting("shape", "ROUNDED"));

            previewKeyboardView.setConfiguration(theme, height, customHeight, shape);
        } catch (Exception ignored) {}
    }

    private View createHeightCard() {
        LinearLayout layout = createCardContainer();

        TextView title = new TextView(this);
        title.setText("Tinggi Keyboard (Ukuran Tombol)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#1A237E"));
        layout.addView(title);

        int currentHeightVal = 52;
        try {
            String customH = dbHelper.getSetting("custom_key_height", "");
            if (!customH.isEmpty()) {
                currentHeightVal = Integer.parseInt(customH);
            } else {
                KeyboardTheme.HeightStyle hs = KeyboardTheme.HeightStyle.valueOf(dbHelper.getSetting("height", "NORMAL"));
                currentHeightVal = hs.getKeyHeightDp();
            }
        } catch (Exception ignored) {
            currentHeightVal = 52;
        }

        final TextView valText = new TextView(this);
        valText.setText("Tinggi Tombol: " + currentHeightVal + " dp");
        valText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        valText.setTextColor(Color.parseColor("#333333"));
        valText.setPadding(0, dpToPx(8), 0, dpToPx(4));
        layout.addView(valText);

        final SeekBar heightSlider = new SeekBar(this);
        heightSlider.setMax(50); // range: 35dp to 85dp (min 35)
        heightSlider.setProgress(Math.max(0, Math.min(50, currentHeightVal - 35)));
        heightSlider.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));

        heightSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int heightDp = 35 + progress;
                valText.setText("Tinggi Tombol: " + heightDp + " dp");
                dbHelper.setSetting("custom_key_height", String.valueOf(heightDp));
                updatePreviewConfig();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        layout.addView(heightSlider);

        // Preset quick chips
        LinearLayout presetsLayout = new LinearLayout(this);
        presetsLayout.setOrientation(LinearLayout.HORIZONTAL);
        presetsLayout.setPadding(0, dpToPx(6), 0, dpToPx(4));

        for (final KeyboardTheme.HeightStyle hs : KeyboardTheme.HeightStyle.values()) {
            Button btn = new Button(this);
            btn.setText(hs.getDisplayName() + " (" + hs.getKeyHeightDp() + ")");
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            btn.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
            
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#EDE7F6"));
            bg.setCornerRadius(dpToPx(6));
            btn.setBackground(bg);
            btn.setTextColor(Color.parseColor("#4527A0"));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, dpToPx(34), 1.0f);
            lp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int val = hs.getKeyHeightDp();
                    heightSlider.setProgress(val - 35);
                    valText.setText("Tinggi Tombol: " + val + " dp");
                    dbHelper.setSetting("custom_key_height", String.valueOf(val));
                    dbHelper.setSetting("height", hs.name());
                    updatePreviewConfig();
                }
            });

            presetsLayout.addView(btn);
        }

        layout.addView(presetsLayout);
        return layout;
    }

    private View createShapeCard() {
        LinearLayout layout = createCardContainer();

        TextView title = new TextView(this);
        title.setText("Bentuk & Gaya Tombol");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#1A237E"));
        layout.addView(title);

        RadioGroup group = new RadioGroup(this);
        String currentShape = dbHelper.getSetting("shape", "ROUNDED");

        int index = 0;
        for (final KeyboardTheme.KeyShapeStyle ss : KeyboardTheme.KeyShapeStyle.values()) {
            RadioButton rb = new RadioButton(this);
            rb.setId(200 + index);
            rb.setText(ss.getDisplayName());
            rb.setTextColor(Color.parseColor("#333333"));
            if (ss.name().equals(currentShape)) rb.setChecked(true);

            rb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    dbHelper.setSetting("shape", ss.name());
                    updatePreviewConfig();
                }
            });
            group.addView(rb);
            index++;
        }

        layout.addView(group);
        return layout;
    }

    private View createThemeCard() {
        LinearLayout layout = createCardContainer();

        TextView title = new TextView(this);
        title.setText("Tema Warna Keyboard");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#1A237E"));
        layout.addView(title);

        RadioGroup group = new RadioGroup(this);
        String currentTheme = dbHelper.getSetting("theme", "DARK");

        int index = 0;
        for (final KeyboardTheme.ThemeStyle ts : KeyboardTheme.ThemeStyle.values()) {
            RadioButton rb = new RadioButton(this);
            rb.setId(300 + index);
            rb.setText(ts.getDisplayName());
            rb.setTextColor(Color.parseColor("#333333"));
            if (ts.name().equals(currentTheme)) rb.setChecked(true);

            rb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    dbHelper.setSetting("theme", ts.name());
                    updatePreviewConfig();
                }
            });
            group.addView(rb);
            index++;
        }

        layout.addView(group);
        return layout;
    }

    private View createIntelligenceCard() {
        LinearLayout layout = createCardContainer();

        TextView title = new TextView(this);
        title.setText("Fitur Cerdas & Respons");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#1A237E"));
        layout.addView(title);

        predictionSwitch = new Switch(this);
        predictionSwitch.setText("Prediksi Teks Cerdas");
        predictionSwitch.setTextColor(Color.parseColor("#333333"));
        predictionSwitch.setChecked("1".equals(dbHelper.getSetting("prediction", "1")));
        predictionSwitch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dbHelper.setSetting("prediction", predictionSwitch.isChecked() ? "1" : "0");
                updatePreviewConfig();
            }
        });
        layout.addView(predictionSwitch);

        autocorrectSwitch = new Switch(this);
        autocorrectSwitch.setText("Koreksi Otomatis (Spasi)");
        autocorrectSwitch.setTextColor(Color.parseColor("#333333"));
        autocorrectSwitch.setChecked("1".equals(dbHelper.getSetting("autocorrect", "1")));
        autocorrectSwitch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dbHelper.setSetting("autocorrect", autocorrectSwitch.isChecked() ? "1" : "0");
                updatePreviewConfig();
            }
        });
        layout.addView(autocorrectSwitch);

        hapticSwitch = new Switch(this);
        hapticSwitch.setText("Getar Sentuhan (Haptic Feedback)");
        hapticSwitch.setTextColor(Color.parseColor("#333333"));
        hapticSwitch.setChecked("1".equals(dbHelper.getSetting("haptic", "1")));
        hapticSwitch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dbHelper.setSetting("haptic", hapticSwitch.isChecked() ? "1" : "0");
                updatePreviewConfig();
            }
        });
        layout.addView(hapticSwitch);

        Button openFullSettingsBtn = new Button(this);
        openFullSettingsBtn.setText("⚙️ Buka Halaman Pengaturan Lengkap");
        openFullSettingsBtn.setAllCaps(false);
        openFullSettingsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        openFullSettingsBtn.setTypeface(Typeface.DEFAULT_BOLD);
        openFullSettingsBtn.setTextColor(Color.parseColor("#1A237E"));
        GradientDrawable fullSettingsBtnBg = new GradientDrawable();
        fullSettingsBtnBg.setColor(Color.parseColor("#E8EAF6"));
        fullSettingsBtnBg.setCornerRadius(dpToPx(8));
        openFullSettingsBtn.setBackground(fullSettingsBtnBg);
        openFullSettingsBtn.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, dpToPx(10), 0, 0);
        openFullSettingsBtn.setLayoutParams(btnLp);
        openFullSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        layout.addView(openFullSettingsBtn);

        return layout;
    }

    private EditText addShortcutInput;
    private EditText addReplacementInput;
    private View searchClearBtn;

    private View createDictionaryCard() {
        LinearLayout layout = createCardContainer();

        // 1. Header with Title and Badges
        LinearLayout topHeader = new LinearLayout(this);
        topHeader.setOrientation(LinearLayout.VERTICAL);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("⚡ AutoText (Teks Otomatis)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#1A237E"));
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnOpenAddDialog = new Button(this);
        btnOpenAddDialog.setText("+ Baru");
        btnOpenAddDialog.setAllCaps(false);
        btnOpenAddDialog.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnOpenAddDialog.setTypeface(Typeface.DEFAULT_BOLD);
        btnOpenAddDialog.setTextColor(Color.WHITE);
        GradientDrawable openAddBg = new GradientDrawable();
        openAddBg.setColor(Color.parseColor("#1A237E"));
        openAddBg.setCornerRadius(dpToPx(6));
        btnOpenAddDialog.setBackground(openAddBg);
        btnOpenAddDialog.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        btnOpenAddDialog.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showAddOrEditAutoTextDialog(null);
            }
        });
        titleRow.addView(btnOpenAddDialog);
        topHeader.addView(titleRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Ketik pintasan singkat (cth: 'gith', 'opt') untuk otomatis memunculkan teks/template panjang.");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subtitle.setTextColor(Color.parseColor("#666666"));
        subtitle.setPadding(0, dpToPx(2), 0, dpToPx(8));
        topHeader.addView(subtitle);

        // Stats Badges Row
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setPadding(0, 0, 0, dpToPx(10));

        customWordCountTextView = new TextView(this);
        customWordCountTextView.setText("⚡ Total AutoText: 0 item");
        customWordCountTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        customWordCountTextView.setTypeface(Typeface.DEFAULT_BOLD);
        customWordCountTextView.setTextColor(Color.parseColor("#4527A0"));
        customWordCountTextView.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        GradientDrawable customBadgeBg = new GradientDrawable();
        customBadgeBg.setColor(Color.parseColor("#EDE7F6"));
        customBadgeBg.setCornerRadius(dpToPx(12));
        customWordCountTextView.setBackground(customBadgeBg);
        statsRow.addView(customWordCountTextView);

        topHeader.addView(statsRow);
        layout.addView(topHeader);

        // 2. Fast Inline Add Bar
        LinearLayout addBar = new LinearLayout(this);
        addBar.setOrientation(LinearLayout.VERTICAL);
        addBar.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        GradientDrawable addBarBg = new GradientDrawable();
        addBarBg.setColor(Color.parseColor("#F8F9FA"));
        addBarBg.setCornerRadius(dpToPx(10));
        addBarBg.setStroke(dpToPx(1), Color.parseColor("#E0E0E0"));
        addBar.setBackground(addBarBg);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        addShortcutInput = new EditText(this);
        addShortcutInput.setHint("Pintasan (cth: gith)");
        addShortcutInput.setHintTextColor(Color.parseColor("#9E9E9E"));
        addShortcutInput.setTextColor(Color.parseColor("#212121"));
        addShortcutInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        addShortcutInput.setTypeface(Typeface.DEFAULT_BOLD);
        addShortcutInput.setBackground(null);
        addShortcutInput.setSingleLine(true);
        addShortcutInput.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        inputRow.addView(addShortcutInput, new LinearLayout.LayoutParams(dpToPx(110), ViewGroup.LayoutParams.WRAP_CONTENT));

        View verticalSep = new View(this);
        verticalSep.setBackgroundColor(Color.parseColor("#E0E0E0"));
        inputRow.addView(verticalSep, new LinearLayout.LayoutParams(dpToPx(1), dpToPx(24)));

        addReplacementInput = new EditText(this);
        addReplacementInput.setHint("Teks pengganti / template...");
        addReplacementInput.setHintTextColor(Color.parseColor("#9E9E9E"));
        addReplacementInput.setTextColor(Color.parseColor("#212121"));
        addReplacementInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        addReplacementInput.setBackground(null);
        addReplacementInput.setSingleLine(true);
        addReplacementInput.setPadding(dpToPx(8), dpToPx(6), dpToPx(6), dpToPx(6));
        inputRow.addView(addReplacementInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnAddWord = new Button(this);
        btnAddWord.setText("+ Simpan");
        btnAddWord.setAllCaps(false);
        btnAddWord.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnAddWord.setTypeface(Typeface.DEFAULT_BOLD);
        btnAddWord.setTextColor(Color.WHITE);
        GradientDrawable btnAddBg = new GradientDrawable();
        btnAddBg.setColor(Color.parseColor("#1A237E"));
        btnAddBg.setCornerRadius(dpToPx(6));
        btnAddWord.setBackground(btnAddBg);
        btnAddWord.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
        btnAddWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performAddAutoText();
            }
        });
        inputRow.addView(btnAddWord);

        addBar.addView(inputRow);
        layout.addView(addBar);

        // Spacer
        View space1 = new View(this);
        layout.addView(space1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(10)));

        // 3. Search Bar with Clear Button
        LinearLayout searchContainer = new LinearLayout(this);
        searchContainer.setOrientation(LinearLayout.HORIZONTAL);
        searchContainer.setGravity(Gravity.CENTER_VERTICAL);
        searchContainer.setPadding(dpToPx(10), dpToPx(2), dpToPx(10), dpToPx(2));
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.parseColor("#F1F3F4"));
        searchBg.setCornerRadius(dpToPx(8));
        searchContainer.setBackground(searchBg);

        TextView searchIcon = new TextView(this);
        searchIcon.setText("🔍");
        searchIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        searchIcon.setPadding(0, 0, dpToPx(6), 0);
        searchContainer.addView(searchIcon);

        searchEditText = new EditText(this);
        searchEditText.setHint("Cari AutoText (pintasan / isi teks)...");
        searchEditText.setHintTextColor(Color.parseColor("#888888"));
        searchEditText.setTextColor(Color.parseColor("#212121"));
        searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        searchEditText.setBackground(null);
        searchEditText.setSingleLine(true);
        searchEditText.setPadding(0, dpToPx(6), 0, dpToPx(6));
        searchContainer.addView(searchEditText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        final TextView clearBtn = new TextView(this);
        clearBtn.setText("✕");
        clearBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        clearBtn.setTypeface(Typeface.DEFAULT_BOLD);
        clearBtn.setTextColor(Color.parseColor("#757575"));
        clearBtn.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4));
        clearBtn.setVisibility(View.GONE);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEditText.setText("");
            }
        });
        searchClearBtn = clearBtn;
        searchContainer.addView(clearBtn);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearBtn.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                refreshDictionaryList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        layout.addView(searchContainer);

        // Spacer
        View space2 = new View(this);
        layout.addView(space2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(8)));

        // 4. Scrollable Word List (AutoText list matching screenshot style)
        ScrollView wordScrollView = new ScrollView(this);
        wordScrollView.setVerticalScrollBarEnabled(true);
        GradientDrawable scrollBorder = new GradientDrawable();
        scrollBorder.setColor(Color.parseColor("#FFFFFF"));
        scrollBorder.setCornerRadius(dpToPx(8));
        scrollBorder.setStroke(dpToPx(1), Color.parseColor("#E5E7EB"));
        wordScrollView.setBackground(scrollBorder);

        wordListContainer = new LinearLayout(this);
        wordListContainer.setOrientation(LinearLayout.VERTICAL);
        wordListContainer.setPadding(0, dpToPx(2), 0, dpToPx(2));
        wordScrollView.addView(wordListContainer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        layout.addView(wordScrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(280)));

        // 5. Action Footer Toolbar
        LinearLayout footerActions = new LinearLayout(this);
        footerActions.setOrientation(LinearLayout.HORIZONTAL);
        footerActions.setPadding(0, dpToPx(12), 0, 0);

        Button clearAllBtn = new Button(this);
        clearAllBtn.setText("🗑️ Hapus Semua");
        clearAllBtn.setAllCaps(false);
        clearAllBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        clearAllBtn.setTextColor(Color.parseColor("#C62828"));
        GradientDrawable clearAllBg = new GradientDrawable();
        clearAllBg.setColor(Color.parseColor("#FFEBEE"));
        clearAllBg.setCornerRadius(dpToPx(6));
        clearAllBtn.setBackground(clearAllBg);
        clearAllBtn.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
        clearAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Hapus Semua AutoText?")
                        .setMessage("Semua data AutoText akan dihapus.")
                        .setPositiveButton("Hapus Semua", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                dbHelper.deleteAllAutoText();
                                com.example.prediction.PredictionEngine.getInstance().reloadAutoTextAsync(dbHelper);
                                refreshDictionaryList();
                                Toast.makeText(MainActivity.this, "Semua AutoText telah dihapus", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            }
        });
        LinearLayout.LayoutParams clearAllLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        clearAllLp.setMargins(0, 0, dpToPx(6), 0);
        footerActions.addView(clearAllBtn, clearAllLp);

        Button resetDefaultBtn = new Button(this);
        resetDefaultBtn.setText("🔄 Reset Contoh Gambar");
        resetDefaultBtn.setAllCaps(false);
        resetDefaultBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        resetDefaultBtn.setTextColor(Color.parseColor("#1565C0"));
        GradientDrawable resetBg = new GradientDrawable();
        resetBg.setColor(Color.parseColor("#E3F2FD"));
        resetBg.setCornerRadius(dpToPx(6));
        resetDefaultBtn.setBackground(resetBg);
        resetDefaultBtn.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
        resetDefaultBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dbHelper.resetDefaultAutoText();
                com.example.prediction.PredictionEngine.getInstance().reloadAutoTextAsync(dbHelper);
                refreshDictionaryList();
                Toast.makeText(MainActivity.this, "AutoText bawaan berhasil dimuat ulang!", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        resetLp.setMargins(dpToPx(6), 0, 0, 0);
        footerActions.addView(resetDefaultBtn, resetLp);

        layout.addView(footerActions);

        return layout;
    }

    private void performAddAutoText() {
        if (addShortcutInput == null || addReplacementInput == null) return;
        String shortcut = addShortcutInput.getText().toString().trim();
        String replacement = addReplacementInput.getText().toString().trim();

        if (shortcut.isEmpty() || replacement.isEmpty()) {
            Toast.makeText(this, "Pintasan dan teks pengganti tidak boleh kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        dbHelper.insertAutoText(shortcut, replacement, true);
        com.example.prediction.PredictionEngine.getInstance().addAutoTextAsync(shortcut, replacement);

        addShortcutInput.setText("");
        addReplacementInput.setText("");
        Toast.makeText(this, "AutoText '" + shortcut + "' berhasil disimpan!", Toast.LENGTH_SHORT).show();

        refreshDictionaryList();
    }

    private void showAddOrEditAutoTextDialog(final DatabaseHelper.AutoTextItem itemToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(itemToEdit == null ? "Tambah AutoText Baru" : "Edit AutoText (" + itemToEdit.shortcut + ")");

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(12));

        TextView shortcutLabel = new TextView(this);
        shortcutLabel.setText("Pintasan / Shortcut (cth: gith, opt, ccc):");
        shortcutLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        shortcutLabel.setTypeface(Typeface.DEFAULT_BOLD);
        shortcutLabel.setTextColor(Color.parseColor("#374151"));
        dialogLayout.addView(shortcutLabel);

        final EditText shortcutField = new EditText(this);
        shortcutField.setHint("Ketik pintasan...");
        if (itemToEdit != null) shortcutField.setText(itemToEdit.shortcut);
        dialogLayout.addView(shortcutField);

        TextView repLabel = new TextView(this);
        repLabel.setText("Teks Pengganti / Template:");
        repLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        repLabel.setTypeface(Typeface.DEFAULT_BOLD);
        repLabel.setTextColor(Color.parseColor("#374151"));
        repLabel.setPadding(0, dpToPx(12), 0, 0);
        dialogLayout.addView(repLabel);

        final EditText repField = new EditText(this);
        repField.setHint("Ketik teks panjang, email, kaomoji, template...");
        repField.setMinLines(2);
        if (itemToEdit != null) repField.setText(itemToEdit.replacement);
        dialogLayout.addView(repField);

        builder.setView(dialogLayout);

        builder.setPositiveButton("Simpan", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                String sc = shortcutField.getText().toString().trim();
                String rep = repField.getText().toString().trim();
                if (sc.isEmpty() || rep.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Pintasan dan teks tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (itemToEdit == null) {
                    dbHelper.insertAutoText(sc, rep, true);
                    com.example.prediction.PredictionEngine.getInstance().addAutoTextAsync(sc, rep);
                    Toast.makeText(MainActivity.this, "AutoText berhasil ditambahkan!", Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.updateAutoText(itemToEdit.id, sc, rep);
                    com.example.prediction.PredictionEngine.getInstance().reloadAutoTextAsync(dbHelper);
                    Toast.makeText(MainActivity.this, "AutoText berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                }
                refreshDictionaryList();
            }
        });
        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    private void showItemActionMenu(final DatabaseHelper.AutoTextItem item) {
        // Matching the exact screenshot popup menu ("pp" header with "Delete", "Edit", "Copy")
        final String[] options = {"Delete", "Edit", "Salin Teks"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.shortcut);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) { // Delete
                    dbHelper.deleteAutoText(item.id);
                    com.example.prediction.PredictionEngine.getInstance().reloadAutoTextAsync(dbHelper);
                    Toast.makeText(MainActivity.this, "AutoText '" + item.shortcut + "' dihapus", Toast.LENGTH_SHORT).show();
                    refreshDictionaryList();
                } else if (which == 1) { // Edit
                    showAddOrEditAutoTextDialog(item);
                } else if (which == 2) { // Salin Teks
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        ClipData clip = ClipData.newPlainText("AutoText", item.replacement);
                        cm.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, "Teks disalin ke clipboard", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        builder.show();
    }

    private void refreshDictionaryList() {
        if (dbHelper == null) return;
        if (customWordCountTextView != null) {
            customWordCountTextView.setText("⚡ Total AutoText: " + dbHelper.getAutoTextCount() + " item");
        }

        if (wordListContainer == null) return;
        wordListContainer.removeAllViews();

        String query = searchEditText != null ? searchEditText.getText().toString() : "";
        List<DatabaseHelper.AutoTextItem> items = dbHelper.getAllAutoText(query);

        if (items == null || items.isEmpty()) {
            LinearLayout emptyBox = new LinearLayout(this);
            emptyBox.setOrientation(LinearLayout.VERTICAL);
            emptyBox.setGravity(Gravity.CENTER);
            emptyBox.setPadding(0, dpToPx(32), 0, dpToPx(32));

            TextView emptyIcon = new TextView(this);
            emptyIcon.setText("🍃");
            emptyIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            emptyIcon.setGravity(Gravity.CENTER);
            emptyBox.addView(emptyIcon);

            TextView emptyText = new TextView(this);
            if (query != null && !query.trim().isEmpty()) {
                emptyText.setText("Tidak ada AutoText yang cocok dengan '" + query + "'");
            } else {
                emptyText.setText("Belum ada AutoText.\nKetik di atas untuk menambah AutoText baru.");
            }
            emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            emptyText.setTextColor(Color.parseColor("#888888"));
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, dpToPx(4), 0, 0);
            emptyBox.addView(emptyText);

            wordListContainer.addView(emptyBox);
            return;
        }

        // Render matching exact list style from screenshot
        for (final DatabaseHelper.AutoTextItem item : items) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
            row.setClickable(true);
            row.setFocusable(true);

            // Ripple background
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            row.setBackgroundResource(outValue.resourceId);

            // Line 1: Shortcut Title (Bold, large text as in screenshot: ccc, gg, gith, hh, ii, etc.)
            TextView shortcutTv = new TextView(this);
            shortcutTv.setText(item.shortcut);
            shortcutTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            shortcutTv.setTypeface(Typeface.DEFAULT_BOLD);
            shortcutTv.setTextColor(Color.parseColor("#111827"));
            row.addView(shortcutTv);

            // Line 2: Replacement Text (Greyish subtext as in screenshot)
            TextView replacementTv = new TextView(this);
            replacementTv.setText(item.replacement);
            replacementTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            replacementTv.setTextColor(Color.parseColor("#4B5563"));
            replacementTv.setPadding(0, dpToPx(2), 0, 0);
            replacementTv.setMaxLines(2);
            replacementTv.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(replacementTv);

            // Click or Long-Click to show popup menu matching screenshot
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showItemActionMenu(item);
                }
            };
            row.setOnClickListener(clickListener);
            row.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    showItemActionMenu(item);
                    return true;
                }
            });

            wordListContainer.addView(row);

            // Thin divider line as in screenshot
            View divider = new View(this);
            divider.setBackgroundColor(Color.parseColor("#E5E7EB"));
            wordListContainer.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1)));
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
