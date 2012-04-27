/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import NitrateIntegration.RpcCommandScript;
import com.redhat.nitrate.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import redstone.xmlrpc.*;

/**
 *
 * @author asaleh
 */
public class TcmsUploader {

    public static TestCase getTestCase(TestCase.create command, TcmsConnection connection) {
        try {
            TestCase.filter f = new TestCase.filter();
            f.summary__icontain = command.summary;
            
            RpcCommandScript script = new RpcCommandScript(f,TestCase.class);
            script.perform(connection);
            return  script.getResult(TestCase.class);
         } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsUploader.class.getName()).log(Level.SEVERE, null, ex);
         } 
        return null;
    }
    public static Build getBuild(Build.create command, TcmsConnection connection) {
        try {
            Build.check_build f = new Build.check_build();
            f.name = command.name;
            f.productid = command.product;
            
            RpcCommandScript script = new RpcCommandScript(f,Build.class);
            script.perform(connection);
            return  script.getResult(Build.class);
         } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsUploader.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return null;
    }
    public static TestRun getRun(TestRun.create command, TcmsConnection connection) {
        try {
            TestRun.filter f = new TestRun.filter();
            f.build = command.build;
            f.plan = command.plan;
            f.product = command.product;
            f.manager = command.manager;

            RpcCommandScript script = new RpcCommandScript(f,TestRun.class);
            script.perform(connection);
            return  script.getResult(TestRun.class);
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsUploader.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return null;
    }
 
    private static void invoke(RpcCommandScript command, TcmsConnection connection) throws XmlRpcFault{
        command.setPerforming();
        Object o = null;
        if(command.current() instanceof Build.create){
            Build.create cmd =(Build.create) command.current();
            o=getBuild(cmd, connection);
            if(o==null){
                command.perform(connection);
            }
        }else if(command.current() instanceof TestRun.create){
            TestRun.create cmd =(TestRun.create) command.current();
            o=getRun(cmd, connection);
            if(o==null){
                command.perform(connection);
            }
        }else if(command.current() instanceof TestCase.create){
            TestCase.create cmd =(TestCase.create) command.current();
            o=getTestCase(cmd, connection);
            if(o==null){
                command.perform(connection);
            }
            
        }else if(command.current() instanceof TestCaseRun.create){
                command.perform(connection);
        }else{
            o = connection.invoke(command.current());
        }
        command.setCompleted();
    }

    public static void upload(TcmsGatherer gathered, TcmsConnection connection) throws XmlRpcFault {
        boolean at_least_one = true;

        while (at_least_one) {
            at_least_one = false;
            for (RpcCommandScript command : gathered) {
                if (command.resolved() && command.performed() ==false) {
                    if(command.getDependecies().isEmpty()){
                        at_least_one = true;
                        invoke(command,connection);
                    }else if(command.current() instanceof TestRun.create){
                        int build = -1;
                        for(RpcCommandScript deps:command.getDependecies()){
                            if(deps.current() instanceof Build.create){
                                Build b = deps.getResult(Build.class);
                                build = b.build_id;
                            }
                        }
                        
                        if(build!=-1){
                            ((TestRun.create)command.current()).build = build;
                            at_least_one = true;
                            invoke(command, connection);
                        }
                    }else if(command.current() instanceof TestCaseRun.create){
                        int build = -1;
                        int run = -1;
                        int caseVar = -1;
                        for(RpcCommandScript deps:command.getDependecies()){
                            if(deps.current() instanceof Build.create){
                                Build r = deps.getResult(Build.class);
                                build = r.build_id;
                            }else if(deps.current() instanceof TestRun.create){
                                TestRun r = deps.getResult(TestRun.class);
                                run = r.build_id;
                            }else if(deps.current() instanceof TestCase.create){
                                TestCase r = deps.getResult(TestCase.class);
                                caseVar = r.case_id;
                            }
                        }
                        
                        if(build!=-1 && run != -1 && caseVar!=-1){
                            ((TestCaseRun.create)command.current()).build = build;
                            ((TestCaseRun.create)command.current()).caseVar = caseVar;
                            ((TestCaseRun.create)command.current()).run = run;
                            at_least_one = true;
                            invoke(command, connection);
                        }
                    }
                    
                }
            }
        }
    }
}
