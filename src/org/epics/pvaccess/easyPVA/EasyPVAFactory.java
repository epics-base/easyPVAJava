/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelAccess;
import org.epics.pvaccess.client.ChannelAccessFactory;
import org.epics.pvaccess.client.ChannelGet;
import org.epics.pvaccess.client.ChannelGetRequester;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelPut;
import org.epics.pvaccess.client.ChannelPutRequester;
import org.epics.pvaccess.client.ChannelRPC;
import org.epics.pvaccess.client.ChannelRPCRequester;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvaccess.client.CreateRequestFactory;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.misc.LinkedList;
import org.epics.pvdata.misc.LinkedListCreate;
import org.epics.pvdata.misc.LinkedListNode;
import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.PVAlarm;
import org.epics.pvdata.property.PVAlarmFactory;
import org.epics.pvdata.property.PVTimeStamp;
import org.epics.pvdata.property.PVTimeStampFactory;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
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
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Type;


/**
 * @author mrk
 *
 */
public class EasyPVAFactory {
    static public synchronized EasyPVA get() {
        if(easyPVA==null) {
            easyPVA = new EasyPVAImpl();
            org.epics.pvaccess.ClientFactory.start();
        }
        return easyPVA;
    }

    static private EasyPVA easyPVA = null;
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    //private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    //private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final Convert convert = ConvertFactory.getConvert();
    private static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    private static final String easyPVAName = "easyPVA";

    static private class EasyPVAImpl implements EasyPVA {
        private static LinkedListCreate<EasyChannel> easyChannelListCreate = new LinkedListCreate<EasyChannel>();
        private static LinkedList<EasyChannel> easyChannelList = easyChannelListCreate.create();
        private boolean isDestroyed = false;
        private Requester requester = null;
        private boolean autoGet = true;
        private boolean autoPut = true;
        private Status status = statusCreate.getStatusOK();
        private StatusType autoMessageThreashold = StatusType.WARNING;

        @Override
        public void destroy() {
            synchronized (this) {
               if(isDestroyed) return;
               isDestroyed = true;
            }
            LinkedListNode<EasyChannel> listNode = easyChannelList.removeTail();
            while(listNode!=null) {
               EasyChannel channel = (EasyChannel)listNode.getObject();
               channel.destroy();
               listNode = easyChannelList.removeTail();
            }
        }
        @Override
        public EasyChannel createChannel(String channelName) {
            return createChannel(channelName,"pvAccess");
        }
        @Override
        public EasyMultiChannel createMultiChannel(String[] channelNames) {
            return createMultiChannel(channelNames,"pvAccess");
        }
        @Override
        public EasyChannel createChannel(String channelName,String providerName) {
            if(isDestroyed) return null;
            EasyChannel easyChannel = new EasyChannelImpl(this,channelName,providerName);
            LinkedListNode<EasyChannel> listNode = easyChannelListCreate.createNode(easyChannel);
            easyChannelList.addTail(listNode);
            return easyChannel;
        }
        @Override
        public EasyMultiChannel createMultiChannel(String[] channelNames,String providerName) {
            if(isDestroyed) return null;
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public void setRequester(Requester requester) {
        	this.requester = requester;
        }
        @Override
        public void clearRequester() {
        	requester = null;
        }
        @Override
        public void setStatus(Status status) {
        	this.status = status;
        	StatusType type = status.getType();
        	if(type.ordinal()>=autoMessageThreashold.ordinal()) {
        	    MessageType messageType = MessageType.info;
        	    switch(type) {
        	    case OK:      messageType = MessageType.info; break;
        	    case WARNING: messageType = MessageType.warning; break;
        	    case ERROR:   messageType = MessageType.error; break;
        	    case FATAL:   messageType = MessageType.fatalError; break;
        	    }
        		String  message = status.getMessage();
        		String stackDump = status.getStackDump();
        		if(stackDump!=null && stackDump.length()>0) message += " " + stackDump;
        		message(message,messageType);
        	}
        }
        @Override
        public Status getStatus() {
        	Status save = status;
        	status = statusCreate.getStatusOK();
        	return save;
        }
        @Override
        public void setAuto(boolean get, boolean put) {
        	this.autoGet = get;
        	this.autoPut = put;
        }
        @Override
        public boolean isAutoGet() {
        	return autoGet;
        }
        @Override
        public boolean isAutoPut() {
        	return autoPut;
        }
        @Override
        public void setAutoMessage(StatusType type) {
        	this.autoMessageThreashold = type;
        }
		@Override
        public String getRequesterName() {
			if(requester!=null) return requester.getRequesterName();
	        return easyPVAName;
        }
		@Override
        public void message(String message, MessageType messageType) {
	        if(requester!=null) {
	        	requester.message(message, messageType);
	        	return;
	        }
	        System.out.printf("%s %s%n", messageType.name(),message);
        }
    }

