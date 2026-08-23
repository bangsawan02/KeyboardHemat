package com.example;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.database.DatabaseHelper;

public class SettingsActivity extends Activity {

    private DatabaseHelper dbHelper;
    private Switch predictionSwitch;
    private Switch autoCorrectionSwitch;
    private Switch hapticSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = DatabaseHelper.getInstance(this);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.parseColor("#F8F9FA"));

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(24));

        // 1. Navigation Header
        LinearLayout navHeader = new LinearLayout(this);
        navHeader.setOrientation(LinearLayout.HORIZONTAL);
        navHeader.setGravity(Gravity.CENTER_VERTICAL);
        navHeader.setPadding(0, dpToPx(8), 0, dpToPx(16));

        Button backBtn = new Button(this);
        backBtn.setText("← Kembali");
        backBtn.setAllCaps(false);
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        backBtn.setTypeface(Typeface.DEFAULT_BOLD);
        backBtn.setTextColor(Color.parseColor("#1A237E"));
        GradientDrawable backBg = new GradientDrawable();
        backBg.setColor(Color.parseColor("#E8EAF6"));
        backBg.setCornerRadius(dpToPx(8));
        backBtn.setBackground(backBg);
        backBtn.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        navHeader.addView(backBtn);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("Pengaturan Fitur");
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        headerTitle.setTypeface(Typeface.DEFAULT_BOLD);
        headerTitle.setTextColor(Color.parseColor("#1A237E"));
        headerTitle.setPadding(dpToPx(12), 0, 0, 0);
        navHeader.addView(headerTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        rootLayout.addView(navHeader);

        // Subtitle Description
        TextView subDesc = new TextView(this);
        subDesc.setText("Sesuaikan fitur cerdas pengetikan keyboard sesuai kenyamanan Anda.");
        subDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subDesc.setTextColor(Color.parseColor("#5C6BC0"));
        subDesc.setPadding(0, 0, 0, dpToPx(16));
        rootLayout.addView(subDesc);

        // 2. Settings Card Container
        LinearLayout settingsCard = createCardContainer();

        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("⚙️ Fitur Pengetikan & Prediksi");
        sectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        sectionTitle.setTypeface(Typeface.DEFAULT_BOLD);
        sectionTitle.setTextColor(Color.parseColor("#1A237E"));
        sectionTitle.setPadding(0, 0, 0, dpToPx(12));
        settingsCard.addView(sectionTitle);

        // Toggle 1: Text Prediction (Prediksi Teks)
        boolean isPredictionOn = "1".equals(dbHelper.getSetting("prediction", "1"));
        predictionSwitch = createSettingItem(
                settingsCard,
                "Prediksi Teks (Text Prediction)",
                "Menampilkan bilah saran dan rekomendasi kata cerdas saat mengetik.",
                isPredictionOn,
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        dbHelper.setSetting("prediction", isChecked ? "1" : "0");
                        showFeedbackToast("Prediksi Teks " + (isChecked ? "Diaktifkan" : "Dinonaktifkan"));
                    }
                }
        );

        settingsCard.addView(createDivider());

        // Toggle 2: Auto-Correction (Koreksi Otomatis)
        boolean isAutoCorrectionOn = "1".equals(dbHelper.getSetting("autocorrect", "1"));
        autoCorrectionSwitch = createSettingItem(
                settingsCard,
                "Koreksi Otomatis (Auto-Correction)",
                "Otomatis mengoreksi singkatan dan memilih saran terbaik saat menekan tombol spasi.",
                isAutoCorrectionOn,
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        dbHelper.setSetting("autocorrect", isChecked ? "1" : "0");
                        showFeedbackToast("Koreksi Otomatis " + (isChecked ? "Diaktifkan" : "Dinonaktifkan"));
                    }
                }
        );

        settingsCard.addView(createDivider());

        // Toggle 3: Haptic Feedback (Getaran)
        boolean isHapticOn = "1".equals(dbHelper.getSetting("haptic", "1"));
        hapticSwitch = createSettingItem(
                settingsCard,
                "Getaran Sentuhan (Haptic Feedback)",
                "Memberikan getaran responsif setiap kali tombol keyboard ditekan.",
                isHapticOn,
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        dbHelper.setSetting("haptic", isChecked ? "1" : "0");
                        showFeedbackToast("Getaran " + (isChecked ? "Diaktifkan" : "Dinonaktifkan"));
                    }
                }
        );

        settingsCard.addView(createDivider());

        // Haptic Intensity Selector
        String currentHapticIntensity = dbHelper.getSetting("haptic_intensity", "MEDIUM");
        settingsCard.addView(createRadioSelector(
                "Intensitas Getaran",
                new String[]{"Ringan", "Sedang", "Kuat"},
                new String[]{"LIGHT", "MEDIUM", "STRONG"},
                currentHapticIntensity,
                new OnSelectListener() {
                    @Override
                    public void onSelected(String val) {
                        dbHelper.setSetting("haptic_intensity", val);
                        showFeedbackToast("Intensitas getaran diperbarui");
                    }
                }
        ));

        settingsCard.addView(createDivider());

        // One-Handed Mode Selector
        String currentOneHanded = dbHelper.getSetting("one_handed", "OFF");
        settingsCard.addView(createRadioSelector(
                "Mode Satu Tangan (One-Handed Mode)",
                new String[]{"Mati", "Kiri", "Kanan"},
                new String[]{"OFF", "LEFT", "RIGHT"},
                currentOneHanded,
                new OnSelectListener() {
                    @Override
                    public void onSelected(String val) {
                        dbHelper.setSetting("one_handed", val);
                        showFeedbackToast("Mode satu tangan diperbarui");
                    }
                }
        ));

        rootLayout.addView(settingsCard);

        // 3. Live Test Area Card
        LinearLayout testCard = createCardContainer();

        TextView testTitle = new TextView(this);
        testTitle.setText("📝 Uji Coba Pengetikan");
        testTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        testTitle.setTypeface(Typeface.DEFAULT_BOLD);
        testTitle.setTextColor(Color.parseColor("#1A237E"));
        testTitle.setPadding(0, 0, 0, dpToPx(6));
        testCard.addView(testTitle);

        TextView testSubtitle = new TextView(this);
        testSubtitle.setText("Ketik di kolom bawah ini untuk mencoba efek pengaturan secara langsung:");
        testSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        testSubtitle.setTextColor(Color.parseColor("#666666"));
        testSubtitle.setPadding(0, 0, 0, dpToPx(10));
        testCard.addView(testSubtitle);

        EditText testInput = new EditText(this);
        testInput.setHint("Ketik di sini untuk mengetes keyboard...");
        testInput.setHintTextColor(Color.parseColor("#9E9E9E"));
        testInput.setTextColor(Color.parseColor("#212121"));
        testInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        testInput.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#F5F5F5"));
        inputBg.setCornerRadius(dpToPx(8));
        inputBg.setStroke(dpToPx(1), Color.parseColor("#E0E0E0"));
        testInput.setBackground(inputBg);
        testCard.addView(testInput);

        rootLayout.addView(testCard);

        scrollView.addView(rootLayout);
        setContentView(scrollView);
    }

    private Switch createSettingItem(
            LinearLayout parent,
            String title,
            String description,
            boolean initialChecked,
            CompoundButton.OnCheckedChangeListener listener
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(10), 0, dpToPx(10));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setTextColor(Color.parseColor("#212121"));
        textCol.addView(titleTv);

        TextView descTv = new TextView(this);
        descTv.setText(description);
        descTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        descTv.setTextColor(Color.parseColor("#757575"));
        descTv.setPadding(0, dpToPx(2), 0, 0);
        textCol.addView(descTv);

        row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        final Switch sw = new Switch(this);
        sw.setChecked(initialChecked);
        sw.setOnCheckedChangeListener(listener);
        row.addView(sw);

        // Clicking the whole row toggles the switch
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sw.setChecked(!sw.isChecked());
            }
        });

        parent.addView(row);
        return sw;
    }

    private View createDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
        lp.setMargins(0, dpToPx(4), 0, dpToPx(4));
        divider.setLayoutParams(lp);
        return divider;
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

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(16));
        layout.setLayoutParams(lp);

        return layout;
    }

    private interface OnSelectListener {
        void onSelected(String value);
    }

    private LinearLayout createRadioSelector(String title, final String[] labels, final String[] values, String currentVal, final OnSelectListener listener) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dpToPx(8), 0, dpToPx(8));

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setTextColor(Color.parseColor("#212121"));
        layout.addView(titleTv);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dpToPx(6), 0, 0);

        final android.widget.RadioButton[] rbs = new android.widget.RadioButton[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final String val = values[i];
            final android.widget.RadioButton rb = new android.widget.RadioButton(this);
            rb.setText(labels[i]);
            rb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            rb.setTextColor(Color.parseColor("#424242"));
            rb.setChecked(val.equals(currentVal));
            rbs[i] = rb;

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            rb.setLayoutParams(lp);

            final int index = i;
            rb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int j = 0; j < rbs.length; j++) {
                        rbs[j].setChecked(j == index);
                    }
                    if (listener != null) {
                        listener.onSelected(val);
                    }
                }
            });

            row.addView(rb);
        }

        layout.addView(row);
        return layout;
    }

    private void showFeedbackToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
