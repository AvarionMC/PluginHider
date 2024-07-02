package org.avarion.plugin_hider.util;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;

public class ReceivedPackets {
    public final ZonedDateTime created = ZonedDateTime.now(ZoneOffset.UTC);
    private final int maxSecondsDelay;
    private final ArrayList<String> receivedMessages = new ArrayList<>();

    public ReceivedPackets(int maxSecondsDelay) {
        this.maxSecondsDelay = maxSecondsDelay;
    }

    public boolean isStale() {
        return created.isBefore(ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(maxSecondsDelay));
    }

    public void addSystemChatLine(String line) {
        receivedMessages.add(line);
    }

    public boolean isFinished() {
        return false;
        //return !receivedMessages.isEmpty();
    }
}
