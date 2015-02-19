/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;

/**
 * An easy way to get data from multiple channels.
 * @author mrk
 *
 */
public interface EasyMultiData {
    
    /**
     * Set the introspection interface for the specified  channel.
     * @param topStructure The interface.
     * @param indChannel The index of the channel.
     */
    void setStructure(Structure topStructure,int indChannel);
    /**
     * Update the data for the specified channel.
     * @param topPVStructure The newest data for the channel.
     * @param bitset The bitSet showing which fields have changed value.
     * @param indChannel The index of the channel.
     */
    void setPVStructure(PVStructure topPVStructure,BitSet bitset,int indChannel);
    /**
     * Get the number of channels.
     * @return The number of channels.
     */
    int getNumber();
    /**
     * Set the timeStamp base for computing deltaTimes. 
     */
    void startDeltaTime();
    /**
     * Update NTMultiChannel fields and/or doubleOnly fields.
     */
    void endDeltaTime(); 
    /**
     * Get the time when the last get was made.
     * @return The timeStamp.
     */
    TimeStamp getTimeStamp();
    /**
     * Is value a double[] ?
     * @return The answer.
     */
    boolean doubleOnly();
    /**
     * Get the value field as a MTMultiChannel structure.
     * @return The value.
     * This is null if doubleOnly is true.
     */
    PVStructure getNTMultiChannel();
    /**
     * Get the top level structure if the value field is a double[]
     * @return The top level structure.
     * This is null if doubleOnly is false.
     */
    PVStructure getPVTop();
    /**
     * Return the value field.
     * @return The double[]
     * This is null if doubleOnly is false.
     */
    double[] getDoubleArray();
    /**
     * Get the data from the value field.
     * @param offset The offset into the data of the value field.
     * @param data The place to copy the data.
     * @param length The number of elements to copy.
     * @return The number of elements copied.
     * This is 0 if doubleOnly is false.
     */
    int getDoubleArray(int offset, double[]data,int length);
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
