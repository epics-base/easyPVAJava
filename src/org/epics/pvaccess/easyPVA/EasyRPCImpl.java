package org.epics.pvaccess.easyPVA;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.ChannelRPC;
import org.epics.pvaccess.client.ChannelRPCRequester;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;

public class EasyRPCImpl {
    
    /**
     * Factory for creating a new EasyPVStructure.
     * @return The interface.
     */
    static EasyRPC create(EasyChannel easyChannel, Channel channel, PVStructure pvRequest) {
        return new ERPC(easyChannel,channel,pvRequest);
    }
    
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    
    private static class ERPC implements EasyRPC, ChannelRPCRequester {
        private final EasyChannel easyChannel;
        private final Channel channel;
        private final PVStructure pvRequest;
        
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waitForConnect = lock.newCondition();
        private final Condition waitForRPC = lock.newCondition();

        private volatile boolean isDestroyed = false;
        
        private volatile Status status = statusCreate.getStatusOK();
        private volatile ChannelRPC channelRPC = null;
        private volatile PVStructure result = null;
        
        private enum ConnectState {connectIdle,notConnected,connected};
        private volatile ConnectState connectState = ConnectState.connectIdle;
        
        private boolean checkConnected() {
            if(connectState==ConnectState.connectIdle) connect();
            if(connectState==ConnectState.connected) return true;
            if(connectState==ConnectState.notConnected) {
                String message = channel.getChannelName() + " rpc not connected";
                Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
                setStatus(status);
                return false;
            }
            String message = channel.getChannelName() + " illegal rpcConnect state";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
            return false;
    
        }
    
        private enum RPCState {rpcIdle,rpcActive,rpcDone};
        private RPCState rpcState = RPCState.rpcIdle;
        
        ERPC(EasyChannel easyChannel,Channel channel,PVStructure pvRequest) {
            this.easyChannel = easyChannel;
            this.channel = channel;
            this.pvRequest = pvRequest;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.Requester#getRequesterName()
         */
        @Override
        public String getRequesterName() {
            return easyChannel.getRequesterName();
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.Requester#message(java.lang.String, org.epics.pvdata.pv.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            if(isDestroyed) return;
            easyChannel.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelRPCRequester#channelRPCConnect(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelRPC)
         */
        @Override
        public void channelRPCConnect(Status status, ChannelRPC channelRPC) {
            if(isDestroyed) return;
            this.channelRPC = channelRPC;
            this.status = status;
            if(!status.isSuccess()) {
                connectState = ConnectState.notConnected;
            } else {
                connectState = ConnectState.connected;
            }
            lock.lock();
            try {
               waitForConnect.signal();
            } finally {
               lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelRPCRequester#requestDone(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelRPC, org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public void requestDone(Status status, ChannelRPC channelRPC, PVStructure result) {
            this.status = status;
            this.result = result;
            rpcState = RPCState.rpcDone;
            lock.lock();
            try {
               waitForRPC.signal();
            } finally {
               lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyRPC#destroy()
         */
        @Override
        public void destroy() {
            synchronized (this) {
               if(isDestroyed) return;
               isDestroyed = true;
            }
            channelRPC.destroy();
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyRPC#connect()
         */
        @Override
        public boolean connect() {
            issueConnect();
            return waitConnect();
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyRPC#issueConnect()
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
            channelRPC = channel.createChannelRPC(this, pvRequest);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyRPC#waitConnect()
         */
        @Override
        public boolean waitConnect() {
            if(isDestroyed) return false;
            try {
                lock.lock();
                try {
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
            if(connectState==ConnectState.connectIdle) {
                Status status = statusCreate.createStatus(StatusType.ERROR," did not connect",null);
                setStatus(status);
                return false;
            }
            return true;
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyRPC#request(org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public PVStructure request(PVStructure request) {
            issueRequest(request);
            return waitRequest();
        }
    
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyRPC#issueRequest(org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public void issueRequest(PVStructure request) {
            if(isDestroyed) return;
            if(!checkConnected()) return;
            if(rpcState!=RPCState.rpcIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"rpc already issued",null);
                setStatus(status);
                return;
            }
            rpcState = RPCState.rpcActive;
            channelRPC.request(request);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyRPC#waitRequest()
         */
        @Override
        public PVStructure waitRequest() {
            if(isDestroyed) return null;
            if(!checkConnected()) return null;
            try {
                lock.lock();
                try {
                    if(rpcState==RPCState.rpcActive) waitForRPC.await();
                } catch(InterruptedException e) {
                    Status status = statusCreate.createStatus(StatusType.ERROR, e.getMessage(), e.fillInStackTrace());
                    setStatus(status);
                    return null;
                }
            } finally {
                lock.unlock();
            }
            if(rpcState==RPCState.rpcActive) {
                Status status = statusCreate.createStatus(StatusType.ERROR," rpc failed",null);
                setStatus(status);
                return null;
            }
            rpcState = RPCState.rpcIdle;
            return result;
        }
    
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyRPC#setStatus(org.epics.pvdata.pv.Status)
         */
        @Override
        public void setStatus(Status status) {
            this.status = status;
            easyChannel.setStatus(status);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyRPC#getStatus()
         */
        @Override
        public Status getStatus() {
            Status save = status;
            status = statusCreate.getStatusOK();
            return save;
        }
    
    }
}
