package com.ikea;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class App implements RequestHandler<SQSEvent, List<String>>
{
    /**
     * Lambda entry point called with SQSEven which may contain many SQSMessages.
     * It returns a list of handled message ids. If an error occurs during processing
     * the messages are returned to the queue.
     *
     * @param event
     * @param context
     * @return
     */
    public List<String> handleRequest(SQSEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("aggregator started");

        List<String> messageIds = new ArrayList<>();
        List<AttributeValue> messages = new ArrayList<>();

        // aggregate messages and create a list of
        for (SQSEvent.SQSMessage record : event.getRecords()) {
            messageIds.add(record.getMessageId());
            messages.add(AttributeValue.builder().s(record.getBody()).build());
        }

        if (messages.size() > 0) {
            // which time interval to add messages to?
            // TODO it's likely that not all are from the same timeframe
            final Date intervalDate = getTimeFrame(new Date());
            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            final String interval = formatter.format(intervalDate);

            // connect to DynamoDB and store aggregated messages
            try (DynamoDbClient dbClient = DynamoDbClient.builder().build()) {
                // need to use a conditional add to list of points
                // - if key does not exist list is set
                // - if key exists data points are added to the list
                final UpdateItemRequest request = UpdateItemRequest.builder()
                        .tableName(System.getenv("DB_TABLE"))
                        .key(Map.of("time_interval", AttributeValue.builder().s(interval).build()))
                        .updateExpression("SET #data = list_append(if_not_exists(#data, :list), :list)")
                        .expressionAttributeNames(Map.of("#data", "batchOfPoints"))
                        .expressionAttributeValues(Map.of(
                                ":list", AttributeValue.builder().l(messages).build()
                        ))
                        .build();
                final UpdateItemResponse response = dbClient.updateItem(request);
                logger.log("stored interval " + interval + " with points " + response.responseMetadata().requestId());
            }
        }

        return messageIds;
    }

    /**
     * Get current time frame (either start of minute or at 30s).
     *
     * @return
     */
     Date getTimeFrame(Date date) {
        // which timeframe to add data points to?
        // since sampling is separated by 30s we can set seconds to 0 and 30s to see which timeframe we're in
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        final Date current = calendar.getTime();
        calendar.set(Calendar.SECOND, 0);
        final Date startOfMinute = calendar.getTime();
        calendar.set(Calendar.SECOND, 30);
        final Date halfOfMinute = calendar.getTime();
        return current.compareTo(halfOfMinute) >= 0 ? halfOfMinute : startOfMinute;
    }
}
