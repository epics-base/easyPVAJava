/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.Status;

/**
 * An easy to use alternative to directly calling the Channel::getField method of pvAccess.
 * @author mrk
 *
 */
public interface EasyField {
    /**
     * Destroy the EasyField.
     */
    void destroy();
    /**
     * Calls issueGet and then waitGet.
     * @return The result from waitGet.
     */
    boolean get();
    /**
     * Issue a get request and return immediately.
     */
    void issueGet();
    /**
     * Block until the get request completes.
     * If the request fails than getStatus can be called to find the reason.
     * @return (false,true) if request (is not, is) successful.
     */
    boolean waitGet();
    /**
     * Get the introspection interface.
     * @return The interface.
     */
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
