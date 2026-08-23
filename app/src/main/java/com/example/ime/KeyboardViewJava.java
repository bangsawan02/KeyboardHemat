package com.example.ime;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.ui.theme.KeyboardTheme;

import java.util.ArrayList;
import java.util.List;

public class KeyboardViewJava extends LinearLayout {

    public interface OnKeyboardActionListener {
        void onKeyPress(char c);
        void onSpecialPress(String action);
        void onPredictionClick(String word);
        void onPasteClip(String text);
        void onClearClipboard();
        void onEmojiClick(String emoji);
    }

    private OnKeyboardActionListener listener;

    private KeyboardTheme.ThemeStyle activeTheme = KeyboardTheme.ThemeStyle.DARK;
    private KeyboardTheme.HeightStyle heightStyle = KeyboardTheme.HeightStyle.NORMAL;
    private int customKeyHeightDp = 0;
    private KeyboardTheme.KeyShapeStyle shapeStyle = KeyboardTheme.KeyShapeStyle.ROUNDED;

    private boolean isShifted = false;
    private int currentLayoutPage = 0; // 0 = Letters, 1 = Symbols 1, 2 = Symbols 2

    private LinearLayout suggestionsContainer;
    private LinearLayout keyboardGridContainer;
    private LinearLayout clipboardContainer;
    private LinearLayout emojiContainer;

    private List<String> currentPredictions = new ArrayList<>();
    private List<String> clipboardClips = new ArrayList<>();
    private boolean isClipboardVisible = false;
    private boolean isEmojiVisible = false;
    private int selectedEmojiCategory = 0;

    private static final String[] EMOJI_CATEGORIES = {"😀 Wajah", "👍 Gestur", "❤️ Simbol", "🎉 Objek", "🌟 Lainnya"};

    private static final String[][] EMOJI_DATA = {
        // 0: Wajah & Emosi
        {
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
            "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔",
            "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "🤥",
            "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮",
            "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "🥸", "😎",
            "🤓", "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳", "🥺",
            "😦", "😧", "📁", "😭", "😱", "😖", "😣", "😞", "😓", "😩",
            "😫", "🥱", "😤", "😡", "😠", "🤬", "😈", "👿", "💀", "💩"
        },
        // 1: Gestur & Orang
        {
            "👍", "👎", "👌", "🤌", "🤏", "✌️", "🤞", "🤟", "🤘", "🤙",
            "👈", "👉", "👆", "🖕", "👇", "☝️", "👋", "🤚", "🖐️", "✋",
            "🖖", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳",
            "💪", "🦾", "🦵", "🦶", "👂", "🦻", "👃", "🫀", "🫁", "🧠",
            "👀", "👁️", "👅", "👄", "👶", "🧒", "👦", "👧", "🧑", "👱",
            "👨", "🧔", "👩", "🧓", "👴", "👵", "👮", "🕵️", "👷", "💂"
        },
        // 2: Hati & Simbol
        {
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
            "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "☮️",
            "✝️", "☪️", "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️", "☦️", "🛐",
            "💯", "🔥", "✨", "🌟", "💫", "💥", "💢", "💦", "💧", "💤",
            "☀️", "🌙", "⭐", "☁️", "⛅", "🌧️", "⚡", "❄️", "🌈", "🔥",
            "✔️", "❌", "❓", "❗", "⚠️", "⛔", "🚫", "✅", "🔔", "🔕"
        },
        // 3: Objek & Aktivitas
        {
            "🎉", "🎊", "🎁", "🎈", "🎂", "🍰", "🍻", "🥂", "☕", "🍵",
            "🍕", "🍔", "🍟", "🍿", "🍩", "🍫", "🍬", "🍭", "🍦", "🍎",
            "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🎱", "🏓", "🏸", "🥊",
            "🎯", "🎮", "🕹️", "🎲", "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️",
            "🚓", "🚑", "🚒", "🚐", "🚚", "🚛", "🚜", "🛵", "🏍️", "🚲",
            "✈️", "🚀", "🛸", "🚁", "⛵", "🚤", "🛳️", "⛴️", "🚢", "🏠"
        },
        // 4: Lainnya (Hewan, Alam, Tumbuhan)
        {
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
            "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤", "🦆",
            "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋",
            "🌸", "💮", "🏵️", "🌹", "🥀", "🌺", "🌻", "🌼", "🌷", "🌱",
            "🌲", "🌳", "🌴", "🌵", "🌾", "🌿", "☘️", "🍀", "🍁", "🍂"
        }
    };

    private PopupWindow keyPopup;
    private TextView popupTextView;
    private final Handler popupHandler = new Handler(Looper.getMainLooper());
    private final Runnable hidePopupRunnable = new Runnable() {
        @Override
        public void run() {
            if (keyPopup != null && keyPopup.isShowing()) {
                try {
                    keyPopup.dismiss();
                } catch (Exception ignored) {}
            }
        }
    };