    static private class EasyChannelImpl implements EasyChannel, ChannelRequester {
        private boolean isDestroyed = false;
        private EasyPVA easyPVA;
        private String channelName;
        private String providerName;
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitForConnect = lock.newCondition();
        private Channel channel = null;
        private Status status = statusCreate.getStatusOK();
        private enum ConnectState {connectIdle,notConnected,connected};
        private ConnectState connectState = ConnectState.connectIdle;
        
        private boolean checkConnected() {
            if(connectState==ConnectState.connectIdle) connect(2.0);
            if(connectState==ConnectState.connected) return true;
            if(connectState==ConnectState.notConnected) return false;
            String message = channelName + " illegal connect state";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
            return false;
        }

        EasyChannelImpl(EasyPVA easyPVA,String channelName,String providerName) {
        	this.easyPVA = easyPVA;
            this.channelName = channelName;
            this.providerName = providerName;
        }
        @Override
        public String getRequesterName() {
            return easyPVA.getRequesterName();
        }
        @Override
        public void message(String message, MessageType messageType) {
            if(isDestroyed) return;
            String mess = channelName + " " + message;
            easyPVA.message(mess, messageType);
        }
        @Override
        public void channelCreated(Status status, Channel channel) {
            this.status = status;
            this.channel = channel;
        }
        @Override
        public void channelStateChange(Channel c,ConnectionState connectionState) {
            if(c!=channel) throw new IllegalStateException("logic error in channelStateChange");
            synchronized (this) {
               if(isDestroyed) return;
               if(connectionState!=ConnectionState.CONNECTED) {
                  connectState = ConnectState.notConnected;
                  Status status = statusCreate.createStatus(
                       StatusType.ERROR,
                       channelName + " connection state " + connectionState.name(),
                       null);
                  setStatus(status);
               }
            }
            lock.lock();
            connectState = ConnectState.connected;
            try {
               waitForConnect.signal();
            } finally {
               lock.unlock();
            }
        }
        @Override
        public void destroy() {
            synchronized (this) {
               if(isDestroyed) return;
               isDestroyed = true;
            }
            channel.destroy();
        }

        @Override
        public String getChannelName() {
            return channelName;
        }

        @Override
        public Channel getChannel() {
            return channel;
        }

        @Override
        public boolean connect(double timeout) {
            issueConnect();
            return waitConnect(timeout);
        }

        @Override
        public void issueConnect() {
            if(isDestroyed) return;
            if(connectState!=ConnectState.connectIdle) {
            	 Status status = statusCreate.createStatus(
                     StatusType.ERROR,
                     channelName + " issueConnect called multiple times ",
                     null);
                 setStatus(status);
            }
            ChannelProvider channelProvider = channelAccess.getProvider(providerName);
            channel = channelProvider.createChannel(channelName, this, ChannelProvider.PRIORITY_DEFAULT);
        }

        @Override
        public boolean waitConnect(double timeout) {
            if(isDestroyed) return false;
            try {
                lock.lock();
                try {
                    if(connectState==ConnectState.connectIdle) {
                        long nano = (long)(timeout*1e9);
                        waitForConnect.awaitNanos(nano);
                    }
                } catch(InterruptedException e) {
                    Status status = statusCreate.createStatus(StatusType.ERROR,e.getMessage(), e.fillInStackTrace());
                    setStatus(status);
                    return false;
                }
            } finally {
                lock.unlock();
            }
            if(connectState==ConnectState.connectIdle) {
                status = statusCreate.createStatus(StatusType.ERROR," did not connect",null);
                return false;
            }
            return true;
        }
        @Override
        public EasyField createField() {
        	return createField("");
        }

		@Override
        public EasyField createField(String subField) {
	        // TODO Auto-generated method stub
	        return null;
        }

