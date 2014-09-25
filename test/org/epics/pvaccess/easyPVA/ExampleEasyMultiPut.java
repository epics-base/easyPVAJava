/**
 * 
 */
package org.epics.pvaccess.easyPVA;
import org.epics.pvdata.pv.*;
import org.epics.pvdata.factory.*;
import junit.framework.TestCase;

/**
 * @author mrk
 *
 */
public class ExampleEasyMultiPut  extends TestCase{
    static EasyPVA easyPVA = EasyPVAFactory.get();
    static UnionArrayData unionArrayData = new UnionArrayData();
    static Convert convert = ConvertFactory.getConvert();
    
   
    public static void testMultiPut() {
        int nchannel = 5;
        String[] channelName = new String[nchannel];
        for(int i=0; i<nchannel; ++i) channelName[i] = "double0" + i;
        exampleUnionArrayCheck(channelName);
        exampleUnionArraySimple(channelName);
        for(int i=0; i<nchannel; ++i) channelName[i] = "string0" + i;
        exampleUnionArraySimple(channelName);
        channelName[0] = "double00";
        channelName[1] = "int00";
        channelName[2] = "stringArray01";
        channelName[3] = "doubleArray01";
        exampleUnionArraySimple(channelName);
        for(int i=0; i<nchannel; ++i) channelName[i] = "double0" + i;
        exampleDoubleCheck(channelName);
        exampleDoubleSimple(channelName);
        channelName[2] = "int00";
        channelName[3] = "int01";
        channelName[4] = "calc00";
        exampleDoubleCheck(channelName);
        exampleDoubleSimple(channelName);
        channelName[0] = "junk";
        System.out.println("following will fail with message");
        exampleUnionArrayCheck(channelName);
        System.out.println("following will generate exception");
        exampleUnionArraySimple(channelName);
        easyPVA.destroy();
        System.out.println("all done");
    }
    
    static PVStructure exampleUnionArrayCommon(EasyMultiPut multiPut) {
        PVStructure pvStructure = multiPut.getNTMultiChannel();
        PVUnionArray pvUnionArray = pvStructure.getSubField(PVUnionArray.class, "value");
        int length = pvUnionArray.getLength();
        pvUnionArray.get(0,length,unionArrayData);
        for(int i=0; i<length; ++i) {
            PVUnion pvUnion = unionArrayData.data[i];
            PVField pvField = pvUnion.get();
            Type type = pvField.getField().getType();
            if(type==Type.scalar) {
                PVScalar pvScalar =(PVScalar)pvField;
                ScalarType scalarType = pvScalar.getScalar().getScalarType();
                if(scalarType.isNumeric()) {
                    double value = convert.toDouble(pvScalar);
                    value += i +1;
                    convert.fromDouble(pvScalar, value);
                } else if(scalarType==ScalarType.pvBoolean) {
                    PVBoolean pvBoolean = (PVBoolean)pvScalar;
                    pvBoolean.put(!pvBoolean.get());
                } else if(scalarType==ScalarType.pvString) {
                    PVString pvString = (PVString)pvScalar;
                    String value = pvString.get();
                    if(value.length()<1) value = "string";
                    value += i +1;
                    pvString.put(value);
                }
            } else if(type==Type.scalarArray) {
                PVScalarArray pvScalarArray = (PVScalarArray)pvField;
                ScalarType scalarType = pvScalarArray.getScalarArray().getElementType();
                if(scalarType.isNumeric()) {
                    int len = pvScalarArray.getLength();
                    double[] value = new double[len];
                    convert.toDoubleArray(pvScalarArray, 0, len, value, 0);
                    for(int j=0; j<len; j++) value[j] += i+j;
                    convert.fromDoubleArray(pvScalarArray, 0, len, value, 0);
                } else if(scalarType==ScalarType.pvBoolean) {
                    PVBooleanArray pvBoolean = (PVBooleanArray)pvScalarArray;
                    int len = pvScalarArray.getLength();
                    BooleanArrayData data = new BooleanArrayData();
                    pvBoolean.get(0, len, data);
                    for(int j=0; j<len; j++) data.data[j] = !data.data[j];
                } else if(scalarType==ScalarType.pvString) {
                    PVStringArray pvString = (PVStringArray)pvScalarArray;
                    int len = pvScalarArray.getLength();
                    StringArrayData data = new StringArrayData();
                    pvString.get(0, len, data);
                    for(int j=0; j<len; j++) {
                        String value = data.data[j];
                        value += i +j;
                        data.data[j] = value;
                    }
                }
            }
        }
        return pvStructure;
    }
    
