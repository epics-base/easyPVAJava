package org.epics.pvaccess.easyPVA.example;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.easyPVA.EasyMultiChannel;
import org.epics.pvaccess.easyPVA.EasyMultiMonitor;
import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;

public class ExampleMultiMonitorDouble
{

	static EasyPVA easyPVA= EasyPVAFactory.get();
	
	public static void main( String[] args )
	{
	    ReentrantLock lock = new ReentrantLock();
	    Condition waitForEvent = lock.newCondition();
	    int nchannel = 5;
        String[] channelName = new String[nchannel];
        for(int i=0; i<nchannel; ++i) channelName[i] = "double0" + i;
	    EasyMultiChannel multiChannel = easyPVA.createMultiChannel(channelName);
	    boolean result = multiChannel.connect(5.0, nchannel);
	    if(!result) {
            System.out.println("did not create monitor");
            return;
        }
	    EasyMultiMonitor monitor = multiChannel.createMonitor(true);
	    MyMonitor my = new MyMonitor(lock,waitForEvent);
	    monitor.setRequester(my);
        if(!monitor.start(.1))
        {
            System.out.println("could not start monitor");
            return;
        }
        double[] data = new double[monitor.getLength()];
        while(true) {
            lock.lock();
            try {
                waitForEvent.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            } finally {
                lock.unlock();
            }

            int numMonitor =  monitor.poll();
            while(numMonitor>0) {
                int length = monitor.getDoubleArray(0, data, nchannel);
                System.out.printf("length " + length + " value [");
                for(int i=0; i< length; ++i) {
                    if(i>0) System.out.printf(",");
                    System.out.printf(Double.toString(data[i]));
                }
                System.out.println("]");
                if(monitor.release()) {
                    numMonitor =  monitor.poll();
                } else {
                    numMonitor = 0;
                };
            }
        }
	}
	
	public static class MyMonitor implements EasyMultiMonitor.EasyMultiRequester {
	    ReentrantLock lock;
	    Condition waitForEvent;
	    MyMonitor(ReentrantLock lock,Condition waitForEvent)
	    {
	        this.lock = lock;
	        this.waitForEvent = waitForEvent;
	    }
        @Override
        public void event(EasyMultiMonitor monitor) {
            lock.lock();
            try {
                waitForEvent.signal();
            } finally {
                lock.unlock();
            }
        }
	}
}
