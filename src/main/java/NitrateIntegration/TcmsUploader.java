/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import NitrateIntegration.TcmsGatherer.RpcCommandScript;
import com.redhat.nitrate.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import redstone.xmlrpc.*;

/**
 *
 * @author asaleh
 */
public class TcmsUploader {

    public static TestCase getTestCase(TestCase.create command, TcmsConnection connection) throws IllegalAccessException, InstantiationException  {
        try {
            TestCase.filter f = new TestCase.filter();
            f.summary__icontain = command.summary;
            XmlRpcStruct struct = (XmlRpcStruct) connection.invoke(f);
            TestCase testcase =  TcmsConnection.rpcStructToFields(struct, TestCase.class);
            return testcase;
         } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsUploader.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return null;
    }
    public static Build getBuild(Build.create command, TcmsConnection connection) throws IllegalAccessException, InstantiationException  {
        try {
            Build.check_build f = new Build.check_build();
            f.name = command.name;
            XmlRpcStruct struct = (XmlRpcStruct) connection.invoke(f);
            Build build = TcmsConnection.rpcStructToFields(struct, Build.class);
            return build;
         } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsUploader.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return null;
    }
        public static TestRun getRun(TestRun.create command, TcmsConnection connection) throws IllegalAccessException, InstantiationException {
        try {
            TestRun.filter f = new TestRun.filter();
            f.build = command.build;
            f.plan = command.plan;
            f.product = command.product;
            f.manager = command.manager;

            XmlRpcStruct struct = (XmlRpcStruct) connection.invoke(f);
            TestRun run = TcmsConnection.rpcStructToFields(struct, TestRun.class);
            return run;
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsUploader.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return null;
    }
    private static void invoke(RpcCommandScript command, TcmsConnection connection) throws XmlRpcFault, IllegalAccessException, InstantiationException{
        command.setPerforming();
        Object o = null;
        if(command.current() instanceof Build.create){
            Build.create cmd =(Build.create) command.current();
            o=getBuild(cmd, connection);
            if(o==null) o = connection.invoke(command.current());
            
        }else if(command.current() instanceof TestRun.create){
            TestRun.create cmd =(TestRun.create) command.current();
            o=getRun(cmd, connection);
            if(o==null) o = connection.invoke(command.current());
            
        }else if(command.current() instanceof TestCase.create){
            TestCase.create cmd =(TestCase.create) command.current();
            o=getTestCase(cmd, connection);
            if(o==null) o = connection.invoke(command.current());
            
        }else{
            o = connection.invoke(command.current());
        }
        command.setResult(o);
        command.setCompleted();
    }

    public static void upload(TcmsGatherer gathered, TcmsConnection connection) throws XmlRpcFault, InstantiationException, IllegalAccessException {
        boolean at_least_one = true;

        while (at_least_one) {
            at_least_one = false;
            for (RpcCommandScript command : gathered) {
                if (command.resolved()) {
                    if(command.getDependecies().isEmpty()){
                        invoke(command,connection);
                    }else if(command.current() instanceof TestRun.create){
                        int build = -1;
                        for(RpcCommandScript deps:command.getDependecies()){
                            if(deps.current() instanceof Build.create){
                                Build b = TcmsConnection.rpcStructToFields((XmlRpcStruct)deps.getResult(), Build.class);
                                build = b.build_id;
                            }
                        }
                        
                        if(build!=-1){
                            ((TestRun.create)command.current()).build = build;
                            invoke(command, connection);
                        }
                    }else if(command.current() instanceof TestCaseRun.create){
                        int build = -1;
                        int run = -1;
                        int caseVar = -1;
                        for(RpcCommandScript deps:command.getDependecies()){
                            if(deps.current() instanceof Build.create){
                                Build r = TcmsConnection.rpcStructToFields((XmlRpcStruct)deps.getResult(), Build.class);
                                build = r.build_id;
                            }else if(deps.current() instanceof TestRun.create){
                                TestRun r = TcmsConnection.rpcStructToFields((XmlRpcStruct)deps.getResult(), TestRun.class);
                                run = r.build_id;
                            }else if(deps.current() instanceof TestCase.create){
                                TestCase r = TcmsConnection.rpcStructToFields((XmlRpcStruct)deps.getResult(), TestCase.class);
                                caseVar = r.case_id;
                            }
                        }
                        
                        if(build!=-1 && run != -1 && caseVar!=-1){
                            ((TestCaseRun.create)command.current()).build = build;
                            ((TestCaseRun.create)command.current()).caseVar = caseVar;
                            ((TestCaseRun.create)command.current()).run = run;

                            invoke(command, connection);
                        }
                    }
                    
                }
            }
        }
    }
}
