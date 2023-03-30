package com.ikea;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class App implements RequestHandler<SQSEvent, List<String>>
{
    public List<String> handleRequest(SQSEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("aggregator started");

        List<String> messageIds = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // aggregate messages and create a list of
        for (SQSEvent.SQSMessage record : event.getRecords()) {
            messageIds.add(record.getMessageId());
            messages.add(record.getBody());
        }

        if (messages.size() > 0) {
            // prepare body
            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            final String interval = formatter.format(new Date());
            final String points = String.join(",", messages);
            final String data = String.format(
                    "{\"timeInterval\":\"%s\",\"batchOfPoints\":[%s]}",
                    interval,
                    points
            );
            logger.log("aggregated " + data);

            // connect to DynamoDB and store aggregated messages
            try (DynamoDbClient dbClient = DynamoDbClient.builder().build()) {
                logger.log("got client");
                final HashMap<String, AttributeValue> itemValues = new HashMap<>(2);
                itemValues.put("time_interval", AttributeValue.builder().s(interval).build());
                itemValues.put("data", AttributeValue.builder().s(data).build());
                logger.log("got data");

                final PutItemRequest request = PutItemRequest.builder()
                        .tableName(System.getenv("DB_TABLE"))
                        .item(itemValues)
                        .build();
                logger.log("got request");
                final PutItemResponse response = dbClient.putItem(request);
                logger.log("stored new batch of points " + response.responseMetadata().requestId());
            }
        }



        return messageIds;
    }
}
