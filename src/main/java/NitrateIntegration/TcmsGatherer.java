/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import NitrateIntegration.RpcCommandScript;
import com.redhat.engineering.jenkins.testparser.Parser;
import com.redhat.engineering.jenkins.testparser.results.MethodResult;
import com.redhat.engineering.jenkins.testparser.results.TestResults;
import com.redhat.nitrate.*;
import hudson.FilePath;
import hudson.matrix.Combination;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import redstone.xmlrpc.XmlRpcStruct;

/**
 *
 * @author asaleh
 */
public class TcmsGatherer implements Iterable<RpcCommandScript> {

    PrintStream logger;
    LinkedList<RpcCommandScript> list = new LinkedList<RpcCommandScript>();
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

    private TestRun.create tcmsCreateRun(AbstractBuild run) {
        TestRun.create create = new TestRun.create();
        create.product = this.properties.getProductID();
        create.product_version = this.properties.getProduct_vID();
        create.plan = this.properties.getPlanID();
        create.build = -1;
        create.manager = this.properties.getManagerId();
        create.summary = run.getDisplayName();
        if(run instanceof MatrixRun){
            MatrixRun mrun = (MatrixRun) run;
            Combination c= mrun.getProject().getCombination();
            create.summary += c.toString(); 
        }
        return create;
    }

    private void CreateTestCaseRun(MethodResult result, int status, RpcCommandScript run, RpcCommandScript build) {
        TestCaseRun.create c = new TestCaseRun.create();
        c.run = -1;
        c.caseVar = -1;
        RpcCommandScript dependency = null;
        dependency = add(tcmsCreateCase(result), null,TestCase.class);
        c.build = -1;
        c.case_run_status = status;
        RpcCommandScript case_run = add(c, dependency,TestCaseRun.class);
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
        
        if(build_s==null) build_s = add(tcmsCreateBuild(build),null,Build.class);
        RpcCommandScript run_s =  add(tcmsCreateRun(run),build_s,TestRun.class);
        gatherTestInfo(results, run_s,build_s);

    }

    public void clear() {
        list.clear();
    }

    private RpcCommandScript add(TcmsCommand current, RpcCommandScript dependecy,Class result_class) {
        RpcCommandScript script = new RpcCommandScript(current, dependecy,result_class);
        list.add(script);
        return script;
    }

    public Iterator<RpcCommandScript> iterator() {
        return list.listIterator();
    }
}
