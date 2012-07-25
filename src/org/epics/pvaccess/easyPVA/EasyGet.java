/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;

/**
 * @author mrk
 *
 */
public interface EasyGet {
    void destroy();
    boolean connect();
    void issueConnect();
    boolean waitConnect();
    boolean get();
    void issueGet();
    boolean waitGet();
    Alarm getAlarm();
    TimeStamp getTimeStamp();
    boolean hasValue();
    boolean isValueScalar();
    PVField getValue();
    PVScalar getScalarValue();
    PVArray getArrayValue();
    PVScalarArray getScalarArrayValue();
    
    boolean getBoolean();
    byte getByte();
    short getShort();
    int getInt();
    long getLong();
    float getFloat();
    double getDouble();
    String getString();
    
    boolean[] getBooleanArray();
    byte[] getByteArray();
    short[] getShortArray();
    int[] getIntArray();
    long[] getLongArray();
    float[] getFloatArray();
    double[] getDoubleArray();
    String[] getStringArray();
    
    int getBooleanArray(boolean[] value,int length);
    int getByteArray(byte[] value,int length);
    int getShortArray(short[] value,int length);
    int getIntArray(int[] value,int length);
    int getLongArray(long[] value,int length);
    int getFloatArray(float[] value,int length);
    int getDoubleArray(double[] value,int length);
    int getStringArray(String[] value,int length);
    PVStructure getPVStructure();
    BitSet getBitSet();
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
