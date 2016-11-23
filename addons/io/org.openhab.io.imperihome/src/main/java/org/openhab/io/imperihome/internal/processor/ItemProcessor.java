/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.imperihome.internal.processor;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.ItemRegistryChangeListener;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.io.imperihome.internal.action.ActionRegistry;
import org.openhab.io.imperihome.internal.model.device.AbstractDevice;
import org.openhab.io.imperihome.internal.model.device.AbstractNumericValueDevice;
import org.openhab.io.imperihome.internal.model.device.Co2SensorDevice;
import org.openhab.io.imperihome.internal.model.device.DeviceType;
import org.openhab.io.imperihome.internal.model.device.DimmerDevice;
import org.openhab.io.imperihome.internal.model.device.ElectricityDevice;
import org.openhab.io.imperihome.internal.model.device.GenericSensorDevice;
import org.openhab.io.imperihome.internal.model.device.HygrometryDevice;
import org.openhab.io.imperihome.internal.model.device.LockDevice;
import org.openhab.io.imperihome.internal.model.device.LuminosityDevice;
import org.openhab.io.imperihome.internal.model.device.MultiSwitchDevice;
import org.openhab.io.imperihome.internal.model.device.NoiseDevice;
import org.openhab.io.imperihome.internal.model.device.PressureDevice;
import org.openhab.io.imperihome.internal.model.device.RainDevice;
import org.openhab.io.imperihome.internal.model.device.RgbLightDevice;
import org.openhab.io.imperihome.internal.model.device.SceneDevice;
import org.openhab.io.imperihome.internal.model.device.SwitchDevice;
import org.openhab.io.imperihome.internal.model.device.TempHygroDevice;
import org.openhab.io.imperihome.internal.model.device.TemperatureDevice;
import org.openhab.io.imperihome.internal.model.device.TrippableDevice;
import org.openhab.io.imperihome.internal.model.device.UvDevice;
import org.openhab.io.imperihome.internal.model.device.WindDevice;
import org.openhab.io.imperihome.internal.util.DigestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor of openHAB Items. Parses ISS tags and creates and registers {@link AbstractDevice} implementations where
 * applicable.
 *
 * @author Pepijn de Geus - Initial contribution
 */
public class ItemProcessor implements ItemRegistryChangeListener {

    private static final String PREFIX_ISS = "iss:";

    private final Logger logger = LoggerFactory.getLogger(ItemProcessor.class);

    private final ItemRegistry itemRegistry;
    private final DeviceRegistry deviceRegistry;
    private final ActionRegistry actionRegistry;

    public ItemProcessor(ItemRegistry itemRegistry, DeviceRegistry deviceRegistry, ActionRegistry actionRegistry) {
        this.itemRegistry = itemRegistry;
        this.deviceRegistry = deviceRegistry;
        this.actionRegistry = actionRegistry;

        allItemsChanged(null);
        itemRegistry.addRegistryChangeListener(this);
    }

    public void destroy() {
        itemRegistry.removeRegistryChangeListener(this);

        // Destroy all Devices (unregisters state listeners)
        for (Item item : itemRegistry.getItems()) {
            String deviceId = getDeviceId(item);
            if (deviceRegistry.hasDevice(deviceId)) {
                deviceRegistry.remove(deviceId).destroy();
            }
        }
    }

    private void parseItem(Item item) {
        Map<TagType, List<String>> issTags = getIssTags(item);
        if (!issTags.isEmpty()) {
            logger.debug("Found item {} with ISS tags: {}", item, issTags);

            DeviceType deviceType = getDeviceType(item, issTags);
            if (deviceType == null) {
                logger.warn("Unrecognized device type for item: {}", item);
            } else {
                AbstractDevice device = getDeviceInstance(deviceType, item);
                device.setId(getDeviceId(item));
                device.setName(getLabel(item, issTags));
                device.setInverted(isInverted(issTags));
                device.setActionRegistry(actionRegistry);

                setDeviceRoom(device, issTags);
                setDeviceLinks(device, item, issTags);
                setMapping(device, item, issTags);
                setUnit(device, issTags);

                // Set initial state
                device.stateUpdated(item, item.getState());

                logger.debug("Item parsed to device: {}", device);
                deviceRegistry.add(device);
            }
        }
    }

