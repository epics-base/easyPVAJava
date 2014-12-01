/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;

/**
 * @author mrk
 *
 */
public interface EasyRPC {
    /**
     * Disconnect from channel and destroy all resources.
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
     * Call issueRequest and the waitRequest.
     * @param request The request pvStructure.
     * @return The result pvStructure.
     */
    PVStructure request(PVStructure request);
    /**
     * call Monitor::request and return.
     * @param request The request pvStructure.
     */
    void issueRequest(PVStructure request);
    /**
     * Wait until Monitor::request completes or for timeout.
     * If failure null is returned.
     * If failure getStatus can be called to get reason.
     * @return The result pvStructure.
     */
    PVStructure waitRequest();
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
