package org.epics.pvaccess.easyPVA.example;

import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;
import org.epics.pvdata.pv.PVStructure;

class ExampleGet
{

	static EasyPVA easyPVA= EasyPVAFactory.get();

	public static void main( String[] args )
	{
	    if(args.length<1) {
	        System.out.println("channelName not given");
	    }
	    String channelName = args[0];
		PVStructure pvStructure = easyPVA.createChannel(channelName).createGet().getPVStructure();
		System.out.println(channelName +" = " + pvStructure);
		System.exit(0);
	}

}
