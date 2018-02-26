package com.github.draju.rcsel;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Contains utility functions for automated testing of REDCap using Selenium
 */
public class RedcapTestUtil {

  /** 
   * Set to 1 to print debugging statements to the console.
   * These messages are meant to track down errors only.
   * Informative messages on test progress should be in the test class itself.
   */ 
  public static int DEBUG = 0;  

  //Update the following constants to match your test environment
  public static final String BASE_URL = "https://localhost/redcap";   
  public static final String REDCAP_VERSION = "redcap_v8.2.1";     
  public static final String DB_USER = "root";                  
  public static final String DB_PASS = "*******";
  public static final String DEFAULT_REDCAP_USER = "draju";
  public static final String DEFAULT_REDCAP_PASS = "Rc123!@#$";
  public static final String GECKO_DRIVER_LOC = "/home/draju/Downloads/geckodriver";

  public static WebDriver driver;
  public static Connection conn = null;
  public static Statement stmt = null;
  public static ResultSet rs = null;
  public static Map<String, List<String>> recSet;

  /**
   * Create connection to REDCap database server
   */
  public static void initializeDB(){
    try {
      if(conn == null){
        //Update if your database is not MySQL/MariaDB or if not named 'redcap' 
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/redcap",
                                           DB_USER,DB_PASS);
      }
    }
    catch (SQLException ex) {
      System.out.println("SQLException: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());
    }
  }

  /**
   * Close connection to REDCap database server
   */
  public static void closeDB(){
    if (rs != null) try { rs.close();  } catch(Exception e) {}
    if (stmt != null) try { stmt.close(); } catch(Exception e) {}
    if (conn != null) try { conn.close(); conn = null; } catch(Exception e) {}
  }

  /**
   * Create browser instance and load the REDCap URL
   */
  public static void loadRedcap(){
    //Note that geckodriver is only needed with recent versions of Firefox
    System.setProperty("webdriver.gecko.driver",GECKO_DRIVER_LOC);
    //Reduce the verbosity of Firefox logging to the console
    System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,"/dev/null");

  	driver = new FirefoxDriver();
  	driver.get(BASE_URL);
  }

  /**
   * Login as the default REDCap user via the REDCap login form
   */
  public static void loginRedcap() {
    WebElement element = driver.findElement(By.id("username"));
    element.sendKeys(DEFAULT_REDCAP_USER);
    element = driver.findElement(By.id("password"));
    element.sendKeys(DEFAULT_REDCAP_PASS);
    driver.findElement(By.id("login_btn")).click();
  }

  /**
   * Login as the specified REDCap user via the REDCap login form
   *
   * @param username REDCap username 
   * @param passwd   REDCap password
   */
  public static void loginRedcap(String username, String passwd) {
    WebElement element = driver.findElement(By.id("username"));
    element.sendKeys(username);
    element = driver.findElement(By.id("password"));
    element.sendKeys(passwd);
    driver.findElement(By.id("login_btn")).click();
  }
  
  /**
   * Log out by clicking the REDCap logout link
   */
  public static void logoutRedcap(){
    driver.findElement(By.partialLinkText("Log out")).click(); 
  }

  /**
   * Find link based on link text and click it
   *
   * @param partialLinkText link text to search for, partial match is okay
   */
  public static void clickOnLink(String partialLinkText){
    WebElement element = new WebDriverWait(driver, 10).until(ExpectedConditions.elementToBeClickable(By.partialLinkText(partialLinkText)));
    element.click(); 
  }

  /**
   * Shortcut function to create a new record and open the first data entry form.
   * Clicks on the "Add / Edit Records" link and the "Add New Record" button in succession.
   * Includes wait time to allow each respective page to load.
   */
  public static void selectAddNewRecord(){
    //Click Add/Edit Records link on sidebar
    WebElement element = new WebDriverWait(driver, 10).until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Add / Edit Records")));
    element.click();

    //Click 'Add new record' button
    element = new WebDriverWait(driver, 10).until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(.,'Add new record')]")));
    element.click();

    //Wait for next page to load before returning
    //For longitudinal projects it's the event grid, for cross-sectional it's data entry form - both will contain record ID in URL
    new WebDriverWait(driver, 5).until(ExpectedConditions.urlContains("id=")); 
  }

  /**
   * Clicks on the "Add / Edit Records" link and then selects specified record from the dropdown
   * Includes wait time to allow each respective page to load
   *
   * @param recNum record ID to select
   * @param selectID HTML ID attribute for select menu since there could be 1-3 of them, Ex: "record" (if all records complete), "record_select1" (Incomplete), "record_select2" (Unverified), "record_select3" (Complete)
   */
  public static void selectExistingRecord(int recNum, String selectID){

    //Click Add/Edit Records link on sidebar
	WebElement element = new WebDriverWait(driver, 10).until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Add / Edit Records")));
	element.click();

	//Wait for dropdown to appear before selecting the record
	element = new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.id(selectID))); 
	Select dropdown = new Select(element);    
	dropdown.selectByValue(""+recNum);

  }

