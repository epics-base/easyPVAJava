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
    /**
     * clean up resources used.
     */
    void destroy();
    /**
     * call issueConnect and then waitConnect.
     * @return the result from waitConnect.
     */
    boolean connect();
    /**
     * create the channelGet connection to the channel.
     * This can only be called once.
     */
    void issueConnect();
    /**
     * wait until the channelGet connection to the channel is complete.
     * @return (false,true) if the channelGet (was not, was) created.
     * If false is returned getStatus will provide the reason why the connection request failed, 
     */
    boolean waitConnect();
    /**
     * Call issueGet and then waitGet.
     * @return
     */
    boolean get();
    /**
     * 
     */
    void issueGet();
    /**
     * @return
     */
    boolean waitGet();
    /**
     * @return
     */
    Alarm getAlarm();
    /**
     * @return
     */
    TimeStamp getTimeStamp();
    /**
     * @return
     */
    boolean hasValue();
    /**
     * @return
     */
    boolean isValueScalar();
    /**
     * @return
     */
    PVField getValue();
    /**
     * @return
     */
    PVScalar getScalarValue();
    /**
     * @return
     */
    PVArray getArrayValue();
    /**
     * @return
     */
    PVScalarArray getScalarArrayValue();
    
    
    /**
     * @return
     */
    boolean getBoolean();
    /**
     * @return
     */
    byte getByte();
    /**
     * @return
     */
    short getShort();
    /**
     * @return
     */
    int getInt();
    /**
     * @return
     */
    long getLong();
    /**
     * @return
     */
    float getFloat();
    /**
     * @return
     */
    double getDouble();
    /**
     * @return
     */
    String getString();
    
    /**
     * @return
     */
    boolean[] getBooleanArray();
    /**
     * @return
     */
    byte[] getByteArray();
    /**
     * @return
     */
    short[] getShortArray();
    /**
     * @return
     */
    int[] getIntArray();
    /**
     * @return
     */
    long[] getLongArray();
    /**
     * @return
     */
    float[] getFloatArray();
    /**
     * @return
     */
    double[] getDoubleArray();
    /**
     * @return
     */
    String[] getStringArray();
    
    /**
     * @param value
     * @param length
     * @return
     */
    int getBooleanArray(boolean[] value,int length);
    /**
     * @param value
     * @param length
     * @return
     */
    int getByteArray(byte[] value,int length);
    /**
     * @param value
     * @param length
     * @return
     */
    int getShortArray(short[] value,int length);
    /**
     * @param value
     * @param length
     * @return
     */
    int getIntArray(int[] value,int length);
    /**
     * @param value
     * @param length
     * @return
     */
    int getLongArray(long[] value,int length);
    /**
     * @param value
     * @param length
     * @return
     */
    int getFloatArray(float[] value,int length);
    /**
     * @param value
     * @param length
     * @return
     */
    int getDoubleArray(double[] value,int length);
    /**
     * @param value
     * @param length
     * @return
     */
    int getStringArray(String[] value,int length);
    /**
     * @return
     */
    PVStructure getPVStructure();
    /**
     * @return
     */
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
