/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.pv.Status;

/**
 * EasyMonitor is not implemented.
 * The following is a guess at the methods to be implemented.
 * @author mrk
 *
 */
public interface EasyMonitor {
    /**
     * Optional callback for client
     */
    public interface EasyRequester {
        
        /**
         * A monitor event has occurred.
         * @param monitor The EasyMonitor that traped the event.
         */
        void event(EasyMonitor monitor);
    }
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
     * create the monitor connection to the channel.
     * This can only be called once.
     */
    void issueConnect();
    /**
     * wait until the monitor connection to the channel is complete.
     * If failure getStatus can be called to get reason.
     * @return (false,true) means (failure,success)
     */
    boolean waitConnect();
    /**
     * Optional request to be notified when monitors occur.
     * @param requester The requester which must be implemented by the caller.
     */
    void setRequester(EasyRequester requester);
    /**
     * Start monitoring.
     * This will wait until the monitor is connected.
     * If false is returned then failure and getMessage will return reason.
     * @return (false,true) means (failure.success)
     */
    boolean start();
    /**
     * Stop monitoring.
     * @return (false,true) means (failure.success)
     */
    boolean stop();
    /**
     * Get the data for the next monitor.
     * @return the next monitor or null if no new monitorElement are present.
     * If successful releaseEvent must be called before another call to poll.
     */
    MonitorElement poll();
    /**
     * Release the monitorElement returned by poll.
     *  @return (false,true) means (failure.success)
     */
    boolean releaseEvent();
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
