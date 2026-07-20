package com.example.client;

import com.example.client.tileman.TilemanState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class ExampleModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.

        // Loads (or creates) the Tileman save file as soon as the game starts.
        TilemanState.getInstance();

        // Make sure the latest state is flushed to disk when the game closes.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
            TilemanState.getInstance().save()
        );
    }
}
