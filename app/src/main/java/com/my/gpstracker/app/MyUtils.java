package com.my.gpstracker.app;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;

/**
 * Created by Sknictik on 12.06.14.
 */
public abstract class MyUtils {

    public static String formatTime(long timeInMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeInMillis);
        DecimalFormat nft = new DecimalFormat("#00.###");
        nft.setDecimalSeparatorAlwaysShown(false);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        return nft.format(month) + "." + nft.format(day) + "  " + nft.format(hour)
                + ":" + nft.format(minute) + ":" + nft.format(second);
    }

    public static void writeToLogFile(String line) {
        try {
            File newFolder = new File(Environment.getExternalStorageDirectory(), "GPSTracker");
            if (!newFolder.exists()) {
                newFolder.mkdir();
                assert newFolder.isDirectory();
            }
            try {
                File file = new File(newFolder, "Log" + ".txt");
                file.createNewFile();
                assert file.isFile();
                DecimalFormat nft = new DecimalFormat("#00.###");
                nft.setDecimalSeparatorAlwaysShown(false);
                Calendar cal = Calendar.getInstance();
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);
                int second = cal.get(Calendar.SECOND);
                String time = nft.format(hour)
                        + ":" + nft.format(minute) + ":" + nft.format(second);
                appendToFile(time + line + "\n",file);
            }
            catch (Exception ex) {
                System.out.println("ex: " + ex);
            }
        }
        catch (Exception e) {
            System.out.println("e: " + e);
        }
    }

    public static void appendToFile(String dataAppend, File file) throws IOException
    {
        BufferedWriter fWriter = new BufferedWriter(new FileWriter(file, true));
        fWriter.write(dataAppend);
        fWriter.write(System.getProperty("line.separator"));
        fWriter.flush();
        fWriter.close();
    }

    public static String readTempFile(String fileName) throws IOException {
        File tempCoordFile = new File(fileName);
        if (!tempCoordFile.exists())
            return "";
        BufferedReader reader = new BufferedReader(new FileReader(tempCoordFile));
        StringBuilder text = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            text.append(line);
            text.append("\n");
        }
        reader.close();
        if (tempCoordFile.delete()) {
            writeToLogFile("Unable to delete temp file");
        }
        return text.toString();
    }

    public static void cleanTmp() {
        File tempCoordFile = new File(Environment.getExternalStorageDirectory() +
                System.getProperty("file.separator") +
                "GPSTracker" + System.getProperty("file.separator")
                + "CoordinatesTmp.txt");
        if (tempCoordFile.exists())
            tempCoordFile.delete();
    }

}
