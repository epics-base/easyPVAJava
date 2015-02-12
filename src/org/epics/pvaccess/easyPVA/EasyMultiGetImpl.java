package org.epics.pvaccess.easyPVA;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.ChannelGet;
import org.epics.pvaccess.client.ChannelGetRequester;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.factory.StandardFieldFactory;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.property.PVTimeStamp;
import org.epics.pvdata.property.PVTimeStampFactory;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVIntArray;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVUnionArray;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.StandardField;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Type;
import org.epics.pvdata.pv.Union;
import org.epics.pvdata.pv.UnionArrayData;

public class EasyMultiGetImpl {
    
    /**
     * Factory for creating a new EasyPVStructure.
     * @return The interface.
     */
    static EasyMultiGet create(
        EasyMultiChannel easyMultiChannel,
        Channel[] channel,
        PVStructure pvRequest,
        Union union) {
        return new EMultiGet(easyMultiChannel,channel,pvRequest,union);
    }

    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Convert convert = ConvertFactory.getConvert();
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final StandardField standardField = StandardFieldFactory.getStandardField();
    
    private static class EMultiGet implements EasyMultiGet, ChannelGetRequester {
        private final EasyMultiChannel easyMultiChannel;
        private final Channel[] channel;
        private final PVStructure pvRequest;
        private final Union union;
        private final int nchannel;
        
        // following initialized by init and 
        boolean doubleOnly = false;
        private volatile PVStructure[] topPVStructure = null;
        private volatile ChannelGet[] channelGet = null;
        private volatile int offsetToSeverity = -1;
        private volatile int[] alarmSeverity = null;
        private volatile int[] alarmStatus = null;
        private volatile String[] alarmMessage = null;
        private volatile PVIntArray pvSeverity = null;
        private volatile PVIntArray pvStatus = null;
        private volatile PVStringArray pvMessage = null;
        private volatile int offsetToDeltaTime = -1;
        private volatile double[] deltaTime= null;
        private volatile PVDoubleArray pvDeltaTime = null;
        private volatile double[] doubleValue = null;
        private volatile PVStructure pvTop = null;
        private volatile PVDoubleArray pvDoubleArray = null;
        private volatile PVUnionArray pvUnionArray = null;
        private volatile UnionArrayData unionArrayData = null;
        private volatile PVStructure pvTimeStampStructure = null;
        
        
        // following used by connect
        private volatile int numConnectCallback = 0;
        private volatile int numConnected = 0;
        private volatile boolean[] isConnected = null;
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
        private final PVTimeStamp pvTimeStamp = PVTimeStampFactory.create();
        private final TimeStamp timeStamp = TimeStampFactory.create();
        private final TimeStamp startGet = TimeStampFactory.create();
        
        
        EMultiGet(EasyMultiChannel easyMultiChannel,Channel[] channel,PVStructure pvRequest,Union union) {
            this.easyMultiChannel = easyMultiChannel;
            this.channel = channel;
            this.pvRequest = pvRequest;
            this.union = union;
            nchannel = channel.length;
            isConnected = new boolean[nchannel];
            for(int i=0; i<nchannel; ++i) isConnected[i] = false;
            
        }
        
