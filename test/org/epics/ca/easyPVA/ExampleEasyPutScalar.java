/**
 * 
 */
package org.epics.ca.easyPVA;

/**
 * @author mrk
 *
 */
public class ExampleEasyPutScalar {
	static EasyPVA easyPVA = EasyPVAFactory.get();
   
    public static void main(String[] args) {
    	String channelName = "byte01";
    	double setValue = 1.0; 
    	exampleDouble(channelName,setValue);
    	exampleDoublePrint(channelName,setValue);
    	exampleDoubleCheck(channelName,setValue);
    	channelName = "short01";
    	setValue = 2.0; 
    	exampleDouble(channelName,setValue);
    	exampleDoublePrint(channelName,setValue);
    	channelName = "int01";
    	setValue = 3.0; 
    	exampleDouble(channelName,setValue);
    	exampleDoublePrint(channelName,setValue);
    	channelName = "float01";
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
        easyPVA.createChannel(channelName).createPut("record[process=true]field(value)").putDouble(value);
    }
    
    static void exampleDoublePrint(String channelName,double setValue) {
        // get the scalar value
        double value = easyPVA.createChannel(channelName).createGet().getDouble();
        System.out.printf("channelName %s setValue %f value %f%n",channelName,setValue,value);
    }
    
    static void exampleDoubleCheck(String channelName,double value) {
        easyPVA.setAuto(false, false);
        EasyChannel channel =  easyPVA.createChannel(channelName);
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
