/**
 * 
 */
package org.epics.pvaccess.easyPVA;
import org.epics.pvdata.pv.PVStructure;
import junit.framework.TestCase;

/**
 * @author mrk
 *
 */
public class ExampleEasyMultiGet  extends TestCase{
    static EasyPVA easyPVA = EasyPVAFactory.get();
   
    public static void testMultiGet() {
        int nchannel = 5;
        String[] channelName = new String[nchannel];
        for(int i=0; i<nchannel; ++i) channelName[i] = "double0" + i;
        exampleCheck(channelName);
        exampleSimple(channelName);
        for(int i=0; i<nchannel; ++i) channelName[i] = "string0" + i;
        exampleSimple(channelName);
        channelName[0] = "double00";
        channelName[1] = "int01";
        channelName[2] = "stringArray01";
        channelName[3] = "doubleArray01";
        exampleSimple(channelName);
        for(int i=0; i<nchannel; ++i) channelName[i] = "double0" + i;
        exampleDoubleCheck(channelName);
        exampleDoubleSimple(channelName);
        channelName[2] = "int00";
        channelName[3] = "int01";
        exampleDoubleCheck(channelName);
        exampleDoubleSimple(channelName);
        channelName[0] = "junk";
        exampleCheck(channelName);
        exampleSimple(channelName);
        System.out.println("all done");
    }

    static void exampleSimple(String[] channelName) {
        System.out.println();
        System.out.println("exampleSimple");
        try {
            PVStructure pvStructure = easyPVA.createMultiChannel(channelName,"ca").createGet().getNTMultiChannel();
            System.out.println(pvStructure);
        } catch( Exception e) {
            System.out.println("exception " + e.getMessage());
            StackTraceElement[] element = e.getStackTrace();
            for(int i=0; i<element.length; ++i) {
                System.out.println(element[i].toString());
            }
        }
    }
    
    static void exampleCheck(String[] channelName) {
        System.out.println();
        System.out.println("exampleCheck");
        easyPVA.setAuto(false, true);
        EasyMultiChannel channel =  easyPVA.createMultiChannel(channelName,"ca");
        boolean result = channel.connect(2.0);
        if(!result) {
            System.out.printf(
                "exampleCheck %s channel connect failed %s%n",
                channelName,
                channel.getStatus());
            boolean allConnected = channel.allConnected();
            System.out.println("all connected " + allConnected);
            if(!allConnected) {
                boolean[] connected = channel.isConnected();
                for(int i=0; i<channelName.length; ++i) {
                    if(connected[i]) continue;
                    System.out.println("channel " + channelName[i] + " not connected");
                }
            }
            easyPVA.setAuto(true, true);
            return;
        }
        EasyMultiGet get = channel.createGet();
        if(get==null) {
            System.out.printf(
                    "exampleCheck %s createGet failed%n");
                easyPVA.setAuto(true, true);
                return;
        }
        result = get.connect();
        if(!result) {
            System.out.printf(
                "exampleCheck %s get connect failed %s%n",
                channelName,
                get.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        get.issueGet();
        result = get.waitGet();
        if(!result) {
            System.out.printf(
                "exampleCheck %s get failed %s%n",
                channelName,
                get.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        System.out.printf("multiGet %s%n",get.getNTMultiChannel());
        channel.destroy();
        easyPVA.setAuto(true, true);
    }
    
    static void exampleDoubleSimple(String[] channelName) {
        System.out.println();
        System.out.println("exampleDoubleSimple");
        try {
            double[] value = easyPVA.createMultiChannel(channelName,"ca").createGet(true).getDoubleArray();
            System.out.println("value length " + value.length);
            for(int i=0; i<value.length; ++i) System.out.println(value[i]);
        } catch( Exception e) {
            System.out.println("exception " + e.getMessage());
            StackTraceElement[] element = e.getStackTrace();
            for(int i=0; i<element.length; ++i) {
                System.out.println(element[i].toString());
            }
        }
    }
    
    static void exampleDoubleCheck(String[] channelName) {
        System.out.println();
        System.out.println("exampleDoubleCheck");
        easyPVA.setAuto(false, true);
        EasyMultiChannel channel =  easyPVA.createMultiChannel(channelName,"ca");
        boolean result = channel.connect(2.0);
        if(!result) {
            System.out.printf(
                "exampleCheck %s channel connect failed %s%n",
                channelName,
                channel.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        EasyMultiGet get = channel.createGet(true);
        if(get==null) {
            System.out.printf(
                    "exampleCheck %s createGet failed%n");
                easyPVA.setAuto(true, true);
                return;
        }
        result = get.connect();
        if(!result) {
            System.out.printf(
                "exampleCheck %s get connect failed %s%n",
                channelName,
                get.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        get.issueGet();
        result = get.waitGet();
        if(!result) {
            System.out.printf(
                "exampleCheck %s get failed %s%n",
                channelName,
                get.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        System.out.printf("multiGet %s%n",get.getPVTop());
        double[] value = get.getDoubleArray();
        System.out.println("value length " + value.length);
        for(int i=0; i<value.length; ++i) System.out.println(value[i]);
        channel.destroy();
        easyPVA.setAuto(true, true);
    }
    
}
