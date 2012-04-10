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
public interface EasyRPC {
    void destroy();
    boolean connect();
    void issueConnect();
    boolean waitConnect();
    PVStructure request(PVStructure request,boolean lastRequest);
    void issueRequest(PVStructure request,boolean lastRequest);
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
