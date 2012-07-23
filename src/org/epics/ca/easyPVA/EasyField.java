/**
 * 
 */
package org.epics.ca.easyPVA;

import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.Status;

/**
 * @author mrk
 *
 */
public interface EasyField {
    void destroy();
    boolean get();
    void issueGet();
    boolean waitGet();
    Field getField();
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
