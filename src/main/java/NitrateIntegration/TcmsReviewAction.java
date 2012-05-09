/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import NitrateIntegration.TcmsReviewAction.GatherFiles;
import com.redhat.nitrate.Auth;
import com.redhat.nitrate.TcmsCommand;
import com.redhat.nitrate.TcmsConnection;
import hudson.Extension;
import hudson.FilePath;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.FormValidation;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import redstone.xmlrpc.XmlRpcFault;

/**
 *
 * @author asaleh
 */
public class TcmsReviewAction implements Action {

    public AbstractBuild<?, ?> build;
    public TcmsGatherer gatherer;
    private TcmsConnection connection;
    String serverUrl;
    String username;
    String password;
    public final TcmsProperties properties;
    public final TcmsEnvironment environment;
    private LinkedList<EnvStatus> env_status;

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

    public LinkedList<EnvStatus> getEnv_status() {
        return env_status;
    }

    public TcmsEnvironment getEnvironment() {
        return environment;
    }

    public AbstractBuild getBuild() {
        return build;
    }

    @DataBoundConstructor
    public TcmsReviewAction(AbstractBuild build, String serverUrl, String username, String password,
            String plan,
            String product,
            String product_v,
            String category,
            String priority,
            String manager,
            String env,
            String testPath) {
        //this.build = this.;
        this.gatherer = gatherer;
        this.properties = new TcmsProperties(plan, product, product_v, category, priority, manager);
        this.username = username;
        this.password = password;
        this.serverUrl = serverUrl;
        this.environment = new TcmsEnvironment(env);
        this.build = build;
        env_status = new LinkedList<EnvStatus>();
    }

    public void doGather(StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException, InterruptedException {
        rsp.sendRedirect("../" + Definitions.__URL_NAME);
        gatherer = new TcmsGatherer(properties);

        try {
                connection = new TcmsConnection(serverUrl);
                connection.setUsernameAndPassword(username, password);
                Auth.login_krbv auth = new Auth.login_krbv();
                String session;
                session = auth.invoke(connection);
                if (session.length() > 0) {
                    connection.setSession(session);
                }
                environment.setConnection(connection);
                environment.reloadEnvId();
                
                properties.setConnection(connection);
                properties.reload();

            } catch (XmlRpcFault ex) {
                Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MalformedURLException ex) {
                Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
            }
       
        for (GatherFiles gatherfile : gatherFiles) {
            gatherer.gather(gatherfile.paths, build, gatherfile.build);
        }
    }

    public static class GatherFiles {

        public FilePath[] paths;
        public AbstractBuild build;

        public GatherFiles(FilePath[] paths, AbstractBuild build) {
            this.paths = paths;
            this.build = build;
        }
    }
    LinkedList<GatherFiles> gatherFiles = new LinkedList<GatherFiles>();

    public void clearGatherPaths() {
        gatherFiles.clear();
    }

    public void addGatherPath(FilePath[] paths, AbstractBuild build) {
        gatherFiles.add(new GatherFiles(paths, build));
    }

    public void doCheckSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException, InterruptedException {
        if (build instanceof MatrixBuild) {
            MatrixBuild mb = (MatrixBuild) build;
            AxisList al = mb.getParent().getAxes();

            try {
                connection = new TcmsConnection(serverUrl);
                connection.setUsernameAndPassword(username, password);
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
            } catch (MalformedURLException ex) {
                Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
            }
            env_status.clear();
            for (Axis ax : al) {
                String name = ax.getName();
                for (String val : ax.getValues()) {
                    /*
                     * check value
                     */
                    if (environment.containsProperty(name)) {
                        if (environment.containsValue(name, val)) {
                            env_status.add(new EnvStatus(name, val, "CHECKED"));
                        } else {
                            env_status.add(new EnvStatus(name, val, "VALUE"));
                        }
                    } else {
                        env_status.add(new EnvStatus(name, val, "PROPERTY"));
                    }
                }
            }
        }
        rsp.sendRedirect("../" + Definitions.__URL_NAME);
    }

    public void doReportSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException, InterruptedException {
        
        // parse 
        String input = null;
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

        try {
            connection = new TcmsConnection(serverUrl);
            connection.setUsernameAndPassword(username, password);
            Auth.login_krbv auth = new Auth.login_krbv();
            String session;
            session = auth.invoke(connection);
            if (session.length() > 0) {
                connection.setSession(session);
            }
            properties.setConnection(connection);
            properties.reload();

            upload(gatherer, connection);
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
        }
        rsp.sendRedirect("../" + Definitions.__URL_NAME);


    }

    public void upload(TcmsGatherer gathered, TcmsConnection connection) throws XmlRpcFault {
        boolean at_least_one;
        boolean at_least_one_not_duplicate;
        do {
            at_least_one = false;
            at_least_one_not_duplicate = false;
            for (CommandWrapper command : gathered) {
                if (command.isExecutable()) {
                    if (command.resolved() && command.performed() == false) {
                        boolean tmp = command.perform(connection);
                        if (tmp) {
                            at_least_one = true;
                        }
                        if (command.duplicate() == false) {
                            at_least_one_not_duplicate = true;
                        }
                    }
                }
            }
        } while (at_least_one && at_least_one_not_duplicate);
    }

    public static class EnvStatus {

        private final String property;
        private final String value;
        private final String status;

        public EnvStatus(String property, String value, String status) {
            this.property = property;
            this.value = value;
            this.status = status;
        }

        public String getProperty() {
            return property;
        }

        public String getStatus() {
            return status;
        }

        public String getValue() {
            return value;
        }
    }
}
