/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package NitrateIntegration;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

/**
 *
 * @author asaleh
 */
public class TcmsRunListener extends RunListener<Run> {

    /*@Override
    public void onComnpleted(Run r, TaskListener listener){
        if(r instanceof AbstractBuild){
            r.getActions().add(null);
        }
    }*/
}
