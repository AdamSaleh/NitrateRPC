/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration.CommandWrapper;

import NitrateIntegration.TcmsEnvironment;
import NitrateIntegration.TcmsProperties;
import com.redhat.nitrate.TcmsCommand;
import com.redhat.nitrate.TcmsConnection;
import com.redhat.nitrate.command.Build;
import java.util.Hashtable;

/**
 *
 * @author asaleh
 */
public class BuildCreate extends CommandWrapper {

    static{
        CommandWrapper.enlistWrapper(Build.create.class, new WrapperConstructor() {

            public CommandWrapper create(TcmsCommand current, Class result_type,TcmsProperties properties,TcmsEnvironment env) {
                return new BuildCreate(current, result_type, properties, env);
            }
        });
    }

    public BuildCreate(TcmsCommand current, Class result_type,TcmsProperties properties,TcmsEnvironment env) {
        super(current, result_type, properties, env);
    }

    @Override
    public Object getResultIfDuplicate(TcmsConnection connection) {
        Build.check_build f = new Build.check_build();
        Build.create comand = (Build.create) current;
        f.name = comand.name;
        f.productid = comand.product;

        CommandWrapper script = new Generic(f, Build.class, properties, env);
        script.perform(connection);
        return script.getResult(Build.class);
    }

    @Override
    public boolean processDependecies() {
        return true;
    }

    
     public Hashtable<String, String> description() { 
      Hashtable<String,String> map = current.descriptionMap(); 
      map.put("product",properties.product + " (" + map.get("product") + ")"); 
      return map; 
     }
     
     public String summary() { return description().get("name") + " " +
      description().get("product");
     }
     
    public String toString() {
        return "Create Build";
    }
}