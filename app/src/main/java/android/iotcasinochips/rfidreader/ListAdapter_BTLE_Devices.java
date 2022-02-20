package android.iotcasinochips.rfidreader;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
* Class to expand list of RFID devices as they are discovered
*/
public class ListAdapter_BTLE_Devices extends ArrayAdapter<BTLE_Device> {

    Activity activity;
    int layoutResourceID;
    ArrayList<BTLE_Device> devices;

    public ListAdapter_BTLE_Devices(Activity activity, int resource, ArrayList<BTLE_Device> objects) {
        super(activity.getApplicationContext(), resource, objects);

        this.activity = activity;
        layoutResourceID = resource;
        devices = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater =
                    (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layoutResourceID, parent, false);
        }

        // Get device
        BTLE_Device device = devices.get(position);

        // Get name of device
        String name = device.getName();

        // Get RSSI of device
        int rssi = device.getRSSI();

        // Get MAC address of device
        String address = device.getAddress();

        // Set name in the TextView on the list
        TextView tv = null;

        // If name is null, set "No Name".
        // This shouldn't ever happen, as the RFID is programmed to have a name
        // If a "No Name" device appears, the filter is incorrect -> BUG
        tv = (TextView) convertView.findViewById(R.id.tv_name);
        if (name != null && name.length() > 0) {
            tv.setText(device.getName());
        }
        else {
            tv.setText("No Name");
        }

        // Set RSSI in the TextView on the list
        tv = (TextView) convertView.findViewById(R.id.tv_rssi);
        tv.setText("RSSI: " + Integer.toString(rssi));

        // Set MAC address in the TextView on the list
        tv = (TextView) convertView.findViewById(R.id.tv_macaddr);
        if (address != null && address.length() > 0) {
            tv.setText(device.getAddress());
        }
        else {
            tv.setText("No Address");
        }

        // Return view
        return convertView;
    }
}
