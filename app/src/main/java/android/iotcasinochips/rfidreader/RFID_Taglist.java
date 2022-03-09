package android.iotcasinochips.rfidreader;

import android.bluetooth.BluetoothDevice;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RFID_Taglist extends AppCompatActivity {
    public static final String TAG_BUNDLE = "android.iotcasinochips.rfidreader.RFID_Taglist.BUNDLE";
    public static final String TAG_ARRAY_LIST = "android.iotcasinochips.rfidreader.RFID_Taglist.ARRAYLIST"; // Added to intent?
    private ListView taglist;
    private ArrayAdapter<String> taglistAdapter;

    private Activity_BTLE_Services services;

    private ArrayList<RFID_Tag> surferTagArrayList;
    private HashMap<String, RFID_Tag> surferTagStringEPCs;
    private ArrayList<String> surferTagStringArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView(R.layout.rfid_taglist);

        Context context = this;

        // Declare storage lists for tags
        surferTagStringArray = new ArrayList<>();

        Button btnClear = (Button) findViewById(R.id.btnClear);

        // Disconnect from reader and return to Scan page
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Buttons", "Clear Tags button pressed.");
                //services.surferTagArrayList.clear();
                surferTagArrayList.clear();
                surferTagStringEPCs.clear();
                surferTagStringArray.clear();
                taglistAdapter = new ArrayAdapter<String>(context, R.layout.simple_list_item1, surferTagStringArray);
                taglist = (ListView) findViewById(R.id.taglist);
                taglist.setAdapter(taglistAdapter);
            }
        });




        // Need to grab the ArrayList from our intent
        Bundle args = getIntent().getBundleExtra(TAG_BUNDLE);
        if( surferTagArrayList != null ) {
            surferTagArrayList.clear();
        }
        if( args != null ) {
            surferTagArrayList = (ArrayList<RFID_Tag>)args.getSerializable(TAG_ARRAY_LIST);
        }

        // Make the Surfer Tag String EPC HashMap
        if( surferTagStringEPCs == null ) {
            surferTagStringEPCs = new HashMap<>();
        }
        else {
            surferTagStringEPCs.clear();
        }

        int i = 0;
        // Add all tags
        for( RFID_Tag element : surferTagArrayList ) {

            // Get tags' EPC address
            String epcTag = Utils.hexToString(element.RFID_EPC);

            // Add tag to list if not on the list
            // Compare existing EPC tags on list to EPC tag of this tag
            if (!surferTagStringEPCs.containsKey(epcTag)) {
                // Add tag to hash map and array list
                surferTagStringEPCs.put(epcTag, element);

                // Tag formatting
                surferTagStringArray.add("Tag "+ String.valueOf(i) + ": " + epcTag.toLowerCase().replaceAll("\\s", ""));
            }
            // If list already has tag, do not add it again
            // Update the tag RSSI instead
            /*else {
                Objects.requireNonNull(surferTagStringEPCs.get(epcTag)).magAntHop = element.magAntHop;
            }*/

            // Count tags
            i++;

        }





        // Setup a Basic Listview
        taglistAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item1, surferTagStringArray);
        taglist = (ListView) findViewById(R.id.taglist);
        taglist.setAdapter(taglistAdapter);
        taglist.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                        Intent intent3 = new Intent(view.getContext(), Info_RFID_Tag.class);
                        //Bundle args = new Bundle();
                        //args.putSerializable(Info_RFID_Tag.TAG_INFO, surferTagArrayList.get(position));
                        intent3.putExtra(Info_RFID_Tag.TAG_INFO, surferTagArrayList.get(position));
                        startActivity(intent3);
                    }
                }
        );
        // Need to set listener with following
        //taglistExpandable.setOnChildClickListener(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Instantiate GATT server handler
        services = new Activity_BTLE_Services();
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
