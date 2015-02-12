
package org.epics.pvaccess.easyPVA.example;

import org.epics.pvaccess.easyPVA.*;
import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;
import org.epics.pvdata.monitor.*;

public class ExampleEasyMonitor {
    private static final EasyPVA easyPVA = EasyPVAFactory.get();
    
    public static void main(String[] args) {
        if(args.length==0) {
            System.out.println("no channelName");
            return;
        }
        String channelName = args[0];
        EasyMonitor monitor = easyPVA.createChannel(channelName).createMonitor("field()");
       
        if(monitor==null) {
            System.out.println("did not create monitor");
            return;
        }
        if(!monitor.start())
        {
            System.out.println("could not start monitor");
            return;
        }
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            MonitorElement monitorElement = monitor.poll();
            while(monitorElement!=null) {
                System.out.println(monitorElement.getPVStructure());
                System.out.println("changed " + monitorElement.getChangedBitSet());
                System.out.println("overrun " + monitorElement.getOverrunBitSet());
                monitor.releaseEvent();
                monitorElement = monitor.poll();
            }
        }
    }

}
