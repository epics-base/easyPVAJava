/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.Status;

/**
 * This is an easy to use alternative to ChannelGet
 * @author mrk
 *
 */
public interface EasyGet extends EasyPVStructure
{
    /**
     * clean up resources used.
     */
    void destroy();
    /**
     * call issueConnect and then waitConnect.
     * @return the result from waitConnect.
     */
    boolean connect();
    /**
     * create the channelGet connection to the channel.
     * This can only be called once.
     */
    void issueConnect();
    /**
     * wait until the channelGet connection to the channel is complete.
     * If failure getStatus can be called to get reason.
     * @return (false,true) means (failure,success)
     */
    boolean waitConnect();
    /**
     * Call issueGet and then waitGet.
     * @return (false,true) means (failure,success)
     */
    boolean get();
    /**
     * Issue a get and return immediately.
     */
    void issueGet();
    /**
     * Wait until get completes.
     * If failure getStatus can be called to get reason.
     * @return (false,true) means (failure,success)
     */
    boolean waitGet();

    /**
     * Get the bitSet for the top level structure.
     * @return The bitSet.
     */
    BitSet getBitSet();
    /**
     * Set a new status value. The new value will replace the current status. The initial status is statusOK.
     * @param status The new status.
     */
    void setStatus(Status status);
}
