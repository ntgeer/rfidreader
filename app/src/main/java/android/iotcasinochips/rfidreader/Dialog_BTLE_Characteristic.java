package android.iotcasinochips.rfidreader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import java.lang.Integer;

/**
 * This class is for a description of the characteristic received from the device.
 * This class also controls the dialogue for writing to a certain characteristic.
 * Click on a characteristic, and type the value to write, and click send.
 *
 * PAUL --> Reference "onClick" in this file for a write + send that works
 * */
public class Dialog_BTLE_Characteristic extends DialogFragment implements DialogInterface.OnClickListener {

    private String title;
    private Service_BTLE_GATT service;
    private BluetoothGattCharacteristic characteristic;
    Context context;

    // When the dialogue window is created (not very important)
    @SuppressLint("NewApi")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        context = this.getContext();

        builder.setView(inflater.inflate(R.layout.dialog_btle_characteristic, null))
                .setNegativeButton("Cancel", this).setPositiveButton("Send", this);
        builder.setTitle(title);

        return builder.create();
    }

    // Convert value to transmittable byte
    public byte[] initializeByte(String n){
        byte[] b = {(byte) Integer.parseInt(n)};
        return b;
    }

    // When clicking "okay" to send, or "cancel" to not send
    @Override
    public void onClick(DialogInterface dialog, int which) {

        EditText edit = (EditText) ((AlertDialog) dialog).findViewById(R.id.et_submit);

        switch (which) {
            case -2:
                // Do not send if cancel button pressed
                break;
            case -1:
                // Send if okay button pressed
                if (service != null) {
                    Utils.toast(context, "Sending...");

                    // Set value of write from what was typed
                    characteristic.setValue(initializeByte(edit.getText().toString()));

                    // Send write to reader
                    service.writeCharacteristic(characteristic);
                }
                break;
            default:
                break;
        }
    }

    // Set title of dialogue
    public void setTitle(String title) {
        this.title = title;
    }

    // Set service of dialogue
    public void setService(Service_BTLE_GATT service) {
        this.service = service;
    }

    // // Set characteristic of dialogue
    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }
}