    private AbstractDevice getDeviceInstance(DeviceType deviceType, Item item) {
        switch (deviceType) {
            case SWITCH:
                return new SwitchDevice(item);
            case DIMMER:
                return new DimmerDevice(item);
            case RGB_LIGHT:
                return new RgbLightDevice(item);
            case TEMPERATURE:
                return new TemperatureDevice(item);
            case TEMP_HYGRO:
                return new TempHygroDevice(item);
            case LUMINOSITY:
                return new LuminosityDevice(item);
            case HYGROMETRY:
                return new HygrometryDevice(item);
            case CO2:
                return new Co2SensorDevice(item);
            case ELECTRICITY:
                return new ElectricityDevice(item);
            case SCENE:
                return new SceneDevice(item);
            case MULTI_SWITCH:
                return new MultiSwitchDevice(item);
            case GENERIC_SENSOR:
                return new GenericSensorDevice(item);
            case PRESSURE:
                return new PressureDevice(item);
            case UV:
                return new UvDevice(item);
            case NOISE:
                return new NoiseDevice(item);
            case RAIN:
                return new RainDevice(item);
            case WIND:
                return new WindDevice(item);
            case LOCK:
                return new LockDevice(item);
            case CO2_ALERT:
            case SMOKE:
            case DOOR:
            case MOTION:
            case FLOOD:
                return new TrippableDevice(deviceType, item);
            default:
                break;
        }

        throw new IllegalArgumentException("Unknown device type: " + deviceType);
    }

    private String getLabel(Item item, Map<TagType, List<String>> issTags) {
        if (issTags.containsKey(TagType.LABEL)) {
            return issTags.get(TagType.LABEL).get(0);
        }

        if (StringUtils.isNotBlank(item.getLabel())) {
            String label = item.getLabel().trim();
            if (label.matches("\\[.*\\]$")) {
                label = label.substring(0, label.indexOf("["));
            }
            return label;
        }

        return item.getName();
    }

    private boolean isInverted(Map<TagType, List<String>> issTags) {
        return issTags.containsKey(TagType.INVERT) && BooleanUtils.toBoolean(issTags.get(TagType.INVERT).get(0));
    }

    private void setDeviceRoom(AbstractDevice device, Map<TagType, List<String>> issTags) {
        String roomName = "No room";
        if (issTags.containsKey(TagType.ROOM)) {
            roomName = issTags.get(TagType.ROOM).get(0);
        }

        device.setRoom(DigestUtil.sha1(roomName));
        device.setRoomName(roomName);
    }

    private void setDeviceLinks(AbstractDevice device, Item item, Map<TagType, List<String>> issTags) {
        if (issTags.containsKey(TagType.LINK)) {
            // Pass device registry to device for linked device lookup
            device.setDeviceRegistry(deviceRegistry);

            // Parse link tags
            for (String link : issTags.get(TagType.LINK)) {
                String[] parts = link.split(":");
                if (parts.length == 2) {
                    device.addLink(parts[0].toLowerCase().trim(), parts[1].trim());
                } else {
                    logger.error("Item has incorrect link format (should be 'iss:link:<type>:<item>'): {}", item);
                }
            }
        }
    }

    /**
     * Parses a mapping tag, if it exists. Format: "iss:mapping:1=Foo,2=Bar,3=Foobar".
     */
    private void setMapping(AbstractDevice device, Item item, Map<TagType, List<String>> issTags) {
        if (issTags.containsKey(TagType.MAPPING)) {
            String mapItems = issTags.get(TagType.MAPPING).get(0);

            Map<String, String> mapping = new HashMap<>();
            for (String mapItem : mapItems.split(",")) {
                String[] keyVal = mapItem.split("=", 2);
                if (keyVal.length != 2) {
                    logger.error("Invalid mapping syntax for Item {}", item);
                    return;
                }
                mapping.put(keyVal[0].trim(), keyVal[1].trim());
            }

            device.setMapping(mapping);
        }
    }

