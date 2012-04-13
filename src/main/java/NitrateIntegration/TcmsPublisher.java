package NitrateIntegration;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Extension;
import hudson.tasks.BuildStepMonitor;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Recorder;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

import com.redhat.nitrate.*;
import hudson.tasks.test.TestResult;

import hudson.tasks.junit.TestResultAction;
import java.net.URL;
import java.util.Hashtable;

import java.util.LinkedList;

import redstone.xmlrpc.*;
import com.redhat.engineering.jenkins.testparser.Parser;
import com.redhat.engineering.jenkins.testparser.results.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link HelloWorldBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class TcmsPublisher extends Recorder {

    public final String serverUrl;
    public final String username;
    public final String password;
    public final String product;
    private int product_id;
    public final String testPath;
    private int product_category;
    private TcmsConnection connection;

    public TcmsGatherer gatherer;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public TcmsPublisher(String serverUrl, String username, String password, String product, String testPath) {
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
        this.product = product;
        this.testPath = testPath;

        try {
            connection = new TcmsConnection(serverUrl);

            Product.check_product get_command = new Product.check_product();
            get_command.name = product;

            XmlRpcArray array = (XmlRpcArray) connection.invoke(get_command);
            XmlRpcStruct struct = array.getStruct(0);
            Product p = (Product) TcmsConnection.rpcStructToFields(struct, Product.class);

            this.product_id = p.id;
            this.product_category = p.category;


        } catch (IllegalAccessException ex) {
            Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        listener.getLogger().println("Starting TCMS integration plugin");
        listener.getLogger().println("Looking for TestNG results report in workspace using pattern: "
                + testPath);


        listener.getLogger().println("Connecting to TCMS at " + serverUrl);
        listener.getLogger().println("Using login: " + username);

        Auth.login auth = new Auth.login(username, password);
        String session;
        try {
            session = auth.invoke(connection);
            listener.getLogger().println("TCMS session started: " + session);
            if (session.length() > 0) {
                connection.setSession(session);
            }

            gatherer = new TcmsGatherer(listener.getLogger(), build, connection);
            gatherer.gather(testPath);
            build.getActions().add(new TcmsReviewAction(build,gatherer));
           // TcmsUploader.upload(gatherer, connection);

            connection.invoke(new Auth.logout());
        } catch (XmlRpcFault ex) {
            listener.getLogger().println(ex.getMessage());
            return false;
        }
        listener.getLogger().println("Logged out");
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }

    /**
     * Descriptor for {@link HelloWorldBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<hudson.tasks.Publisher> {

        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        /**
         * Do not instantiate DescriptorImpl.
         */
        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckServerUrl(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please set an url");
            }
            try {
                URL url = new URL(value);
                boolean testTcmsConnection = TcmsConnection.testTcmsConnection(url);
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
                @QueryParameter("product") final String product) {
            FormValidation url_val = doCheckServerUrl(serverUrl);
            if (url_val != FormValidation.ok()) {
                return url_val;
            }

            TcmsConnection c = null;
            try {
                c = new TcmsConnection(serverUrl);
            } catch (MalformedURLException ex) {
                Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
            }

            Auth.login auth = new Auth.login(username, password);
            String session;
            try {
                session = auth.invoke(c);
                if (session.length() > 0) {
                    c.setSession(session);
                }
            } catch (XmlRpcFault ex) {
                return FormValidation.error("Possibly wrong username/password");
            }

            Product.check_product get_command = new Product.check_product();
            get_command.name = product;
            try {
                Object o = c.invoke(get_command);
            } catch (XmlRpcFault ex) {
                Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
                return FormValidation.error("Product possibly doesn't exist");
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
