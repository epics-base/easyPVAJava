/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import org.epics.pvaccess.easyPVA.EasyChannel;
import junit.framework.TestCase;
import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;
import org.epics.pvaccess.easyPVA.EasyPut;

/**
 * @author mrk
 *
 */
public class ExampleEasyPutScalar extends TestCase {
	static EasyPVA easyPVA = EasyPVAFactory.get();
   
    public static void testPutScalar() {
    	String channelName = "int00";
    	double setValue = 1.0; 
    	exampleDouble(channelName,setValue);
    	exampleDoublePrint(channelName,setValue);
    	exampleDoubleCheck(channelName,setValue);
    	channelName = "int01";
    	setValue = 2.0; 
    	exampleDouble(channelName,setValue);
    	exampleDoublePrint(channelName,setValue);
    	channelName = "calc00";
    	setValue = 3.0; 
    	exampleDouble(channelName,setValue);
    	exampleDoublePrint(channelName,setValue);
    	channelName = "double00";
    	setValue = 4.0; 
    	exampleDouble(channelName,setValue);
    	exampleDoublePrint(channelName,setValue);
    	channelName = "double01";
    	setValue = 5.0; 
    	exampleDouble(channelName,setValue);
    	exampleDoublePrint(channelName,setValue);
        
        System.out.println("all done");
    }

    static void exampleDouble(String channelName, double value) {
        // put the scalar value
        easyPVA.createChannel(channelName,"ca").createPut("record[process=true]field(value)").putDouble(value);
    }
    
    static void exampleDoublePrint(String channelName,double setValue) {
        // get the scalar value
        double value = easyPVA.createChannel(channelName,"ca").createPut().getDouble();
        if(!easyPVA.createChannel(channelName,"ca").createPut().putDouble(value)) {
            System.out.printf(
                    "exampleDoublePrint %s put failed %s%n",
                    channelName,
                    easyPVA.getStatus());
            return;
        }
        System.out.printf("channelName %s setValue %f value %f%n",channelName,setValue,value);
    }
    
    static void exampleString(String channelName) {
        String value = easyPVA.createChannel(channelName).createGet().getString();
        easyPVA.createChannel(channelName).createPut().putString(value);
    }
    
    static void exampleDoubleCheck(String channelName,double value) {
        easyPVA.setAuto(false, false);
        EasyChannel channel =  easyPVA.createChannel(channelName,"ca");
        boolean result = channel.connect(2.0);
        if(!result) {
            System.out.printf(
                    "exampleDoubleCheck %s channel connect failed %s%n",
                    channelName,
                    channel.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        EasyPut put = channel.createPut();
        result = put.connect();
        if(!result) {
            System.out.printf(
                    "exampleDoubleCheck %s get connect failed %s%n",
                    channelName,
                    put.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        result = put.putDouble(value);
        if(!result) {
            System.out.printf(
                "exampleDoubleCheck %s putDouble failed %s%n",
                channelName,
                put.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        put.issuePut();
        result = put.waitPut();
        if(!result) {
            System.out.printf(
                "exampleDoubleCheck %s waitPut failed %s%n",
                channelName,
                put.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        easyPVA.setAuto(true, true);
    }
       
}