        public boolean init()
        {
            PVField pvValue = pvRequest.getSubField("field.value");
            if(pvValue==null ) {
                Status status = statusCreate.createStatus(StatusType.ERROR,"pvRequest did not specify value",null);
                setStatus(status);
                return false;
            }
            if(!union.isVariant()) {
                Field[] field = union.getFields();
                if(field.length==1){
                    if(field[0].getType()==Type.scalar) {
                        Scalar scalar = (Scalar)field[0];
                        if(scalar.getScalarType()==ScalarType.pvDouble) doubleOnly = true;
                    }
                }
            }
            channelGet = new ChannelGet[nchannel];
            topPVStructure = new PVStructure[nchannel];
            int nsub = 3; // value,pvName,timeStamp
            if(doubleOnly) {
                Field[] field = new Field[nsub];
                String[] fieldName = new String[nsub];
                fieldName[0] = "value";
                field[0] = fieldCreate.createScalarArray(ScalarType.pvDouble);
                fieldName[1] = "channelName";
                field[1] = fieldCreate.createScalarArray(ScalarType.pvString);
                fieldName[2] = "timeStamp";
                field[2] = standardField.timeStamp();
                doubleValue = new double[nchannel];
                pvTop = pvDataCreate.createPVStructure(fieldCreate.createStructure(fieldName, field));
                pvDoubleArray = pvTop.getSubField(PVDoubleArray.class, "value");
                PVStringArray pvChannelName = pvTop.getSubField(PVStringArray.class,"channelName");
                pvChannelName.put(0, nchannel,easyMultiChannel.getChannelNames(), 0);
                pvTimeStampStructure = pvTop.getSubField(PVStructure.class,"timeStamp");
                return true;
            }
            PVField pvAlarm = pvRequest.getSubField("field.alarm");
            PVField pvTimeStamp = pvRequest.getSubField("field.timeStamp");
            if(pvAlarm!=null) {
                offsetToSeverity = nsub;
                alarmSeverity = new int[nchannel];
                alarmStatus = new int[nchannel];
                alarmMessage = new String[nchannel];
                nsub+=3;
            }
            if(pvTimeStamp!=null) {
                offsetToDeltaTime = nsub;
                deltaTime = new double[nchannel];
                nsub+=1;
            }
            Field[] field = new Field[nsub];
            String[] fieldName = new String[nsub];
            fieldName[0] = "value";
            field[0] = fieldCreate.createUnionArray(union);
            fieldName[1] = "channelName";
            field[1] = fieldCreate.createScalarArray(ScalarType.pvString);
            fieldName[2] = "timeStamp";
            field[2] = standardField.timeStamp();
            if(offsetToSeverity>=0) {
                fieldName[offsetToSeverity] = "severity";
                fieldName[offsetToSeverity+1] = "status";
                fieldName[offsetToSeverity+2] = "message";
                field[offsetToSeverity] = fieldCreate.createScalarArray(ScalarType.pvInt);
                field[offsetToSeverity+1] = fieldCreate.createScalarArray(ScalarType.pvInt);
                field[offsetToSeverity+2] = fieldCreate.createScalarArray(ScalarType.pvString);
            }
            if(offsetToDeltaTime>=0) {
                fieldName[offsetToDeltaTime] = "deltaTime";
                field[offsetToDeltaTime] = fieldCreate.createScalarArray(ScalarType.pvDouble);
            }
            pvTop = pvDataCreate.createPVStructure(fieldCreate.createStructure(fieldName, field));
            pvUnionArray = pvTop.getUnionArrayField("value");
            pvUnionArray.setLength(nchannel);
            unionArrayData = new UnionArrayData();
            pvUnionArray.get(0, nchannel, unionArrayData);
            for(int i=0; i<nchannel; i++) {
                unionArrayData.data[i] = pvDataCreate.createPVUnion(union);
            }
            PVStringArray pvChannelName = pvTop.getSubField(PVStringArray.class,"channelName");
            pvChannelName.put(0, nchannel,easyMultiChannel.getChannelNames(), 0);
            pvTimeStampStructure = pvTop.getSubField(PVStructure.class,"timeStamp");
            if(offsetToSeverity>=0) {
                pvSeverity = pvTop.getSubField(PVIntArray.class,"severity");
                pvStatus = pvTop.getSubField(PVIntArray.class,"status");
                pvMessage = pvTop.getSubField(PVStringArray.class,"message");
            }
            if(offsetToDeltaTime>=0) {
                pvDeltaTime = pvTop.getSubField(PVDoubleArray.class,"deltaTime");
            }
            return true;
        }
        
        private void checkConnected() {
            if(connectState==ConnectState.connectIdle) connect();
        }
        
