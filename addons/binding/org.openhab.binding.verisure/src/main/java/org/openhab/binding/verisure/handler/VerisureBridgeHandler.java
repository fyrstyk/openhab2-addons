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
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
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

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_BRIDGE);

    private Logger logger = LoggerFactory.getLogger(VerisureBridgeHandler.class);

    private List<ObjectStatusListener> objectStatusListeners = new CopyOnWriteArrayList<>();

    private BigDecimal refresh;
    private String authstring;

    ScheduledFuture<?> refreshJob;

    private VerisureSession session = null;

    Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            logger.debug("VerisureBridgeHandler - Thread is up and running!");
            try {
                // boolean success = updateVerisureData();
                boolean success = session.refresh();
                if (success) {
                    ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_ALARM_STATUS);
                    updateState(cuid, session.getAlarmStatus());

                    logger.debug("alarmstatusnumeric is: " + session.getAlarmStatusNumeric());
                    cuid = new ChannelUID(getThing().getUID(), CHANNEL_ALARM_STATUS_NUMERIC);
                    updateState(cuid, session.getAlarmStatusNumeric());

                    cuid = new ChannelUID(getThing().getUID(), CHANNEL_ALARMCHANGERNAME);
                    updateState(cuid, session.getAlarmChangerName());

                    cuid = new ChannelUID(getThing().getUID(), CHANNEL_ALARMTIMESTAMP);
                    updateState(cuid, session.getAlarmTimestamp());

                }
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            }
            logger.debug("Thread finished");
        }
    };

    public VerisureBridgeHandler(Bridge bridge) {
        super(bridge);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.error("Supposed to handle a command" + command.toString());
        if (channelUID.getId().equals(CHANNEL_ALARM_STATUS_NUMERIC)) {

            if (command.toString().equals("0")) {
                logger.debug("attempting to turn off alarm!");
                session.disarmAlarm();
                ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_ALARM_STATUS);
                updateState(cuid, new StringType("pending"));
            } else if (command.toString().equals("1")) {
                logger.debug("arming at home");
                session.armHomeAlarm();
                ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_ALARM_STATUS);
                updateState(cuid, new StringType("pending"));

            } else if (command.toString().equals("2")) {
                logger.debug("arming away!");
                session.armAwayAlarm();
                ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_ALARM_STATUS);
                updateState(cuid, new StringType("pending"));
            } else {
                logger.debug("unknown command!");
            }
        }
    }

    public VerisureSession getSession() {
        return session;
    }

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");

        logger.debug("Initializing Verisure Binding");
        updateStatus(ThingStatus.INITIALIZING);
        super.initialize();

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

        session = new VerisureSession();
        session.initialize(authstring);

        startAutomaticRefresh();
    }

    private void startAutomaticRefresh() {

        logger.debug("Scheduling at fixed rate");
        refreshJob = scheduler.scheduleAtFixedRate(pollingRunnable, 30, refresh.intValue(), TimeUnit.SECONDS);
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

    public boolean registerObjectStatusListener(ObjectStatusListener objectStatusListener) {
        logger.debug("this method is not implemented");
        if (objectStatusListener == null) {
            throw new NullPointerException("It's not allowed to pass a null ObjectStatusListener.");
        }
        boolean result = objectStatusListeners.add(objectStatusListener);
        if (result) {
            onUpdate();
            // inform the listener initially about all lights and their states
            /*
             * for (FullLight light : lastLightStates.values()) {
             * lightStatusListener.onLightAdded(bridge, light);
             * }
             */

            logger.debug("some code should probably go here");
        }
        return result;
    }

    private synchronized void onUpdate() {
        logger.debug("VerisureBridgeHandler:onUpdate()");
        if (session != null) {
            if (refreshJob == null || refreshJob.isCancelled()) {
                refreshJob = scheduler.scheduleAtFixedRate(pollingRunnable, 1, refresh.intValue(), TimeUnit.SECONDS);
            }
        }
    }
}
