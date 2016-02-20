package org.openhab.binding.verisure.internal;

import static org.openhab.binding.verisure.VerisureBindingConstants.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.verisure.handler.VerisureBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class VerisureSession {
    private HashMap<String, VerisureObjectJSON> verisureObjects = new HashMap<String, VerisureObjectJSON>();

    private Logger logger = LoggerFactory.getLogger(VerisureBridgeHandler.class);
    private String authstring;
    private String csrf;

    private VerisureAlarmJSON alarmData = null;
    private CookieManager cm = new CookieManager();
    Gson gson = new GsonBuilder().create();

    public void initialize(String _authstring) {
        logger.debug("VerisureSession:initialize");

        authstring = _authstring.substring(0);

        // CookieHandler keeps us logged in
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cm);

        logIn();
        updateStatus();
    }

    private void updateStatus() {
        logger.debug("VerisureSession:updateStatus");
        updateAlarmStatus();
        updateVerisureObjects();
    }

    public synchronized void updateVerisureObjects() {
        logger.debug("VerisureSession:updateVerisureObjects");

        // Get JSON
        String result = httpGet(BASEURL + CLIMATEDATA_PATH);

        Gson gson = new GsonBuilder().create();
        JsonParser parser = new JsonParser();
        JsonArray jArray = parser.parse(result).getAsJsonArray();

        ArrayList<VerisureObjectJSON> lcs = new ArrayList<VerisureObjectJSON>();

        for (JsonElement obj : jArray) {
            VerisureObjectJSON cse = gson.fromJson(obj, VerisureObjectJSON.class);
            cse.setId(cse.getId().replaceAll("[^a-zA-Z0-9_]", "_"));
            lcs.add(cse);
            System.out.println(cse.getId());
            verisureObjects.put(cse.getId(), cse);

            // Should probably check if any of the items have been updated and notify caller
        }
    }

    public VerisureObjectJSON getVerisureObject(String serialNumber) {
        return verisureObjects.get(serialNumber);
    }

    public HashMap<String, VerisureObjectJSON> getVerisureObjects() {
        return verisureObjects;
    }

    public State getAlarmStatus() {
        if (alarmData != null) {

            String status = alarmData.getStatus();
            if (status != null) {
                return new StringType(status);
            }
        }
        return UnDefType.UNDEF;
    }

    public State getAlarmStatusNumeric() {

        if (alarmData != null) {

            String status = alarmData.getStatus();
            DecimalType val;

            if (status != null) {
                if (status.equals("unarmed")) {
                    val = new DecimalType(0);
                    return val;
                } else if (status.equals("armedhome")) {
                    val = new DecimalType(1);
                    return val;
                } else if (status.equals("armedaway")) {
                    val = new DecimalType(2);
                    return val;
                }

            }

        }
        return UnDefType.UNDEF;
    }

    public State getAlarmChangerName() {

        if (alarmData != null) {
            String status = alarmData.getName();
            if (status != null) {
                return new StringType(status);
            }
        }
        return UnDefType.UNDEF;
    }

    public State getAlarmTimestamp() {

        if (alarmData != null) {
            String status = alarmData.getDate();
            if (status != null) {
                return new StringType(status);
            }
        }
        return UnDefType.UNDEF;
    }

    public synchronized void updateAlarmStatus() {
        logger.debug("VerisureSession:updateAlarmStatus");

        // Get JSON
        String result = httpGet(BASEURL + ALARMSTATUS_PATH);

        // Trim JSON
        // Should replace with Jsonarrar/jsonparser
        result = result.substring(1, result.length() - 1);

        // Print in Console
        VerisureAlarmJSON jsonObject = gson.fromJson(result, VerisureAlarmJSON.class);

        alarmData = jsonObject;
    }

    public String httpGet(String urlString) {

        System.out.println("httpGetURL: " + urlString);
        String json = null;

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            int status = conn.getResponseCode();
            System.out.println("Status = " + status);
            // String key;
            // System.out.println("Headers-------start-----");
            // for (int i = 1; (key = conn.getHeaderFieldKey(i)) != null; i++) {
            // System.out.println(key + ":" + conn.getHeaderField(i));
            // }
            // System.out.println("Headers-------end-----");
            System.out.println("Content-------start-----");
            String inputLine;
            json = "";

            while ((inputLine = reader.readLine()) != null) {
                // System.out.println(inputLine);
                json += inputLine;
            }
            // System.out.println("Content-------end-----");
            System.out.println(json);
            in.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        return json;

    }

    private synchronized void logIn() {
        logger.debug("Attempting to log in to mypages.verisure.com");

        String url = BASEURL + LOGON_SUF;
        String source = sendHTTPpost(url, authstring);

        // System.out.println(source);

        url = BASEURL + START_SUF;
        source = httpGet(url);
        csrf = getCsrfToken2(source);

        logger.debug("Got CSRF: " + csrf);
        return;
    }

    private String sendHTTPpost(String urlString, String data) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            if (csrf != null) {
                conn.setRequestProperty("x-csrf-token", csrf);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
            }

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());

            wr.writeBytes(data);
            wr.flush();
            wr.close();

            InputStream in = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            // int status = conn.getResponseCode();
            // System.out.println("Status = " + status);
            // String key;
            // System.out.println("Headers-------start-----");
            // for (int i = 1; (key = conn.getHeaderFieldKey(i)) != null; i++) {
            // System.out.println(key + ":" + conn.getHeaderField(i));
            // }
            // System.out.println("Headers-------end-----");
            System.out.println("Content-------start-----");
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                System.out.println(inputLine);
            }
            System.out.println("Content-------end-----");
            in.close();

            return inputLine;
        } catch (Exception e) {
            logger.debug("had an exception" + e.toString());
        }
        return null;
    }

    private String sendHTTPpostAfterLogin1(String urlString, String data) {

        logger.debug("posting data to url: " + urlString);
        logger.debug("posting hte data: " + data);
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestProperty("x-csrf-token", csrf);

            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());

            wr.writeBytes(data);
            wr.flush();
            wr.close();

            InputStream in = conn.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            // int status = conn.getResponseCode();
            // System.out.println("Status = " + status);
            // String key;
            // System.out.println("Headers-------start-----");
            // for (int i = 1; (key = conn.getHeaderFieldKey(i)) != null; i++) {
            // System.out.println(key + ":" + conn.getHeaderField(i));
            // }
            // System.out.println("Headers-------end-----");
            System.out.println("Content-------start-----");
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                System.out.println(inputLine);
            }
            System.out.println("Content-------end-----");
            in.close();

            return inputLine;
        } catch (Exception e) {
            logger.debug("had an exception" + e.toString());
        }
        return null;
    }

    private boolean areWeLoggedIn() {
        logger.debug("areWeLoggedIn() - Checking if we are logged in");
        String urlString = "https://mypages.verisure.com/remotecontrol";

        try {

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setInstanceFollowRedirects(false);
            conn.connect();

            int status = conn.getResponseCode();

            switch (status) {
                case 200:
                    // Redirection
                    logger.debug("Status code 200. Probably logged in");
                    // updateStatus(ThingStatus.ONLINE);
                    return true;

                case 302:
                    // Redirection
                    logger.debug("Status code 302. Redirected. Probably not logged in");
                    // updateStatus(ThingStatus.OFFLINE);
                    return false;

                case 404:
                    // not found
                    logger.debug("Status code 404. Probably logged on too");
                    break;

                default:
                    break;
            }

        } catch (Exception e) {
            System.out.println("Error:" + e);
        }

        return false;

    }

    public boolean refresh() {

        for (int i = 0; i < 3; i++) {
            if (areWeLoggedIn()) {
                updateStatus();
                return true;
            } else {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                logIn();
            }
        }

        return false;
    }

    public void dispose() {
        logger.debug("Should dispose of objects here in session");

    }

    private String getCsrfToken2(String htmlText) {
        // Method should be replaced by regex logix
        String subString = null;
        try {
            int labelIndex = htmlText.indexOf("_csrf\" value=");

            subString = htmlText.substring(labelIndex + 14, labelIndex + 78);
            // logger.debug("QA test", "csrf-token = " + subString);
        } catch (IndexOutOfBoundsException e) {
            logger.debug("QA test", "Parsing Error = " + e.toString());
        }
        // Assert.assertNotNull("Null csrf-token ", subString);
        return subString;
    }

    public boolean disarmAlarm() {
        logger.debug("Sending command to disarm the alarm!");

        String url = BASEURL + ALARM_COMMAND;
        String data = "code=1000&state=DISARMED";

        sendHTTPpost(url, data);
        return true;
    }

    public boolean armHomeAlarm() {
        logger.debug("Sending command to arm_home the alarm!");

        String url = BASEURL + ALARM_COMMAND;
        String data = "code=1000&state=ARMED_HOME";

        sendHTTPpost(url, data);
        return true;
    }

    public boolean armAwayAlarm() {
        logger.debug("Sending command to arm_away the alarm!");

        String url = BASEURL + ALARM_COMMAND;
        String data = "code=1000&state=ARMED_AWAY";

        sendHTTPpost(url, data);
        return true;
    }
}
