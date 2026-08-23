package com.example.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "keyboard_hemat.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_WORDS = "words";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_WORD = "word";
    public static final String COLUMN_FREQ = "frequency";
    public static final String COLUMN_IS_CUSTOM = "is_custom";

    public static final String TABLE_AUTOTEXT = "autotext";
    public static final String COLUMN_SHORTCUT = "shortcut";
    public static final String COLUMN_REPLACEMENT = "replacement";

    public static final String TABLE_SETTINGS = "settings";
    public static final String COLUMN_SETTING_KEY = "key";
    public static final String COLUMN_SETTING_VAL = "value";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
        insertInitialWords(db);
        insertInitialAutoText(db);
    }

    private void createTables(SQLiteDatabase db) {
        String createWordsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_WORDS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_WORD + " TEXT UNIQUE NOT NULL, " +
                COLUMN_FREQ + " INTEGER DEFAULT 1, " +
                COLUMN_IS_CUSTOM + " INTEGER DEFAULT 0);";

        String createAutoTextTable = "CREATE TABLE IF NOT EXISTS " + TABLE_AUTOTEXT + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SHORTCUT + " TEXT NOT NULL, " +
                COLUMN_REPLACEMENT + " TEXT NOT NULL, " +
                COLUMN_FREQ + " INTEGER DEFAULT 1, " +
                COLUMN_IS_CUSTOM + " INTEGER DEFAULT 0);";

        String createSettingsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + " (" +
                COLUMN_SETTING_KEY + " TEXT PRIMARY KEY, " +
                COLUMN_SETTING_VAL + " TEXT);";

        db.execSQL(createWordsTable);
        db.execSQL(createAutoTextTable);
        db.execSQL(createSettingsTable);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        createTables(db);
        ensureInitialDataIfEmpty(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createTables(db);
        ensureInitialDataIfEmpty(db);
    }

    private void ensureInitialDataIfEmpty(SQLiteDatabase db) {
        try {
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_AUTOTEXT, null);
            boolean empty = true;
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    empty = cursor.getInt(0) == 0;
                }
                cursor.close();
            }
            if (empty) {
                insertInitialAutoText(db);
            }
        } catch (Exception ignored) {}
    }

    private void insertInitialAutoText(SQLiteDatabase db) {
        String[][] defaults = {
            {"ccc", "(*'▽')ﾉ ^--=Ξ☆Congratulation!!"},
            {"gg", "http://www.google.com"},
            {"gith", "build rilis, push ke github beserta file apk nya"},
            {"hh", "☆H Ë L L O☆"},
            {"ii", "I❤️U"},
            {"ii", "I❤️U2"},
            {"kk", "ヽ(❤️´3｀)ノ/kiss kiss~"},
            {"kk", "..../\\„„„/\\.... ...( =';'= ). ..../*❤️❤️*\\... ..(.|.|..|.|.)."},
            {"mm", "honsohanwriting@gmail.com"},
            {"opt", "cek codebase, beritahu saya apa yang bisa ditambah atau..."},
            {"pp", "(o `-')ノ”Pia!☆(>_<)"},
            {"rr", "<(-'.'->)♪ θ θ relax~"},
            {"ss", "See you later."}
        };

        db.beginTransaction();
        try {
            for (String[] pair : defaults) {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_SHORTCUT, pair[0]);
                cv.put(COLUMN_REPLACEMENT, pair[1]);
                cv.put(COLUMN_FREQ, 1);
                cv.put(COLUMN_IS_CUSTOM, 0);
                db.insert(TABLE_AUTOTEXT, null, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertInitialWords(SQLiteDatabase db) {
        String[] initialWords = {
            "yang", "dan", "di", "ke", "dari", "ini", "itu", "untuk", "dengan", "saya",
            "aku", "kamu", "anda", "mereka", "kita", "kami", "dia", "akan", "bisa", "dapat",
            "harus", "sudah", "belum", "sangat", "paling", "lebih", "tidak", "bukan", "ada", "juga",
            "hanya", "atau", "karena", "jika", "kalau", "maka", "tetapi", "tapi", "namun", "seperti",
            "sebagai", "dalam", "pada", "tentang", "lagi", "mau", "pun", "kalian", "beliau", "mana",
            "apa", "siapa", "mengapa", "kenapa", "bagaimana", "gimana", "dimana", "kemana", "darimana", "kapan",
            "terima", "kasih", "sama", "kembali", "selamat", "pagi", "siang", "sore", "malam",
            "halo", "hai", "tolong", "maaf", "permisi", "iya", "ya", "baik", "siap", "oke",
            "mantap", "semoga", "sukses", "sehat", "pasti", "tentu", "sip", "mohon", "izin",
            "makan", "minum", "tidur", "bangun", "jalan", "lari", "baca", "tulis", "lihat", "dengar",
            "bicara", "ngomong", "ngobrol", "kerja", "belajar", "main", "beli", "jual", "bayar", "buat",
            "bikin", "buka", "tutup", "pikir", "tahu", "kenal", "minta", "beri", "bawa", "kirim",
            "ambil", "taruh", "simpan", "hapus", "ubah", "ganti", "cari", "temu", "tunggu", "masuk",
            "keluar", "duduk", "berdiri", "pindah", "ingat", "lupa", "bantu", "pakai", "gunakan", "pesan",
            "rumah", "kantor", "sekolah", "kampus", "toko", "pasar", "jalan", "kota", "desa", "negara",
            "tempat", "lokasi", "orang", "teman", "kawan", "sahabat", "keluarga", "anak", "orangtua", "bapak",
            "ibu", "kakak", "adik", "suami", "istri", "makanan", "minuman", "kopi", "teh", "air",
            "nasi", "baju", "celana", "sepatu", "tas", "buku", "pena", "kertas", "meja", "kursi",
            "uang", "harga", "biaya", "waktu", "hari", "minggu", "bulan", "tahun", "jam", "menit",
            "detik", "hp", "ponsel", "nomor", "foto", "gambar", "suara", "video", "pesan", "kabar",
            "surat", "email", "dokumen", "laporan", "kegiatan", "acara", "rapat", "tugas", "proyek",
            "bagus", "keren", "cantik", "ganteng", "ramah", "cepat", "lambat", "mudah", "gampang", "susah",
            "sulit", "besar", "kecil", "panjang", "pendek", "tinggi", "rendah", "banyak", "sedikit", "semua",
            "sebagian", "murah", "mahal", "bersih", "kotor", "panas", "dingin", "segar", "enak", "sedap",
            "capek", "lelah", "senang", "bahagia", "sedih", "baru", "lama", "tua", "muda", "penting",
            "perlu", "jelas", "benar", "salah", "nyaman", "aman", "tenang", "ramai", "sepi", "lengkap",
            "fungsi", "program", "kode", "variabel", "sistem", "data", "aplikasi", "keyboard", "layar", "tombol"
        };

        db.beginTransaction();
        try {
            for (int i = 0; i < initialWords.length; i++) {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_WORD, initialWords[i].toLowerCase());
                cv.put(COLUMN_FREQ, 100 - i);
                cv.put(COLUMN_IS_CUSTOM, 0);
                db.insertWithOnConflict(TABLE_WORDS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public synchronized void addOrIncrementWord(String word, boolean isCustom) {
        if (word == null || word.trim().length() < 2) return;
        String cleanWord = word.trim().toLowerCase();
        
        try {
            SQLiteDatabase db = getWritableDatabase();
            createTables(db);
            Cursor cursor = db.query(TABLE_WORDS, new String[]{COLUMN_FREQ, COLUMN_IS_CUSTOM},
                    COLUMN_WORD + "=?", new String[]{cleanWord}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int freq = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FREQ));
                cursor.close();
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_FREQ, freq + 1);
                db.update(TABLE_WORDS, cv, COLUMN_WORD + "=?", new String[]{cleanWord});
            } else {
                if (cursor != null) cursor.close();
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_WORD, cleanWord);
                cv.put(COLUMN_FREQ, isCustom ? 50 : 1);
                cv.put(COLUMN_IS_CUSTOM, isCustom ? 1 : 0);
                db.insertWithOnConflict(TABLE_WORDS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized List<String> getPredictions(String prefix, int limit) {
        List<String> list = new ArrayList<>();
        if (prefix == null || prefix.trim().isEmpty()) {
            list.add("terima");
            list.add("kasih");
            list.add("kembali");
            return list;
        }

        String cleanPrefix = prefix.trim().toLowerCase();
        try {
            SQLiteDatabase db = getReadableDatabase();
            createTables(db);
            Cursor cursor = db.query(TABLE_WORDS, new String[]{COLUMN_WORD},
                    COLUMN_WORD + " LIKE ?", new String[]{cleanPrefix + "%"},
                    null, null, COLUMN_FREQ + " DESC", String.valueOf(limit));

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD)));
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (list.isEmpty()) {
            list.add(cleanPrefix + "an");
            list.add(cleanPrefix);
            list.add(cleanPrefix + "kan");
        }
        return list;
    }

    public static class WordItem {
        public long id;
        public String word;
        public int frequency;
        public boolean isCustom;

        public WordItem(long id, String word, int frequency, boolean isCustom) {
            this.id = id;
            this.word = word;
            this.frequency = frequency;
            this.isCustom = isCustom;
        }
    }

    public synchronized List<WordItem> searchWords(String query, String filterType) {
        List<WordItem> list = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            createTables(db);

            StringBuilder selection = new StringBuilder();
            List<String> selectionArgs = new ArrayList<>();

            if (query != null && !query.trim().isEmpty()) {
                selection.append(COLUMN_WORD).append(" LIKE ?");
                selectionArgs.add("%" + query.trim().toLowerCase() + "%");
            }

            if ("custom".equalsIgnoreCase(filterType)) {
                if (selection.length() > 0) selection.append(" AND ");
                selection.append(COLUMN_IS_CUSTOM).append("=1");
            } else if ("frequent".equalsIgnoreCase(filterType)) {
                if (selection.length() > 0) selection.append(" AND ");
                selection.append(COLUMN_FREQ).append(">10");
            }

            String selStr = selection.length() > 0 ? selection.toString() : null;
            String[] selArgs = selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]);

            Cursor cursor = db.query(TABLE_WORDS, null, selStr, selArgs, null, null, COLUMN_FREQ + " DESC", "100");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String word = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD));
                    int freq = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FREQ));
                    boolean isCustom = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CUSTOM)) == 1;
                    list.add(new WordItem(id, word, freq, isCustom));
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized void deleteWord(String word) {
        if (word == null) return;
        try {
            SQLiteDatabase db = getWritableDatabase();
            createTables(db);
            db.delete(TABLE_WORDS, COLUMN_WORD + "=?", new String[]{word.toLowerCase()});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void deleteAllCustomWords() {
        try {
            SQLiteDatabase db = getWritableDatabase();
            createTables(db);
            db.delete(TABLE_WORDS, COLUMN_IS_CUSTOM + "=1", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void resetDefaultWords() {
        try {
            SQLiteDatabase db = getWritableDatabase();
            createTables(db);
            insertInitialWords(db);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized int getTotalWordCount() {
        int count = 0;
        try {
            SQLiteDatabase db = getReadableDatabase();
            createTables(db);
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORDS, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) count = cursor.getInt(0);
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public synchronized int getCustomWordCount() {
        int count = 0;
        try {
            SQLiteDatabase db = getReadableDatabase();
            createTables(db);
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORDS + " WHERE " + COLUMN_IS_CUSTOM + "=1", null);
            if (cursor != null) {
                if (cursor.moveToFirst()) count = cursor.getInt(0);
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public static class AutoTextItem {
        public long id;
        public String shortcut;
        public String replacement;
        public int frequency;
        public boolean isCustom;

        public AutoTextItem(long id, String shortcut, String replacement, int frequency, boolean isCustom) {
            this.id = id;
            this.shortcut = shortcut;
            this.replacement = replacement;
            this.frequency = frequency;
            this.isCustom = isCustom;
        }
    }

    public synchronized long insertAutoText(String shortcut, String replacement, boolean isCustom) {
        if (shortcut == null || replacement == null) return -1;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SHORTCUT, shortcut.trim());
        cv.put(COLUMN_REPLACEMENT, replacement.trim());
        cv.put(COLUMN_FREQ, 1);
        cv.put(COLUMN_IS_CUSTOM, isCustom ? 1 : 0);
        return db.insert(TABLE_AUTOTEXT, null, cv);
    }

    public synchronized void updateAutoText(long id, String shortcut, String replacement) {
        if (shortcut == null || replacement == null) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SHORTCUT, shortcut.trim());
        cv.put(COLUMN_REPLACEMENT, replacement.trim());
        db.update(TABLE_AUTOTEXT, cv, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    public synchronized void deleteAutoText(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_AUTOTEXT, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    public synchronized void deleteAutoTextByShortcut(String shortcut) {
        if (shortcut == null) return;
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_AUTOTEXT, COLUMN_SHORTCUT + "=?", new String[]{shortcut.trim()});
    }

    public synchronized void deleteAllAutoText() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_AUTOTEXT, null, null);
    }

    public synchronized void resetDefaultAutoText() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_AUTOTEXT, null, null);
        insertInitialAutoText(db);
    }

    public synchronized List<AutoTextItem> getAllAutoText(String query) {
        List<AutoTextItem> list = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            createTables(db);

            String selection = null;
            String[] selectionArgs = null;

            if (query != null && !query.trim().isEmpty()) {
                selection = COLUMN_SHORTCUT + " LIKE ? OR " + COLUMN_REPLACEMENT + " LIKE ?";
                String arg = "%" + query.trim() + "%";
                selectionArgs = new String[]{arg, arg};
            }

            Cursor cursor = db.query(TABLE_AUTOTEXT, null, selection, selectionArgs, null, null, COLUMN_SHORTCUT + " ASC", "300");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String shortcut = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SHORTCUT));
                    String replacement = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REPLACEMENT));
                    int freq = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FREQ));
                    boolean isCustom = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CUSTOM)) == 1;
                    list.add(new AutoTextItem(id, shortcut, replacement, freq, isCustom));
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized List<String> getAutoTextReplacements(String shortcut) {
        List<String> results = new ArrayList<>();
        if (shortcut == null || shortcut.trim().isEmpty()) return results;
        try {
            SQLiteDatabase db = getReadableDatabase();
            createTables(db);
            Cursor cursor = db.query(TABLE_AUTOTEXT, new String[]{COLUMN_REPLACEMENT},
                    COLUMN_SHORTCUT + "=? COLLATE NOCASE", new String[]{shortcut.trim()}, null, null, COLUMN_FREQ + " DESC");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String rep = cursor.getString(0);
                    if (!results.contains(rep)) {
                        results.add(rep);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    public synchronized int getAutoTextCount() {
        int count = 0;
        try {
            SQLiteDatabase db = getReadableDatabase();
            createTables(db);
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_AUTOTEXT, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) count = cursor.getInt(0);
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public synchronized String getSetting(String key, String defaultValue) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{COLUMN_SETTING_VAL},
                COLUMN_SETTING_KEY + "=?", new String[]{key}, null, null, null);
        String val = defaultValue;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val = cursor.getString(0);
            }
            cursor.close();
        }
        return val;
    }

    public synchronized void setSetting(String key, String value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SETTING_KEY, key);
        cv.put(COLUMN_SETTING_VAL, value);
        db.insertWithOnConflict(TABLE_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
