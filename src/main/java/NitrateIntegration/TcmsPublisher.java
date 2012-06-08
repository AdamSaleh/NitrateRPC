package NitrateIntegration;

import com.redhat.engineering.jenkins.testparser.Parser;
import com.redhat.engineering.jenkins.testparser.results.TestResults;
import com.redhat.nitrate.Auth;
import com.redhat.nitrate.TcmsAccessCredentials;
import com.redhat.nitrate.TcmsConnection;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import redstone.xmlrpc.XmlRpcFault;

/**
 * Sample {@link Builder}.
 *
 * <p> When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new {@link HelloWorldBuilder}
 * is created. The created instance is persisted to the project configuration
 * XML by using XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p> When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * @author Kohsuke Kawaguchi
 */
public class TcmsPublisher extends Recorder {

    public final String serverUrl;
    private TcmsAccessCredentials credentials;
    public final String reportLocationPattern;
    public final String plan;
    public final String product;
    public final String product_v;
    public final String category;
    public final String priority;
    public final String manager;
    public final String env;
    static final Object lock = new Object();

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public TcmsPublisher(String serverUrl, String username, String password,
            String plan,
            String product,
            String product_v,
            String category,
            String priority,
            String manager,
            String env,
            String testPath) {

        this.serverUrl = serverUrl;
        this.reportLocationPattern = testPath;

        this.category = category;
        this.manager = manager;
        this.plan = plan;
        this.priority = priority;
        this.product = product;
        this.product_v = product_v;

        this.env = env;

    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Starting TCMS integration plugin");
        listener.getLogger().println("Looking for TestNG results report in workspace using pattern: "
                + reportLocationPattern);

        AbstractBuild agregateBuild = build;
        if (build instanceof MatrixRun) {
            MatrixRun mrun = (MatrixRun) build;
            agregateBuild = mrun.getParentBuild();
        }
        synchronized (lock) {
            if (agregateBuild.getAction(TcmsReviewAction.class) == null) {
                agregateBuild.getActions().add(new TcmsReviewAction(agregateBuild,
                        serverUrl,
                        plan,
                        product,
                        product_v,
                        category,
                        priority,
                        manager,
                        env,
                        reportLocationPattern));
            }
        }


        FilePath[] paths = null;
        paths = Parser.locateReports(build.getWorkspace(), reportLocationPattern);

        if (paths.length == 0) {
            listener.getLogger().println("Did not find any matching files.");
            return true;
        }

        //paths = Parser.checkReports(build, paths, listener.getLogger());

        boolean filesSaved = Parser.saveReports(Parser.getReportDir(build), paths, listener.getLogger(), "test-results");
        if (!filesSaved) {
            listener.getLogger().println("Failed to save TestNG XML reports");
            return true;
        }

        TestResults results = Parser.loadResults(build, null, "test-results");

        TcmsReviewAction action = agregateBuild.getAction(TcmsReviewAction.class);
        Map<String, String> vars = new HashMap<String, String>();
        vars.putAll(build.getBuildVariables());

        action.addGatherPath(results, build, vars);

        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }

    /**
     * Descriptor for {@link HelloWorldBuilder}. Used as a singleton. The class
     * is marked as public so that it can be accessed from views.
     *
     * <p> See
     * <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<hudson.tasks.Publisher> {

        /**
         * To persist global configuration information, simply store it in a
         * field and call save().
         *
         * <p> If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        /**
         * Do not instantiate DescriptorImpl.
         */
        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value This parameter receives the value that the user has
         * typed.
         * @return Indicates the outcome of the validation. This is sent to the
         * browser.
         */
        public FormValidation checkServerUrl(String value, String username, String password) {
            if (value.length() == 0) {
                return FormValidation.error("Please set an url");
            }
            try {
                TcmsConnection testCon = new TcmsConnection(value);
                testCon.setUsernameAndPassword(username, password);
                boolean testTcmsConnection = testCon.testTcmsConnection();
                if (testTcmsConnection == false) {
                    return FormValidation.warning("XML-RPC Service not found");
                }
            } catch (MalformedURLException ex) {
                return FormValidation.error("Url is malformed");
            } catch (IOException ex) {
                return FormValidation.warning("Connection error");
            }
            return FormValidation.ok();
        }

        public FormValidation doTestConnection(@QueryParameter("serverUrl") final String serverUrl,
                @QueryParameter("username") final String username,
                @QueryParameter("password") final String password,
                @QueryParameter("plan") final String plan,
                @QueryParameter("product") final String product,
                @QueryParameter("product_v") final String product_v,
                @QueryParameter("category") final String category,
                @QueryParameter("priority") final String priority,
                @QueryParameter("manager") final String manager) {
            FormValidation url_val = checkServerUrl(serverUrl, username, password);
            if (url_val != FormValidation.ok()) {
                return url_val;
            }

            TcmsConnection c = null;
            try {
                c = new TcmsConnection(serverUrl);
            } catch (MalformedURLException ex) {
                Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
                return FormValidation.error("Something weird happened");
            }

            c.setUsernameAndPassword(username, password);
            Auth.login_krbv auth = new Auth.login_krbv();
            String session;
            TcmsProperties properties = new TcmsProperties(plan, product, product_v, category, priority, manager);

            try {
                session = auth.invoke(c);
                if (session.length() > 0) {
                    c.setSession(session);
                }
                properties.setConnection(c);
                properties.reload();
            } catch (XmlRpcFault ex) {
                return FormValidation.error("Possibly wrong username/password");
            }

            if (properties.getPlanID() == null) {
                return FormValidation.error("Possibly wrong plan id");
            }
            if (properties.getProductID() == null) {
                return FormValidation.error("Possibly wrong product name");
            }
            if (properties.getProduct_vID() == null) {
                return FormValidation.error("Possibly wrong product version");
            }
            if (properties.getCategoryID() == null) {
                return FormValidation.error("Possibly wrong category name");
            }
            if (properties.getPriorityID() == null) {
                return FormValidation.error("Possibly wrong priority name");
            }
            if (properties.getManagerId() == null) {
                return FormValidation.error("Possibly wrong manager's username");
            }
            return FormValidation.ok();

        }

        public FormValidation doTestEnv(@QueryParameter("serverUrl") final String serverUrl,
                @QueryParameter("username") final String username,
                @QueryParameter("password") final String password,
                @QueryParameter("env") final String env) {
            FormValidation url_val = checkServerUrl(serverUrl, username, password);
            if (url_val != FormValidation.ok()) {
                return url_val;
            }

            TcmsConnection c = null;
            try {
                c = new TcmsConnection(serverUrl);
            } catch (MalformedURLException ex) {
                Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
                return FormValidation.error("Something weird happened");
            }


            c.setUsernameAndPassword(username, password);
            Auth.login_krbv auth = new Auth.login_krbv();
            String session;
            TcmsEnvironment environment = new TcmsEnvironment(env);

            try {
                session = auth.invoke(c);
                if (session.length() > 0) {
                    c.setSession(session);
                }
                environment.setConnection(c);
                environment.reloadEnvId();
            } catch (XmlRpcFault ex) {
                return FormValidation.error("Possibly wrong username/password");
            }

            if (environment.getEnvId() == null) {
                return FormValidation.error("Possibly wrong environment group");
            }
            return FormValidation.ok();

        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Integration with Nitrate TCMS";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            //formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req, formData);
        }
    }
}
