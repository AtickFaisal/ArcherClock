package com.quanfield.software.archerclock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class SettingsActivity extends AppCompatActivity {

    private Spinner prepTime, mainTime;
    private int mainTimeValue = 80, preparationTimeValue = 10;
    private String playerName = "A";
    private int key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Archer Clock Settings");
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            key = bundle.getInt("key");
        }

        //////////////////////////////////////////////////////
        prepTime = findViewById(R.id.prep_time_spinner);
        mainTime = findViewById(R.id.main_time_spinner);
        Button saveButton = findViewById(R.id.save_button);
        //////////////////////////////////////////////////////

        ArrayAdapter<CharSequence> prepTimeAdapter = ArrayAdapter.createFromResource(this,
                R.array.prep_time_array, android.R.layout.simple_spinner_item);
        prepTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prepTime.setAdapter(prepTimeAdapter);
        prepTime.setOnItemSelectedListener(new SpinnerActivity());

        /////////////////////////////////////////////////////////////////////////////////////////

        ArrayAdapter<CharSequence> mainTimeAdapter = ArrayAdapter.createFromResource(this,
                R.array.main_time_array, android.R.layout.simple_spinner_item);
        mainTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mainTime.setAdapter(mainTimeAdapter);
        mainTime.setOnItemSelectedListener(new SpinnerActivity());

        /////////////////////////////////////////////////////////////////////////////////////////

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if(key == 1) {
                    intent = new Intent(SettingsActivity.this, MainActivity.class);
                } else {
                    intent = new Intent(SettingsActivity.this, AlternateActivity.class);
                }
                intent.putExtra("main_time", mainTimeValue);
                intent.putExtra("prep_time", preparationTimeValue);
                intent.putExtra("player_name", playerName);
                startActivity(intent);
                finish();
            }
        });
    }

    @SuppressLint("Registered")
    public class SpinnerActivity extends Activity implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view,
                                   int pos, long id) {
            if (parent == prepTime)
                preparationTimeValue = Integer.parseInt(parent.getItemAtPosition(pos).toString());
            else if (parent == mainTime)
                mainTimeValue = Integer.parseInt(parent.getItemAtPosition(pos).toString());
            else
                playerName = parent.getItemAtPosition(pos).toString();
        }

        public void onNothingSelected(AdapterView<?> parent) {
            //////////////////////////////////
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        return true;
    }
}