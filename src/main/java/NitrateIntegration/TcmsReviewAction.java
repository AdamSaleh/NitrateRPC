/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import NitrateIntegration.CommandWrapper.CommandWrapper;
import com.redhat.engineering.jenkins.testparser.results.TestResults;
import com.redhat.nitrate.TcmsAccessCredentials;
import com.redhat.nitrate.TcmsConnection;
import com.redhat.nitrate.TcmsException;
import com.redhat.nitrate.command.Build;
import com.redhat.nitrate.command.TestRun;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author asaleh
 * @author jrusnack
 */
public class TcmsReviewAction implements Action {

    public AbstractBuild<?, ?> build;
    public TcmsReport report;
    public TcmsReviewActionSettings settings;
    public HashSet<String> envCheckProblems = new HashSet<String>();
    /**
     * Stores mapping from old properties/Jenkins axes(names and values) to new
     * names and values possibly changed by user
     */
    public final PropertyTransform property = new PropertyTransform();
    private TcmsGatherer gatherer;
    private LinkedHashMap<String, Hashtable<String, String>> env_status;
    private boolean wrongProperty;
    private HashSet<String> propertyWWrongValue;
    boolean change_axis = false;
    boolean envVarsChecked = false;

    /*
     * Used to store exception, if occurs, and print it in reasonable format,
     * not ugly long exception. Shown under Update settings and Check
     * Environmental vars
     */
    private String envCheckException;


    /**
     * Class that defines transformations (key, value) -> (key, value). This is
     * used in case when user renames Jenkins`s axes to some new names - new
     * transformation from original names and values to new ones is added.
     */
    private class PropertyTransform {

        private class Touple<K, V> implements Entry<K, V> {

            private K key;
            private V val;

            public Touple(K key, V val) {
                this.key = key;
                this.val = val;
            }

            public K getKey() {
                return key;
            }

            public V getValue() {
                return val;
            }

            public Object setValue(Object v) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        public void clearTransformations() {
            propertyTransform.clear();
        }

        public void addTransformation(String oldprop, String oldval, String newprop, String newval) {
            propertyTransform.put(new Touple(oldprop, oldval), new Touple(newprop, newval));
        }
        private HashMap<Entry<String, String>, Entry<String, String>> propertyTransform;

        public Map<String, String> transformVariables(Map<String, String> old) {

            HashMap<String, String> transformed = new HashMap<String, String>();

            for (Entry<String, String> prop_value : old.entrySet()) {

                Entry<String, String> newprop_value = prop_value;
                if (propertyTransform.containsKey(prop_value)) {
                    newprop_value = propertyTransform.get(prop_value);
                }


                transformed.put(newprop_value.getKey(), newprop_value.getValue());
            }
            return transformed;
        }
    }


    public TcmsReviewAction(AbstractBuild build, String serverUrl,
            String plan,
            String product,
            String product_v,
            String category,
            String priority,
            String manager,
            String env,
            String testPath) {

        this.build = build;
        
        this.report = new TcmsReport();
        this.settings = new TcmsReviewActionSettings(serverUrl, plan, product, product_v, category, priority, manager, env, testPath);
        
        
        gatherer = new TcmsGatherer(this.settings.getProperties(), this.settings.getEnvironment());
        env_status = new LinkedHashMap<String, Hashtable<String, String>>();
        wrongProperty = false;
        propertyWWrongValue = new HashSet<String>();
    }

    public boolean isChange_axis() {
        return change_axis;
    }

    public TcmsReviewActionSettings getSettings() {
        return settings;
    }

    public HashSet<String> getEnvCheckProblems() {
        return envCheckProblems;
    }
    
    public HashSet<String> getPropertyWWrongValue() {
        return propertyWWrongValue;
    }

    public String getIconFileName() {
        return Definitions.__ICON_FILE_NAME;
    }

    public String getDisplayName() {
        return Definitions.__DISPLAY_NAME;
    }

    public String getUrlName() {
        return Definitions.__URL_NAME;
    }

    public String getPrefix() {
        return Definitions.__PREFIX;
    }

    public TcmsGatherer getGatherer() {
        return gatherer;
    }

    public LinkedHashMap<String, Hashtable<String, String>> getEnv_status() {
        return env_status;
    }

    public boolean existsWrongProperty() {
        return wrongProperty;
    }



    public AbstractBuild getBuild() {
        return build;
    }

    

    public boolean envCheckExceptionOccured() {
        if (envCheckException == null) {
            return false;
        }
        return !envCheckException.isEmpty();
    }


    public String getEnvCheckException() {
        return envCheckException;
    }

