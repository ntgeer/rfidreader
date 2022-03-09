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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class RFID_Taglist extends AppCompatActivity {
    public static final String TAG_BUNDLE = "android.iotcasinochips.rfidreader.RFID_Taglist.BUNDLE";
    public static final String TAG_ARRAY_LIST = "android.iotcasinochips.rfidreader.RFID_Taglist.ARRAYLIST"; // Added to intent?
    private ListView taglist;
    private ArrayAdapter<String> taglistAdapter;

    private ArrayList<RFID_Tag> surferTagArrayList;
    private ArrayList<String> surferTagStringEPCs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView(R.layout.rfid_taglist);

        // Need to grab the ArrayList from our intent
        Bundle args = getIntent().getBundleExtra(TAG_BUNDLE);
        if( surferTagArrayList != null ) {
            surferTagArrayList.clear();
        }
        if( args != null ) {
            surferTagArrayList = (ArrayList<RFID_Tag>)args.getSerializable(TAG_ARRAY_LIST);
        }

        // Make the Surfer Tag String EPC ArrayList
        if( surferTagStringEPCs == null ) {
            surferTagStringEPCs = new ArrayList<>();
        }
        else {
            surferTagStringEPCs.clear();
        }
        for( RFID_Tag element : surferTagArrayList ) {
            String str = Utils.hexToString(element.RFID_EPC);
            surferTagStringEPCs.add(str);
        }

        // Setup a Basic Listview
        taglistAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item1, surferTagStringEPCs);
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
