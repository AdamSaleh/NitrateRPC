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
import com.redhat.nitrate.command.TestRun;
import java.util.Hashtable;

/**
 *
 * @author asaleh
 */
public class TestRunCreate extends CommandWrapper {

    {
        CommandWrapper.enlistWrapper(TestRun.create.class, new WrapperConstructor() {

            public CommandWrapper create(TcmsCommand current, Class result_type, TcmsProperties properties, TcmsEnvironment env) {
                return new TestRunCreate(current, result_type, properties, env);
            }
        });
    }

    public TestRunCreate(TcmsCommand current, Class result_type, TcmsProperties properties, TcmsEnvironment env) {
        super(current, result_type, properties, env);
    }

    @Override
    public Object getResultIfDuplicate(TcmsConnection connection) {
        TestRun.filter f = new TestRun.filter();
        TestRun.create command = (TestRun.create) current;
        f.build = command.build;
        f.plan = command.plan;
        f.summary = command.summary;
        f.manager = command.manager;

        CommandWrapper script = new Generic(f, TestRun.class, properties, env);
        script.perform(connection);
        return script.getResult(TestRun.class);
    }

    @Override
    public boolean processDependecies() {
        int build = -1;
        for (CommandWrapper deps : getDependecies()) {
            if (deps.current() instanceof Build.create) {
                Build b = deps.getResult(Build.class);
                build = b.build_id;
            }
        }

        if (build != -1) {
            ((TestRun.create) current()).build = build;

            return true;
        }
        return false;
    }

    public Hashtable<String, String> description() {
        Hashtable<String, String> map = current.descriptionMap();
        map.put("product_version",
                properties.product_v + " (" + map.get("product_version") + ")");
        map.put("manager", properties.manager + " (" + map.get("manager") + ")");
        map.put("product", properties.product + " (" + map.get("product") + ")");
        map.put("plan", properties.plan + " (" + map.get("plan") + ")");

        for (CommandWrapper deps : getDependecies()) {
            if (deps.current() instanceof Build.create) {
                map.put("build",
                        deps.description().get("name") + " (" + map.get("build") + ")");
            }
        }

        return map;
    }

    public String summary() {
        return description().get("summary");
    }

    public String toString() {
        return "Create Test Run";
    }
}
