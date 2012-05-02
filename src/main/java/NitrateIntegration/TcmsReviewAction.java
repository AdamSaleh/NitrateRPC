/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import com.redhat.nitrate.Auth;
import com.redhat.nitrate.TcmsConnection;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Run;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
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
    
    public AbstractBuild getBuild(){
        return build;
    }

    public TcmsReviewAction(AbstractBuild<?, ?> build, TcmsGatherer gatherer, 
            String serverUrl,String username,String password, TcmsProperties properties) {
        this.build = build;
        this.gatherer = gatherer;
        this.properties = properties;
        
        this.username = username;
        this.password = password;
        this.serverUrl = serverUrl;
                
    }

    public void doReportSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException, InterruptedException {
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
            
            TcmsUploader.upload(gatherer, connection);
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(TcmsPublisher.class.getName()).log(Level.SEVERE, null, ex);
        }
       
    }
}
