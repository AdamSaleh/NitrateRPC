/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import NitrateIntegration.CommandWrapper.CommandWrapper;
import com.redhat.engineering.jenkins.testparser.results.TestResults;
import com.redhat.nitrate.TcmsAccessCredentials;
import com.redhat.nitrate.TcmsConnection;
import com.redhat.nitrate.command.Auth;
import com.redhat.nitrate.command.Build;
import com.redhat.nitrate.command.TestRun;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;

/**
 *
 * @author asaleh
 * @author jrusnack
 */
public class TcmsReviewAction implements Action {

    public AbstractBuild<?, ?> build;
    public TcmsProperties properties;
    public TcmsEnvironment environment;
    public List<String> update_problems = new LinkedList<String>();
    public HashSet<String> env_check_problems = new HashSet<String>();
    /**
     * Stores mapping from old properties/Jenkins axes(names and values) to new
     * names and values possibly changed by user
     */
    public final PropertyTransform property = new PropertyTransform();
    private TcmsGatherer gatherer;
    private String serverUrl;
    private TcmsAccessCredentials credentials;
    private LinkedHashMap<String, Hashtable<String, String>> env_status;
    private boolean wrongProperty;
    private HashSet<String> propertyWWrongValue;
    boolean change_axis = false;
    boolean setting_updated = false;
    LinkedList<GatherFiles> gatherFiles = new LinkedList<GatherFiles>();

    /*
     * Used to store exception, if occurs, and print it in reasonable format,
     * not ugly long exception. Shown under Update settings and Check
     * Environmental vars
     */
    private String updateException;
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

    public static class GatherFiles {

        public TestResults results;
        public AbstractBuild build;
        public Map<String, String> variables;

        public GatherFiles(TestResults results, AbstractBuild build, Map<String, String> variables) {
            this.results = results;
            this.build = build;
            this.variables = variables;
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

        this.properties = new TcmsProperties(plan, product, product_v, category, priority, manager);
        this.credentials = new TcmsAccessCredentials();

        this.serverUrl = serverUrl;
        this.environment = new TcmsEnvironment(env);
        this.build = build;
        gatherer = new TcmsGatherer(properties, environment);
        env_status = new LinkedHashMap<String, Hashtable<String, String>>();
        propertyWWrongValue = new HashSet<String>();
        wrongProperty = false;
    }

