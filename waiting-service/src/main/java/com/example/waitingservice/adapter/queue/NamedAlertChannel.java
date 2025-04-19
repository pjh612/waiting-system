package com.example.waitingservice.adapter.queue;


import com.alert.core.messaging.model.AlertChannel;

public class NamedAlertChannel implements AlertChannel {
    private final String queueName;

    public NamedAlertChannel(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public String name() {
        return this.queueName;
    }
}
