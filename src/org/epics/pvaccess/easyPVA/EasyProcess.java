/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.pv.Status;

/**
 * An easy interface to channelProcess.
 * @author mrk
 *
 */
public interface EasyProcess {
    /**
     * Destroy the EasyProcess.
     */
    void destroy();
    /**
     * Call issueConnect and then waitConnect.
     * @return (false,true) means (failure,success)
     */
    boolean connect();
    /**
     * Issue a connect request and return immediately.
     */
    void issueConnect();
    /**
     * Wait until connection completes or for timeout.
     * If failure getStatus can be called to get reason.
     * @return (false,true) means (failure,success)
     */
    boolean waitConnect();
    /**
     * Call issueProcess and then waitProcess.
     * @return (false,true) means (failure,success)
     */
    boolean process();
    /**
     * Issue a process request and return immediately.
     */
    void issueProcess();
    /**
     * Wait until process completes or for timeout.
     * If failure getStatus can be called to get reason.
     * @return (false,true) means (failure,success)
     */
    boolean waitProcess();
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
