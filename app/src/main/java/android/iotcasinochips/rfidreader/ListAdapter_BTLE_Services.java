package android.iotcasinochips.rfidreader;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Services class - This class processes and displays received services
 *
 */
public class ListAdapter_BTLE_Services extends BaseExpandableListAdapter {

    private Activity activity;
    private ArrayList<BluetoothGattService> services_ArrayList;
    private HashMap<String, ArrayList<BluetoothGattCharacteristic>> characteristics_HashMap;

    public ListAdapter_BTLE_Services(Activity activity, ArrayList<BluetoothGattService> listDataHeader,
                                 HashMap<String, ArrayList<BluetoothGattCharacteristic>> listChildData) {

        this.activity = activity;
        this.services_ArrayList = listDataHeader;
        this.characteristics_HashMap = listChildData;
    }

    // Utility functions --------------------------------------------------------------------------

    @Override
    public int getGroupCount() {
        return services_ArrayList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return characteristics_HashMap.get(
                services_ArrayList.get(groupPosition).getUuid().toString()).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return services_ArrayList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {

        return characteristics_HashMap.get(
                services_ArrayList.get(groupPosition).getUuid().toString()).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    // --------------------------------------------------------------------------------------------

    // Processing and display function for group services. These are the services you see at the top
    // of the page that you can click on to expand and see more services
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        // Get group service
        BluetoothGattService bluetoothGattService = (BluetoothGattService) getGroup(groupPosition);

        // Get UUID of group service
        String serviceUUID = bluetoothGattService.getUuid().toString();
        if (convertView == null) {
            LayoutInflater inflater =
                    (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.btle_service_list_item, null);
        }

        // Display service group in a string lead by an "S"
        TextView tv_service = (TextView) convertView.findViewById(R.id.tv_service_uuid);
        tv_service.setText("S: " + serviceUUID);

        return convertView;
    }

    // Processing and display function for single services. These are the services you see after
    // expanding a group service
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        // Get characteristic
        BluetoothGattCharacteristic bluetoothGattCharacteristic = (BluetoothGattCharacteristic) getChild(groupPosition, childPosition);

        // Get UUID of characteristic
        String characteristicUUID =  bluetoothGattCharacteristic.getUuid().toString();
        if (convertView == null) {
            LayoutInflater inflater =
                    (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.btle_characteristics_list_item, null);
        }

        // Label service as characteristic in a string lead by "C: "
        TextView tv_service = (TextView) convertView.findViewById(R.id.tv_characteristic_uuid);
        tv_service.setText("C: " + characteristicUUID);

        // Get properties of characteristic
        int properties = bluetoothGattCharacteristic.getProperties();
        TextView tv_property = (TextView) convertView.findViewById(R.id.tv_properties);

        // Create a properties label string
        StringBuilder sb = new StringBuilder();

        // If characteristic has Read property, add "R" to the label
        if (Utils.hasReadProperty(properties) != 0) {
            sb.append("R");
        }

        // If characteristic has Write property, add "W" to the label
        if (Utils.hasWriteProperty(properties) != 0) {
            sb.append("W");
        }

        // If characteristic has Notify property, add "N" to the label
        if (Utils.hasNotifyProperty(properties) != 0) {
            sb.append("N");
        }

        // SURFER change: If characteristic has Indicate property, add "I" to the label
        if (Utils.hasIndicateProperty(properties) != 0) {
            sb.append("I");
        }

        // Display the properties label string
        tv_property.setText(sb.toString());

        TextView tv_value = (TextView) convertView.findViewById(R.id.tv_value);

        // Get the value of the characteristic and set it after "Value: "
        byte[] data = bluetoothGattCharacteristic.getValue();
        if (data != null) {
            tv_value.setText("Value: " + Utils.hexToString(data));
        }
        else {
            tv_value.setText("Value: ---");
        }

        return convertView;
    }

    // Seems like a useless setup function
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
