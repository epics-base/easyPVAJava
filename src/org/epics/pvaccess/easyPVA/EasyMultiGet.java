/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;

/**
 * An easy way to get data from multiple channels.
 * @author mrk
 *
 */
public interface EasyMultiGet {
    /**
     * Clean up
     */
    void destroy();
    /**
     * Calls issueConnect and then waitConnect.
     * @return (false,true) if (not connected, connected)
     */
    boolean connect();
    /**
     * create the channelGet for all channels.
     */
    void issueConnect();
    /**
     * Wait until all channelGets are created.
     * @return (false,true) if (not all connected, all connected)
     */
    boolean waitConnect();
    /**
     * call issueGet and the waitGet.
     * @return (false,true) if (failure, success)
     */
    boolean get();
    /**
     * Issue a get for each channel.
     */
    void issueGet();
    /**
     * wait until all gets are complete.
     * @return (true,false) if (no errors, errors) resulted from gets.
     * If an error occurred then getStatus returns a reason.
     * @return (false,true) if (failure, success)
     */
    boolean waitGet();
    /**
     * Get the time when the last get was made.
     * @return The timeStamp.
     */
    TimeStamp getTimeStamp();
    /**
     * Get the number of channels.
     * @return The number of channels.
     */
    int getLength();
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
     * Get the top level structure of the value field is a double[[]
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
