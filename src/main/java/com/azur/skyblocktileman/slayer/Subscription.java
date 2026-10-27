package com.azur.skyblocktileman.slayer;

/** Handle returned when subscribing, so a listener can be removed again. */
@FunctionalInterface
public interface Subscription {

    void unsubscribe();

    Subscription NOOP = () -> {};
}
