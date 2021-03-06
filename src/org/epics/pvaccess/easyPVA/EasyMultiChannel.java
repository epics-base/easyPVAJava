/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Union;

/**
 * An easy to use interface to get/put data from/to multiple channels.
 * @author mrk
 *
 */
public interface EasyMultiChannel {
    /**
     * Get the requester name.
     * @return The name.
     */
    public String getRequesterName();
    /**
     * new message.
     * @param message The message string.
     * @param messageType The message type.
     */
    public void message(String message, MessageType messageType);
    /**
     * Get the channelNames.
     * @return the names.
     */
    String[] getChannelNames();
    /**
     * Destroy all resources.
     */
    void destroy();
    /**
     * Calls issueConnect and then waitConnect.
     * @param timeout timeOut for waitConnect.
     * @return (false,true) if (not connected, connected)
     */
    boolean connect(double timeout);
    /**
     * Calls issueConnect and then waitConnect.
     * @param timeout timeOut for waitConnect
     * @param minConnect minConnect for waitConnect
     * @return (false,true) means (did not, did) connect.
     */
    boolean connect(double timeout,int minConnect);
    /**
     * Connect to all channels.
     */
    void issueConnect();
    /**
     * Wait until all channels are connected or until timeout.
     * @param timeout The time to wait for connections.
     * When a timeout occurs a new time out will start if any channels connected since the last timeout.
     * @return (false,true) if all channels (did not, did) connect.
     */
    boolean waitConnect(double timeout);
    /**
     * Wait until minConnect channels are connected or until timeout and no more channels connect.
     * @param timeout The time to wait for connections.
     * @param minConnect The minimum number of channels that must connect.
     * When a timeout occurs a new time out will start if any channels connected since the last timeout.
     * @return (false,true) of all channels (did not, did) connect.
     */
    boolean waitConnect(double timeout,int minConnect);
    /**
     * Are all channels connected?
     * @return if all are connected.
     */
    boolean allConnected();
    /**
     * Get the connection state of each channel.
     * @return The state of each channel.
     */
    boolean[] isConnected();
    /**
     * Create an EasyMultiData.
     * @param pvRequest The pvRequest for each channel.
     * @param union The union for each channel.
     * @return The interface.
     */
    EasyMultiData createEasyMultiData(PVStructure pvRequest,Union union);
    /**
     * create a multiChannelGet that presents data as a NTMultiChannel.
     * calls the next method with request = "field(value,alarm,timeStamp)"
     * @return The interface.
     */
    EasyMultiGet createGet(); 
    /**
     * create a multiChannelGet.
     * calls the next method after creating a pvRequest structure.
     * @param request A request string valid for creatRequest.
     * @return The interface.
     */
    EasyMultiGet createGet(String request);
    /**
     * create a multiChannelGet.
     * @param pvRequest The pvRequest for each channel.
     * @return The interface.
     */
    EasyMultiGet createGet(PVStructure pvRequest);
    /**
     * create a multiChannelGet.
     * @param doubleOnly true if data presented as a double[].
     * @return The interface.
     */
    EasyMultiGet createGet(boolean doubleOnly); 
    /**
     * create a multiChannelGet.
     * calls the next method with request = "field(value)"
     * @param doubleOnly true if data presented as a double[].
     * @param request  A request string valid for creatRequest.
     * @return EasyMultiGet or null if invalid request.
     */
    EasyMultiGet createGet(boolean doubleOnly,String request);
    /**
     * create a multiChannelGet.
     * @param doubleOnly true if data presented as a double[].
     * @param pvRequest The pvRequest for each channel.
     * @return  EasyMultiGet or null if invalid request.
     */
    EasyMultiGet createGet(boolean doubleOnly,PVStructure pvRequest);
    /**
     * create a multiChannelPut.
     * @return The interface.
     */
    EasyMultiPut createPut();
    /**
     * create a multiChannelPut.
     * @param doubleOnly true if data must be presented as a double[].
     * @return EasyMultiPut or null if invalid request.
     */
    EasyMultiPut createPut(boolean doubleOnly);
    /**
     * Call the next method with request =  "field(value,alarm,timeStamp)" 
     * @return The interface.
     */
    EasyMultiMonitor createMonitor();
    /**
     * First call createRequest as implemented by pvDataJava and then calls the next method.
     * @param request The request as described in package org.epics.pvdata.copy
     * @return The interface.
     */
    EasyMultiMonitor createMonitor(String request);
    /**
     * Creates an EasyMultiMonitor.
     * The pvRequest is used to create the monitor for each channel.
     * @param pvRequest The syntax of pvRequest is described in package org.epics.pvdata.copy.
     * @return The interface.
     */
    EasyMultiMonitor createMonitor(PVStructure pvRequest);
    /**
     * Call the next method with request =  "field(value,alarm,timeStamp)" 
     * @param doubleOnly true if data must be presented as a double[].
     * @return The interface.
     */
    EasyMultiMonitor createMonitor(boolean doubleOnly);
    /**
     * First call createRequest as implemented by pvDataJava and then calls the next method.
     * @param doubleOnly true if data must be presented as a double[].
     * @param request The request as described in package org.epics.pvdata.copy
     * @return The interface.
     */
    EasyMultiMonitor createMonitor(boolean doubleOnly,String request);
    /**
     * Creates an EasyMultiMonitor.
     * The pvRequest is used to create the monitor for each channel.
     * @param doubleOnly true if data must be presented as a double[].
     * @param pvRequest The syntax of pvRequest is described in package org.epics.pvdata.copy.
     * @return The interface.
     */
    EasyMultiMonitor createMonitor(boolean doubleOnly,PVStructure pvRequest);
   
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
