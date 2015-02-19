/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;

/**
 * An easy way to get data from multiple channels.
 * @author mrk
 *
 */
public interface EasyMultiMonitor {
    /**
     * Optional callback for client
     */
    public interface EasyMultiRequester {
        
        /**
         * A monitor event has occurred.
         * @param monitor The EasyMonitor that traped the event.
         */
        void event(EasyMultiMonitor monitor);
    }
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
     * Optional request to be notified when monitors occur.
     * @param requester The requester which must be implemented by the caller.
     */
    void setRequester(EasyMultiRequester requester);
    /**
     * Start monitoring.
     * This will wait until the monitor is connected.
     * @param waitBetweenEvents The time to wait between events to see if more are available. 
     * @return (false,true) means (failure,success).
     * If false is returned then failure and getMessage will return reason.
     */
    boolean start(double waitBetweenEvents);
    /**
     * Stop monitoring.
     */
    boolean stop();
    /**
     * Get the monitorElements.
     * The client MUST only access the monitorElements between poll and release.
     * An element is null if it has data.
     * @return The monitorElements.
     */
    MonitorElement[] getMonitorElement();
    /**
     * poll for new events.
     * @return the number of channels with a new monitor.
     * If > 0 then each element that is not null has the data for the corresponding channel.
     * A new poll can not be issued until release is called.
     */
    int poll();
    /**
     * Release each monitor element that is not null.
     * @return (false,true) if additional monitors are available.
     */
    boolean release();
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
