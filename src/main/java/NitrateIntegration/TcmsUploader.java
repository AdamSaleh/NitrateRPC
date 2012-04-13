/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import NitrateIntegration.TcmsGatherer.TcmsRpcCommandScript;
import com.redhat.nitrate.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import redstone.xmlrpc.*;

/**
 *
 * @author asaleh
 */
public class TcmsUploader {

    public static Integer getTcmsTestCaseId(String name, TcmsConnection connection) throws XmlRpcFault {
        try {
            TestCase.filter f = new TestCase.filter();
            f.summary__icontain = name;
            XmlRpcStruct struct = (XmlRpcStruct) connection.invoke(f);
            TestCase testcase = (TestCase) TcmsConnection.rpcStructToFields(struct, TestCase.class);
            return testcase.case_id;
        } catch (IllegalAccessException ex) {
            Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public static void upload(TcmsGatherer gathered, TcmsConnection connection) throws XmlRpcFault {
        boolean at_least_one = true;

        while (at_least_one) {
            at_least_one = false;
            for (TcmsRpcCommandScript command : gathered) {
                if (command.dependecy == null) {
                    command.performed = true;
                    connection.invoke(command.current);
                    command.completed = true;

                    at_least_one = true;
                } else {
                    /*need to resolve dependecy manualy*/
                    if (command.dependecy.completed == true) {
                        if (command.current instanceof TestCaseRun.create) {
                            if (command.dependecy.current instanceof TestCase.create) {
                                 TestCase.create testcase = ( TestCase.create) command.dependecy.current;
                                int i = getTcmsTestCaseId(testcase.summary, connection);
                                if(i>-1){
                                    TestCaseRun.create testcaserun = (TestCaseRun.create)command.current;
                                    testcaserun.caseVar = i;
                                    command.performed = true;
                                    connection.invoke(command.current);
                                    command.completed = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
