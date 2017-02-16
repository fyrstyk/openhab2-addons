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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class VerisureSession {
    private HashMap<String, VerisureBaseObjectJSON> verisureObjects = new HashMap<String, VerisureBaseObjectJSON>();

    private Logger logger = LoggerFactory.getLogger(VerisureSession.class);
    private String authstring;
    private String csrf;

    private VerisureAlarmJSON alarmData = null;
    private CookieManager cm = new CookieManager();
    private Gson gson = new GsonBuilder().create();

    private VerisureAlarmJSON doorData;

    private List<DeviceStatusListener> deviceStatusListeners = new CopyOnWriteArrayList<>();

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
        try {
            updateAlarmStatus();
            updateVerisureObjects();
        } catch (RuntimeException e) {
            logger.error("Failed in updatestatus", e);
        }
    }

    public synchronized void updateVerisureObjects() {
        logger.debug("VerisureSession:updateVerisureObjects");

        // Get JSON
        String result = httpGet(BASEURL + CLIMATEDATA_PATH);
        logger.trace("Data:" + result);
        VerisureSensorJSON[] sensors = gson.fromJson(result, VerisureSensorJSON[].class);
        logger.trace("Sensor:" + sensors);
        for (VerisureSensorJSON cse : sensors) {
            cse.setId(cse.getId().replaceAll("[^a-zA-Z0-9_]", "_"));
            logger.trace(cse.getId());
            VerisureBaseObjectJSON oldObj = verisureObjects.get(cse.getId());
            if (oldObj == null || !oldObj.equals(cse)) {
                logger.debug("Sensor data Changed {}", cse.toString());
                verisureObjects.put(cse.getId(), cse);
                notifyListeners(cse);
            } else {
                logger.debug("Sensor data NOT Changed {}", cse.toString());
            }

            // Should probably check if any of the items have been updated and notify caller
        }
    }

    private void notifyListeners(VerisureBaseObjectJSON cse) {
        for (DeviceStatusListener listener : deviceStatusListeners) {
            listener.onDeviceStateChanged(cse);
        }
    }

    public VerisureBaseObjectJSON getVerisureObject(String serialNumber) {
        return verisureObjects.get(serialNumber);
    }

    public HashMap<String, VerisureBaseObjectJSON> getVerisureObjects() {
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
        logger.trace("VerisureSession:updateAlarmStatus");

        // Get JSON
        String result = httpGet(BASEURL + ALARMSTATUS_PATH);

        // Trim JSON
        // Should replace with Jsonarrar/jsonparser
        // result = result.substring(1, result.length() - 1);

        // Print in Console
        VerisureAlarmJSON[] jsonObjects = gson.fromJson(result, VerisureAlarmJSON[].class);
        for (VerisureAlarmJSON object : jsonObjects) {
            if (object.getType().equals("ARM_STATE")) {
                setAlarmData(object);
            } else if (object.getType().equals("DOOR_LOCK")) {
                setDoorData(object);
            }
        }

    }

    private void setAlarmData(VerisureAlarmJSON object) {
        if (!object.equals(alarmData)) {
            logger.debug("Alarm data Updated {}", object.toString());
            this.alarmData = object;
            notifyListeners(alarmData);
        } else {
            logger.debug("Alarm data Not Changed {}", object.toString());
        }
    }

    private void setDoorData(VerisureAlarmJSON object) {
        if (!object.equals(doorData)) {
            logger.debug("Door data Updated {}", object.toString());
            this.doorData = object;
            this.verisureObjects.put(doorData.getId(), this.doorData);
            notifyListeners(doorData);
        } else {
            logger.debug("Door data Not Changed {}", object.toString());
        }
    }

    public String httpGet(String urlString) {

        logger.debug("httpGetURL: " + urlString);
        String json = null;

        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            int status = conn.getResponseCode();
            String contentType = conn.getHeaderField("Content-Type");

            logger.debug("Status code:{} contentType:{} ", status, contentType);

            logger.trace("Content-------start-----");
            String inputLine;
            json = "";

            while ((inputLine = reader.readLine()) != null) {
                // logger.debug(inputLine);
                json += inputLine;
            }
            // logger.debug("Content-------end-----");
            logger.trace("Received content: {}", json);
            in.close();
        } catch (Exception e) {
            logger.error("Failed when talking to myverisure", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return json;

    }

    private synchronized void logIn() {
        logger.debug("Attempting to log in to mypages.verisure.com");

        String url = BASEURL + LOGON_SUF;
        String source = sendHTTPpost(url, authstring);

        // logger.debug(source);

        url = BASEURL + START_SUF;
        source = httpGet(url);
        csrf = getCsrfToken2(source);

        logger.trace("Got CSRF: " + csrf);
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
            // logger.debug("Status = " + status);
            // String key;
            // logger.debug("Headers-------start-----");
            // for (int i = 1; (key = conn.getHeaderFieldKey(i)) != null; i++) {
            // logger.debug(key + ":" + conn.getHeaderField(i));
            // }
            // logger.debug("Headers-------end-----");
            logger.trace("Content-------start-----");
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                logger.trace(inputLine);
            }
            logger.trace("Content-------end-----");
            in.close();

            return inputLine;
        } catch (Exception e) {
            logger.warn("had an exception" + e.toString());
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
            // Map<String, List<String>> headers = conn.getHeaderFields();
            // for (String header : headers.keySet()) {
            // logger.debug("Response header {}: value {}", header, headers.get(header));
            // }
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
            logger.warn("Error:" + e);
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
                    logger.warn("Interupted waiting for new login ", e);
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

    public VerisureAlarmJSON getDoorStatus() {
        // TODO Auto-generated method stub
        return doorData;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openhab.binding.tellstick.handler.TelldusBridgeHandlerIntf#registerDeviceStatusListener(org.openhab.binding.
     * tellstick.handler.DeviceStatusListener)
     */
    public boolean registerDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
        if (deviceStatusListener == null) {
            throw new NullPointerException("It's not allowed to pass a null deviceStatusListener.");
        }
        boolean result = deviceStatusListeners.add(deviceStatusListener);
        if (result) {
            // onUpdate();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openhab.binding.tellstick.handler.TelldusBridgeHandlerIntf#unregisterDeviceStatusListener(org.openhab.binding
     * .tellstick.handler.DeviceStatusListener)
     */
    public boolean unregisterDeviceStatusListener(DeviceStatusListener deviceStatusListener) {
        boolean result = deviceStatusListeners.remove(deviceStatusListener);
        if (result) {
            // onUpdate();
        }
        return result;
    }

}