/**
 * Select a form to enter from the event grid based on the form name.
 * Assumes that you are already on Event Grid page.
 *
 * @param formName name of form (case-sensitive) to select for data entry
 */
public static void selectFormFromGrid(String formName){

  // Identify row of table with form name and then click on the first circular button you find
  // This method using xpath is the most concise, but could break if event table is modified 
  String xpathSelector = "//*[@id='event_grid_table']/tbody/tr/td[text()='"+formName+"']/following-sibling::td/a/img";
  WebElement element = new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpathSelector))); 
  element.click();

}

/**
 * Select REDCap data entry form to enter from those listed in the sidebar
 *
 * @param formName name of data entry form to select for entry
 */
public static void selectFormFromSidebar(String formName){

  WebElement element = new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("form["+formName+"]"))); 
  element.click();

}

/**
 * Select and click on a particular save button from the dropdown at the bottom of a data entry form
 *
 * @param buttonID id attribute of the button to click
 */
public static void selectSaveButtonDropdown(String buttonID){

  //Sometimes the save button is elevated from the dropdown choices to the top-level button itself
  //So check the primary button before looking through the dropdown choices
  String primaryButtonCSS = "button#"+buttonID;
  //There would only be one primary button, but use findElements so no exception is thrown if not found
  List<WebElement> primaryButtons = driver.findElements(By.cssSelector(primaryButtonCSS));
  if(primaryButtons.size() > 0){
    primaryButtons.get(0).click();
  }
  else {
    //First click the downward arrow to display the links
    WebElement element = new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.id("submit-btn-dropdown"))); 
    element.click();

    //Click on the dropdown link
    String linkID = "a#"+buttonID;
    element = new WebDriverWait(driver, 10).until(ExpectedConditions.elementToBeClickable(By.cssSelector(linkID)));
    element.click(); 
  }
}

