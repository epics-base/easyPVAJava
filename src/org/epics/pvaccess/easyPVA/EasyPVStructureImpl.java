package org.epics.pvaccess.easyPVA;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.PVAlarm;
import org.epics.pvdata.property.PVAlarmFactory;
import org.epics.pvdata.property.PVTimeStamp;
import org.epics.pvdata.property.PVTimeStampFactory;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
import org.epics.pvdata.pv.BooleanArrayData;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVBooleanArray;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Type;

public class EasyPVStructureImpl {
    
    /**
     * Factory for creating a new EasyPVStructure.
     * @return The interface.
     */
    static EasyPVStructure create() {
        return new EPVStructure();
    }

    private static class EPVStructure implements EasyPVStructure {
        
        public EPVStructure() {}
        
        private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
        private static final Convert convert = ConvertFactory.getConvert();
        private Status statusOK = statusCreate.getStatusOK();
        private Status status = statusOK;
        private Status statusNoPVStructure = statusCreate.createStatus(StatusType.ERROR,"setPVStructure not called",null);
        private Status statusNoValue = statusCreate.createStatus(StatusType.ERROR,"no value field",null);
        private Status statusNoScalar = statusCreate.createStatus(StatusType.ERROR,"value is not a scalar",null);
        private Status statusNoNumericScalar = statusCreate.createStatus(StatusType.ERROR,"value is not a numeric scalar",null);
        private Status statusNoScalarArray = statusCreate.createStatus(StatusType.ERROR,"value is not a scalarArray",null);
        private Status statusNoNumericScalarArray = statusCreate.createStatus(StatusType.ERROR,"value is not a numeric scalarArray",null);
        

        private PVStructure pvStructure = null;
        private PVField pvValue;

        private final PVAlarm pvAlarm = PVAlarmFactory.create();
        private final Alarm alarm = new Alarm();
        private final PVTimeStamp pvTimeStamp = PVTimeStampFactory.create();
        private final TimeStamp timeStamp = TimeStampFactory.create();
        private final BooleanArrayData booleanArrayData = new BooleanArrayData();

        private boolean checkPVStructure()
        {
            if(pvStructure!=null) return true;
            status = statusNoPVStructure;
            return false;
        }
        
        private boolean checkValue()
        {
            if(pvValue==null) {
                status = statusNoValue;
                return false;
            }
            return true;
        }

        private PVScalar checkScalar()
        {
            if(!checkPVStructure()) return null;
            PVScalar pv = pvStructure.getSubField(PVScalar.class,"value");
            if(pv==null) {
                status = statusNoScalar;
                return null;
            }
            return pv;
        }
        
        private PVScalar checkNumericScalar() {
            PVScalar pv = checkScalar();
            if(pv==null) return null;
            if(!pv.getScalar().getScalarType().isNumeric()) {
                status = statusNoNumericScalar;
                return null;
            }
            return pv;
        }

        private PVScalarArray checkScalarArray()
        {
            if(!checkPVStructure()) return null;
            PVScalarArray pv = pvStructure.getSubField(PVScalarArray.class,"value");
            if(pv==null) {
                status = statusNoScalarArray;
                return null;
            }
            return pv;
        }
        private PVScalarArray checkNumericScalarArray() {
            PVScalarArray pv = checkScalarArray();
            if(pv==null) return null;
            if(!pv.getScalarArray().getElementType().isNumeric()) {
                status = statusNoNumericScalarArray;
                return null;
            }
            return pv;
        }

        public void setPVStructure(PVStructure pvStructure)
        {
            this.pvStructure = pvStructure;
            this.pvValue = pvStructure.getSubField("value");
        }

        public Status getStatus() {
            Status save = status;
            status = statusCreate.getStatusOK();
            return save;
        }

        

        public PVField getValue() {
            if(!checkPVStructure()) return null;
            PVField pv = pvStructure.getSubField("value");
            if(pv==null) {
                status = statusCreate.createStatus(StatusType.ERROR,"no value field",null);
                return null;
            }
            return pv;
        }

        public PVScalar getScalarValue() {
            return checkScalar();
        }

        public PVScalarArray getScalarArrayValue() {
            return checkScalarArray();
        }

