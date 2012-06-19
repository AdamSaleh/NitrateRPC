package NitrateIntegration;

import com.redhat.engineering.jenkins.testparser.Parser;
import com.redhat.engineering.jenkins.testparser.results.TestResults;
import com.redhat.nitrate.TcmsAccessCredentials;
import com.redhat.nitrate.TcmsConnection;
import com.redhat.nitrate.command.Auth;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixBuild;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import redstone.xmlrpc.XmlRpcException;
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

    /**
     * Check whether TcmsReviewAction has been added to the build, if not, fix
     * that. This method contains mutex, so that TcmsReviewAction is added only
     * once.
     *
     * @param build
     * @param listener
     * @return Always true, so Jenkins may continue building.
     */
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
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

        return true;
    }

    /**
     * Locates, checks and saves reports after build finishes, and adds result
     * to TcmsReviewAction corresponding to the build.
     *
     * @param build
     * @param launcher
     * @param listener
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Starting TCMS integration plugin");
        listener.getLogger().println("Looking for TestNG results report in workspace using pattern: "
                + reportLocationPattern);



        FilePath[] paths = null;
        paths = Parser.locateReports(build.getWorkspace(), reportLocationPattern);

        if (paths.length == 0) {
            listener.getLogger().println("Did not find any matching files.");
            return true;
        }

        boolean filesSaved = Parser.saveReports(Parser.getReportDir(build), paths, listener.getLogger(), "test-results");
        if (!filesSaved) {
            listener.getLogger().println("Failed to save TestNG XML reports");
            return true;
        }

        TestResults results = Parser.loadResults(build, null, "test-results");

        TcmsReviewAction action = build instanceof MatrixRun
                ? ((MatrixRun) build).getParentBuild().getAction(TcmsReviewAction.class)
                : ((MatrixBuild) build).getAction(TcmsReviewAction.class);

        Map<String, String> vars = new HashMap<String, String>();
        vars.putAll(build.getBuildVariables());

        action.addGatherPath(results, build, vars);

        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<hudson.tasks.Publisher> {

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
                return FormValidation.warning("Connection error: " + ex.getMessage());
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
            List<String> problems = new LinkedList();
            
            try {
                session = auth.invoke(c);
                if (session.length() > 0) {
                    c.setSession(session);
                }
                properties.setConnection(c);
                properties.reload();
                problems = TcmsProperties.checkUsersetProperties(properties);
                
            } catch (XmlRpcFault ex) {
                // FIXME: check if really only username or password can go wrong (network down, timeout, conn. refused...)
                return FormValidation.error("Possibly wrong username/password");
            }
            
            if(!problems.isEmpty()){
                return FormValidation.error(problems.toString().replace("[", "").replace("]", ""));
            }

            return FormValidation.ok();

        }

        public FormValidation doTestEnv(@QueryParameter("serverUrl") final String serverUrl,
                @QueryParameter("username") final String username,
                @QueryParameter("password") final String password,
                @QueryParameter("env") final String env) {

            TcmsConnection c = null;
            try {
                c = new TcmsConnection(serverUrl);
            } catch (MalformedURLException ex) {
                return FormValidation.error("Possibly wrong server URL");
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
                return FormValidation.error(ex.getMessage());
            } catch (XmlRpcException ex) {
                if (ex.getMessage().equals("The response could not be parsed.")) {
                    return FormValidation.error("Possibly wrong username/password");
                }
                if (ex.getMessage().equals("A network error occurred.")) {
                    return FormValidation.error("Cannot connect to server. Check URL or try reloading this page");
                }
                return FormValidation.error(ex.getMessage());
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
            save();
            return super.configure(req, formData);
        }
    }
}
