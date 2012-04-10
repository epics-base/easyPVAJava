/**
 * 
 */
package org.epics.ca.easyPVA;

import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;

/**
 * @author mrk
 *
 */
public interface EasyMultiChannel{
    void destroy();
    boolean connect(double timeout);
    void issueConnect();
    boolean waitConnect(double timeout);
    // EAField TBD
    // EasyMultiProcess TBD 
    EasyMultiGet createGet(); 
    EasyMultiGet createGet(String request);
    EasyMultiGet createGet(PVStructure pvRequest);
    EasyMultiPut createPut();
    EasyMultiPut createPut(String request);
    EasyMultiPut createPut(PVStructure pvRequest);
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
