/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package NitrateIntegration;

import com.redhat.nitrate.TcmsConnection;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Run;
import java.io.IOException;
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
  public final AbstractBuild<?, ?>  build;
  public final TcmsGatherer gatherer;
  public final TcmsConnection connection;

  
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
    
    public TcmsReviewAction(AbstractBuild<?, ?>  build,TcmsGatherer gatherer,TcmsConnection connection) {
        this.build = build;
        this.gatherer = gatherer;
        this.connection = connection;
    }

    public void doReportSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException, InterruptedException {
        try {
            TcmsUploader.upload(gatherer,connection);
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsReviewAction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
  
}
