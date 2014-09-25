/**
 * 
 */
package org.epics.pvaccess.easyPVA;
import junit.framework.TestCase;

/**
 * @author mrk
 *
 */
public class ExampleEasyPutArrayForever extends TestCase{
    static EasyPVA easyPVA = EasyPVAFactory.get();
    static int forever = 3;
   
    public static void testPutArrayForever() {
        String channelName = "doubleArray01";
        EasyPut easyPut = easyPVA.createChannel(channelName,"ca").createPut();
        if(easyPut==null) {
            System.out.println(easyPVA.getStatus().getMessage());
        } else {
            int length = 5;
            double[] value = new double[5];
            int num = easyPut.putDoubleArray(value,length);
            if(num<=0) {
                System.out.println(easyPVA.getStatus().getMessage());
            }
            try {
                for(int time =0; time<forever; ++time) {
                    num = easyPut.getDoubleArray(value, length);
                    if(num<=0) {
                        System.out.println(easyPVA.getStatus().getMessage());
                    }
                    for(int i=0; i<length; ++i) value[i] +=1.0;
                    num = easyPut.putDoubleArray(value, length);
                    if(num<=0) {
                        System.out.println(easyPVA.getStatus().getMessage());
                    }
                    for(int i=0; i<num; ++i) {
                        if(i>0) System.out.print(" ");
                        System.out.print(value[i]);
                    }
                    System.out.println();
                    Thread.sleep(1000);
                }
            } catch (Exception e) {

            }
        }
        easyPVA.destroy();
        System.out.println("all done");
    }
}
