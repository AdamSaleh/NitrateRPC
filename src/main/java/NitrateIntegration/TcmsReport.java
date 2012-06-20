/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import com.redhat.engineering.jenkins.testparser.results.TestResults;
import com.redhat.nitrate.TcmsException;
import hudson.model.AbstractBuild;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jrusnack
 */
public class TcmsReport {
    
    private Set<TestRunResults> testRuns;
    private Map<String, Set<String>> environmentMapping;
    private Set<String> wrongProperties;
    private Set<String> wrongValues;
    
    /**
     * Formerly known as GatherFiles
     * 
     * Stores all important information about one TestRun (in Jenkins`s language
     * one MatrixRun for multiconf project or just build in freestyle project)
     */
    private class TestRunResults{

        public TestResults results;
        public AbstractBuild build;
        /* FIXME: Total MESS !! we can`t store axes and their value as variables
         * or expect they will be only variables present for build. Fetch axes 
         * from multiconf project and work around freestyle project. 
         * 
         * THIS IS TEMPORARY
         */
        public Map<String, String> variables;

        public TestRunResults(TestResults results, AbstractBuild build, Map<String, String> variables) {
            this.results = results;
            this.build = build;
            this.variables = variables;
        }
    }
    
    public TcmsReport(){
        testRuns = new HashSet<TestRunResults>();
        environmentMapping = new HashMap<String, Set<String>>();
        wrongProperties = new HashSet<String>();
        wrongValues = new HashSet<String>();
    }
    
    public Set getProperties(){
        return environmentMapping.keySet();
    }
    
    public Set getValues(String property){
        return environmentMapping.get(property);                
    }
    
    public void changeProperty(String oldProperty, String newProperty) {
        if (environmentMapping.containsKey(oldProperty)) {
            environmentMapping.put(newProperty, environmentMapping.get(oldProperty));
            environmentMapping.remove(oldProperty);
            for (TestRunResults testRunRes : testRuns) {
                testRunRes.variables.put(newProperty, testRunRes.variables.get(oldProperty));
                testRunRes.variables.remove(oldProperty);
            }
        }
    }

    public void changeValue(String property, String oldValue, String newValue) {
        if (environmentMapping.get(property).contains(oldValue)) {
            environmentMapping.get(property).remove(oldValue);
            environmentMapping.get(property).add(newValue);
            for (TestRunResults testRunRes : testRuns) {
                if(testRunRes.variables.containsKey(property) && testRunRes.variables.get(property).equals(oldValue)){                    
                    testRunRes.variables.put(property, newValue);
                }                    
            }
        }
    }
    
    /**
     * Adds one TestRun to report. Formerly known as addGatherPath
     * 
     * @param results
     * @param build
     * @param variables 
     */
    public void addTestRun(TestResults results, AbstractBuild build, Map<String, String> variables){
        testRuns.add(new TestRunResults(results, build, variables));
        for(String property: variables.keySet()){
            if(!environmentMapping.containsKey(property)){
                environmentMapping.put(property, new HashSet<String>());
                environmentMapping.get(property).add(variables.get(property));
            } else {
                if(!environmentMapping.get(property).contains(variables.get(property))){
                    environmentMapping.get(property).add(variables.get(property));
                }
            }
        }
    }    
   
    public void checkEnvironmentMapping(TcmsEnvironment environment) throws TcmsException{        
        environment.fetchAvailableProperties();
        
        for(String property : environmentMapping.keySet()){
            if(environment.containsProperty(property)){
                /* When property is OK, check its values */
                environment.reloadProperty(property);                
                for(String value : environmentMapping.get(property)) {
                    if (!environment.containsValue(property, value)) {
                        wrongValues.add(value);
                    }
                }
            } else {
                wrongProperties.add(property);
            }
        }
    }
    
    public boolean existsWrongProperty(){
        return !wrongProperties.isEmpty();
    }
    
    public boolean existWrongValue(){
        return !wrongValues.isEmpty();
    }
    
    public Set getWrongProperties(){
        return wrongProperties;
    }
    
    public Set getWrongValues(){
        return wrongValues;
    }
    
}