    /**
     * Parses the unit tag, if it exists. Format: "iss:unit:°C".
     */
    private void setUnit(AbstractDevice device, Map<TagType, List<String>> issTags) {
        if (issTags.containsKey(TagType.UNIT)) {
            if (!(device instanceof AbstractNumericValueDevice)) {
                logger.warn("Unit tag is not supported for device {}", device);
                return;
            }

            ((AbstractNumericValueDevice) device).setUnit(issTags.get(TagType.UNIT).get(0));
        }
    }

    /**
     * Determines the Device type for the given Item. Uses the 'type' tag first, tries to auto-detect the type if no
     * such tag exists.
     */
    private DeviceType getDeviceType(Item item, Map<TagType, List<String>> issTags) {
        if (issTags.containsKey(TagType.TYPE)) {
            return DeviceType.forApiString(issTags.get(TagType.TYPE).get(0));
        }

        List<Class<? extends State>> acceptedDataTypes = item.getAcceptedDataTypes();
        String name = item.getName().toLowerCase();

        if (acceptedDataTypes.contains(DecimalType.class)) {
            if (name.contains("tempe")) {
                return DeviceType.TEMPERATURE;
            } else if (name.contains("lumi")) {
                return DeviceType.LUMINOSITY;
            } else if (name.contains("hygro")) {
                return DeviceType.HYGROMETRY;
            } else if (name.contains("wind")) {
                return DeviceType.WIND;
            } else {
                return DeviceType.GENERIC_SENSOR;
            }
        }

        if (acceptedDataTypes.contains(HSBType.class)) {
            return DeviceType.RGB_LIGHT;
        }

        if (acceptedDataTypes.contains(OpenClosedType.class)) {
            return DeviceType.DOOR;
        }
        if (acceptedDataTypes.contains(OnOffType.class)) {
            return DeviceType.SWITCH;
        }

        return null;
    }

    private Map<TagType, List<String>> getIssTags(Item item) {
        Map<TagType, List<String>> tags = new EnumMap<>(TagType.class);

        for (String tag : item.getTags()) {
            if (tag.startsWith(PREFIX_ISS)) {
                String issTag = tag.substring(PREFIX_ISS.length());
                for (TagType tagType : TagType.values()) {
                    if (issTag.startsWith(tagType.getPrefix() + ":")) {
                        String tagValue = issTag.substring(tagType.getPrefix().length() + 1);
                        if (!tags.containsKey(tagType)) {
                            tags.put(tagType, new LinkedList<String>());
                        } else if (!tagType.isMultiValue()) {
                            logger.error("Found multiple values for tag " + tagType.getPrefix()
                                    + " - only first value is used");
                        }
                        tags.get(tagType).add(tagValue);
                        break;
                    }
                }
            }
        }

        return tags;
    }

    /**
     * Removes the given item for the device list.
     *
     * @param item Item to remove.
     */
    private void removeItem(Item item) {
        removeItem(item.getName());
    }

    /**
     * Removes the given item for the device list.
     *
     * @param itemName Name of the Item to remove.
     */
    private void removeItem(String itemName) {
        String deviceId = getDeviceId(itemName);
        if (deviceRegistry.hasDevice(deviceId)) {
            logger.debug("Removing Device from ISS registry for Item: {}", itemName);
            deviceRegistry.remove(deviceId).destroy();
        }
    }

    /**
     * Generates an unique device ID for the given item.
     */
    public static String getDeviceId(Item item) {
        return getDeviceId(item.getName());
    }

    /**
     * Generates an unique device ID for the given item name.
     */
    public static String getDeviceId(String itemName) {
        return DigestUtil.sha1(itemName);
    }

    @Override
    public void added(Item item) {
        parseItem(item);
    }

    @Override
    public void removed(Item item) {
        removeItem(item);
    }

    @Override
    public void updated(Item oldItem, Item newItem) {
        removeItem(oldItem);
        parseItem(newItem);
    }

    @Override
    public void allItemsChanged(Collection<String> oldItems) {
        if (oldItems != null) {
            for (String oldItem : oldItems) {
                removeItem(oldItem);
            }
        }

        if (deviceRegistry.hasDevices()) {
            logger.warn("There are still Devices left after processing all Items from allItemsChanged(): "
                    + deviceRegistry.getDevices());
            deviceRegistry.clear();
        }

        for (Item item : itemRegistry.getItems()) {
            parseItem(item);
        }
    }

}
