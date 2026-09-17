package org.dreamscape_lab.emf_compat_basic_weapons.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import strm.emfcompat.core.PoseManager;

public class Emf_compat_basic_weaponsClient implements ClientModInitializer {

    public static final String POSE_SOURCE = "emf_compat_basic_weapons:pike";

    @Override
    public void onInitializeClient() {
        // Beat passive sources (sit/glide/etc.)
        PoseManager.setSourcePriority(POSE_SOURCE, 100);
        ClientTickEvents.END_CLIENT_TICK.register(PolearmAttackGate::tick);
    }
}
