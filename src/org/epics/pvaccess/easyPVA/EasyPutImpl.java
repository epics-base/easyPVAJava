package org.epics.pvaccess.easyPVA;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.ChannelPut;
import org.epics.pvaccess.client.ChannelPutRequester;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.BooleanArrayData;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVBooleanArray;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Type;

public class EasyPutImpl {
    
    
    /**
     * Factory for creating a new EasyPVStructure.
     * @return The interface.
     */
    static EasyPut create(EasyChannel easyChannel, Channel channel, PVStructure pvRequest) {
        return new EPut(easyChannel,channel,pvRequest);
    }

    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final EasyPVA easyPVA = EasyPVAFactory.get();
    private static final Convert convert = ConvertFactory.getConvert();
    
    private static class EPut implements EasyPut, ChannelPutRequester {
        private final EasyChannel easyChannel;
        private final Channel channel;
        private final PVStructure pvRequest;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waitForConnect = lock.newCondition();
        private final Condition waitForPutOrGet = lock.newCondition();

        private volatile boolean isDestroyed = false;
        private volatile Status status = statusCreate.getStatusOK();
        private volatile ChannelPut channelPut = null;
        private volatile PVStructure pvStructure = null;
        private volatile BitSet bitSet = null;
        
        private enum ConnectState {connectIdle,notConnected,connected};
        private volatile ConnectState connectState = ConnectState.connectIdle;
        private volatile boolean waitingForConnect = false;
        
        // volatile-s!!!
        private volatile boolean hasValue;
        private volatile boolean valueIsScalar = false;
        private volatile ScalarType scalarTypeValue = null;
        private volatile boolean valueIsNumeric = false;
        
        private volatile PVField pvValue = null;
        private volatile PVScalar pvScalarValue = null;
        private volatile PVArray pvArrayValue =  null;
        private volatile PVScalarArray pvScalarArrayValue = null;
        private volatile PVBoolean pvBooleanValue = null;
        private final BooleanArrayData booleanArrayData = new BooleanArrayData();

        private boolean checkConnected() {
            if(connectState==ConnectState.connectIdle) connect();
            if(connectState==ConnectState.connected) return true;
            if(connectState==ConnectState.notConnected){
                 String message = channel.getChannelName() + " put not connected";
                 Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
                 setStatus(status);
                return false;
            }
            String message = channel.getChannelName() + " illegal putConnect state";
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
            if(putState!=PutState.putIdle) {
                String message = channel.getChannelName() + " put is active";
                Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
                setStatus(status);
                return false;
            }
            if(getState==GetState.getIdle) get();
            if(getState==GetState.getIdle) return true;
            String message = channel.getChannelName() + " illegal get state";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
            return false;
        }

        private enum PutState {putIdle,putActive,putDone};
        private volatile PutState putState = PutState.putIdle;
        private volatile boolean putSuccess = false;
        
