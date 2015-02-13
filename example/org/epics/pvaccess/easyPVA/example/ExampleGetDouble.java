package org.epics.pvaccess.easyPVA.example;

import org.epics.pvaccess.easyPVA.EasyChannel;
import org.epics.pvaccess.easyPVA.EasyGet;
import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;

class ExampleGetDouble
{

	static EasyPVA easyPVA;

	public static void main( String[] args )
	{
		easyPVA = EasyPVAFactory.get();
		exampleDouble(args[0]);
		exampleDoubleCheck(args[0]);
		System.exit(0);
	}


	static void exampleDouble( String channelName )
	{
	    // get the scalar value
	    double value = 
	            easyPVA.createChannel(channelName).createGet().getDouble();
	    System.out.println(channelName +" = " + value);	
	}


	static void exampleDoubleCheck(String channelName) 
	{
		System.out.format("Attempting checked get of %s\n",channelName);
		
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

}
