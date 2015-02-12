/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelProviderRegistryFactory;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.misc.LinkedList;
import org.epics.pvdata.misc.LinkedListCreate;
import org.epics.pvdata.misc.LinkedListNode;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Union;


/**
 * The factory that creates an instance of EasyPVA.
 * @author mrk
 *
 */
public class EasyPVAFactory {
	
    /**
     * Create an instance of EasyPVA.
     * @return The newly created EasyPVA.
     */
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

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#destroy()
         */
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
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#createChannel(java.lang.String)
         */
        @Override
        public EasyChannel createChannel(String channelName) {
            return createChannel(channelName,defaultProvider);
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#createChannel(java.lang.String, java.lang.String)
         */
        @Override
        public EasyChannel createChannel(String channelName,String providerName) {
            if(isDestroyed) return null;
            EasyChannel easyChannel = new EasyChannelImpl(this,channelName,providerName);
            LinkedListNode<EasyChannel> listNode = easyChannelListCreate.createNode(easyChannel);
            easyChannelList.addTail(listNode);
            return easyChannel;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#createMultiChannel(java.lang.String[])
         */
        @Override
        public EasyMultiChannel createMultiChannel(String[] channelNames) {
            return createMultiChannel(channelNames,defaultProvider);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#createMultiChannel(java.lang.String[], java.lang.String)
         */
        @Override
        public EasyMultiChannel createMultiChannel(String[] channelNames,String providerName) {
            return createMultiChannel(channelNames,providerName,variantUnion);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#createMultiChannel(java.lang.String[], java.lang.String, org.epics.pvdata.pv.Union)
         */
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
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#setRequester(org.epics.pvdata.pv.Requester)
         */
        @Override
        public void setRequester(Requester requester) {
        	this.requester = requester;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#clearRequester()
         */
        @Override
        public void clearRequester() {
        	requester = null;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#setStatus(org.epics.pvdata.pv.Status)
         */
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
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#getStatus()
         */
        @Override
        public Status getStatus() {
        	Status save = status;
        	status = statusCreate.getStatusOK();
        	return save;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#setAuto(boolean, boolean)
         */
        @Override
        public void setAuto(boolean get, boolean put) {
        	this.autoGet = get;
        	this.autoPut = put;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#isAutoGet()
         */
        @Override
        public boolean isAutoGet() {
        	return autoGet;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#isAutoPut()
         */
        @Override
        public boolean isAutoPut() {
        	return autoPut;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyPVA#setAutoMessage(org.epics.pvdata.pv.Status.StatusType)
         */
        @Override
        public void setAutoMessage(StatusType type) {
        	this.autoMessageThreashold = type;
        }
		/* (non-Javadoc)
		 * @see org.epics.pvdata.pv.Requester#getRequesterName()
		 */
		@Override
        public String getRequesterName() {
			if(requester!=null) return requester.getRequesterName();
	        return easyPVAName;
        }
		/* (non-Javadoc)
		 * @see org.epics.pvdata.pv.Requester#message(java.lang.String, org.epics.pvdata.pv.MessageType)
		 */
		@Override
        public void message(String message, MessageType messageType) {
	        if(requester!=null) {
	        	requester.message(message, messageType);
	        	return;
	        }
	        System.out.printf("%s %s%n", messageType.name(),message);
        }
    }

    static private class EasyChannelImpl implements EasyChannel,ChannelRequester {
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
            return EasyGetImpl.create(this,channel,pvRequest);
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
            return EasyPutImpl.create(this,channel,pvRequest);
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
            return EasyRPCImpl.create(this,channel,pvRequest);
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
            return createMonitor("field(value,alarm,timeStamp)");
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
            return EasyMonitorImpl.create(this, channel, pvRequest);
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
        public  void channelCreated(Status status, Channel channel) {
            for(int i=0; i<channelName.length; ++i) {
                if(channelName[i].equals(channel.getChannelName())) {
                    this.channel[i] = channel;
                    channelStatus[i] = status;
                    return;
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelRequester#channelStateChange(org.epics.pvaccess.client.Channel, org.epics.pvaccess.client.Channel.ConnectionState)
         */
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
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#destroy()
         */
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
        public String getProviderName() {
            return providerName;
        }

        @Override
        public String[] getChannelNames() {
            return channelName;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#connect(double)
         */
        @Override
        public boolean connect(double timeout) {
            issueConnect();
            return waitConnect(timeout);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#issueConnect()
         */
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

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#waitConnect(double)
         */
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
        
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#allConnected()
         */
        @Override
        public boolean allConnected() {
           if(numConnected==numChannel) return true;
           return false;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#isConnected()
         */
        @Override
        public boolean[] isConnected() {
            return isConnected;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#createGet()
         */
        @Override
        public EasyMultiGet createGet() {
            return createGet("field(value,alarm,timeStamp)");
        }
        

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#createGet(java.lang.String)
         */
        @Override
        public EasyMultiGet createGet(String request) {
            PVStructure pvStructure = createRequest(request);
            if(pvStructure==null) return null;
            return createGet(pvStructure);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#createGet(org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public EasyMultiGet createGet(PVStructure pvRequest) {
            if(!checkConnected()) return null;
            EasyMultiGet multiGet = EasyMultiGetImpl.create(this,channel,pvRequest,union);
            if(multiGet.init()) return multiGet;
            return null;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#createGet(boolean)
         */
        @Override
        public EasyMultiGet createGet(boolean doubleOnly) {
            return createGet(doubleOnly,"field(value)");
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#createGet(boolean, java.lang.String)
         */
        @Override
        public EasyMultiGet createGet(boolean doubleOnly, String request) {
            PVStructure pvStructure = createRequest(request);
            if(pvStructure==null) return null;
            return createGet(doubleOnly,pvStructure);
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#createGet(boolean, org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public EasyMultiGet createGet(boolean doubleOnly, PVStructure pvRequest) {
            if(!checkConnected()) return null;
            Union union = this.union;
            if(doubleOnly) {
                Field[] field = new Field[1];
                String[] name = new String[1];
                name[0] = "double";
                field[0] = fieldCreate.createScalar(ScalarType.pvDouble);
                union = fieldCreate.createUnion(name, field);
            }
            EasyMultiGet  multiGet = EasyMultiGetImpl.create(this,channel,pvRequest,union);
            if(multiGet.init()) return multiGet;
            return null;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#createPut()
         */
        @Override
        public EasyMultiPut createPut() {
             return createPut(false);
        }
 
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#createPut(boolean)
         */
        @Override
        public EasyMultiPut createPut(boolean doubleOnly) {
            if(!checkConnected()) return null;
            EasyMultiPut multiPut = EasyMultiPutImpl.create(this,channel,doubleOnly);
            if(!multiPut.init()) return null;
            return multiPut;
        }

        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#setStatus(org.epics.pvdata.pv.Status)
         */
        @Override
        public void setStatus(Status status) {
            this.status = status;
            easyPVA.setStatus(status);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.easyPVA.EasyMultiChannel#getStatus()
         */
        @Override
        public Status getStatus() {
            Status save = status;
            status = statusCreate.getStatusOK();
            return save;
        }
    }
}