        public Alarm getAlarm() {
            if(!checkPVStructure()) return null;
            PVStructure xxx = pvStructure.getSubField(PVStructure.class,"alarm");
            if(xxx!=null) {
                pvAlarm.attach(xxx);
                if(pvAlarm.isAttached()) {
                    pvAlarm.get(alarm);
                    pvAlarm.detach();
                    return alarm;
                }
            }
            status = statusCreate.createStatus(StatusType.ERROR,"no alarm field",null);
            return null;
        }

        public TimeStamp getTimeStamp() {
            if(!checkPVStructure()) return null;
            PVStructure xxx = pvStructure.getSubField(PVStructure.class,"timeStamp");
            if(xxx!=null) {
                pvTimeStamp.attach(xxx);
                if(pvTimeStamp.isAttached()) {
                    pvTimeStamp.get(timeStamp);
                    pvTimeStamp.detach();
                    return timeStamp;
                }
            }
            status = statusCreate.createStatus(StatusType.ERROR,"no timeStamp field",null);
            return null;
        }
 
        public boolean hasValue() {
            if(pvValue==null) return false;
            return true;
        }
        public boolean isValueScalar() {
            PVScalar pv = checkScalar();
            if(pv==null) return false;
            return true;
        }

        public boolean getBoolean() {
            PVScalar pvScalar = checkScalar();
            if(pvScalar==null) return false;
            if(pvScalar.getScalar().getScalarType()!=ScalarType.pvBoolean) {
                status = statusCreate.createStatus(StatusType.ERROR,"value is not boolean",null);
                return false;
            }
            PVBoolean pv = (PVBoolean)pvScalar;
            return pv.get();
        }

        public byte getByte() {
            PVScalar pvScalar = checkNumericScalar();
            if(pvScalar==null) return 0;
            return convert.toByte(pvScalar);
        }
        public short getShort() {
            PVScalar pvScalar = checkNumericScalar();
            if(pvScalar==null) return 0;
            return convert.toShort(pvScalar);
        }
        public int getInt() {
            PVScalar pvScalar = checkNumericScalar();
            if(pvScalar==null) return 0;
            return convert.toInt(pvScalar);
        }
        public long getLong() {
            PVScalar pvScalar = checkNumericScalar();
            if(pvScalar==null) return 0;
            return convert.toLong(pvScalar);
        }
        public float getFloat() {
            PVScalar pvScalar = checkNumericScalar();
            if(pvScalar==null) return 0;
            return convert.toFloat(pvScalar);
        }
        public double getDouble() {
            PVScalar pvScalar = checkNumericScalar();
            if(pvScalar==null) return 0;
            return convert.toDouble(pvScalar);
        }
        public String getString() {
            if(!checkValue()) return null;
            return pvValue.toString();
        }
        public boolean[] getBooleanArray() {
            PVScalarArray pvScalarArray = checkScalarArray();
            if(pvScalarArray==null) return new boolean[0];
            if(pvScalarArray.getScalarArray().getElementType()!=ScalarType.pvBoolean) {
                status = statusCreate.createStatus(StatusType.ERROR,"value is not boolean array",null);
                return new boolean[0];
            }
            PVBooleanArray pv = (PVBooleanArray)pvScalarArray;
            int length = pv.getLength();
            boolean[] data = new boolean[length];
            getBooleanArray(data,length);
            return data;
        }
        
        public byte[] getByteArray() {
            PVScalarArray pvScalarArray = checkNumericScalarArray();
            if(pvScalarArray==null) return new byte[0];
            int length = pvScalarArray.getLength();
            byte[] data = new byte[length];
            getByteArray(data,length);
            return data;
        }

        public short[] getShortArray() {
            PVScalarArray pvScalarArray = checkScalarArray();
            if(pvScalarArray==null) return new short[0];
            int length = pvScalarArray.getLength();
            short[] data = new short[length];
            getShortArray(data,length);
            return data;
        }

        public int[] getIntArray() {
            PVScalarArray pvScalarArray = checkScalarArray();
            if(pvScalarArray==null) return new int[0];
            int length = pvScalarArray.getLength();
            int[] data = new int[length];
            getIntArray(data,length);
            return data;
        }

