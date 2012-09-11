package org.epics.pvaccess.easyPVA;

import org.epics.pvaccess.CAException;
import org.epics.pvaccess.server.rpc.RPCRequestException;
import org.epics.pvaccess.server.rpc.RPCServer;
import org.epics.pvaccess.server.rpc.RPCService;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;

/**
 * Provides an example RPC service for illustrating the EasyPVA RPC client. 
 * <p>
 * At the time of writing, the RPCServiceExample of the easyPVA package, is a duplicate of
 * the same named class in org.epics.pvaccess.server.rpc.test.RPCServiceExample. It is reproduced
 * here to make EasyPVA self contained. Example execution of sever side: </p>
 * <code>
 * [alphaJava/easyPVA/bin] greg% java -classpath .:../../../pvDataJava/bin:../../../pvAccessJava/bin \
 * org.epics.pvaccess.easyPVA.RPCServiceExample
 * </code>
 * @author 11-Sep-2012, Greg White (greg@slac.stanford.edu)
 * @see ExampleEasyRPC
 */
public class RPCServiceExample {

	private final static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
	
	private final static Structure resultStructure =
		fieldCreate.createStructure(
				new String[] { "c" },
				new Field[] { fieldCreate.createScalar(ScalarType.pvDouble) }
				);

	static class SumServiceImpl implements RPCService
	{
		@Override
		public PVStructure request(PVStructure args) throws RPCRequestException {
			// TODO error handling
			
			double a = Double.valueOf(args.getStringField("a").get());
			double b = Double.valueOf(args.getStringField("b").get());
			
			PVStructure result = PVDataFactory.getPVDataCreate().createPVStructure(resultStructure);
			result.getDoubleField("c").put(a+b);
			
			return result;
		}
	}
	
	public static void main(String[] args) throws CAException
	{

		RPCServer server = new RPCServer();
		
		server.registerService("sum", new SumServiceImpl());
		// you can register as many services as you want here ...
		
		server.printInfo();
		server.run(0);
	}

}
