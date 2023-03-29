package com.ikea;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Date;
import java.util.Map;
import java.util.Random;

public class App implements RequestHandler<Map<String, String>, Void>
{
    @Override
    public Void handleRequest(Map<String, String> stringStringMap, Context context) {
        final LambdaLogger logger = context.getLogger();
        logger.log("Generate data");

        // prepare new datapoint
        final String json = String.format(
                "{\"timestamp\": %d, \"point\": %d}",
                getTimestamp(),
                generateRandom()
        );

        // push datapoint to SNS so aggregator can handle it later on
        logger.log("Send to SNS");
        final String region = System.getenv("REGION");
        final String snsTopic = System.getenv("SNS_TOPIC");
        try (SnsClient client = SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build()) {
            final PublishRequest request = PublishRequest.builder()
                    .message(json)
                    .topicArn(snsTopic)
                    .build();
            final PublishResponse response = client.publish(request);
            logger.log("Message sent");
        }

        return null;
    }

    /**
     * Generate random number from 1-1000
     * @return
     */
    private int generateRandom() {
        final Random rand = new Random();
        return rand.nextInt(1000) + 1;
    }

    /**
     * Get current timestamp
     * @return
     */
    private long getTimestamp() {
        final Date date = new Date();
        return date.getTime() / 1000;
    }
}
