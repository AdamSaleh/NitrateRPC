/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration.CommandWrapper;

import NitrateIntegration.TcmsEnvironment;
import NitrateIntegration.TcmsProperties;
import com.redhat.nitrate.TcmsCommand;
import com.redhat.nitrate.TcmsConnection;
import com.redhat.nitrate.command.Build;
import com.redhat.nitrate.command.TestCase;
import com.redhat.nitrate.command.TestCaseRun;
import com.redhat.nitrate.command.TestRun;
import java.util.Hashtable;

/**
 *
 * @author asaleh
 */
public class TestCaseRunCreate extends CommandWrapper {

     static{
        CommandWrapper.enlistWrapper(TestCaseRun.create.class, new WrapperConstructor() {

            public CommandWrapper create(TcmsCommand current, Class result_type,TcmsProperties properties,TcmsEnvironment env) {
                return new TestCaseRunCreate(current, result_type, properties, env);
            }
        });
    }

    public TestCaseRunCreate(TcmsCommand current, Class result_type,TcmsProperties properties,TcmsEnvironment env) {
        super(current, result_type, properties, env);
    }

    @Override
    public Object getResultIfDuplicate(TcmsConnection connection) {
        TestCaseRun.filter f = new TestCaseRun.filter();
        TestCaseRun.create command = (TestCaseRun.create) current;
        f.build = command.build;
        f.run = command.run;
        f.caseVar = command.caseVar;
        f.case_run_status = command.case_run_status;

        CommandWrapper script = new Generic(f, TestCaseRun.class, properties, env);
        script.perform(connection);
        return script.getResult(TestCaseRun.class);
    }

    @Override
    public boolean processDependecies() {
        int build = -1;
        int run = -1;
        int caseVar = -1;
        for (CommandWrapper deps : getDependecies()) {
            if (deps.current() instanceof Build.create) {
                Build r = deps.getResult(Build.class);
                build = r.build_id;
            } else if (deps.current() instanceof TestRun.create) {
                TestRun r = deps.getResult(TestRun.class);
                run = r.run_id;
            } else if (deps.current() instanceof TestCase.create) {
                TestCase r = deps.getResult(TestCase.class);
                caseVar = r.case_id;
            }
        }

        if (build != -1 && run != -1 && caseVar != -1) {

            ((TestCaseRun.create) current()).build = build;
            ((TestCaseRun.create) current()).caseVar = caseVar;
            ((TestCaseRun.create) current()).run = run;
            return true;
        }
        return false;
    }

    public Hashtable<String, String> description() {
        Hashtable<String, String> map = current.descriptionMap();

        String status = map.get("case_run_status");
        if (status.contentEquals(String.valueOf(TestCaseRun.FAILED))) {
            status = "FAILED";
        } else if (status.contentEquals(String.valueOf(TestCaseRun.PASSED))) {
            status = "PASSED";
        } else if (status.contentEquals(String.valueOf(TestCaseRun.WAIVED))) {
            status = "WAIVED";
        }
        map.put("case_run_status", status);

        for (CommandWrapper deps : getDependecies()) {
            if (deps.current() instanceof Build.create) {
                map.put("build",deps.description().get("name") + " (" + map.get("build") + ")");

            } else if (deps.current() instanceof TestRun.create) {
                map.put("run", deps.description().get("summary") + " (" + map.get("run") + ")");

            } else if (deps.current() instanceof TestCase.create) {

                map.put("case", deps.description().get("summary") + " ("
                        + map.get("case") + ")");

            }
        }

        return map;
    }

    public String summary() {
        return description().get("case") + " "
                + description().get("case_run_status");
    }

    public String toString() {
        return "Create Test Case Run";
    }
}
