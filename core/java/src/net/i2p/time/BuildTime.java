package net.i2p.time;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import net.i2p.I2PAppContext;
import net.i2p.util.SystemVersion;

/**
 *  Get the build date as set in i2p.jar,
 *  and reasonable min and max values for the current time,
 *  to be used as sanity checks.
 *
 *  Idea taken from Chrome, which assumes any clock more than
 *  2 days before or 1 year after the build date is bad.
 *
 *  Not maintained as a public API, not for use by plugins or applications.
 *
 *  @since 0.9.25 modded from FileDumpHelper
 */
public class BuildTime {
    
    private static final long _buildTime;
    private static final long _earliestTime;
    private static final long _latestTime;
    private static final long YEARS_25 = 25L*365*24*60*60*1000;
    /** update this periodically */
    private static final String EARLIEST = "2016-02-19 12:00:00 UTC";

    static {
        // this is the standard format of build.timestamp as set in the top-level build.xml
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
        TimeZone utc = TimeZone.getTimeZone("GMT");
        fmt.setTimeZone(utc);
        long min;
        try {
            Date date = fmt.parse(EARLIEST);
            if (date == null)
                throw new RuntimeException("BuildTime FAIL");
            min = date.getTime();
        } catch (ParseException pe) {
            System.out.println("BuildTime FAIL");
            pe.printStackTrace();
            throw new RuntimeException("BuildTime FAIL", pe);
        }
        long max = min + YEARS_25;
        long build = getBuildTime(fmt, "i2p.jar");
        if (build > max) {
            System.out.println("Warning: Strange build time, contact packager: " + new Date(build));
            build = max;
        } else if (build < min) {
            if (build > 0)
                System.out.println("Warning: Strange build time, contact packager: " + new Date(build));
            build = min;
        } else {
            // build time looks reasonable
            // allow 24h skew on build machine
            min = build - 24*60*60*1000L;
        }
        _earliestTime = min;
        _latestTime = max;
        _buildTime = build;
    }

    /**
     *  Get the build date for i2p.jar.
     *
     *  @return the earliest possible time if actual build date is unknown
     */
    public static long getBuildTime() {
        return _buildTime;
    }

    /**
     *  Get the earliest it could possibly be right now.
     *  Latest of the build time minus a day, or a hardcoded time.
     *
     *  @return the time
     */
    public static long getEarliestTime() {
        return _earliestTime;
    }

    /**
     *  Get the latest it could possibly be right now.
     *  Hardcoded.
     *
     *  @return the time
     */
    public static long getLatestTime() {
        return _latestTime;
    }

    private BuildTime() {}

    /**
     *  Won't be available on Android or on any builds not using our build.xml.
     *
     *  @return 0 if unknown
     */
    private static long getBuildTime(SimpleDateFormat fmt, String jar) {
        if (SystemVersion.isAndroid())
            return 0;
        File f = new File(I2PAppContext.getGlobalContext().getBaseDir(), "lib");
        f = new File(f, jar);
        Attributes atts = attributes(f);
        if (atts == null)
            return 0;
        String s = atts.getValue("Build-Date");
        if (s == null)
            return 0;
        try {
            Date date = fmt.parse(s);
            if (date != null) {
                return date.getTime();
            }
        } catch (ParseException pe) {}
        return 0;
    }

    private static Attributes attributes(File f) {
        InputStream in = null;
        try {
            in = (new URL("jar:file:" + f.getAbsolutePath() + "!/META-INF/MANIFEST.MF")).openStream();
            Manifest man = new Manifest(in);
            return man.getMainAttributes();
        } catch (IOException ioe) {
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (IOException e) {}
        }
    }

/****
    public static void main(String[] args) {
        long date = getEarliestTime();
        System.out.println("Earliest date: " + new Date(date));
        date = getBuildTime();
        System.out.println("Build date:    " + new Date(date));
        date = System.currentTimeMillis();
        System.out.println("System time:   " + new Date(date));
        date = I2PAppContext.getGlobalContext().clock().now();
        System.out.println("I2P time:      " + new Date(date));
        date = getLatestTime();
        System.out.println("Latest date:   " + new Date(date));
    }
****/
}