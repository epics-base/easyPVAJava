/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;

/**
 * @author mrk
 *
 */
public interface EasyMultiGet {
    void destroy();
    boolean connect();
    void issueConnect();
    boolean waitConnect();
    boolean get();
    void issueGet();
    boolean waitGet();
    TimeStamp getTimeStamp();
    int getLength();
   
    boolean doubleOnly();
    PVStructure getNTMultiChannel();
    PVStructure getPVTop();
    double[] getDoubleArray();
    int getDoubleArray(int index, double[]data,int length);
     
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
