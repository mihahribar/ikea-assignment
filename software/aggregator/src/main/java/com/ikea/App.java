package com.ikea;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;

import java.util.ArrayList;
import java.util.List;

public class App implements RequestHandler<SNSEvent, List<String>>
{
    public List<String> handleRequest(SNSEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("EVENT TYPE: " + event.getClass().toString());
        List<String> messagesFound = new ArrayList<>();
        for (SNSEvent.SNSRecord record : event.getRecords()) {
            SNSEvent.SNS message = record.getSNS();
            messagesFound.add(message.getMessage());
        }
        return messagesFound;
    }
}
