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
    private Set<String> wrongEnvProperties;
    private Set<String> wrongEnvValues;
    private Set<String> envPropertiesWithWrongValues;
    
    /**
     * Formerly known as GatherFiles
     * 
     * Stores all important information about one TestRun (in Jenkins`s language
     * one MatrixRun for multiconf project or just build in freestyle project)
     */
    public static class TestRunResults{

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
        wrongEnvProperties = new HashSet<String>();
        wrongEnvValues = new HashSet<String>();
        envPropertiesWithWrongValues = new HashSet<String>();
    }
    
    public Set<String> getEnvProperties(){
        return environmentMapping.keySet();
    }
    
    public Set<String> getEnvValues(String property){
        return environmentMapping.get(property);                
    }
    
    public void changeEnvProperty(String oldProperty, String newProperty) {
        if (!environmentMapping.containsKey(oldProperty)) {
            throw new IllegalArgumentException("Nonexistent property");
        }

        if (environmentMapping.containsKey(newProperty)) {
            throw new IllegalArgumentException("Duplicate property " + newProperty);
        }

        environmentMapping.put(newProperty, environmentMapping.get(oldProperty));
        environmentMapping.remove(oldProperty);
        for (TestRunResults testRunRes : testRuns) {
            testRunRes.variables.put(newProperty, testRunRes.variables.get(oldProperty));
            testRunRes.variables.remove(oldProperty);
        }

    }

    public void changeEnvValue(String envProperty, String oldValue, String newValue) {
        if (!environmentMapping.containsKey(envProperty)) {
            throw new IllegalArgumentException("Nonexistent property");
        }

        if (!environmentMapping.get(envProperty).contains(oldValue)) {
            throw new IllegalArgumentException("Nonexistent value");
        }

        if (environmentMapping.get(envProperty).contains(newValue)) {
            throw new IllegalArgumentException("Duplicate value " + newValue);
        }

        environmentMapping.get(envProperty).remove(oldValue);
        environmentMapping.get(envProperty).add(newValue);
        for (TestRunResults testRunRes : testRuns) {
            if (testRunRes.variables.containsKey(envProperty) && testRunRes.variables.get(envProperty).equals(oldValue)) {
                testRunRes.variables.put(envProperty, newValue);
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
        for(String envProperty: variables.keySet()){
            if(!environmentMapping.containsKey(envProperty)){
                environmentMapping.put(envProperty, new HashSet<String>());
                environmentMapping.get(envProperty).add(variables.get(envProperty));
            } else {
                if(!environmentMapping.get(envProperty).contains(variables.get(envProperty))){
                    environmentMapping.get(envProperty).add(variables.get(envProperty));
                }
            }
        }
    }    
   
    public void checkEnvironmentMapping(TcmsEnvironment environment) throws TcmsException{          
        environment.fetchAvailableProperties();
        
        for(String envProperty : environmentMapping.keySet()){
            if(environment.containsProperty(envProperty)){
                /* When property is OK, check its values */
                environment.reloadProperty(envProperty);                
                for(String value : environmentMapping.get(envProperty)) {
                    if (!environment.containsValue(envProperty, value)) {
                        wrongEnvValues.add(value);
                        envPropertiesWithWrongValues.add(envProperty);
                    }
                }
            } else {
                wrongEnvProperties.add(envProperty);
            }
        }
    }
    
    public boolean existsWrongEnvProperty(){
        return !wrongEnvProperties.isEmpty();
    }
    
    public boolean existWrongEnvValue(){
        return !wrongEnvValues.isEmpty();
    }
    
    public boolean isWrongEnvProperty(String envProperty){
        return wrongEnvProperties.contains(envProperty);
    }
    
    
    public boolean isWrongEnvValue(String envProperty){
        return wrongEnvValues.contains(envProperty);
    }
    
    public Set getWrongEnvProperties(){
        return wrongEnvProperties;
    }
    
    public Set getWrongEnvValues(){
        return wrongEnvValues;
    }
    
    public Set getEnvPropertiesWithWrongValues(){
        return envPropertiesWithWrongValues;
    }
    
    public Set<TestRunResults> getTestRuns(){
        return testRuns;
    }
    
    
    
}
