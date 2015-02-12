package org.epics.pvaccess.easyPVA;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.client.Channel;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Structure;

public class EasyMonitorImpl {
    
    /**
     * Factory for creating a new EasyPVStructure.
     * @return The interface.
     */
    static EasyMonitor create(EasyChannel easyChannel, Channel channel, PVStructure pvRequest) {
        return new EMonitor(easyChannel,channel,pvRequest);
    }

    private static class EMonitor implements EasyMonitor, MonitorRequester {
        private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
        
        private final EasyChannel easyChannel;
        private final Channel channel;
        private final PVStructure pvRequest;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waitForConnect = lock.newCondition();
        
        private volatile boolean isDestroyed = false;
        private volatile Status status = statusCreate.getStatusOK();
        private volatile Monitor monitor = null;
        private volatile MonitorElement monitorElement = null;
        private volatile EasyRequester easyRequester = null;
        
        private enum ConnectState {connectIdle,notConnected,connected};
        private volatile ConnectState connectState = ConnectState.connectIdle;
        private volatile boolean waitingForConnect = false;
        
        private boolean checkConnected() {
            if(connectState==ConnectState.connectIdle) connect();
            if(connectState==ConnectState.connected) return true;
            if(connectState==ConnectState.notConnected) {
                String message = channel.getChannelName() + " monitor not connected";
                Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
                setStatus(status);
                return false;
            }
            String message = channel.getChannelName() + " illegal monitor state";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
            return false;

        }

        
        private volatile boolean isStarted = false;
        
        EMonitor(EasyChannel easyChannel, Channel channel, PVStructure pvRequest)
        {
            this.easyChannel = easyChannel;
            this.channel = channel;
            this.pvRequest = pvRequest;
        }
        @Override
        public String getRequesterName() {
            return easyChannel.getChannelName();
        }
        @Override
        public void message(String message, MessageType messageType) {
            if(isDestroyed) return;
            easyChannel.message(message, messageType);
        }
        @Override
        public void monitorConnect(
            Status status,
            Monitor monitor,
            Structure structure)
        {
            if(isDestroyed) return;
            this.monitor = monitor;
            if(!status.isSuccess()) {
                setStatus(status);
                connectState = ConnectState.notConnected;
            } else {
                connectState = ConnectState.connected;
            }
            lock.lock();
            try {
                if(waitingForConnect) waitForConnect.signal();
            } finally {
               lock.unlock();
            }
        }
        @Override
        public void monitorEvent(Monitor monitor) {
            if(isDestroyed) return;
            if(easyRequester!=null) easyRequester.event(this);
        }
        @Override
        public void unlisten(Monitor monitor) {
            destroy();
        }
        @Override
        public void destroy() {
            synchronized (this) {
                if(isDestroyed) return;
                isDestroyed = true;
             }
            if(monitor!=null) monitor.destroy();
            monitorElement = null;
        }
        @Override
        public boolean connect() {
            issueConnect();
            return waitConnect();
        }
        @Override
        public void issueConnect() {
            if(isDestroyed) return;
            if(connectState!=ConnectState.connectIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"connect already issued",null);
                setStatus(status);
                return;
            }
            monitor = channel.createMonitor(this, pvRequest);
        }
        @Override
        public boolean waitConnect() {
            if(isDestroyed) return false;
            try {
                lock.lock();
                try {
                    waitingForConnect = true;
                    if(connectState==ConnectState.connectIdle) waitForConnect.await();
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
            if(connectState==ConnectState.notConnected) return false;
            return true;
        }
        @Override
        public void setRequester(EasyRequester requester) {
            easyRequester = requester;
        }
        @Override
        public boolean start() {
            if(!checkConnected()) return false;
            if(isStarted) {
                String message = channel.getChannelName() + "already started";
                Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
                setStatus(status);
                return false;
            }
            isStarted = true;
            monitorElement = null;
            monitor.start();
            return true;
        }
        @Override
        public boolean stop() {
            if(!checkConnected()) return false;
            if(isStarted) {
                String message = channel.getChannelName() + "not started";
                Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
                setStatus(status);
                return false;
            }
            isStarted = false;
            monitor.stop();
            monitorElement = null;
            return true;
        }
        @Override
        public MonitorElement poll() {
            if(!isStarted) {
                if(!start()) return null;
            }
            monitorElement = monitor.poll();
            return monitorElement;
        }
        @Override
        public boolean releaseEvent() {
            if(monitorElement==null) {
                String message = channel.getChannelName() + "no monitorElement";
                Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
                setStatus(status);
                return false;
            }
            monitor.release(monitorElement);
            monitorElement = null;
            return true;
        }
        @Override
        public void setStatus(Status status) {
            this.status = status;
            easyChannel.setStatus(status);
        }
        @Override
        public Status getStatus() {
            Status save = status;
            status = statusCreate.getStatusOK();
            return save;
        }
    }
}
