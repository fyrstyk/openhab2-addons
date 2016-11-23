package org.openhab.binding.verisure.handler;

import static org.openhab.binding.verisure.VerisureBindingConstants.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.verisure.internal.VerisureObjectJSON;
import org.openhab.binding.verisure.internal.VerisureSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class VerisureObjectHandler extends BaseThingHandler {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_ALARM,
            THING_TYPE_SMARTPLUG, THING_TYPE_CLIMATESENSOR);

    private Logger logger = LoggerFactory.getLogger(VerisureBridgeHandler.class);

    private BigDecimal refresh;

    private VerisureSession session = null;

    ScheduledFuture<?> refreshJob;

    Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            logger.debug("VerisureObjectHandler:Thread is up and running!");
            try {
                update();

            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
            }
            logger.debug("erisureObjectHandler:Thread finished");
        }
    };

    public VerisureObjectHandler(Thing thing) {
        super(thing);

    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Supposed to handle command for object handler: " + command.toString());

    }

    @Override
    public void initialize() {
        super.initialize();

        

    }
    public void bridgeHandlerInitialized(ThingHandler thingHandler, Bridge bridge) {
        VerisureBridgeHandler vbh = (VerisureBridgeHandler) this.getBridge().getHandler();
        session = vbh.getSession();

        // updateStatus(ThingStatus.INITIALIZING);

        refresh = new BigDecimal(60);

        refreshJob = scheduler.scheduleAtFixedRate(pollingRunnable, 10, refresh.intValue(), TimeUnit.SECONDS);
    }
    public synchronized void update() {
        logger.debug("VerisureObjectHandler:update()");

        if (getThing().getThingTypeUID().toString().equals("verisure:climatesensor")) {
            logger.debug("this is a climate sensor");
            logger.debug("getid: " + getThing().getUID().getId());
            ChannelUID cuid = new ChannelUID(getThing().getUID(), CHANNEL_TEMPERATURE);

            VerisureObjectJSON obj = session.getVerisureObject(this.getThing().getUID().getId());

            String val = null;

            if (obj == null) {
                val = "0.0";
                updateStatus(ThingStatus.OFFLINE);
            } else {
                val = obj.getTemperature().substring(0, obj.getTemperature().length() - 6).replace(",", ".");
                updateStatus(ThingStatus.ONLINE);
            }
            logger.debug("Val is: " + val);

            DecimalType number = new DecimalType(val);
            updateState(cuid, number);

        } else {
            logger.debug("cant handle this thingtypeuid: " + getThing().getThingTypeUID());
        }

    }
}
