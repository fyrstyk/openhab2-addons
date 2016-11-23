package org.openhab.binding.verisure.internal;

import javax.annotation.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class VerisureAlarmJSON {
    @SerializedName("date")
    @Expose
    private String date;
    @SerializedName("notAllowedReason")
    @Expose
    private String notAllowedReason;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("changeAllowed")
    @Expose
    private Boolean changeAllowed;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("label")
    @Expose
    private String label;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("status")
    @Expose
    private String status;

      @SerializedName("location")
    @Expose
    private String location;

    /**
     *
     * @return
     *         The date
     */
    public String getDate() {
        return date;
    }

    /**
     *
     * @param date
     *            The date
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     *
     * @return
     *         The notAllowedReason
     */
    public String getNotAllowedReason() {
        return notAllowedReason;
    }

    /**
     *
     * @param notAllowedReason
     *            The notAllowedReason
     */
    public void setNotAllowedReason(String notAllowedReason) {
        this.notAllowedReason = notAllowedReason;
    }

    /**
     *
     * @return
     *         The name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     *            The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     *         The changeAllowed
     */
    public Boolean getChangeAllowed() {
        return changeAllowed;
    }

    /**
     *
     * @param changeAllowed
     *            The changeAllowed
     */
    public void setChangeAllowed(Boolean changeAllowed) {
        this.changeAllowed = changeAllowed;
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
     *         The label
     */
    public String getLabel() {
        return label;
    }

    /**
     *
     * @param label
     *            The label
     */
    public void setLabel(String label) {
        this.label = label;
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
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


}
