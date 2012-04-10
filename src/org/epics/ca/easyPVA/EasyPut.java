/**
 * 
 */
package org.epics.ca.easyPVA;

import org.epics.pvData.pv.*;
import org.epics.pvData.property.*;
import org.epics.pvData.misc.*;

/**
 * @author mrk
 *
 */
public interface EasyPut {
    void destroy();
    boolean connect();
    void issueConnect();
    boolean waitConnect();
    boolean get();
    void issueGet();
    boolean waitGet();
    boolean put();
    void issuePut();
    boolean waitPut();

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
    
    boolean putBoolean(boolean value);
    boolean putByte(byte value);
    boolean putShort(short value);
    boolean putInt(int value);
    boolean putLong(long value);
    boolean putFloat(float value);
    boolean putDouble(double value);
    boolean putString(String value);
    
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
    
    int putBooleanArray(boolean[] value,int length);
    int putByteArray(byte[] value,int length);
    int putShortArray(short[] value,int length);
    int putIntArray(int[] value,int length);
    int putLongArray(long[] value,int length);
    int putFloatArray(float[] value,int length);
    int putDoubleArray(double[] value,int length);
    int putStringArray(String[] value,int length);

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
