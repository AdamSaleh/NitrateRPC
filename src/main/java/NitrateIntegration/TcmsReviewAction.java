/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package NitrateIntegration;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Run;

/**
 *
 * @author asaleh
 */
public class TcmsReviewAction implements Action {
  public final AbstractBuild<?, ?>  build;
  public final TcmsGatherer gatherer;

  public String getIconFileName() {
	return Definitions.__ICON_FILE_NAME;
    }

    public String getDisplayName() {
	return Definitions.__DISPLAY_NAME;
    }

    public String getUrlName() {
	return Definitions.__URL_NAME;
    }

    public String getPrefix() {
	return Definitions.__PREFIX;
    }

    public TcmsReviewAction(AbstractBuild<?, ?>  build,TcmsGatherer gatherer) {
        this.build = build;
        this.gatherer = gatherer;
    }

}
