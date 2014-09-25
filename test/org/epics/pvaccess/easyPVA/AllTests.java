package org.epics.pvaccess.easyPVA;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite()
    {
        TestSuite suite = new TestSuite(
                "Test for " + AllTests.class.getPackage().getName());
        //$JUnit-BEGIN$
        suite.addTestSuite(ExampleEasyPutScalar.class);
        suite.addTestSuite(ExampleEasyPutArray.class);
        suite.addTestSuite(ExampleEasyMultiPut.class);
        suite.addTestSuite(ExampleEasyGetScalar.class);
        suite.addTestSuite(ExampleEasyGetArray.class);
        suite.addTestSuite(ExampleEasyMultiGet.class);
        suite.addTestSuite(ExampleEasyPutArrayForever.class);
        suite.addTestSuite(ExampleEasyMultiPutForever.class);
        suite.addTestSuite(ExampleEasyRPC.class);
        //$JUnit-END$
        return suite;
    }
}

