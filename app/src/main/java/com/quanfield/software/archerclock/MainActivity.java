package com.quanfield.software.archerclock;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
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

public class MainActivity extends AppCompatActivity {

    private static int preparationTime = 10, mainTime = 80;
    long delay = 1000;
    long startTime = 0;
    boolean resetMode = true;
    private CircularSeekBar timerClock;
    private TextView timerText, labelText;
    private Button settingsButton;
    private Menu menu;
    private MqttAndroidClient client;
    private boolean FLAG = false;
    private boolean phaseEnded = false, notDestroyed = true;
    private int counter = preparationTime;
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            if (counter == 0 && !phaseEnded) {
                counter = mainTime;
                timerClock.setMax(mainTime);

                labelText.setText("Main Time");

                sendColorCode("green");

                timerClock.setCircleProgressColor(getResources().getColor(R.color.mainTime));
                timerClock.setPointerHaloColor(getResources().getColor(R.color.mainTime));
                timerClock.setPointerColor(getResources().getColor(R.color.mainTime));

                phaseEnded = true;
            }

            if(counter == 30) {
                sendColorCode("yellow");

                timerClock.setCircleProgressColor(getResources().getColor(R.color.criticalTime));
                timerClock.setPointerHaloColor(getResources().getColor(R.color.criticalTime));
                timerClock.setPointerColor(getResources().getColor(R.color.criticalTime));
            }

            sendValue();

            timerText.setText(String.format(Locale.US, "%ds", counter));
            timerClock.setProgress(counter);
        }
    };
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Normal Mode");
        }

        connect();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            int pt = bundle.getInt("prep_time");
            int mt = bundle.getInt("main_time");
            if (pt != 0 && mt != 0) {
                preparationTime = pt;
                mainTime = mt;
                counter = preparationTime;
            } else if(pt == 0 && mt != 0) {
                preparationTime = pt;
                mainTime = mt;
                counter = preparationTime;
                handler.sendEmptyMessage(0);
            }
        }

        timerClock = findViewById(R.id.timer_clock);
        timerText = findViewById(R.id.timer_text);
        labelText = findViewById(R.id.timer_label);
        settingsButton = findViewById(R.id.settings_button);

        initTimer();
        timer();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void timer() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (notDestroyed) {
                    if (FLAG) {
                        startTime = System.currentTimeMillis();
                        long futureTime = startTime + delay;
                        while (futureTime > System.currentTimeMillis()) ;
                        if (FLAG && counter != 0) {
                            counter = counter - 1;
                            handler.sendEmptyMessage(0);
                        }
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressLint("SetTextI18n")
    private void initTimer() {
        labelText.setText("Preparation Time");
        timerClock.setMax(preparationTime);
        timerClock.setProgress(counter);
        timerText.setText(String.format(Locale.US, "%ds", counter));
        timerClock.setCircleProgressColor(getResources().getColor(R.color.preparationTime));
        timerClock.setPointerHaloColor(getResources().getColor(R.color.preparationTime));
        timerClock.setPointerColor(getResources().getColor(R.color.preparationTime));
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void onClickReset(View view) {
        resetTimer();
    }

    public void onClickBuzzer(View view) {
        Toast.makeText(getApplicationContext(), "BEEP", Toast.LENGTH_SHORT).show();
    }

    public void onClickSettings(View view) {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        intent.putExtra("key", 1);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        this.menu = menu;
        return true;
    }

    public void onClickTimer(View view) {
        if(FLAG) {
            ImageView img= findViewById(R.id.timer_control);
            img.setImageResource(R.drawable.play);
        } else {
            ImageView img= findViewById(R.id.timer_control);
            img.setImageResource(R.drawable.pause);
        }
        FLAG = !FLAG;
        resetMode = false;
        settingsButton.setClickable(false);
        settingsButton.setAlpha(0.5f);
        menu.getItem(1).setEnabled(false);
        menu.getItem(1).setIcon(R.drawable.account_convert_dim);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void connect() {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://192.168.1.102:1883", clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    menu.getItem(0).setIcon(R.drawable.signal);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    menu.getItem(0).setIcon(R.drawable.signal_off);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void sendValue() {
        String topic = "value";
        String payload = String.format(Locale.US, "%d", counter);
        byte[] encodedPayload;
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void sendPlayerId(String playerName) {
        String topic = "player";
        byte[] encodedPayload;
        try {
            encodedPayload = playerName.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void sendColorCode(String color) {
        String topic = "color";
        byte[] encodedPayload;
        try {
            encodedPayload = color.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connection:
                connect();
                return true;
            case R.id.alternate:
                Intent intent1 = new Intent(MainActivity.this, AlternateActivity.class);
                startActivity(intent1);
                finish();
                return true;
            case R.id.about:
                Intent intent2 = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent2);
                return true;
            case R.id.player_a:
                sendPlayerId("A");
                return true;
            case R.id.player_b:
                sendPlayerId("B");
                return true;
            case R.id.player_c:
                sendPlayerId("C");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private  void resetTimer() {
        initTimer();
        FLAG = false;
        counter = preparationTime;
        phaseEnded = false;
        handler.sendEmptyMessage(0);

        ImageView img= findViewById(R.id.timer_control);
        img.setImageResource(R.drawable.play);

        resetMode = true;

        settingsButton.setClickable(true);
        settingsButton.setAlpha(1);
        menu.getItem(1).setEnabled(true);
        menu.getItem(1).setIcon(R.drawable.account_convert);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onDestroy() {
        super.onDestroy();
        notDestroyed = false;

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