		@Override
        public EasyProcess createProcess() {
			return createProcess("");
        }

		@Override
		public EasyProcess createProcess(String request) {
			PVStructure pvStructure = CreateRequestFactory.createRequest(request, this);
			if(pvStructure==null) return null;
			return createProcess(pvStructure);
		}

		@Override
        public EasyProcess createProcess(PVStructure pvRequest) {
	   
	        return null;
        }

		@Override
        public EasyGet createGet() {
            return createGet("record[]field(value,alarm,timeStamp)");
        }

        @Override
        public EasyGet createGet(String request) {
            PVStructure pvStructure = CreateRequestFactory.createRequest(request, this);
            if(pvStructure==null) return null;
            return createGet(pvStructure);
        }

        @Override
        public EasyGet createGet(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            return new EasyGetImpl(this,channel,pvRequest);
        }

        @Override
        public EasyPut createPut() {
        	return createPut("record[]field(value)");
        }

        @Override
        public EasyPut createPut(String request) {
        	 PVStructure pvStructure = CreateRequestFactory.createRequest(request, this);
             if(pvStructure==null) return null;
             return createPut(pvStructure);
        }

        @Override
        public EasyPut createPut(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            return new EasyPutImpl(this,channel,pvRequest);
        }

        @Override
        public EasyPutGet createPutGet() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public EasyPutGet createPutGet(String request) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public EasyPutGet createPutGet(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            return null;
        }

        @Override
        public EasyRPC createRPC() {
        	return createRPC((PVStructure)null);	// null allowed for RPC
        }

        @Override
        public EasyRPC createRPC(String request) {
       	 PVStructure pvStructure = CreateRequestFactory.createRequest(request, this);
         if(pvStructure==null) return null;
         return createRPC(pvStructure);
        }

        @Override
        public EasyRPC createRPC(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            return new EasyRPCImpl(this,channel,pvRequest);
        }

        @Override
        public EasyArray createArray() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public EasyArray createArray(String request) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public EasyArray createArray(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            return null;
        }

        @Override
        public EasyMonitor createMonitor() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public EasyMonitor createMonitor(String request) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public EasyMonitor createMonitor(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            return null;
        }
        @Override
        public void setStatus(Status status) {
        	this.status = status;
        	easyPVA.setStatus(status);
        }
        @Override
        public Status getStatus() {
        	Status save = status;
        	status = statusCreate.getStatusOK();
            return save;
        }
    }

    private static class EasyGetImpl implements EasyGet, ChannelGetRequester {
        private boolean isDestroyed = false;
        private EasyChannelImpl easyChannel;
        private Channel channel;
        private PVStructure pvRequest;
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitForConnect = lock.newCondition();
        private Condition waitForGet = lock.newCondition();
        private Status status = statusCreate.getStatusOK();
        private ChannelGet channelGet = null;
        private PVStructure pvStructure = null;
        private BitSet bitSet = null;
        private boolean hasValue;
        private boolean valueIsScalar = false;
        private PVField pvValue = null;
        private ScalarType scalarTypeValue = null;
        private PVScalar pvScalarValue = null;
        private PVArray pvArrayValue =  null;
        private PVScalarArray pvScalarArrayValue = null;
        private boolean valueIsNumeric = false;
        private PVBoolean pvBooleanValue = null;
        private PVStructure pvAlarmStructure = null;
        private PVAlarm pvAlarm = PVAlarmFactory.create();
        private Alarm alarm = new Alarm();
        private PVStructure pvTimeStampStructure = null;
        private PVTimeStamp pvTimeStamp = PVTimeStampFactory.create();
        private TimeStamp timeStamp = TimeStampFactory.create();
        private BooleanArrayData booleanArrayData = new BooleanArrayData();
        
        private enum ConnectState {connectIdle,notConnected,connected};
        private ConnectState connectState = ConnectState.connectIdle;
        
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
        private GetState getState = GetState.getIdle;
        
        private boolean checkGetState() {
        	if(!checkConnected()) return false;
            if(!easyPVA.isAutoGet()) return true;
            if(getState==GetState.getIdle) get();
            if(getState==GetState.getIdle) return true;
            String message = channel.getChannelName() + " illegal get state";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
            return false;
        }
        
