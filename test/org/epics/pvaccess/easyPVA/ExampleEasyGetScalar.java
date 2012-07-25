/**
 * 
 */
package org.epics.pvaccess.easyPVA;
import java.util.Date;

import org.epics.pvaccess.easyPVA.EasyChannel;
import org.epics.pvaccess.easyPVA.EasyGet;
import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;
import org.epics.pvdata.property.Alarm;
import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.property.TimeStamp;

/**
 * @author mrk
 *
 */
public class ExampleEasyGetScalar {
    static EasyPVA easyPVA = EasyPVAFactory.get();
   
    public static void main(String[] args) {
        exampleDouble("double01");
        exampleDoubleCheck("int01");
        exampleDoubleCheck("xxxxJUNK");
        exampleDoubleAlarmTimeStamp("double01");
        exampleDoubleAlarmTimeStamp("byte01");
        exampleTwoChannels("double01","byte01");
        easyPVA.destroy();
        System.out.println("all done");
    }

    static void exampleDouble(String channelName) {
        // get the scalar value
        double value = easyPVA.createChannel(channelName).createGet().getDouble();
        System.out.println(channelName +" = " + value);
    }
    
    static void exampleDoubleCheck(String channelName) {
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
        get.issueGet();
        result = get.waitGet();
        if(!result) {
            System.out.printf(
                "exampleDoubleCheck %s get failed %s%n",
                channelName,
                get.getStatus());
            easyPVA.setAuto(true, true);
            return;
        }
        System.out.printf("%s %s%n",channelName,get.getString());
        channel.destroy();
        easyPVA.setAuto(true, true);
    }
    
    static void exampleDoubleAlarmTimeStamp(String channelName) {
        EasyGet easyGet = easyPVA.createChannel(channelName).createGet();
        double value = easyGet.getDouble();
        Alarm alarm = easyGet.getAlarm();
        TimeStamp timeStamp = easyGet.getTimeStamp();
        System.out.printf(
            "%s %s %s %s%n",
            channelName,
            Double.toString(value),
            alarmToString(alarm),
            timeStampToString(timeStamp));
    }
    
    static void exampleTwoChannels(String channelName0,String channelName1) {
        easyPVA.setAuto(false, true);
        EasyChannel channel0 =  easyPVA.createChannel(channelName0);
        EasyChannel channel1 = easyPVA.createChannel(channelName1);
        // the connects will be done in parallel
        channel0.issueConnect();
        channel1.issueConnect();
        channel0.waitConnect(2.0);
        channel1.waitConnect(2.0);
        EasyGet get0 = channel0.createGet();
        EasyGet get1 = channel1.createGet();
        // the get connects will be done in parallel
        get0.issueConnect();
        get1.issueConnect();
        get0.waitConnect();
        get1.waitConnect();
        // the gets will be done in parallel
        get0.issueGet();
        get1.issueGet();
        get0.waitGet();
        get1.waitGet();
        System.out.printf("%s %s %s %s%n",
            channelName0, get0.getString(),
            channelName1,get1.getString());
        channel0.destroy();
        channel1.destroy();
        easyPVA.setAuto(true, true);
    } 
    
    static String alarmToString(Alarm alarm) {
       String result = new String("");
       AlarmSeverity severity = alarm.getSeverity();
       if(severity!=AlarmSeverity.NONE) result += " severity " + severity.name();
       AlarmStatus status = alarm.getStatus();
       if(status!=AlarmStatus.NONE) result += " status " + status.name();
       String message = alarm.getMessage();
       if(message!=null && message.length()>0) result += " " + message;
       return result;
    }
    
    static String timeStampToString(TimeStamp timeStamp) {
        long milliPastEpoch = timeStamp.getMilliSeconds();
        int userTag = timeStamp.getUserTag();
        Date date = new Date(milliPastEpoch);
        String result = String.format("timeStamp %tF %tT.%tL", date,date,date);
        if(userTag!=0) result += " userTag " + Integer.toString(userTag);
        return result;
    }
    
}
