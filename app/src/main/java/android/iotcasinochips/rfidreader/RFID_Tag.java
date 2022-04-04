package android.iotcasinochips.rfidreader;

import android.util.Log;

import java.io.Serializable;
import java.lang.Math;

public class RFID_Tag implements Serializable {
    // Variables for Tag
    protected byte [] RFID_EPC;
    protected double freqHopMHz = 0;
    protected double magAntHop = 0;
    protected double phaseAntHop = 0;
    protected double magCalHop = 0;
    protected double phaseCalHop = 0;
    protected byte nonceHop = 0;
    protected double freqSkipMHz = 0;
    protected double magAntSkip = 0;
    protected double phaseAntSkip = 0;
    protected double magCalSkip = 0;
    protected double phaseCalSkip = 0;
    protected byte nonceSkip = 0;
    protected double pdoaRangeMeters = 0;
    protected double minRSSI = -100.0;

    // Intermediate Variables for Calculations
    byte m_frequencySlot;
    int m_antMagI;
    int m_antMagQ;
    byte m_dataIdOld;
    byte m_dataIdNew;
    boolean expectingSupplementData;

    // Constructor
    RFID_Tag( byte [] data ) {
        RFID_EPC = new byte[12];
        this.m_frequencySlot = 0;
        this.m_antMagI = 0;
        this.m_antMagQ = 0;
        this.m_dataIdOld = (byte)(0xff);
        this.m_dataIdNew = (byte)(0xff);
        this.tagDataProcessor( data );
    }

    void tagDataProcessor(byte [] data ) {
        // Note that a Data Packet 1 is 20 bytes, and Data Packet 2 is 16 bytes
        if(data.length == 20) {
            System.arraycopy(data, 0, this.RFID_EPC, 0, 12);
            this.expectingSupplementData = (data[12] & 128) != 0; //Supplement data indicator is contained in the msb of this byte
            this.m_frequencySlot = (byte) (data[12] & (byte) (31));
            this.m_antMagI = (int) (((int) (data[13]) << 24) + ((int) (data[14]) << 16) + ((int) (data[15]) << 8));
            this.m_antMagQ = (int) (((int) (data[16]) << 24) + ((int) (data[17]) << 16) + ((int) (data[18]) << 8));

            // These don't matter for the moment, we can update them later
            this.m_dataIdOld = m_dataIdNew;
            this.m_dataIdNew = data[19];

            if(m_dataIdNew != ((m_dataIdOld+1) % 256)){
                Log.i("SURFER_Error","Got data packets out of order. May be due to reader reset.");
            }

            if (!this.expectingSupplementData){
                saveTagWithEPC(this.m_frequencySlot, true, (byte)128, this.m_antMagI, this.m_antMagQ, 0, 0);
            }
        }
        else if( data.length == 16 ) { // For a DataPacket2 Call
            //Don't add LSB for now. Actually this turned out not to really matter so try to add in when possible.
            this.m_antMagI += (int)data[1];
            this.m_antMagQ += (int)data[2];
            //Next, get the calibration magI and magQ
            int calMagI = (int)(((int)data[4] << 24)+((int)data[5] << 16)+((int)data[6] << 8)+((int)data[7] << 0));
            int calMagQ = (int)(((int)data[8] << 24)+((int)data[9] << 16)+((int)data[10] << 8)+((int)data[11] << 0));
            boolean hopNotSkip = (data[12] == (byte)255);
            m_dataIdOld=m_dataIdNew;
            byte hopSkipNonce=data[14];
            m_dataIdNew=data[15]; //A nonce that we can use if we get packets out of order

            if(m_dataIdNew != ((m_dataIdOld+1) % 256)){
                Log.i("SURFER_Error","Got data packets out of order. May be due to reader reset.");
            }

            saveTagWithEPC(this.m_frequencySlot, hopNotSkip, hopSkipNonce, this.m_antMagI, this.m_antMagQ, calMagI, calMagQ);

            // Clear flag -- workaround from switch/case state machine from iOS code
            this.expectingSupplementData = false;
        }
        else {
            // This is an error where the number of bytes for the packet is wrong
        }
    }

