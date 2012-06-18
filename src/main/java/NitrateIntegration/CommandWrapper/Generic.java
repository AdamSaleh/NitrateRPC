package NitrateIntegration.CommandWrapper;

import NitrateIntegration.TcmsEnvironment;
import NitrateIntegration.TcmsProperties;
import com.redhat.nitrate.TcmsCommand;
import com.redhat.nitrate.TcmsConnection;
import redstone.xmlrpc.XmlRpcArray;
import redstone.xmlrpc.XmlRpcStruct;

/**
 *
 * @author asaleh
 */
public class Generic extends CommandWrapper {

        public Generic(TcmsCommand current, Class result_type,TcmsProperties properties,TcmsEnvironment env) {
            super(current, result_type, properties, env);
        }

        @Override
        public Object getResultIfDuplicate(TcmsConnection connection) {
            return null;
        }

        @Override
        public boolean processDependecies() {
            return true;
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
                        status = CommandWrapper.Status.EXCEPTION;
                        return;
                    } else {
                        result = TcmsConnection.rpcStructToFields((XmlRpcStruct) o, result_type);
                        unexpected = null;
                        return;
                    }

                } else if (o instanceof XmlRpcArray) {
                    XmlRpcArray array = (XmlRpcArray) o;
                    if (array.size() > 0) { // usualy when query shows no results
                        setResult(array.get(0));
                        return;
                    } else {
                        result = null;
                        unexpected = o;
                        status = CommandWrapper.Status.EXCEPTION;
                        return;
                    }

                } else {
                    result = null;
                    unexpected = o;
                    status = CommandWrapper.Status.EXCEPTION;
                    return;
                }

            }
        }
        
        @Override
        public String summary() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }