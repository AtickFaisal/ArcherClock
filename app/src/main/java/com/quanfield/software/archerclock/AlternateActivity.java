package com.quanfield.software.archerclock;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import devadvance.circularseekbar.CircularSeekBar;

public class AlternateActivity extends AppCompatActivity {

    private static int preparationTime = 10, mainTime = 80;
    long delay1 = 1000, delay2 = 1000;
    long startTime1 = 0, startTime2 = 0;
    private CircularSeekBar timerClock1, timerClock2;
    private TextView timerText1, timerText2, labelText;
    private Button settingsButton;
    private Menu menu;
    private MqttAndroidClient client;
    private boolean FLAG1 = false, FLAG2 = false;
    private boolean phaseEnded1 = false, phaseEnded2 = false, notDestroyed = true, selection = true, resetMode = true, stupidFlag = false;
    private int counter1 = preparationTime, counter2 = preparationTime;


    @SuppressLint("HandlerLeak")
    Handler handler1 = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            //////////////////////////////////////////////////////////////////////////////////////// check if prep time ended
            if (counter1 == 0 && !phaseEnded1) { /////////////////////////////////////////////////// enter here if prep time ended
                sendColorCode("green"); //////////////////////////////////////////////////////////// send color code for main time
                counter1 = mainTime; /////////////////////////////////////////////////////////////// set counter 1 time to main time
                timerClock1.setMax(mainTime); ////////////////////////////////////////////////////// set the max of the ring
                labelText.setText("Main Time"); //////////////////////////////////////////////////// change label to main time
                //////////////////////////////////////////////////////////////////////////////////// change the colors
                timerClock1.setCircleProgressColor(getResources().getColor(R.color.mainTime));
                timerClock1.setPointerHaloColor(getResources().getColor(R.color.mainTime));
                timerClock1.setPointerColor(getResources().getColor(R.color.mainTime));
                phaseEnded1 = true; //////////////////////////////////////////////////////////////// set the flag for main time to true
            }
            sendValue1(); ////////////////////////////////////////////////////////////////////////// send the value for player1
            //////////////////////////////////////////////////////////////////////////////////////// update the clock
            timerText1.setText(String.format(Locale.US, "%ds", counter1));
            timerClock1.setProgress(counter1);
        }
    };

    @SuppressLint("HandlerLeak")
    Handler handler2 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (counter2 == 0 && !phaseEnded2) {
                counter2 = mainTime;
                timerClock2.setMax(mainTime);

                timerClock2.setCircleProgressColor(getResources().getColor(R.color.mainTime));
                timerClock2.setPointerHaloColor(getResources().getColor(R.color.mainTime));
                timerClock2.setPointerColor(getResources().getColor(R.color.mainTime));

                phaseEnded2 = true;


                if (selection) {
                    FLAG1 = true;
                    FLAG2 = false;
                } else {
                    FLAG1 = false;
                    FLAG2 = true;
                }

            }
            sendValue2();

            timerText2.setText(String.format(Locale.US, "%ds", counter2));
            timerClock2.setProgress(counter2);
        }
    };

    @Override ////////////////////////////////////////////////////////////////////////////////////// On create method
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alternate);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); ////////////////////// keep screen on
        //////////////////////////////////////////////////////////////////////////////////////////// set action bar title
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Alternate Mode");
        }
        connect(); ///////////////////////////////////////////////////////////////////////////////// connect to mqtt
        //////////////////////////////////////////////////////////////////////////////////////////// check for values from settings
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            int pt = bundle.getInt("prep_time");
            int mt = bundle.getInt("main_time");
            if (pt != 0 && mt != 0) {
                preparationTime = pt;
                mainTime = mt;
                counter1 = preparationTime;
                counter2 = preparationTime;
            } else if(pt == 0 && mt != 0) {
                preparationTime = pt;
                mainTime = mt;
                counter1 = preparationTime;
                counter2 = preparationTime;
                FLAG1 = false;
                FLAG2 = false;
                handler1.sendEmptyMessage(0);
                handler2.sendEmptyMessage(0);
            }
        }
        //////////////////////////////////////////////////////////////////////////////////////////// in initializing the views
        timerClock1 = findViewById(R.id.timer_clock_1);
        timerClock2 = findViewById(R.id.timer_clock_2);
        timerText1 = findViewById(R.id.timer_text1);
        timerText2 = findViewById(R.id.timer_text2);
        labelText = findViewById(R.id.label);
        settingsButton = findViewById(R.id.settings_button_alternate);

        initTimer(); /////////////////////////////////////////////////////////////////////////////// initialize the timer
        timer1(); ////////////////////////////////////////////////////////////////////////////////// start timer1
        timer2(); ////////////////////////////////////////////////////////////////////////////////// start timer2
    }

    public void onClickTimer(View view) { ////////////////////////////////////////////////////////// alternate button press
        //////////////////////////////////////////////////////////////////////////////////////////// alter the clock selection flag values
        FLAG1 = !FLAG1;
        FLAG2 = !FLAG2;
        resetMode = false; ///////////////////////////////////////////////////////////////////////// change the reset flag
        findViewById(R.id.clock1).setAlpha(1); ///////////////////////////////////////////////////// make bth clocks visible
        findViewById(R.id.clock2).setAlpha(1);
        settingsButton.setClickable(false);
        settingsButton.setAlpha(0.5f);
        menu.getItem(1).setEnabled(false); /////////////////////////////////////////////////////// hide option to change activity
        menu.getItem(1).setIcon(R.drawable.timer_dim);
        stupidFlag = true;
    }

    public void onClickReset(View view) { ////////////////////////////////////////////////////////// when reset button is pressed
        initTimer(); /////////////////////////////////////////////////////////////////////////////// init timer
        FLAG1 = false; ///////////////////////////////////////////////////////////////////////////// init selection flags
        FLAG2 = false;
        counter1 = preparationTime; //////////////////////////////////////////////////////////////// init clocks
        counter2 = preparationTime;
        phaseEnded1 = false; /////////////////////////////////////////////////////////////////////// init clock flags
        phaseEnded2 = false;
        handler1.sendEmptyMessage(0); ///////////////////////////////////////////////////////// update the interface
        handler2.sendEmptyMessage(0);

        resetMode = true; ////////////////////////////////////////////////////////////////////////// update reset flag
        findViewById(R.id.clock1).setAlpha(1);
        findViewById(R.id.clock2).setAlpha(0.5f);
        settingsButton.setClickable(true); ///////////////////////////////////////////////////////// enable settings button
        settingsButton.setAlpha(1);
        menu.getItem(1).setEnabled(true); //////////////////////////////////////////////////////// enable menu options
        menu.getItem(1).setIcon(R.drawable.timer);
        stupidFlag = false;
    }

    public void onClickBuzzer(View view) { ///////////////////////////////////////////////////////// buzzer button press
        Toast.makeText(getApplicationContext(), "BEEP", Toast.LENGTH_SHORT).show();
    }

    public void onClickSettings(View view) { /////////////////////////////////////////////////////// go to settings activity
        Intent intent = new Intent(AlternateActivity.this, SettingsActivity.class);
        intent.putExtra("key", 2);
        startActivity(intent);
        finish(); ////////////////////////////////////////////////////////////////////////////////// finish current activity
    }

    public void clockOneClicked(View view) { /////////////////////////////////////////////////////// player selection
        if (resetMode) {
            if (selection) {
                findViewById(R.id.clock1).setAlpha(0.5f);
                findViewById(R.id.clock2).setAlpha(1);
            } else {
                findViewById(R.id.clock2).setAlpha(0.5f);
                findViewById(R.id.clock1).setAlpha(1);
            }
            selection = !selection;
        }
    }

    public void clockTwoClicked(View view) { /////////////////////////////////////////////////////// player selection
        if (resetMode) {
            if (selection) {
                findViewById(R.id.clock1).setAlpha(0.5f);
                findViewById(R.id.clock2).setAlpha(1);
            } else {
                findViewById(R.id.clock2).setAlpha(0.5f);
                findViewById(R.id.clock1).setAlpha(1);
            }
            selection = !selection;
        }
    }

    private void timer1() { //////////////////////////////////////////////////////////////////////// timer for player1
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (notDestroyed) { ///////////////////////////////////////////////////////////// stop clock when activity destroyed
                    if (FLAG1 && stupidFlag) {
                        startTime1 = System.currentTimeMillis();
                        long futureTime = startTime1 + delay1;
                        while (futureTime > System.currentTimeMillis()) ;
                        if (FLAG1 && counter1 != 0) {
                            counter1 = counter1 - 1;
                            handler1.sendEmptyMessage(0); ///////////////////////////////////// update clock
                        }
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void timer2() { //////////////////////////////////////////////////////////////////////// timer for player2
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (notDestroyed) { ///////////////////////////////////////////////////////////// infinite loop
                    if (FLAG2 && stupidFlag) {
                        startTime2 = System.currentTimeMillis();
                        long futureTime = startTime2 + delay2;
                        while (futureTime > System.currentTimeMillis()) ;
                        if (FLAG2 && counter2 != 0) {
                            counter2 = counter2 - 1;
                            handler2.sendEmptyMessage(0); ///////////////////////////////////// update the clock
                        }
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    @SuppressLint("SetTextI18n")
    private void initTimer() { ///////////////////////////////////////////////////////////////////// initialize timers
        labelText.setText("Preparation Time"); ///////////////////////////////////////////////////// init label
        //////////////////////////////////////////////////////////////////////////////////////////// init interface for clock1
        timerClock1.setMax(preparationTime);
        timerClock1.setProgress(counter1);
        timerText1.setText(String.format(Locale.US, "%ds", counter1));
        timerClock1.setCircleProgressColor(getResources().getColor(R.color.preparationTime));
        timerClock1.setPointerHaloColor(getResources().getColor(R.color.preparationTime));
        timerClock1.setPointerColor(getResources().getColor(R.color.preparationTime));
        //////////////////////////////////////////////////////////////////////////////////////////// init interface for clock2
        timerClock2.setMax(preparationTime);
        timerClock2.setProgress(counter2);
        timerText2.setText(String.format(Locale.US, "%ds", counter2));
        timerClock2.setCircleProgressColor(getResources().getColor(R.color.preparationTime));
        timerClock2.setPointerHaloColor(getResources().getColor(R.color.preparationTime));
        timerClock2.setPointerColor(getResources().getColor(R.color.preparationTime));
    }

    void connect() { /////////////////////////////////////////////////////////////////////////////// connect to mqtt
        String clientId = MqttClient.generateClientId(); /////////////////////////////////////////// generate a random id
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://192.168.1.102:1883", clientId); // ip of broker

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    menu.getItem(0).setIcon(R.drawable.signal); ////////////////////////////////// update the signal strength icon
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    menu.getItem(0).setIcon(R.drawable.signal_off); ////////////////////////////// update the signal strength icon
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    void sendValue1() { //////////////////////////////////////////////////////////////////////////// send clock1 value
        String topic = "value1"; /////////////////////////////////////////////////////////////////// topic value1
        String payload = String.format(Locale.US, "%d", counter1); ////////////////////////// clock1 value
        byte[] encodedPayload;
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    void sendValue2() { //////////////////////////////////////////////////////////////////////////// send clock2 value
        String topic = "value2"; /////////////////////////////////////////////////////////////////// topic value2
        String payload = String.format(Locale.US, "%d", counter2);
        byte[] encodedPayload;
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    void sendColorCode(String color) { ///////////////////////////////////////////////////////////// send color info
        String topic = "color"; //////////////////////////////////////////////////////////////////// topic color
        byte[] encodedPayload;
        try {
            encodedPayload = color.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //////////////////////////////////////////////// load the menu options
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_alternate, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { ////////////////////////////////////////// handle menu option clicks
        switch (item.getItemId()) {
            case R.id.alternate:
                Intent intent1 = new Intent(AlternateActivity.this, MainActivity.class);
                startActivity(intent1);
                finish();
                return true;
            case R.id.about:
                Intent intent2 = new Intent(AlternateActivity.this, AboutActivity.class);
                startActivity(intent2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() { /////////////////////////////////////////////////////////////////// when activity destroyed
        super.onDestroy();
        notDestroyed = false; ////////////////////////////////////////////////////////////////////// update the flag

        try {
            IMqttToken disconnectToken = client.disconnect();
            disconnectToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
