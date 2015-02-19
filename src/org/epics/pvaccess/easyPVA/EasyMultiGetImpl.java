package org.epics.pvaccess.easyPVA;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.ChannelGet;
import org.epics.pvaccess.client.ChannelGetRequester;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Union;

public class EasyMultiGetImpl {
    
    /**
     * Factory for creating a new EasyPVStructure.
     * @return The interface.
     */
    static EasyMultiGet create(
        EasyMultiChannel easyMultiChannel,
        Channel[] channel,
        PVStructure pvRequest,
        Union union)
    {
        EMultiGet multiGet = new EMultiGet(easyMultiChannel,channel,pvRequest,union);
        if(multiGet.init()) return multiGet;
        return null;
    }

    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    
    private static class ChannelGetRequesterPvt implements ChannelGetRequester
    {
        private final EMultiGet multiGet;
        private final int indChannel;
        
        ChannelGetRequesterPvt(EMultiGet multiGet,int indChannel)
        {
            this.multiGet = multiGet;
            this.indChannel = indChannel;
        }
        public String getRequesterName() {
            return multiGet.getRequesterName();
        }
        public void message(String message, MessageType messageType) {
            multiGet.message(message, messageType);
        }
        public void channelGetConnect(Status status, ChannelGet channelGet,
                Structure structure)
        {
            multiGet.channelGetConnect(status,channelGet,structure,indChannel);
        }
        public void getDone(Status status, ChannelGet channelGet,
                PVStructure pvStructure, BitSet bitSet)
        {
            multiGet.getDone(status,channelGet,pvStructure,bitSet,indChannel);
        }
    }
    private static class EMultiGet implements EasyMultiGet {
        private final EasyMultiChannel easyMultiChannel;
        private final Channel[] channel;
        private final PVStructure pvRequest;
        private final int nchannel;
        private final EasyMultiData easyMultiData;
        
        private ChannelGetRequesterPvt[] channelGetRequester = null;
        private volatile ChannelGet[] channelGet = null;

        
        
        // following used by connect
        private volatile int numGetToConnect = 0;
        private volatile int numConnectCallback = 0;
        private volatile int numGetConnected = 0;
        private volatile boolean[] isGetConnected = null;
        private enum ConnectState {connectIdle,connectActive,connectDone};
        private volatile ConnectState connectState = ConnectState.connectIdle;
        
        // following used by get
        private volatile int numGet = 0;
        private volatile boolean badGet = false;
        private enum GetState {getIdle,getActive,getFailed,getDone};
        private volatile GetState getState = GetState.getIdle;
        
        private volatile boolean isDestroyed = false;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waitForConnect = lock.newCondition();
        private final Condition waitForGet = lock.newCondition();
        private volatile Status status = statusCreate.getStatusOK();
        
        
        EMultiGet(EasyMultiChannel easyMultiChannel,Channel[] channel,PVStructure pvRequest,Union union) {
            this.easyMultiChannel = easyMultiChannel;
            this.channel = channel;
            this.pvRequest = pvRequest;
            nchannel = channel.length;
            easyMultiData = easyMultiChannel.createEasyMultiData(pvRequest, union);
            
        }
        
