
package org.epics.pvaccess.easyPVA.example;

import static java.lang.System.out;

import java.util.Date;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvaccess.easyPVA.EasyMonitor;
import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;
import org.epics.pvaccess.easyPVA.EasyPVStructure;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;


public class ExampleMonitorCallback {
    private static final EasyPVA easyPVA = EasyPVAFactory.get();
    
    public static void main(String[] args) {
        if(args.length==0) {
            out.println("no channelName");
            return;
        }
        EasyPVStructure ePVstructure = easyPVA.createEasyPVStructure();
        MonitorRequester monitorRequester = new MonitorRequester();
        Status status = ePVstructure.getStatus();
        String channelName = args[0];
        EasyMonitor monitor = easyPVA.createChannel(channelName).createMonitor("field()");
        if(monitor==null) {
            System.out.println("did not create monitor");
            return;
        }
        monitor.setRequester(monitorRequester);
        if(!monitor.start())
        {
            System.out.println("could not start monitor");
            return;
        }
        
        while(true) {
            monitorRequester.waitForMonitor();
            MonitorElement monitorElement = monitor.poll();
            while(monitorElement!=null) {
                PVStructure pvStructure = monitorElement.getPVStructure();
                out.println(monitorElement.getPVStructure());
                out.println("changed " + monitorElement.getChangedBitSet());
                out.println("overrun " + monitorElement.getOverrunBitSet());
                ePVstructure.setPVStructure(pvStructure);
                ePVstructure.getStatus(); // clear any outstanding status
                double dvalue = ePVstructure.getDouble();
                status = ePVstructure.getStatus();
                if(dvalue!=0.0 || status.isOK()) {
                    out.println("double " + dvalue);
                }
                double[] darray = ePVstructure.getDoubleArray();
                status = ePVstructure.getStatus();
                if(status.isOK() && darray.length>0) {
                    int len = darray.length;
                    out.println("doubleArray len " + len + " [0]=" + darray[0] + " [" + (len-1) + "]="  + darray[len-1]);
                }
                Alarm alarm =ePVstructure.getAlarm();
                status = ePVstructure.getStatus();
                if(status.isOK()) {
                    out.println("alarm message " + alarm.getMessage() + " severity " + alarm.getSeverity().name() + " status " + alarm.getStatus().name());
                }
                TimeStamp timeStamp =ePVstructure.getTimeStamp();
                status = ePVstructure.getStatus();
                if(status.isOK()) {
                    long milliPastEpoch = timeStamp.getMilliSeconds();
                    int userTag = timeStamp.getUserTag();
                    Date date = new Date(milliPastEpoch);
                    out.println(String.format("timeStamp %tF %tT.%tL userTag %d",date,date,date,userTag));
                }
                // Note that ePVstructure has many other methods.
                monitor.releaseEvent();
                monitorElement = monitor.poll();
            }
        }
    }
    
    private static class MonitorRequester implements EasyMonitor.EasyRequester{
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waitForEvent = lock.newCondition();
        MonitorRequester() {}
        
        @Override
        public void event(EasyMonitor monitor) {
            lock.lock();
            try {
                waitForEvent.signal();
            } finally {
               lock.unlock();
            }
        }
        
        void waitForMonitor()
        {
            lock.lock();
            try {
                waitForEvent.signal();
            } finally {
               lock.unlock();
            }
        }
    }

}
