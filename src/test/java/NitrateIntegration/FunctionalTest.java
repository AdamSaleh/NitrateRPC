/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;


/**
 *
 * @author asaleh
 */
public class FunctionalTest {
    
    public FunctionalTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    
 

    
    private WebDriver driver;
	private String baseUrl;
	private StringBuffer verificationErrors = new StringBuffer();
	@Before
	public void setUp() throws Exception {
		driver = new FirefoxDriver();
		baseUrl = "http://localhost:8080/";
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	}

	@Test
	public void testJava() throws Exception {
		driver.get(baseUrl + "job/selenium_test/");
		driver.findElement(By.linkText("Build Now")).click();
		driver.findElement(By.name("file")).sendKeys("/home/asaleh/Downloads/testng-report.xml");
		driver.findElement(By.cssSelector("button[type=\"button\"]")).click();
		driver.get(baseUrl + "job/selenium_test/lastBuild/");

		driver.findElement(By.linkText("Nitrate Plugin")).click();
		driver.findElement(By.name("_.username")).clear();
		driver.findElement(By.name("_.username")).sendKeys(PrivatePassword.name);
		driver.findElement(By.name("_.password")).clear();
		driver.findElement(By.name("_.password")).sendKeys(PrivatePassword.password);
		driver.findElement(By.cssSelector("button[type=\"button\"]")).click();
		try {
			assertEquals("https://tcms.engineering.redhat.com/xmlrpc/", driver.findElement(By.name("_.serverUrl")).getAttribute("value"));
		} catch (Error e) {
			verificationErrors.append(e.toString());
		}
		try {
			assertEquals("asaleh", driver.findElement(By.name("_.username")).getAttribute("value"));
		} catch (Error e) {
			verificationErrors.append(e.toString());
		}
		try {
			assertEquals("5866", driver.findElement(By.name("_.plan")).getAttribute("value"));
		} catch (Error e) {
			verificationErrors.append(e.toString());
		}
		try {
			assertEquals("TCMS", driver.findElement(By.name("_.product")).getAttribute("value"));
		} catch (Error e) {
			verificationErrors.append(e.toString());
		}
		try {
			assertEquals("Devel", driver.findElement(By.name("_.product_v")).getAttribute("value"));
		} catch (Error e) {
			verificationErrors.append(e.toString());
		}
		try {
			assertEquals("Functional", driver.findElement(By.name("_.category")).getAttribute("value"));
		} catch (Error e) {
			verificationErrors.append(e.toString());
		}
		try {
			assertEquals("P1", driver.findElement(By.name("_.priority")).getAttribute("value"));
		} catch (Error e) {
			verificationErrors.append(e.toString());
		}
		try {
			assertEquals("asaleh", driver.findElement(By.name("_.manager")).getAttribute("value"));
		} catch (Error e) {
			verificationErrors.append(e.toString());
		}
		try {
			assertEquals("RTT", driver.findElement(By.name("_.environment")).getAttribute("value"));
		} catch (Error e) {
			verificationErrors.append(e.toString());
		}
		driver.findElement(By.cssSelector("form[name=\"envCheck\"] > table > tbody > tr > td.setting-main > input[name=\"Submit\"]")).click();
		// ERROR: Caught exception [ERROR: Unsupported command [isTextPresent]]
	}

	@After
	public void tearDown() throws Exception {
		driver.quit();
		String verificationErrorString = verificationErrors.toString();
		if (!"".equals(verificationErrorString)) {
			fail(verificationErrorString);
		}
	}

	private boolean isElementPresent(By by) {
		try {
			driver.findElement(by);
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}
    
}
