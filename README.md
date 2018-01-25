# redcap-selenium
Library for automated testing of REDCap using Selenium in Java

Setup Instructions
------------------

1. Create a REDCap test instance on your local machine or dedicated test server.  **Never** run automated tests on your production server.

2. Install Java 8.

3. Install Maven.

4. Clone this repository and configure pom.xml to manage your dependencies.  Mine include:

    + TestNG framework
    + MySQL/MariaDB driver for Java

5. Update the constants in RedcapTestUtil.java to reflect your environment (REDCAP_URL, REDCAP_VERSION, DB_USER, DB_PASS, etc...)

    + I recommend creating a new REDCap account/password on your test instance to avoid accidentally connecting to your production server or exposing your password.
    + This library was built using REDCap v6.16.3.  Your version may have UI differences - HTML/CSS attributes of page elements may be different.  If your tests fail to find specific page elements, you will need to update the relevant places in the code to match your REDCap version.  I intend to update this library to work with REDCap v8.x by spring of 2018.

6. If you are testing on a recent version of Firefox, install [geckodriver](https://github.com/mozilla/geckodriver/releases) and update GECKO_DRIVER_LOC in RedcapTestUtil.java.  If you are testing on another browser, update the loadRedcap() function accordingly.

7. Create an example project in REDCap with a 'My First Instrument' form and enter a test record manually.

8. Update myfirst.testng.xml with parameters for the test record you entered above.

9. Go to the directory where pom.xml is located.  Run the tests defined in MyFirstInstrumentTest.java by typing:

mvn clean test  

If the code is working, a browser will open, log into REDCap, navigate to your 'My First Instrument' form and enter a new record with the same information you previously entered in Step #7.

10. View the HTML output in the target/surefire-reports directory.
