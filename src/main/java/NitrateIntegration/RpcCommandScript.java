/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import com.redhat.nitrate.TcmsCommand;
import com.redhat.nitrate.TcmsConnection;
import com.redhat.nitrate.TestCase;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.XmlRpcStruct;

/**
 *
 * @author asaleh
 */
/*
 * Yay, dependencies list! Way to shoot yourself to the leg :D
 */
public class RpcCommandScript {

    public TcmsCommand current;
    private boolean performed;
    private boolean completed;
    private LinkedList<RpcCommandScript> dependecy;
    private Object result;
    private Object unexpected;
    private Class result_type;

    public RpcCommandScript(TcmsCommand current, RpcCommandScript dependecy, Class result_type) {
        this.current = current;

        this.performed = false;
        this.completed = false;
        this.dependecy = new LinkedList<RpcCommandScript>();
        this.result_type = result_type;
        if (dependecy != null) {
            this.dependecy.push(dependecy);
        }

        result = null;
    }

    public LinkedList<RpcCommandScript> getDependecies() {
        return dependecy;
    }

    public void addDependecy(RpcCommandScript dep) {
        dependecy.push(dep);
    }

    public boolean resolved() {
        if (dependecy.size() == 0) {
            return true;
        }
        for (RpcCommandScript s : dependecy) {
            if (s.completed() == false) {
                return false;
            }
        }
        return true;
    }

    public boolean completed() {
        return completed;
    }

    public RpcCommandScript(TcmsCommand current, Class result_type) {
        this(current, null, result_type);
    }

    TcmsCommand current() {
        return current;
    }

    public void setResult(Object o) {
        if (this.result == null) {
            Object r = null;
            if (result_type.isInstance(o)) {
                result = o;
                unexpected = null;
                return;
            } else if (o instanceof XmlRpcStruct) {
                XmlRpcStruct struct = (XmlRpcStruct) o;
                if (struct.containsKey("args")) { // usualy when query shows no results
                    result = null;
                    unexpected = o;
                    return;
                } else {
                    result = TcmsConnection.rpcStructToFields((XmlRpcStruct) o, result_type);
                    unexpected = null;
                    return;
                }

            } else {
                result = null;
                unexpected = o;
                return;
            }

        }
    }

    public void perform(TcmsConnection connection) throws XmlRpcFault {
        Object o = connection.invoke(current());
        setResult(o);
    }

    public <T extends Object> T getResult(Class<T> c) {
        return c.cast(result);
    }

    public Object getUnexpected() {
        return unexpected;
    }

    public void setPerforming() {
        this.performed = true;
    }

    public boolean performed() {
        return performed;
    }

    public void setCompleted() {
        this.completed = true;
    }
}
