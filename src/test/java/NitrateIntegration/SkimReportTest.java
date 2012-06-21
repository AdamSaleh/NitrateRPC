/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.regex.Pattern;

 /**
* To run this test, you should run project beforehand and keep it running
* (this selenium doesn`t run project, it assumes it is running and
* connects to localhost:8080/, so e.g. in Netbeans hit Run project and afterwards test
* project).
*
* Also, you need create a job before testing, with following parameters:
*
* Name: selenium_test, multiconfiguration, check This build is parametrized
* and enter file location test.xml, add one Axis Arch and values i386 and
* x86_64, check Integration with Nitrate TCMS and fill values.
*
* This setting (parametrized build) will require file before building, so
* make sure you have testng xml file to upload and that path is correct (
* see line driver.findElement(By.name("file")).sendKeys("") below).
*
* Don`t forget to create Java class PrivatePassword with your username
* and password - they are obviously not part of repo.
* 
*
* @throws Exception
*/
public class SkimReportTest {
    
    public SkimReportTest() {
    }
        @Test
	public void test(){
        }
/*
    
	@Before
	public void setUp() throws Exception {
		selenium = new DefaultSelenium("localhost", 4444, "*chrome", "http://localhost:8080/");
		selenium.start();
                
           
        }

        public void init() throws Exception{
             selenium.open("job/selenium_test/");
		selenium.click("link=Build Now");
		selenium.waitForPageToLoad("30000");
		selenium.type("name=file", PrivatePassword.testFile);
		selenium.click("css=button[type=\"button\"]");
		selenium.waitForPageToLoad("30000");
                
                selenium.open("job/selenium_test/lastBuild/");
		
		selenium.waitForPageToLoad("30000");
		selenium.click("link=Nitrate Plugin");
		selenium.waitForPageToLoad("30000");
		selenium.type("name=_.username", PrivatePassword.name);
		selenium.type("name=_.password", PrivatePassword.password);
		selenium.click("css=button[type=\"button\"]");
		selenium.waitForPageToLoad("30000");
        }
        
	
        @Test
	public void testSkim_report() throws Exception {
                            init();
		selenium.open("/job/selenium_test/9/nitrate-plugin/");
		verifyTrue(selenium.isTextPresent("TCMS (243)"));
		verifyTrue(selenium.isTextPresent("katello-tests.org-tests.Verify proper error message when invalid org name is used"));
		verifyTrue(selenium.isTextPresent("katello-tests.provider-tests.redhat-content-provider-tests.Upload the same manifest to an org using force"));
		selenium.click("id=details-Exp-1415138589");
		selenium.type("name=buildName", "123 build new");
		selenium.click("xpath=(//input[@name='Submit'])[5]");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("123 build new TCMS (243)"));
		selenium.click("id=details-Exp-1324750798");
		verifyTrue(selenium.isTextPresent("is_automated	1"));
		verifyTrue(selenium.isTextPresent("P1 (1)"));
		verifyTrue(selenium.isTextPresent("Functional (182)"));
		verifyTrue(selenium.isTextPresent("TCMS (243)"));
		verifyTrue(selenium.isTextPresent("\" ]\" :name-must-not-contain-characters"));
		verifyTrue(selenium.isTextPresent("5866"));
		selenium.click("id=details-Exp-590775724");
		verifyTrue(selenium.isTextPresent("Devel (885)"));
		verifyTrue(selenium.isTextPresent("manager	asaleh"));
		selenium.click("id=subitems-Exp-590775724");
		verifyTrue(selenium.isTextPresent("Link Run to Environmental Variable	ia64 (75)"));
		selenium.click("id=details-Exp-1610121531");
		verifyTrue(selenium.isTextPresent("run_id	-1"));
		verifyTrue(selenium.isTextPresent("env_value_id	ia64 (75)"));
		verifyTrue(selenium.isTextPresent("Create Test Case Run	katello-tests.org-tests.Verify proper error message when invalid org name is used (-1) FAILED"));
		selenium.click("id=details-Exp-438978257");
		verifyTrue(selenium.isTextPresent("java.lang.AssertionError Verification failed: (pred results) results : {:type :success, :msg \"×Repository 'testrepo-1339059183934' finished syncing successfully.\"} pred : (katello.validation/expect-error :name-must-not-contain-characters) verify.clj:23 tools.verify/check AFn.java:172 clojure.lang.AFn.applyToHelper AFn.java:151 clojure.lang.AFn.applyTo core.clj:601 clojure.core/apply trace.clj:44 fn.trace/trace-fn-call[fn] trace.clj:43 fn.trace/trace-fn-call trace.clj:68 fn.trace/rebind-map[fn] RestFn.java:457 clojure.lang.RestFn.invoke validation.clj:67 katello.validation/field-validation organizations.clj:36 katello.tests.organizations/verify-bad-org-name-gives-expected-error core.clj:2317 clojure.core/juxt[fn] AFn.java:163 clojure.lang.AFn.applyToHelper RestFn.java:132 clojure.lang.RestFn.applyTo AFunction.java:29 clojure.lang.AFunction$1.doInvoke RestFn.java:137 clojure.lang.RestFn.applyTo core.clj:601 clojure.core/apply tree.clj:31 test.tree/wrap-data-driven[fn] AFn.java:159 clojure.lang.AFn.applyToHelper AFn.java:151 clojure.lang.AFn.applyTo AFunction.java:29 clojure.lang.AFunction$1.doInvoke RestFn.java:397 clojure.lang.RestFn.invoke tree.clj:20 test.tree/execute jenkins.clj:16 test.tree.jenkins/wrap-tracing[fn] tree.clj:43 test.tree/wrap-blockers[fn] tree.clj:50 test.tree/wrap-timer[fn] tree.clj:32 test.tree/wrap-data-driven[fn] tree.clj:78 test.tree/run-test[fn] tree.clj:77 test.tree/run-test tree.clj:109 test.tree/queue[fn] tree.clj:95 test.tree/consume setup.clj:70 katello.setup/thread-runner[fn] AFn.java:24 clojure.lang.AFn.run Thread.java:636 java.lang.Thread.run\n\n{:trace\n (katello.ui-tasks/navigate :top-level)\n   (com.redhat.qe.auto.selenium.selenium/call-sel \"isElementPresent\" :log-out)\n     true\n   (com.redhat.qe.auto.selenium.selenium/call-sel \"isElementPresent\" :confirmation-dialog)\n     false\n   nil\n (katello.ui-tasks/create-organization \"   ]\")\n   (katello.ui-tasks/navigate :new-organization-page)\n     (com.redhat.qe.auto.selenium.selenium/call-sel \"isElementPresent\" :log-out)\n       true\n     (com.redhat.qe.auto.selenium.selenium/call-sel \"isElementPresent\" :confirmation-dialog)\n       false\n     (com.redhat.qe.auto.selenium.selenium/call-sel \"clickAndWait\" :organizations)\n       nil\n     (com.redhat.qe.auto.selenium.selenium/call-sel \"click\" :new-organization)\n       nil\n     nil\n   (katello.ui-tasks/fill-ajax-form\n     {:org-name-text \"   ]\", :org-description-text nil, :org-initial-env-name-text nil, :org-initial-env-desc-text nil}\n     :create-organization)\n     (com.redhat.qe.auto.selenium.selenium/call-sel \"getElementType\" :org-name-text)\n       \"textbox\"\n     (com.redhat.qe.auto.selenium.selenium/call-sel \"setText\" :org-name-text \"   ]\")\n       nil\n     (com.redhat.qe.auto.selenium.selenium/call-sel \"click\" :create-organization)\n       nil\n     ([:org-name-text \"   ]\"])\n   (katello.ui-tasks/check-for-success)\n     (katello.ui-tasks/notification nil)\n       (com.redhat.qe.auto.selenium.selenium/call-sel \"waitForElement\" :notification \"15000\")\n         nil\n       (com.redhat.qe.auto.selenium.selenium/call-sel \"getText\" :notification)\n         \"×Repository 'testrepo-1339059183934' finished syncing successfully.\"\n       (com.redhat.qe.auto.selenium.selenium/call-sel \"getAttributes\" :notification)\n         #\n       (katello.ui-tasks/clear-all-notifications)\n         (com.redhat.qe.auto.selenium.selenium/call-sel\n           \"isElementPresent\"\n           #=(com.redhat.qe.auto.selenium.Element. \"xpath=(//div[contains(@class,'jnotify-notification-error')]//a[@class='jnotify-close'])[1]\"))\n           true\n         (com.redhat.qe.auto.selenium.selenium/call-sel\n           \"click\"\n           #=(com.redhat.qe.auto.selenium.Element. \"xpath=(//div[contains(@class,'jnotify-notification-error')]//a[@class='jnotify-close'])[1]\"))\n           nil\n         (com.redhat.qe.auto.selenium.selenium/call-sel\n           \"isElementPresent\"\n           #=(com.redhat.qe.auto.selenium.Element. \"xpath=(//div[contains(@class,'jnotify-notification-error')]//a[@class='jnotify-close'])[2]\"))\n           false\n         nil\n       {:type :success, :msg \"×Repository 'testrepo-1339059183934' finished syncing successfully.\"}\n     (katello.ui-tasks/success? {:type :success, :msg \"×Repository 'testrepo-1339059183934' finished syncing successfully.\"})\n       true\n     {:type :success, :msg \"×Repository 'testrepo-1339059183934' finished syncing successfully.\"}\n   {:type :success, :msg \"×Repository 'testrepo-1339059183934' finished syncing successfully.\"}\n (tools.verify/check\n   nil\n   (pred results)\n   {results {:type :success, :msg \"×Repository 'testrepo-1339059183934' finished syncing successfully.\"},\n    pred (katello.validation/expect-error :name-must-not-contain-characters)}\n   nil)\n   #=(java.lang.AssertionError. \"Verification failed: (pred results)\\n\\tresults : {:type :success, :msg \\\"×Repository 'testrepo-1339059183934' finished syncing successfully.\\\"}\\n\\tpred : (katello.validation/expect-error :name-must-not-contain-characters)\\n\")\n ,\n :object\n #=(java.lang.AssertionError. \"Verification failed: (pred results)\\n\\tresults : {:type :success, :msg \\\"×Repository 'testrepo-1339059183934' finished syncing successfully.\\\"}\\n\\tpred : (katello.validation/expect-error :name-must-not-contain-characters)\\n\"),\n :message\n \"Verification failed: (pred results)\\n\\tresults : {:type :success, :msg \\\"×Repository 'testrepo-1339059183934' finished syncing successfully.\\\"}\\n\\tpred : (katello.validation/expect-error :name-must-not-contain-characters)\\n\",\n :cause nil}"));
	}

	@After
	public void tearDown() throws Exception {
		selenium.stop();
	}
*/
}
