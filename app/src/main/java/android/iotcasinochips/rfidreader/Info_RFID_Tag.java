package android.iotcasinochips.rfidreader;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

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
            tagToDisplayInformation = new ArrayList<>();
        }

        tagToDisplayInformation.add("RFID EPC: " + Utils.hexToString(tagToDisplay.RFID_EPC));
        tagToDisplayInformation.add("RSSI: " + String.format("%f", tagToDisplay.magAntHop));
        tagToDisplayInformation.add("Range: " + String.format("%f", tagToDisplay.pdoaRangeMeters));
        tagToDisplayInformation.add("freqHopMHz: " + String.format("%f", tagToDisplay.freqHopMHz));
        tagToDisplayInformation.add("phaseAntHop: " + String.format("%f", tagToDisplay.phaseAntHop));
        tagToDisplayInformation.add("magCalHop: " + String.format("%f", tagToDisplay.magCalHop));
        tagToDisplayInformation.add("phaseCalHop: " + String.format("%f", tagToDisplay.phaseCalHop));
        tagToDisplayInformation.add("nonceHop: " + String.format("%b", tagToDisplay.nonceHop));
        tagToDisplayInformation.add("freqSkipMhz: " + String.format("%f", tagToDisplay.freqSkipMHz));
        tagToDisplayInformation.add("magAntSkip: " + String.format("%f", tagToDisplay.magAntSkip));
        tagToDisplayInformation.add("phaseAntSkip: " + String.format("%f", tagToDisplay.phaseAntSkip));
        tagToDisplayInformation.add("magCalSkip: " + String.format("%f", tagToDisplay.magCalSkip));
        tagToDisplayInformation.add("phaseCalSkip: " + String.format("%f", tagToDisplay.phaseCalSkip));
        tagToDisplayInformation.add("nonceSkip: " + String.format("%b", tagToDisplay.nonceSkip));

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
