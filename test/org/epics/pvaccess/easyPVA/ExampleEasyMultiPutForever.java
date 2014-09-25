/**
 * 
 */
package org.epics.pvaccess.easyPVA;

import junit.framework.TestCase;
/**
 * @author mrk
 *
 */
public class ExampleEasyMultiPutForever extends TestCase {
    static EasyPVA easyPVA = EasyPVAFactory.get();
    static int forever = 3;
   
    public static void testMultiPutForever() {
        int nchannel = 5;
        String[] channelName = new String[nchannel];
        for(int i=0; i<nchannel; ++i) channelName[i] = "double0" + i;
        EasyMultiPut multiPut = easyPVA.createMultiChannel(channelName,"ca").createPut(true);
        if(multiPut==null) {
            System.out.println(easyPVA.getStatus().getMessage());
        } else {
            double[] value = new double[nchannel];
            try {
                for(int time =0; time<forever; ++time) {
                    int num = multiPut.getDoubleArray(0, value, nchannel);
                    if(num!=nchannel) {
                        System.out.println("num!=length " +easyPVA.getStatus().getMessage());
                    } else {
                        System.out.print("getDoubleArray ");
                        for(int i=0; i<value.length; ++i) {
                            System.out.print(" " + value[i]);
                        }
                        System.out.println();
                        for(int i=0; i<num; ++i) value[i] = value[i] + 1;
                        boolean result = multiPut.put(value);
                        if(result) {
                            System.out.print("put            " );
                            for(int i=0; i<value.length; ++i) {
                                System.out.print(" " + value[i]);
                            }
                            System.out.println();
                        } else {
                            System.out.println("failure " + multiPut.getStatus().getMessage());
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {

            }
        }
        easyPVA.destroy();
        System.out.println("all done");
    }
}
