package org.epics.pvaccess.easyPVA;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.client.Channel;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Union;

public class EasyMultiMonitorImpl {
    
    /**
     * Factory for creating a new EasyPVStructure.
     * @return The interface.
     */
    static EasyMultiMonitor create(
        EasyMultiChannel easyMultiChannel,
        Channel[] channel,
        PVStructure pvRequest,
        Union union)
    {
        return new EMultiMonitor(easyMultiChannel,channel,pvRequest,union);
    }

    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Status clientNotActive = statusCreate.createStatus(
            StatusType.ERROR,"method can only be called between poll and release",null);
    
    private static class MonitorRequesterPvt implements MonitorRequester
    {
        private final EMultiMonitor multiMonitor;
        private final int indChannel;
        
        MonitorRequesterPvt(EMultiMonitor xx, int indChannel)
        {
            this.multiMonitor = xx;
            this.indChannel = indChannel;
        }
        public String getRequesterName() {
            return multiMonitor.getRequesterName();
        }
        public void message(String message, MessageType messageType) {
            multiMonitor.message(message, messageType);
        }
        public void monitorConnect(Status status, Monitor monitor,
                Structure structure)
        {
            multiMonitor.monitorConnect(status,monitor,structure,indChannel);   
        }
        public void monitorEvent(Monitor monitor)
        {
            multiMonitor.monitorEvent(indChannel);
        }
        public void unlisten(Monitor monitor){
            multiMonitor.lostChannel(indChannel);
        }
        
    }
    
    private static class EMultiMonitor implements EasyMultiMonitor{
        private final EasyMultiChannel easyMultiChannel;
        private final Channel[] channel;
        private final PVStructure pvRequest;
        private final int nchannel;
        private final EasyMultiData easyMultiData;
        
        private MonitorRequesterPvt[] monitorRequester = null;
        private EasyMultiRequester multiRequester = null;
        private volatile Monitor[] monitor = null;
        private volatile MonitorElement[] monitorElement= null;
        
        // following used by connect
        private volatile int numMonitorToConnect = 0;
        private volatile int numConnectCallback = 0;
        private volatile int numMonitorConnected = 0;
        private volatile boolean[] isMonitorConnected = null;
        private enum ConnectState {connectIdle,connectActive,connectDone};
        private volatile ConnectState connectState = ConnectState.connectIdle;
        
        // following used by for monitors
        private int numMonitors = 0;
        private double waitBetweenEvents = 1.0;
        private enum MonitorState {monitorIdle,monitorActive,monitorClientActive};
        private volatile MonitorState monitorState = MonitorState.monitorIdle;
        private final TimeStamp timeStampBegin = TimeStampFactory.create();
        private final TimeStamp timeStampLatest = TimeStampFactory.create();
        
        private volatile boolean isDestroyed = false;
        private final ReentrantLock connectLock = new ReentrantLock();
        private final ReentrantLock monitorLock = new ReentrantLock();
        private final Condition waitForConnect = connectLock.newCondition();
        private volatile Status status = statusCreate.getStatusOK();
        
        EMultiMonitor(EasyMultiChannel easyMultiChannel,Channel[] channel,PVStructure pvRequest,Union union) {
            this.easyMultiChannel = easyMultiChannel;
            this.channel = channel;
            this.pvRequest = pvRequest;
            nchannel = channel.length;
            isMonitorConnected = new boolean[nchannel];
            monitorRequester = new MonitorRequesterPvt[nchannel];
            monitor = new Monitor[nchannel];
            monitorElement = new MonitorElement[nchannel];
            for(int i=0; i<nchannel; ++i) {
                isMonitorConnected[i] = false;
                monitorRequester[i] = new MonitorRequesterPvt(this,i);
                monitorElement[i] = null;
                if(channel[i].isConnected()) ++numMonitorToConnect;
            }
            easyMultiData = easyMultiChannel.createEasyMultiData(pvRequest, union);
            
        }
        
