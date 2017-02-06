/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.verisure.handler;

import static org.openhab.binding.verisure.VerisureBindingConstants.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.verisure.internal.DeviceStatusListener;
import org.openhab.binding.verisure.internal.VerisureSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VerisureBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author l3rum - Initial contribution
 */
public class VerisureBridgeHandler extends BaseBridgeHandler {

    private static final int REFRESH_DELAY = 10;

    @Override
    protected void updateThing(Thing thing) {
        // TODO Auto-generated method stub
        super.updateThing(thing);
    }

    @Override
    protected void updateConfiguration(Configuration configuration) {
        // TODO Auto-generated method stub
        stopAutomaticRefresh();
        super.updateConfiguration(configuration);
        initialize();
    }

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_BRIDGE);

    private Logger logger = LoggerFactory.getLogger(VerisureBridgeHandler.class);

    private BigDecimal refresh;
    private String authstring;

    ScheduledFuture<?> refreshJob;
    ScheduledFuture<?> immediateRefreshJob;

    private VerisureSession session = null;

    Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            logger.debug("VerisureBridgeHandler - Thread is up and running!");
            try {
                // boolean success = updateVerisureData();
                boolean success = session.refresh();
                if (success) {
                    updateStatus(ThingStatus.ONLINE);
                    updateAlarmState();

                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                }
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            }
            logger.debug("Thread finished");
        }
    };

    public VerisureBridgeHandler(Bridge bridge) {
        super(bridge);
        session = new VerisureSession();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.info("Supposed to handle a command {} ", command);
        if (command instanceof RefreshType) {
            updateAlarmState();
            scheduleImmediateRefresh();
        } else if (channelUID.getId().equals(CHANNEL_STATUS_NUMERIC)) {

            if (command.toString().equals("0")) {
                logger.debug("attempting to turn off alarm!");
                session.disarmAlarm();
                ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_STATUS);
                updateState(cuid, new StringType("pending"));
            } else if (command.toString().equals("1")) {
                logger.debug("arming at home");
                session.armHomeAlarm();
                ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_STATUS);
                updateState(cuid, new StringType("pending"));

            } else if (command.toString().equals("2")) {
                logger.debug("arming away!");
                session.armAwayAlarm();
                ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_STATUS);
                updateState(cuid, new StringType("pending"));
            } else {
                logger.debug("unknown command!");
            }
        } else {
            logger.warn("unknown command! {}", command);
        }
    }

    private void scheduleImmediateRefresh() {
        // We schedule in 10 sec, to avoid multiple updates
        logger.debug("Current remaining delay {}", refreshJob.getDelay(TimeUnit.SECONDS));
        if (refreshJob.getDelay(TimeUnit.SECONDS) > REFRESH_DELAY) {
            if (immediateRefreshJob == null || immediateRefreshJob.isDone()) {
                immediateRefreshJob = scheduler.schedule(pollingRunnable, REFRESH_DELAY, TimeUnit.SECONDS);
            }
        }
    }

    public VerisureSession getSession() {
        return session;
    }

    @Override
    public void initialize() {

        logger.debug("Initializing Verisure Binding");
        Configuration config = getThing().getConfiguration();

        // Get refresh
        try {
            refresh = (BigDecimal) config.get("refresh");
        } catch (Exception e) {
        }

        if (refresh == null) {
            // let's go for the default
            refresh = new BigDecimal(600);
        }

        // Get auth string
        try {
            authstring = (String) config.get("authstring");
        } catch (Exception e) {
        }

        if (authstring == null) {
            // let's go for the default
            logger.error("Could not get authentication String");
            // Failed init. Return
            return;
        }
        try {

            session.initialize(authstring);

            startAutomaticRefresh();
        } catch (Error e) {
            logger.error("Failed", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        }
    }

    private void stopAutomaticRefresh() {
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
    }

    private void startAutomaticRefresh() {
        if (refreshJob == null || refreshJob.isCancelled()) {
            logger.debug("Scheduling at fixed rate");
            refreshJob = scheduler.scheduleAtFixedRate(pollingRunnable, REFRESH_DELAY, refresh.intValue(),
                    TimeUnit.SECONDS);
        }
    }

    @Override
    public void dispose() {
        logger.debug("Handler disposed.");
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
        session.dispose();
        session = null;
    }

    public boolean registerObjectStatusListener(DeviceStatusListener deviceStatusListener) {
        return session.registerDeviceStatusListener(deviceStatusListener);
    }

    /**
     *
     */
    private void updateAlarmState() {
        try {
            ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_STATUS);
            updateState(cuid, session.getAlarmStatus());

            logger.debug("alarmstatusnumeric is: " + session.getAlarmStatusNumeric());
            cuid = new ChannelUID(getThing().getUID(), CHANNEL_STATUS_NUMERIC);
            updateState(cuid, session.getAlarmStatusNumeric());

            cuid = new ChannelUID(getThing().getUID(), CHANNEL_CHANGERNAME);
            updateState(cuid, session.getAlarmChangerName());

            cuid = new ChannelUID(getThing().getUID(), CHANNEL_TIMESTAMP);
            updateState(cuid, session.getAlarmTimestamp());
        } catch (Exception e) {
            logger.error("Failed to update state ", e);
        }
    }
}
