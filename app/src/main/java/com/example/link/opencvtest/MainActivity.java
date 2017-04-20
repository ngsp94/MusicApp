package com.example.link.opencvtest;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import android.view.ViewGroup.LayoutParams;
import android.view.View;

import android.content.Intent;

import java.util.ArrayList;

class InstrumentData {

    public String instrumentType;
    public int sensitivity;
    public float[] orientation;

    public InstrumentData(String type, int sensitivity, float x, float y, float z) {
        this.instrumentType = type;
        this.sensitivity = sensitivity;
        this.orientation = new float[3];
        this.orientation[0] = x;
        this.orientation[1] = y;
        this.orientation[2] = z;
    }

    public InstrumentData(String type, int sensitivity, float[] orientation) {
        this.instrumentType = type;
        this.sensitivity = sensitivity;
        this.orientation = orientation;
    }

    public String toString() {
        return instrumentType + ", " + sensitivity + ", " + orientation[0];
    }
}

public class MainActivity extends AppCompatActivity {

    private LinearLayout viewList;

    private ArrayList<InstrumentData> instruments;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewList = (LinearLayout) findViewById(R.id.list);
        instruments = new ArrayList<InstrumentData>();
    }

    private LinearLayout createNewTextView(String text) {
        final LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        final LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setLayoutParams(lparams);

        final TextView textView = new TextView(this);
        textView.setLayoutParams(lparams);
        textView.setText(text);

        Button settingsBtn = new Button(this);
        settingsBtn.setLayoutParams(lparams);
        settingsBtn.setText("Settings");
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout box = (LinearLayout) v.getParent();
                int idx = viewList.indexOfChild(box);
                InstrumentData data = instruments.get(idx);

                Intent intent = new Intent(getBaseContext(), Settings.class);
                intent.putExtra("STATUS", "UPDATE");
                intent.putExtra("ID", idx);
                intent.putExtra("INSTRUMENT_CHOICE", data.instrumentType);
                intent.putExtra("SENSITIVITY_VALUE", data.sensitivity);
                intent.putExtra("ORIENTATION_X", data.orientation[0]);
                intent.putExtra("ORIENTATION_Y", data.orientation[1]);
                intent.putExtra("ORIENTATION_Z", data.orientation[2]);
                startActivityForResult(intent, 2);
            }
        });

        Button deleteBtn = new Button(this);
        deleteBtn.setLayoutParams(lparams);
        deleteBtn.setText("Delete");
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout box = (LinearLayout) v.getParent();
                int idx = viewList.indexOfChild(box);
                viewList.removeViewAt(idx);
                instruments.remove(idx);
            }
        });

        container.addView(textView);
        container.addView(settingsBtn);
        container.addView(deleteBtn);

        return container;
    }

    public void onNewInstrument(View v) {

        Intent intent = new Intent(this, Settings.class);
        intent.putExtra("STATUS", "NEW");
        intent.putExtra("ID", instruments.size());

//        intent.putExtra("NUM_DATA", instruments.size());
//        for(int i=0; i < instruments.size(); i++) {
//            intent.putExtra("ORIENTATION_" + i, instruments.get(i).orientation);
//        }
        startActivityForResult(intent, 1);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1: {
                if (resultCode == Activity.RESULT_OK) {
                    String instrumentChoice = data.getStringExtra("INSTRUMENT_CHOICE");
                    int sensitivity = data.getExtras().getInt("SENSITIVITY_VALUE");
                    float x = data.getExtras().getFloat("ORIENTATION_X");
                    float y = data.getExtras().getFloat("ORIENTATION_Y");
                    float z = data.getExtras().getFloat("ORIENTATION_Z");

                    InstrumentData newInstrument = new InstrumentData(instrumentChoice, sensitivity, x, y, z);
                    instruments.add(newInstrument);
                    viewList.addView(createNewTextView(instrumentChoice));
                }
            }
            break;
            case 2: {
                if (resultCode == Activity.RESULT_OK) {
                    int id = data.getExtras().getInt("ID");
                    String instrumentChoice = data.getStringExtra("INSTRUMENT_CHOICE");
                    int sensitivity = data.getExtras().getInt("SENSITIVITY_VALUE");
                    float x = data.getExtras().getFloat("ORIENTATION_X");
                    float y = data.getExtras().getFloat("ORIENTATION_Y");
                    float z = data.getExtras().getFloat("ORIENTATION_Z");

                    InstrumentData info = instruments.get(id);
                    info.instrumentType = instrumentChoice;
                    info.sensitivity = sensitivity;
                    info.orientation[0] = x;
                    info.orientation[1] = y;
                    info.orientation[2] = z;

                    LinearLayout box = (LinearLayout) viewList.getChildAt(id);
                    TextView textView = (TextView) box.getChildAt(0);
                    textView.setText(instrumentChoice);
                }
            }
            break;
        }
    }

    public void startPlaying(View v) {
        Intent startIntent = new Intent(this, Play.class);
        System.out.println("START");
        startIntent.putExtra("NUM_DATA", instruments.size());
        for(int i=0; i < instruments.size(); i++) {
            startIntent.putExtra("TYPE_" + i, instruments.get(i).instrumentType);
            startIntent.putExtra("SENSITIVITY_" + i, instruments.get(i).sensitivity);
            startIntent.putExtra("ORIENTATION_" + i, instruments.get(i).orientation);
        }
        System.out.println("SEND_START");
        startActivity(startIntent);
    }
}
