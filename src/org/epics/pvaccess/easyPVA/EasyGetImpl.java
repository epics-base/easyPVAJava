package org.epics.pvaccess.easyPVA;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.ChannelGet;
import org.epics.pvaccess.client.ChannelGetRequester;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Structure;

public class EasyGetImpl {
    
    /**
     * Factory for creating a new EasyPVStructure.
     * @return The interface.
     */
    static EasyGet create(EasyChannel easyChannel, Channel channel, PVStructure pvRequest) {
        return new EGet(easyChannel,channel,pvRequest);
    }

    private static class EGet implements EasyGet, ChannelGetRequester {
        private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
        private static final EasyPVA easyPVA = EasyPVAFactory.get();
        private final EasyChannel easyChannel;
        private final Channel channel;
        private final PVStructure pvRequest;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waitForConnect = lock.newCondition();
        private final Condition waitForGet = lock.newCondition();
        private final EasyPVStructure easyPVStructure = EasyPVStructureImpl.create();
 
        private volatile boolean isDestroyed = false;
        private volatile Status status = statusCreate.getStatusOK();
        private volatile ChannelGet channelGet = null;
        private volatile BitSet bitSet = null;
        
        private enum ConnectState {connectIdle,notConnected,connected};
        private volatile ConnectState connectState = ConnectState.connectIdle;
        private volatile boolean waitingForConnect = false;

       
        
        private boolean checkConnected() {
            if(connectState==ConnectState.connectIdle) connect();
            if(connectState==ConnectState.connected) return true;
            if(connectState==ConnectState.notConnected) {
                String message = channel.getChannelName() + " get not connected";
                Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
                setStatus(status);
                return false;
            }
            String message = channel.getChannelName() + " illegal getConnect state";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
            return false;

        }

        private enum GetState {getIdle,getActive,getDone};
        private volatile GetState getState = GetState.getIdle;
        private volatile boolean getSuccess = false;
        
        private boolean checkGetState() {
            if(!checkConnected()) return false;
            if(getSuccess) return true;
            if(!easyPVA.isAutoGet()) return false;
            if(getState==GetState.getIdle) get();
            if(getState==GetState.getIdle) return true;
            String message = channel.getChannelName() + " illegal get state";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
            return false;
        }
         
        EGet(EasyChannel easyChannel,Channel channel,PVStructure pvRequest) {
            this.easyChannel = easyChannel;
            this.channel = channel;
            this.pvRequest = pvRequest;
        }
        @Override
        public void setPVStructure(PVStructure pvStructure) {
            throw new IllegalArgumentException("EasyGet does not allow this method");
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
         * @see org.epics.pvaccess.client.ChannelGetRequester#channelGetConnect(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelGet, org.epics.pvdata.pv.Structure)
         */
        @Override
        public void channelGetConnect(Status status, ChannelGet channelGet, Structure structure) {
            if(isDestroyed) return;
            this.channelGet = channelGet;
            if(!status.isSuccess()) {
                setStatus(status);
                connectState = ConnectState.notConnected;
            } else {
                connectState = ConnectState.connected;
            }
            // signal
            lock.lock();
            try {
                if(waitingForConnect) waitForConnect.signal();
            } finally {
               lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelGetRequester#getDone(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelGet, org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
         */
        @Override
        public void getDone(Status status, ChannelGet channelGet, PVStructure pvStructure, BitSet bitSet) { 
            easyPVStructure.setPVStructure(pvStructure);
            this.bitSet = bitSet;
            if(!status.isSuccess()) {
                setStatus(status);
                getSuccess = false;
            } else {
                getSuccess = true;
            }
        
            lock.lock();
            try {
               getState = GetState.getDone;
               waitForGet.signal();
            } finally {
               lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyGet#destroy()
         */
        @Override
        public void destroy() {
            synchronized (this) {
               if(isDestroyed) return;
               isDestroyed = true;
            }
            if(channelGet!=null) channelGet.destroy();
            easyPVStructure.setPVStructure(null);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyGet#connect()
         */
        @Override
        public boolean connect() {
            issueConnect();
            return waitConnect();
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyGet#issueConnect()
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
            channelGet = channel.createChannelGet(this, pvRequest);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyGet#waitConnect()
         */
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
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyGet#get()
         */
        @Override
        public boolean get() {
            issueGet();
            return waitGet();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyGet#issueGet()
         */
        @Override
        public void issueGet() {
            if(isDestroyed) return;
            if(!checkConnected()) return;
            if(getState!=GetState.getIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"get already issued",null);
                setStatus(status);
                return;
            }
            getState = GetState.getActive;
            channelGet.get();
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyGet#waitGet()
         */
        @Override
        public boolean waitGet() {
            if(isDestroyed) return false;
            if(!checkConnected()) return false;
            try {
                lock.lock();
                try {
                    if(getState==GetState.getActive) waitForGet.await();
                } catch(InterruptedException e) {
                    Status status = statusCreate.createStatus(StatusType.ERROR, e.getMessage(), e.fillInStackTrace());
                    setStatus(status);
                    getSuccess = false;
                }
            } finally {
                lock.unlock();
            }
            getState = GetState.getIdle;
            return getSuccess;
        }
       
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVStructure#getValue()
         */
        public PVField getValue() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getValue();
        }
        
        public PVScalar getScalarValue() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getScalarValue();
        }
        
        public PVArray getArrayValue() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getArrayValue();
        }
        
        public PVScalarArray getScalarArrayValue() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getScalarArrayValue();
        }
       
        public Alarm getAlarm() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getAlarm();
        }
       
        public TimeStamp getTimeStamp() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getTimeStamp();
        }
        
        public boolean hasValue() {
            return easyPVStructure.hasValue();
        }
        
        public boolean isValueScalar() {
            return easyPVStructure.isValueScalar();
        }
 
        public boolean getBoolean() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return false;
            }
            return easyPVStructure.getBoolean();
        }