    // FIXME: javadoc
    public void doGather(StaplerRequest req, StaplerResponse rsp) throws IOException {
        settings.clearUpdateException();
        gatherer.clear();
        
        try {
            
            if (req.getParameter("Submit").equals("Gather report from test-files")) {
                settings.updateCredentialsFromRequest(req);
                
                /*
                 * This part may be omitted, because bad credentials would still
                 * generate exception (see TcmsConnection.invoke), but testTcmsConnection
                 * can precisely identify HTTP 401, while TcmsConnection.invoke 
                 * just guesses (in case of HTTP 401 XmlRpcException "The 
                 * response could not be parsed." is thrown)
                 */
                // FIXME: doesn`t work, connect throws exception sooner than textTcmsConnection
                //TcmsConnection connection = settings.getConnection();
                //boolean test = connection.testTcmsConnection();
            } 

            settings.getConnectionAndUpdate();
            settings.getEnvironment().reloadEnvId();
            report.checkEnvironmentMapping(settings.getEnvironment());
            settings.getProperties().reload();

            gatherer.setProperties(settings.getProperties());
            gatherer.setEnvironment(settings.getEnvironment());

            for (TcmsReport.TestRunResults r : report.getTestRuns()) {
                gatherer.gather(r.results, build, r.build, r.variables);
            }

        } catch (TcmsException ex) {
            Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
            settings.setUpdateException(ex.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
            settings.setUpdateException( ex.toString());
        } finally {
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        }

    }

    // FIXME: javadoc
    public void doUpdateSettings(StaplerRequest req, StaplerResponse rsp) throws IOException {
       settings.doUpdateSettings(req, rsp, report);
    }    
    
    public void doCheckSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException  {
        HashSet<String> problems = new HashSet<String>();
        envCheckException = "";
        change_axis = false;
        
        if (req.getParameter("Submit").equals("Change")) {
            change_axis = true;
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        }

        problems = updateReportFromReq(req);
        
        try {
            settings.getConnectionAndUpdate();

            report.checkEnvironmentMapping(settings.getEnvironment());
        } catch (TcmsException ex){
            envCheckException = ex.getMessage();
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        }

        this.envCheckProblems = problems;
        envVarsChecked = true;

        rsp.sendRedirect("../" + Definitions.__URL_NAME);
    }
    
    
    private HashSet<String> updateReportFromReq(StaplerRequest req){
        Map params = req.getParameterMap();
        HashSet<String> problems = new HashSet<String>();
        
        /*
         * update values first
         */
        for (Iterator it = params.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) it.next();
            
            if (entry.getKey().startsWith("value-")) {
                String newValue = ((String[]) entry.getValue())[0];
                String property = entry.getKey().replaceFirst("value-", "");
                String oldValue = property.split("=>")[1];
                property = property.split("=>")[0];

                if (!oldValue.equals(newValue)) {
                    try {
                        report.changeEnvValue(property, oldValue, newValue);
                    } catch (IllegalArgumentException ex) {
                        problems.add(ex.getMessage());
                    }
                }
            }
        }
        
        /*
         * change property-names second
         */
        for (Iterator it = params.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) it.next();
            
            if (entry.getKey().startsWith("property-")) {
                String newProperty = ((String[]) entry.getValue())[0];
                String oldProperty = entry.getKey().replaceFirst("property-", "");
                
                if (!oldProperty.equals(newProperty)) {
                    try {
                        report.changeEnvProperty(oldProperty, newProperty);
                    } catch (IllegalArgumentException ex) {
                        problems.add(ex.getMessage());
                    }
                }
            }
        }
        
        return problems;
    }

    // refactor
    public void doReportSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException, InterruptedException, TcmsException {

        if (req.getParameter("Submit").equals("update")) {
            // update build name
            Build.create buildCreate = (Build.create) gatherer.getCommandList("Build.create").getFirst().current;
            buildCreate.name = req.getParameter("buildName");

            // update testRun summary
            TestRun.create testRunCreate = (TestRun.create) gatherer.getCommandList("TestRun.create").getFirst().current;
            testRunCreate.summary = req.getParameter("testRunSummary");

            rsp.sendRedirect("../" + Definitions.__URL_NAME);
        } else {
            try {
                // parse 
                String input;
                for (CommandWrapper c : gatherer) {
                    String a = new Integer(c.hashCode()).toString();
                    input = req.getParameter(a);
                    if (input != null) {
                        c.setExecutable(true);
                        c.setChecked(true);
                    } else {
                        c.setExecutable(false);
                        c.setChecked(false);
                    }
                }
                
                
                TcmsConnection connection = null;
                connection = settings.getConnection();
                upload(gatherer,connection);

            } catch (TcmsException ex) {
                Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
            }
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
        }

    }

    public static void upload(TcmsGatherer gathered, TcmsConnection connection) {
        boolean at_least_one;
        boolean at_least_one_not_duplicate;
        do {
            at_least_one = false;
            at_least_one_not_duplicate = false;
            for (CommandWrapper command : gathered) {
                if (command.isExecutable()) {
                    if (command.resolved()) { //If dependecnies are satisfied
                        if (command.completed() == false) { // not to run command again
                            if (command.performed() == false) { // this command had satisfied dependecies but failed for some reason, so dont loop o it
                                boolean tmp = command.perform(connection);
                                if (tmp) {
                                    at_least_one = true;
                                }
                                if (command.duplicate() == false) {
                                    at_least_one_not_duplicate = true;
                                }
                            }
                        }
                    } else { // dependencies we not met
                        command.setUnmetDependencies();
                    }
                }

            }
        } while (at_least_one && at_least_one_not_duplicate);
    }
    
}
