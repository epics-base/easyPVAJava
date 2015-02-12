package org.epics.pvaccess.easyPVA;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.ChannelPut;
import org.epics.pvaccess.client.ChannelPutRequester;
import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVUnion;
import org.epics.pvdata.pv.PVUnionArray;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Type;
import org.epics.pvdata.pv.UnionArrayData;

public class EasyMultiPutImpl {
    
    /**
     * Factory for creating a new EasyPVStructure.
     * @return The interface.
     */
    static EasyMultiPut create(
        EasyMultiChannel easyMultiChannel,
        Channel[] channel,
        boolean doubleOnly) {
        return new EMultiPut(easyMultiChannel,channel,doubleOnly);
    }

    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Convert convert = ConvertFactory.getConvert();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    
    private static class EMultiPut implements EasyMultiPut, ChannelPutRequester {
        private final EasyMultiChannel easyMultiChannel;
        private final Channel[] channel;
        private boolean doubleOnly;
        private final int nchannel;
        
        // following initialized by init and 
        
        private volatile PVStructure[] topPVStructure = null;
        private volatile BitSet[] putBitSet = null;
        private volatile ChannelPut[] channelPut = null;
        private EasyMultiGet easyMultiGet = null;
        
        
     // following used by connect
        private volatile int numConnectCallback = 0;
        private volatile int numConnected = 0;
        private volatile boolean[] isConnected = null;
        private enum ConnectState {connectIdle,connectActive,connectDone};
        private volatile ConnectState connectState = ConnectState.connectIdle;
        
        // following used by put
        private volatile int numPut = 0;
        private volatile boolean badPut = false;
        private volatile boolean illegalPut = false;
        private enum PutState {putIdle,putActive,putFailed,putDone};
        private volatile PutState putState = PutState.putIdle;
        
        private volatile boolean isDestroyed = false;
        private final PVStructure pvRequest = CreateRequest.create().createRequest("field(value)");
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waitForConnect = lock.newCondition();
        private final Condition waitForPut = lock.newCondition();
        private volatile Status status = statusCreate.getStatusOK();
        private volatile UnionArrayData unionArrayData = new UnionArrayData();


        
        
        EMultiPut(EasyMultiChannel easyMultiChannel,Channel[] channel,Boolean doubleOnly) {
            this.easyMultiChannel = easyMultiChannel;
            this.channel = channel;
            this.doubleOnly = doubleOnly;
            nchannel = channel.length;
            isConnected = new boolean[nchannel];
            for(int i=0; i<nchannel; ++i) isConnected[i] = false;
        }
        
        public boolean init()
        {
            channelPut = new ChannelPut[nchannel];
            topPVStructure = new PVStructure[nchannel];
            easyMultiGet = easyMultiChannel.createGet(doubleOnly, "field(value)");
            putBitSet = new BitSet[nchannel];
            return true;
        }

