
package org.openhab.binding.verisure.discovery;

import static org.openhab.binding.verisure.VerisureBindingConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.verisure.handler.VerisureBridgeHandler;
import org.openhab.binding.verisure.handler.VerisureObjectHandler;
import org.openhab.binding.verisure.internal.VerisureAlarmJSON;
import org.openhab.binding.verisure.internal.VerisureBaseObjectJSON;
import org.openhab.binding.verisure.internal.VerisureSensorJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class VerisureObjectDiscoveryService extends AbstractDiscoveryService {
    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets
            .union(VerisureBridgeHandler.SUPPORTED_THING_TYPES, VerisureObjectHandler.SUPPORTED_THING_TYPES);

    private Logger logger = LoggerFactory.getLogger(VerisureObjectDiscoveryService.class);

    private final static int SEARCH_TIME = 60;

    private VerisureBridgeHandler verisureBridgeHandler;

    public VerisureObjectDiscoveryService(VerisureBridgeHandler bridgeHandler) throws IllegalArgumentException {
        // super(SEARCH_TIME);
        super(SUPPORTED_THING_TYPES, SEARCH_TIME);

        this.verisureBridgeHandler = bridgeHandler;

    }

    @Override
    public void startScan() {
        removeOlderResults(getTimestampOfLastScan());
        logger.debug("VerisureObjectDiscoveryService:startScan");

        HashMap<String, VerisureBaseObjectJSON> verisureObjects = verisureBridgeHandler.getSession()
                .getVerisureObjects();

        for (Map.Entry<String, VerisureBaseObjectJSON> entry : verisureObjects.entrySet()) {

            System.out.println(
                    "Checking if " + entry.getValue().getId() + " with Key: " + entry.getKey() + "is a thing!");

            String test = entry.getValue().getId().trim();
            System.out.println("check if we can get object based on copy of key:" + verisureObjects.get(test));

            onObjectAddedInternal(entry.getValue());
        }
    }

    private void onObjectAddedInternal(VerisureBaseObjectJSON value) {
        logger.debug("VerisureObjectDiscoveryService:OnObjectAddedInternal");
        ThingUID thingUID = getThingUID(value);
        if (thingUID != null) {
            ThingUID bridgeUID = verisureBridgeHandler.getThing().getUID();
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(DOOR, value.getId());

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withBridge(bridgeUID).withLabel(value.getLocation()).build();

            logger.debug("thinguid: " + thingUID.toString());
            logger.debug("withproperties: " + properties.toString());
            logger.debug("with bridgeuid: " + bridgeUID);
            logger.debug("with label: " + value.getName());

            logger.debug("thingDiscovered: " + discoveryResult);
            thingDiscovered(discoveryResult);
        } else {
            logger.debug("discovered unsupported light of type '{}' with id {}", value.getId(), value.getId());
        }

    }

    private void onObjectAddedInternal(VerisureSensorJSON value) {
        logger.debug("VerisureObjectDiscoveryService:OnObjectAddedInternal");
        ThingUID thingUID = getThingUID(value);
        if (thingUID != null) {
            ThingUID bridgeUID = verisureBridgeHandler.getThing().getUID();
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(CLIMATESENSOR_ID, value.getId());

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withBridge(bridgeUID).withLabel(value.getLocation()).build();

            logger.debug("thinguid: " + thingUID.toString());
            logger.debug("withproperties: " + properties.toString());
            logger.debug("with bridgeuid: " + bridgeUID);
            logger.debug("with label: " + value.getName());

            logger.debug("thingDiscovered: " + discoveryResult);
            thingDiscovered(discoveryResult);
        } else {
            logger.debug("discovered unsupported light of type '{}' with id {}", value.getId(), value.getId());
        }

    }

    public void activate() {
    }

    private ThingUID getThingUID(VerisureBaseObjectJSON voj) {
        ThingUID bridgeUID = verisureBridgeHandler.getThing().getUID();

        ThingUID tuid = null;
        if (voj instanceof VerisureAlarmJSON) {
            tuid = new ThingUID(THING_TYPE_DOOR, bridgeUID, voj.getId().replaceAll("[^a-zA-Z0-9_]", "_"));
        } else {
            tuid = new ThingUID(THING_TYPE_CLIMATESENSOR, bridgeUID, voj.getId().replaceAll("[^a-zA-Z0-9_]", "_"));
        }

        return tuid;
    }

}
