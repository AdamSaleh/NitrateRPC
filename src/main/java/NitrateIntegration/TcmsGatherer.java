/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import NitrateIntegration.TcmsGatherer.RpcCommandScript;
import com.redhat.engineering.jenkins.testparser.Parser;
import com.redhat.engineering.jenkins.testparser.results.MethodResult;
import com.redhat.engineering.jenkins.testparser.results.TestResults;
import com.redhat.nitrate.*;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author asaleh
 */
public class TcmsGatherer implements Iterable<RpcCommandScript> {

    PrintStream logger;
    RpcCommandScript head = null;
    RpcCommandScript tail = null;
    private int run_id;
    private int build_id;
    private TcmsProperties properties;
    RpcCommandScript build_s;
    
    
    public TcmsGatherer(PrintStream logger, TcmsProperties properties) {
        this.logger = logger;
        this.properties = properties;
        this.build_s=null;
    }

    private TestCase.create tcmsCreateCase(MethodResult result) {
        TestCase.create create = new TestCase.create();
        create.product = this.properties.getProductID();
        create.category = this.properties.getCategoryID();
        create.priority = this.properties.getPriorityID();
        create.summary = result.getName();
        return create;
    }

    private Build.create tcmsCreateBuild(AbstractBuild build) {
        Build.create create = new Build.create();
        create.product = this.properties.getProductID();
        create.name = build.getId();
        create.description = build.getDescription();
        return create;
    }

    private TestRun.create tcmsCreateRun(AbstractBuild build) {
        TestRun.create create = new TestRun.create();
        create.product = this.properties.getProductID();
        create.plan = this.properties.getPlanID();
        create.build = -1;
        create.manager = this.properties.getManagerId();
        return create;
    }

    private void CreateTestCaseRun(MethodResult result, int status, RpcCommandScript run, RpcCommandScript build) {
        TestCaseRun.create c = new TestCaseRun.create();
        c.run = -1;
        c.caseVar = -1;
        RpcCommandScript dependency = null;
        dependency = add(tcmsCreateCase(result), null);
        c.build = -1;
        c.case_run_status = status;
        RpcCommandScript case_run = add(c, dependency);
        case_run.addDependecy(run);
        case_run.addDependecy(build);
    }

    private void gatherTestInfo(TestResults results, RpcCommandScript run, RpcCommandScript build) {

        for (MethodResult result : results.getFailedTests()) {
            CreateTestCaseRun(result, TestCaseRun.FAILED, run,build);
        }
        for (MethodResult result : results.getPassedTests()) {
            CreateTestCaseRun(result, TestCaseRun.PASSED, run,build);
        }
        for (MethodResult result : results.getSkippedTests()) {
            CreateTestCaseRun(result, TestCaseRun.WAIVED, run,build);
        }

    }

    public void gather(FilePath[] paths, AbstractBuild build, AbstractBuild run) throws IOException, InterruptedException {
        
        Parser testParser = new Parser(logger);

        TestResults results = testParser.parse(paths, false);

        if (results == null) {
            return;
        }
        
        if(build_s==null) build_s = add(tcmsCreateBuild(build),null);
        RpcCommandScript run_s =  add(tcmsCreateRun(run),build_s);
        gatherTestInfo(results, run_s,build_s);

    }

    public void clear() {
        head = null;
    }

    private RpcCommandScript add(TcmsCommand current, RpcCommandScript dependecy) {
        RpcCommandScript script = new RpcCommandScript(current, tail, dependecy);
        if (head == null) {
            head = script;
        }
        tail = script;
        return tail;
    }

    public Iterator<RpcCommandScript> iterator() {
        return new CommadScriptIterator();
    }

    public class CommadScriptIterator implements Iterator<RpcCommandScript> {

        RpcCommandScript current;

        public CommadScriptIterator() {
            current = head;
        }

        public boolean hasNext() {
            //if(current==null) return false;
            return current != null;
        }

        public RpcCommandScript next() {
            RpcCommandScript temp = current;
            if (current != null) {
                current = current.next;
            }
            return temp;
        }

        public void remove() {
            return;
        }
    }

    /*
     * Yay, linked list! Way to shoot yourself to the leg :D
     */
    public class RpcCommandScript {

        public TcmsCommand current;
        private RpcCommandScript previous;
        private RpcCommandScript next;
        private boolean performed;
        private boolean completed;
        private LinkedList<RpcCommandScript> dependecy;
        private Object result;

        public RpcCommandScript(TcmsCommand current, RpcCommandScript previous, RpcCommandScript dependecy) {
            this.current = current;

            this.previous = previous;
            this.next = null;
            if (previous != null) {
                this.previous.next = this;
            }

            this.performed = false;
            this.completed = false;
            this.dependecy = new LinkedList<RpcCommandScript>();
            if (dependecy != null) {
                this.dependecy.push(dependecy);
            }
            result = null;
        }

        public LinkedList<RpcCommandScript> getDependecies() {
            return dependecy;
        }
        public void addDependecy(RpcCommandScript dep) {
            dependecy.push(dep);
        }
        
        public boolean resolved() {
            if (dependecy.size() == 0) {
                return true;
            }
            for (RpcCommandScript s : dependecy) {
                if(s.completed()==false) return false;
            }
            return true;
        }

        public boolean completed() {
            return completed;
        }

        public RpcCommandScript(TcmsCommand current, RpcCommandScript previous) {
            this(current, previous, null);
        }

        TcmsCommand current() {
             return current;
        }

        public void setResult(Object result) {
            if(this.result == null)this.result = result;
        }
        public Object getResult() {
            return result;
        }

        public void setPerforming() {
            this.performed = true;
        }

        public boolean performed() {
            return performed;
        }

        public void setCompleted() {
            this.completed = true;
        }
        
        
        
    }
}
