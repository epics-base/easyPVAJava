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
public interface EasyArray {
    void destroy();
    boolean connect();
    void issueConnect();
    boolean waitConnect();
    PVArray getPVArray();
    void get(int offset,int length);
    void put(int offset,int length);
    
    void getBoolean(boolean[] array, int offset, int length);
    void getByte(byte[] array,int offset,int length);
    void getShort(short[] array, int offset, int length);
    void getInt(int[] array, int offset, int length);
    void getLong(long[] array, int offset, int length);
    void getFloat(float[] array, int offset, int length);
    void getDouble(double[] array, int offset, int length);
    void getString(String[] array, int offset, int length);
    void getStructure(PVStructure[] array, int offset, int length);
    
    void putBoolean(boolean[] array, int offset, int length);
    void putByte(byte[] array,int offset,int length);
    void putShort(short[] array, int offset, int length);
    void putInt(int[] array, int offset, int length);
    void putLong(long[] array, int offset, int length);
    void putFloat(float[] array, int offset, int length);
    void putDouble(double[] array, int offset, int length);
    void putString(String[] array, int offset, int length);
    void putStructure(PVStructure[] array, int offset, int length);
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