        private boolean checkNumericScalar() {
        	if(!checkGetState()) return false;
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
        
        private boolean checkNumericScalarArray() {
        	if(!checkGetState()) return false;
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
        
        EasyGetImpl(EasyChannelImpl easyChannel,Channel channel,PVStructure pvRequest) {
            this.easyChannel = easyChannel;
            this.channel = channel;
            this.pvRequest = pvRequest;
        }
        @Override
        public String getRequesterName() {
            return easyChannel.providerName;
        }
        @Override
        public void message(String message, MessageType messageType) {
            if(isDestroyed) return;
            easyChannel.message(message, messageType);
        }
        @Override
        public void channelGetConnect(Status status, ChannelGet channelGet,PVStructure pvStructure, BitSet bitSet) {
            if(isDestroyed) return;
            this.channelGet = channelGet;
            this.pvStructure = pvStructure;
            this.bitSet = bitSet;
            this.status = status;
            if(!status.isSuccess()) {
            	connectState = ConnectState.notConnected;
            } else {
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
            		connectState = ConnectState.connected;
            	}
            pvTimeStampStructure = pvStructure.getStructureField("timeStamp");
            pvAlarmStructure = pvStructure.getStructureField("alarm");
            }
            lock.lock();
            try {
               waitForConnect.signal();
            } finally {
               lock.unlock();
            }
        }
        @Override
        public void getDone(Status status) {
            this.status = status;
            getState = GetState.getDone;
            lock.lock();
            try {
               waitForGet.signal();
            } finally {
               lock.unlock();
            }
        }
        @Override
        public void destroy() {
            synchronized (this) {
               if(isDestroyed) return;
               isDestroyed = true;
            }
            channelGet.destroy();
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
        	channelGet = channel.createChannelGet(this, pvRequest);
        }
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
        @Override
        public boolean get() {
            issueGet();
            return waitGet();
        }

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
            channelGet.get(false);
        }
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
                    return false;
                }
            } finally {
                lock.unlock();
            }
            if(getState==GetState.getActive) {
                Status status = statusCreate.createStatus(StatusType.ERROR," get failed",null);
                setStatus(status);
                return false;
            }
            getState = GetState.getIdle;
            return true;
        }
        @Override
        public PVField getValue() {
            checkGetState();
            return pvValue;
        }
        @Override
        public PVScalar getScalarValue() {
            checkGetState();
            return pvScalarValue;
        }
        @Override
        public PVArray getArrayValue() {
        	checkGetState();
            return pvArrayValue;
        }
		@Override
        public PVScalarArray getScalarArrayValue() {
			checkGetState();
            return pvScalarArrayValue;
        }
        @Override
        public Alarm getAlarm() {
            if(pvAlarmStructure!=null) {
               pvAlarm.attach(pvAlarmStructure);
               pvAlarm.get(alarm);
            }
            return alarm;
        }
        @Override
        public TimeStamp getTimeStamp() {
            if(pvTimeStampStructure!=null) {
               pvTimeStamp.attach(pvTimeStampStructure);
               pvTimeStamp.get(timeStamp);
            }
            return timeStamp;
        }
        @Override
        public boolean hasValue() {
            return hasValue;
        }

        @Override
        public boolean isValueScalar() {
            return valueIsScalar;
        }

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
        @Override
        public byte getByte() {
        	if(!checkNumericScalar()) return 0;
        	return convert.toByte(pvScalarValue);
        }

        @Override
        public short getShort() {
        	if(!checkNumericScalar()) return 0;
            return convert.toShort(pvScalarValue);
        }

        @Override
        public int getInt() {
        	if(!checkNumericScalar()) return 0;
            return convert.toInt(pvScalarValue);	
        }

        @Override
        public long getLong() {
        	if(!checkNumericScalar()) return 0;
            return convert.toLong(pvScalarValue);
        }

        @Override
        public float getFloat() {
        	if(!checkNumericScalar()) return 0;
            return convert.toFloat(pvScalarValue);
        }

        @Override
        public double getDouble() {
        	if(!checkNumericScalar()) return 0;
            return convert.toDouble(pvScalarValue);
        }

        @Override
        public boolean[] getBooleanArray() {
        	if(!checkGetState()) return null;
            if(pvScalarArrayValue==null || scalarTypeValue!=ScalarType.pvBoolean) {
            	Status status = statusCreate.createStatus(StatusType.ERROR," value is not a boolean array",null);
        		setStatus(status);
            	return new boolean[0];
            }
            int length = pvScalarArrayValue.getLength();
            boolean[] data = new boolean[length];
            getBooleanArray(data,length);
            return data;
        }

        @Override
        public byte[] getByteArray() {
        	if(!checkNumericScalarArray()) return new byte[0];
            int length = pvScalarArrayValue.getLength();
            byte[] data = new byte[length];
            getByteArray(data,length);
            return data;
        }

        @Override
        public short[] getShortArray() {
        	if(!checkNumericScalarArray()) return new short[0];
            int length = pvScalarArrayValue.getLength();
            short[] data = new short[length];
            getShortArray(data,length);
            return data;
        }

        @Override
        public int[] getIntArray() {
        	if(!checkNumericScalarArray()) return new int[0];
            int length = pvScalarArrayValue.getLength();
            int[] data = new int[length];
            getIntArray(data,length);
            return data;
        }

        @Override
        public long[] getLongArray() {
        	if(!checkNumericScalarArray()) return new long[0];
            int length = pvScalarArrayValue.getLength();
            long[] data = new long[length];
            getLongArray(data,length);
            return data;
        }

        @Override
        public float[] getFloatArray() {
        	if(!checkNumericScalarArray()) return new float[0];
            int length = pvScalarArrayValue.getLength();
            float[] data = new float[length];
            getFloatArray(data,length);
            return data;
        }

        @Override
        public double[] getDoubleArray() {
        	if(!checkNumericScalarArray()) return new double[0];
            int length = pvScalarArrayValue.getLength();
            double[] data = new double[length];
            getDoubleArray(data,length);
            return data;
        }

        @Override
        public String[] getStringArray() {
        	if(!checkGetState()) return null;
            if(pvScalarArrayValue==null || scalarTypeValue!=ScalarType.pvString) {
            	Status status = statusCreate.createStatus(StatusType.ERROR," value is not a string array",null);
        		setStatus(status);
            	return new String[0];
            }
            int length = pvScalarArrayValue.getLength();
            String[] data = new String[length];
            getStringArray(data,length);
            return data;
        }

        @Override
        public int getBooleanArray(boolean[] value, int length) {
        	if(!checkGetState()) return 0;
            if(pvScalarArrayValue==null || scalarTypeValue!=ScalarType.pvBoolean) {
            	Status status = statusCreate.createStatus(StatusType.ERROR," value is not a boolean array",null);
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

        @Override
        public int getByteArray(byte[] value, int length) {
        	if(!checkNumericScalarArray()) return 0;
            return convert.toByteArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getShortArray(short[] value, int length) {
        	if(!checkNumericScalarArray()) return 0;
            return convert.toShortArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getIntArray(int[] value, int length) {
        	if(!checkNumericScalarArray()) return 0;
            return convert.toIntArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getLongArray(long[] value, int length) {
        	if(!checkNumericScalarArray()) return 0;
            return convert.toLongArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getFloatArray(float[] value, int length) {
        	if(!checkNumericScalarArray()) return 0;
            return convert.toFloatArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getDoubleArray(double[] value, int length) {
        	if(!checkNumericScalarArray()) return 0;
            return convert.toDoubleArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getStringArray(String[] value, int length) {
        	if(!checkGetState()) return 0;
            if(pvScalarArrayValue==null || scalarTypeValue!=ScalarType.pvString) {
            	Status status = statusCreate.createStatus(StatusType.ERROR," value is not a string array",null);
        		setStatus(status);
            	return 0;
            }
            return convert.toStringArray(pvScalarArrayValue,0, length,value, 0);
        }

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

        @Override
        public PVStructure getPVStructure() {
            if(!checkGetState()) return null;
            return pvStructure;
        }

        @Override
        public BitSet getBitSet() {
            if(!checkGetState()) return null;
            return bitSet;
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
    
    private static class EasyPutImpl implements EasyPut, ChannelPutRequester {
        private boolean isDestroyed = false;
        private EasyChannelImpl easyChannel;
        private Channel channel;
        private PVStructure pvRequest;
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitForConnect = lock.newCondition();
        private Condition waitForPutOrGet = lock.newCondition();
        private Status status = statusCreate.getStatusOK();
        private ChannelPut channelPut = null;
        private PVStructure pvStructure = null;
        private BitSet bitSet = null;
        private boolean hasValue;
        private boolean valueIsScalar = false;
        private PVField pvValue = null;
        private ScalarType scalarTypeValue = null;
        private PVScalar pvScalarValue = null;
        private PVArray pvArrayValue =  null;
        private PVScalarArray pvScalarArrayValue = null;
        private boolean valueIsNumeric = false;
        private PVBoolean pvBooleanValue = null;
        private BooleanArrayData booleanArrayData = new BooleanArrayData();
        
        private enum ConnectState {connectIdle,notConnected,connected};
        private ConnectState connectState = ConnectState.connectIdle;
        
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
        private GetState getState = GetState.getIdle;
        
        private boolean checkGetState() {
        	if(!checkConnected()) return false;
            if(!easyPVA.isAutoGet()) return true;
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
        private PutState putState = PutState.putIdle;
        
        // TODO this methid is never called! Is this OK??!!!!
        private boolean checkPutState() {
        	if(!checkConnected()) return false;
            if(!easyPVA.isAutoPut()) return true;
            if(getState!=GetState.getIdle) {
            	String message = channel.getChannelName() + " get is active";
                Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
                setStatus(status);
            	return false;
        	}
            if(putState==PutState.putIdle) put();
            if(putState==PutState.putIdle) return true;
            String message = channel.getChannelName() + " illegal put state";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
            return false;
        }

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

        EasyPutImpl(EasyChannelImpl easyChannel,Channel channel,PVStructure pvRequest) {
            this.easyChannel = easyChannel;
            this.channel = channel;
            this.pvRequest = pvRequest;
        }

        @Override
        public String getRequesterName() {
            return easyChannel.providerName;
        }
        @Override
        public void message(String message, MessageType messageType) {
            if(isDestroyed) return;
            easyChannel.message(message, messageType);
        }
        @Override
        public void channelPutConnect(Status status, ChannelPut channelPut,PVStructure pvStructure, BitSet bitSet) {
            if(isDestroyed) return;
            this.channelPut = channelPut;
            this.pvStructure = pvStructure;
            this.bitSet = bitSet;
            this.status = status;
            if(!status.isSuccess()) {
            	connectState = ConnectState.notConnected;
            } else {
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
            		connectState = ConnectState.connected;
            	}
           
            }
            lock.lock();
            try {
               waitForConnect.signal();
            } finally {
               lock.unlock();
            }
        }

        @Override
        public void getDone(Status status) {
            this.status = status;
            getState = GetState.getDone;
            lock.lock();
            try {
               waitForPutOrGet.signal();
            } finally {
               lock.unlock();
            }
        }

        @Override
        public void putDone(Status status) {
        	this.status = status;
            putState = PutState.putDone;
            lock.lock();
            try {
               waitForPutOrGet.signal();
            } finally {
               lock.unlock();
            }
        }


		@Override
        public void destroy() {
            synchronized (this) {
               if(isDestroyed) return;
               isDestroyed = true;
            }
            channelPut.destroy();
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
            channelPut = channel.createChannelPut(this, pvRequest);
        }

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

        @Override
        public boolean get() {
            issueGet();
            return waitGet();
        }

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
            channelPut.get();
        }

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
        	if(getState==GetState.getActive) {
        		Status status = statusCreate.createStatus(StatusType.ERROR," get failed",null);
        		setStatus(status);
        		return false;
        	}
        	getState = GetState.getIdle;
        	return true;
        }
        
        @Override
        public boolean put() {
            issuePut();
            return waitPut();
        }

        @Override
        public void issuePut() {
            if(isDestroyed) return;
            if(!checkConnected()) return;
            if(putState!=PutState.putIdle) {
            	Status status = statusCreate.createStatus(
        				StatusType.ERROR,"put already issued",null);
        		setStatus(status);
        		return;
            }
            putState = PutState.putActive;
            channelPut.put(false);
        }

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
        	if(putState==PutState.putActive) {
        		Status status = statusCreate.createStatus(StatusType.ERROR," put failed",null);
        		setStatus(status);
        		return false;
        	}
        	putState = PutState.putIdle;
        	return true;
        }
        
        @Override
        public boolean hasValue() {
            checkGetState();
            return hasValue;
        }
        @Override
        public boolean isValueScalar() {
            checkGetState();
            return valueIsScalar;
        }
        @Override
        public PVField getValue() {
        	checkGetState();
            return pvValue;
        }
        @Override
        public PVScalar getScalarValue() {
        	checkGetState();
            return pvScalarValue;
        }
        @Override
        public PVArray getArrayValue() {
        	checkGetState();
            return pvArrayValue;
        }
		@Override
        public PVScalarArray getScalarArrayValue() {
			checkGetState();
            return pvScalarArrayValue;
        }

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
        @Override
        public byte getByte() {
        	if(!checkNumericScalar(true)) return 0;
            return convert.toByte(pvScalarValue);
        }
        @Override
        public short getShort() {
        	if(!checkNumericScalar(true)) return 0;
            return convert.toShort(pvScalarValue);
        }
        @Override
        public int getInt() {
        	if(!checkNumericScalar(true)) return 0;
            return convert.toInt(pvScalarValue);	
        }
        @Override
        public long getLong() {
        	if(!checkNumericScalar(true)) return 0;
            return convert.toLong(pvScalarValue);
        }
        @Override
        public float getFloat() {
        	if(!checkNumericScalar(true)) return 0;
            return convert.toFloat(pvScalarValue);
        }
        @Override
        public double getDouble() {
        	if(!checkNumericScalar(true)) return 0;
            return convert.toDouble(pvScalarValue);
        }
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
        @Override
        public boolean putByte(byte value) {
        	if(!checkNumericScalar(false)) return false;
        	convert.fromByte(pvScalarValue,value);
        	bitSet.set(pvScalarValue.getFieldOffset());
        	if(easyPVA.isAutoPut()) return put();
            return true;
        }
        @Override
        public boolean putShort(short value) {
        	if(!checkNumericScalar(false)) return false;
        	convert.fromShort(pvScalarValue,value);
        	bitSet.set(pvScalarValue.getFieldOffset());
        	return put();
        }
        @Override
        public boolean putInt(int value) {
        	if(!checkNumericScalar(false)) return false;
        	convert.fromInt(pvScalarValue,value);
        	bitSet.set(pvScalarValue.getFieldOffset());
        	if(easyPVA.isAutoPut()) return put();
            return true;
        }
        @Override
        public boolean putLong(long value) {
        	if(!checkNumericScalar(false)) return false;
        	convert.fromLong(pvScalarValue,value);
        	bitSet.set(pvScalarValue.getFieldOffset());
        	if(easyPVA.isAutoPut()) return put();
            return true;
        }
        @Override
        public boolean putFloat(float value) {
        	if(!checkNumericScalar(false)) return false;
        	convert.fromFloat(pvScalarValue,value);
        	bitSet.set(pvScalarValue.getFieldOffset());
        	if(easyPVA.isAutoPut()) return put();
            return true;
        }
        @Override
        public boolean putDouble(double value) {
        	if(!checkNumericScalar(false)) return false;
        	convert.fromDouble(pvScalarValue,value);
        	bitSet.set(pvScalarValue.getFieldOffset());
        	bitSet.set(pvScalarValue.getFieldOffset());
        	if(easyPVA.isAutoPut()) return put();
            return true;
        }
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

        @Override
        public byte[] getByteArray() {
        	if(!checkNumericScalarArray(true)) return new byte[0];
            int length = pvScalarArrayValue.getLength();
            byte[] data = new byte[length];
            getByteArray(data,length);
            return data;
        }

        @Override
        public short[] getShortArray() {
        	if(!checkNumericScalarArray(true)) return new short[0];
            int length = pvScalarArrayValue.getLength();
            short[] data = new short[length];
            getShortArray(data,length);
            return data;
        }

        @Override
        public int[] getIntArray() {
        	if(!checkNumericScalarArray(true)) return new int[0];
            int length = pvScalarArrayValue.getLength();
            int[] data = new int[length];
            getIntArray(data,length);
            return data;
        }

        @Override
        public long[] getLongArray() {
        	if(!checkNumericScalarArray(true)) return new long[0];
            int length = pvScalarArrayValue.getLength();
            long[] data = new long[length];
            getLongArray(data,length);
            return data;
        }

        @Override
        public float[] getFloatArray() {
        	if(!checkNumericScalarArray(true)) return new float[0];
            int length = pvScalarArrayValue.getLength();
            float[] data = new float[length];
            getFloatArray(data,length);
            return data;
        }

        @Override
        public double[] getDoubleArray() {
        	if(!checkNumericScalarArray(true)) return new double[0];
            int length = pvScalarArrayValue.getLength();
            double[] data = new double[length];
            getDoubleArray(data,length);
            return data;
        }

        @Override
        public String[] getStringArray() {
        	if(!checkGetState()) return null;
            if(pvScalarArrayValue==null || scalarTypeValue!=ScalarType.pvString) {
            	Status status = statusCreate.createStatus(StatusType.ERROR," value is not string array",null);
        		setStatus(status);
            	return new String[0];
            }
            int length = pvScalarArrayValue.getLength();
            String[] data = new String[length];
            getStringArray(data,length);
            return data;
        }

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

        @Override
        public int getByteArray(byte[] value, int length) {
        	if(!checkNumericScalarArray(true)) return 0;
            return convert.toByteArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getShortArray(short[] value, int length) {
        	if(!checkNumericScalarArray(true)) return 0;
            return convert.toShortArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getIntArray(int[] value, int length) {
        	if(!checkNumericScalarArray(true)) return 0;
            return convert.toIntArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getLongArray(long[] value, int length) {
        	if(!checkNumericScalarArray(true)) return 0;
            return convert.toLongArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getFloatArray(float[] value, int length) {
        	if(!checkNumericScalarArray(true)) return 0;
            return convert.toFloatArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getDoubleArray(double[] value, int length) {
        	if(!checkNumericScalarArray(true)) return 0;
            return convert.toDoubleArray(pvScalarArrayValue,0, length,value, 0);
        }

        @Override
        public int getStringArray(String[] value, int length) {
        	if(!checkGetState()) return 0;
        	if(pvScalarArrayValue==null || scalarTypeValue!=ScalarType.pvString) {
        		Status status = statusCreate.createStatus(StatusType.ERROR," value is not string array",null);
        		setStatus(status);
        		return 0;
        	}
        	return convert.toStringArray(pvScalarArrayValue,0, length,value, 0);
        }
        
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
        @Override
        public int putStringArray(String[] value,int length) {
        	if(!checkConnected()) return 0;
        	if(pvScalarArrayValue==null || scalarTypeValue!=ScalarType.pvString) {
        		Status status = statusCreate.createStatus(StatusType.ERROR," value is not string array",null);
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
        
        @Override
        public PVStructure getPVStructure() {
            if(!checkGetState()) throw new IllegalStateException("not connected");
            return pvStructure;
        }

        @Override
        public BitSet getBitSet() {
            if(!checkGetState()) throw new IllegalStateException("not connected");
            return bitSet;
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

	private static class EasyRPCImpl implements EasyRPC, ChannelRPCRequester {
	    private volatile boolean isDestroyed = false;
	    private final EasyChannelImpl easyChannel;
	    private final Channel channel;
	    private final PVStructure pvRequest;
	    
	    private final ReentrantLock lock = new ReentrantLock();
	    private final Condition waitForConnect = lock.newCondition();
	    private final Condition waitForRPC = lock.newCondition();
	    
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
	    
	    EasyRPCImpl(EasyChannelImpl easyChannel,Channel channel,PVStructure pvRequest) {
	        this.easyChannel = easyChannel;
	        this.channel = channel;
	        this.pvRequest = pvRequest;
	    }
	    @Override
	    public String getRequesterName() {
	        return easyChannel.providerName;
	    }
	    @Override
	    public void message(String message, MessageType messageType) {
	        if(isDestroyed) return;
	        easyChannel.message(message, messageType);
	    }
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
	    @Override
	    public void requestDone(Status status, PVStructure result) {
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
	    @Override
	    public void destroy() {
	        synchronized (this) {
	           if(isDestroyed) return;
	           isDestroyed = true;
	        }
	        channelRPC.destroy();
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
	    	channelRPC = channel.createChannelRPC(this, pvRequest);
	    }
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
	    
		@Override
		public PVStructure request(PVStructure request) {
	        issueRequest(request);
	        return waitRequest();
	    }
	
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
	        channelRPC.request(request,false);
	    }
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
