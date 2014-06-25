/**
 * 
 */
package org.epics.pvaccess.ntMultiChannel;

import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.pv.*;

/**
 * @author mrk
 *
 */
public interface NTMultiChannel {
    TimeStamp getTimeStamp();
    int getLength();
    void getAlarm(Alarm[]alarms,int length);
    void getTimeStamp(TimeStamp[]timeStamps,int length);
    PVArray getValue();
    PVField getElementValue(int index);
    boolean allValueScalar();
    boolean isValueScalar(int index);
    boolean isValueScalarArray(int index);
    
    PVStructure getNTMultiChannel();
    boolean putMultiChannel(PVStructure pvStructure);
 
    boolean[] getBooleanArray();
    byte[] getByteArray();
    short[] getShortArray();
    int[] getIntArray();
    long[] getLongArray();
    float[] getFloatArray();
    double[] getDoubleArray();
    String[] getStringArray();
    
    boolean getBooleanArray(boolean[]data,int length);
    boolean getByteArray(byte[]data,int length);
    boolean getShortArray(short[]data,int length);
    boolean getIntArray(int[]data,int length);
    boolean getLongArray(long[]data,int length);
    boolean getFloatArray(float[]data,int length);
    boolean getDoubleArray( double[]data,int length);
    boolean getStringArray(String[]data,int length);
    
    
    int getLength(int index);
    int getBooleanArray(int index,boolean[]data,int length);
    int getByteArray(int index,byte[]data,int length);
    int getShortArray(int index,short[]data,int length);
    int getIntArray(int index,int[]data,int length);
    int getLongArray(int index,long[]data,int length);
    int getFloatArray(int index,float[]data,int length);
    int getDoubleArray(int index, double[]data,int length);
    int getStringArray(int index,String[]data,int length);
    
}
