/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import com.redhat.nitrate.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.html.HTML;
import org.apache.commons.jelly.Jelly;
import redstone.xmlrpc.XmlRpcArray;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.XmlRpcStruct;

/**
 *
 * @author asaleh
 */
/*
 * Yay, dependencies list! Way to shoot yourself to the leg :D
 */
public abstract class CommandWrapper {

    public TcmsCommand current;
    private boolean performed;
    private boolean completed;
    private boolean duplicate;
    private boolean executable;
    
    private LinkedList<CommandWrapper> dependecy;
    private Object result;
    private Object unexpected;
    private Class result_type;
    TcmsProperties properties;

    public CommandWrapper(TcmsCommand current, CommandWrapper dependecy, Class result_type,TcmsProperties properties) {
        this.current = current;

        this.performed = false;
        this.completed = false;
        this.duplicate = false;
        this.executable = true;
        this.dependecy = new LinkedList<CommandWrapper>();
        this.result_type = result_type;
        if (dependecy != null) {
            this.dependecy.push(dependecy);
        }
        this.properties = properties;
        result = null;
    }

    public LinkedList<CommandWrapper> getDependecies() {
        return dependecy;
    }

    public void addDependecy(CommandWrapper dep) {
        dependecy.push(dep);
    }

    public boolean resolved() {
        if (dependecy.size() == 0) {
            return true;
        }
        for (CommandWrapper s : dependecy) {
            if (s.completed() == false) {
                return false;
            }
        }
        return true;
    }

    public boolean completed() {
        return completed;
    }

    public boolean duplicate() {
        return duplicate;
    }
    

    public CommandWrapper(TcmsCommand current, Class result_type,TcmsProperties properties) {
        this(current, null, result_type,properties);
    }

