package net.kdt.pojavlaunch.aerix.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Keeps the log of the launch before this one.
 *
 * <p>{@code Logger.begin} opens {@code latestlog.txt} with {@code O_WRONLY | O_TRUNC}
 * (see {@code jni/stdio_is.c}), so every launch starts from an empty file. That is right for the
 * log you are reading live, and it is fatal for the one you need: a game that dies on startup is
 * normally retried immediately, and the retry erases the only copy of the output that explains the
 * failure. "It crashes instantly and I have no log" is that truncation, not a missing report - the
 * report existed for exactly as long as the player took to tap launch again.
 *
 * <p>So before a launch truncates the file, the previous one is moved aside. Nothing here decides
 * whether the previous launch failed: a clean exit is archived too. Guessing wrong in the other
 * direction is what loses the evidence this exists to keep, and five small text files is a cheap
 * price for never guessing at all.
 *
 * <p>Pure {@code java.io} on purpose - no Android types - so the rotation can be exercised by
 * {@code tests/previous_launch_log_smoke.py} against a temporary directory.
 */
public final class PreviousLaunchLog {

    /** The live log, in the game home. Owned and truncated by the native logger. */
    public static final String LATEST_NAME = "latestlog.txt";

    /** Where finished launches are kept, as a folder beside that file. */
    public static final String ARCHIVE_DIR_NAME = "aerix-crash-logs";

    public static final String ARCHIVE_PREFIX = "aerix-log-";
    public static final String ARCHIVE_SUFFIX = ".txt";

    /** How many finished launches to keep. Bounded so a crash loop cannot fill the disk. */
    public static final int KEEP = 5;

    private static final String STAMP_FORMAT = "yyyyMMdd-HHmmss";

    private PreviousLaunchLog() { }

    /** Moves the live log aside, if it holds anything. Returns the archive, or null. */
    public static File archive(File gameHome) {
        return archive(gameHome, System.currentTimeMillis());
    }

    /**
     * Moves {@code gameHome/latestlog.txt} into the archive folder, unless it is missing or empty.
     *
     * <p>Never throws: losing the archive is a reason to launch anyway, and a failure here must not
     * become the crash this is meant to help diagnose.
     *
     * @return the archived file, or null when there was nothing to keep or the move failed
     */
    public static File archive(File gameHome, long whenMs) {
        if (gameHome == null) return null;
        try {
            File latest = new File(gameHome, LATEST_NAME);
            if (!latest.isFile() || latest.length() <= 0) return null;

            File dir = archiveDir(gameHome);
            if (!dir.isDirectory() && !dir.mkdirs()) return null;

            File target = uniqueTarget(dir, stamp(whenMs));
            if (target == null) return null;

            if (!latest.renameTo(target) && !copyThenTruncate(latest, target)) return null;
            prune(dir);
            return target;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static File archiveDir(File gameHome) {
        return new File(gameHome, ARCHIVE_DIR_NAME);
    }

    /** The archives, newest first. Empty when there are none or the folder is unreadable. */
    public static List<File> list(File gameHome) {
        List<File> found = new ArrayList<>();
        if (gameHome == null) return found;
        File dir = archiveDir(gameHome);
        File[] files = dir.listFiles(f ->
                f.isFile()
                        && f.getName().startsWith(ARCHIVE_PREFIX)
                        && f.getName().endsWith(ARCHIVE_SUFFIX));
        if (files == null) return found;
        found.addAll(Arrays.asList(files));
        found.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return found;
    }

    /** Deletes everything past {@link #KEEP}, oldest first. */
    static void prune(File dir) {
        File[] files = dir.listFiles(f ->
                f.isFile()
                        && f.getName().startsWith(ARCHIVE_PREFIX)
                        && f.getName().endsWith(ARCHIVE_SUFFIX));
        if (files == null || files.length <= KEEP) return;
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (int i = KEEP; i < files.length; i++) {
            //noinspection ResultOfMethodCallIgnored
            files[i].delete();
        }
    }

    /** A name that does not collide: two launches in the same second must keep both logs. */
    private static File uniqueTarget(File dir, String stamp) {
        File candidate = new File(dir, ARCHIVE_PREFIX + stamp + ARCHIVE_SUFFIX);
        for (int i = 1; candidate.exists() && i < 1000; i++) {
            candidate = new File(dir, ARCHIVE_PREFIX + stamp + "-" + i + ARCHIVE_SUFFIX);
        }
        return candidate.exists() ? null : candidate;
    }

    private static String stamp(long whenMs) {
        return new SimpleDateFormat(STAMP_FORMAT, Locale.US).format(new Date(whenMs));
    }

    /**
     * Fallback for a filesystem that will not rename. The copy has to be complete before the
     * original is emptied, or a failure halfway through would leave an archive shorter than the
     * crash it was taken from.
     */
    private static boolean copyThenTruncate(File source, File target) {
        try {
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            if (target.length() != source.length()) {
                //noinspection ResultOfMethodCallIgnored
                target.delete();
                return false;
            }
            try (FileOutputStream clear = new FileOutputStream(source, false)) {
                clear.flush();
            }
            return true;
        } catch (IOException error) {
            //noinspection ResultOfMethodCallIgnored
            target.delete();
            return false;
        }
    }
}