/**
 * Loads REDCap data into memory for a particular combination of project ID, record ID and event ID
 *
 * @param origPID REDCap project ID 
 * @param origRecord REDCap record ID
 * @param origEventID REDCap event ID
 */
  public static void getOriginalRecord(int origPID, int origRecord, int origEventID){

    try {
      if(conn == null){
        initializeDB();
      }

      //Store data in memory as a map of variable name to list of values
      //Note that a list is needed because checkbox variables may be associated with multiple values 
      recSet = new HashMap<String,List<String>>();

      String selectSQL = "SELECT * from redcap_data where project_id='"+origPID+"' and record='"+origRecord+"' and event_id='"+origEventID+"'";
      stmt = conn.createStatement();
      rs = stmt.executeQuery(selectSQL);

      while(rs.next()){
        ResultSetMetaData rsmd = rs.getMetaData();
        int numVars = rsmd.getColumnCount();
        String fieldName = null;
        String fieldValue = null;
        for(int i=1; i<= numVars; i++){
            String colName = rsmd.getColumnName(i);
            String colValue = rs.getString(i);
            if(colName.equals("field_name")){
                fieldName = colValue;
            }
            if(colName.equals("value")){
                fieldValue = colValue;
            }                
        }
        List<String> listOfValues = recSet.get(fieldName);
        if(listOfValues == null){
          recSet.put(fieldName,listOfValues=new ArrayList<String>());
        }
        listOfValues.add(fieldValue);
        //echoDebug("Loaded fieldName=" + fieldName + "; fieldValue="+fieldValue);
      }
    } 
    catch (SQLException ex) {
      System.out.println("SQLException: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());
    }       
  }

  /**
   * Enters data for the current REDCap data entry form based on data that was previously entered for another record.
   * This function should only be called after the form has been opened and ID field has been entered or auto-generated.
   *
   * @param origPID REDCap project ID of original record
   * @param origRecord REDCap record ID of original record
   * @param origEventID REDCap event ID of original record
   * @param recVarName variable that holds the participant ID number, e.g. 'record_id'
   * @param saveButtonID identifies which save button to click at the bottom of the data entry form
   * @return the record ID for the form that was just entered or -1 on error
   */
  public static int enterForm(int origPID, int origRecord, int origEventID, String recVarName, String saveButtonID){

    //Load data from a previously entered record into memory so you can re-enter it in the current form
  	getOriginalRecord(origPID,origRecord,origEventID);

    //Save the record ID which should have been entered or pre-filled by REDCap so you can return it to the calling function
    int recNum = -1;
    String xpathSelector = "//tr[@id='"+recVarName+"-tr']/td[contains(@class,'data')]";
    WebElement element = new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpathSelector))); 
    recNum = Integer.parseInt(element.getText());
    echoDebug("The new record number="+recNum);

    //Loop through the form's data fields and enter data that was previously loaded in memory with getOriginalRecord()          
    List<WebElement> dataCells = driver.findElements(By.cssSelector("td.data"));
    echoDebug("There are " + dataCells.size() + " td.data rows");

    //Note that REDCap hides the select fields of auto-complete dropdowns and uses javascript to manipulate them
    //To avoid that complexity, unhide these fields so they can be treated as normal select elements below
    ((JavascriptExecutor)driver).executeScript("jQuery('select.rc-autocomplete').css('display','block')");

    for (WebElement cell : dataCells){
      //Loop through any dropdown menus in this cell and select the appropriate option
      List<WebElement> selectElements = cell.findElements(By.tagName("select"));
      for(WebElement selectElement : selectElements){
        if(selectElement != null && selectElement.isEnabled() && selectElement.isDisplayed()){
            Select selectField = new Select(selectElement);
            String selectName = selectElement.getAttribute("name");
            //echoDebug("Select element's name: "+selectName);
            List<WebElement> optionFields = selectElement.findElements(By.tagName("option"));
            for(WebElement option: optionFields){
              String optionValue = option.getAttribute("value");
              List<String> dbFieldValues = (List<String>) recSet.get(selectName);
              if(dbFieldValues != null){
	              for(String dbFieldValue : dbFieldValues){
	                if(dbFieldValue != null && dbFieldValue.equals(optionValue)){
	                  selectField.selectByValue(optionValue);
	                  break;
	                }
	              }
              } 
            }
        }
      }

      //Loop through any textarea elements within this data cell and enter them
      List<WebElement> textAreaElements = cell.findElements(By.tagName("textarea"));
      for(WebElement textArea:textAreaElements){
        if(textArea != null && textArea.isEnabled() && textArea.isDisplayed()){
        	String textAreaName = textArea.getAttribute("name");
        	List<String> textAreaValues = recSet.get(textAreaName);
        	if(textAreaValues != null){
	        	for(String val:textAreaValues){
	        		textArea.sendKeys(val);
	        		break;
	        	}
        	}
        }
      }

      //Loop through all input tags within the data cell and enter them
      List<WebElement> inputFields = cell.findElements(By.tagName("input"));
      String inputName, lookupName, inputType, classStr, inputStyle;
      List<String> inputValues;
      for(WebElement inputField : inputFields){
        // Reset values in case they are defined as blank strings
        inputName = null; lookupName = null; inputType = null; inputValues = null; classStr = null; inputStyle = null;
        inputType = inputField.getAttribute("type");
        inputName = inputField.getAttribute("name");
        //Need to strip off the ___radio in radio field name before doing the DB lookup
        if(inputType.equals("radio")){
          lookupName = inputName.replace("___radio","");
        }
        //Need to strip off the __chkn__ in checkbox field name before doing the DB lookup
        else if(inputType.equals("checkbox")){
          lookupName = inputName.replace("__chkn__","");  
        }                                
        else{
          lookupName = inputName;
        }
        classStr = inputField.getAttribute("class");
        inputStyle = inputField.getAttribute("style");
        //Note that a single variable can be associated with multiple values if it's a checkbox
        inputValues = (List<String>) recSet.get(lookupName);
        if(inputValues != null){
          for(String inputValue : inputValues){
            //echoDebug("Found field name=" + name + "; Type=" + type + "; Value=" + value + "; Class=" + classStr + "; Style=" + style);
            //Ignore hidden and disabled fields
            if(inputField.isEnabled() && inputField.isDisplayed()){
              //Enter text field, unless its value is blank or just a precursor to radio buttons
              if(inputType.equals("text")  && !classStr.contains("frmrd0") && inputValue != null){
                inputField.sendKeys(inputValue);
              }
              //Click the appropriate radio button
              else if(inputType.equals("radio")){
                String attrValue = inputField.getAttribute("value");
                //echoDebug("Radio button: linked varName=" + lookupName + "; DB value=" + value + "; Tag Value=" + fValue);
                if(inputValue != null && inputValue.equals(attrValue)){
                  inputField.click();
                  //echoDebug("Clicked Radio Button!");
                }
              }
              //Check the appropriate checkbox
              else if(inputType.equals("checkbox")){
                String codeStr = inputField.getAttribute("code");
                //echoDebug("Checkbox: linked varName=" + lookupName + "; DB value=" + value + "; Code=" + codeStr);
                if(inputValue != null && inputValue.equals(codeStr)){
                  inputField.click();
                  //echoDebug("Clicked Checkbox!");
                }
              }                       
            }
          } // end loop over values associated with this variable name 
        } // values != null for field name
      } // end loop over input tags

      //See if there is a save button in this cell, indicating that you are at the end of the form
      //If one of the save button dropdown options is used, click the dropdown.
      //Otherwise, look for the 'Save and Exit Record' button outside the dropdown
      if(!saveButtonID.equals("submit-btn-saverecord")){ 
        //There should only be one dropdown per form, but use findElements since it doesn't throw an exception if not found in this cell
        List<WebElement> saveButtonDropdowns = cell.findElements(By.cssSelector("button#submit-btn-dropdown"));        
        for(WebElement saveButtonDropdown : saveButtonDropdowns){
          selectSaveButtonDropdown(saveButtonID);
          waitAndHandlePopup(driver,"div.ui-dialog-buttonset button.ui-button","Ignore and leave record");
          return recNum;          
        }
      }
      else {
        //Click 'Save and Exit Form'
        List<WebElement> formButtons = cell.findElements(By.tagName("button"));
        String buttonID;
        for(WebElement formButton : formButtons){
          buttonID = formButton.getAttribute("id");
          if(formButton.isDisplayed() && buttonID.equals("submit-btn-saverecord")){
            formButton.click();
            waitAndHandlePopup(driver,"div.ui-dialog-buttonset button.ui-button","Ignore and leave record");
            return recNum;
          }
        }
      }
  } // end loop over td.data cells

  return recNum;
} // End of enterForm function 

