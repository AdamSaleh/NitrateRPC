/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import com.thoughtworks.selenium.*;
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
public class CheckEnvTest extends SeleneseTestCase{
    
    public CheckEnvTest() {
    }

    
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
	public void testTest_check_env() throws Exception {
                            init();

		selenium.click("css=form[name=\"envCheck\"] > table > tbody > tr > td.setting-main > input[name=\"Submit\"]");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("i386	Property checked"));
		verifyTrue(selenium.isTextPresent("ia64	Property checked"));
		selenium.click("xpath=(//input[@name='Submit'])[3]");
		selenium.waitForPageToLoad("30000");
		selenium.type("name=value-Arch=>i386", "iasd");
		selenium.click("css=form[name=\"envCheck\"] > table > tbody > tr > td.setting-main > input[name=\"Submit\"]");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("Value is not linked with Arch."));
		verifyTrue(selenium.isTextPresent("Available values for property Arch:"));
		selenium.type("name=value-Arch=>iasd", "i686");
		selenium.click("xpath=(//input[@name='Submit'])[3]");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("i686	Property checked"));
		selenium.type("name=property-Arch", "asdf");
		selenium.click("css=form[name=\"envCheck\"] > table > tbody > tr > td.setting-main > input[name=\"Submit\"]");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("Property is not linked to RTT."));
		verifyTrue(selenium.isTextPresent("Available properties for group RTT:"));
		selenium.type("name=property-asdf", "Arch");
		selenium.click("css=form[name=\"envCheck\"] > table > tbody > tr > td.setting-main > input[name=\"Submit\"]");
		
	
	}
	@After
	public void tearDown() throws Exception {
		selenium.stop();
	}

}
