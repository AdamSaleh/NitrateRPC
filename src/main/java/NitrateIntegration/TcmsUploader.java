/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import NitrateIntegration.CommandWrapper;
import com.redhat.nitrate.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import redstone.xmlrpc.*;

/**
 *
 * @author asaleh
 */
public class TcmsUploader {
 
  
 
    public static void upload(TcmsGatherer gathered, TcmsConnection connection) throws XmlRpcFault {
        boolean at_least_one = true;

        while (at_least_one) {
            at_least_one = false;
            for (CommandWrapper command : gathered) {
                if (command.resolved() && command.performed() ==false) {
                        at_least_one = command.perform(connection);
                }
            }
        }
    }
}