/**
 * Looks for a matching REDCap log entry based on selection criteria and occuring within a certain number of seconds of the passed in timestamp.
 * Allows partial matches for sqlLog, dataValues, description and changeReason.
 * Limits query results for the redcap_log_event database table for the 10 most recent entries.
 * 
 * @param projectID redcap_log_event.project_id field
 * @param pk redcap_log_event.pk field
 * @param eventID redcap_log_event.event_id
 * @param sqlLog redcap_log_event.sql_log 
 * @param dataValues redcap_log_event.data_values field
 * @param description redcap_log_event.description field
 * @param changeReason redcap_log_event.change_reason field
 * @param timestamp passed in timestamp to compare against redcap_log_event.ts field
 * @param numSec number of seconds allowance that timestamp varies from redcap_log_event.ts field
 * @return value of redcap_log_event.log_event_id field for the matching REDCap log entry or -1 if no matching entry found
 */
public static Long checkRecentLogEntry(int projectID, int pk, int eventID, String sqlLog, String dataValues, String description, String changeReason, String timestamp, int numSec){
    try {
      if(conn == null){
        initializeDB();
      }
      //Make sure you are only selecting on indexed fields for query performance
      //Also set a limit on number of rows to return since log table could be enormous
      String selectSQL = "SELECT * from redcap_log_event where project_id='"+projectID+"' and event_id='"+eventID+"' order by log_event_id desc limit 10";
      //echoDebug("Log table lookup SQL = "+selectSQL);
      stmt = conn.createStatement();
      rs = stmt.executeQuery(selectSQL);
      while(rs.next()){
        ResultSetMetaData rsmd = rs.getMetaData();
        int numVars = rsmd.getColumnCount();
        //Return -1 if log entry not found
        Long logEventID = -1L;
        //These variables will be loaded from the DB to match against passed in parameters
        String tsField = "";
        String sqlLogField = "";
        String dataValuesField = "";
        String pkField = "";
        String descriptionField = "";
        String changeReasonField = "";

        for(int i=1; i<= numVars; i++){
            String colName = rsmd.getColumnName(i);
            String colValue = rs.getString(i);
            //echoDebug("Log record: colName="+colName+";colValue="+colValue);
            if(colValue == null){
              colValue = "";
            }
            if(colName.equals("log_event_id")){
                logEventID = Long.parseLong(colValue);
            }            
            if(colName.equals("ts")){
                tsField = colValue;
            }
            if(colName.equals("sql_log")){
                sqlLogField = colValue;
            }             
            if(colName.equals("data_values")){
                dataValuesField = colValue;
            }          
            if(colName.equals("pk")){
                pkField = colValue;
            }
            if(colName.equals("description")){
                descriptionField = colValue;
            }
            if(colName.equals("change_reason")){
                changeReasonField = colValue;
            }
                                                                    
        }
        if(tsField != "" && pkField != ""){
            int pkValue = Integer.parseInt(pkField);
            long deStamp = Long.parseLong(timestamp); //passed in timestamp when enterForm was called
            long logStamp = Long.parseLong(tsField);  //timestamp of log record
            long timeDiff = deStamp - logStamp;
            //echoDebug("Log Entry: pkValue="+pkValue+"; pk="+pk+"; ts="+tsField+"; timeDiff="+timeDiff+"; dataValues="+dataValuesField);
            if(pkValue == pk && sqlLogField.contains(sqlLog) && dataValuesField.contains(dataValues) && descriptionField.contains(description) && changeReasonField.contains(changeReason)){
                if(Math.abs(timeDiff) < numSec){
                  echoDebug("Matching log entry recorded in last " + numSec + " seconds - Success!");
                  return logEventID;
                }
                else{
                  echoDebug("Found matching log entry but it wasn't recorded in the last " + numSec + " seconds - Failure!");
                }
            }  
        }
      }
    } 
    catch (SQLException ex) {
      System.out.println("SQLException: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());
    }
    return null;  
}

/**
 * Waits for the specified popup to appear and if it does, clicks on the specified button to close it
 *
 * @param driver current WebDriver browser instance
 * @param buttonSelector css selector to identify the popup dialog button
 * @param buttonText text of the button you want to click 
 */
public static void waitAndHandlePopup(final WebDriver driver, String buttonSelector, String buttonText) {

  try{
    List<WebElement> buttons = new WebDriverWait(driver, 2).until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(buttonSelector)));

	for(WebElement button : buttons){
	  //echoDebug("waitAndHandlePopup button text = " + button.getText());
	  if(button.getText().equals(buttonText)){
	    //The button could be detected before it becomes clickable, so add a wait here     
	    button = new WebDriverWait(driver, 1).until(ExpectedConditions.elementToBeClickable(button));
	    button.click();
	    return;
	  }
	}
  }
  catch(TimeoutException e){
  	echoDebug("No popup button found with text="+buttonText);
  }
}

