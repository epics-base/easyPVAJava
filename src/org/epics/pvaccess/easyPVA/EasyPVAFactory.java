/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelGet;
import org.epics.pvaccess.client.ChannelGetRequester;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelProviderRegistryFactory;
import org.epics.pvaccess.client.ChannelPut;
import org.epics.pvaccess.client.ChannelPutRequester;
import org.epics.pvaccess.client.ChannelRPC;
import org.epics.pvaccess.client.ChannelRPCRequester;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.factory.StandardFieldFactory;
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
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVBooleanArray;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVIntArray;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVUnion;
import org.epics.pvdata.pv.PVUnionArray;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.ScalarArray;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.StandardField;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Type;
import org.epics.pvdata.pv.Union;
import org.epics.pvdata.pv.UnionArrayData;


/**
 * @author mrk
 *
 */
public class EasyPVAFactory {
	
    static public synchronized EasyPVA get() {
        if(easyPVA==null) {
            easyPVA = new EasyPVAImpl();
        	org.epics.pvaccess.ClientFactory.start();
            org.epics.ca.ClientFactory.start();
        }
        return easyPVA;
    }

    private static EasyPVA easyPVA = null;
    
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final StandardField standardField = StandardFieldFactory.getStandardField();
    private static final Convert convert = ConvertFactory.getConvert();
    private static final String easyPVAName = "easyPVA";
    private static final String defaultProvider = org.epics.pvaccess.ClientFactory.PROVIDER_NAME;
    private static final Union variantUnion = FieldFactory.getFieldCreate().createVariantUnion();
    
    private static PVStructure createRequest(String request)
    {
        CreateRequest factory = CreateRequest.create();
        PVStructure pvStructure = factory.createRequest(request);
        if (pvStructure == null) 
            throw new IllegalArgumentException("invalid pvRequest: " + factory.getMessage());
        else
            return pvStructure;
    }
    private enum ConnectState {connectIdle,notConnected,connected};

    static private class EasyPVAImpl implements EasyPVA {
        private static final LinkedListCreate<EasyChannel> easyChannelListCreate = new LinkedListCreate<EasyChannel>();
        private static final LinkedList<EasyChannel> easyChannelList = easyChannelListCreate.create();
        private static final LinkedListCreate<EasyMultiChannel> easyMultiChannelListCreate = new LinkedListCreate<EasyMultiChannel>();
        private static final LinkedList<EasyMultiChannel> easyMultiChannelList = easyMultiChannelListCreate.create();
 
        private boolean isDestroyed = false;
        private Requester requester = null;
        private boolean autoGet = true;
        private boolean autoPut = true;
        private Status status = statusCreate.getStatusOK();
        private StatusType autoMessageThreashold = StatusType.WARNING;

