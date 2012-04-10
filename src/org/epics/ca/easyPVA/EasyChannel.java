/**
 * 
 */
package org.epics.ca.easyPVA;

import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;
import org.epics.ca.client.*;

/**
 * @author mrk
 *
 */
public interface EasyChannel {
    void destroy();
    String getChannelName();
    Channel getChannel();
    boolean connect(double timeout);
    void issueConnect();
    boolean waitConnect(double timeout);
    EasyField createField();
    EasyField createField(String subField);
    EasyProcess createProcess();
    EasyProcess createProcess(String request);
    EasyProcess createProcess(PVStructure pvRequest);
    EasyGet createGet();
    EasyGet createGet(String request);
    EasyGet createGet(PVStructure pvRequest);
    EasyPut createPut();
    EasyPut createPut(String request);
    EasyPut createPut(PVStructure pvRequest);
    EasyPutGet createPutGet();
    EasyPutGet createPutGet(String request);
    EasyPutGet createPutGet(PVStructure pvRequest);
    EasyRPC createRPC();
    EasyRPC createRPC(String request);
    EasyRPC createRPC(PVStructure pvRequest);
    EasyArray createArray();
    EasyArray createArray(String request);
    EasyArray createArray(PVStructure pvRequest);
    EasyMonitor createMonitor();
    EasyMonitor createMonitor(String request);
    EasyMonitor createMonitor(PVStructure pvRequest);
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
