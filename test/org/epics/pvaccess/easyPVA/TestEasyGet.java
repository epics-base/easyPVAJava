package org.epics.pvaccess.easyPVA;

import java.util.Date;

import junit.framework.TestCase;

import org.epics.pvaccess.easyPVA.EasyChannel;
import org.epics.pvaccess.easyPVA.EasyGet;
import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;
import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Status;


public class TestEasyGet extends TestCase {
    private static final EasyPVA easyPVA = EasyPVAFactory.get();
    
    public static void bad(Status status) {
        fail(status.getType().name() + " " +status.getMessage());
    }
    
    public static EasyGet getCommon(String testName,EasyChannel channel) {
        System.out.printf("%n%s channel %s%n",testName,channel.getChannelName());
        boolean result = channel.connect(2.0);
        if(!result) bad(channel.getStatus());
        EasyGet easyGet = channel.createGet();
        result = easyGet.connect();
        if(!result) bad(easyGet.getStatus());
        easyGet.get();
        PVStructure pvStructure = easyGet.getPVStructure();
        System.out.printf("pvStructure %n %s%n", pvStructure);
        Alarm alarm = easyGet.getAlarm();
        System.out.printf("severity %s status %s message %s%n",alarm.getSeverity().name(),alarm.getStatus().name(),alarm.getMessage());
        TimeStamp timeStamp = easyGet.getTimeStamp();
        long milliPastEpoch = timeStamp.getMilliSeconds();
        int userTag = timeStamp.getUserTag();
        Date date = new Date(milliPastEpoch);
        System.out.println(String.format("timeStamp %tF %tT.%tL userTag %d", date,date,date,userTag));
        return easyGet;
    }
    
    public static void printScalar(EasyGet easyGet) {
        PVField pvField = easyGet.getValue();
        if(pvField==null) {
            System.out.println("value field does not exists");
            return;
        }
        PVScalar pvScalar = easyGet.getScalarValue();
        if(pvScalar==null) {
            System.out.println("value field is not a scalar");
            return;
        }
        ScalarType scalarType = pvScalar.getScalar().getScalarType();
        if(scalarType==ScalarType.pvBoolean) {
            System.out.println("bollean " + easyGet.getBoolean());
            return;
        }
        if(scalarType==ScalarType.pvString) {
            System.out.println("string " + easyGet.getString());
            return;
        }
        System.out.println("  byte " + easyGet.getByte());
        System.out.println(" short " + easyGet.getShort());
        System.out.println("   int " + easyGet.getInt());
        System.out.println("  long " + easyGet.getLong());
        System.out.println(" float " + easyGet.getFloat());
        System.out.println("double " + easyGet.getDouble());
    }
            
