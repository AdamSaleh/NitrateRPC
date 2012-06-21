/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import com.redhat.nitrate.TcmsException;
import java.io.IOException;
import java.util.*;
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

        problems = updateReportFromReq(req,report);
        
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
    
      private HashSet<String> updateReportFromReq(StaplerRequest req,TcmsReport report){
        Map params = req.getParameterMap();
        HashSet<String> problems = new HashSet<String>();
        
        /*
         * update values first
         */
        for (Iterator it = params.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) it.next();
            
            if (entry.getKey().startsWith("value-")) {
                String newValue = ((String[]) entry.getValue())[0];
                String property = entry.getKey().replaceFirst("value-", "");
                String oldValue = property.split("=>")[1];
                property = property.split("=>")[0];

                if (!oldValue.equals(newValue)) {
                    try {
                        report.changeEnvValue(property, oldValue, newValue);
                    } catch (IllegalArgumentException ex) {
                        problems.add(ex.getMessage());
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
                String newProperty = ((String[]) entry.getValue())[0];
                String oldProperty = entry.getKey().replaceFirst("property-", "");
                
                if (!oldProperty.equals(newProperty)) {
                    try {
                        report.changeEnvProperty(oldProperty, newProperty);
                    } catch (IllegalArgumentException ex) {
                        problems.add(ex.getMessage());
                    }
                }
            }
        }
        
        return problems;
    }
   
}
