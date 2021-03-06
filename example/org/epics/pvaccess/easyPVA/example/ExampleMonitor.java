
package org.epics.pvaccess.easyPVA.example;

import static java.lang.System.out;

import org.epics.pvaccess.easyPVA.EasyMonitor;
import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;
import org.epics.pvdata.monitor.MonitorElement;


public class ExampleMonitor {
    private static final EasyPVA easyPVA = EasyPVAFactory.get();
    
    public static void main(String[] args) {
        if(args.length==0) {
            out.println("no channelName");
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
                out.println(monitorElement.getPVStructure());
                out.println("changed " + monitorElement.getChangedBitSet());
                out.println("overrun " + monitorElement.getOverrunBitSet());
                monitor.releaseEvent();
                monitorElement = monitor.poll();
            }
        }
    }

}