    public static void printArray(EasyGet easyGet) {
        PVField pvField = easyGet.getValue();
        if(pvField==null) {
            System.out.println("value field does not exists");
            return;
        }
        PVScalarArray pvScalarArray = easyGet.getScalarArrayValue();
        if(pvScalarArray==null) {
            System.out.println("value field is not scalar array");
            return;
        }
        int length = pvScalarArray.getLength();
        ScalarType scalarType = pvScalarArray.getScalarArray().getElementType();
        if(scalarType==ScalarType.pvBoolean) {
            boolean[] bvalue = new boolean[length];
            int len = easyGet.getBooleanArray(bvalue, length);
            System.out.printf("%nboolean length %d len %d%n[",length,len);
            for(int i=0;i<len;i++) {
                  if(i%10 == 0) {
                       System.out.printf("%n  ");
                  }
                  if(i!=0) System.out.printf(",");
                  System.out.printf("%b",bvalue[i]);
            }
            System.out.printf("%n]%n");
            boolean[] value = easyGet.getBooleanArray();
            assertTrue(value.length==length);
            for(int i=0; i<length; i++) assertTrue(value[i]==bvalue[i]);
            return;
        }
        if(scalarType==ScalarType.pvString) {
            String[] svalue = new String[length];
            int len = easyGet.getStringArray(svalue, length);
            System.out.printf("%nstring length %d len %d%n[",length,len);
            for(int i=0;i<len;i++) {
                if(i%10 == 0) {
                    System.out.printf("%n  ");
                }
                if(i!=0) System.out.printf(",");
                System.out.printf("%s",svalue[i]);
            }
            System.out.printf("%n]%n");
            String[] value = easyGet.getStringArray();
            assertTrue(value.length==length);
            for(int i=0; i<length; i++) assertTrue(value[i]==svalue[i]);
            return;
        }
        
        byte[] bvalue = new byte[length];
        int len = easyGet.getByteArray(bvalue, length);
        System.out.printf("%nbyte length %d len %d%n[",length,len);
        for(int i=0;i<len;i++) {
            if(i%10 == 0) {
                System.out.printf("%n  ");
            }
            if(i!=0) System.out.printf(",");
            System.out.printf("%d",bvalue[i]);
        }
        System.out.printf("%n]%n");
        byte[] bbvalue = easyGet.getByteArray();
        assertTrue(bbvalue.length==length);
        for(int i=0; i<length; i++) assertTrue(bbvalue[i]==bvalue[i]);
        
        short[] svalue = new short[length];
        len = easyGet.getShortArray(svalue, length);
        System.out.printf("%n short length %d len %d%n[",length,len);
        for(int i=0;i<len;i++) {
            if(i%10 == 0) {
                System.out.printf("%n  ");
            }
            if(i!=0) System.out.printf(",");
            System.out.printf("%d",svalue[i]);
        }
        System.out.printf("%n]%n");
        short[] ssvalue = easyGet.getShortArray();
        assertTrue(ssvalue.length==length);
        for(int i=0; i<length; i++) assertTrue(ssvalue[i]==svalue[i]);
        
        int[] ivalue = new int[length];
        len = easyGet.getIntArray(ivalue, length);
        System.out.printf("%n int length %d len %d%n[",length,len);
        for(int i=0;i<len;i++) {
            if(i%10 == 0) {
                System.out.printf("%n  ");
            }
            if(i!=0) System.out.printf(",");
            System.out.printf("%d",ivalue[i]);
        }
        System.out.printf("%n]%n");
        int[] iivalue = easyGet.getIntArray();
        assertTrue(iivalue.length==length);
        for(int i=0; i<length; i++) assertTrue(iivalue[i]==ivalue[i]);
        
        long[] lvalue = new long[length];
        len = easyGet.getLongArray(lvalue, length);
        System.out.printf("%n long length %d len %d%n[",length,len);
        for(int i=0;i<len;i++) {
            if(i%10 == 0) {
                System.out.printf("%n  ");
            }
            if(i!=0) System.out.printf(",");
            System.out.printf("%d",lvalue[i]);
        }
        System.out.printf("%n]%n");
        long[] llvalue = easyGet.getLongArray();
        assertTrue(llvalue.length==length);
        for(int i=0; i<length; i++) assertTrue(llvalue[i]==lvalue[i]);
        
        float[] fvalue = new float[length];
        len = easyGet.getFloatArray(fvalue, length);
        System.out.printf("%n float length %d len %d%n[",length,len);
        for(int i=0;i<len;i++) {
            if(i%10 == 0) {
                System.out.printf("%n  ");
            }
            if(i!=0) System.out.printf(",");
            System.out.printf("%e",fvalue[i]);
        }
        System.out.printf("%n]%n");
        float[] ffvalue = easyGet.getFloatArray();
        assertTrue(ffvalue.length==length);
        for(int i=0; i<length; i++) assertTrue(ffvalue[i]==fvalue[i]);
        
        double[] dvalue = new double[length];
        len = easyGet.getDoubleArray(dvalue, length);
        System.out.printf("%n double length %d len %d%n[",length,len);
        for(int i=0;i<len;i++) {
            if(i%10 == 0) {
                System.out.printf("%n  ");
            }
            if(i!=0) System.out.printf(",");
            System.out.printf("%e",dvalue[i]);
        }
        System.out.printf("%n]%n");
        double[] ddvalue = easyGet.getDoubleArray();
        assertTrue(ddvalue.length==length);
        for(int i=0; i<length; i++) assertTrue(ddvalue[i]==dvalue[i]);
    }
    
    public void testGetNumericScalar() {
        EasyChannel channel = easyPVA.createChannel("byte01");
        EasyGet easyGet = getCommon("testGetNumericScalar",channel);
        printScalar(easyGet);
        channel.destroy();

        channel = easyPVA.createChannel("short01");
        easyGet = getCommon("testGetNumericScalar",channel);
        printScalar(easyGet);

        channel = easyPVA.createChannel("int01","ca");
        easyGet = getCommon("testGetNumericScalar",channel);
        printScalar(easyGet);

        channel = easyPVA.createChannel("long01");
        easyGet = getCommon("testGetNumericScalar",channel);
        printScalar(easyGet);

        channel = easyPVA.createChannel("float01");
        easyGet = getCommon("testGetNumericScalar",channel);
        printScalar(easyGet);

        channel = easyPVA.createChannel("double01","ca");
        easyGet = getCommon("testGetNumericScalar",channel);
        printScalar(easyGet);

        channel = easyPVA.createChannel("string01","ca");
        easyGet = getCommon("testGetNumericScalar",channel);
        printScalar(easyGet);
    }
    
    public void testGetNumericArray() {
        EasyChannel channel = easyPVA.createChannel("byteArray01","ca");
        EasyGet easyGet = getCommon("testGetNumericArray",channel);
        printArray(easyGet);
        
        channel = easyPVA.createChannel("shortArray01","ca");
        easyGet = getCommon("testGetNumericArray",channel);
        printArray(easyGet);
        
        channel = easyPVA.createChannel("intArray01","ca");
        easyGet = getCommon("testGetNumericArray",channel);
        printArray(easyGet);
        
        //channel = easyPVA.createChannel("longArray01");
        //easyGet = getCommon("testGetNumericArray",channel);
        //printArray(easyGet);
        
        channel = easyPVA.createChannel("floatArray01","ca");
        easyGet = getCommon("testGetNumericArray",channel);
        printArray(easyGet);
        
        channel = easyPVA.createChannel("doubleArray01","ca");
        easyGet = getCommon("testGetNumericArray",channel);
        printArray(easyGet);
        
        channel = easyPVA.createChannel("stringArray01","ca");
        easyGet = getCommon("testGetNumericArray",channel);
        printArray(easyGet);
    }
    
    
}
