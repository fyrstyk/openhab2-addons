package org.openhab.binding.verisure.handler;

import static org.openhab.binding.verisure.VerisureBindingConstants.*;

import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.verisure.VerisureBindingConstants;
import org.openhab.binding.verisure.internal.DeviceStatusListener;
import org.openhab.binding.verisure.internal.VerisureAlarmJSON;
import org.openhab.binding.verisure.internal.VerisureBaseObjectJSON;
import org.openhab.binding.verisure.internal.VerisureSensorJSON;
import org.openhab.binding.verisure.internal.VerisureSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class VerisureObjectHandler extends BaseThingHandler implements DeviceStatusListener {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_ALARM,
            THING_TYPE_SMARTPLUG, THING_TYPE_CLIMATESENSOR, THING_TYPE_DOOR);

    private Logger logger = LoggerFactory.getLogger(VerisureObjectHandler.class);

    private VerisureSession session = null;

    private String id = null;
    ScheduledFuture<?> refreshJob;

    public VerisureObjectHandler(Thing thing) {
        super(thing);
        this.id = thing.getUID().getId();

    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Supposed to handle command for object handler: " + command.toString());
        if (command instanceof RefreshType) {
            update(session.getVerisureObject(this.id));
        } else if (channelUID.getId().equals(CHANNEL_SETSTATUS)) {
            handleChangeDoorState(command);
            session.refresh();
        } else {
            logger.warn("unknown command! {}", command);
        }
    }

    private void handleChangeDoorState(Command command) {
        if (command.toString().equals("0")) {
            logger.debug("attempting to turn off alarm!");
            session.unLockDoor(this.id);
            ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_STATUS);
            updateState(cuid, new StringType("pending"));
        } else if (command.toString().equals("1")) {
            logger.debug("arming at home");
            session.lockDoor(this.id);
            ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_STATUS);
            updateState(cuid, new StringType("pending"));

        } else if (command.toString().equals("2")) {
            logger.debug("arming away!");
            session.lockDoor(this.id);
            ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_STATUS);
            updateState(cuid, new StringType("pending"));
        } else {
            logger.debug("unknown command!");
        }
    }

    @Override
    public void initialize() {
        // Do not go online
        if (getBridge() != null) {
            this.bridgeStatusChanged(getBridge().getStatusInfo());
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            VerisureBridgeHandler vbh = (VerisureBridgeHandler) this.getBridge().getHandler();
            session = vbh.getSession();
            update(session.getVerisureObject(this.id));
            vbh.registerObjectStatusListener(this);
        }
        super.bridgeStatusChanged(bridgeStatusInfo);
    }

    public synchronized void update(VerisureBaseObjectJSON object) {
        logger.trace("VerisureObjectHandler:update()");

        if (getThing().getThingTypeUID().equals(THING_TYPE_CLIMATESENSOR)) {
            logger.trace("this is a climate sensor");
            logger.trace("getid: " + getThing().getUID().getId());
            VerisureSensorJSON obj = (VerisureSensorJSON) object;
            updateStatus(ThingStatus.ONLINE);
            updateSensorState(obj);

        } else if (getThing().getThingTypeUID().equals(THING_TYPE_DOOR)) {
            VerisureAlarmJSON obj = (VerisureAlarmJSON) object;
            updateStatus(ThingStatus.ONLINE);
            updateDoorState(obj);
        } else {
            logger.warn("cant handle this thingtypeuid: {}", getThing().getThingTypeUID());

        }

    }

    private void updateSensorState(VerisureSensorJSON obj) {
        ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_TEMPERATURE);
        ChannelUID huid = new ChannelUID(getThing().getUID(), CHANNEL_HUMIDITY);
        ChannelUID luid = new ChannelUID(getThing().getUID(), CHANNEL_LASTUPDATE);
        ChannelUID loid = new ChannelUID(getThing().getUID(), CHANNEL_LOCATION);
        String val;
        val = obj.getTemperature().substring(0, obj.getTemperature().length() - 6).replace(",", ".");
        logger.trace("Val is: " + val);

        DecimalType number = new DecimalType(val);
        updateState(cuid, number);
        if (obj.getHumidity() != null && obj.getHumidity().length() > 1) {
            val = obj.getHumidity().substring(0, obj.getHumidity().indexOf("%")).replace(",", ".");
            PercentType hnumber = new PercentType(val);
            updateState(huid, hnumber);
        }
        StringType lastUpdate = new StringType(obj.getTimestamp());
        updateState(luid, lastUpdate);
        StringType location = new StringType(obj.getLocation());
        updateState(loid, location);
    }

    private void updateDoorState(VerisureAlarmJSON status) {
        ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_STATUS);
        updateState(cuid, new StringType(status.getStatus()));

        cuid = new ChannelUID(getThing().getUID(), CHANNEL_CHANGERNAME);
        updateState(cuid, new StringType(status.getName()));

        cuid = new ChannelUID(getThing().getUID(), CHANNEL_TIMESTAMP);
        updateState(cuid, new StringType(status.getDate()));

        cuid = new ChannelUID(getThing().getUID(), CHANNEL_LOCATION);
        updateState(cuid, new StringType(status.getLocation()));

        cuid = new ChannelUID(getThing().getUID(), VerisureBindingConstants.CHANNEL_STATUS_LOCALIZED);
        updateState(cuid, new StringType(status.getLabel()));
    }

    @Override
    public void onDeviceStateChanged(VerisureBaseObjectJSON updateObject) {
        if (updateObject.getId().equals(this.id)) {
            update(updateObject);
        }

    }

    @Override
    public void onDeviceRemoved(VerisureBaseObjectJSON updateObject) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDeviceAdded(VerisureBaseObjectJSON updateObject) {
        // TODO Auto-generated method stub

    }
}