    void saveTagWithEPC(byte freqSlot, boolean hopNotSkip, byte hopSkipNonce, int antMagI, int antMagQ, int calMagI, int calMagQ){
        double antRSSIdBm  = computeTagRSSIFromMagI(antMagI, antMagQ);
        double calRSSIdBm  = computeTagRSSIFromMagI(calMagI, calMagQ);
        double antPhaseDeg = computeTagPhaseFromMagI(antMagI, antMagQ);
        double calPhaseDeg = computeTagPhaseFromMagI(calMagI, calMagQ);
        double freqInMHz = computeFreqMHzFromSlot(freqSlot);

        // Converting Temporary Data into permanent
        if(hopNotSkip){
            this.freqHopMHz  =   freqInMHz;
            this.magAntHop   =   antRSSIdBm; //This is the data from which tag RSSI is reported.
            if (this.minRSSI < this.magAntHop) this.minRSSI = this.magAntHop;
            this.magCalHop   =   calRSSIdBm;
            this.phaseAntHop =   antPhaseDeg;
            this.phaseCalHop =   calPhaseDeg;
            this.nonceHop    =   hopSkipNonce;
            //Note that since hop must come first, we clear out the skip data from before
            //However, we don't clear out the computed PDOA range from before
            this.freqSkipMHz  =   0;
            this.magAntSkip   =   0; //This is the data from which tag RSSI is reported.
            this.magCalSkip   =   0;
            this.phaseAntSkip =   0;
            this.phaseCalSkip =   0;
            //Don't do anything with the nonce. Setting it to 0 may cause bugs.
        } else {
            this.freqSkipMHz  =   freqInMHz;
            this.magAntSkip   =   antRSSIdBm; //This is the data from which tag RSSI is reported.
            if (this.minRSSI < this.magAntSkip) this.minRSSI = this.magAntSkip;
            this.magCalSkip   =   calRSSIdBm;
            this.phaseAntSkip =   antPhaseDeg;
            this.phaseCalSkip =   calPhaseDeg;
            this.nonceSkip    =   hopSkipNonce;

            //Now we also compute PDOA range
            this.pdoaRangeMeters = computeTagPDOARange(this);
            Log.i("RangeCalculator","Tag is "+String.valueOf(this.pdoaRangeMeters)+" away.");
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

    protected double computeTagRSSIFromMagI(int magI, int magQ) {
        double PCEPC_ACK_BITS = (float)(128.0);
        double MILLER_M = (float)8.0;
        double DBE_OSR = (float)24.0;
        double RCVR_GAIN_DB = (float)131.5;

        double rssiInWatts = 0;
        double rssiIndBm = 0;
        double nChipsPerPacket = PCEPC_ACK_BITS * MILLER_M * DBE_OSR;
        double receiverPowerGain = 50.0* (64.0/(Math.pow(Math.PI, 4))) * Math.pow(10, RCVR_GAIN_DB/10);
        rssiInWatts = (1/receiverPowerGain)*(Math.pow(magI/nChipsPerPacket,2)+Math.pow(magQ/nChipsPerPacket,2));
        rssiIndBm = 10*Math.log10(rssiInWatts)+30;

        return rssiIndBm;
    }

    protected double computeTagPhaseFromMagI(int magI, int magQ) {
        return ((Math.atan(-(double)magQ/(double)magI)+Math.PI) % Math.PI);
    }

    protected double computeTagPDOARange(RFID_Tag tag) {
        int SPEED_LIGHT_VAC = 299792458;
        double ER_PCB = 4.2; //Will need to change to 4.2 for actual operation
        double ER_CAB = 2.0; //Need to check if this is PTFE or not. Same for cable and connector. Will need to change for actual operation (2.0).
        double ANT_PCB_ROUTE_M = 0.0095;
        double ANT_CAB_ROUTE_M = 0.2286;

        double ant_known_phase_hop = 0.0;
        double ant_known_phase_skip = 0.0;
        double cal_known_phase_hop = 0.0;
        double cal_known_phase_skip = 0.0;
        double corrected_phase_hop = 0.0;
        double corrected_phase_skip = 0.0;

        //First thing is we need to determine whether or not we can compute the range.
        //What are the cases in which we should fail (return a dummy value that can be tested for)?
        //1. Hop/skip nonce don't match
        //2. No skip data (skip data is nil'd out).
        //3. Frequency difference between hop and skip is less than 3.1MHz (otherwise it causes aliasing at range with a big patch antenna.

        if(tag.nonceHop != tag.nonceSkip){
            Log.i("Tag_Range", "Nonces don't match, so range won't be updated");
            return tag.pdoaRangeMeters; //If the nonces don't match, don't update the range.
            //In rare cases, there may be a bug in which the nonces wrap around but we imagine that will be rare enough to be acceptable.
        }

        if(tag.phaseAntHop == 0 || tag.phaseAntSkip == 0 || tag.phaseCalHop == 0 || tag.phaseCalSkip == 0
                || tag.freqHopMHz == 0 || tag.freqSkipMHz == 0 || tag.magCalHop < -70 || tag.magCalSkip < -70){
            Log.i("Tag_Range","Attempted to compute PDOA ranging for a tag with incomplete phase data");
            //Note that if the returned calibration RSSI is too low, phase data is likely also invalid.
            return 99.9;
        }

        if(Math.abs(tag.freqHopMHz - tag.freqSkipMHz) > 3.1){
            Log.i("Tag_Range","Attempted to compute PDOA ranging for a tag with too large of a hop/skip frequency delta");
            return 99.9;
        }

        if(Math.abs(tag.freqHopMHz - tag.freqSkipMHz) < 0.9){
            Log.i("Tag_Range","Attempted to compute PDOA ranging for a tag with too small of a hop/skip frequency delta");
            return 99.9;
        }

        //Second thing is we need to compute the phase of the signal on the PCB/cabling that is not part of a shared path.
        //In other words, we wish to compute the distance of the antenna to the tag, but both the antenna and calibration device
        //exist at finite and known distances from the RF receiver.

        //For the moment, we're going to try a calibration device being a tag that sits right outside of the antenna.
        //We'll leave the distance between the antenna and the tag as an error for the moment.

        //4 Pi is actually 2*2pi, the 2 coming from out and back phase changes.

        final double v = ANT_PCB_ROUTE_M / (SPEED_LIGHT_VAC / Math.sqrt(ER_PCB));
        final double v1 = ANT_CAB_ROUTE_M / (SPEED_LIGHT_VAC / Math.sqrt(ER_CAB));
        ant_known_phase_hop = cal_known_phase_hop = (4.0*Math.PI*tag.freqHopMHz*(1e6)*(v + v1)) % Math.PI;
        ant_known_phase_skip = cal_known_phase_skip = (4.0*Math.PI*tag.freqSkipMHz *(1e6)*(v + v1)) % Math.PI;

        //Third thing is that we need to compute the corrected hop frequency phase and the corrected skip frequency phase.
        //Because TX and RX phase in the receiver varies randomly with respect to each other, and because the SAW filter
        //phase varies all over the place, the calibration phase must be removed from the antenna phase

        corrected_phase_hop     =   ((tag.phaseAntHop - tag.phaseCalHop) - (ant_known_phase_hop - cal_known_phase_hop)+2*Math.PI) % Math.PI;
        corrected_phase_skip    =   ((tag.phaseAntSkip - tag.phaseCalSkip) - (ant_known_phase_skip - cal_known_phase_skip)+2*Math.PI) % Math.PI;

        //Fourth thing is to finally subtract the hop and skip phases from one another
        //Div by 4 is for out and back phase change, otherwise it would just be 2*pi.

        Log.i("Tag_Range","Success!" + " Hop: " + tag.freqHopMHz + " Skip: " +  tag.freqSkipMHz);

        float l4 = (float)(SPEED_LIGHT_VAC/4);

        if(tag.freqHopMHz > tag.freqSkipMHz){
            return (l4/Math.PI/((tag.freqHopMHz - tag.freqSkipMHz)*(1e6)))*((corrected_phase_hop-corrected_phase_skip+2*Math.PI)%Math.PI);
        } else {
            return (l4/Math.PI/((tag.freqSkipMHz - tag.freqHopMHz)*(1e6)))*((corrected_phase_skip-corrected_phase_hop+2*Math.PI)%Math.PI);
        }
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
