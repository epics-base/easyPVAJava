/**
 * 
 */
package org.epics.ca.easyPVA;

import org.epics.pvData.pv.*;
import org.epics.pvData.property.*;
import org.epics.pvData.property.*;
import org.epics.pvData.misc.*;

/**
 * @author mrk
 *
 */
public interface EasyMultiGet {
    void destroy();
    boolean connect();
    void issueConnect();
    boolean waitConnect();
    void get();
    Alarm getAlarm();
    TimeStamp getTimeStamp();
    int getLength();
    
    Alarm[] getAlarms();
    TimeStamp[] getTimeStamps();
    boolean[] isConnectedArray();
    boolean[] getBooleanArray();
    byte[] getByteArray();
    short[] getShortArray();
    int[] getIntArray();
    long[] getLongArray();
    float[] getFloatArray();
    double[] getDoubleArray();
    String[] getStringArray();
    
    void getAlarms(Alarm[]alarms,int length);
    void getTimeStamps(TimeStamp[]timeStamps,int length);
    void isConnectedArray(boolean[]data,int length);
    void getBooleanArray(boolean[]data,int length);
    void getByteArray(byte[]data,int length);
    void getShortArray(short[]data,int length);
    void getIntArray(int[]data,int length);
    void getLongArray(long[]data,int length);
    void getFloatArray(float[]data,int length);
    void getDoubleArray( double[]data,int length);
    void getStringArray(String[]data,int length);
    /**
     * Set a new status value. The new value will replace the current status. The initial status is statusOK.
     * @param status The new status.
     */
    void setStatus(Status status);
    /**
     * Get the latest status. Calling this resets the latest status to statusOK.
     * @return The status.
     */
    Status getStatus();

}