        private boolean checkNumericScalar(boolean isGet) {
            if(isGet) {
                if(!checkGetState()) return false;
            } else {
                if(!checkConnected()) return false;
            }
            if(!valueIsScalar) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not scalar",null);
                setStatus(status);
                return false;
            }
            if(!valueIsNumeric) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not numeeric",null);
                setStatus(status);
                return false;
            }
            return true;
        }
        
        private boolean checkNumericScalarArray(boolean isGet) {
            if(isGet) {
                if(!checkGetState()) return false;
            } else {
                if(!checkConnected()) return false;
            }
            if(pvScalarArrayValue==null) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not a scalar array",null);
                setStatus(status);
                return false;
            }
            if(!valueIsNumeric) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not numeeric",null);
                setStatus(status);
                return false;
            }
            return true;
        }

        EPut(EasyChannel easyChannel,Channel channel,PVStructure pvRequest) {
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
        private void setPVStructure(PVStructure pvStructure, BitSet bitSet)
        {
            this.bitSet = bitSet;
            if (this.pvStructure != pvStructure)
            {
                this.pvStructure = pvStructure;
                pvValue = pvStructure.getSubField("value");
                if(pvValue!=null) {
                    hasValue = true;
                    if(pvValue.getField().getType()==Type.scalar) {
                        valueIsScalar = true;
                        pvScalarValue = (PVScalar)pvValue;
                        scalarTypeValue = pvScalarValue.getScalar().getScalarType();
                        if(scalarTypeValue==ScalarType.pvBoolean) {
                            pvBooleanValue = (PVBoolean)pvValue;
                        }
                        if(scalarTypeValue.isNumeric()) valueIsNumeric = true;
                    } else if(pvValue.getField().getType()==Type.scalarArray) {
                        pvArrayValue = (PVArray)pvValue;
                        pvScalarArrayValue = (PVScalarArray)pvValue;
                        scalarTypeValue = pvScalarArrayValue.getScalarArray().getElementType();
                        if(scalarTypeValue.isNumeric()) valueIsNumeric = true;
                    } else if(pvValue.getField().getType()==Type.structureArray) {
                        pvArrayValue = (PVArray)pvValue;
                    }
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelPutRequester#channelPutConnect(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelPut, org.epics.pvdata.pv.Structure)
         */
        @Override
        public void channelPutConnect(Status status, ChannelPut channelPut, Structure structure) {
            if(isDestroyed) return;
            this.channelPut = channelPut;
            this.status = status;
            if(!status.isSuccess()) {
                connectState = ConnectState.notConnected;
                setStatus(status);
            } else {
                // until get is called there is no pvStructure and bitSet
                // create one in case only put is called
                PVStructure newPVStructure = PVDataFactory.getPVDataCreate().createPVStructure(structure);
                setPVStructure(newPVStructure,
                               new BitSet(newPVStructure.getNumberFields()));
                
                connectState = ConnectState.connected;
            }
            
            lock.lock();
            try {
                if(waitingForConnect) waitForConnect.signal();
            } finally {
               lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelPutRequester#getDone(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelPut, org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
         */
        @Override
        public void getDone(Status status, ChannelPut channelPut, PVStructure pvStructure, BitSet bitSet) {
            if(!status.isSuccess()) {
                setStatus(status);
                getSuccess = false;
            } else {
                setPVStructure(pvStructure, bitSet);
                getSuccess = true;
            }
            lock.lock();
            try {
                getState = GetState.getDone;
                waitForPutOrGet.signal();
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelPutRequester#putDone(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelPut)
         */
        @Override
        public void putDone(Status status, ChannelPut channelPut) {
            if(!status.isSuccess()) {
                setStatus(status);
                putSuccess = false;
            } else {
                putSuccess = true;
            }
            lock.lock();
            try {
                putState = PutState.putDone;
                waitForPutOrGet.signal();
            } finally {
                lock.unlock();
            }
        }


        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#destroy()
         */
        @Override
        public void destroy() {
            synchronized (this) {
               if(isDestroyed) return;
               isDestroyed = true;
            }
            channelPut.destroy();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#connect()
         */
        @Override
        public boolean connect() {
            issueConnect();
            return waitConnect();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#issueConnect()
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
            channelPut = channel.createChannelPut(this, pvRequest);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#waitConnect()
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
         * @see org.epics.pvaccess.easyPVA.EasyPut#get()
         */
        @Override
        public boolean get() {
            issueGet();
            return waitGet();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#issueGet()
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
            if(putState!=PutState.putIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"put already issued",null);
                setStatus(status);
                return;
            }
            getState = GetState.getActive;
            channelPut.get();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#waitGet()
         */
        @Override
        public boolean waitGet() {
            if(isDestroyed) return false;
            if(!checkConnected()) return false;
            try {
                lock.lock();
                try {
                    if(getState==GetState.getActive) waitForPutOrGet.await();
                } catch(InterruptedException e) {
                    Status status = statusCreate.createStatus(StatusType.ERROR, e.getMessage(), e.fillInStackTrace());
                    setStatus(status);
                    return false;
                }
            } finally {
                lock.unlock();
            }
            getState = GetState.getIdle;
            return getSuccess;
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#put()
         */
        @Override
        public boolean put() {
            issuePut();
            return waitPut();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#issuePut()
         */
        @Override
        public void issuePut() {
            if(isDestroyed) return;
            if(!checkConnected()) return;
            if(!checkConnected()) return;
            if(getState!=GetState.getIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"get already issued",null);
                setStatus(status);
                return;
            }
            if(putState!=PutState.putIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"put already issued",null);
                setStatus(status);
                return;
            }
            putState = PutState.putActive;
            channelPut.put(pvStructure, bitSet);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#waitPut()
         */
        @Override
        public boolean waitPut() {
            if(isDestroyed) return false;
            if(!checkConnected()) return false;
            try {
                lock.lock();
                try {
                    if(putState==PutState.putActive) waitForPutOrGet.await();
                } catch(InterruptedException e) {
                    Status status = statusCreate.createStatus(StatusType.ERROR, e.getMessage(), e.fillInStackTrace());
                    setStatus(status);
                    return false;
                }
            } finally {
                lock.unlock();
            }
            putState = PutState.putIdle;
            return putSuccess;
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#hasValue()
         */
        @Override
        public boolean hasValue() {
            checkGetState();
            return hasValue;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#isValueScalar()
         */
        @Override
        public boolean isValueScalar() {
            checkGetState();
            return valueIsScalar;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getValue()
         */
        @Override
        public PVField getValue() {
            checkGetState();
            return pvValue;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getScalarValue()
         */
        @Override
        public PVScalar getScalarValue() {
            checkGetState();
            return pvScalarValue;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getArrayValue()
         */
        @Override
        public PVArray getArrayValue() {
            checkGetState();
            return pvArrayValue;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getScalarArrayValue()
         */
        @Override
        public PVScalarArray getScalarArrayValue() {
            checkGetState();
            return pvScalarArrayValue;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getBoolean()
         */
        @Override
        public boolean getBoolean() {
            if(!checkGetState()) return false;
            if(pvBooleanValue==null) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not boolean",null);
                setStatus(status);
                return false;
            }
            return pvBooleanValue.get();
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getByte()
         */
        @Override
        public byte getByte() {
            if(!checkNumericScalar(true)) return 0;
            return convert.toByte(pvScalarValue);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getShort()
         */
        @Override
        public short getShort() {
            if(!checkNumericScalar(true)) return 0;
            return convert.toShort(pvScalarValue);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getInt()
         */
        @Override
        public int getInt() {
            if(!checkNumericScalar(true)) return 0;
            return convert.toInt(pvScalarValue);    
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getLong()
         */
        @Override
        public long getLong() {
            if(!checkNumericScalar(true)) return 0;
            return convert.toLong(pvScalarValue);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getFloat()
         */
        @Override
        public float getFloat() {
            if(!checkNumericScalar(true)) return 0;
            return convert.toFloat(pvScalarValue);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getDouble()
         */
        @Override
        public double getDouble() {
            if(!checkNumericScalar(true)) return 0;
            return convert.toDouble(pvScalarValue);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getString()
         */
        @Override
        public String getString() {
            if(!checkGetState()) return null;
            if(valueIsScalar) {
               return convert.toString(pvScalarValue);
            }
            if(pvValue!=null) {
               return pvValue.toString();
            }
            return "not scalar or scalarArray";
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putBoolean(boolean)
         */
        @Override
        public boolean putBoolean(boolean value) {
            if(!checkConnected()) return false;
            if(pvBooleanValue==null) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not boolean",null);
                setStatus(status);
                return false;
            }
            pvBooleanValue.put(value);
            bitSet.set(pvBooleanValue.getFieldOffset());
            if(easyPVA.isAutoPut()) return put();
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putByte(byte)
         */
        @Override
        public boolean putByte(byte value) {
            if(!checkNumericScalar(false)) return false;
            convert.fromByte(pvScalarValue,value);
            bitSet.set(pvScalarValue.getFieldOffset());
            if(easyPVA.isAutoPut()) return put();
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putShort(short)
         */
        @Override
        public boolean putShort(short value) {
            if(!checkNumericScalar(false)) return false;
            convert.fromShort(pvScalarValue,value);
            bitSet.set(pvScalarValue.getFieldOffset());
            return put();
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putInt(int)
         */
        @Override
        public boolean putInt(int value) {
            if(!checkNumericScalar(false)) return false;
            convert.fromInt(pvScalarValue,value);
            bitSet.set(pvScalarValue.getFieldOffset());
            if(easyPVA.isAutoPut()) return put();
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putLong(long)
         */
        @Override
        public boolean putLong(long value) {
            if(!checkNumericScalar(false)) return false;
            convert.fromLong(pvScalarValue,value);
            bitSet.set(pvScalarValue.getFieldOffset());
            if(easyPVA.isAutoPut()) return put();
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putFloat(float)
         */
        @Override
        public boolean putFloat(float value) {
            if(!checkNumericScalar(false)) return false;
            convert.fromFloat(pvScalarValue,value);
            bitSet.set(pvScalarValue.getFieldOffset());
            if(easyPVA.isAutoPut()) return put();
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putDouble(double)
         */
        @Override
        public boolean putDouble(double value) {
            if(!checkNumericScalar(false)) return false;
            convert.fromDouble(pvScalarValue,value);
            bitSet.set(pvScalarValue.getFieldOffset());
            bitSet.set(pvScalarValue.getFieldOffset());
            if(easyPVA.isAutoPut()) return put();
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putString(java.lang.String)
         */
        @Override
        public boolean putString(String value) {
            if(!checkConnected()) return false;
            if(!valueIsScalar) {
                String message = channel.getChannelName() + " value is not scalar";
                Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
                setStatus(status);
                return false;
            }
            convert.fromString(pvScalarValue,value);
            bitSet.set(pvScalarValue.getFieldOffset());
            if(easyPVA.isAutoPut()) return put();
            return true;
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getBooleanArray()
         */
        @Override
        public boolean[] getBooleanArray() {
            if(!checkGetState()) return null;
            if(pvScalarArrayValue==null || scalarTypeValue!=ScalarType.pvBoolean) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not boolean array",null);
                setStatus(status);
                return new boolean[0];
            }
            int length = pvScalarArrayValue.getLength();
            boolean[] data = new boolean[length];
            getBooleanArray(data,length);
            return data;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getByteArray()
         */
        @Override
        public byte[] getByteArray() {
            if(!checkNumericScalarArray(true)) return new byte[0];
            int length = pvScalarArrayValue.getLength();
            byte[] data = new byte[length];
            getByteArray(data,length);
            return data;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getShortArray()
         */
        @Override
        public short[] getShortArray() {
            if(!checkNumericScalarArray(true)) return new short[0];
            int length = pvScalarArrayValue.getLength();
            short[] data = new short[length];
            getShortArray(data,length);
            return data;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getIntArray()
         */
        @Override
        public int[] getIntArray() {
            if(!checkNumericScalarArray(true)) return new int[0];
            int length = pvScalarArrayValue.getLength();
            int[] data = new int[length];
            getIntArray(data,length);
            return data;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getLongArray()
         */
        @Override
        public long[] getLongArray() {
            if(!checkNumericScalarArray(true)) return new long[0];
            int length = pvScalarArrayValue.getLength();
            long[] data = new long[length];
            getLongArray(data,length);
            return data;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getFloatArray()
         */
        @Override
        public float[] getFloatArray() {
            if(!checkNumericScalarArray(true)) return new float[0];
            int length = pvScalarArrayValue.getLength();
            float[] data = new float[length];
            getFloatArray(data,length);
            return data;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getDoubleArray()
         */
        @Override
        public double[] getDoubleArray() {
            if(!checkNumericScalarArray(true)) return new double[0];
            int length = pvScalarArrayValue.getLength();
            double[] data = new double[length];
            getDoubleArray(data,length);
            return data;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getStringArray()
         */
        @Override
        public String[] getStringArray() {
            if(!checkGetState()) return null;
            if(pvScalarArrayValue==null) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not a scalar array",null);
                setStatus(status);
                return new String[0];
            }
            int length = pvScalarArrayValue.getLength();
            String[] data = new String[length];
            getStringArray(data,length);
            return data;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getBooleanArray(boolean[], int)
         */
        @Override
        public int getBooleanArray(boolean[] value, int length) {
            if(!checkGetState()) return 0;
            if(pvScalarArrayValue==null || scalarTypeValue!=ScalarType.pvBoolean) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not boolean array",null);
                setStatus(status);
                return 0;
            }
            PVBooleanArray pvdata = (PVBooleanArray)pvScalarArrayValue;
            int len = length;
            int ntransfered = 0;
            int offset = 0;
            while (len > 0) {
               int num = 0;
               boolean[] dataArray = null;
               int dataOffset = 0;
               synchronized (booleanArrayData) {
                  num = pvdata.get(offset, len, booleanArrayData);
                  dataArray = booleanArrayData.data;
                  dataOffset = booleanArrayData.offset;
               }
               if (num <= 0)
                  break;
               System.arraycopy(dataArray, dataOffset, value,offset, num);
               len -= num;
               offset += num;
               ntransfered += num;
            }
            return ntransfered;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getByteArray(byte[], int)
         */
        @Override
        public int getByteArray(byte[] value, int length) {
            if(!checkNumericScalarArray(true)) return 0;
            return convert.toByteArray(pvScalarArrayValue,0, length,value, 0);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getShortArray(short[], int)
         */
        @Override
        public int getShortArray(short[] value, int length) {
            if(!checkNumericScalarArray(true)) return 0;
            return convert.toShortArray(pvScalarArrayValue,0, length,value, 0);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getIntArray(int[], int)
         */
        @Override
        public int getIntArray(int[] value, int length) {
            if(!checkNumericScalarArray(true)) return 0;
            return convert.toIntArray(pvScalarArrayValue,0, length,value, 0);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getLongArray(long[], int)
         */
        @Override
        public int getLongArray(long[] value, int length) {
            if(!checkNumericScalarArray(true)) return 0;
            return convert.toLongArray(pvScalarArrayValue,0, length,value, 0);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getFloatArray(float[], int)
         */
        @Override
        public int getFloatArray(float[] value, int length) {
            if(!checkNumericScalarArray(true)) return 0;
            return convert.toFloatArray(pvScalarArrayValue,0, length,value, 0);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getDoubleArray(double[], int)
         */
        @Override
        public int getDoubleArray(double[] value, int length) {
            if(!checkNumericScalarArray(true)) return 0;
            return convert.toDoubleArray(pvScalarArrayValue,0, length,value, 0);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getStringArray(java.lang.String[], int)
         */
        @Override
        public int getStringArray(String[] value, int length) {
            if(!checkGetState()) return 0;
            if(pvScalarArrayValue==null) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not a scalar array",null);
                setStatus(status);
                return 0;
            }
            return convert.toStringArray(pvScalarArrayValue,0, length,value, 0);
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putBooleanArray(boolean[], int)
         */
        @Override
        public int putBooleanArray(boolean[] value,int length){
            if(!checkConnected()) return 0;
            if(pvScalarArrayValue==null || scalarTypeValue!=ScalarType.pvBoolean) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not boolean array",null);
                setStatus(status);
                return 0;
            }
            PVBooleanArray pvBooleanArray = (PVBooleanArray)pvScalarArrayValue;
            int num = pvBooleanArray.put(0, length, value, 0);
            bitSet.set(pvScalarArrayValue.getFieldOffset());
            if(!easyPVA.isAutoPut()) return 0;
            boolean result = put();
            if(!result) {
                status = statusCreate.createStatus(StatusType.ERROR, "put failed", null);
                num = 0;
            }
            return num;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putByteArray(byte[], int)
         */
        @Override
        public int putByteArray(byte[] value,int length) {
            if(!checkNumericScalarArray(false)) return 0;
            int  num = convert.fromByteArray(pvScalarArrayValue, 0, length, value, 0);
            bitSet.set(pvScalarArrayValue.getFieldOffset());
            if(!easyPVA.isAutoPut()) return 0;
            boolean result = put();
            if(!result) {
             status = statusCreate.createStatus(StatusType.ERROR, "put failed", null);
             num = 0;
            }
            return num;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putShortArray(short[], int)
         */
        @Override
        public int putShortArray(short[] value,int length) {
            if(!checkNumericScalarArray(false)) return 0;
            int num = convert.fromShortArray(pvScalarArrayValue, 0, length, value, 0);
            bitSet.set(pvScalarArrayValue.getFieldOffset());
            if(!easyPVA.isAutoPut()) return 0;
            boolean result = put();
            if(!result) {
             status = statusCreate.createStatus(StatusType.ERROR, "put failed", null);
             num = 0;
            }
            return num;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putIntArray(int[], int)
         */
        @Override
        public int putIntArray(int[] value,int length) {
            if(!checkNumericScalarArray(false)) return 0;
            int num = convert.fromIntArray(pvScalarArrayValue, 0, length, value, 0);
            bitSet.set(pvScalarArrayValue.getFieldOffset());
            if(!easyPVA.isAutoPut()) return 0;
            boolean result = put();
            if(!result) {
             status = statusCreate.createStatus(StatusType.ERROR, "put failed", null);
             num = 0;
            }
            return num;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putLongArray(long[], int)
         */
        @Override
        public int putLongArray(long[] value,int length) {
            if(!checkNumericScalarArray(false)) return 0;
            int num = convert.fromLongArray(pvScalarArrayValue, 0, length, value, 0);
            bitSet.set(pvScalarArrayValue.getFieldOffset());
            if(!easyPVA.isAutoPut()) return 0;
            boolean result = put();
            if(!result) {
             status = statusCreate.createStatus(StatusType.ERROR, "put failed", null);
             num = 0;
            }
            return num;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putFloatArray(float[], int)
         */
        @Override
        public int putFloatArray(float[] value,int length) {
            if(!checkNumericScalarArray(false)) return 0;
            int num = convert.fromFloatArray(pvScalarArrayValue, 0, length, value, 0);
            bitSet.set(pvScalarArrayValue.getFieldOffset());
            if(!easyPVA.isAutoPut()) return 0;
            boolean result = put();
            if(!result) {
             status = statusCreate.createStatus(StatusType.ERROR, "put failed", null);
             num = 0;
            }
            return num;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putDoubleArray(double[], int)
         */
        @Override
        public int putDoubleArray(double[] value,int length) {
            if(!checkNumericScalarArray(false)) return 0;
            int num = convert.fromDoubleArray(pvScalarArrayValue, 0, length, value, 0);
            bitSet.set(pvScalarArrayValue.getFieldOffset());
            if(!easyPVA.isAutoPut()) return 0;
            boolean result = put();
            if(!result) {
             status = statusCreate.createStatus(StatusType.ERROR, "put failed", null);
             num = 0;
            }
            return num;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#putStringArray(java.lang.String[], int)
         */
        @Override
        public int putStringArray(String[] value,int length) {
            if(!checkConnected()) return 0;
            if(pvScalarArrayValue==null) {
                Status status = statusCreate.createStatus(StatusType.ERROR," value is not a scalar array",null);
                setStatus(status);
                return 0;
            }
            int num = convert.fromStringArray(pvScalarArrayValue, 0, length, value, 0);
            bitSet.set(pvScalarArrayValue.getFieldOffset());
            if(!easyPVA.isAutoPut()) return 0;
            boolean result = put();
            if(!result) {
             status = statusCreate.createStatus(StatusType.ERROR, "put failed", null);
             num = 0;
            }
            return num;
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getPVStructure()
         */
        @Override
        public PVStructure getPVStructure() {
            if(!checkGetState()) throw new IllegalStateException("not connected");
            return pvStructure;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getBitSet()
         */
        @Override
        public BitSet getBitSet() {
            if(!checkGetState()) throw new IllegalStateException("not connected");
            return bitSet;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#setStatus(org.epics.pvdata.pv.Status)
         */
        @Override
        public void setStatus(Status status) {
            this.status = status;
            easyChannel.setStatus(status);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPut#getStatus()
         */
        @Override
        public Status getStatus() {
            Status save = status;
            status = statusCreate.getStatusOK();
            return save;
        }
    }
    
}