        private void checkConnected() {
            if(connectState==ConnectState.connectIdle) connect();
        }
        
        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.Requester#getRequesterName()
         */
        @Override
        public String getRequesterName() {
            return easyMultiChannel.getRequesterName();
        }

        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.Requester#message(java.lang.String, org.epics.pvdata.pv.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            if(isDestroyed) return;
            easyMultiChannel.message(message, messageType);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelPutRequester#channelPutConnect(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelPut, org.epics.pvdata.pv.Structure)
         */
        @Override
        public void channelPutConnect(Status status, ChannelPut channelPut,Structure structure) {
            if(isDestroyed) return;
            int index = -1;
            for(int i=0; i<channel.length; ++i) {
                if(easyMultiChannel.getChannelNames()[i].equals(channelPut.getChannel().getChannelName())) {
                    index = i;
                    break;
                }
            }
            if(index<0) {
                throw new IllegalStateException("should not happen");
            }
            this.channelPut[index] = channelPut;
            if(status.isOK()) {
                if(!isConnected[index]) {
                    ++numConnected;
                    isConnected[index] = true;
                    topPVStructure[index] = pvDataCreate.createPVStructure(structure);
                    putBitSet[index] = new BitSet(topPVStructure[index].getNumberFields());
                    Field field = structure.getField("value");
                    if(field==null) {
                        setStatus(statusCreate.createStatus(
                                StatusType.ERROR,"channel " + channel[index].getChannelName()
                                +" does not have top level value field",null));
                    } else {
                        boolean success= true;
                        if(doubleOnly) {
                            if(field.getType()!=Type.scalar) {
                                success = false;
                            } else {
                                Scalar scalar = (Scalar)field;
                                if(!scalar.getScalarType().isNumeric()) success = false;
                            }
                            if(!success) {
                                setStatus(statusCreate.createStatus(
                                        StatusType.ERROR,"channel value is not a numeric scalar",null));
                            }
                        }
                    }
                }
            } else {
                if(isConnected[index]) {
                    --numConnected;
                    isConnected[index] = false;
                    setStatus(status);
                }
            }
            if(connectState!=ConnectState.connectActive) return;
            lock.lock();
            try {
                numConnectCallback++;
                if(numConnectCallback==nchannel) {
                    connectState = ConnectState.connectDone;
                    waitForConnect.signal();
                }
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelPutRequester#putDone(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelPut)
         */
        @Override
        public void putDone(Status status, ChannelPut channelPut) {
            int index = -1;
            for(int i=0; i<channel.length; ++i) {
                if(easyMultiChannel.getChannelNames()[i].equals(channelPut.getChannel().getChannelName())) {
                    index = i;
                    break;
                }
            }
            if(index<0) {
                throw new IllegalStateException("should not happen");
            }
            if(!status.isOK()) {
                badPut = true;
            }
            lock.lock();
            try {
                ++numPut;
                if(numPut==nchannel) {
                    if(badPut) {
                        putState = PutState.putFailed;
                    } else {
                        putState = PutState.putDone;
                    }
                    waitForPut.signal();
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelPutRequester#getDone(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelPut, org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
         */
        @Override
        public void getDone(Status status, ChannelPut channelPut,PVStructure pvStructure, BitSet bitSet) {
            // using EasyMultiGet so this not used.
            
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#destroy()
         */
        @Override
        public void destroy() {
            synchronized (this) {
                if(isDestroyed) return;
                isDestroyed = true;
             }
            easyMultiGet.destroy();
            for(int i=0; i<nchannel; ++i) {
                if(channelPut[i]!=null) channelPut[i].destroy();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#connect()
         */
        @Override
        public boolean connect() {
            issueConnect();
            return waitConnect();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#issueConnect()
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
            connectState = ConnectState.connectActive;
            for(int i=0; i<channel.length; ++i) channelPut[i] = channel[i].createChannelPut(this, pvRequest);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#waitConnect()
         */
        @Override
        public boolean waitConnect() {
            if(isDestroyed) return false;
            try {
                lock.lock();
                try {
                    if(numConnectCallback<nchannel) waitForConnect.await();
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
            if(numConnected!=nchannel) {
                Status status = statusCreate.createStatus(StatusType.ERROR," did not connect",null);
                setStatus(status);
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#get()
         */
        @Override
        public boolean get() {
            return easyMultiGet.get();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#issueGet()
         */
        @Override
        public void issueGet() {
            easyMultiGet.issueGet();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#waitGet()
         */
        @Override
        public boolean waitGet() {
            return easyMultiGet.waitGet();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#getLength()
         */
        @Override
        public int getLength() {
            return nchannel;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#doubleOnly()
         */
        @Override
        public boolean doubleOnly() {
            return doubleOnly;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#getNTMultiChannel()
         */
        @Override
        public PVStructure getNTMultiChannel() {
            if(doubleOnly) return null;
            return easyMultiGet.getNTMultiChannel();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#getPVTop()
         */
        @Override
        public PVStructure getPVTop() {
            return easyMultiGet.getPVTop();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#getDoubleArray()
         */
        @Override
        public double[] getDoubleArray() {
            return easyMultiGet.getDoubleArray();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#getDoubleArray(int, double[], int)
         */
        @Override
        public int getDoubleArray(int index, double[] data, int length) {
            return easyMultiGet.getDoubleArray(index,data,length);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#put(org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public boolean put(PVStructure pvNTMultiChannel) {
            issuePut(pvNTMultiChannel);
            return waitPut();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#issuePut(org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public void issuePut(PVStructure pvNTMultiChannel) {
            if(doubleOnly) {
                illegalPut = true;
                return;
            }
            checkConnected();
            if(putState!=PutState.putIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"put already issued",null);
                setStatus(status);
                return;
            }
            boolean allConnected = true;
            for(int i=0; i<nchannel; ++i) if(!channelPut[i].getChannel().isConnected()) allConnected = false;
            if(!allConnected) {
                badPut = true;
                return;
            }
            illegalPut = false;
            numPut = 0;
            badPut = false;
            putState = PutState.putActive;
            PVUnionArray pvArray = pvNTMultiChannel.getUnionArrayField("value");
            pvArray.get(0, nchannel, unionArrayData);
            for(int i=0; i<nchannel; ++i) {
                PVStructure top = topPVStructure[i];
                PVField pvTo = top.getSubField("value");
                PVUnion pvUnion = unionArrayData.data[i];
                PVField pvFrom = pvUnion.get();
                if(convert.isCopyCompatible(pvFrom.getField(),pvTo.getField())) {
                    convert.copy(pvFrom, pvTo);
                    putBitSet[i].clear();
                    putBitSet[i].set(0);
                    channelPut[i].put(top, putBitSet[i]);
                } else {
                    String message = "channel " + channel[i].getChannelName();
                    message += " can not copy value";
                    setStatus(statusCreate.createStatus(StatusType.ERROR,message,null));
                }
            }
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#put(double[])
         */
        @Override
        public boolean put(double[] value) {
            issuePut(value);
            return waitPut();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#issuePut(double[])
         */
        @Override
        public void issuePut(double[] value) {
            if(!doubleOnly) {
                illegalPut = true;
                return;
            }
            checkConnected();
            if(putState!=PutState.putIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"put already issued",null);
                setStatus(status);
                return;
            }
            boolean allConnected = true;
            for(int i=0; i<nchannel; ++i) if(!channelPut[i].getChannel().isConnected()) allConnected = false;
            if(!allConnected) {
                badPut = true;
                return;
            }
            illegalPut = false;
            numPut = 0;
            badPut = false;
            putState = PutState.putActive;
            for(int i=0; i<nchannel; ++i) {
                PVStructure top = topPVStructure[i];
                PVScalar pvScalar = top.getSubField(PVScalar.class,"value");
                convert.fromDouble(pvScalar,value[i]);
                putBitSet[i].clear();
                putBitSet[i].set(0);
                channelPut[i].put(top, putBitSet[i]);
            }
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#waitPut()
         */
        @Override
        public boolean waitPut() {
            if(isDestroyed) return false;
            checkConnected();
            if(illegalPut) {
                Status status = statusCreate.createStatus(StatusType.ERROR,"illegal put request", null);
                setStatus(status);
                return false;
            }
            try {
                lock.lock();
                try {
                    if(putState==PutState.putActive) waitForPut.await();
                } catch(InterruptedException e) {
                    Status status = statusCreate.createStatus(StatusType.ERROR, e.getMessage(), e.fillInStackTrace());
                    setStatus(status);
                    return false;
                }
            } finally {
                lock.unlock();
            }
            putState = PutState.putIdle;
            if(badPut) {
                Status status = statusCreate.createStatus(StatusType.ERROR," put failed",null);
                setStatus(status);
                return false;
            }
            return true;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#setStatus(org.epics.pvdata.pv.Status)
         */
        @Override
        public void setStatus(Status status) {
            this.status = status;
            easyMultiChannel.setStatus(status);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiPut#getStatus()
         */
        @Override
        public Status getStatus() {
            Status save = status;
            status = statusCreate.getStatusOK();
            return save;
        }
        
    }
    
}