        private boolean checkGetState() {
            checkConnected();
            if(getState==GetState.getIdle) return get();
            return false;
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
         * @see org.epics.pvaccess.client.ChannelGetRequester#channelGetConnect(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelGet, org.epics.pvdata.pv.Structure)
         */
        @Override
        public void channelGetConnect(Status status, ChannelGet channelGet, Structure structure) {
            if(isDestroyed) return;
            int index = -1;
            for(int i=0; i<channel.length; ++i) {
                if(easyMultiChannel.getChannelNames()[i].equals(channelGet.getChannel().getChannelName())) {
                    index = i;
                    break;
                }
            }
            if(index<0) {
                throw new IllegalStateException("should not happen");
            }
            this.channelGet[index] = channelGet;
            if(status.isOK()) {  
                if(!isConnected[index]) {
                    ++numConnected;
                    isConnected[index] = true;
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
                        } else {
                            if(!union.isVariant()) {
                                Field[] fields = union.getFields();
                                int ind = -1;
                                for(int i=0; i<fields.length; ++i) {
                                    if(fields[i].equals(field)) {
                                        ind = i;
                                        break;
                                    }
                                }
                                if(ind==-1) success = false;
                            }
                            if(success) {
                                PVField value = pvDataCreate.createPVField(field);
                                unionArrayData.data[index].set(value);

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
         * @see org.epics.pvaccess.client.ChannelGetRequester#getDone(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.ChannelGet, org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
         */
        @Override
        public void getDone(Status status, ChannelGet channelGet, PVStructure pvStructure, BitSet bitSet) {
            if(isDestroyed) return;
            int index = -1;
            for(int i=0; i<channel.length; ++i) {
                if(this.channelGet[i]== channelGet) {
                    index = i;
                    break;
                }
            }
            if(index<0) {
                throw new IllegalStateException("should not happen");
            }
            if(status.isOK()) {
                if(doubleOnly) {
                    doubleValue[index] = convert.toDouble((PVScalar)pvStructure.getSubField("value"));
                } else {
                    topPVStructure[index] = pvStructure;
                    if(offsetToSeverity>=0) {
                        alarmSeverity[index] = pvStructure.getSubField(PVInt.class,"alarm.severity").get();
                        alarmStatus[index] = pvStructure.getSubField(PVInt.class,"alarm.status").get();
                        alarmMessage[index] = pvStructure.getSubField(PVString.class,"alarm.message").get();
                    }
                    if(offsetToDeltaTime>=0) {
                        pvTimeStamp.attach(pvStructure.getSubField("timeStamp"));
                        pvTimeStamp.get(timeStamp);
                        deltaTime[index] = startGet.diff(timeStamp, startGet);
                    }
                    PVField from = pvStructure.getSubField("value");
                    PVField to = unionArrayData.data[index].get();
                    convert.copy(from, to);
                }
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
            connectState = ConnectState.connectActive;
            for(int i=0; i<channel.length; ++i) channelGet[i] = channel[i].createChannelGet(this, pvRequest);
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
            startGet.getCurrentTime();
            pvTimeStamp.attach(pvTop.getSubField("timeStamp"));
            pvTimeStamp.set(startGet);
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
            if(doubleOnly) {
                pvDoubleArray.put(0, nchannel, doubleValue, 0);
            }
            if(offsetToSeverity>=0) {
                pvSeverity.put(0, nchannel, alarmSeverity, 0);
                pvStatus.put(0, nchannel, alarmStatus, 0);
                pvMessage.put(0, nchannel, alarmMessage, 0);
            }
            if(offsetToDeltaTime>=0) {
                pvDeltaTime.put(0, nchannel, deltaTime, 0);
            }
            return true;
        }
       
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getTimeStamp()
         */
        @Override
        public TimeStamp getTimeStamp() {
            if(pvTimeStampStructure!=null) {
               pvTimeStamp.attach(pvTimeStampStructure);
               pvTimeStamp.get(timeStamp);
            }
            return timeStamp;
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
            return doubleOnly;
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getNTMultiChannel()
         */
        @Override
        public PVStructure getNTMultiChannel() {
            boolean result = checkGetState();
            if(!result) return null;
            if(doubleOnly) return null;
            return pvTop;
        }
        
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getPVTop()
         */
        @Override
        public PVStructure getPVTop() {
            checkGetState();
            return pvTop;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getDoubleArray()
         */
        @Override
        public double[] getDoubleArray() {
            boolean result = checkGetState();
            if(!result) return null;
            return doubleValue;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getDoubleArray(int, double[], int)
         */
        @Override
        public int getDoubleArray(int offset, double[] data, int length) {
            boolean result = checkGetState();
            if(!result) return 0;
            int num = length;
            if(doubleValue.length-offset<length) num = doubleValue.length-offset;
            if(num<0) num =0;
            for(int i=0; i<num; ++i) data[i] = doubleValue[i+offset];
            return num;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#setStatus(org.epics.pvdata.pv.Status)
         */
        @Override
        public void setStatus(Status status) {
            this.status = status;
            easyMultiChannel.setStatus(status);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiGet#getStatus()
         */
        @Override
        public Status getStatus() {
            Status save = status;
            status = statusCreate.getStatusOK();
            return save;
        }
    }
    
}
