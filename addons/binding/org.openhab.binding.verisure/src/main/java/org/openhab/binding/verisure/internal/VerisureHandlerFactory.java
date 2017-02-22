/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.verisure.internal;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.verisure.discovery.VerisureObjectDiscoveryService;
import org.openhab.binding.verisure.handler.VerisureBridgeHandler;
import org.openhab.binding.verisure.handler.VerisureObjectHandler;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * The {@link VerisureHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author l3rum - Initial contribution
 */
public class VerisureHandlerFactory extends BaseThingHandlerFactory {

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets
            .union(VerisureBridgeHandler.SUPPORTED_THING_TYPES, VerisureObjectHandler.SUPPORTED_THING_TYPES);

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    private Logger logger = LoggerFactory.getLogger(VerisureBridgeHandler.class);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        logger.debug("trying to create handler for thing:" + thing.getUID());
        if (VerisureBridgeHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {

            logger.debug("creating handler for:" + thing.toString());

            VerisureBridgeHandler handler = new VerisureBridgeHandler((Bridge) thing);
            registerObjectDiscoveryService(handler);

            return handler;

        } else if (VerisureObjectHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {

            logger.debug("Created handler for typeUID:  " + thing.getThingTypeUID());
            return new VerisureObjectHandler(thing);
        }
        return null;

    }

    private synchronized void registerObjectDiscoveryService(VerisureBridgeHandler bridgeHandler) {
        VerisureObjectDiscoveryService discoveryService = new VerisureObjectDiscoveryService(bridgeHandler);
        discoveryService.activate();
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

}
