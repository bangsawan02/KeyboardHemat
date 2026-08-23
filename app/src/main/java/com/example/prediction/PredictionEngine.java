package com.example.prediction;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import com.example.database.DatabaseHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Background Text Prediction Engine for Indonesian Vocabulary.
 * Runs prediction queries, dictionary indexing, and database sync on a low-priority background thread
 * to guarantee ultra-low memory usage, zero UI lag, and smooth 60fps typing.
 */
public class PredictionEngine {

    private static final Pattern SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ]");

    public interface PredictionCallback {
        void onPredictionsReady(String query, List<String> predictions);
    }

    private final Trie trie;
    private final ExecutorService backgroundExecutor;
    private final Handler mainHandler;
    private final AtomicLong querySequence = new AtomicLong(0);
    private volatile boolean isInitialized = false;

    private static volatile PredictionEngine instance;

    public static PredictionEngine getInstance() {
        if (instance == null) {
            synchronized (PredictionEngine.class) {
                if (instance == null) {
                    instance = new PredictionEngine();
                }
            }
        }
        return instance;
    }

    public PredictionEngine() {
        this.trie = new Trie();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.backgroundExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "IndoPredictionWorker");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });
    }

    /**
     * Initializes dictionary and user custom words from SQLite DB asynchronously in the background.
     */
    public void initAsync(final DatabaseHelper dbHelper, final Runnable onReady) {
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                } catch (Throwable ignored) {}

                if (dbHelper != null) {
                    try {
                        List<DatabaseHelper.WordItem> words = dbHelper.searchWords(null, null);
                        for (DatabaseHelper.WordItem item : words) {
                            trie.insert(item.word, item.frequency);
                        }

                        // Load User Bigrams
                        List<DatabaseHelper.BigramItem> bigrams = dbHelper.getAllUserBigrams();
                        for (DatabaseHelper.BigramItem item : bigrams) {
                            trie.learnBigram(item.prevWord, item.nextWord);
                        }

                        // Load User Trigrams
                        List<DatabaseHelper.TrigramItem> trigrams = dbHelper.getAllUserTrigrams();
                        for (DatabaseHelper.TrigramItem item : trigrams) {
                            trie.learnTrigram(item.prevWord1, item.prevWord2, item.nextWord);
                        }

                        // Load AutoText mapping
                        trie.clearAutoText();
                        List<DatabaseHelper.AutoTextItem> autoTexts = dbHelper.getAllAutoText(null);
                        for (DatabaseHelper.AutoTextItem item : autoTexts) {
                            trie.addAutoText(item.shortcut, item.replacement);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                isInitialized = true;
                if (onReady != null) {
                    mainHandler.post(onReady);
                }
            }
        });
    }

    public void reloadAutoTextAsync(final DatabaseHelper dbHelper) {
        if (dbHelper == null) return;
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    trie.clearAutoText();
                    List<DatabaseHelper.AutoTextItem> autoTexts = dbHelper.getAllAutoText(null);
                    for (DatabaseHelper.AutoTextItem item : autoTexts) {
                        trie.addAutoText(item.shortcut, item.replacement);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void addAutoTextAsync(final String shortcut, final String replacement) {
        if (shortcut == null || replacement == null) return;
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                trie.addAutoText(shortcut, replacement);
            }
        });
    }

    public void removeAutoTextAsync(final String shortcut) {
        if (shortcut == null) return;
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                trie.removeAutoText(shortcut);
            }
        });
    }

    /**
     * Executes prediction lookup in the background with contextual previous word support.
     */
    public void predictAsync(final String prefix, final String previousWord, final int maxResults, final PredictionCallback callback) {
        predictAsync(prefix, null, previousWord, maxResults, callback);
    }

    /**
    * Executes prediction lookup in the background with contextual N-gram (trigram & bigram) support.
    */
    public void predictAsync(final String prefix, final String prevWord1, final String prevWord2, final int maxResults, final PredictionCallback callback) {
        final long currentQueryId = querySequence.incrementAndGet();
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // If a newer query was requested while this task was queued, skip computation
                if (currentQueryId != querySequence.get()) {
                    return;
                }

                try {
                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                } catch (Throwable ignored) {}

                final List<String> results = trie.getPredictions(prefix, prevWord1, prevWord2, maxResults);

                // Check again before posting back to main thread
                if (currentQueryId == querySequence.get() && callback != null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (currentQueryId == querySequence.get()) {
                                callback.onPredictionsReady(prefix, results);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Executes prediction lookup in the background. Discards outdated queries automatically.
     */
    public void predictAsync(final String prefix, final int maxResults, final PredictionCallback callback) {
        predictAsync(prefix, null, maxResults, callback);
    }

    /**
     * Gets instantaneous auto-correction for abbreviations/slang synchronously.
     */
    public String getAutoCorrection(String word) {
        return trie.getAutoCorrection(word);
    }

    /**
     * Learns a typed word or tokens and persists to SQLite database entirely in background.
     */
    public void learnWordAsync(final String text, final boolean isCustom, final DatabaseHelper dbHelper) {
        if (text == null || text.trim().isEmpty()) return;
        final String raw = text.trim();
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                } catch (Throwable ignored) {}

                if (raw.contains(" ")) {
                    String[] parts = raw.split("\\s+");
                    for (String p : parts) {
                        String clean = SANITIZE_PATTERN.matcher(p).replaceAll("").toLowerCase();
                        if (clean.length() >= 2) {
                            trie.insert(clean, isCustom ? 50 : 1);
                            if (dbHelper != null) {
                                try {
                                    dbHelper.addOrIncrementWord(clean, isCustom);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                } else {
                    String clean = SANITIZE_PATTERN.matcher(raw).replaceAll("").toLowerCase();
                    if (clean.length() >= 2) {
                        trie.insert(clean, isCustom ? 50 : 1);
                        if (dbHelper != null) {
                            try {
                                dbHelper.addOrIncrementWord(clean, isCustom);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        });
    }

    /**
     * Learns user typing style (bigrams and trigrams context) and word frequency in the background.
     */
    public void learnStyleAsync(final String wordTwoStepsAgo, final String wordOneStepAgo, final String currentWord, final DatabaseHelper dbHelper) {
        if (currentWord == null || currentWord.trim().isEmpty()) return;
        final String cleanWord = SANITIZE_PATTERN.matcher(currentWord.trim().toLowerCase()).replaceAll("");
        if (cleanWord.length() < 1) return;
        
        final String w1 = wordOneStepAgo != null ? wordOneStepAgo.trim().toLowerCase() : null;
        final String w2 = wordTwoStepsAgo != null ? wordTwoStepsAgo.trim().toLowerCase() : null;

        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                } catch (Throwable ignored) {}

                // 1. Learn word frequency
                trie.insert(cleanWord, 35);
                if (dbHelper != null) {
                    try {
                        dbHelper.addOrIncrementWord(cleanWord, true);
                    } catch (Exception ignored) {}
                }

                // 2. Learn typing style context (Bigram / Trigram)
                // If we have "WordTwoStepsAgo" (w2) and "WordOneStepAgo" (w1), learn Trigram "w2 w1" -> currentWord
                if (w2 != null && !w2.isEmpty() && w1 != null && !w1.isEmpty()) {
                    trie.learnTrigram(w2, w1, cleanWord);
                    if (dbHelper != null) {
                        try {
                            dbHelper.addOrIncrementTrigram(w2, w1, cleanWord);
                        } catch (Exception ignored) {}
                    }
                }
                // Learn Bigram "WordOneStepAgo" (w1) -> currentWord
                if (w1 != null && !w1.isEmpty()) {
                    trie.learnBigram(w1, cleanWord);
                    if (dbHelper != null) {
                        try {
                            dbHelper.addOrIncrementBigram(w1, cleanWord);
                        } catch (Exception ignored) {}
                    }
                }
            }
        });
    }

    /**
     * Removes a word from in-memory trie in the background.
     */
    public void removeWordAsync(final String word) {
        if (word == null) return;
        final String clean = word.trim().toLowerCase();
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                trie.remove(clean);
            }
        });
    }

    /**
     * Synchronous prediction lookup.
     */
    public List<String> getPredictionsSync(String prefix, int maxResults) {
        return trie.getPredictions(prefix, maxResults);
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public Trie getTrie() {
        return trie;
    }
}
