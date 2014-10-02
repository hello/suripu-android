package com.hello.ble.util;

import android.os.Environment;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by pangwu on 3/3/14.
 */
public class IO {
    public final static String DOWNLOAD_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();


    public static boolean appendStringToFile(File file, String content) {
        try {
            FileWriter fileWriter = new FileWriter(file, true);
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter, 100 * 1024);

            bufferWriter.write(content);
            bufferWriter.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean writeStringToFile(File file, String content) {
        try {
            FileWriter fileWriter = new FileWriter(file, false);
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter, 100 * 1024);

            bufferWriter.write(content);
            bufferWriter.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }


    public static File getCSVFileforToday(String prefix, String header) {

        File targetFile = getFileByDate(new DateTime(), prefix, "csv");

        if (!targetFile.exists()) {
            IO.appendStringToFile(targetFile, header + "\r\n");
        }


        return targetFile;
    }

    public static File getFileByDate(DateTime date, String prefix, String extension) {
        File parentDirectory = new File(DOWNLOAD_DIR);
        if (!parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }


        String filePathPrefix = DOWNLOAD_DIR + "/" + prefix;

        String fileName = filePathPrefix + "_" + date.toString("dd_MM_yyy") + "." + extension;
        File targetFile = new File(fileName);
        //targetFile.mkdirs();

        return targetFile;
    }

    public static File getFile(String fileName) {
        final File file = new File(DOWNLOAD_DIR + "/" + fileName);
        return file;
    }

    public static DateTime getDateFromFileName(String fileName) {
        String[] parts = IO.getFileName(fileName).split("_");
        DateTime date = null;

        if (parts.length > 3) {
            String dateString = parts[parts.length - 3] + "_" +
                    parts[parts.length - 2] + "_" +
                    parts[parts.length - 1].substring(0, parts[parts.length - 1].indexOf("."));
            date = DateTime.parse(dateString, DateTimeFormat.forPattern("dd_MM_yyyy")).withTimeAtStartOfDay();

        }

        return date;
    }

    public static File getFileByName(String prefix, String extension) {
        File parentDirectory = new File(DOWNLOAD_DIR);
        if (!parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }

        String fileName = DOWNLOAD_DIR + "/" + prefix + "." + extension;
        File file = new File(fileName);
        //file.mkdirs();
        return file;
    }

    public static String[] getFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.isDirectory())
            return new String[0];
        File[] files = directory.listFiles();
        String[] result = new String[files.length];

        int index = 0;
        for (File file : files) {
            result[index] = file.getAbsolutePath();
            index++;
        }

        return result;
    }

    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static String getFileExtension(String path) {
        if (path == null) {
            return "";
        }

        int indexOfDot = path.lastIndexOf(".");
        if (indexOfDot == -1) {
            return "";
        }

        return path.substring(indexOfDot + 1);
    }

    public static void log(String content) {
        DateTime today = new DateTime();
        File logFile = getFileByDate(today, "ble", "txt");

        appendStringToFile(logFile, today.toString("yyyy-MM-dd HH:mm:ss") + ": " + content + "\r\n");
    }

    public static void log(Throwable throwable) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        log(writer.toString());
    }
}
