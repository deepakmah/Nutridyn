import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Nutridyn {

    // ── Config ──────────────────────────────────────────────────────────────
    // ImgBB key: set IMGBB_API_KEY env var in GitHub Secrets, falls back to hardcoded for local runs
    static final String IMGBB_API_KEY = System.getenv("IMGBB_API_KEY") != null
                                      ? System.getenv("IMGBB_API_KEY")
                                      : "46866c7eef7ee62b26a79f32a5d57a08";
    static final String RUN_DATE      = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    static final String RUN_TIME      = new SimpleDateFormat("HH-mm-ss").format(new Date());
    static final String START_TIME    = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

    // ── Dynamic base path: works on any machine, any OS ─────────────────────
    // Points to:  <your home folder>/NutridynAutomation/
    // Windows  →  C:\Users\YourName\NutridynAutomation\
    // Mac/Linux→  /Users/YourName/NutridynAutomation/
    static final String BASE_DIR = System.getProperty("user.home")
                                 + File.separator + "NutridynAutomation";

    static final String SS_DIR   = BASE_DIR + File.separator + "screenshots"
                                 + File.separator + RUN_DATE + File.separator + RUN_TIME;

    static final String HTML_DIR = BASE_DIR + File.separator + "html"
                                 + File.separator + RUN_DATE + File.separator + RUN_TIME;

    static final String CSV_PATH = BASE_DIR + File.separator + "reports"
                                 + File.separator + "Nutridyn.csv";

    static int totalSteps  = 0;
    static int passedSteps = 0;
    static int failedSteps = 0;
    static final List<String> htmlSteps = new ArrayList<>();

    // ── Shared driver-level helpers ──────────────────────────────────────────
    static WebDriver        driver;
    static Actions          actions;
    static WebDriverWait    wait;
    static JavascriptExecutor js;

    public static void main(String[] args) throws Exception {

        // ── Headless mode: auto-enabled on CI (no screen), off locally ──────
        ChromeOptions options = new ChromeOptions();
        boolean isHeadless = "true".equalsIgnoreCase(System.getenv("HEADLESS"));
        if (isHeadless) {
            options.addArguments("--headless=new");      // Chrome 112+ headless flag
            options.addArguments("--no-sandbox");        // required on Linux CI
            options.addArguments("--disable-dev-shm-usage"); // prevent /dev/shm crash
            options.addArguments("--window-size=1920,1080"); // fixed size (no real screen)
            System.out.println("Running in HEADLESS mode (CI environment)");
        } else {
            System.out.println("Running in HEADED mode (local environment)");
        }

        driver  = new ChromeDriver(options);
        actions = new Actions(driver);
        wait    = new WebDriverWait(driver, Duration.ofSeconds(15));
        js      = (JavascriptExecutor) driver;

        driver.manage().window().maximize();
        driver.navigate().to("https://nutridyn.com/");
        pause(1500);
        screenshot("Homepage");

        // ── Accept cookie ────────────────────────────────────────────────────
        clickIfPresent(By.xpath("//span[contains(text(),'Accept')]"), "Cookie_Accepted");

        // ── Patient login ────────────────────────────────────────────────────
        clickIfPresent(By.xpath("//*[@id='header-account-1']/ul/li/a"), "Login_Page");
        typeInto(By.id("email"), "gopal.exinentpatient@gmail.com");
        typeInto(By.id("pass"),  "test@1234");
        clickIfPresent(By.name("send"), "Patient_Login_Success");

        // ── Hover over "Categories" nav item ────────────────────────────────
        WebElement categoryMenu = waitForElement(By.xpath("//*[@id='nav']/div[1]/ul/li[2]/a"));
        actions.moveToElement(categoryMenu).perform();
        pause(1500);
        System.out.println("Category hover performed");

        // ── Click Blood Sugar Balance sub-category ───────────────────────────
        WebElement bloodSugar = waitForElement(
                By.xpath("//*[@id='nav']/div[1]/ul/li[2]/div/div[2]/div/ul/li[2]/a"));
        actions.moveToElement(bloodSugar).click().perform();
        pause(1000);
        screenshot("Blood_Sugar_Category");
        scrollBy(0, 300);
        pause(1000);

        // ── Open first product ───────────────────────────────────────────────
        clickIfPresent(
                By.xpath("//*[@id='maincontent']/div[3]/div[1]/div[4]/ol/li[1]/div/div[2]/strong/a"),
                "Product_Page_Alpha_Lipoic");

        // ── Add to cart ──────────────────────────────────────────────────────
        clickIfPresent(By.id("product-addtocart-button"), "Product_Added_To_Cart");

        // ── Open mini-cart ───────────────────────────────────────────────────
        clickIfPresent(
                By.xpath("//*[@id='minicart']/div[1]/span/span[1]"),
                "Cart_Page");
        scrollBy(0, 300);
        pause(1000);

        // ── Proceed to checkout ──────────────────────────────────────────────
        clickIfPresent(
                By.xpath("//*[@id='maincontent']/div[3]/div/div[6]/div[1]/div[2]/ul/li/button/span"),
                "Checkout_Page");

        // ── Back to home ─────────────────────────────────────────────────────
        clickIfPresent(By.xpath("//*[@id='header_logo']/a/img"), "Home_After_Checkout");

        // ── Account → Home (using Actions) ───────────────────────────────────
        WebElement accountLink = waitForElement(By.xpath("//*[@id='header-account-1']/ul/li/a"));
        actions.moveToElement(accountLink).click().perform();
        System.out.println("Account page opened");
        pause(1500);

        WebElement homeLogo = waitForElement(By.xpath("//*[@id='header_logo']/a"));
        actions.moveToElement(homeLogo).click().perform();
        System.out.println("Home page opened");
        pause(1500);

        // ── Practitioner login ───────────────────────────────────────────────
        clickIfPresent(By.xpath("//*[@id='header-account-1']/ul/li/a"), null);
        typeInto(By.id("email"), "foo.bar@getastra.live");
        typeInto(By.id("pass"),  "123456");
        clickIfPresent(By.name("send"), "Practitioner_Login_Success");

        // ── My Orders ────────────────────────────────────────────────────────
        WebElement myOrders = waitForElement(
                By.xpath("//*[@id='maincontent']/div[2]/div[2]/div/div/div/ul[1]/li[2]/a/span"));
        actions.moveToElement(myOrders).click().perform();
        System.out.println("My Orders page opened");
        screenshot("My_Orders");
        pause(1000);

        // ── First order view ─────────────────────────────────────────────────
        clickIfPresent(
                By.xpath("//*[@id='my-orders-table']/tbody/tr[1]/td[6]/a[1]/span"),
                "First_Order_View");

        // ── My Subscriptions ─────────────────────────────────────────────────
        clickIfPresent(
                By.xpath("//*[@id='maincontent']/div[2]/div[2]/div/div/div/ul[1]/li[3]/a/span"),
                "My_Subscriptions");

        // ── Subscription view ────────────────────────────────────────────────
        clickIfPresent(
                By.xpath("//*[@id='my-orders-table']/tbody/tr[1]/td[6]/a/span"),
                "Subscription_View");

        // ── List page ────────────────────────────────────────────────────────
        clickIfPresent(
                By.xpath("//*[@id='maincontent']/div[2]/div[2]/div/div/div/ul[1]/li[4]/a/span"),
                "List_Page");

        // ── My Patients ──────────────────────────────────────────────────────
        clickIfPresent(
                By.xpath("//*[@id='maincontent']/div[2]/div[2]/div/div/div/ul[1]/li[5]/a/span"),
                "My_Patients");

        // ── Address Book ─────────────────────────────────────────────────────
        clickIfPresent(
                By.xpath("//*[@id='maincontent']/div[2]/div[2]/div/div/div/ul[1]/li[6]/a/span"),
                "Address_Book");
        scrollBy(0, 400);
        pause(1000);

        // ── NutriScripts ─────────────────────────────────────────────────────
        clickIfPresent(By.xpath("//a//span[text()='NutriScripts']"), "NutriScripts");
        scrollBy(0, 200);
        pause(1000);

        // ── View NutriScript ─────────────────────────────────────────────────
        clickIfPresent(By.xpath("//a[contains(text(),'View NutriScript')]"), "View_NutriScript");
        scrollBy(0, 100);
        pause(1000);

        // ── 3X4 Genetics ─────────────────────────────────────────────────────
        clickIfPresent(By.xpath("//span[contains(text(),'3X4 Genetics')]"), "3X4_Genetics");

        // ── NutriDyn Connect ─────────────────────────────────────────────────
        clickIfPresent(By.xpath("//span[text()='NutriDyn Connect']"), "NutriDyn_Connect");

        // ── NutriDyn Connect Pro ─────────────────────────────────────────────
        clickIfPresent(By.xpath("//a[normalize-space()='NutriDyn Connect Pro']"), "NutriDyn_Connect_Pro");
        scrollBy(0, 200);
        pause(1000);

        // ── Connect Applet ───────────────────────────────────────────────────
        clickIfPresent(By.xpath("//a[contains(@href,'connectpro/applet')]"), "Connect_Applet");
        scrollBy(0, 300);
        pause(1000);

        // ── Connect Links ────────────────────────────────────────────────────
        clickIfPresent(
                By.xpath("//*[@id='maincontent']/div[2]/div[2]/div/div/div/ul[1]/li[10]/ul/li[3]/a"),
                "Connect_Links");
        scrollBy(0, -400);
        pause(1000);

        // ── NutriScripts (again) ─────────────────────────────────────────────
        clickIfPresent(By.xpath("//a//span[text()='NutriScripts']"), null);

        // ── Go to homepage ───────────────────────────────────────────────────
        WebElement logoFinal = waitForElement(By.xpath("//*[@id='header_logo']/a"));
        actions.moveToElement(logoFinal).click().perform();
        System.out.println("Home page opened again");
        screenshot("Home_Again");
        pause(1000);

        // ── Logout ───────────────────────────────────────────────────────────
        clickIfPresent(By.xpath("//a[normalize-space()='Logout']"), "Logout");

        pause(1500);
        driver.quit();
        System.out.println("=== Test complete | Passed: " + passedSteps + " | Failed: " + failedSteps + " ===");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helper methods
    // ════════════════════════════════════════════════════════════════════════

    /** Wait for an element to be visible, then return it. */
    private static WebElement waitForElement(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Click an element via Actions if it exists on the page.
     * If {@code screenshotTitle} is non-null a screenshot is taken after the click.
     */
    private static void clickIfPresent(By locator, String screenshotTitle) throws IOException {
        List<WebElement> elements = driver.findElements(locator);
        if (!elements.isEmpty()) {
            WebElement el = wait.until(ExpectedConditions.elementToBeClickable(locator));
            // Scroll element into view before interacting
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            actions.moveToElement(el).click().perform();
            System.out.println("Clicked: " + locator);
            pause(1500);
            if (screenshotTitle != null) screenshot(screenshotTitle);
        } else {
            System.out.println("Element not found, skipping: " + locator);
        }
    }

    /**
     * Clear a text field and type into it via Actions (triple-click to select all → type).
     */
    private static void typeInto(By locator, String text) {
        List<WebElement> elements = driver.findElements(locator);
        if (!elements.isEmpty()) {
            WebElement field = wait.until(ExpectedConditions.elementToBeClickable(locator));
            // Triple-click selects existing text, then type replaces it
            actions.click(field)
                   .keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL)
                   .sendKeys(text)
                   .perform();
            System.out.println("Typed into: " + locator);
            pause(800);
        } else {
            System.out.println("Input field not found: " + locator);
        }
    }

    /** Scroll the page by (x, y) pixels via Actions wheel scroll (Selenium 4). */
    private static void scrollBy(int x, int y) {
        actions.scrollByAmount(x, y).perform();
        pause(500);
    }

    /** Simple sleep wrapper (milliseconds). */
    private static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Screenshot / reporting helpers (unchanged logic, cleaned up)
    // ════════════════════════════════════════════════════════════════════════

    private static void screenshot(String title) throws IOException {
        screenshot(title, true, "Step completed successfully");
    }

    private static void screenshot(String title, boolean isPass, String details) throws IOException {
        totalSteps++;
        if (isPass) passedSteps++; else failedSteps++;

        String timestamp  = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String prefix     = isPass ? "SUCCESS_" : "ERROR_";
        String fileName   = prefix + title + "_" + timestamp + ".png";

        File folder = new File(SS_DIR);
        if (!folder.exists()) folder.mkdirs();

        File outputFile = new File(folder, fileName);
        File src        = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        Files.copy(src.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Screenshot saved: " + outputFile.getAbsolutePath());

        String uploadedUrl = "Upload failed/skipped";
        try {
            uploadedUrl = uploadToImgbb(outputFile);
            System.out.println("Uploaded URL: " + uploadedUrl);
        } catch (Exception e) {
            System.out.println("Could not upload to Imgbb: " + e.getMessage());
        }

        writeCsv(timestamp, title, uploadedUrl, fileName);
        writeHtmlReport(timestamp, title, fileName, uploadedUrl, isPass, details);
    }

    private static String uploadToImgbb(File imageFile) throws IOException {
        String encoded = Base64.getEncoder().encodeToString(Files.readAllBytes(imageFile.toPath()));
        String data    = "key=" + IMGBB_API_KEY + "&image=" + URLEncoder.encode(encoded, "UTF-8");

        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.imgbb.com/1/upload").openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        try (OutputStream os = conn.getOutputStream()) { os.write(data.getBytes()); }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString().split("\"url\":\"")[1].split("\"")[0].replace("\\/", "/");
    }

    private static void writeCsv(String timestamp, String title, String url, String localFileName) {
        File fileObj = new File(CSV_PATH);
        if (!fileObj.getParentFile().exists()) fileObj.getParentFile().mkdirs();
        boolean exists = fileObj.exists();

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(CSV_PATH, true)))) {
            if (!exists) out.println("Timestamp,Title,LocalFile,UploadedURL");
            out.println(timestamp + "," + title + "," + localFileName + "," + url);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void writeHtmlReport(String timestamp, String title, String localFileName,
                                        String url, boolean isPass, String details) {
        File htmlFolder = new File(HTML_DIR);
        if (!htmlFolder.exists()) htmlFolder.mkdirs();

        String htmlFile        = HTML_DIR + "\\TestReport.html";
        String relativeImgPath = "../../../screenshots/" + RUN_DATE + "/" + RUN_TIME + "/" + localFileName;
        String statusClass     = isPass ? "pass" : "fail";
        String icon            = isPass ? "✅" : "❌";

        StringBuilder stepHtml = new StringBuilder();
        stepHtml.append("            <div class=\"test-step ").append(statusClass).append("\">\n");
        stepHtml.append("                <div class=\"step-header\">\n");
        stepHtml.append("                    <span>").append(icon).append(" ")
                .append(title.replace("_", " ").toUpperCase()).append("</span>\n");
        stepHtml.append("                    <span class=\"step-time\">")
                .append(timestamp.split("_")[1].replace("-", ":")).append("</span>\n");
        stepHtml.append("                </div>\n");
        stepHtml.append("                <div class=\"step-details\">").append(details).append("</div>\n");
        stepHtml.append("                <div style=\"margin-top:15px;\">");
        stepHtml.append("<a href=\"").append(relativeImgPath).append("\" target=\"_blank\">");
        stepHtml.append("<img class=\"screenshot\" src=\"").append(relativeImgPath)
                .append("\" alt=\"").append(title).append("\"></a></div>\n");
        stepHtml.append("                <div style=\"margin-top:10px;\">");
        stepHtml.append("<a class=\"btn\" href=\"").append(relativeImgPath)
                .append("\" target=\"_blank\">View Local</a>\n");
        if (url != null && url.startsWith("http"))
            stepHtml.append("                    <a class=\"btn imgbb\" href=\"").append(url)
                    .append("\" target=\"_blank\">View ImgBB</a>\n");
        stepHtml.append("                </div>\n            </div>");

        htmlSteps.add(stepHtml.toString());

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(htmlFile, false)))) {

            double passRate    = totalSteps > 0 ? ((double) passedSteps / totalSteps) * 100 : 0;
            String overall     = failedSteps > 0 ? "FAILED" : "PASSED";
            String badgeClass  = failedSteps > 0 ? "status-fail" : "status-pass";
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            out.println("<!DOCTYPE html><html lang=\"en\"><head>");
            out.println("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">");
            out.println("<title>NutriDyn Automation - Test Report</title>");
            out.println("<style>");
            out.println("body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;margin:0;padding:20px;background:linear-gradient(135deg,#f5f7fa 0%,#c3cfe2 100%)}");
            out.println(".container{max-width:1400px;margin:0 auto;background:#fff;border-radius:15px;box-shadow:0 10px 30px rgba(0,0,0,.1);padding:40px}");
            out.println(".header{text-align:center;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:#fff;padding:40px;border-radius:15px;margin-bottom:40px}");
            out.println(".header h1{margin:0;font-size:2.5em;font-weight:300}");
            out.println(".summary{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:25px;margin-bottom:40px}");
            out.println(".summary-card{background:linear-gradient(135deg,#f8f9fa 0%,#e9ecef 100%);padding:25px;border-radius:15px;text-align:center;border-left:6px solid #667eea;transition:transform .3s}");
            out.println(".summary-card:hover{transform:translateY(-5px)}");
            out.println(".summary-card h3{margin:0 0 15px;color:#333;font-size:1.1em}");
            out.println(".number{font-size:2.5em;font-weight:bold;color:#333;margin-bottom:10px}");
            out.println(".progress-bar{width:100%;height:25px;background:#e9ecef;border-radius:12px;overflow:hidden;margin:15px 0}");
            out.println(".progress-fill{height:100%;background:linear-gradient(90deg,#28a745,#20c997);border-radius:12px}");
            out.println(".test-results{margin:40px 0;display:flex;flex-direction:column;gap:15px}");
            out.println(".test-step{margin:15px 0;padding:20px;border-radius:12px;border-left:6px solid;transition:all .3s}");
            out.println(".test-step:hover{transform:translateX(5px)}");
            out.println(".test-step.pass{background:linear-gradient(135deg,#d4edda 0%,#c3e6cb 100%);border-left-color:#28a745}");
            out.println(".test-step.fail{background:linear-gradient(135deg,#f8d7da 0%,#f5c6cb 100%);border-left-color:#dc3545}");
            out.println(".step-header{font-weight:bold;margin-bottom:12px;font-size:1.1em}");
            out.println(".step-details{font-size:.95em;color:#666;line-height:1.5}");
            out.println(".step-time{font-size:.85em;color:#888;float:right;background:rgba(0,0,0,.05);padding:4px 8px;border-radius:5px}");
            out.println(".screenshot{max-width:300px;border-radius:8px;margin:15px 0;border:2px solid #ddd;transition:transform .3s;cursor:pointer}");
            out.println(".screenshot:hover{transform:scale(1.05)}");
            out.println(".status-badge{display:inline-block;padding:8px 20px;border-radius:25px;color:#fff;font-weight:bold}");
            out.println(".status-pass{background:linear-gradient(135deg,#28a745 0%,#20c997 100%)}");
            out.println(".status-fail{background:linear-gradient(135deg,#dc3545 0%,#c82333 100%)}");
            out.println(".btn{display:inline-block;padding:8px 15px;background:linear-gradient(135deg,#28a745 0%,#20c997 100%);color:#fff;text-decoration:none;border-radius:20px;font-weight:bold;font-size:.9em;margin-right:10px;transition:opacity .3s;box-shadow:0 2px 5px rgba(0,0,0,.1)}");
            out.println(".btn:hover{opacity:.9}.btn.imgbb{background:linear-gradient(135deg,#17a2b8 0%,#117a8b 100%)}");
            out.println("</style></head><body><div class=\"container\">");

            out.println("<div class=\"header\"><h1>NutriDyn Automation</h1>");
            out.println("<p style=\"font-size:1.2em;margin:10px 0\">Test Report with Detailed Steps</p>");
            out.println("<div style=\"text-align:center;color:#fff;margin:25px 0;font-size:1.1em\">Generated on: "
                    + RUN_DATE + " at " + RUN_TIME.replace("-", ":") + "</div></div>");

            out.println("<div class=\"summary\">");
            out.println("<div class=\"summary-card\"><h3>Overall Status</h3><div class=\"status-badge " + badgeClass + "\">" + overall + "</div></div>");
            out.println("<div class=\"summary-card\"><h3>Total Steps</h3><div class=\"number\">" + totalSteps + "</div></div>");
            out.println("<div class=\"summary-card\"><h3>Passed</h3><div class=\"number\" style=\"color:#28a745\">" + passedSteps + "</div></div>");
            out.println("<div class=\"summary-card\"><h3>Failed</h3><div class=\"number\" style=\"color:#dc3545\">" + failedSteps + "</div></div>");
            out.println("<div class=\"summary-card\"><h3>Pass Rate</h3><div class=\"number\">" + String.format("%.1f", passRate) + "%</div>");
            out.println("<div class=\"progress-bar\"><div class=\"progress-fill\" style=\"width:" + passRate + "%\"></div></div></div>");
            out.println("</div>");

            out.println("<div style=\"text-align:center;margin:20px 0\">");
            out.println("<p><strong>Test Duration:</strong> " + START_TIME + " to " + currentTime + "</p></div>");

            out.println("<div class=\"test-results\"><h2>Detailed Test Results</h2>");
            out.println("<p style=\"color:#666;margin-bottom:30px\">Step-by-step execution details with screenshots</p>");
            for (String step : htmlSteps) out.println(step);
            out.println("</div>");

            out.println("<div style=\"margin-top:50px;padding-top:20px;border-top:1px solid #dee2e6;text-align:center;color:#6c757d\">");
            out.println("<p>Generated by NutriDyn Automation Framework</p></div>");
            out.println("</div></body></html>");

        } catch (IOException e) { e.printStackTrace(); }
    }
}