        public long[] getLongArray() {
            PVScalarArray pvScalarArray = checkScalarArray();
            if(pvScalarArray==null) return new long[0];
            int length = pvScalarArray.getLength();
            long[] data = new long[length];
            getLongArray(data,length);
            return data;
        }

        public float[] getFloatArray() {
            PVScalarArray pvScalarArray = checkScalarArray();
            if(pvScalarArray==null) return new float[0];
            int length = pvScalarArray.getLength();
            float[] data = new float[length];
            getFloatArray(data,length);
            return data;
        }

        public double[] getDoubleArray() {
            PVScalarArray pvScalarArray = checkScalarArray();
            if(pvScalarArray==null) return new double[0];
            int length = pvScalarArray.getLength();
            double[] data = new double[length];
            getDoubleArray(data,length);
            return data;
        }

        public String[] getStringArray() {
            PVScalarArray pvScalarArray = checkScalarArray();
            if(pvScalarArray==null) return new String[0];
            int length = pvScalarArray.getLength();
            String[] data = new String[length];
            getStringArray(data,length);
            return data;
        }
        
        public int getBooleanArray(boolean[] value, int length) {
            PVScalarArray pvScalarArray = checkScalarArray();
            if(pvScalarArray==null) return 0;
            if(pvScalarArray.getScalarArray().getElementType()!=ScalarType.pvBoolean) {
                status = statusCreate.createStatus(StatusType.ERROR,"value is not boolean array",null);
                return 0;
            }
            PVBooleanArray pvdata = (PVBooleanArray)pvScalarArray;
            int len = length;
            int ntransfered = 0;
            int offset = 0;
            while (len > 0) {
                int num = 0;
                boolean[] dataArray = null;
                int dataOffset = 0;
                synchronized (booleanArrayData) {
                    num = pvdata.get(offset, len, booleanArrayData);
                    dataArray = booleanArrayData.data;
                    dataOffset = booleanArrayData.offset;
                }
                if (num <= 0)
                    break;
                System.arraycopy(dataArray, dataOffset, value,offset, num);
                len -= num;
                offset += num;
                ntransfered += num;
            }
            return ntransfered;
        }

        

        public PVArray getArrayValue() {
            if(!checkValue()) return null;
            if(pvValue.getField().getType()==Type.scalarArray) return (PVArray)pvValue;
            if(pvValue.getField().getType()==Type.unionArray) return (PVArray)pvValue;
            if(pvValue.getField().getType()==Type.structureArray) return (PVArray)pvValue;
            return null;
        }

        public int getByteArray(byte[] value, int length) {
            PVScalarArray pv = checkNumericScalarArray();
            if(pv==null) return 0;
            return convert.toByteArray(pv, 0, length, value, 0);
        }

        public int getShortArray(short[] value, int length) {
            PVScalarArray pv = checkNumericScalarArray();
            if(pv==null) return 0;
            return convert.toShortArray(pv, 0, length, value, 0);
        }

        public int getIntArray(int[] value, int length) {
            PVScalarArray pv = checkNumericScalarArray();
            if(pv==null) return 0;
            return convert.toIntArray(pv, 0, length, value, 0);
        }

        public int getLongArray(long[] value, int length) {
            PVScalarArray pv = checkNumericScalarArray();
            if(pv==null) return 0;
            return convert.toLongArray(pv, 0, length, value, 0);
        }

        public int getFloatArray(float[] value, int length) {
            PVScalarArray pv = checkNumericScalarArray();
            if(pv==null) return 0;
            return convert.toFloatArray(pv, 0, length, value, 0);
        }

        public int getDoubleArray(double[] value, int length) {
            PVScalarArray pv = checkNumericScalarArray();
            if(pv==null) return 0;
            return convert.toDoubleArray(pv, 0, length, value, 0);
        }

        public int getStringArray(String[] value, int length) {
            PVScalarArray pv = checkNumericScalarArray();
            if(pv==null) return 0;
            return convert.toStringArray(pv, 0, length, value, 0);
        }

        public PVStructure getPVStructure() {
            if(!checkPVStructure()) return null;
            return pvStructure;
        }
    } 
}
