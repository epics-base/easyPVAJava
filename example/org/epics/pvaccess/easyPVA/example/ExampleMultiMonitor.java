package org.epics.pvaccess.easyPVA.example;

import static java.lang.System.out;

import org.epics.pvaccess.easyPVA.EasyMultiChannel;
import org.epics.pvaccess.easyPVA.EasyMultiMonitor;
import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;
import org.epics.pvdata.monitor.MonitorElement;

public class ExampleMultiMonitor
{

	static EasyPVA easyPVA= EasyPVAFactory.get();

	public static void main( String[] args )
	{
	    int nchannel = 5;
        String[] channelName = new String[nchannel];
        for(int i=0; i<nchannel; ++i) channelName[i] = "double0" + i;
	    EasyMultiChannel multiChannel = easyPVA.createMultiChannel(channelName);
	    boolean result = multiChannel.connect(5.0, nchannel);
	    if(!result) {
            System.out.println("did not create monitor");
            return;
        }
	    EasyMultiMonitor monitor = multiChannel.createMonitor();
        if(!monitor.start(.1))
        {
            System.out.println("could not start monitor");
            return;
        }
        
        MonitorElement[] monitorElements = monitor.getMonitorElement();
        while(true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }

            int numMonitor =  monitor.poll();
            while(numMonitor>0) {
                System.out.printf("multiMonitor %s%n",monitor.getNTMultiChannel());
                for(int i=0; i< nchannel; ++i) {
                    MonitorElement monitorElement = monitorElements[i];
                    if(monitorElement==null) continue;
                    out.println(monitorElement.getPVStructure());
                    out.println("changed " + monitorElement.getChangedBitSet());
                    out.println("overrun " + monitorElement.getOverrunBitSet());
                }
                if(monitor.release()) {
                    numMonitor =  monitor.poll();
                } else {
                    numMonitor = 0;
                }
            }
        }
	}
	
	public static class MyMonitor implements EasyMultiMonitor.EasyMultiRequester {

        @Override
        public void event(EasyMultiMonitor monitor) {
            // TODO Auto-generated method stub
            
        }
	    
	}
}
