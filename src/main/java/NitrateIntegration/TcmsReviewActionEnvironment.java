/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import com.redhat.nitrate.TcmsException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author asaleh
 */
public class TcmsReviewActionEnvironment {

    public HashSet<String> envCheckProblems = new HashSet<String>();
    boolean change_axis = false;
   
    /*
     * Used to store exception, if occurs, and print it in reasonable format,
     * not ugly long exception. Shown under Update settings and Check
     * Environmental vars
     */
    private String envCheckException;

    public TcmsReviewActionEnvironment() {
    }
    
    public boolean isChange_axis() {
        return change_axis;
    }
    
    public HashSet<String> getEnvCheckProblems() {
        return envCheckProblems;
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
    
     public void doCheckSubmit(StaplerRequest req, StaplerResponse rsp,TcmsReport report,TcmsReviewActionSettings settings) throws IOException  {
        HashSet<String> problems = new HashSet<String>();
        envCheckException = "";
        change_axis = false;
        
        if (req.getParameter("Submit").equals("Change")) {
            change_axis = true;
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        }

        problems = report.updateReportFromReq(req.getParameterMap());
        
        try {
            settings.getConnectionAndUpdate();

            report.checkEnvironmentMapping(settings.getEnvironment());
        } catch (TcmsException ex){
            envCheckException = ex.getMessage();
            rsp.sendRedirect("../" + Definitions.__URL_NAME);
            return;
        }

        this.envCheckProblems = problems;
  
        rsp.sendRedirect("../" + Definitions.__URL_NAME);
    }
    
     
   
}