    private static final String[][] QWERTY_ROW_1 = {{"q","w","e","r","t","y","u","i","o","p"}};
    private static final String[][] QWERTY_ROW_2 = {{"a","s","d","f","g","h","j","k","l"}};
    private static final String[] QWERTY_ROW_3 = {"z","x","c","v","b","n","m"};

    private static final String[][] SYM_ROW_1 = {{"1","2","3","4","5","6","7","8","9","0"}};
    private static final String[][] SYM_ROW_2 = {{"@","#","$","%","&","-","+","(",")"}};
    private static final String[] SYM_ROW_3 = {"*","\"","'",":",";","!","?"};

    private static final String[][] SYM2_ROW_1 = {{"~","`","|","•","√","π","÷","×","¶","Δ"}};
    private static final String[][] SYM2_ROW_2 = {{"£","¥","$","¢","^","°","=","{","}"}};
    private static final String[] SYM2_ROW_3 = {"\\","<",">","[","]","™","®"};

    public KeyboardViewJava(Context context) {
        super(context);
        init();
    }

    public KeyboardViewJava(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        suggestionsContainer = new LinearLayout(getContext());
        suggestionsContainer.setOrientation(HORIZONTAL);
        suggestionsContainer.setGravity(Gravity.CENTER_VERTICAL);
        suggestionsContainer.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

        keyboardGridContainer = new LinearLayout(getContext());
        keyboardGridContainer.setOrientation(VERTICAL);

        clipboardContainer = new LinearLayout(getContext());
        clipboardContainer.setOrientation(VERTICAL);
        clipboardContainer.setVisibility(GONE);

        emojiContainer = new LinearLayout(getContext());
        emojiContainer.setOrientation(VERTICAL);
        emojiContainer.setVisibility(GONE);

        addView(suggestionsContainer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(42)));
        addView(keyboardGridContainer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(clipboardContainer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(emojiContainer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        renderKeyboard();
    }

    public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
        this.listener = listener;
    }

    private android.graphics.drawable.Drawable.ConstantState normalKeyBgCache;
    private android.graphics.drawable.Drawable.ConstantState specialKeyBgCache;
    private android.graphics.drawable.Drawable.ConstantState actionKeyBgCache;

    public void setConfiguration(KeyboardTheme.ThemeStyle theme, KeyboardTheme.HeightStyle height,
                                 KeyboardTheme.KeyShapeStyle shape) {
        setConfiguration(theme, height, 0, shape);
    }

    public void setConfiguration(KeyboardTheme.ThemeStyle theme, KeyboardTheme.HeightStyle height,
                                 int customHeightDp, KeyboardTheme.KeyShapeStyle shape) {
        this.activeTheme = theme != null ? theme : KeyboardTheme.ThemeStyle.DARK;
        this.heightStyle = height != null ? height : KeyboardTheme.HeightStyle.NORMAL;
        this.customKeyHeightDp = customHeightDp;
        this.shapeStyle = shape != null ? shape : KeyboardTheme.KeyShapeStyle.ROUNDED;
        
        normalKeyBgCache = null;
        specialKeyBgCache = null;
        actionKeyBgCache = null;
        
        renderKeyboard();
    }

    public void setPredictions(List<String> predictions) {
        this.currentPredictions = predictions != null ? predictions : new ArrayList<String>();
        renderSuggestions();
    }

    public void setClipboardClips(List<String> clips) {
        this.clipboardClips = clips != null ? clips : new ArrayList<String>();
        if (isClipboardVisible) {
            updateClipboardContainer();
        }
    }

    private void renderKeyboard() {
        KeyboardTheme.ColorPalette colors = KeyboardTheme.getColors(activeTheme);
        setBackgroundColor(colors.backgroundColor);

        renderSuggestions();
        renderGrid();

        if (isClipboardVisible) {
            keyboardGridContainer.setVisibility(GONE);
            emojiContainer.setVisibility(GONE);
            clipboardContainer.setVisibility(VISIBLE);
            updateClipboardContainer();
        } else if (isEmojiVisible) {
            keyboardGridContainer.setVisibility(GONE);
            clipboardContainer.setVisibility(GONE);
            emojiContainer.setVisibility(VISIBLE);
            updateEmojiContainer();
        } else {
            keyboardGridContainer.setVisibility(VISIBLE);
            clipboardContainer.setVisibility(GONE);
            emojiContainer.setVisibility(GONE);
        }
    }

    private void renderSuggestions() {
        suggestionsContainer.removeAllViews();
        KeyboardTheme.ColorPalette colors = KeyboardTheme.getColors(activeTheme);

        GradientDrawable sugBg = new GradientDrawable();
        sugBg.setColor(colors.suggestionBackgroundColor);
        sugBg.setCornerRadius(dpToPx(shapeStyle.getCornerRadiusDp()));
        suggestionsContainer.setBackground(sugBg);

        // Edit Layout Toggle Button
        TextView editBtn = new TextView(getContext());
        editBtn.setText(currentLayoutPage == 3 ? "Abc" : "Edit");
        editBtn.setTextColor(colors.suggestionTextColor);
        editBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        editBtn.setTypeface(Typeface.DEFAULT_BOLD);
        editBtn.setGravity(Gravity.CENTER);
        editBtn.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));
        editBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                isClipboardVisible = false;
                isEmojiVisible = false;
                currentLayoutPage = currentLayoutPage == 3 ? 0 : 3;
                renderKeyboard();
            }
        });
        suggestionsContainer.addView(editBtn);

        // Clipboard Toggle Button
        TextView toggleBtn = new TextView(getContext());
        toggleBtn.setText("📋");
        toggleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        toggleBtn.setGravity(Gravity.CENTER);
        toggleBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        toggleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleClipboard();
            }
        });
        suggestionsContainer.addView(toggleBtn);

        // Emoji Picker Toggle Button
        TextView emojiBtn = new TextView(getContext());
        emojiBtn.setText("😊");
        emojiBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        emojiBtn.setGravity(Gravity.CENTER);
        emojiBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        emojiBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleEmoji();
            }
        });
        suggestionsContainer.addView(emojiBtn);

        // Strictly 1 horizontal row/line for suggestions
        HorizontalScrollView scroll = new HorizontalScrollView(getContext());
        scroll.setHorizontalScrollBarEnabled(false);

        LinearLayout sugRow = new LinearLayout(getContext());
        sugRow.setOrientation(HORIZONTAL);
        sugRow.setGravity(Gravity.CENTER_VERTICAL);

        if (currentPredictions != null && !currentPredictions.isEmpty()) {
            for (final String word : currentPredictions) {
                TextView tv = new TextView(getContext());
                tv.setText(word);
                tv.setTextColor(colors.suggestionTextColor);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                tv.setTypeface(Typeface.DEFAULT_BOLD);
                tv.setSingleLine(true);
                tv.setEllipsize(TextUtils.TruncateAt.END);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(dpToPx(14), dpToPx(4), dpToPx(14), dpToPx(4));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(dpToPx(4), 0, dpToPx(4), 0);
                tv.setLayoutParams(lp);

                tv.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showKeyPopup(v, word);
                        if (listener != null) listener.onPredictionClick(word);
                    }
                });
                sugRow.addView(tv);
            }
        }

        scroll.addView(sugRow);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        suggestionsContainer.addView(scroll, scrollLp);
    }

    private void renderGrid() {
        keyboardGridContainer.removeAllViews();
        KeyboardTheme.ColorPalette colors = KeyboardTheme.getColors(activeTheme);
        int keyHeightPx = dpToPx(customKeyHeightDp > 0 ? customKeyHeightDp : heightStyle.getKeyHeightDp());

        if (currentLayoutPage == 0) {
            // Page 0: QWERTY
            addSimpleKeyRow(QWERTY_ROW_1[0], colors, keyHeightPx);
            addSimpleKeyRow(QWERTY_ROW_2[0], colors, keyHeightPx);

            // Row 3: Shift + Keys + Delete
            LinearLayout row3 = new LinearLayout(getContext());
            row3.setOrientation(HORIZONTAL);
            row3.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Shift Button
            row3.addView(createSpecialKey("⇧", 1.5f, isShifted ? colors.actionKeyBackgroundColor : colors.specialKeyBackgroundColor,
                    isShifted ? colors.actionKeyTextColor : colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                        @Override public void onClick(View v) {
                            showKeyPopup(v, "⇧");
                            isShifted = !isShifted;
                            renderGrid();
                        }
                    }));

            for (final String k : QWERTY_ROW_3) {
                final String letter = isShifted ? k.toUpperCase() : k;
                row3.addView(createNormalKey(letter, 1.0f, colors, keyHeightPx, new OnClickListener() {
                    @Override public void onClick(View v) {
                        showKeyPopup(v, letter);
                        if (listener != null) listener.onKeyPress(letter.charAt(0));
                        if (isShifted) {
                            isShifted = false;
                            renderGrid();
                        }
                    }
                }));
            }

            // Backspace Button
            row3.addView(createSpecialKey("⌫", 1.5f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "⌫");
                    if (listener != null) listener.onSpecialPress("BACKSPACE");
                }
            }));

            keyboardGridContainer.addView(row3);

            // Row 4: ?123 + , + 😊 + SPACE + . + ENTER
            LinearLayout row4 = new LinearLayout(getContext());
            row4.setOrientation(HORIZONTAL);
            row4.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            row4.addView(createSpecialKey("?123", 1.3f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "?123");
                    currentLayoutPage = 1;
                    renderGrid();
                }
            }));

            row4.addView(createNormalKey(",", 0.9f, colors, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, ",");
                    if (listener != null) listener.onKeyPress(',');
                }
            }));

            row4.addView(createSpecialKey("😊", 1.0f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "😊");
                    toggleEmoji();
                }
            }));

            row4.addView(createNormalKey("spasi", 3.8f, colors, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "␣");
                    if (listener != null) listener.onSpecialPress("SPACE");
                }
            }));

            row4.addView(createNormalKey(".", 0.9f, colors, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, ".");
                    if (listener != null) listener.onKeyPress('.');
                }
            }));

            row4.addView(createSpecialKey("↵", 1.3f, colors.actionKeyBackgroundColor, colors.actionKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "↵");
                    if (listener != null) listener.onSpecialPress("ENTER");
                }
            }));

            keyboardGridContainer.addView(row4);

        } else if (currentLayoutPage == 1) {
            // Page 1: Symbols 1
            addSimpleKeyRow(SYM_ROW_1[0], colors, keyHeightPx);
            addSimpleKeyRow(SYM_ROW_2[0], colors, keyHeightPx);

            LinearLayout row3 = new LinearLayout(getContext());
            row3.setOrientation(HORIZONTAL);
            row3.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            row3.addView(createSpecialKey("=\\<", 1.5f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "=\\<");
                    currentLayoutPage = 2;
                    renderGrid();
                }
            }));

            for (final String k : SYM_ROW_3) {
                row3.addView(createNormalKey(k, 1.0f, colors, keyHeightPx, new OnClickListener() {
                    @Override public void onClick(View v) {
                        showKeyPopup(v, k);
                        if (listener != null) listener.onKeyPress(k.charAt(0));
                    }
                }));
            }

            row3.addView(createSpecialKey("⌫", 1.5f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "⌫");
                    if (listener != null) listener.onSpecialPress("BACKSPACE");
                }
            }));

            keyboardGridContainer.addView(row3);

            // Row 4
            LinearLayout row4 = new LinearLayout(getContext());
            row4.setOrientation(HORIZONTAL);
            row4.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            row4.addView(createSpecialKey("ABC", 1.3f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "ABC");
                    currentLayoutPage = 0;
                    renderGrid();
                }
            }));

            row4.addView(createSpecialKey("😊", 1.0f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "😊");
                    toggleEmoji();
                }
            }));

            row4.addView(createNormalKey("spasi", 4.5f, colors, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "␣");
                    if (listener != null) listener.onSpecialPress("SPACE");
                }
            }));

            row4.addView(createSpecialKey("↵", 1.4f, colors.actionKeyBackgroundColor, colors.actionKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "↵");
                    if (listener != null) listener.onSpecialPress("ENTER");
                }
            }));

            keyboardGridContainer.addView(row4);

        } else if (currentLayoutPage == 2) {
            // Page 2: Symbols 2
            addSimpleKeyRow(SYM2_ROW_1[0], colors, keyHeightPx);
            addSimpleKeyRow(SYM2_ROW_2[0], colors, keyHeightPx);

            LinearLayout row3 = new LinearLayout(getContext());
            row3.setOrientation(HORIZONTAL);
            row3.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            row3.addView(createSpecialKey("?123", 1.5f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "?123");
                    currentLayoutPage = 1;
                    renderGrid();
                }
            }));

            for (final String k : SYM2_ROW_3) {
                row3.addView(createNormalKey(k, 1.0f, colors, keyHeightPx, new OnClickListener() {
                    @Override public void onClick(View v) {
                        showKeyPopup(v, k);
                        if (listener != null) listener.onKeyPress(k.charAt(0));
                    }
                }));
            }

            row3.addView(createSpecialKey("⌫", 1.5f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "⌫");
                    if (listener != null) listener.onSpecialPress("BACKSPACE");
                }
            }));

            keyboardGridContainer.addView(row3);

            // Row 4
            LinearLayout row4 = new LinearLayout(getContext());
            row4.setOrientation(HORIZONTAL);
            row4.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            row4.addView(createSpecialKey("ABC", 1.3f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "ABC");
                    currentLayoutPage = 0;
                    renderGrid();
                }
            }));

            row4.addView(createSpecialKey("😊", 1.0f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "😊");
                    toggleEmoji();
                }
            }));

            row4.addView(createNormalKey("spasi", 4.5f, colors, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "␣");
                    if (listener != null) listener.onSpecialPress("SPACE");
                }
            }));

            row4.addView(createSpecialKey("↵", 1.4f, colors.actionKeyBackgroundColor, colors.actionKeyTextColor, keyHeightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, "↵");
                    if (listener != null) listener.onSpecialPress("ENTER");
                }
            }));

            keyboardGridContainer.addView(row4);
        } else if (currentLayoutPage == 3) {
            // Page 3: Editor Layout
            String[][] editorRows = {
                {"✂", "📋", "▲", "📄", "⬇"},
                {"➦", "◀", "⛶", "▶", "↩"},
                {"⇡", "⇤", "▼", "⇥", "⌫"},
                {"TAB", "⏫", "␣", "⏬", "↵"}
            };
            
            String[][] editorActions = {
                {"CUT", "PASTE", "DPAD_UP", "COPY", "HIDE"},
                {"REDO", "DPAD_LEFT", "SELECT_ALL", "DPAD_RIGHT", "UNDO"},
                {"TOP", "MOVE_HOME", "DPAD_DOWN", "MOVE_END", "BACKSPACE"},
                {"TAB", "PAGE_UP", "SPACE", "PAGE_DOWN", "ENTER"}
            };

            for (int r = 0; r < editorRows.length; r++) {
                LinearLayout editRow = new LinearLayout(getContext());
                editRow.setOrientation(HORIZONTAL);
                editRow.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                for (int c = 0; c < editorRows[r].length; c++) {
                    final String label = editorRows[r][c];
                    final String action = editorActions[r][c];
                    editRow.addView(createSpecialKey(label, 1.0f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
                        @Override public void onClick(View v) {
                            showKeyPopup(v, label);
                            if (listener != null) listener.onSpecialPress(action);
                        }
                    }));
                }
                keyboardGridContainer.addView(editRow);
            }
        }
    }

    private void addSimpleKeyRow(String[] keys, KeyboardTheme.ColorPalette colors, int heightPx) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (final String k : keys) {
            row.addView(createNormalKey(k, 1.0f, colors, heightPx, new OnClickListener() {
                @Override public void onClick(View v) {
                    showKeyPopup(v, k);
                    if (listener != null) listener.onKeyPress(k.charAt(0));
                }
            }));
        }
        keyboardGridContainer.addView(row);
    }

    private android.graphics.drawable.Drawable getNormalKeyBg(KeyboardTheme.ColorPalette colors) {
        if (normalKeyBgCache == null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(colors.keyBackgroundColor);
            bg.setCornerRadius(dpToPx(shapeStyle.getCornerRadiusDp()));
            normalKeyBgCache = bg.getConstantState();
        }
        return normalKeyBgCache.newDrawable();
    }

    private View createNormalKey(final String label, float weight, KeyboardTheme.ColorPalette colors, int heightPx, OnClickListener clickListener) {
        final String subSymbol = (currentLayoutPage == 0) ? getLongPressSymbol(label) : null;

        FrameLayout container = new FrameLayout(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, heightPx, weight);
        lp.setMargins(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(3));
        container.setLayoutParams(lp);

        container.setBackground(getNormalKeyBg(colors));

        // Primary Label
        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setTextColor(colors.keyTextColor);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(tv, tvLp);

        // Sub-symbol hint (if available)
        if (subSymbol != null) {
            TextView subTv = new TextView(getContext());
            subTv.setText(subSymbol);
            subTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            subTv.setTextColor(Color.parseColor("#888888"));
            subTv.setTypeface(Typeface.DEFAULT_BOLD);
            FrameLayout.LayoutParams subLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subLp.gravity = Gravity.TOP | Gravity.END;
            subLp.setMargins(0, dpToPx(2), dpToPx(4), 0);
            container.addView(subTv, subLp);
        }

        container.setOnClickListener(clickListener);

        // Long Press Handler for Symbol
        container.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (subSymbol != null) {
                    showKeyPopup(v, subSymbol);
                    if (listener != null) {
                        listener.onKeyPress(subSymbol.charAt(0));
                    }
                    return true;
                }
                return false;
            }
        });

        return container;
    }

    private final Handler repeatHandler = new Handler(Looper.getMainLooper());
    private Runnable repeatRunnable;

    private android.graphics.drawable.Drawable getSpecialKeyBg(int bgColor, KeyboardTheme.ColorPalette colors) {
        if (bgColor == colors.specialKeyBackgroundColor) {
            if (specialKeyBgCache == null) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(bgColor);
                bg.setCornerRadius(dpToPx(shapeStyle.getCornerRadiusDp()));
                specialKeyBgCache = bg.getConstantState();
            }
            return specialKeyBgCache.newDrawable();
        } else if (bgColor == colors.actionKeyBackgroundColor) {
            if (actionKeyBgCache == null) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(bgColor);
                bg.setCornerRadius(dpToPx(shapeStyle.getCornerRadiusDp()));
                actionKeyBgCache = bg.getConstantState();
            }
            return actionKeyBgCache.newDrawable();
        } else {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(bgColor);
            bg.setCornerRadius(dpToPx(shapeStyle.getCornerRadiusDp()));
            return bg;
        }
    }

    private View createSpecialKey(final String label, float weight, int bgColor, int textColor, int heightPx, final OnClickListener clickListener) {
        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTextColor(textColor);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);

        tv.setBackground(getSpecialKeyBg(bgColor, KeyboardTheme.getColors(activeTheme)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, heightPx, weight);
        lp.setMargins(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(3));
        tv.setLayoutParams(lp);

        if ("⌫".equals(label)) {
            tv.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            clickListener.onClick(v);
                            repeatRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    if (listener != null) listener.onSpecialPress("BACKSPACE");
                                    repeatHandler.postDelayed(this, 50);
                                }
                            };
                            repeatHandler.postDelayed(repeatRunnable, 400);
                            v.setPressed(true);
                            return true;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (repeatRunnable != null) {
                                repeatHandler.removeCallbacks(repeatRunnable);
                                repeatRunnable = null;
                            }
                            v.setPressed(false);
                            return true;
                    }
                    return false;
                }
            });
        } else {
            tv.setOnClickListener(clickListener);
        }

        return tv;
    }

    private static String getLongPressSymbol(String key) {
        if (key == null || key.isEmpty()) return null;
        String k = key.toLowerCase();
        switch (k) {
            case "q": return "1";
            case "w": return "2";
            case "e": return "3";
            case "r": return "4";
            case "t": return "5";
            case "y": return "6";
            case "u": return "7";
            case "i": return "8";
            case "o": return "9";
            case "p": return "0";
            case "a": return "@";
            case "s": return "#";
            case "d": return "$";
            case "f": return "%";
            case "g": return "&";
            case "h": return "-";
            case "j": return "+";
            case "k": return "(";
            case "l": return ")";
            case "z": return "*";
            case "x": return "\"";
            case "c": return "'";
            case "v": return ":";
            case "b": return ";";
            case "n": return "!";
            case "m": return "?";
            default: return null;
        }
    }

    private void showKeyPopup(View anchorView, String text) {
        if (text == null || text.trim().isEmpty() || text.length() > 20) return;
        try {
            if (keyPopup == null) {
                popupTextView = new TextView(getContext());
                popupTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                popupTextView.setTypeface(Typeface.DEFAULT_BOLD);
                popupTextView.setTextColor(Color.WHITE);
                popupTextView.setGravity(Gravity.CENTER);
                popupTextView.setPadding(dpToPx(14), dpToPx(6), dpToPx(14), dpToPx(6));

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#E0212121"));
                bg.setCornerRadius(dpToPx(8));
                popupTextView.setBackground(bg);

                keyPopup = new PopupWindow(popupTextView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                keyPopup.setOutsideTouchable(true);
                keyPopup.setFocusable(false);
            }

            popupTextView.setText(text);

            // Measure the popup dimensions dynamically
            popupTextView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                  View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int popupWidth = popupTextView.getMeasuredWidth();
            int popupHeight = popupTextView.getMeasuredHeight();

            // Center horizontally and place cleanly ABOVE the key
            int xOffset = (anchorView.getWidth() - popupWidth) / 2;
            int yOffset = -anchorView.getHeight() - popupHeight - dpToPx(12);

            if (keyPopup.isShowing()) {
                keyPopup.update(anchorView, xOffset, yOffset, popupWidth, popupHeight);
            } else {
                keyPopup.setWidth(popupWidth);
                keyPopup.setHeight(popupHeight);
                keyPopup.showAsDropDown(anchorView, xOffset, yOffset);
            }

            popupHandler.removeCallbacks(hidePopupRunnable);
            popupHandler.postDelayed(hidePopupRunnable, 300);
        } catch (Exception ignored) {}
    }

    private void toggleClipboard() {
        isClipboardVisible = !isClipboardVisible;
        if (isClipboardVisible) {
            isEmojiVisible = false;
            keyboardGridContainer.setVisibility(GONE);
            emojiContainer.setVisibility(GONE);
            clipboardContainer.setVisibility(VISIBLE);
            updateClipboardContainer();
        } else {
            clipboardContainer.setVisibility(GONE);
            keyboardGridContainer.setVisibility(VISIBLE);
        }
    }

    private void toggleEmoji() {
        isEmojiVisible = !isEmojiVisible;
        if (isEmojiVisible) {
            isClipboardVisible = false;
            keyboardGridContainer.setVisibility(GONE);
            clipboardContainer.setVisibility(GONE);
            emojiContainer.setVisibility(VISIBLE);
            updateEmojiContainer();
        } else {
            emojiContainer.setVisibility(GONE);
            keyboardGridContainer.setVisibility(VISIBLE);
        }
    }

    private void updateEmojiContainer() {
        if (emojiContainer == null) return;
        emojiContainer.removeAllViews();

        KeyboardTheme.ColorPalette colors = KeyboardTheme.getColors(activeTheme);
        emojiContainer.setBackgroundColor(colors.backgroundColor);

        // Header / Category Tabs
        HorizontalScrollView catScroll = new HorizontalScrollView(getContext());
        catScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout catLayout = new LinearLayout(getContext());
        catLayout.setOrientation(HORIZONTAL);
        catLayout.setGravity(Gravity.CENTER_VERTICAL);
        catLayout.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        for (int i = 0; i < EMOJI_CATEGORIES.length; i++) {
            final int catIndex = i;
            TextView catTab = new TextView(getContext());
            catTab.setText(EMOJI_CATEGORIES[i]);
            catTab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            catTab.setTypeface(Typeface.DEFAULT_BOLD);
            catTab.setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5));
            catTab.setGravity(Gravity.CENTER);

            GradientDrawable tabBg = new GradientDrawable();
            if (catIndex == selectedEmojiCategory) {
                tabBg.setColor(colors.actionKeyBackgroundColor);
                catTab.setTextColor(colors.actionKeyTextColor);
            } else {
                tabBg.setColor(colors.keyBackgroundColor);
                catTab.setTextColor(colors.keyTextColor);
            }
            tabBg.setCornerRadius(dpToPx(shapeStyle.getCornerRadiusDp()));
            catTab.setBackground(tabBg);

            LinearLayout.LayoutParams catLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            catLp.setMargins(dpToPx(3), 0, dpToPx(3), 0);
            catTab.setLayoutParams(catLp);

            catTab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedEmojiCategory = catIndex;
                    updateEmojiContainer();
                }
            });

            catLayout.addView(catTab);
        }

        catScroll.addView(catLayout);
        emojiContainer.addView(catScroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Scrollable Grid of Emojis
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setVerticalScrollBarEnabled(true);

        int keyHeightPx = dpToPx(customKeyHeightDp > 0 ? customKeyHeightDp : heightStyle.getKeyHeightDp());
        int contentHeightPx = (3 * keyHeightPx) + dpToPx(16);
        if (contentHeightPx < dpToPx(130)) {
            contentHeightPx = dpToPx(160);
        }

        LinearLayout gridLayout = new LinearLayout(getContext());
        gridLayout.setOrientation(VERTICAL);
        gridLayout.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        String[] currentEmojis = (selectedEmojiCategory >= 0 && selectedEmojiCategory < EMOJI_DATA.length) 
                ? EMOJI_DATA[selectedEmojiCategory] : EMOJI_DATA[0];

        int cols = 7;
        LinearLayout currentRow = null;
        for (int i = 0; i < currentEmojis.length; i++) {
            if (i % cols == 0) {
                currentRow = new LinearLayout(getContext());
                currentRow.setOrientation(HORIZONTAL);
                currentRow.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                gridLayout.addView(currentRow);
            }

            final String emoji = currentEmojis[i];
            TextView emojiTv = new TextView(getContext());
            emojiTv.setText(emoji);
            emojiTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            emojiTv.setGravity(Gravity.CENTER);
            emojiTv.setPadding(0, dpToPx(6), 0, dpToPx(6));

            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            itemLp.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
            emojiTv.setLayoutParams(itemLp);

            emojiTv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showKeyPopup(v, emoji);
                    if (listener != null) {
                        listener.onEmojiClick(emoji);
                    }
                }
            });

            if (currentRow != null) {
                currentRow.addView(emojiTv);
            }
        }

        // Fill remaining spaces in last row if not full
        if (currentRow != null && (currentEmojis.length % cols) != 0) {
            int remaining = cols - (currentEmojis.length % cols);
            for (int r = 0; r < remaining; r++) {
                View placeholder = new View(getContext());
                LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                itemLp.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
                placeholder.setLayoutParams(itemLp);
                currentRow.addView(placeholder);
            }
        }

        scrollView.addView(gridLayout);
        emojiContainer.addView(scrollView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, contentHeightPx));

        // Bottom action bar inside Emoji Picker (ABC, Space, Backspace, Enter)
        LinearLayout bottomRow = new LinearLayout(getContext());
        bottomRow.setOrientation(HORIZONTAL);
        bottomRow.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        bottomRow.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        bottomRow.addView(createSpecialKey("ABC", 1.5f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleEmoji();
            }
        }));

        bottomRow.addView(createNormalKey("spasi", 4.0f, colors, keyHeightPx, new OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyPopup(v, "␣");
                if (listener != null) listener.onSpecialPress("SPACE");
            }
        }));

        bottomRow.addView(createSpecialKey("⌫", 1.5f, colors.specialKeyBackgroundColor, colors.specialKeyTextColor, keyHeightPx, new OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyPopup(v, "⌫");
                if (listener != null) listener.onSpecialPress("BACKSPACE");
            }
        }));

        bottomRow.addView(createSpecialKey("↵", 1.5f, colors.actionKeyBackgroundColor, colors.actionKeyTextColor, keyHeightPx, new OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyPopup(v, "↵");
                if (listener != null) listener.onSpecialPress("ENTER");
            }
        }));

        emojiContainer.addView(bottomRow);
    }

    private void updateClipboardContainer() {
        if (clipboardContainer == null) return;
        clipboardContainer.removeAllViews();

        KeyboardTheme.ColorPalette colors = KeyboardTheme.getColors(activeTheme);
        clipboardContainer.setBackgroundColor(colors.backgroundColor);

        // Header Layout
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));

        TextView titleTv = new TextView(getContext());
        titleTv.setText("📋 Riwayat Papan Klip");
        titleTv.setTextColor(colors.suggestionTextColor);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        header.addView(titleTv, titleLp);

        // Hapus Semua button
        if (clipboardClips != null && !clipboardClips.isEmpty()) {
            TextView clearTv = new TextView(getContext());
            clearTv.setText("❌ Hapus Semua");
            clearTv.setTextColor(Color.parseColor("#D32F2F"));
            clearTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            clearTv.setTypeface(Typeface.DEFAULT_BOLD);
            clearTv.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            clearTv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onClearClipboard();
                    toggleClipboard(); // Tutup setelah hapus semua
                }
            });
            header.addView(clearTv);
        }

        // Close/Cancel button
        TextView closeTv = new TextView(getContext());
        closeTv.setText("✕ Tutup");
        closeTv.setTextColor(colors.suggestionTextColor);
        closeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        closeTv.setTypeface(Typeface.DEFAULT_BOLD);
        closeTv.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));
        closeTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleClipboard();
            }
        });
        header.addView(closeTv);

        clipboardContainer.addView(header);

        // Scrollable content
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setVerticalScrollBarEnabled(true);

        // Calculate dynamic height matching QWERTY rows to avoid resize jitter
        int keyHeightPx = dpToPx(heightStyle.getKeyHeightDp());
        int contentHeightPx = (4 * keyHeightPx) + dpToPx(24) - dpToPx(40); // 4 rows + spacing - header height
        if (contentHeightPx < dpToPx(120)) {
            contentHeightPx = dpToPx(160);
        }

        LinearLayout itemsLayout = new LinearLayout(getContext());
        itemsLayout.setOrientation(VERTICAL);
        itemsLayout.setPadding(dpToPx(12), dpToPx(2), dpToPx(12), dpToPx(12));

        if (clipboardClips == null || clipboardClips.isEmpty()) {
            TextView emptyTv = new TextView(getContext());
            emptyTv.setText("Papan klip kosong. Teks yang disalin akan muncul di sini.");
            emptyTv.setTextColor(colors.specialKeyTextColor);
            emptyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            emptyTv.setGravity(Gravity.CENTER);
            emptyTv.setPadding(0, dpToPx(32), 0, dpToPx(32));
            itemsLayout.addView(emptyTv);
        } else {
            for (final String clipText : clipboardClips) {
                LinearLayout clipRow = new LinearLayout(getContext());
                clipRow.setOrientation(HORIZONTAL);
                clipRow.setGravity(Gravity.CENTER_VERTICAL);
                clipRow.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
                
                GradientDrawable itemBg = new GradientDrawable();
                itemBg.setColor(colors.keyBackgroundColor);
                itemBg.setCornerRadius(dpToPx(shapeStyle.getCornerRadiusDp()));
                clipRow.setBackground(itemBg);

                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowLp.setMargins(0, dpToPx(4), 0, dpToPx(4));
                clipRow.setLayoutParams(rowLp);

                TextView textTv = new TextView(getContext());
                String display = clipText;
                if (display.length() > 120) {
                    display = display.substring(0, 117) + "...";
                }
                textTv.setText(display);
                textTv.setTextColor(colors.keyTextColor);
                textTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                textTv.setSingleLine(false);
                textTv.setMaxLines(2);
                textTv.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                textTv.setLayoutParams(textLp);

                clipRow.addView(textTv);

                // Tap row to Paste
                clipRow.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showKeyPopup(v, "Menempel...");
                        if (listener != null) listener.onPasteClip(clipText);
                        toggleClipboard(); // Tutup setelah menempel
                    }
                });

                itemsLayout.addView(clipRow);
            }
        }

        scrollView.addView(itemsLayout);
        clipboardContainer.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, contentHeightPx));
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}