/**
 * Helper function that returns a value from the redcap database based on sql select query.
 * The function assumes that your query is one that returns a single value only - it returns null if the query returns multiple rows.
 * Best used for looking up specific info, e.g. the email address of a specific user
 *
 * @param sqlQuery SQL SELECT statement to return a specific value from the REDCap database
 * @return single value resulting from query or null if multiple rows are returned
 */
public static String getDBvalue(String sqlQuery){
    String fieldValue = null;
    try {
      //echoDebug("getDBValue sqlQuery = " + sqlQuery);
      if(conn == null){
        initializeDB();
      }
      stmt = conn.createStatement();
      rs = stmt.executeQuery(sqlQuery);
      int rowCount = 0;
      while(rs.next()){
        rowCount++;
        if(rowCount > 1){
          return null;
        }
        fieldValue = rs.getString(1);             
      }
      //echoDebug("getDBValue returns " + fieldValue);
    } 
    catch (SQLException ex) {
      System.out.println("SQLException: " + ex.getMessage());
      System.out.println("SQLState: " + ex.getSQLState());
      System.out.println("VendorError: " + ex.getErrorCode());
    }
    return fieldValue; 
}

  /**
   * Echoes debugging information to console
   *
   * @param str String printed to console
   */
  public static void echoDebug(String str){
    if(DEBUG > 0){
      System.out.println(str);
    }
  }

  /**
   * Echoes informational messages on individual test progress to console
   *
   * @param flag Flag that's passed in from Test Class; Set to 1 to display message, 0 to suppress
   * @param str String printed to console
   */
  public static void echoTestInfo(int flag, String str){
    if(flag > 0){
      System.out.println(str);
    }
  }

} // End of Class RedcapTestUtil