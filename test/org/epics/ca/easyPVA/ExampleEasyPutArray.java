/**
 * 
 */
package org.epics.ca.easyPVA;
import java.util.Date;

import org.epics.ca.client.*;
import org.epics.ca.client.Channel.ConnectionState;
import org.epics.pvData.misc.*;
import org.epics.pvData.property.*;
import org.epics.pvData.pv.*;
import org.epics.pvData.pv.Status.StatusType;
import org.epics.pvData.factory.*;
import org.epics.pvData.pv.Status;

/**
 * @author mrk
 *
 */
public class ExampleEasyPutArray {
    static EasyPVA easyPVA = EasyPVAFactory.get();
   
    public static void main(String[] args) {
        exampleDoubleArrayNoChecks("doubleArray01");
        exampleDoubleArray("doubleArray01");
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
        int len = 5;
        double[] value = new double[5];
        for(int i=0; i< len; i++) value[i] = i;
        int num = easyPVA.createChannel(
            channelName).createPut().putDoubleArray(value,len);
        System.out.printf("%s put %d elements%n",channelName,num);
    }
    
    static void exampleDoubleArray(String channelName) {
        easyPVA.setAuto(false, false);
        int length = 5;
        double[] value = new double[5];
        for(int i=0; i< length; i++) value[i] = i;
        EasyChannel channel =  easyPVA.createChannel(channelName);
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
        EasyGet easyGet = easyPVA.createChannel(channelName).createGet();
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
                    channelName).createPut().putBooleanArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvByte: {
                int length = 5;
                byte[] value = new byte[5];
                for(int i=0; i<length; i++) value[i] = (byte)i;
                int num  = easyPVA.createChannel(
                    channelName).createPut().putByteArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvShort: {
                int length = 5;
                short[] value = new short[5];
                for(int i=0; i<length; i++) value[i] = (short)(i*10);
                int num  = easyPVA.createChannel(
                    channelName).createPut().putShortArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvInt: {
                int length = 5;
                int[] value = new int[5];
                for(int i=0; i<length; i++) value[i] = i*100;
                int num  = easyPVA.createChannel(
                    channelName).createPut().putIntArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvLong: {
                int length = 5;
                long[] value = new long[5];
                for(int i=0; i<length; i++) value[i] = (long)(i*1000);
                int num  = easyPVA.createChannel(
                    channelName).createPut().putLongArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvFloat: {
                int length = 5;
                float[] value = new float[5];
                for(int i=0; i<length; i++) value[i] = i;
                int num  = easyPVA.createChannel(
                    channelName).createPut().putFloatArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
            case pvDouble : {
                int length = 5;
                double[] value = new double[5];
                for(int i=0; i<length; i++) value[i] = 1.5*(i*100);
                int num  = easyPVA.createChannel(
                    channelName).createPut().putDoubleArray(value,length);
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
                        channelName).createPut().putStringArray(value,length);
                System.out.printf("%s put %d values%n",channelName,num);
            }
            break;
        }
    }
}