    public boolean isChange_axis() {
        return change_axis;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public boolean isSetting_updated() {
        return setting_updated;
    }

    public List<String> getUpdate_problems() {
        return update_problems;
    }

    public HashSet<String> getEnv_check_problems() {
        return env_check_problems;
    }

    public String getUsername() {
        return credentials.getUsername();
    }

    public String getPassword() {
        return credentials.getPassword();
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

    public HashSet<String> getPropertyWWrongValue() {
        return propertyWWrongValue;
    }

    public TcmsEnvironment getEnvironment() {
        return environment;
    }

    public AbstractBuild getBuild() {
        return build;
    }

    public boolean updateExceptionOccured() {
        if (updateException == null) {
            return false;
        }
        return !updateException.isEmpty();
    }

    public boolean envCheckExceptionOccured() {
        if (envCheckException == null) {
            return false;
        }
        return !envCheckException.isEmpty();
    }

    public String getUpdateException() {
        return updateException;
    }

    public String getEnvCheckException() {
        return envCheckException;
    }

    public void clearGatherPaths() {
        gatherFiles.clear();
    }

    public void addGatherPath(TestResults results, AbstractBuild build, Map<String, String> variables) {
        /*
         * Preventing creation of un-initialized null-s, that would be added to
         * gather-files
         */
        GatherFiles f = null;
        f = new GatherFiles(results, build, variables);

        if (f != null) {
            gatherFiles.add(f);
        }
    }

    public static TcmsConnection connect(String serverUrl, TcmsAccessCredentials credentials) throws IOException, XmlRpcFault {
        TcmsConnection connection = null;
        connection = new TcmsConnection(serverUrl);
        connection.setUsernameAndPassword(credentials.getUsername(), credentials.getPassword());

        Auth.login_krbv auth = new Auth.login_krbv();
        String session;
        session = auth.invoke(connection);
        if (session.length() > 0) {
            connection.setSession(session);
        } else {
            throw new IOException("Couln't connect to tcms server");
        }
        return connection;
    }

    public void doGather(StaplerRequest req, StaplerResponse rsp) throws IOException {
        updateException = "";
        gatherer.clear();
        if (req.getParameter("Submit").equals("Gather report from test-files")) {
            credentials.setUsername(req.getParameter("_.username"));
            credentials.setPassword(req.getParameter("_.password"));
        }

        try {
            TcmsConnection connection = null;
            connection = connect(serverUrl, credentials);

            environment.setConnection(connection);
            environment.reloadEnvId();

            properties.setConnection(connection);
            properties.reload();

            gatherer.setProperties(properties);
            gatherer.setEnvironment(environment);

            for (GatherFiles gatherfile : gatherFiles) {
                gatherer.gather(gatherfile.results, build, gatherfile.build, gatherfile.variables);
            }
        } catch (IOException ex) {
            updateException = ex.toString();
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        } catch (XmlRpcException ex) {
            updateException = ex.toString();
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        } catch (XmlRpcFault ex) {
            updateException = ex.toString();
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        }

        rsp.sendRedirect("../" + Definitions.__URL_NAME);
    }

    public void doUpdateSettings(StaplerRequest req, StaplerResponse rsp) throws IOException {
        setting_updated = false;
        List<String> problems = new LinkedList<String>();

        String serverUrl = req.getParameter("_.serverUrl");
        String username = req.getParameter("_.username");
        String password = req.getParameter("_.password");
        updateException = "";

        /**
         * First try new URL, username and password, if unsuccessful, set
         * exception and end
         */
        if (this.serverUrl.contentEquals(serverUrl)
                && credentials.getUsername().contentEquals(username)
                && credentials.getPassword().contentEquals(password)) {
            //do nothing
        } else {

            try {
                TcmsConnection c = new TcmsConnection(serverUrl);
                c.setUsernameAndPassword(username, password);
                if (c.testTcmsConnection()) {
                    this.serverUrl = serverUrl;
                    this.credentials = new TcmsAccessCredentials(username, password);
                }
            } catch (IOException ex) {
                Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
                updateException = ex.toString();
                rsp.sendRedirect("../" + Definitions.__URL_NAME);
                return;
            }

            credentials.setUsername(username);
            credentials.setPassword(password);
        }

        String plan = req.getParameter("_.plan");
        String product = req.getParameter("_.product");
        String product_v = req.getParameter("_.product_v");
        String category = req.getParameter("_.category");
        String priority = req.getParameter("_.priority");
        String manager = req.getParameter("_.manager");

        TcmsProperties properties = new TcmsProperties(plan, product, product_v, category, priority, manager);


        String env = req.getParameter("_.environment");
        TcmsEnvironment environment = new TcmsEnvironment(env);
        String session;
        Auth.login_krbv auth = new Auth.login_krbv();

        try {
            TcmsConnection connection = null;
            connection = connect(serverUrl, credentials);

            session = auth.invoke(connection);

            if (session != null && session.length() > 0) {
                connection.setSession(session);
            }
            environment.setConnection(connection);
            environment.reloadEnvId();
            properties.setConnection(connection);
            properties.reload();

        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
            updateException = ex.toString();
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        } catch (XmlRpcException ex) {
            Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
            updateException = ex.toString();
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        }

        problems = TcmsProperties.checkUsersetProperties(properties);
        
        /**
         * Check environment only if some identifier was submitted
         */
        if (!env.isEmpty() && environment.getEnvId() == null) {
            problems.add("Possibly wrong environment group: " + environment.env);
        }

        if (problems.isEmpty()) {
            this.properties = properties;
            this.environment = environment;
            setting_updated = true;
        }

        this.update_problems = problems;
        rsp.sendRedirect("../" + Definitions.__URL_NAME);
    }


    public void doCheckSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException {

        HashSet<String> problems = new HashSet<String>();
        envCheckException = "";

        change_axis = false;
        if (req.getParameter("Submit").equals("Change")) {
            change_axis = true;
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        }

        Map params = req.getParameterMap();

        /*
         * update values first
         */
        for (Iterator it = params.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) it.next();
            if (entry.getKey().startsWith("value-")) {
                String value = ((String[]) entry.getValue())[0];

                String property_name = entry.getKey().replaceFirst("value-", "");
                String value_name = property_name.split("=>")[1];
                property_name = property_name.split("=>")[0];



                for (GatherFiles env : gatherFiles) {

                    /*
                     * Assert that we are trying to assing new value
                     */
                    if (!value.equals(value_name)) {
                        /*
                         * Assert that new value is not already present under
                         * property
                         */
                        if (!env_status.get(property_name).containsKey(value)) {
                            if (env.variables.containsKey(property_name)) {
                                if (env.variables.get(property_name).equals(value_name)) {
                                    env.variables.remove(property_name);
                                    env.variables.put(property_name, value);
                                }
                            }
                        } else {
                            /*
                             * If new value is already present, print error
                             */
                            problems.add(property_name + " already contained value " + value);
                        }
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
                String new_property_name = ((String[]) entry.getValue())[0];

                String property_name = entry.getKey().replaceFirst("property-", "");

                for (GatherFiles env : gatherFiles) {
                    if (env.variables.containsKey(property_name) && !property_name.equals(new_property_name)) {
                        if (env.variables.containsKey(new_property_name)) {
                            problems.add("Duplicit property name error.");
                        } else {
                            String val = env.variables.get(property_name);
                            env.variables.remove(property_name);
                            env.variables.put(new_property_name, val);
                        }

                    }
                }
            }
        }

        /*
         * test
         */
        try {
            TcmsConnection connection = null;
            connection = connect(serverUrl, credentials);

            connection = new TcmsConnection(serverUrl);
            connection.setUsernameAndPassword(credentials.getUsername(), credentials.getPassword());

            boolean test = connection.testTcmsConnection();
            if (test == false) {
                throw new IOException("Couln't connect to tcms server");
            }

            Auth.login_krbv auth = new Auth.login_krbv();
            String session;
            session = auth.invoke(connection);
            if (session.length() > 0) {
                connection.setSession(session);
            }
            environment.setConnection(connection);
            environment.reloadEnvId();

        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
            envCheckException = ex.getMessage();
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        } catch (XmlRpcException ex) {
            /**
             * This exception might happen if method
             * connection.testTcmsConnection passes, but afterwards network
             * fails and auth.invoke(connection) fails with exception "The
             * response could not be parsed."
             */
            Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
            envCheckException = ex.toString();
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        } catch (MalformedURLException ex) {
            Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
            envCheckException = ex.toString();
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        } catch (IOException ex) {
            /**
             * This exception is usually thrown by
             * connection.testTcmsConnection, in case when network is down.
             * Produces exception message that contains just URL
             */
            Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
            envCheckException = ex.toString();
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        }

        env_status.clear();
        propertyWWrongValue.clear();
        wrongProperty = false;
        for (GatherFiles env : gatherFiles) {
            for (Map.Entry<String, String> prop : env.variables.entrySet()) {
                // check value
                String name = prop.getKey();
                String val = prop.getValue();

                String result = "UNKNOWN";
                if (environment.containsProperty(name)) {
                    if (environment.containsValue(name, val)) {
                        result = "CHECKED";
                    } else {
                        result = "VALUE";
                        propertyWWrongValue.add(name);
                    }
                } else {
                    result = "PROPERTY";
                    wrongProperty = true;
                }


                if (env_status.containsKey(name) == false) {
                    env_status.put(name, new Hashtable<String, String>());
                }
                env_status.get(name).put(val, result);


            }
        }

        this.env_check_problems = problems;

        rsp.sendRedirect("../" + Definitions.__URL_NAME);
    }

    public void doReportSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException, InterruptedException {

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

                connectAndUpload(gatherer, credentials, serverUrl);

            } catch (XmlRpcFault ex) {
                Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MalformedURLException ex) {
                Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
            }
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
        }

    }

    public static void connectAndUpload(TcmsGatherer gathered, TcmsAccessCredentials credentials, String serverUrl) throws MalformedURLException, XmlRpcFault, IOException {
        TcmsConnection connection = null;
        connection = connect(serverUrl, credentials);
        upload(gathered, connection);
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
