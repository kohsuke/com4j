# com4j Event Handling

This document explains how you can subscribe to COM events.

## Event Interface Definition

To subscribe to COM events, you first need an interface that defines the IID of the event interface,
plus event methods that you'd like to subscribe, such as this:

    @IID("{5846EB78-317E-4B6F-B0C3-11EE8C8FEEF2}")
    public interface _IiTunesEvents {
        /**
         * Fired when a database change occurs.
         */
        @DISPID(1)
        void onDatabaseChangedEvent(Object deletedObjectIDs, Object changedObjectIDs);

        /**
         * Fired when a track has started playing.
         */
        @DISPID(2)
        void onPlayerPlayEvent(Object iTrack);
    }

An event interface does not have to list all the event methods; if you omit some methods, those events
will be simply ignored. Also, technically the event interface can be a class &mdash; the only thing
required is the `@IID` annotation and `@DISPID` that designates which are event methods.

To make it easy to subscribe to COM events, tlbimp generate a class with empty methods.
But you can also choose to write it manually.

## Subscribe/Unsubscribe to events

You can subscribe to a COM object by using the following code:

    Com4jObject comObject = ...; // more likely it's an interface derived from Com4jObject
    EventCookie cookie = comObject.advise(_IiTunesEvents.class, new MyEventReceiver());

    ...

    cookie.close(); // terminate subscription

Because of the reference counting in COM, you must explicitly perform unsubscription
by using the `close` method, or otherwise both COM and Java objects will leak.

## Event and Thread

If your thread X invokes a COM method, which in turn fires an event Y, then the thread
that executes the event method Y will not be X, but a thread that com4j maintains internally.
So the acess to thread local resources need to be done carefully.
But note that there's no need of synchronization --- thread X blocks until your event code returns.

For some other kinds of events (such as iTunes playerStart/playerStop events), they
happen without you calling into COM first. Those events are delivered in
truly asynchronous ways, so you will need synchronization.