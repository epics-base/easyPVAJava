/**
 * 
 */
package org.epics.ca.easyPVA;

import org.epics.pvData.misc.BitSet;
import org.epics.pvData.monitor.MonitorRequester;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;

/**
 * @author mrk
 *
 */
public interface EasyMonitor {
    void destroy();
    boolean connect();
    void issueConnect();
    boolean waitConnect();
    void setRequester(MonitorRequester monitorRequester);
    void start();
    void stop();
    PVStructure getEvent();
    BitSet getChangedBitSet();
    BitSet getOverrunBitSet();
    void releaseEvent();
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
