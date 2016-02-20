package org.openhab.binding.verisure.handler;

import org.openhab.binding.verisure.internal.VerisureSession;

public interface ObjectStatusListener {
    public void onLightStateChanged(VerisureSession session, String serialnumber);

    public void onLightRemoved(VerisureSession session, String serialnumber);

    public void onLightAdded(VerisureSession session, String serialnumber);

}
