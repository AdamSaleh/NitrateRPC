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
 *
 * @author asaleh
 */
public class FunctionalTest extends SeleneseTestCase{
    
    public FunctionalTest() {
    }

    
	@Before
	public void setUp() throws Exception {
		selenium = new DefaultSelenium("localhost", 4444, "*chrome", "http://localhost:8080/");
		selenium.start();
                
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
	public void testTest_manage_settings() throws Exception {
            
            
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("Settings updated"));
		selenium.type("name=_.username", "a");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("Error: Server returned HTTP 401 Unauthorized. Please check username and password."));
		selenium.type("name=_.plan", "asdf");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("asdf is possibly wrong plan id"));
		selenium.type("name=_.product", "T");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("T is possibly wrong product name (couldn't check product version and category)"));
		selenium.type("name=_.product_v", "eval");
		selenium.type("name=_.product_v", "evfqwal");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("evfqwal is possibly wrong product version"));
		selenium.type("name=_.category", "dewqd");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("dewqd is possibly wrong category name"));
		selenium.type("name=_.priority", "freaafd");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("freaafd is possibly wrong priority name"));
		selenium.type("name=_.manager", "sdfae");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("sdfae is possibly wrong manager's username"));
		selenium.type("name=_.environment", "weftrs");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("Possibly wrong environment group: weftrs"));
		selenium.type("name=_.manager", "jrusnack");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("Settings updated"));
		selenium.type("name=_.priority", "P2");
		selenium.type("name=_.manager", "asaleh");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("Settings updated"));
		selenium.type("name=_.product_v", "dsfaewwa");
		selenium.type("name=_.category", "dsafrewef");
		selenium.type("name=_.priority", "waefwqf");
		selenium.type("name=_.manager", "ewfa");
		selenium.type("name=_.environment", "wfasf");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("dsfaewwa is possibly wrong product version"));
		verifyTrue(selenium.isTextPresent("dsafrewef is possibly wrong category name"));
		verifyTrue(selenium.isTextPresent("waefwqf is possibly wrong priority name"));
		verifyTrue(selenium.isTextPresent("ewfa is possibly wrong manager's username"));
		verifyTrue(selenium.isTextPresent("Possibly wrong environment group: wfasf"));
		selenium.type("name=_.product", "adsafwea");
		selenium.type("name=_.product_v", "asfas");
		selenium.type("name=_.category", "asfafdsa");
		selenium.type("name=_.priority", "asfas");
		selenium.type("name=_.manager", "sadfas");
		selenium.type("name=_.environment", "sadffsa");
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("adsafwea is possibly wrong product name (couldn't check product version and category)"));
		verifyTrue(selenium.isTextPresent("asfas is possibly wrong priority name"));
		verifyTrue(selenium.isTextPresent("sadfas is possibly wrong manager's username"));
		verifyTrue(selenium.isTextPresent("Possibly wrong environment group: sadffsa"));
		selenium.click("name=Submit");
		selenium.waitForPageToLoad("30000");
		verifyTrue(selenium.isTextPresent("Settings updated"));
	}

	@After
	public void tearDown() throws Exception {
		selenium.stop();
	}

}
