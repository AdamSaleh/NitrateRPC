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
    public final TcmsReport.PropertyTransform property = new TcmsReport.PropertyTransform();

    
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
        if(!environment.isEmpty()){
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
