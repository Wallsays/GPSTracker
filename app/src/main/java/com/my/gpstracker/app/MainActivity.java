package com.my.gpstracker.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


public class MainActivity extends Activity {

    private float updateFreq = 0.083f; //in minutes

    //CONSTANTS
    public static final String UPDATE_FREQUENCY = "frequency";
    public static final String IS_IN_FILE = "infile";
    public static final String MESSENGER = "messenger";
    public static final String SAVED_TEXT = "tvtext";
    public static final String SAVED_OUT_CHOICE = "out";
    public static final String SAVED_FREQUENCY_UPDATE = "frequency";

    private boolean writeInFile = false;

    //TODO ELEVATION
    //TODO GOOGLE MAPS
    //TODO ПРоверить меню
    //TODO 5 sec 15 sec 1 min 2 min 5 min
    //TODO onPause - onResume crash
    //TODO после сворачивания и через некоторое время краш
    private GPSTrackingService myServiceBinder;
    private ServiceConnection myConnection;
    private Handler myHandler;

    private TextView coordTV;
    private Button startBtn;
    private RelativeLayout mainLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MyUtils.writeToLogFile("entering onCreate");
        mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);

        startBtn = (Button) findViewById(R.id.button);
        coordTV = (TextView) findViewById(R.id.textView);
        coordTV.setMovementMethod(new ScrollingMovementMethod());
        if (savedInstanceState != null) {
            MyUtils.writeToLogFile("Restoring activity state");

            if (GPSTrackingService.isWritingInFile()) {
                writeInFile = true;
                coordTV.setVisibility(View.GONE);
            }

            if (GPSTrackingService.isTracking()) {
                //holo_red_light
                mainLayout.setBackgroundColor(Color.argb(255, 255, 66, 66));
                startBtn.setText("Стоп");
            }
        }


        myHandler = new Handler() {
            public void handleMessage(Message message) {
                MyUtils.writeToLogFile("Activity got new message");
                final Bundle data = message.getData();
                if (data != null) {
                    appendTextAndScroll(data.getString(GPSTrackingService.COORDINATE));
                }
            }
        };


        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!GPSTrackingService.isTracking()) {
                    MyUtils.writeToLogFile("start button pressed");
                    if (!isGPSon()) {
                        MyUtils.writeToLogFile("start button cancelled");
                        return;
                    }
                    //holo_red_light
                    mainLayout.setBackgroundColor(Color.argb(255, 255, 66, 66));
                    ((Button) v).setText("Стоп");
                    new StartTrackingTask().execute();

                }
                else {
                    MyUtils.writeToLogFile("stop button pressed");
                    //holo_blue_light
                    mainLayout.setBackgroundColor(Color.argb(255, 51, 181, 229));
                    ((Button) v).setText("Старт");
                    new StopTrackingTask().execute();
                }
            }
        });
    }

    class StartTrackingTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            startTracking();
            return null;
        }
    }

    class StopTrackingTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            stopTracking();
            return null;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        MyUtils.writeToLogFile("entered onResume");
        if (GPSTrackingService.isWritingInFile()) {
            coordTV.setVisibility(View.GONE);
        }

        if (GPSTrackingService.isTracking()) {
            //holo_red_light
            mainLayout.setBackgroundColor(Color.argb(255, 255, 66, 66));
            startBtn.setText("Стоп");
        }

        // Restore preferences
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        coordTV.setText(settings.getString(SAVED_TEXT, ""));
        writeInFile = settings.getBoolean(SAVED_OUT_CHOICE, false);
        updateFreq = settings.getFloat(SAVED_FREQUENCY_UPDATE, updateFreq);

        //Read coordinates got when application was onPause if they exist
        try {
            MyUtils.writeToLogFile("Reading temporary file");
            coordTV.append(MyUtils.readTempFile(Environment.getExternalStorageDirectory() +
                    System.getProperty("file.separator") +
                    "GPSTracker" + System.getProperty("file.separator")
                    + "CoordinatesTmp.txt"));
        }
        catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            MyUtils.writeToLogFile(exceptionAsString);
        }

        //Restart service if needed
        if (GPSTrackingService.isTracking() &&
                !GPSTrackingService.isWritingInFile()) {
            stopTracking();
            startTracking();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SAVED_TEXT, coordTV.getText().toString());
        editor.putBoolean(SAVED_OUT_CHOICE, writeInFile);
        editor.putFloat(SAVED_FREQUENCY_UPDATE, updateFreq);

        // Commit the edits!
        editor.commit();
        //Set service to write in temp file
        if (myServiceBinder != null)
            myServiceBinder.setWorkingInBackground(true);
        /*if (myServiceBinder != null &&
                GPSTrackingService.isTracking() &&
                !GPSTrackingService.isWritingInFile()) {
            unbindService(myConnection);
            myServiceBinder = null;
        }*/

    }

    /*@Override
    protected void onDestroy() {
        MyUtils.writeToLogFile("entered onDestroy");
        if (myServiceBinder != null &&
                GPSTrackingService.isTracking() &&
                !GPSTrackingService.isWritingInFile()) {
            stopTracking();
            myServiceBinder = null;
        }
        super.onDestroy();
    }*/


    private void appendTextAndScroll(String text)
    {
        if(coordTV != null){
            coordTV.append(text + "\n");
            final Layout layout = coordTV.getLayout();
            if(layout != null){
                int scrollDelta = layout.getLineBottom(coordTV.getLineCount() - 1)
                        - coordTV.getScrollY() - coordTV.getHeight();
                if(scrollDelta > 0)
                    coordTV.scrollBy(0, scrollDelta);
            }
        }
    }

    private boolean isGPSon() {
        boolean gpsON = ((LocationManager) this.getSystemService(Context.LOCATION_SERVICE))
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsON) {
            MyUtils.writeToLogFile("GPS is offline. Error dialog.");
            new AlertDialog.Builder(this)
                    .setTitle("Ошибка")
                            // добавляем одну кнопку для закрытия диалога
                    .setNeutralButton("ОК",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dialog.dismiss();
                                }
                            })

                            // добавляем переключатели
                    .setMessage("GPS отключен")
                    .create()
                    .show();
            return false;
        }
        return true;
    }

    //on start button click
    private void startTracking() {

        Intent serviceIntent = new Intent(this, GPSTrackingService.class);
        serviceIntent.putExtra(UPDATE_FREQUENCY, updateFreq);
        serviceIntent.putExtra(IS_IN_FILE, writeInFile);
        startService(serviceIntent);
        MyUtils.writeToLogFile("Service started");
        if (!writeInFile) {
            MyUtils.writeToLogFile("Start binding service");
            doBindService();
        }
    }


    //On stop button click
    private void stopTracking() {
        MyUtils.writeToLogFile("Stopping GPS tracking");
        if (myServiceBinder != null) {
            unbindService(myConnection);
            myServiceBinder = null;
        }
        stopService(new Intent(this, GPSTrackingService.class));

    }


    private void doBindService() {

        myConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder binder) {
                myServiceBinder = ((GPSTrackingService.MyBinder) binder).getService();
            }

            public void onServiceDisconnected(ComponentName className) {
                myServiceBinder = null;
            }
        };


        Intent intent = new Intent(this, GPSTrackingService.class);
        // Create a new Messenger for the communication back
        // From the Service to the Activity
        Messenger messenger = new Messenger(myHandler);
        intent.putExtra(MESSENGER, messenger);

        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
        MyUtils.writeToLogFile("Service has been binded");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            final String[] mFreqChoices = { "5 секунд", "1 минута", "5 минут", "15 минут", "30 минут", "60 минут" };

            new AlertDialog.Builder(this)
                .setTitle("Частота обновления текущего положения")
                        // добавляем одну кнопку для закрытия диалога
                .setNeutralButton("Применить",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {

                                MyUtils.writeToLogFile("New interval selected" + id);
                                int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                                setUpdateTime(selectedPosition);
                                dialog.dismiss();
                                if (GPSTrackingService.isTracking()) {
                                    MyUtils.writeToLogFile("Beginning service restart");
                                    stopTracking();
                                    startTracking();
                                    MyUtils.writeToLogFile("Service restarted successfuly");
                            }
                            }
                        })

                 // добавляем переключатели
                .setSingleChoiceItems(mFreqChoices, getFreqPosition(), null)
                .create()
                .show();
        }
        else if (id == R.id.action_out) {

            final String[] mSelection ={"Вывод в файл", "Вывод на экран"};

            new AlertDialog.Builder(this)
                    .setTitle("Выберите как выводить данные") // заголовок для диалога
                    .setSingleChoiceItems(mSelection, getOutPosition(), null)
                    .setNeutralButton("Применить",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int item) {
                                    int selectedPosition = ((AlertDialog) dialog).getListView()
                                            .getCheckedItemPosition();
                                    if (selectedPosition == 0) {
                                        MyUtils.writeToLogFile("Write in file selected");
                                        writeInFile = true;
                                        coordTV.setVisibility(View.GONE);
                                    } else {
                                        MyUtils.writeToLogFile("Show on screen selected");
                                        writeInFile = false;
                                        coordTV.setVisibility(View.VISIBLE);
                                    }
                                    if (GPSTrackingService.isTracking()) {
                                        MyUtils.writeToLogFile("Beginning service restart");
                                        stopTracking();
                                        startTracking();
                                        MyUtils.writeToLogFile("Service restarted successfuly");
                                    }
                                }

                            })
                    .create()
                    .show();
            }
            else if (id == R.id.action_clear) {
            coordTV.setText("");
        }
        return super.onOptionsItemSelected(item);
    }

    private int getFreqPosition() {
        if (updateFreq == 0.083) return 0;
            else if (updateFreq == 1) return 1;
            else if (updateFreq == 5) return 2;
            else if (updateFreq == 15) return 3;
            else if (updateFreq == 30) return 4;
            else if (updateFreq == 60) return 5;
            else return -1;
        }

    private int getOutPosition() {
        if (GPSTrackingService.isTracking())
            if (GPSTrackingService.isWritingInFile())
                return 0;
            else
                return 1;
        else if (writeInFile)
            return 0;
        else
            return 1;
    }

    private void setUpdateTime(int choice) {
        switch(choice) {
            case 0: {
                updateFreq = 0.083f;
                break;
            }
            case 1: {
                updateFreq = 1;
                break;
            }
            case 2: {
                updateFreq = 5;
                break;
            }
            case 3: {
                updateFreq = 15;
                break;
            }
            case 4: {
                updateFreq = 30;
                break;
            }
            case 5: {
                updateFreq = 60;
                break;
            }
            default: {
                //Do nothing
            }
        }
    }

}
