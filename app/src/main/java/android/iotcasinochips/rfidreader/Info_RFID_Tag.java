package android.iotcasinochips.rfidreader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Info_RFID_Tag extends AppCompatActivity {
    public static final String TAG_INFO = "android.iotcasinochips.rfidreader.Info_RFID_Tag.TAG_INFO";
    //public static final String TAG_BUNDLE = "android.iotcasinochips.rfidreader.Info_RFID_Tag.TAG_BUNDLE"; // Added to intent?

    private ListView tagInformation;
    private ArrayAdapter<String> tagToDisplayAdapter;
    private RFID_Tag tagToDisplay;
    private ArrayList<String> tagToDisplayInformation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView(R.layout.info_rfid_tag);

        //Bundle args = getIntent().getBundleExtra(TAG_BUNDLE);
        tagToDisplay = (RFID_Tag)getIntent().getSerializableExtra(TAG_INFO);

        if(tagToDisplayInformation == null) {
            tagToDisplayInformation = new ArrayList<>(14);
        }

        tagToDisplayInformation.set(0,"RFID EPC: " + Utils.hexToString(tagToDisplay.RFID_EPC));
        tagToDisplayInformation.set(1,"freqHopMHz: " + String.format("%f", tagToDisplay.freqHopMHz));
        tagToDisplayInformation.set(2,"magAntHop: " + String.format("%f", tagToDisplay.magAntHop));
        tagToDisplayInformation.set(3,"phaseAntHop: " + String.format("%f", tagToDisplay.phaseAntHop));
        tagToDisplayInformation.set(4,"magCalHop: " + String.format("%f", tagToDisplay.magCalHop));
        tagToDisplayInformation.set(5,"phaseCalHop: " + String.format("%f", tagToDisplay.phaseCalHop));
        tagToDisplayInformation.set(6,"nonceHop: " + String.format("%b", tagToDisplay.nonceHop));
        tagToDisplayInformation.set(7,"freqSkipMhz: " + String.format("%f", tagToDisplay.freqSkipMhz));
        tagToDisplayInformation.set(8,"magAntSkip: " + String.format("%f", tagToDisplay.magAntSkip));
        tagToDisplayInformation.set(9,"phaseAntSkip: " + String.format("%f", tagToDisplay.phaseAntSkip));
        tagToDisplayInformation.set(10,"magCalSkip: " + String.format("%f", tagToDisplay.magCalSkip));
        tagToDisplayInformation.set(11,"phaseCalSkip: " + String.format("%f", tagToDisplay.phaseCalSkip));
        //tagToDisplayInformation.set(12,"nonceSkip: " + String.format("%b", tagToDisplay.nonceSkip)); // Not Setup at all rn
        //tagToDisplayInformation.set(13,"pdoaRangeMeters: " + String.format("%f", tagToDisplay.pdoaRangeMeters)); // Not Setup at all rn

        tagToDisplayAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item1, tagToDisplayInformation);
        tagInformation = (ListView) findViewById(R.id.info_rfid_tag_list);
        tagInformation.setAdapter(tagToDisplayAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    // Closing app.
    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
