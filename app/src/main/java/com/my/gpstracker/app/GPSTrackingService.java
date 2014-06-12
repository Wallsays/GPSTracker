package com.my.gpstracker.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.io.File;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by Sknictik on 11.06.14.
 */
public class GPSTrackingService extends Service {

    private LocationManager locationManager;
    private MyLocationListener locationListener;
    private Messenger outMessenger;

    //TODO Make service in different thread
    private static boolean tracking = false;
    private static boolean writeInFile;
    private static boolean writeInTempFile;

    public static final String COORDINATE = "coordinate";

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(final Location location) {
            // Called when a new location is found by the network location provider.
            MyUtils.writeToLogFile("Service acquired new location");
            MyUtils.writeToLogFile("Building notification");
            Notification newCoord = new Notification.Builder(getApplicationContext())
                    .setContentTitle("Получены новые координаты")
                    .setContentText(MyUtils.formatTime(location.getTime()) +
                            " Latitude:" + location.getLatitude() +
                            " Longitude:" + location.getLongitude())
                    .setAutoCancel(true)
                    .setShowWhen(true)
                    .setSmallIcon(R.drawable.icon)
                    .build();

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.notify(0, newCoord);
            MyUtils.writeToLogFile("Notification sent");
            if (writeInFile) {
                new Thread( new Runnable() {
                @Override
                public void run() {
                    MyUtils.writeToLogFile("\"Write in file\" thread running");
                    try {
                        File newFolder = new File(Environment.getExternalStorageDirectory(), "GPSTracker");
                        if (!newFolder.exists()) {
                            newFolder.mkdir();
                            assert newFolder.isDirectory();
                        }
                        try {
                            File file = new File(newFolder, "Coordinates" + ".txt");
                            file.createNewFile();
                            assert file.isFile();
                            MyUtils.appendToFile(MyUtils.formatTime(location.getTime()) +
                                    " Latitude:" + location.getLatitude() +
                                    " Longitude:" + location.getLongitude(),
                                    file);
                            MyUtils.writeToLogFile("New entry added to file Coordinates.txt");

                        } catch (Exception ex) {
                            StringWriter sw = new StringWriter();
                            ex.printStackTrace(new PrintWriter(sw));
                            String exceptionAsString = sw.toString();
                            MyUtils.writeToLogFile(exceptionAsString);
                        }
                    } catch (Exception e) {
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        String exceptionAsString = sw.toString();
                        MyUtils.writeToLogFile(exceptionAsString);
                    }
                    }
            }).start();
            }
            else if (!writeInTempFile)
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MyUtils.writeToLogFile("\"Send coordinate to activity\" thread running");
                        Message coord = Message.obtain();
                        Bundle myBundle = new Bundle();
                        myBundle.putString(COORDINATE, MyUtils.formatTime(location.getTime()) +
                                " Latitude:" + location.getLatitude() +
                                " Longitude:" + location.getLongitude());
                        assert coord != null;
                        coord.setData(myBundle);
                        MyUtils.writeToLogFile("Coordinate sent");
                        try {
                            outMessenger.send(coord);
                        } catch (RemoteException e) {
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            String exceptionAsString = sw.toString();
                            MyUtils.writeToLogFile(exceptionAsString);
                        }
                    }
                }).start();
            }
            else {
                new Thread( new Runnable() {
                    @Override
                    public void run() {
                    MyUtils.writeToLogFile("\"Write in temp file\" thread running");
                    try {
                        File newFolder = new File(Environment.getExternalStorageDirectory(), "GPSTracker");
                        if (!newFolder.exists()) {
                            newFolder.mkdir();
                            assert newFolder.isDirectory();
                        }
                        try {
                            File file = new File(newFolder, "CoordinatesTmp" + ".txt");
                            file.createNewFile();
                            assert file.isFile();
                            MyUtils.appendToFile(MyUtils.formatTime(location.getTime()) +
                                    " Latitude:" + location.getLatitude() +
                                    " Longitude:" + location.getLongitude(),
                                    file);
                            MyUtils.writeToLogFile("New entry added to file CoordinatesTmp.txt");

                        } catch (Exception ex) {
                        StringWriter sw = new StringWriter();
                        ex.printStackTrace(new PrintWriter(sw));
                        String exceptionAsString = sw.toString();
                        MyUtils.writeToLogFile(exceptionAsString);
                        }
                    } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String exceptionAsString = sw.toString();
                    MyUtils.writeToLogFile(exceptionAsString);
                    }
                }
        }).start();
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    }

    public class MyBinder extends Binder {
        GPSTrackingService getService() {
            return GPSTrackingService.this;
        }
    }


    public static boolean isTracking() {
        return tracking;
    }

    public static boolean isWritingInFile() {
        return writeInFile;
    }

    @Override
    public void onCreate() {
        writeInTempFile = false;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        MyUtils.writeToLogFile("Location manager initialized");
        locationListener = new MyLocationListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MyUtils.writeToLogFile("Begin starting service");
        final double updateFreq = intent.getDoubleExtra(MainActivity.UPDATE_FREQUENCY, 5);
        writeInFile = intent.getBooleanExtra(MainActivity.IS_IN_FILE, false);

        // Register the listener with the Location Manager to receive location updates
        boolean gpsON = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (gpsON) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    (int) (updateFreq * 60 * 1000), 0, locationListener);
            tracking = true;
            MyUtils.writeToLogFile("Location listener registered");
        }

        return START_STICKY;
    }

    public void setWorkingInBackground(boolean param) {
        MyUtils.writeToLogFile("Background mode: " + String.valueOf(param));
        writeInTempFile = param;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Bundle extras = intent.getExtras();
        MyUtils.writeToLogFile("Start binding service");
        // Get messenger from the Activity
        if (extras != null) {
            MyUtils.writeToLogFile("Getting messenger from extra bundle");
            outMessenger = (Messenger) extras.get(MainActivity.MESSENGER);
        }
        return new MyBinder();
    }

    @Override
    public void onDestroy() {
        MyUtils.writeToLogFile("Destroying service");
        tracking = false;
        locationManager.removeUpdates(locationListener);
        MyUtils.cleanTmp();
        super.onDestroy();
    }

}