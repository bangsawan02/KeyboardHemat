package com.example.ui.theme;

import android.graphics.Color;

public class KeyboardTheme {

    public enum ThemeStyle {
        DARK("Dark Canvas (Default)"),
        LIGHT("Light Pure"),
        OLED("Pitch Black OLED"),
        CYBERPUNK("Cyberpunk Neon"),
        FOREST("Forest Emerald"),
        SUNSET("Sunset Orange"),
        SLATE("Slate Modern");

        private final String displayName;

        ThemeStyle(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum HeightStyle {
        COMPACT("Ringkas", 44),
        NORMAL("Normal", 52),
        TALL("Tinggi", 60),
        EXTRA_TALL("Sangat Tinggi", 68);

        private final String displayName;
        private final int keyHeightDp;

        HeightStyle(String displayName, int keyHeightDp) {
            this.displayName = displayName;
            this.keyHeightDp = keyHeightDp;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getKeyHeightDp() {
            return keyHeightDp;
        }
    }

    public enum KeyShapeStyle {
        SQUARE("Persegi (2dp)", 2),
        ROUNDED("Bulat (8dp)", 8),
        EXTRA_ROUNDED("Sangat Bulat (14dp)", 14),
        PILL("Pill (20dp)", 20);

        private final String displayName;
        private final int cornerRadiusDp;

        KeyShapeStyle(String displayName, int cornerRadiusDp) {
            this.displayName = displayName;
            this.cornerRadiusDp = cornerRadiusDp;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getCornerRadiusDp() {
            return cornerRadiusDp;
        }
    }

    public static class ColorPalette {
        public int backgroundColor;
        public int keyBackgroundColor;
        public int keyTextColor;
        public int specialKeyBackgroundColor;
        public int specialKeyTextColor;
        public int actionKeyBackgroundColor;
        public int actionKeyTextColor;
        public int suggestionBackgroundColor;
        public int suggestionTextColor;
        public int toolbarBackgroundColor;
        public int toolbarTextColor;

        public ColorPalette(int bg, int keyBg, int keyText, int specialBg, int specialText,
                            int actionBg, int actionText, int sugBg, int sugText, int toolBg, int toolText) {
            this.backgroundColor = bg;
            this.keyBackgroundColor = keyBg;
            this.keyTextColor = keyText;
            this.specialKeyBackgroundColor = specialBg;
            this.specialKeyTextColor = specialText;
            this.actionKeyBackgroundColor = actionBg;
            this.actionKeyTextColor = actionText;
            this.suggestionBackgroundColor = sugBg;
            this.suggestionTextColor = sugText;
            this.toolbarBackgroundColor = toolBg;
            this.toolbarTextColor = toolText;
        }
    }

    public static ColorPalette getColors(ThemeStyle style) {
        if (style == null) style = ThemeStyle.DARK;
        switch (style) {
            case LIGHT:
                return new ColorPalette(
                        Color.parseColor("#E0E0E0"), Color.parseColor("#FFFFFF"), Color.parseColor("#212121"),
                        Color.parseColor("#D6D6D6"), Color.parseColor("#212121"), Color.parseColor("#1976D2"),
                        Color.parseColor("#FFFFFF"), Color.parseColor("#EEEEEE"), Color.parseColor("#1565C0"),
                        Color.parseColor("#E0E0E0"), Color.parseColor("#0D47A1")
                );
            case OLED:
                return new ColorPalette(
                        Color.parseColor("#000000"), Color.parseColor("#121212"), Color.parseColor("#FFFFFF"),
                        Color.parseColor("#1E1E1E"), Color.parseColor("#E0E0E0"), Color.parseColor("#6200EE"),
                        Color.parseColor("#FFFFFF"), Color.parseColor("#000000"), Color.parseColor("#BB86FC"),
                        Color.parseColor("#0A0A0A"), Color.parseColor("#BB86FC")
                );
            case CYBERPUNK:
                return new ColorPalette(
                        Color.parseColor("#0D0221"), Color.parseColor("#05D5FA"), Color.parseColor("#000000"),
                        Color.parseColor("#FF007F"), Color.parseColor("#FFFFFF"), Color.parseColor("#FFE600"),
                        Color.parseColor("#000000"), Color.parseColor("#150538"), Color.parseColor("#05D5FA"),
                        Color.parseColor("#0D0221"), Color.parseColor("#FF007F")
                );
            case FOREST:
                return new ColorPalette(
                        Color.parseColor("#1B2E22"), Color.parseColor("#2A4735"), Color.parseColor("#E8F5E9"),
                        Color.parseColor("#1E3827"), Color.parseColor("#A5D6A7"), Color.parseColor("#2E7D32"),
                        Color.parseColor("#FFFFFF"), Color.parseColor("#14241B"), Color.parseColor("#81C784"),
                        Color.parseColor("#1B2E22"), Color.parseColor("#81C784")
                );
            case SUNSET:
                return new ColorPalette(
                        Color.parseColor("#2D1B2D"), Color.parseColor("#4A2545"), Color.parseColor("#FFF3E0"),
                        Color.parseColor("#3A1E37"), Color.parseColor("#FFB74D"), Color.parseColor("#E65100"),
                        Color.parseColor("#FFFFFF"), Color.parseColor("#211321"), Color.parseColor("#FF8A65"),
                        Color.parseColor("#2D1B2D"), Color.parseColor("#FF8A65")
                );
            case SLATE:
                return new ColorPalette(
                        Color.parseColor("#1E293B"), Color.parseColor("#334155"), Color.parseColor("#F8FAFC"),
                        Color.parseColor("#2A3749"), Color.parseColor("#94A3B8"), Color.parseColor("#0EA5E9"),
                        Color.parseColor("#FFFFFF"), Color.parseColor("#0F172A"), Color.parseColor("#38BDF8"),
                        Color.parseColor("#1E293B"), Color.parseColor("#38BDF8")
                );
            case DARK:
            default:
                return new ColorPalette(
                        Color.parseColor("#1C1B1F"), Color.parseColor("#2B2930"), Color.parseColor("#E6E1E5"),
                        Color.parseColor("#36343B"), Color.parseColor("#CAC4D0"), Color.parseColor("#D0BCFF"),
                        Color.parseColor("#381E72"), Color.parseColor("#2B2930"), Color.parseColor("#D0BCFF"),
                        Color.parseColor("#1C1B1F"), Color.parseColor("#E6E1E5")
                );
        }
    }
}
