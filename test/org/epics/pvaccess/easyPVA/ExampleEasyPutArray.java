/**
 * 
 */
package org.epics.pvaccess.easyPVA;
import org.epics.pvaccess.easyPVA.EasyChannel;
import org.epics.pvaccess.easyPVA.EasyGet;
import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;
import org.epics.pvaccess.easyPVA.EasyPut;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Type;
import junit.framework.TestCase;

/**
 * @author mrk
 *
 */
public class ExampleEasyPutArray extends TestCase {
    static EasyPVA easyPVA = EasyPVAFactory.get();
   
    public static void testPutArray() {
        exampleDoubleArrayNoChecks("doubleArray01");
        
        exampleStringArray("byteArray01");
        exampleStringArray("shortArray01");
        exampleStringArray("intArray01");
        exampleStringArray("floatArray01");
        exampleStringArray("doubleArray01");
        exampleStringArray("stringArray01");
        
        System.out.println("following will cause error");
        exampleStringArray("string01");
        System.out.println("following will cause error");
        exampleDoubleArray("stringArray01");
        
        exampleDoubleArray("doubleArray01");
        
        exampleScalarArray("byteArray01");
        exampleScalarArray("shortArray01");
        exampleScalarArray("intArray01");
        exampleScalarArray("floatArray01");
        exampleScalarArray("doubleArray01");
        exampleScalarArray("stringArray01");
        System.out.println("all done");
    }
    
    static void exampleDoubleArrayNoChecks(String channelName) {
        int len = 5;
        double[] value = new double[5];
        for(int i=0; i< len; i++) value[i] = i;
        int num = easyPVA.createChannel(
            channelName,"ca").createPut().putDoubleArray(value,len);
        System.out.printf("%s put %d elements%n",channelName,num);
    }
    
    static void exampleStringArray(String channelName) {
        String[] value = easyPVA.createChannel(
                channelName,"ca").createGet().getStringArray();
        System.out.printf("%s%n[",channelName);
        int num = easyPVA.createChannel(
                channelName,"ca").createPut().putStringArray(value,value.length);
        System.out.printf("%s put %d elements%n",channelName,num);
    }
    
    static void exampleDoubleArray(String channelName) {
        easyPVA.setAuto(false, false);
        int length = 5;
        double[] value = new double[5];
        for(int i=0; i< length; i++) value[i] = i;
        EasyChannel channel =  easyPVA.createChannel(channelName,"ca");
        boolean result = channel.connect(2.0);
        if(!result) {
            System.err.printf(
                "exampleDoubleCheck %s channel connect failed %s%n",
                channelName,
                channel.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        EasyPut put = channel.createPut();
        result = put.connect();
        if(!result) {
            System.err.printf(
                "exampleDoubleCheck %s put connect failed %s%n",
                channelName,
                put.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        PVField pvField = put.getValue();
        if(pvField.getField().getType()!=Type.scalarArray) {
            System.err.printf(
                    "exampleDoubleCheck %s value is not a scalar aarray %s%n",
                    channelName,
                    put.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        PVScalarArray pvScalarArray = (PVScalarArray)pvField;
        if(!pvScalarArray.getScalarArray().getElementType().isNumeric()) {
            System.err.printf(
                    "exampleDoubleCheck %s value is not a numeric scalar array %s%n",
                    channelName,
                    put.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        put.putDoubleArray(value, length);
        put.issuePut();
        result = put.waitPut();
        if(!result) {
            System.err.printf(
                "exampleDoubleCheck %s wait put failed %s%n",
                channelName,
                put.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        channel.destroy();
        easyPVA.setAuto(true, true);
    }

    static void exampleScalarArray(String channelName) {
        EasyGet easyGet = easyPVA.createChannel(channelName,"ca").createGet();
        PVScalarArray pvArray = easyGet.getScalarArrayValue();
        if(pvArray==null) {
            System.err.printf("%s is not a scalar array%s", channelName);
            return;
        }
        ScalarType scalarType = pvArray.getScalarArray().getElementType();
        switch(scalarType) {
            case pvBoolean: {
                int length = 5;
                boolean[] value = new boolean[5];
                value[0] = value[4] = true;
                value[1] = value[2] = value[3] = false;
                int num  = easyPVA.createChannel(
                    channelName,"ca").createPut().putBooleanArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvUByte:
            case pvByte:
            {
                int length = 5;
                byte[] value = new byte[5];
                for(int i=0; i<length; i++) value[i] = (byte)i;
                int num  = easyPVA.createChannel(
                    channelName,"ca").createPut().putByteArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvUShort:
            case pvShort:
            {
                int length = 5;
                short[] value = new short[5];
                for(int i=0; i<length; i++) value[i] = (short)(i*10);
                int num  = easyPVA.createChannel(
                    channelName,"ca").createPut().putShortArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvUInt:
            case pvInt:
            {
                int length = 5;
                int[] value = new int[5];
                for(int i=0; i<length; i++) value[i] = i*100;
                int num  = easyPVA.createChannel(
                    channelName,"ca").createPut().putIntArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvULong:
            case pvLong:
            {
                int length = 5;
                long[] value = new long[5];
                for(int i=0; i<length; i++) value[i] = (long)(i*1000);
                int num  = easyPVA.createChannel(
                    channelName,"ca").createPut().putLongArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvFloat: {
                int length = 5;
                float[] value = new float[5];
                for(int i=0; i<length; i++) value[i] = i;
                int num  = easyPVA.createChannel(
                    channelName,"ca").createPut().putFloatArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvDouble : {
                int length = 5;
                double[] value = new double[5];
                for(int i=0; i<length; i++) value[i] = 1.5*(i*100);
                int num  = easyPVA.createChannel(
                    channelName,"ca").createPut().putDoubleArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvString : {
                int length = 5;
                String[] value = new String[5];
                value[0] = "name 0";
                value[1] = "name 1";
                value[2] = "name 2";
                value[3] = "name 3";
                value[4] = "name 4";
                int num  = easyPVA.createChannel(
                        channelName,"ca").createPut().putStringArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
        }
    }
}