        public void destroy() {
            synchronized (this) {
               if(isDestroyed) return;
               isDestroyed = true;
            }
            for(int i=0; i<nchannel; ++i){
                if(monitor[i]!=null) monitor[i].destroy();
            }
        }
        
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiMonitor#connect()
         */
        public boolean connect() {
            issueConnect();
            return waitConnect();
        }
       
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiMonitor#issueConnect()
         */
        public void issueConnect() {
            if(isDestroyed) return;
            if(connectState!=ConnectState.connectIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"connect already issued",null);
                setStatus(status);
                return;
            }
            numConnectCallback = 0;
            connectState = ConnectState.connectActive;
            for(int i=0; i<channel.length; ++i) {
                if(channel[i].isConnected()) {
                    channel[i].createMonitor(monitorRequester[i], pvRequest);
                }
            }
        }
       
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiMonitor#waitConnect()
         */
        @Override
        public boolean waitConnect() {
            if(isDestroyed) return false;
            try {
                connectLock.lock();
                try {
                    if(numConnectCallback<numMonitorToConnect) waitForConnect.await();
                } catch(InterruptedException e) {
                    Status status = statusCreate.createStatus(
                            StatusType.ERROR,
                            e.getMessage(),
                            e.fillInStackTrace());
                    setStatus(status);
                    return false;
                }
            } finally {
                connectLock.unlock();
            }
            if(numMonitorConnected!=numMonitorToConnect) {
                Status status = statusCreate.createStatus(StatusType.ERROR," did not connect",null);
                setStatus(status);
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiMonitor#setRequester(org.epics.pvaccess.easyPVA.EasyMultiMonitor.EasyMultiRequester)
         */
        public void setRequester(EasyMultiRequester requester) {
            multiRequester = requester;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiMonitor#start(double)
         */
        public boolean start(double waitBetweenEvents) {
            if(connectState==ConnectState.connectIdle) connect();
            if(connectState!=ConnectState.connectDone) {
                Status status = statusCreate.createStatus(StatusType.ERROR,"not connected",null);
                setStatus(status);
                return false;
            }
            if(monitorState!=MonitorState.monitorIdle) {
                Status status = statusCreate.createStatus(StatusType.ERROR,"not idle",null);
                setStatus(status);
                return false;
            }
            monitorState= MonitorState.monitorActive;
            this.waitBetweenEvents = waitBetweenEvents;
            timeStampBegin.getCurrentTime();
            easyMultiData.startDeltaTime();
            for(int i=0; i<nchannel; ++i) {
                if(isMonitorConnected[i]) monitor[i].start();
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiMonitor#stop()
         */
        public boolean stop() {
            if(monitorState!=MonitorState.monitorActive) {
                Status status = statusCreate.createStatus(StatusType.ERROR,"not active",null);
                setStatus(status);
                return false;
            }
            monitorState= MonitorState.monitorIdle;
            for(int i=0; i<nchannel; ++i) {
                if(isMonitorConnected[i]) monitor[i].stop();
            }
            return true;
        }
        public MonitorElement[] getMonitorElement()
        {
            return monitorElement;
        }
        @Override
        public int poll() {
            monitorLock.lock();
            try {
                if(monitorState!=MonitorState.monitorActive) {
                    throw new IllegalStateException("monitor is not owner of elements");
                }
                int num = 0;
                monitorState = MonitorState.monitorClientActive;
                for(int i=0; i<nchannel; ++i) {
                    if(isMonitorConnected[i]) {
                        if(monitorElement[i]!=null) ++num;
                    }
                }
                if(num==0) monitorState = MonitorState.monitorActive;
                easyMultiData.endDeltaTime();
                return num;
            } finally {
                monitorLock.unlock();
            }
        }
        @Override
        public boolean release() {
            monitorLock.lock();
            try {
                if(monitorState!=MonitorState.monitorClientActive) {
                    throw new IllegalStateException("user is not owner of elements");
                }
                boolean moreMonitors = false;

                for(int i=0; i < nchannel; ++i) {
                    if(monitorElement[i]!=null) {
                        monitor[i].release(monitorElement[i]);
                        monitorElement[i] = null;
                    }
                }
                easyMultiData.startDeltaTime();
                if(numMonitors>0) moreMonitors = true;
                numMonitors = 0;
                monitorState = MonitorState.monitorActive;
                return moreMonitors;
            } finally {
                monitorLock.unlock();
            }
        }
       
        @Override
        public TimeStamp getTimeStamp() {
            return easyMultiData.getTimeStamp();
        }
        
        @Override
        public int getLength() {
            return nchannel;
        }

        
        @Override
        public boolean doubleOnly() {
            return easyMultiData.doubleOnly();
        }
        
       
        @Override
        public PVStructure getNTMultiChannel() {
            if(monitorState!=MonitorState.monitorClientActive) {
                setStatus(clientNotActive);
                return null;
            }
            return easyMultiData.getNTMultiChannel();
        }
        
        @Override
        public PVStructure getPVTop() {
            if(monitorState!=MonitorState.monitorClientActive) {
                setStatus(clientNotActive);
                return null;
            }
            return easyMultiData.getPVTop();
        }

        public double[] getDoubleArray() {
            if(monitorState!=MonitorState.monitorClientActive) {
                setStatus(clientNotActive);
                return null;
            }
            return easyMultiData.getDoubleArray();
        }

        public int getDoubleArray(int offset, double[] data, int length) {
            if(monitorState!=MonitorState.monitorClientActive) {
                setStatus(clientNotActive);
                return 0;
            }
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
        
        private String getRequesterName() {
            return easyMultiChannel.getRequesterName();
        }
        
        private void message(String message, MessageType messageType) {
            if(isDestroyed) return;
            easyMultiChannel.message(message, messageType);
        }
        private void monitorConnect(
                Status status,
                Monitor monitor,
                Structure structure,
                int index)
        {
            if(isDestroyed) return;
            
            this.monitor[index] = monitor;
            if(status.isOK()) {  
                if(!isMonitorConnected[index]) {
                    ++numMonitorConnected;
                    isMonitorConnected[index] = true;
                    easyMultiData.setStructure(structure, index);
                }
            } else {
                if(isMonitorConnected[index]) {
                    --numMonitorConnected;
                    isMonitorConnected[index] = false;
                    setStatus(status);
                }
            }
            if(connectState!=ConnectState.connectActive) return;
            connectLock.lock();
            try {
                numConnectCallback++;
                if(numConnectCallback==numMonitorToConnect) {
                    connectState = ConnectState.connectDone;
                    waitForConnect.signal();
                }
            } finally {
                connectLock.unlock();
            }
        }
        
        private void monitorEvent(int indChannel) {
            if(isDestroyed) return;
            boolean callRequester = false;
            monitorLock.lock();
            try {
                ++numMonitors;
                if(numMonitors==nchannel) callRequester=true;
                if(monitorState!=MonitorState.monitorActive) return;
                if(monitorElement[indChannel]!=null) return;
                monitorElement[indChannel] = monitor[indChannel].poll();
                MonitorElement element = monitorElement[indChannel];
                if(element!=null) {
                    easyMultiData.setPVStructure(
                            element.getPVStructure(),element.getChangedBitSet(),indChannel);
                }
                timeStampLatest.getCurrentTime();
                double diff = timeStampLatest.diff(timeStampLatest, timeStampBegin);
                if(diff>=waitBetweenEvents) callRequester = true;
            } finally {
                monitorLock.unlock();
            }
            if(callRequester&&(multiRequester!=null)) multiRequester.event(this);
        }
        
        private void lostChannel(int indChannel) {
            monitorLock.lock();
            try {
            isMonitorConnected[indChannel] = false;
            monitor[indChannel] = null;
            monitorElement[indChannel] = null;
            } finally {
                monitorLock.unlock();
            }
        }
    }
    
}