        private boolean init()
        {
            channelGetRequester = new ChannelGetRequesterPvt[nchannel];
            isGetConnected = new boolean[nchannel];
            channelGet = new ChannelGet[nchannel];
            for(int i=0; i<nchannel; ++i) {
                channelGetRequester[i] = new ChannelGetRequesterPvt(this,i);
                isGetConnected[i] = false;
                channelGet[i] = null;
                if(channel[i].isConnected()) ++numGetToConnect;
            }
            return true;
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#destroy()
         */
        @Override
        public void destroy() {
            synchronized (this) {
               if(isDestroyed) return;
               isDestroyed = true;
            }
            for(int i=0; i<nchannel; ++i){
                if(channelGet[i]!=null) channelGet[i].destroy();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#connect()
         */
        @Override
        public boolean connect() {
            issueConnect();
            return waitConnect();
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#issueConnect()
         */
        @Override
        public void issueConnect() {
            if(isDestroyed) return;
            if(connectState!=ConnectState.connectIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"connect already issued",null);
                setStatus(status);
                return;
            }
            numConnectCallback = 0;
            if(easyMultiChannel.allConnected()) {
                numGetToConnect = channel.length;
            } else {
                for(int i=0; i<channel.length; ++i) {
                    if(channel[i].isConnected()) numGetToConnect++;
                }
            }
            connectState = ConnectState.connectActive;
            for(int i=0; i<channel.length; ++i) {
                if(channel[i].isConnected()) {
                    channel[i].createChannelGet(channelGetRequester[i], pvRequest);
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#waitConnect()
         */
        @Override
        public boolean waitConnect() {
            if(isDestroyed) return false;
            try {
                lock.lock();
                try {
                    if(numConnectCallback<numGetToConnect) waitForConnect.await();
                } catch(InterruptedException e) {
                    Status status = statusCreate.createStatus(
                            StatusType.ERROR,
                            e.getMessage(),
                            e.fillInStackTrace());
                    setStatus(status);
                    return false;
                }
            } finally {
                lock.unlock();
            }
            if(numGetConnected!=numGetToConnect) {
                Status status = statusCreate.createStatus(StatusType.ERROR," did not connect",null);
                setStatus(status);
                return false;
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#get()
         */
        @Override
        public boolean get() {
            issueGet();
            return waitGet();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#issueGet()
         */
        @Override
        public void issueGet() {
            if(isDestroyed) return;
            checkConnected();
            if(getState!=GetState.getIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"get already issued",null);
                setStatus(status);
                return;
            }
            boolean allConnected = true;
            for(int i=0; i<nchannel; ++i) if(!channelGet[i].getChannel().isConnected()) allConnected = false;
            if(!allConnected) {
                badGet = true;
                return;
            }
            numGet = 0;
            badGet = false;
            getState = GetState.getActive;
            easyMultiData.startDeltaTime();
            for(int i=0; i<nchannel; ++i){
                channelGet[i].get();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#waitGet()
         */
        @Override
        public boolean waitGet() {
            if(isDestroyed) return false;
            checkConnected();
            try {
                lock.lock();
                try {
                    if(getState==GetState.getActive) waitForGet.await();
                } catch(InterruptedException e) {
                    Status status = statusCreate.createStatus(StatusType.ERROR, e.getMessage(), e.fillInStackTrace());
                    setStatus(status);
                    return false;
                }
            } finally {
                lock.unlock();
            }
            getState = GetState.getIdle;
            if(badGet) {
                Status status = statusCreate.createStatus(StatusType.ERROR," get failed",null);
                setStatus(status);
                return false;
            }
            easyMultiData.endDeltaTime();
            return true;
        }
       
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getTimeStamp()
         */
        @Override
        public TimeStamp getTimeStamp() {
            return easyMultiData.getTimeStamp();
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getLength()
         */
        @Override
        public int getLength() {
            return nchannel;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#doubleOnly()
         */
        @Override
        public boolean doubleOnly() {
            return easyMultiData.doubleOnly();
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getNTMultiChannel()
         */
        @Override
        public PVStructure getNTMultiChannel() {
            boolean result = checkGetState();
            if(!result) return null;
            return easyMultiData.getNTMultiChannel();
        }
        
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getPVTop()
         */
        @Override
        public PVStructure getPVTop() {
            checkGetState();
            return easyMultiData.getPVTop();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getDoubleArray()
         */
        @Override
        public double[] getDoubleArray() {
            boolean result = checkGetState();
            if(!result) return null;
            return easyMultiData.getDoubleArray();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getDoubleArray(int, double[], int)
         */
        @Override
        public int getDoubleArray(int offset, double[] data, int length) {
            boolean result = checkGetState();
            if(!result) return 0;
            return easyMultiData.getDoubleArray(offset, data, length);
        }

        
        public void setStatus(Status status) {
            this.status = status;
            easyMultiChannel.setStatus(status);
        }
        
        public Status getStatus() {
            Status save = status;
            status = statusCreate.getStatusOK();
            return save;
        }
        private void checkConnected() {
            if(connectState==ConnectState.connectIdle) connect();
        }
        
        private boolean checkGetState() {
            checkConnected();
            if(getState==GetState.getIdle) return get();
            return false;
        }
        
        private String getRequesterName() {
            return easyMultiChannel.getRequesterName();
        }
        
        private void message(String message, MessageType messageType) {
            if(isDestroyed) return;
            easyMultiChannel.message(message, messageType);
        }
        private void channelGetConnect(
            Status status,
            ChannelGet channelGet,
            Structure structure,
            int index)
        {
            if(isDestroyed) return;
            this.channelGet[index] = channelGet;
            if(status.isOK()) {  
                if(!isGetConnected[index]) {
                    ++numGetConnected;
                    isGetConnected[index] = true;
                    easyMultiData.setStructure(structure, index);
                }
            } else {
                if(isGetConnected[index]) {
                    --numGetConnected;
                    isGetConnected[index] = false;
                    setStatus(status);
                }
            }
            if(connectState!=ConnectState.connectActive) return;
            lock.lock();
            try {
                numConnectCallback++;
                if(numConnectCallback==numGetToConnect) {
                    connectState = ConnectState.connectDone;
                    waitForConnect.signal();
                }
            } finally {
                lock.unlock();
            }
        }
        private void getDone(
            Status status,
            ChannelGet channelGet,
            PVStructure pvStructure,
            BitSet bitSet,
            int index)
        {
            if(isDestroyed) return;
            if(status.isOK()) {
                easyMultiData.setPVStructure(pvStructure, bitSet, index);
            } else {
                badGet = true;
                message("getDone " + channel[index].getChannelName() + " " +status.getMessage(),MessageType.error);
            }
            lock.lock();
            try {
                ++numGet;
                if(numGet==nchannel) {
                    if(badGet) {
                        getState = GetState.getFailed;
                    } else {
                        getState = GetState.getDone;
                    }
                    waitForGet.signal();
                }
            } finally {
                lock.unlock();
            }
        }
    }
    
}
