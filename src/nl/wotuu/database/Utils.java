package nl.wotuu.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by Wouter on 6/18/13.
 */
public class Utils {
    /**
     * Join a string array or list with a delimiter.
     *
     * @param s         The array or list to join.
     * @param delimiter The delimiter to glue the pieces together with.
     * @return The joined string.
     */
    public static String join(Collection<?> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    /**
     * Get the difference between two date time objects.
     *
     * @param start The start time.
     * @param end   The end time.
     * @return The formatted string containing the difference
     */
    public static String getTimeDifference(Date start, Date end) {
        return Utils.getTimeStringRepresentation((end.getTime() - start.getTime()) / 1000);
    }

    /**
     * Get the string representation of a timespan in seconds.
     *
     * @param time The timespan.
     * @return The string.
     */
    public static String getTimeStringRepresentation(long time) {

        long diff[] = new long[]{0, 0, 0, 0};
    /* sec */
        diff[3] = (time >= 60 ? time % 60 : time);
    /* min */
        diff[2] = (time = (time / 60)) >= 60 ? time % 60 : time;
    /* hours */
        diff[1] = (time = (time / 60)) >= 24 ? time % 24 : time;
    /* days */
        diff[0] = (time = (time / 24));

        String[] suffixes = new String[]{"d", "h", "m", "s"};

        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (long value : diff) {
            if (value > 0)
                builder.append(String.format("%d%s ", value, suffixes[index]));
            index++;
        }

        String result = builder.toString();
        if( result.length() == 0)
            result += "0s";

        return result.trim();
    }

    /**
     * Safely parses an int.
     * @param s The string to parse.
     * @return The parsed number, or Integer.MIN_VALUE when none is found.
     */
    public static int safeIntegerParse(String s){
        int num = Integer.MIN_VALUE;
        try {
            num = Integer.parseInt(s);
        } catch (NumberFormatException e){
            //ignore
        }
        return num;
    }

    /**
     * Formats a number.
     *
     * @param number The number to format.
     * @return The formatted number.
     */
    public static String formatNumber(int number) {
        return new DecimalFormat("#,###").format(number);
    }

    /**
     * Formats a number.
     *
     * @param number The number to format.
     * @return The formatted number.
     */
    public static String formatNumber(long number) {
        return new DecimalFormat("#,###").format(number);
    }

    /**
     * Converts a stream to a string.
     * @param is The input stream.
     * @return The string containing the input stream's contents.
     * @throws Exception
     */
    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * Get a file's contents as a string.
     * @param filePath The file's path
     * @return
     * @throws Exception
     */
    public static String fileToString(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    /**
     * Splits a string based on its casing.
     * @param s The camel cased string.
     * @return The split string.
     */
    public static String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }
}
