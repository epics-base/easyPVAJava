/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvaccess.client.Channel;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;

/**
 * An easy to use alternative to directly calling the Channel methods of pvAccess.
 * @author mrk
 *
 */
public interface EasyChannel {
    /**
     * Destroy the pvAccess connection.
     */
    void destroy();
    /**
     * Get the name of the channel to which EasyChannel is connected.
     * @return The channel name.
     */
    String getChannelName();
    /**
     * Get the Channel to which EasyChannel is connected.
     * @return The channel.
     */
    Channel getChannel();
    /**
     * Connect to the channel.
     * This calls issueConnect and waitConnect.
     * @param timeout The time to wait for connecting to the channel.
     * @return (false,true) means (failure,success)
     */
    boolean connect(double timeout);
    /**
     * Issue a connect request and return immediately.
     */
    void issueConnect();
    /**
     * Wait until the connection completes or for timeout.
     * If failure getStatus can be called to get reason.
     * @param timeout The time in second to wait.
     * @return (false,true) means (failure,success)
     */
    boolean waitConnect(double timeout);
    /**
     * Calls the next method with subField = "";
     * @return The interface.
     */
    EasyField createField();
    /**
     * Create an EasyField for the specified subField.
     * @param subField The syntax for subField is defined in package org.epics.pvdata.copy
     * @return The interface.
     */
    EasyField createField(String subField);
    /**
     * Calls the next method with request = "";
     * @return The interface.
     */
    EasyProcess createProcess();
    /**
     * First call createRequest as implemented by pvDataJava and then calls the next method.
     * @param request The request as described in package org.epics.pvdata.copy
     * @return The interface.
     */
    EasyProcess createProcess(String request);
    /**
     * Creates an EasyProcess. 
     * @param pvRequest The syntax of pvRequest is described in package org.epics.pvdata.copy.
     * @return The interface.
     */
    EasyProcess createProcess(PVStructure pvRequest);
    /**
     * Call the next method with request =  "field(value,alarm,timeStamp)" 
     * @return The interface.
     */
    EasyGet createGet();
    /**
     * First call createRequest as implemented by pvDataJava and then calls the next method.
     * @param request The request as described in package org.epics.pvdata.copy
     * @return The interface.
     */
    EasyGet createGet(String request);
    /**
     * Creates an EasyGet.
     * @param pvRequest The syntax of pvRequest is described in package org.epics.pvdata.copy.
     * @return The interface.
     */
    EasyGet createGet(PVStructure pvRequest);
    /**
     *  Call the next method with request = "field(value)" 
     * @return The interface.
     */
    EasyPut createPut();
    /**
     * First call createRequest as implemented by pvDataJava and then calls the next method.
     * @param request The request as described in package org.epics.pvdata.copy
     * @return The interface.
     */
    EasyPut createPut(String request);
    /**
     * Create an EasyPut.
     * @param pvRequest The syntax of pvRequest is described in package org.epics.pvdata.copy.
     * @return The interface.
     */
    EasyPut createPut(PVStructure pvRequest);
    /**
     *  Call the next method with request = "record[process=true]putField(argument)getField(result)".
     * @return The interface.
     */
    EasyPutGet createPutGet();
    /**
     * First call createRequest as implemented by pvDataJava and then calls the next method.
     * @param request The request as described in package org.epics.pvdata.copy
     * @return The interface.
     */
    EasyPutGet createPutGet(String request);
    /**
     * Create an EasyPutGet.
     * @param pvRequest The syntax of pvRequest is described in package org.epics.pvdata.copy.
     * @return The interface.
     */
    EasyPutGet createPutGet(PVStructure pvRequest);
    /**
     * Call createRPC(PVStructure(null))
     * @return The interface.
     */
    EasyRPC createRPC();
    /**
     * First call createRequest as implemented by pvDataJava and then calls the next method.
     * @param request The request as described in package org.epics.pvdata.copy
     * @return The interface.
     */
    EasyRPC createRPC(String request);
    /**
     * Create an EasyRPC.
     * @param pvRequest The syntax of pvRequest is described in package org.epics.pvdata.copy.
     * @return The interface.
     */
    EasyRPC createRPC(PVStructure pvRequest);
    /**
     * Call the next method with request = "field(value)";
     * @return The interface.
     */
    EasyArray createArray();
    /**
     * First call createRequest as implemented by pvDataJava and then calls the next method.
     * @param request The request as described in package org.epics.pvdata.copy
     * @return The interface.
     */
    EasyArray createArray(String request);
    /**
     * Create an EasyArray.
     * @param pvRequest The syntax of pvRequest is described in package org.epics.pvdata.copy.
     * @return The interface.
     */
    EasyArray createArray(PVStructure pvRequest);
    /**
     * Call the next method with request = "field(value.alarm,timeStamp)" 
     * @return The interface.
     */
    EasyMonitor createMonitor();
    /**
     * First call createRequest as implemented by pvDataJava and then calls the next method.
     * @param request The request as described in package org.epics.pvdata.copy
     * @return The interface.
     */
    EasyMonitor createMonitor(String request);
    /**
     * Create an EasyMonitor.
     * @param pvRequest  The syntax of pvRequest is described in package org.epics.pvdata.copy.
     * @return The interface.
     */
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
