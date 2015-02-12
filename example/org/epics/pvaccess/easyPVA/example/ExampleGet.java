package org.epics.pvaccess.easyPVA.example;

import org.epics.ca.ClientFactory.*;
import org.epics.pvaccess.easyPVA.*;
import org.epics.pvdata.pv.*;

class ExampleGet
{

	static EasyPVA easyPVA;

	public static void main( String[] args )
	{
	    if(args.length<1) {
	        System.out.println("channelName not given");
	    }
	    String channelName = args[0];
		easyPVA = EasyPVAFactory.get();
		PVStructure pvStructure = easyPVA.createChannel(channelName).createGet().getPVStructure();
		System.out.println(channelName +" = " + pvStructure);
		System.exit(0);
	}

}
