/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package NitrateIntegration;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

/**
 *
 * @author asaleh
 */
@Extension
public class TcmsRunListener extends RunListener<Run> {
  
  /* @Override
   public void onCompleted(Run r, TaskListener listener) {
        if(r instanceof AbstractBuild){
            AbstractBuild b = (AbstractBuild)r;
            r.getActions().add(new TcmsReviewAction(b));
        }
    }*/
}
