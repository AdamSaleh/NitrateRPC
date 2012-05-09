/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import com.redhat.nitrate.Auth;
import com.redhat.nitrate.TcmsConnection;
import hudson.Extension;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import redstone.xmlrpc.XmlRpcFault;

/**
 *
 * @author asaleh
 */
public class TcmsReviewAction implements Action {

    public final AbstractBuild<?, ?> build;
    public final TcmsGatherer gatherer;
    private TcmsConnection connection;
    String serverUrl;
    String username;
    String password;
    public final TcmsProperties properties;

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

    public AbstractBuild getBuild() {
        return build;
    }

    public TcmsReviewAction(AbstractBuild<?, ?> build, TcmsGatherer gatherer,
            String serverUrl, String username, String password, TcmsProperties properties) {
        this.build = build;
        this.gatherer = gatherer;
        this.properties = properties;
        this.username = username;
        this.password = password;
        this.serverUrl = serverUrl;
    }
    public void doCheckSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException, InterruptedException {
        if(build instanceof MatrixBuild){
            MatrixBuild mb = (MatrixBuild) build;
            AxisList al = mb.getParent().getAxes();
            for(Axis ax:al ){
                String name =ax.getName();
                /*test name*/
                
                /*test variables*/
                for(String val:ax.getValues()){
                    /*check value*/
                }
                
            }
        }
    }

    public void doReportSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException, InterruptedException {
        // parse submitted configuration matrix
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

}
