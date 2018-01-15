import com.github.draju.rcsel.RedcapTestUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Date;
import java.text.SimpleDateFormat;



/**
 * Test REDCap data entry of a form titled 'My First Instrument'.  
 */
public class MyFirstInstrumentTest {

  private static WebDriver driver;

  //Intialize variables that are needed across multiple tests
  private static int newRecNum = -1;
  private static String deTimeStamp = null;
  
  /**
   * Opens a connection to REDCap database
   */
  @BeforeTest
  public void openDatabase(){
    System.out.println("=========== Running tests for "+this.getClass().getSimpleName());
    RedcapTestUtil.initializeDB();
  } 

  /**
   * Opens web browser and loads REDCap URL
   */
  @BeforeTest
  public void loadWebsite(){
  	RedcapTestUtil.loadRedcap();
  	driver = RedcapTestUtil.driver;
  }	

  /**
   * Verifies that the REDCap landing page was successfully loaded 
   */
  @Test(priority = 0)
  public void verifyLandingPage() {

    System.out.println("----------- Executing verifyLandingPage");  

    //Wait if necessary for REDCap home page to load
    new WebDriverWait(driver, 5).until(ExpectedConditions.urlContains(RedcapTestUtil.BASE_URL));    
    String expectedTitle = "REDCap";
    String actualTitle = driver.getTitle();
    System.out.println("Page title = "+actualTitle);
    Assert.assertEquals(actualTitle, expectedTitle);

  }

  /**
   * Logs into REDCap using default user/pass specified in REDCapTestUtil class
   */
  @Test(priority = 1)
  public void verifyLogin() {

    System.out.println("----------- Executing verifyLogin");  

    RedcapTestUtil.loginRedcap();
    
    //If login worked, you should see a 'Log out' link
    WebElement logOutLink = null;
    logOutLink = new WebDriverWait(driver, 5).until(ExpectedConditions.elementToBeClickable(By.linkText("Log out")));
    Assert.assertNotNull(logOutLink);

  }

  /**
   * Clicks on the specified REDcap project from the 'My Projects' tab
   */
  @Parameters({ "pid", "project_title" })
  @Test(priority = 2)
  public void verifyProjectPage(int pid, String project_title){

    System.out.println("----------- Executing verifyProjectPage");

    RedcapTestUtil.loadProjectPage(pid);
    //Wait if necessary for project page to load
    new WebDriverWait(driver, 5).until(ExpectedConditions.urlContains(RedcapTestUtil.REDCAP_VERSION+"/ProjectSetup/index.php?pid="+pid));    
    String actualTitle = driver.getTitle();
    System.out.println("Page title = "+actualTitle);
    Assert.assertTrue(actualTitle.contains(project_title));

  }

  /**
   * Clicks on "Add/Edit Records" and "Add new record" in succession to open the first form for data entry
   */
  @Parameters({ "pid", "orig_record", "form_name"})
  @Test(priority = 3)
  public void verifyAddNewRecord(int pid, int orig_record, String form_name){

    System.out.println("----------- Executing verifyAddNewRecord");

    RedcapTestUtil.selectAddNewRecord();     
    String currentUrl = driver.getCurrentUrl();
    String expectedPage = "page="+form_name.toLowerCase().replaceAll(" ", "_");
    //System.out.println("currentUrl="+currentUrl+";  expectedPage="+expectedPage);
    Assert.assertTrue(currentUrl.contains(expectedPage));
  
  }

  /**
   * Re-enters data that was previously entered for another record and saves the form
   */
  @Parameters({ "pid", "orig_record", "event_id", "form_name", "rec_var_name"})
  @Test(priority = 4)
  public void verifyDataEntry(int pid, int orig_record, int event_id, String form_name, String rec_var_name){

    System.out.println("----------- Executing verifyDataEntry");

    newRecNum = RedcapTestUtil.enterForm(pid,orig_record,event_id,rec_var_name);
    deTimeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    if(newRecNum > 0){
      System.out.println("Form '"+form_name+"' entered for Record #"+newRecNum+" at "+deTimeStamp);
    }
    Assert.assertTrue(newRecNum > 0);
  }
 
  /**
   * Verifies that a 'Create record' log entry was created for a matching record in the past 60 seconds
   */
  @Parameters({ "pid","event_id"})
  @Test(priority = 5)
  public void verifyLogEntry(int pid, int event_id){

    System.out.println("----------- Executing verifyLogEntry");

    Long logEventID = RedcapTestUtil.checkRecentLogEntry(pid, newRecNum, event_id,"","","Create record","",deTimeStamp,60);
    System.out.println("Log entry for ID="+logEventID+" found in last 60 seconds");
    Assert.assertTrue(logEventID > 0);
  }

  /**
   * Closes the browser
   */
  @AfterTest
  public void endSession(){
  	driver.quit();   
  }  

  /**
   * Closes the database connection
   */
  @AfterSuite
  public void closeDB(){
    RedcapTestUtil.closeDB();
  }
}