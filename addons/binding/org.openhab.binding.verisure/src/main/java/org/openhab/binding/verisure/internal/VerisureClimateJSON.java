package org.openhab.binding.verisure.internal;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class VerisureClimateJSON {

    @SerializedName("temperatureBelowMinAlertValue")
    @Expose
    private String temperatureBelowMinAlertValue;
    @SerializedName("temperatureAboveMaxAlertValue")
    @Expose
    private String temperatureAboveMaxAlertValue;
    @SerializedName("temperature")
    @Expose
    private String temperature;
    @SerializedName("plottable")
    @Expose
    private Boolean plottable;
    @SerializedName("monitorable")
    @Expose
    private Boolean monitorable;
    @SerializedName("humidity")
    @Expose
    private String humidity;
    @SerializedName("location")
    @Expose
    private String location;
    @SerializedName("humidityBelowMinAlertValue")
    @Expose
    private String humidityBelowMinAlertValue;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("humidityAboveMaxAlertValue")
    @Expose
    private String humidityAboveMaxAlertValue;
    @SerializedName("timestamp")
    @Expose
    private String timestamp;

    /**
     * 
     * @return
     *         The temperatureBelowMinAlertValue
     */
    public String getTemperatureBelowMinAlertValue() {
        return temperatureBelowMinAlertValue;
    }

    /**
     * 
     * @param temperatureBelowMinAlertValue
     *            The temperatureBelowMinAlertValue
     */
    public void setTemperatureBelowMinAlertValue(String temperatureBelowMinAlertValue) {
        this.temperatureBelowMinAlertValue = temperatureBelowMinAlertValue;
    }

    /**
     * 
     * @return
     *         The temperatureAboveMaxAlertValue
     */
    public String getTemperatureAboveMaxAlertValue() {
        return temperatureAboveMaxAlertValue;
    }

    /**
     * 
     * @param temperatureAboveMaxAlertValue
     *            The temperatureAboveMaxAlertValue
     */
    public void setTemperatureAboveMaxAlertValue(String temperatureAboveMaxAlertValue) {
        this.temperatureAboveMaxAlertValue = temperatureAboveMaxAlertValue;
    }

    /**
     * 
     * @return
     *         The temperature
     */
    public String getTemperature() {
        return temperature;
    }

    /**
     * 
     * @param temperature
     *            The temperature
     */
    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    /**
     * 
     * @return
     *         The plottable
     */
    public Boolean getPlottable() {
        return plottable;
    }

    /**
     * 
     * @param plottable
     *            The plottable
     */
    public void setPlottable(Boolean plottable) {
        this.plottable = plottable;
    }

    /**
     * 
     * @return
     *         The monitorable
     */
    public Boolean getMonitorable() {
        return monitorable;
    }

    /**
     * 
     * @param monitorable
     *            The monitorable
     */
    public void setMonitorable(Boolean monitorable) {
        this.monitorable = monitorable;
    }

    /**
     * 
     * @return
     *         The humidity
     */
    public String getHumidity() {
        return humidity;
    }

    /**
     * 
     * @param humidity
     *            The humidity
     */
    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    /**
     * 
     * @return
     *         The location
     */
    public String getLocation() {
        return location;
    }

    /**
     * 
     * @param location
     *            The location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 
     * @return
     *         The humidityBelowMinAlertValue
     */
    public String getHumidityBelowMinAlertValue() {
        return humidityBelowMinAlertValue;
    }

    /**
     * 
     * @param humidityBelowMinAlertValue
     *            The humidityBelowMinAlertValue
     */
    public void setHumidityBelowMinAlertValue(String humidityBelowMinAlertValue) {
        this.humidityBelowMinAlertValue = humidityBelowMinAlertValue;
    }

    /**
     * 
     * @return
     *         The id
     */
    public String getId() {
        return id;
    }

    /**
     * 
     * @param id
     *            The id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 
     * @return
     *         The type
     */
    public String getType() {
        return type;
    }

    /**
     * 
     * @param type
     *            The type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 
     * @return
     *         The humidityAboveMaxAlertValue
     */
    public String getHumidityAboveMaxAlertValue() {
        return humidityAboveMaxAlertValue;
    }

    /**
     * 
     * @param humidityAboveMaxAlertValue
     *            The humidityAboveMaxAlertValue
     */
    public void setHumidityAboveMaxAlertValue(String humidityAboveMaxAlertValue) {
        this.humidityAboveMaxAlertValue = humidityAboveMaxAlertValue;
    }

    /**
     * 
     * @return
     *         The timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * 
     * @param timestamp
     *            The timestamp
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}
