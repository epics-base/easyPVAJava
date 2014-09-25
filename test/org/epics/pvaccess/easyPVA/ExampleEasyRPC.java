/**
 * 
 */
package org.epics.pvaccess.easyPVA;
import org.epics.pvaccess.easyPVA.EasyChannel;

import org.epics.pvaccess.easyPVA.EasyPVA;
import org.epics.pvaccess.easyPVA.EasyPVAFactory;
import org.epics.pvaccess.easyPVA.EasyRPC;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;
import junit.framework.TestCase;

/**
 * ExampleEasyRPC provides examples of using the EasyPVA RPC client support to get data
 * from an EPICS V4 RPC server. 
 * <p>
 * The server this client was written for is RPCServiceExample.</p>
 * <p>
 * Example execution of this client side (assuming the server RPCServiceExample has been started):</p>
 * <code>
 * easyPVA/bin] greg% java -classpath .:../../../pvDataJava/bin:../../../pvAccessJava/bin \
 *                    org.epics.pvaccess.easyPVA.ExampleEasyRPC
 * </code>                    
 * @see RPCServiceExample
 * 
 * @author mrk
 * @version 11-Sep-2012 Greg White (greg@slac.stanford.edu) Added header.
 */
public class ExampleEasyRPC  extends TestCase{
    static EasyPVA easyPVA = EasyPVAFactory.get();
   
	private final static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
	
	private final static Structure requestStructure =
		fieldCreate.createStructure(
				new String[] { "a", "b" },
				new Field[] { fieldCreate.createScalar(ScalarType.pvString),
							  fieldCreate.createScalar(ScalarType.pvString) }
				);

	public static void testEasyRPC() {
		exampleRPC("sum");
		exampleRPCCheck("sum");
        easyPVA.destroy();
        System.out.println("all done");
    }

    static void exampleRPC(String channelName) {
		PVStructure request = PVDataFactory.getPVDataCreate().createPVStructure(requestStructure);
		request.getStringField("a").put("12.3");
		request.getStringField("b").put("45.6");

		PVStructure result = easyPVA.createChannel(channelName).createRPC().request(request);
        System.out.println(request +"\n =\n" + result);
    }
    
    static void exampleRPCCheck(String channelName) {
        EasyChannel channel =  easyPVA.createChannel(channelName);
        boolean result = channel.connect(2.0);
        if(!result) {
            System.out.printf(
                "exampleRPCCheck %s channel connect failed %s%n",
                channelName,
                channel.getStatus());
            return;
        }
        EasyRPC rpc = channel.createRPC();
        result = rpc.connect();
        if(!result) {
            System.out.printf(
                "exampleRPCCheck %s rpc connect failed %s%n",
                channelName,
                rpc.getStatus());
            return;
        }

		PVStructure request = PVDataFactory.getPVDataCreate().createPVStructure(requestStructure);
		request.getStringField("a").put("12.3");
		request.getStringField("b").put("45.6");

		rpc.issueRequest(request);
        PVStructure rpcResult = rpc.waitRequest();
        if(rpcResult==null) {
            System.out.printf(
                "exampleRPCCheck %s rpc failed %s%n",
                channelName,
                rpc.getStatus());
            return;
        }
        System.out.printf("%s %s%n",channelName,rpcResult.toString());
        channel.destroy();
    }

}