        @Override
        public void destroy() {
            if(isDestroyed) return;
            isDestroyed = true;
            LinkedListNode<EasyChannel> listNode = easyChannelList.removeTail();
            while(listNode!=null) {
               EasyChannel channel = (EasyChannel)listNode.getObject();
               channel.destroy();
               listNode = easyChannelList.removeTail();
            }
            LinkedListNode<EasyMultiChannel> multiListNode = easyMultiChannelList.removeTail();
            while(multiListNode!=null) {
               EasyMultiChannel channel = (EasyMultiChannel)multiListNode.getObject();
               channel.destroy();
               multiListNode = easyMultiChannelList.removeTail();
            }
        }
        @Override
        public EasyChannel createChannel(String channelName) {
            return createChannel(channelName,defaultProvider);
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
        public EasyMultiChannel createMultiChannel(String[] channelNames) {
            return createMultiChannel(channelNames,defaultProvider);
        }
        @Override
        public EasyMultiChannel createMultiChannel(String[] channelNames,String providerName) {
            return createMultiChannel(channelNames,providerName,variantUnion);
        }
        @Override
        public EasyMultiChannel createMultiChannel(String[] channelNames,String providerName, Union union) {
            if(isDestroyed) return null;
            int length = channelNames.length;
            for(int i=0; i<length; ++i) {
                for(int j=i+1; j<length; ++j) {
                    if(channelNames[i].equals(channelNames[j])) {
                        setStatus(statusCreate.createStatus(StatusType.ERROR,
                                "duplicate channel name " + channelNames[i],
                                null));
                        return null;
                    }
                }
            }
            EasyMultiChannel easyMultiChannel = new EasyMultiChannelImpl(this,channelNames,providerName,union);
            LinkedListNode<EasyMultiChannel> listNode = easyMultiChannelListCreate.createNode(easyMultiChannel);
            easyMultiChannelList.addTail(listNode);
            return easyMultiChannel;
           
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
        private final EasyPVA easyPVA;
        private final String channelName;
        private final String providerName;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waitForConnect = lock.newCondition();

        private volatile Channel channel = null;
        private volatile boolean isDestroyed = false;
        private volatile Status status = statusCreate.getStatusOK();
        
        private volatile ConnectState connectState = ConnectState.connectIdle;
        private volatile boolean waitingForConnect = false;
        
        private boolean checkConnected() {
            if(connectState==ConnectState.connectIdle) connect(3.0);
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
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.Requester#getRequesterName()
         */
        @Override
        public String getRequesterName() {
            return easyPVA.getRequesterName();
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.Requester#message(java.lang.String, org.epics.pvdata.pv.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            if(isDestroyed) return;
            String mess = channelName + " " + message;
            easyPVA.message(mess, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelRequester#channelCreated(org.epics.pvdata.pv.Status, org.epics.pvaccess.client.Channel)
         */
        @Override
        public void channelCreated(Status status, Channel channel) {
            this.status = status;
            this.channel = channel;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelRequester#channelStateChange(org.epics.pvaccess.client.Channel, org.epics.pvaccess.client.Channel.ConnectionState)
         */
        @Override
        public void channelStateChange(Channel channel,ConnectionState connectionState) {
            synchronized (this) {
               if(isDestroyed) return;
               if(connectionState!=ConnectionState.CONNECTED) {
                  String message = channelName + " connection state " + connectionState.name();
                  message(message,MessageType.error);
                  Status status = statusCreate.createStatus(StatusType.ERROR,message,null);
                  setStatus(status);
                  connectState = ConnectState.notConnected;
               }
               else {
                   connectState = ConnectState.connected;
               }   
            }
            lock.lock();
            try {
                if(waitingForConnect) waitForConnect.signal();
            } finally {
               lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#destroy()
         */
        @Override
        public void destroy() {
            synchronized (this) {
               if(isDestroyed) return;
               isDestroyed = true;
            }
            channel.destroy();
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#getChannelName()
         */
        @Override
        public String getChannelName() {
            return channelName;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#getChannel()
         */
        @Override
        public Channel getChannel() {
            return channel;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#connect(double)
         */
        @Override
        public boolean connect(double timeout) {
            issueConnect();
            return waitConnect(timeout);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#issueConnect()
         */
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
            ChannelProvider channelProvider = ChannelProviderRegistryFactory.getChannelProviderRegistry().getProvider(providerName);
            channel = channelProvider.createChannel(channelName, this, ChannelProvider.PRIORITY_DEFAULT);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#waitConnect(double)
         */
        @Override
        public boolean waitConnect(double timeout) {
            if(isDestroyed) return false;
            try {
                lock.lock();
                try {
                    waitingForConnect = true;
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
                waitingForConnect = false;
                lock.unlock();
            }
            if(connectState!=ConnectState.connected) {
                return false;
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createField()
         */
        @Override
        public EasyField createField() {
        	return createField("");
        }

		/* (non-Javadoc)
		 * @see org.epics.pvaccess.easyPVA.EasyChannel#createField(java.lang.String)
		 */
		@Override
        public EasyField createField(String subField) {
		    String message = channelName + " createField is not implemented";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
	        return null;
        }

		/* (non-Javadoc)
		 * @see org.epics.pvaccess.easyPVA.EasyChannel#createProcess()
		 */
		@Override
        public EasyProcess createProcess() {
			return createProcess("");
        }

		/* (non-Javadoc)
		 * @see org.epics.pvaccess.easyPVA.EasyChannel#createProcess(java.lang.String)
		 */
		@Override
		public EasyProcess createProcess(String request) {
			PVStructure pvRequest = createRequest(request);
			if(pvRequest==null) return null;
			return createProcess(pvRequest);
		}

		/* (non-Javadoc)
		 * @see org.epics.pvaccess.easyPVA.EasyChannel#createProcess(org.epics.pvdata.pv.PVStructure)
		 */
		@Override
        public EasyProcess createProcess(PVStructure pvRequest) {
		    if(!checkConnected()) return null;
		    String message = channelName + " createProcess is not implemented";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
	        return null;
        }

		/* (non-Javadoc)
		 * @see org.epics.pvaccess.easyPVA.EasyChannel#createGet()
		 */
		@Override
        public EasyGet createGet() {
            return createGet("record[]field(value,alarm,timeStamp)");
        }
		

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createGet(java.lang.String)
         */
        @Override
        public EasyGet createGet(String request) {
            PVStructure pvStructure = createRequest(request);
            if(pvStructure==null) return null;
            return createGet(pvStructure);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createGet(org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public EasyGet createGet(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            return new EasyGetImpl(this,channel,pvRequest);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createPut()
         */
        @Override
        public EasyPut createPut() {
        	return createPut("record[]field(value)");
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createPut(java.lang.String)
         */
        @Override
        public EasyPut createPut(String request) {
        	 PVStructure pvStructure = createRequest(request);
             if(pvStructure==null) return null;
             return createPut(pvStructure);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createPut(org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public EasyPut createPut(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            return new EasyPutImpl(this,channel,pvRequest);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createPutGet()
         */
        @Override
        public EasyPutGet createPutGet() {
            return createPutGet("record[]putField(argument)getField(result)");
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createPutGet(java.lang.String)
         */
        @Override
        public EasyPutGet createPutGet(String request) {
            PVStructure pvStructure = createRequest(request);
            if(pvStructure==null) return null;
            return createPutGet(pvStructure);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createPutGet(org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public EasyPutGet createPutGet(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            String message = channelName + " createPutGet is not implemented";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
            return null;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createRPC()
         */
        @Override
        public EasyRPC createRPC() {
        	return createRPC((PVStructure)null);	// null allowed for RPC
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createRPC(java.lang.String)
         */
        @Override
        public EasyRPC createRPC(String request) {
       	 PVStructure pvStructure = createRequest(request);
         if(pvStructure==null) return null;
         return createRPC(pvStructure);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createRPC(org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public EasyRPC createRPC(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            return new EasyRPCImpl(this,channel,pvRequest);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createArray()
         */
        @Override
        public EasyArray createArray() {
            return createArray("");
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createArray(java.lang.String)
         */
        @Override
        public EasyArray createArray(String request) {
            PVStructure pvStructure = createRequest(request);
            if(pvStructure==null) return null;
            return createArray(pvStructure);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createArray(org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public EasyArray createArray(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            String message = channelName + " createArray is not implemented";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
            return null;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createMonitor()
         */
        @Override
        public EasyMonitor createMonitor() {
            return createMonitor("field()");
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createMonitor(java.lang.String)
         */
        @Override
        public EasyMonitor createMonitor(String request) {
            PVStructure pvStructure = createRequest(request);
            if(pvStructure==null) return null;
            return createMonitor(pvStructure);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#createMonitor(org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public EasyMonitor createMonitor(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            String message = channelName + " createMoinitor is not implemented";
            Status status = statusCreate.createStatus(StatusType.ERROR, message, null);
            setStatus(status);
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#setStatus(org.epics.pvdata.pv.Status)
         */
        @Override
        public void setStatus(Status status) {
        	this.status = status;
        	easyPVA.setStatus(status);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyChannel#getStatus()
         */
        @Override
        public Status getStatus() {
        	Status save = status;
        	status = statusCreate.getStatusOK();
            return save;
        }
    }

    private static class EasyGetImpl implements EasyGet, ChannelGetRequester {
        private final EasyChannelImpl easyChannel;
        private final Channel channel;
        private final PVStructure pvRequest;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waitForConnect = lock.newCondition();
        private final Condition waitForGet = lock.newCondition();
 
        private volatile boolean isDestroyed = false;
        private volatile Status status = statusCreate.getStatusOK();
        private volatile ChannelGet channelGet = null;
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
        
        private volatile PVStructure pvAlarmStructure = null;
        private volatile PVStructure pvTimeStampStructure = null;
        
        private final PVAlarm pvAlarm = PVAlarmFactory.create();
        private final Alarm alarm = new Alarm();
        private final PVTimeStamp pvTimeStamp = PVTimeStampFactory.create();
        private final TimeStamp timeStamp = TimeStampFactory.create();
        private final BooleanArrayData booleanArrayData = new BooleanArrayData();
        
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
            }
            else {
            	hasValue = false;
    			valueIsScalar = false;
    			scalarTypeValue = null;
    			valueIsNumeric = false;
            	// update structure info
                Field valueField = structure.getField("value");
                if (valueField != null) {
                	hasValue = true;
                	if (valueField.getType() == Type.scalar) {
            			valueIsScalar = true;
            			Scalar scalar = (Scalar)valueField;
            			scalarTypeValue = scalar.getScalarType();
            			valueIsNumeric = scalarTypeValue.isNumeric();
                	}
                	else if (valueField.getType() == Type.scalarArray) {
            			ScalarArray scalarArray = (ScalarArray)valueField;
            			scalarTypeValue = scalarArray.getElementType();
            			valueIsNumeric = scalarTypeValue.isNumeric();
                	}
                }
                
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
            PVStructure previousPvStructure = this.pvStructure;
            
            this.pvStructure = pvStructure;
            this.bitSet = bitSet;
            
            if(!status.isSuccess()) {
                setStatus(status);
            	getSuccess = false;
            } else {
            	if (previousPvStructure != pvStructure) {
                	// reset (not optimal, but simpler code)
        	        pvValue = null;
        	        pvScalarValue = null;
        	        pvArrayValue =  null;
        	        pvScalarArrayValue = null;
        	        pvBooleanValue = null;
            		
            		// update cached fields
            		if(hasValue){
    	            	pvValue = pvStructure.getSubField("value");
    	            	Field pvValueField = pvValue.getField();
	            		if(pvValueField.getType()==Type.scalar) {
	            			pvScalarValue = (PVScalar)pvValue;
	            			if(scalarTypeValue==ScalarType.pvBoolean) {
	            				pvBooleanValue = (PVBoolean)pvValue;
	            			}
	            		} else if(pvValueField.getType()==Type.scalarArray) {
	            			pvArrayValue = (PVArray)pvValue;
	            			pvScalarArrayValue = (PVScalarArray)pvValue;
	            		} else if(pvValueField.getType()==Type.structureArray) {
	            			pvArrayValue = (PVArray)pvValue;
	            		}
	            	}

	            	pvTimeStampStructure = pvStructure.getStructureField("timeStamp");
		            pvAlarmStructure = pvStructure.getStructureField("alarm");
            	}
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
        @Override
        public void destroy() {
            synchronized (this) {
               if(isDestroyed) return;
               isDestroyed = true;
            }
            if(channelGet!=null) channelGet.destroy();
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
            channelGet.get();
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
                    getSuccess = false;
                }
            } finally {
                lock.unlock();
            }
            getState = GetState.getIdle;
            return getSuccess;
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
            if(pvScalarArrayValue==null) {
            	Status status = statusCreate.createStatus(StatusType.ERROR," value is not a scalar array",null);
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
        private final EasyChannelImpl easyChannel;
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
            if(putState!=PutState.putIdle) {
                Status status = statusCreate.createStatus(
                        StatusType.ERROR,"put already issued",null);
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
        	getState = GetState.getIdle;
            return getSuccess;
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
        	if(pvScalarArrayValue==null) {
        		Status status = statusCreate.createStatus(StatusType.ERROR," value is not a scalar array",null);
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
    
    static private class EasyMultiChannelImpl implements EasyMultiChannel, ChannelRequester {
        private final EasyPVA easyPVA;
        private final String[] channelName;
        private final String providerName;
        private final Union union;
        private final int numChannel;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waitForConnect = lock.newCondition();

        
        private volatile int numConnected = 0;
        private volatile int numNewConnectRequest = 0;
        private volatile Channel[] channel = null;
        private volatile boolean[] connectRequested = null;
        private volatile boolean[] isConnected = null;
        private volatile boolean isDestroyed = false;
        private volatile Status[] channelStatus = null;
        private volatile Status status = statusCreate.getStatusOK();
        private volatile ConnectionState[] connectionState = null;
        
        private boolean checkConnected() {
            if(numConnected==0) connect(3.0);
            if(numConnected==channelName.length) return true;
            return false;
        }

        EasyMultiChannelImpl(EasyPVA easyPVA,String[] channelNames,String providerName,Union union) {
            this.easyPVA = easyPVA;
            this.channelName = channelNames;
            this.providerName = providerName;
            this.union = union;
            numChannel = channelNames.length;
            channel = new Channel[numChannel];
            connectRequested = new boolean[numChannel];
            isConnected = new boolean[numChannel];
            channelStatus = new Status[numChannel];
            connectionState = new ConnectionState[numChannel];
            for(int i=0; i<numChannel; ++i) {
                channel[i] = null;
                connectRequested[i] = false;
                isConnected[i] = false;
                channelStatus[i] = statusCreate.getStatusOK();
                connectionState[i] = ConnectionState.NEVER_CONNECTED;
            }
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
        public  void channelCreated(Status status, Channel channel) {
            for(int i=0; i<channelName.length; ++i) {
                if(channelName[i].equals(channel.getChannelName())) {
                    this.channel[i] = channel;
                    channelStatus[i] = status;
                    return;
                }
            }
        }
        @Override
        public void channelStateChange(Channel channel,ConnectionState connectionState) {
            synchronized (this) {
                if(isDestroyed) return;
                int index =-1;
                for(int i=0; i<channelName.length; ++i) {
                    if(channelName[i].equals(channel.getChannelName())) {
                        this.channel[i] = channel;
                        index = i;
                        break;
                    }
                }
                if(index<0) {
                    throw new IllegalStateException("should not happen");
                }
                this.connectionState[index] = connectionState;
                
                if(connectionState!=ConnectionState.CONNECTED) {
                    if(isConnected[index]) {
                        --numConnected;
                        isConnected[index] = false;
                    }
                    String message = channelName[index] + " connection state " + connectionState.name();
                    message(message,MessageType.error);
                    channelStatus[index] = statusCreate.createStatus(StatusType.ERROR,message,null);
                } else {
                    if(!isConnected[index]) {
                        ++numConnected;
                        isConnected[index] = true;
                    }
                }   
            }
            if(numConnected<this.channel.length) return;
            lock.lock();
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
            for(int i=0; i< channelName.length; ++i) {
                if(channel[i]!=null) channel[i].destroy();
            }
        }

       

        @Override
        public boolean connect(double timeout) {
            issueConnect();
            return waitConnect(timeout);
        }

        @Override
        public synchronized void issueConnect() {
            if(isDestroyed) return;
            if(numNewConnectRequest!=0) {
                 Status status = statusCreate.createStatus(
                     StatusType.ERROR, "multiChannel issueConnect called multiple times ",
                     null);
                 setStatus(status);
            }
            ChannelProvider channelProvider = ChannelProviderRegistryFactory.getChannelProviderRegistry().getProvider(providerName);
            numNewConnectRequest = 0;
            for(int i=0; i<channelName.length; ++i) {
                if(connectRequested[i]) continue;
                numNewConnectRequest++;
                connectRequested[i] = true;
                isConnected[i] = false;
                channel[i] = channelProvider.createChannel(channelName[i], this, ChannelProvider.PRIORITY_DEFAULT);   
            }
        }

        @Override
        public boolean waitConnect(double timeout) {
            if(isDestroyed) return false;
            int numNowConected = 0;
            while(true) {
                try {
                    lock.lock();
                    try {
                        if(numNewConnectRequest==0) break;
                        long nano = (long)(timeout*1e9);
                        numNowConected = numConnected;
                        if(numConnected<numChannel) waitForConnect.awaitNanos(nano);
                    } catch(InterruptedException e) {
                        Status status = statusCreate.createStatus(StatusType.ERROR,e.getMessage(), e.fillInStackTrace());
                        setStatus(status);
                        return false;
                    }
                } finally {
                    lock.unlock();
                }
                if(numConnected==numChannel) break;
                if(numNowConected<numConnected)  continue;
                break;
            }
            if(numConnected!=numChannel) {
                Status status = statusCreate.createStatus(StatusType.ERROR,"all channels are not connected",null);
                setStatus(status);
                return false;
            }
            return true;
        }
        
        @Override
        public boolean allConnected() {
           if(numConnected==numChannel) return true;
           return false;
        }

        @Override
        public boolean[] isConnected() {
            return isConnected;
        }

        @Override
        public EasyMultiGet createGet() {
            return createGet("field(value,alarm,timeStamp)");
        }
        

        @Override
        public EasyMultiGet createGet(String request) {
            PVStructure pvStructure = createRequest(request);
            if(pvStructure==null) return null;
            return createGet(pvStructure);
        }

        @Override
        public EasyMultiGet createGet(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            EasyMultiGetImpl multiGet = new EasyMultiGetImpl(this,channel,pvRequest,union);
            if(multiGet.init()) return multiGet;
            return null;
        }

        @Override
        public EasyMultiGet createGet(boolean doubleOnly) {
            return createGet(doubleOnly,"field(value)");
        }

        @Override
        public EasyMultiGet createGet(boolean doubleOnly, String request) {
            PVStructure pvStructure = createRequest(request);
            if(pvStructure==null) return null;
            return createGet(doubleOnly,pvStructure);
        }

        @Override
        public EasyMultiGet createGet(boolean doubleOnly, PVStructure pvRequest) {
            if(!checkConnected()) return null;
            EasyMultiGetImpl multiGet;
            Union union = this.union;
            if(doubleOnly) {
                Field[] field = new Field[1];
                String[] name = new String[1];
                name[0] = "double";
                field[0] = fieldCreate.createScalar(ScalarType.pvDouble);
                union = fieldCreate.createUnion(name, field);
            }
            multiGet = new EasyMultiGetImpl(this,channel,pvRequest,union);
            if(multiGet.init()) return multiGet;
            return null;
        }

        @Override
        public EasyMultiPut createPut() {
             return createPut(false);
        }
 
        @Override
        public EasyMultiPut createPut(boolean doubleOnly) {
            if(!checkConnected()) return null;
            EasyMultiPutImpl multiPut = new EasyMultiPutImpl(this,channel,doubleOnly);
            if(!multiPut.init()) return null;
            return multiPut;
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
    
    private static class EasyMultiGetImpl implements EasyMultiGet, ChannelGetRequester {
        private final EasyMultiChannelImpl easyMultiChannel;
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
        
        
        EasyMultiGetImpl(EasyMultiChannelImpl easyMultiChannel,Channel[] channel,PVStructure pvRequest,Union union) {
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
                pvChannelName.put(0, nchannel,easyMultiChannel.channelName, 0);
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
            pvChannelName.put(0, nchannel,easyMultiChannel.channelName, 0);
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
        
        @Override
        public String getRequesterName() {
            return easyMultiChannel.providerName;
        }
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
                if(easyMultiChannel.channelName[i].equals(channelGet.getChannel().getChannelName())) {
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
            numConnectCallback = 0;
            connectState = ConnectState.connectActive;
            for(int i=0; i<channel.length; ++i) channelGet[i] = channel[i].createChannelGet(this, pvRequest);
        }
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
        @Override
        public boolean get() {
            issueGet();
            return waitGet();
        }

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
       
        @Override
        public TimeStamp getTimeStamp() {
            if(pvTimeStampStructure!=null) {
               pvTimeStamp.attach(pvTimeStampStructure);
               pvTimeStamp.get(timeStamp);
            }
            return timeStamp;
        }
        @Override
        public int getLength() {
            return nchannel;
        }

        @Override
        public boolean doubleOnly() {
            return doubleOnly;
        }
        
        @Override
        public PVStructure getNTMultiChannel() {
            boolean result = checkGetState();
            if(!result) return null;
            if(doubleOnly) return null;
            return pvTop;
        }
        
        
        @Override
        public PVStructure getPVTop() {
            checkGetState();
            return pvTop;
        }

        @Override
        public double[] getDoubleArray() {
            boolean result = checkGetState();
            if(!result) return null;
            return doubleValue;
        }

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

        @Override
        public void setStatus(Status status) {
            this.status = status;
            easyMultiChannel.setStatus(status);
        }
        @Override
        public Status getStatus() {
            Status save = status;
            status = statusCreate.getStatusOK();
            return save;
        }
    }
    
    private static class EasyMultiPutImpl implements EasyMultiPut, ChannelPutRequester {
        private final EasyMultiChannelImpl easyMultiChannel;
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
        private final PVStructure pvRequest = createRequest("field(value)");
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waitForConnect = lock.newCondition();
        private final Condition waitForPut = lock.newCondition();
        private volatile Status status = statusCreate.getStatusOK();
        private volatile UnionArrayData unionArrayData = new UnionArrayData();


        
        
        EasyMultiPutImpl(EasyMultiChannelImpl easyMultiChannel,Channel[] channel,Boolean doubleOnly) {
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
        
        
        @Override
        public String getRequesterName() {
            return easyMultiChannel.providerName;
        }

        @Override
        public void message(String message, MessageType messageType) {
            if(isDestroyed) return;
            easyMultiChannel.message(message, messageType);
        }

        @Override
        public void channelPutConnect(Status status, ChannelPut channelPut,Structure structure) {
            if(isDestroyed) return;
            int index = -1;
            for(int i=0; i<channel.length; ++i) {
                if(easyMultiChannel.channelName[i].equals(channelPut.getChannel().getChannelName())) {
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

        @Override
        public void putDone(Status status, ChannelPut channelPut) {
            int index = -1;
            for(int i=0; i<channel.length; ++i) {
                if(easyMultiChannel.channelName[i].equals(channelPut.getChannel().getChannelName())) {
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
        @Override
        public void getDone(Status status, ChannelPut channelPut,PVStructure pvStructure, BitSet bitSet) {
            // using EasyMultiGet so this not used.
            
        }

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
            numConnectCallback = 0;
            connectState = ConnectState.connectActive;
            for(int i=0; i<channel.length; ++i) channelPut[i] = channel[i].createChannelPut(this, pvRequest);
        }

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

        @Override
        public boolean get() {
            return easyMultiGet.get();
        }

        @Override
        public void issueGet() {
            easyMultiGet.issueGet();
        }

        @Override
        public boolean waitGet() {
            return easyMultiGet.waitGet();
        }

        @Override
        public int getLength() {
            return nchannel;
        }

        @Override
        public boolean doubleOnly() {
            return doubleOnly;
        }

        @Override
        public PVStructure getNTMultiChannel() {
            if(doubleOnly) return null;
            return easyMultiGet.getNTMultiChannel();
        }

        @Override
        public PVStructure getPVTop() {
            return easyMultiGet.getPVTop();
        }

        @Override
        public double[] getDoubleArray() {
            return easyMultiGet.getDoubleArray();
        }

        @Override
        public int getDoubleArray(int index, double[] data, int length) {
            return easyMultiGet.getDoubleArray(index,data,length);
        }

        @Override
        public boolean put(PVStructure pvNTMultiChannel) {
            issuePut(pvNTMultiChannel);
            return waitPut();
        }

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

        @Override
        public boolean put(double[] value) {
            issuePut(value);
            return waitPut();
        }

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

        @Override
        public void setStatus(Status status) {
            this.status = status;
            easyMultiChannel.setStatus(status);
        }

        @Override
        public Status getStatus() {
            Status save = status;
            status = statusCreate.getStatusOK();
            return save;
        }
        
    }

	private static class EasyRPCImpl implements EasyRPC, ChannelRPCRequester {
	    private final EasyChannelImpl easyChannel;
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
	        channelRPC.request(request);
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
