/**
 * 
 */
package org.epics.ca.easyPVA;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.ScalarType;

/**
 * @author mrk
 *
 */
public class ExampleEasyGetArray {
    static EasyPVA easyPVA = EasyPVAFactory.get();
   
    public static void main(String[] args) {
        exampleDoubleArrayNoChecks("doubleArray01");
        exampleDoubleArray("doubleArray01");
        exampleDoubleArrayMultiCall("doubleArray01");
        exampleDoubleArray("stringArray01");
        exampleScalarArray("byteArray01");
        exampleScalarArray("shortArray01");
        exampleScalarArray("intArray01");
        exampleScalarArray("floatArray01");
        exampleScalarArray("doubleArray01");
        exampleScalarArray("stringArray01");
        easyPVA.destroy();
        System.out.println("all done");
    }
    
    static void exampleDoubleArrayNoChecks(String channelName) {
        double[] value = easyPVA.createChannel(
            channelName).createGet().getDoubleArray();
        System.out.printf("%s%n[",channelName);
        for(int i=0;i<value.length;i++) {
              if(i%10 == 0) {
                   System.out.printf("%n  ");
              }
              if(i!=0) System.out.printf(",");
              System.out.printf("%f",value[i]);
        }
        System.out.printf("%n]%n");
    }
    
