/**
 * 
 */
package org.epics.ca.easyPVA;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Status;

/**
 * @author mrk
 *
 */
public interface EasyPVA extends Requester {
    /**
     * Destroy all the channels and multiChannels.
     */
    void destroy();
    /**
     * Create an EasyChannel. The provider is pvAccess.
     * @param channelName The channelName.
     * @return The interface.
     */
    EasyChannel createChannel(String channelName);
    /**
     * Create an EasyChannel with the specified provider.
     * @param channelName The channelName.
     * @param providerName The provider.
     * @return The interface or null if the provider does not exist.
     */
    EasyChannel createChannel(String channelName,String providerName);
    /**
     * Create an EasyMultiChannel. The provider is pvAccess.
     * @param channelNames The channelNames.
     * @return The interface.
     */
    EasyMultiChannel createMultiChannel(String[] channelNames);
    /**
     * Create an EasyMultiChannel with the specified provider.
     * @param channelNames The channelNames.
     * @param providerName The provider.
     * @return The interface.
     */
    EasyMultiChannel createMultiChannel(String[] channelNames,String providerName);
    /**
     * Set a requester. The default is for EasyPVA to handle messages by printing to System.out.
     * @param requester The requester.
     */
    void setRequester(Requester requester);
    /**
     * Clear the requester. EasyPVA will handle messages.
     */
    void clearRequester();
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
    /**
     * Set auto state. This is a global state for other Easy classes.
     * @param get Will get be called before a convenience get method returns data.
     * @param put Will put be called after a convenience put method is called.
     */
    void setAuto(boolean get, boolean put);
    /**
     * Is autoGet true?
     * @return false or true
     */
    boolean isAutoGet();
    /**
     * Is autoPut true?
     * @return false or true.
     */
    boolean isAutoPut();
    /**
     * This determines if message is called as a result of setStatus.
     * @param type The minimum status which causes message to be called.
     * The default is WARNING.
     */
    void setAutoMessage(Status.StatusType type);
}