        public byte getByte() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getByte();
        }


        public short getShort() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getShort();
        }

        public int getInt() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getInt();
        }

        public long getLong() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getLong();
        }

        public float getFloat() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getFloat();
        }

        public double getDouble() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getDouble();
        }

        public String getString() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getString();
        }
       
        public boolean[] getBooleanArray() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getBooleanArray();
        }

       
        public byte[] getByteArray() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getByteArray();
        }

       
        public short[] getShortArray() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getShortArray();
        }

        public int[] getIntArray() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getIntArray();   
        }

       
        public long[] getLongArray() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getLongArray();
        }

        public float[] getFloatArray() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getFloatArray();
        }

        public double[] getDoubleArray() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getDoubleArray();
        }

        public String[] getStringArray() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getStringArray();
        }

        public int getBooleanArray(boolean[] value, int length) {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getBooleanArray(value,length);
        }

       
        public int getByteArray(byte[] value, int length) {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getByteArray(value,length);
        }

        public int getShortArray(short[] value, int length) {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getShortArray(value,length);
        }

        public int getIntArray(int[] value, int length) {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getIntArray(value,length);
        }

       
        public int getLongArray(long[] value, int length) {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getLongArray(value,length);
        }

        
        public int getFloatArray(float[] value, int length) {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getFloatArray(value,length);
        }

        
        public int getDoubleArray(double[] value, int length) {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getDoubleArray(value,length);
        }
        
        public int getStringArray(String[] value, int length) {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return 0;
            }
            return easyPVStructure.getStringArray(value,length);
        }

        public PVStructure getPVStructure() {
            if(!checkGetState()) {
                setStatus(easyPVStructure.getStatus());
                return null;
            }
            return easyPVStructure.getPVStructure();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyGet#getBitSet()
         */
        @Override
        public BitSet getBitSet() {
            if(!checkGetState()) return null;
            return bitSet;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyGet#setStatus(org.epics.pvdata.pv.Status)
         */
        @Override
        public void setStatus(Status status) {
            this.status = status;
            easyChannel.setStatus(status);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyGet#getStatus()
         */
        @Override
        public Status getStatus() {
            if(status.isOK()) return easyPVStructure.getStatus();
            Status save = status;
            status = statusCreate.getStatusOK();
            return save;
        }

    }
    
}
