package android.iotcasinochips.rfidreader;

import java.io.Serializable;
import java.lang.Math;

public class RFID_Tag implements Serializable {
    // Variables for Tag
    protected byte [] RFID_EPC;
    protected float freqHopMHz;
    protected float magAntHop;
    protected float phaseAntHop;
    protected float magCalHop;
    protected float phaseCalHop;
    protected byte nonceHop;
    protected float freqSkipMhz;
    protected float magAntSkip;
    protected float phaseAntSkip;
    protected float magCalSkip;
    protected float phaseCalSkip;
    protected byte nonceSkip;
    protected float pdoaRangeMeters;

    // Intermediate Variables for Calculations
    byte m_frequencySlot;
    int m_antMagI;
    int m_antMagQ;
    byte m_dataIdOld;
    byte m_dataIdNew;

    // Constructor
    RFID_Tag( byte [] data ) {
        RFID_EPC = new byte[12];
        this.m_frequencySlot = 0;
        this.m_antMagI = 0;
        this.m_antMagQ = 0;
        this.m_dataIdOld = (byte)(0xff);
        this.m_dataIdNew = (byte)(0xff);
        this.update( data );
    }

    void update( byte [] data ) {
        // Note that a Data Packet 1 is 20 bytes, and Data Packet 2 is 16 bytes
        if(data.length == 20) {
            for (int i = 0; i < 12; i++) {
                this.RFID_EPC[i] = data[i];
            }
            this.m_frequencySlot = (byte) (data[12] & (byte) (31));
            this.m_antMagI = (int) (((int) (data[13]) << 24) + ((int) (data[14]) << 16) + ((int) (data[15]) << 8));
            this.m_antMagQ = (int) (((int) (data[16]) << 24) + ((int) (data[17]) << 16) + ((int) (data[18]) << 8));

            // These don't matter for the moment, we can update them later
            this.m_dataIdOld = m_dataIdNew;
            this.m_dataIdNew = data[19];

            // Converting Temporary Data into permanent
            if (true) { // HopNotSkip is something used for different Packets
                this.freqHopMHz = computeFreqMHzFromSlot(this.m_frequencySlot);
                this.magAntHop = computeTagRSSIFromMagI(this.m_antMagI, this.m_antMagQ);
                this.magCalHop = computeTagRSSIFromMagI(0, 0); // Cal Mag stuff that was set to 0 in the IOS code
                this.phaseAntHop = computeTagPhaseFromMagI(this.m_antMagI, this.m_antMagQ);
                this.phaseCalHop = computeTagPhaseFromMagI(0, 0); // Cal Mag stuff that was set to 0 in the IOS code
                this.nonceHop = (byte) 128; // This is a static value set in the IOS Code for the dataPacket1 call of saveTagWithEPC

                // These are used in a DataPacket2 call, but we're not going to be doing anything with them for now
                this.freqSkipMhz = 0;
                this.magAntSkip = 0;
                this.magCalSkip = 0;
                this.phaseAntSkip = 0;
                this.phaseCalSkip = 0;
            }
        }
        else if( data.length == 16 ) { // For a DataPacket2 Call
            // Not implemented for now, because need to prove functionality with Data Packet 1 first
        }
        else {
            // This is an error where the number of bytes for the packet is wrong
        }
    }

    boolean same( byte [] data ) {
        for( int i = 0; i < 12; i++ ) {
            if(data[i] != RFID_EPC[i]) {
                return false;
            }
        }
        return true;
    }

    protected float computeTagRSSIFromMagI(int magI, int magQ) {
        float PCEPC_ACK_BITS = (float)(128.0);
        float MILLER_M = (float)8.0;
        float DBE_OSR = (float)24.0;
        float RCVR_GAIN_DB = (float)131.5;

        float rssiInWatts = 0;
        float rssiIndBm = 0;
        float nChipsPerPacket = PCEPC_ACK_BITS * MILLER_M * DBE_OSR;
        float receiverPowerGain = (float)(50.0)*( (float)(64.0) /( (float)(Math.pow(Math.PI, 4)) ) * (float)(Math.pow(10, RCVR_GAIN_DB/10 )) );
        rssiInWatts = (float)( 1/receiverPowerGain ) * ((float)(Math.pow(magI/nChipsPerPacket,2)) + (float)(Math.pow(magQ/nChipsPerPacket, 2)) );
        rssiIndBm = 10*(float)(Math.log10(rssiInWatts))+30;

        return rssiIndBm;
    }

    protected float computeTagPhaseFromMagI(int magI, int magQ) {
        return (float)( (float)(Math.atan(-(double)(magQ)/(double)(magI))+Math.PI) % (float)(Math.PI));
    }

    protected float computeFreqMHzFromSlot( byte slot ) {
        float freqMHz = (float)0.0;

        switch (slot){
            case 0:     freqMHz = (float)903.0; break;
            case 1:     freqMHz = (float)904.0; break;
            case 2:     freqMHz = (float)905.0; break;
            case 3:     freqMHz = (float)906.0; break;
            case 4:     freqMHz = (float)907.0; break;
            case 5:     freqMHz = (float)908.0; break;
            case 6:     freqMHz = (float)909.0; break;
            case 7:     freqMHz = (float)910.0; break;
            case 8:     freqMHz = (float)911.0; break;
            case 9:     freqMHz = (float)912.0; break;
            case 10:    freqMHz = (float)913.0; break;
            case 11:    freqMHz = (float)914.0; break;
            case 12:    freqMHz = (float)915.0; break;
            case 13:    freqMHz = (float)916.0; break;
            case 14:    freqMHz = (float)917.0; break;
            case 15:    freqMHz = (float)918.0; break;
            case 16:    freqMHz = (float)919.0; break;
            case 17:    freqMHz = (float)920.0; break;
            case 18:    freqMHz = (float)921.0; break;
            case 19:    freqMHz = (float)922.0; break;
            case 20:    freqMHz = (float)923.0; break;
            case 21:    freqMHz = (float)924.0; break;
            case 22:    freqMHz = (float)925.0; break;
            case 23:    freqMHz = (float)926.0; break;
            case 24:    freqMHz = (float)927.0; break;
            default:    freqMHz = (float)915.0; break;
        }

        return freqMHz;
    }
}
