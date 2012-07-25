/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.pv.Status;

/**
 * @author mrk
 *
 */
public interface EasyMultiPut {
    void destroy();
    boolean connect();
    void issueConnect();
    boolean waitConnect();
    void get();
    void put();
    Alarm getAlarm();
    TimeStamp getTimeStamp();
    
    boolean[] isConnectedArray();
    boolean[] getBooleanArray();
    byte[] getByteArray();
    short[] getShortArray();
    int[] getIntArray();
    long[] getLongArray();
    float[] getFloatArray();
    double[] getDoubleArray();
    String[] getStringArray();
    
    void isConnectedArray(boolean[]data,int length);
    void getBooleanArray(boolean[]data,int length);
    void getByteArray(byte[]data,int length);
    void getShortArray(short[]data,int length);
    void getIntArray(int[]data,int length);
    void getLongArray(long[]data,int length);
    void getFloatArray(float[]data,int length);
    void getDoubleArray( double[]data,int length);
    void getStringArray(String[]data,int length);
    
    void putBooleanArray(boolean[] value,int length);
    void putByteArray(byte[] value,int lengt);
    void putShortArray(short[] value,int lengt);
    void putIntArray(int[] value,int lengt);
    void putLongArray(long[] value,int lengt);
    void putFloatArray(float[] value,int lengt);
    void putDoubleArray(double[] value,int lengt);
    void putStringArray(String[] value,int lengt);
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
