
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
import org.openhab.binding.verisure.handler.ObjectStatusListener;
import org.openhab.binding.verisure.handler.VerisureBridgeHandler;
import org.openhab.binding.verisure.handler.VerisureObjectHandler;
import org.openhab.binding.verisure.internal.VerisureObjectJSON;
import org.openhab.binding.verisure.internal.VerisureSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class VerisureObjectDiscoveryService extends AbstractDiscoveryService implements ObjectStatusListener {
    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets
            .union(VerisureBridgeHandler.SUPPORTED_THING_TYPES, VerisureObjectHandler.SUPPORTED_THING_TYPES);

    private Logger logger = LoggerFactory.getLogger(VerisureBridgeHandler.class);

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

        HashMap<String, VerisureObjectJSON> verisureObjects = verisureBridgeHandler.getSession().getVerisureObjects();

        for (Map.Entry<String, VerisureObjectJSON> entry : verisureObjects.entrySet()) {

            System.out.println(
                    "Checking if " + entry.getValue().getId() + " with Key: " + entry.getKey() + "is a thing!");

            String test = entry.getValue().getId().trim();
            System.out.println("check if we can get object based on copy of key:" + verisureObjects.get(test));

            onObjectAddedInternal(entry.getValue());
        }

    }

    private void onObjectAddedInternal(VerisureObjectJSON value) {
        logger.debug("VerisureObjectDiscoveryService:OnObjectAddedInternal");
        ThingUID thingUID = getThingUID(value);
        if (thingUID != null) {
            ThingUID bridgeUID = verisureBridgeHandler.getThing().getUID();
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(CLIMATESENSOR_ID, value.getId());

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withBridge(bridgeUID).withLabel(value.getName()).build();

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
        verisureBridgeHandler.registerObjectStatusListener(this);
    }

    @Override
    public void onLightStateChanged(VerisureSession session, String serialnumber) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onLightRemoved(VerisureSession session, String serialnumber) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onLightAdded(VerisureSession session, String serialnumber) {
        // TODO Auto-generated method stub

    }

    private ThingUID getThingUID(VerisureObjectJSON voj) {
        ThingUID bridgeUID = verisureBridgeHandler.getThing().getUID();
        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, voj.getModelID());

        ThingUID tuid = new ThingUID(thingTypeUID, bridgeUID, voj.getId().replaceAll("[^a-zA-Z0-9_]", "_"));

        return tuid;
    }

}
