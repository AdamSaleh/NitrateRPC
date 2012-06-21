/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import com.redhat.nitrate.TcmsAccessCredentials;
import com.redhat.nitrate.TcmsConnection;
import com.redhat.nitrate.TcmsException;
import hudson.model.AbstractBuild;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author asaleh
 */
public class TcmsReviewActionSettings {
    
    public List<String> update_problems = new LinkedList<String>();
    boolean setting_updated = false;
    private String updateException;
    
    String serverUrl;
    private TcmsAccessCredentials credentials;
    public TcmsProperties properties;
    public TcmsEnvironment environment;
    
   

    public TcmsReviewActionSettings(String serverUrl,
            String plan,
            String product,
            String product_v,
            String category,
            String priority,
            String manager,
            String env,
            String testPath) {
        
            this.serverUrl = serverUrl;

        
            this.properties = new TcmsProperties(plan, product, product_v, category, priority, manager);
            this.environment = new TcmsEnvironment(env);
            this.credentials = new TcmsAccessCredentials();
            
       
    }

    public TcmsEnvironment getEnvironment() {
        return environment;
    }

    public TcmsAccessCredentials getCredentials() {
        return credentials;
    }
     public String getUsername() {
        return credentials.getUsername();
    }

    public String getPassword() {
        return credentials.getPassword();
    }
   public void updateCredentialsFromRequest(StaplerRequest req){
        if(req.hasParameter("_.username")){
            String username = req.getParameter("_.username");
            credentials.setUsername(username);
        }
        if(req.hasParameter("_.password")){
            String password = req.getParameter("_.password");
            credentials.setPassword(password);
        }
    }

    public TcmsProperties getProperties() {
        return properties;
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
    
    public String getUpdateException() {
        return updateException;
    }

    public void setUpdateException(String updateException) {
        this.updateException = updateException;
    }
    public void clearUpdateException() {
        this.updateException = "";
    }
    

    
    public boolean updateExceptionOccured() {
        if (updateException == null) {
            return false;
        }
        return !updateException.isEmpty();
    }
    public TcmsConnection getConnection() throws TcmsException {
        return TcmsConnection.connect(getServerUrl(), getCredentials());
    }
    public TcmsConnection getConnectionAndUpdate() throws TcmsException {
       TcmsConnection connection = null;
       connection = getConnection();
       properties.setConnection(connection);
       environment.setConnection(connection);
       return connection;
    }
    
    // FIXME: javadoc
    public void doUpdateSettings(StaplerRequest req, StaplerResponse rsp,TcmsReport report) throws IOException {
        this.update_problems.clear();
        setting_updated = false;
        List<String> problems = new LinkedList<String>();
        updateException = "";

        credentials = parseCredentialsFromRequest(req);
        TcmsEnvironment environment = parseEnvironmentFromRequest(req);
        TcmsProperties properties = parsePropertiesFromRequest(req);

        try {
            TcmsConnection connection = TcmsConnection.connect(serverUrl, credentials);

            environment.setConnection(connection);
            if (!this.environment.env.equals(environment.env)) {
                environment.reloadEnvId();
            }
            
            if(!environment.isEmpty()){
                report.checkEnvironmentMapping(environment);
            }

            properties.setConnection(connection);
            properties.reload();

            problems = TcmsProperties.checkUsersetProperties(properties);

            if (!environment.env.isEmpty() && environment.getEnvId() == null) {
                problems.add("Possibly wrong environment group: " + environment.env);
            }

            if (problems.isEmpty()) {
                this.properties = properties;
                this.environment = environment;
                setting_updated = true;
            }

            this.update_problems = problems;

        } catch (TcmsException ex) {
            Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
            updateException = ex.getMessage();
        } finally {
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        }

    }  
    
    public static TcmsAccessCredentials parseCredentialsFromRequest(StaplerRequest req){
        String username = req.getParameter("_.username");
        String password = req.getParameter("_.password");
        String serverUrl = req.getParameter("_.serverUrl");
        return new TcmsAccessCredentials(serverUrl, username, password);
    }
    
    public static TcmsProperties parsePropertiesFromRequest(StaplerRequest req){
        String plan = req.getParameter("_.plan");
        String product = req.getParameter("_.product");
        String product_v = req.getParameter("_.product_v");
        String category = req.getParameter("_.category");
        String priority = req.getParameter("_.priority");
        String manager = req.getParameter("_.manager");
        return new TcmsProperties(plan, product, product_v, category, priority, manager);
    }
    
    public static TcmsEnvironment parseEnvironmentFromRequest(StaplerRequest req){
        String env = req.getParameter("_.environment");
        return new TcmsEnvironment(env);    
    }
}