    static void exampleDoubleArray(String channelName) {
        easyPVA.setAuto(false, true);
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
        EasyGet get = channel.createGet();
        result = get.connect();
        if(!result) {
            System.out.printf(
                "exampleDoubleCheck %s get connect failed %s%n",
                channelName,
                get.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        PVScalarArray pvArray = get.getScalarArrayValue();
        if(pvArray==null) {
            System.err.printf("%s is not a scalar array%s", channelName);
            easyPVA.setAuto(true, true);
            return;
        }
        if(!pvArray.getScalarArray().getElementType().isNumeric()) {
            System.err.printf("%s is not a numeric array%n", channelName);
            easyPVA.setAuto(true, true);
            return;
        }
        double[] value = get.getDoubleArray();
        System.out.printf("%s%n[",channelName);
        for(int i=0;i<value.length;i++) {
              if(i%10 == 0) {
                   System.out.printf("%n  ");
              }
              if(i!=0) System.out.printf(",");
              System.out.printf("%f",value[i]);
        }
        System.out.printf("%n]%n");
        channel.destroy();
        easyPVA.setAuto(true, true);
    }
    
    static void exampleDoubleArrayMultiCall(String channelName) {
        EasyGet easyGet = easyPVA.createChannel(channelName).createGet();
        PVScalarArray pvArray = easyGet.getScalarArrayValue();
        if(pvArray==null) {
            System.err.printf("%s is not a scalar array%s", channelName);
            return;
        }
        if(!pvArray.getScalarArray().getElementType().isNumeric()) {
            System.err.printf("%s is not a numeric array%n", channelName);
            return;
        }
        double[] value = easyGet.getDoubleArray();
        int length = value.length;
        System.out.printf("first getDoubleArray %s%n[",channelName);
        for(int i=0;i<length;i++) {
              if(i%10 == 0) {
                   System.out.printf("%n  ");
              }
              if(i!=0) System.out.printf(",");
              System.out.printf("%f",value[i]);
        }
        System.out.printf("%n]%n");
        // at some later time get using already created array
        easyGet.getDoubleArray(value, length);
        System.out.printf("second getDoubleArray %s%n[",channelName);
        for(int i=0;i<length;i++) {
              if(i%10 == 0) {
                   System.out.printf("%n  ");
              }
              if(i!=0) System.out.printf(",");
              System.out.printf("%f",value[i]);
        }
        System.out.printf("%n]%n");
    }

    static void exampleScalarArray(String channelName) {
        EasyGet easyGet = easyPVA.createChannel(channelName).createGet();
        PVScalarArray pvArray = easyGet.getScalarArrayValue();
        if(pvArray==null) {
            System.err.printf("%s is not a scalar array%s", channelName);
            return;
        }
        ScalarType scalarType = pvArray.getScalarArray().getElementType();
        switch(scalarType) {
            case pvBoolean: {
                boolean[] value = easyPVA.createChannel(
                    channelName).createGet().getBooleanArray();
                System.out.printf("%s%n[",channelName);
                for(int i=0;i<value.length;i++) {
                      if(i%10 == 0) {
                           System.out.printf("%n  ");
                      }
                      if(i!=0) System.out.printf(",");
                      System.out.printf("%b",value[i]);
                }
                System.out.printf("%n]%n");
            }
            break;
            case pvByte: {
                byte[] value = easyPVA.createChannel(
                    channelName).createGet().getByteArray();
                System.out.printf("%s%n[",channelName);
                for(int i=0;i<value.length;i++) {
                      if(i%10 == 0) {
                           System.out.printf("%n  ");
                      }
                      if(i!=0) System.out.printf(",");
                      System.out.printf("%d",value[i]);
                }
                System.out.printf("%n]%n");
            }
            break;
            case pvShort: {
                short[] value = easyPVA.createChannel(
                    channelName).createGet().getShortArray();
                System.out.printf("%s%n[",channelName);
                for(int i=0;i<value.length;i++) {
                      if(i%10 == 0) {
                           System.out.printf("%n  ");
                      }
                      if(i!=0) System.out.printf(",");
                      System.out.printf("%d",value[i]);
                }
                System.out.printf("%n]%n");
            }
            break;
            case pvInt: {
                int[] value = easyPVA.createChannel(
                    channelName).createGet().getIntArray();
                System.out.printf("%s%n[",channelName);
                for(int i=0;i<value.length;i++) {
                      if(i%10 == 0) {
                           System.out.printf("%n  ");
                      }
                      if(i!=0) System.out.printf(",");
                      System.out.printf("%d",value[i]);
                }
                System.out.printf("%n]%n");
            }
            break;
            case pvLong: {
                long[] value = easyPVA.createChannel(
                     channelName).createGet().getLongArray();
                System.out.printf("%s%n[",channelName);
                for(int i=0;i<value.length;i++) {
                      if(i%10 == 0) {
                           System.out.printf("%n  ");
                      }
                      if(i!=0) System.out.printf(",");
                      System.out.printf("%d",value[i]);
                }
                System.out.printf("%n]%n");
            }
            break;
            case pvFloat: {
                float[] value = easyPVA.createChannel(
                    channelName).createGet().getFloatArray();
                System.out.printf("%s%n[",channelName);
                for(int i=0;i<value.length;i++) {
                      if(i%10 == 0) {
                           System.out.printf("%n  ");
                      }
                      if(i!=0) System.out.printf(",");
                      System.out.printf("%f",value[i]);
                }
                System.out.printf("%n]%n");
            }
            break;
            case pvDouble : {
                double[] value = easyPVA.createChannel(
                    channelName).createGet().getDoubleArray();
                System.out.printf("%s%n[",channelName);
                for(int i=0;i<value.length;i++) {
                      if(i%10 == 0) {
                           System.out.printf("%n  ");
                      }
                      if(i!=0) System.out.printf(",");
                      System.out.printf("%f",value[i]);
                }
                System.out.printf("%n]%n");
            }
            break;
            case pvString : {
                String[] value = easyPVA.createChannel(
                    channelName).createGet().getStringArray();
                System.out.printf("%s%n[",channelName);
                for(int i=0;i<value.length;i++) {
                      if(i%10 == 0) {
                           System.out.printf("%n  ");
                      }
                      if(i!=0) System.out.printf(",");
                      System.out.printf("%s",value[i]);
                }
                System.out.printf("%n]%n");
            }
            break;
        }
    }
}
