/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import NitrateIntegration.TcmsGatherer.TcmsRpcCommandScript;
import com.redhat.engineering.jenkins.testparser.Parser;
import com.redhat.engineering.jenkins.testparser.results.*;
import com.redhat.nitrate.*;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import redstone.xmlrpc.*;

/**
 *
 * @author asaleh
 */
public class TcmsGatherer implements Iterable<TcmsRpcCommandScript>{

    PrintStream logger;
    TcmsRpcCommandScript head = null;
    TcmsRpcCommandScript tail = null;
    
    private int run_id;
    private int build_id;
  
    private TcmsProperties properties;
    
    AbstractBuild build;
    TcmsConnection connection;

    public TcmsGatherer(PrintStream logger,AbstractBuild build,TcmsConnection connection,TcmsProperties properties) {
        this.logger = logger;
        this.build=build;
        this.connection = connection;
        this.properties = properties;
    }


    private TestCase.create tcmsCreateCase(MethodResult result) {
       TestCase.create create = new TestCase.create();
       create.product = this.properties.getProductID();
       create.category = this.properties.getCategoryID();
       create.priority = this.properties.getPriorityID();
       create.summary = result.getName();
       return create;
    }

    private void CreateTestCaseRun(MethodResult result, int status) {
        TestCaseRun.create c = new TestCaseRun.create();
        c.run = this.run_id;
        try {
            c.caseVar = TcmsUploader.getTcmsTestCaseId(result.getName(), connection);
        } catch (XmlRpcFault ex) {
            c.caseVar = -1;
            //Logger.getLogger(TcmsGatherer.class.getName()).log(Level.SEVERE, null, ex);
        }
        TcmsRpcCommandScript dependency = null;
        if (c.caseVar < 0) {
             dependency = add(tcmsCreateCase(result),null);
        }
        c.build = this.build_id;
        c.case_run_status = status;
        add(c,dependency);
    }

    private void gatherTestInfo(TestResults results) {

        for (MethodResult result : results.getFailedTests()) {
            CreateTestCaseRun(result, TestCaseRun.FAILED);
        }
        for (MethodResult result : results.getPassedTests()) {
            CreateTestCaseRun(result, TestCaseRun.PASSED);
        }
        for (MethodResult result : results.getSkippedTests()) {
            CreateTestCaseRun(result, TestCaseRun.WAIVED);
        }

    }

    public void gather(String testPath) throws IOException, InterruptedException {
        clear();
        Parser testParser = new Parser(logger);
        
        FilePath[] paths = Parser.locateReports(build.getWorkspace(), testPath);
        if (paths.length == 0) {
            logger.println("Did not find any matching files.");
            return;
        }
        
        paths = Parser.checkReports(build, paths, logger);
        
        TestResults results = testParser.parse(paths, false);

        if (results == null) {
            return;
        }
        gatherTestInfo(results);

    }

    public void clear() {
        head = null;
    }

    private TcmsRpcCommandScript add(TcmsCommand current, TcmsRpcCommandScript dependecy) {
        TcmsRpcCommandScript script = new TcmsRpcCommandScript(current, tail, dependecy);
        if (head == null) {
            head = script;
        }
        tail = script;
        return tail;
    }

    public Iterator<TcmsRpcCommandScript> iterator() {
        return new CommadScriptIterator();
    }

    public class CommadScriptIterator implements Iterator<TcmsRpcCommandScript>{
        TcmsRpcCommandScript current;
        public CommadScriptIterator() {
            current = head;
        }

        public boolean hasNext() {
            //if(current==null) return false;
            return current!=null;
        }

        public TcmsRpcCommandScript next() {
            TcmsRpcCommandScript temp=current;
            if(current!=null) current = current.next;
            return temp;
        }

        public void remove() {
            return;
        }

    }

    /*Yay, linked list! Way to shoot yourself to the leg :D */
    public class TcmsRpcCommandScript {

        public TcmsCommand current;
        TcmsRpcCommandScript previous;
        TcmsRpcCommandScript next;
        boolean performed;
        boolean completed;
        public TcmsRpcCommandScript dependecy;
        Object result;
        public TcmsRpcCommandScript(TcmsCommand current, TcmsRpcCommandScript previous, TcmsRpcCommandScript dependecy) {
            this.current = current;

            this.previous = previous;
            this.next = null;
            if(previous!=null) this.previous.next = this;

            this.performed = false;
            this.completed = false;

            this.dependecy = dependecy;
        }

        public TcmsRpcCommandScript(TcmsCommand current, TcmsRpcCommandScript previous) {
            this(current, previous, null);
        }
    }
}
