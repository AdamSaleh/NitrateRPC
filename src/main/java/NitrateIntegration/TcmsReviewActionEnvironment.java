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
    public final TcmsReviewActionEnvironment.PropertyTransform property = new TcmsReviewActionEnvironment.PropertyTransform();
    private LinkedHashMap<String, Hashtable<String, String>> env_status;
    private boolean wrongProperty;
    private HashSet<String> propertyWWrongValue;
    boolean change_axis = false;
    boolean envVarsChecked = false;

    /*
     * Used to store exception, if occurs, and print it in reasonable format,
     * not ugly long exception. Shown under Update settings and Check
     * Environmental vars
     */
    private String envCheckException;

    public TcmsReviewActionEnvironment() {
        env_status = new LinkedHashMap<String, Hashtable<String, String>>();
        wrongProperty = false;
        propertyWWrongValue = new HashSet<String>();
    }
    
    public boolean isChange_axis() {
        return change_axis;
    }
    
    public HashSet<String> getEnvCheckProblems() {
        return envCheckProblems;
    }
    
    public HashSet<String> getPropertyWWrongValue() {
        return propertyWWrongValue;
    }
        public LinkedHashMap<String, Hashtable<String, String>> getEnv_status() {
        return env_status;
    }

    public boolean existsWrongProperty() {
        return wrongProperty;
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
        envVarsChecked = true;

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
    /**
     * Class that defines transformations (key, value) -> (key, value). This is
     * used in case when user renames Jenkins`s axes to some new names - new
     * transformation from original names and values to new ones is added.
     */
    private class PropertyTransform {

        private class Touple<K, V> implements Map.Entry<K, V> {

            private K key;
            private V val;

            public Touple(K key, V val) {
                this.key = key;
                this.val = val;
            }

            public K getKey() {
                return key;
            }

            public V getValue() {
                return val;
            }

            public Object setValue(Object v) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }

        public void clearTransformations() {
            propertyTransform.clear();
        }

        public void addTransformation(String oldprop, String oldval, String newprop, String newval) {
            propertyTransform.put(new Touple(oldprop, oldval), new Touple(newprop, newval));
        }
        private HashMap<Map.Entry<String, String>, Map.Entry<String, String>> propertyTransform;

        public Map<String, String> transformVariables(Map<String, String> old) {

            HashMap<String, String> transformed = new HashMap<String, String>();

            for (Map.Entry<String, String> prop_value : old.entrySet()) {

                Map.Entry<String, String> newprop_value = prop_value;
                if (propertyTransform.containsKey(prop_value)) {
                    newprop_value = propertyTransform.get(prop_value);
                }


                transformed.put(newprop_value.getKey(), newprop_value.getValue());
            }
            return transformed;
        }
    }
}
