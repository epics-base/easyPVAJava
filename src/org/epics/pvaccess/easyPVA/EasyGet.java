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
     * If failure getStatus can be called to get reason.
     * @return (false,true) means (failure,success)
     */
    boolean waitConnect();
    /**
     * Call issueGet and then waitGet.
     * @return (false,true) means (failure,success)
     */
    boolean get();
    /**
     * Issue a get and return immediately.
     */
    void issueGet();
    /**
     * Wait until get completes.
     * If failure getStatus can be called to get reason.
     * @return (false,true) means (failure,success)
     */
    boolean waitGet();
    /**
     * Get the alarm for the last get.
     * @return The alarm.
     */
    Alarm getAlarm();
    /**
     * Get the timeStamp for the last get.
     * @return The timeStamp.
     */
    TimeStamp getTimeStamp();
    /**
     * Is there a top level field named value of the PVstructure returned by channelGet?
     * @return The answer.
     */
    boolean hasValue();
    /**
     * Is the value field a scalar?
     * @return The answer.
     */
    boolean isValueScalar();
    /**
     * Return the interface to the value field.
     * @return The interface or null if no top level value field.
     */
    PVField getValue();
    /**
     * Return the interface to a scalar value field.
     * @return Return the interface for a scalar value field or null if no scalar value field.
     */
    PVScalar getScalarValue();
    /**
     * Return the interface to an array value field.
     * @return Return the interface or null if an array value field does not exist.
     */
    PVArray getArrayValue();
    /**
     * Return the interface to a scalar array value field.
     * @return Return the interface or null if a scalar array value field does not exist
     */
    PVScalarArray getScalarArrayValue();
    
    
    /**
     * Get the boolean value. If value is not a boolean setStatus is called and false is returned. 
     * @return true or false.
     */
    boolean getBoolean();
    /**
     * Get the value as a byte.
     * @return If value is not a numeric scalar setStatus is called and 0 is returned
     */
    byte getByte();
    /**
     * Get the value as a short.
     * @return  If value is not a numeric scalar setStatus is called and 0 is returned.
     */
    short getShort();
    /**
     * Get the value as an int.
     * @return  If value is not a numeric scalar setStatus is called and 0 is returned.
     */
    int getInt();
    /**
     * Get the value as a long.
     * @return  If value is not a numeric scalar setStatus is called and 0 is returned.
     */
    long getLong();
    /**
     * Get the value as a float.
     * @return  If value is not a numeric scalar setStatus is called and 0 is returned.
     */
    float getFloat();
    /**
     * Get the value as a double.
     * @return  If value is not a numeric scalar setStatus is called and 0 is returned.
     */
    double getDouble();
    /**
     * Get the value as a string.
     * @return If value is not a scalar setStatus is called and 0 is returned.
     */
    String getString();
    
    /**
     * Get the value as a boolean array.
     * @return If the value is not a boolean array null is returned.
     */
    boolean[] getBooleanArray();
    /**
     * Get the value as a byte array.
     * @return If the value is not a numeric array null is returned. 
     */
    byte[] getByteArray();
    /**
     * Get the value as a short array.
     * @return If the value is not a numeric array null is returned. 
     */
    short[] getShortArray();
    /**
     * Get the value as an int array.
     * @return If the value is not a numeric array null is returned. 
     */
    int[] getIntArray();
    /**
     * Get the value as a long array.
     * @return If the value is not a numeric array null is returned. 
     */
    long[] getLongArray();
    /**
     * Get the value as a float array.
     * @return If the value is not a numeric array null is returned. 
     */
    float[] getFloatArray();
    /**
     * Get the value as a double array.
     * @return If the value is not a numeric array null is returned. 
     */
    double[] getDoubleArray();
    /**
     * Get the value as a string array.
     * @return If the value is not a scalar array null is returned.
     */
    String[] getStringArray();
    
    /**
     * Copy a sub-array of the value field into value.
     * If the value field is not a boolean array field no elements are copied.
     * @param value The place where data is copied.
     * @param length The maximum number of elements to copy.
     * @return The number of elements copied.
     */
    int getBooleanArray(boolean[] value,int length);
    /**
     * Copy a sub-array of the value field into value.
     * If the value field is not a numeric array field no elements are copied.
     * @param value The place where data is copied.
     * @param length The maximum number of elements to copy.
     * @return The number of elements copied.
     */
    int getByteArray(byte[] value,int length);
    /**
     * Copy a sub-array of the value field into value.
     * If the value field is not a numeric array field no elements are copied.
     * @param value The place where data is copied.
     * @param length The maximum number of elements to copy.
     * @return The number of elements copied.
     */
    int getShortArray(short[] value,int length);
    /**
     * Copy a sub-array of the value field into value.
     * If the value field is not a numeric array field no elements are copied.
     * @param value The place where data is copied.
     * @param length The maximum number of elements to copy.
     * @return The number of elements copied.
     */
    int getIntArray(int[] value,int length);
    /**
     * Copy a sub-array of the value field into value.
     * If the value field is not a numeric array field no elements are copied.
     * @param value The place where data is copied.
     * @param length The maximum number of elements to copy.
     * @return The number of elements copied.
     */
    int getLongArray(long[] value,int length);
    /**
     * Copy a sub-array of the value field into value.
     * If the value field is not a numeric array field no elements are copied.
     * @param value The place where data is copied.
     * @param length The maximum number of elements to copy.
     * @return The number of elements copied.
     */
    int getFloatArray(float[] value,int length);
    /**
     * Copy a sub-array of the value field into value.
     * If the value field is not a numeric array field no elements are copied.
     * @param value The place where data is copied.
     * @param length The maximum number of elements to copy.
     * @return The number of elements copied.
     */
    int getDoubleArray(double[] value,int length);
    /**
     * Copy a sub-array of the value field into value.
     * If the value field is not a scalar array field no elements are copied.
     * @param value The place where data is copied.
     * @param length The maximum number of elements to copy.
     * @return The number of elements copied.
     */
    int getStringArray(String[] value,int length);
    /**
     * Get the top level pvStructure returned by the last channelGet.
     * @return The pvStructutre.
     */
    PVStructure getPVStructure();
    /**
     * Get the bitSet for the top level structure.
     * @return The bitSet.
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
