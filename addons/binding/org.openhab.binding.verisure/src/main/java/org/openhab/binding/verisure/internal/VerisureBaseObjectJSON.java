package org.openhab.binding.verisure.internal;

import com.google.gson.annotations.SerializedName;

public class VerisureBaseObjectJSON {

    @SerializedName("date")

    protected String date;
    @SerializedName("notAllowedReason")

    protected String notAllowedReason;
    @SerializedName("name")

    protected String name;
    @SerializedName("changeAllowed")

    protected Boolean changeAllowed;
    @SerializedName("id")

    protected String id;
    @SerializedName("label")

    protected String label;
    @SerializedName("type")

    protected String type;
    @SerializedName("location")

    protected String location;
    @SerializedName("status")

    protected String status;

    public VerisureBaseObjectJSON() {
        super();
    }

    /**
     *
     * @return
     *         The status
     */
    public String getStatus() {
        return status;
    }

    /**
     *
     * @param status
     *            The status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the date
     */
    public String getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * @return the notAllowedReason
     */
    public String getNotAllowedReason() {
        return notAllowedReason;
    }

    /**
     * @param notAllowedReason the notAllowedReason to set
     */
    public void setNotAllowedReason(String notAllowedReason) {
        this.notAllowedReason = notAllowedReason;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the changeAllowed
     */
    public Boolean getChangeAllowed() {
        return changeAllowed;
    }

    /**
     * @param changeAllowed the changeAllowed to set
     */
    public void setChangeAllowed(Boolean changeAllowed) {
        this.changeAllowed = changeAllowed;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

}