    TcmsCommand current() {
        return current;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    public boolean isExecutable() {
        return executable;
    }

    public void setResult(Object o) {
        if (this.result == null) {
            Object r = null;
            if (result_type.isInstance(o)) {
                result = o;
                unexpected = null;
                return;
            } else if (o instanceof XmlRpcStruct) {
                XmlRpcStruct struct = (XmlRpcStruct) o;
                if (struct.containsKey("args")) { // usualy when query shows no results
                    result = null;
                    unexpected = o;
                    return;
                } else {
                    result = TcmsConnection.rpcStructToFields((XmlRpcStruct) o, result_type);
                    unexpected = null;
                    return;
                }

            } else if (o instanceof XmlRpcArray) {
                XmlRpcArray array = (XmlRpcArray) o;
                if (array.size() > 0) { // usualy when query shows no results
                    setResult(array.get(0));
                    return;
                } else {
                    result = null;
                    unexpected = o;
                    return;
                }

            } else {
                result = null;
                unexpected = o;
                return;
            }

        }
    }

    public boolean perform(TcmsConnection connection) throws XmlRpcFault {
        if (processDependecies()) {
            setPerforming();
            Object o = getResultIfDuplicate(connection);
            if (o == null) {
                o = connection.invoke(current());
            }else{
                duplicate=true;
            }
            setResult(o);
            setCompleted();
            return true;
        }
        return false;
    }

    public <T extends Object> T getResult(Class<T> c) {
        return c.cast(result);
    }

    public Object getUnexpected() {
        return unexpected;
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

    public abstract Object getResultIfDuplicate(TcmsConnection connection);

    public abstract boolean processDependecies();

    public Hashtable<String, String> description() {
        return current.descriptionMap();
    }

    public String name() {
        return current.name();
    }

    public static class Generic extends CommandWrapper {

        public Generic(TcmsCommand current, Class result_type,TcmsProperties properties) {
            super(current, result_type,properties);
        }

        public Generic(TcmsCommand current, CommandWrapper dependecy, Class result_type,TcmsProperties properties) {
            super(current, dependecy, result_type,properties);
        }

        @Override
        public Object getResultIfDuplicate(TcmsConnection connection) {
            return null;
        }

        @Override
        public boolean processDependecies() {
            return true;
        }
    }

    public static class BuildCreate extends CommandWrapper {

        public BuildCreate(TcmsCommand current, Class result_type, TcmsProperties properties) {
            super(current, result_type,properties);
        }

        public BuildCreate(TcmsCommand current, CommandWrapper dependecy, Class result_type,TcmsProperties properties) {
            super(current, dependecy, result_type, properties);
        }

        @Override
        public Object getResultIfDuplicate(TcmsConnection connection) {
            try {
                Build.check_build f = new Build.check_build();
                Build.create comand = (Build.create) current;
                f.name = comand.name;
                f.productid = comand.product;

                CommandWrapper script = new CommandWrapper.Generic(f, Build.class,null);
                script.perform(connection);
                return script.getResult(Build.class);
            } catch (XmlRpcFault ex) {
                Logger.getLogger(TcmsUploader.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        @Override
        public boolean processDependecies() {
            return true;
        }
        public Hashtable<String, String> description() {
            Hashtable<String, String> map = current.descriptionMap();
            map.put("product",properties.product + " (" +map.get("product")+")");
            return map;
        }

    }

    public static class TestCaseCreate extends CommandWrapper {

        public TestCaseCreate(TcmsCommand current, Class result_type,TcmsProperties properties) {
            super(current, result_type, properties);
        }

        public TestCaseCreate(TcmsCommand current, CommandWrapper dependecy, Class result_type,TcmsProperties properties) {
            super(current, dependecy, result_type, properties);
        }

        @Override
        public Object getResultIfDuplicate(TcmsConnection connection) {
            try {
                TestCase.filter f = new TestCase.filter();
                TestCase.create comand = (TestCase.create) current;
                f.summary = comand.summary;
                f.category = comand.category;
                f.priority = comand.priority;
                f.plan = comand.plan;

                CommandWrapper script = new CommandWrapper.Generic(f, TestCase.class,null);
                script.perform(connection);
                return script.getResult(TestCase.class);
            } catch (XmlRpcFault ex) {
                Logger.getLogger(TcmsUploader.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        @Override
        public boolean processDependecies() {
            return true;
        }
        public Hashtable<String, String> description() {
            Hashtable<String, String> map = current.descriptionMap();
            map.put("priority",properties.priority + " (" +map.get("priority")+")");
            map.put("category",properties.category + " (" +map.get("category")+")");
            map.put("product",properties.product + " (" +map.get("product")+")");
            map.put("plan",properties.plan + " (" +map.get("plan")+")");
            
            return map;
        }

    }

    public static class TestRunCreate extends CommandWrapper {

        public TestRunCreate(TcmsCommand current, Class result_type,TcmsProperties properties) {
            super(current, result_type,properties);
        }

        @Override
        public Object getResultIfDuplicate(TcmsConnection connection) {
            try {
                TestRun.filter f = new TestRun.filter();
                TestRun.create command = (TestRun.create) current;
                f.build = command.build;
                f.plan = command.plan;
                f.summary = command.summary;
                f.manager = command.manager;

                CommandWrapper script = new CommandWrapper.Generic(f, TestRun.class,null);
                script.perform(connection);
                return script.getResult(TestRun.class);
            } catch (XmlRpcFault ex) {
                Logger.getLogger(TcmsUploader.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
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
            map.put("product_version",properties.product_v + " (" +map.get("product_version")+")");
            map.put("manager",properties.manager + " (" +map.get("manager")+")");
            map.put("product",properties.product + " (" +map.get("product")+")");
            map.put("plan",properties.plan + " (" +map.get("plan")+")");
            
            for (CommandWrapper deps : getDependecies()) {
                if (deps.current() instanceof Build.create) {
                    map.put("build",deps.description().get("name") + " (" +map.get("build")+")");
                }
            }
            
            return map;
        }
    }

    public static class TestCaseRunCreate extends CommandWrapper {

        public TestCaseRunCreate(TcmsCommand current, Class result_type,TcmsProperties properties) {
            super(current, result_type,properties);
        }

        @Override
        public Object getResultIfDuplicate(TcmsConnection connection) {
             try {
                TestCaseRun.filter f = new TestCaseRun.filter();
                TestCaseRun.create command = (TestCaseRun.create) current;
                f.build = command.build;
                f.run = command.run;
                f.caseVar = command.caseVar;
                f.case_run_status = command.case_run_status;

                CommandWrapper script = new CommandWrapper.Generic(f, TestCaseRun.class,null);
                script.perform(connection);
                return script.getResult(TestCaseRun.class);
            } catch (XmlRpcFault ex) {
                Logger.getLogger(TcmsUploader.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
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
            for (CommandWrapper deps : getDependecies()) {
                 if (deps.current() instanceof Build.create) {
                     map.put("build",deps.description().get("name") + " (" +map.get("build")+")");
          
                } else if (deps.current() instanceof TestRun.create) {
                     map.put("run",deps.description().get("summary") + " (" +map.get("run")+")");
          
                } else if (deps.current() instanceof TestCase.create) {
                     map.put("case",deps.description().get("summary") + " (" +map.get("case")+")");
              
                }
            }
            
            return map;
        }
    }

    public static CommandWrapper wrap(TcmsCommand current, Class result_type,TcmsProperties properties) {
        if (current instanceof TestCase.create) {
            return new CommandWrapper.TestCaseCreate(current, result_type,properties);
        } else if (current instanceof TestCaseRun.create) {
            return new CommandWrapper.TestCaseRunCreate(current, result_type,properties);
        } else if (current instanceof TestRun.create) {
            return new CommandWrapper.TestRunCreate(current, result_type,properties);
        } else if (current instanceof Build.create) {
            return new CommandWrapper.BuildCreate(current, result_type,properties);
        }

        return new CommandWrapper.Generic(current, result_type,properties);
    }
}
