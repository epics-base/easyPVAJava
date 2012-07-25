/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.pv.Status;

/**
 * @author mrk
 *
 */
public interface EasyProcess {
    void destroy();
    boolean connect();
    void issueConnect();
    boolean waitConnect();
    boolean process();
    void issueProcess();
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