    static void exampleUnionArraySimple(String[] channelName) {
        System.out.println();
        System.out.println("exampleUnionArraySimple");
        try {
            EasyMultiPut multiPut = easyPVA.createMultiChannel(channelName,"ca").createPut(false);
            PVStructure pvStructure = exampleUnionArrayCommon(multiPut);
            multiPut.put(pvStructure);
            multiPut.get();
            System.out.println(multiPut.getNTMultiChannel());
            
        } catch( Exception e) {
            System.out.println("exception " + e.getMessage());
            StackTraceElement[] element = e.getStackTrace();
            for(int i=0; i<element.length; ++i) {
                System.out.println(element[i].toString());
            }
        }
    }
    
    static void exampleUnionArrayCheck(String[] channelName) {
        System.out.println();
        System.out.println("exampleUnionArrayCheck");
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
        EasyMultiPut multiPut = channel.createPut(false);
        if(multiPut==null) {
            System.out.printf(
                    "exampleCheck %s createGet failed%n");
                easyPVA.setAuto(true, true);
                return;
        }
        result = multiPut.connect();
        if(!result) {
            System.out.printf(
                "exampleCheck %s get connect failed %s%n",
                channelName,
                multiPut.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        multiPut.issueGet();
        result = multiPut.waitGet();
        if(!result) {
            System.out.printf(
                "exampleCheck %s get failed %s%n",
                channelName,
                multiPut.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        PVStructure pvStructure = exampleUnionArrayCommon(multiPut);
        multiPut.issuePut(pvStructure);
        result = multiPut.waitPut();
        if(!result) {
            System.out.printf(
                "exampleCheck %s put failed %s%n",
                channelName,
                multiPut.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        multiPut.issueGet();
        result = multiPut.waitGet();
        if(!result) {
            System.out.printf(
                "exampleCheck %s get failed %s%n",
                channelName,
                multiPut.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        System.out.println(multiPut.getNTMultiChannel());
        channel.destroy();
        easyPVA.setAuto(true, true);
    }
    
    static void exampleDoubleSimple(String[] channelName) {
        System.out.println();
        System.out.println("exampleDoubleSimple");
        try {
            EasyMultiPut multiPut = easyPVA.createMultiChannel(channelName,"ca").createPut(true);
            double[] value = multiPut.getDoubleArray();
            for(int i=0; i<value.length; ++i) value[i] = value[i] + 1;
            boolean result = multiPut.put(value);
            if(result) {
                System.out.println("success");
            } else {
                System.out.println("failure " + multiPut.getStatus().getMessage());
            }
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
        EasyMultiPut multiPut = channel.createPut(true);
        if(multiPut==null) {
            System.out.printf(
                    "exampleCheck %s createGet failed%n");
                easyPVA.setAuto(true, true);
                return;
        }
        result = multiPut.connect();
        if(!result) {
            System.out.printf(
                "exampleCheck %s get connect failed %s%n",
                channelName,
                multiPut.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        multiPut.issueGet();
        result = multiPut.waitGet();
        if(!result) {
            System.out.printf(
                "exampleCheck %s get failed %s%n",
                channelName,
                multiPut.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        System.out.printf("multiPut original value  %s%n",multiPut.getPVTop());
        double[] value = multiPut.getDoubleArray();
        for(int i=0; i<value.length; ++i) value[i] = value[i] + 1;
        result = multiPut.put(value);
        if(result) {
            System.out.println("success");
        } else {
            System.out.println("failure " + multiPut.getStatus().getMessage());
        }
        System.out.printf("multiPut new value  %s%n",multiPut.getPVTop());
        channel.destroy();
        easyPVA.setAuto(true, true);
    }
    
}
