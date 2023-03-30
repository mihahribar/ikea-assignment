package com.ikea;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class App
{
    public static void main( String[] args ) {

        if (args.length != 2) {
            System.out.println("\n" +
                    "Usage: <region> <queue url>\n\n");
            System.exit(1);
        }

        // push datapoint to SNS so aggregator can handle it later on
        final String region = args[0];
        final String queue = args[1];

        SqsClient sqsClient = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                sendMessage(sqsClient, queue);
            }
        }, 0, 2*1000);
    }

    private static void sendMessage(SqsClient sqsClient, String queue) {
        // prepare new datapoint
        final String json = String.format(
                "{\"timestamp\": %d, \"point\": %d}",
                getTimestamp(),
                generateRandom()
        );
        SendMessageRequest messageRequest = SendMessageRequest.builder()
                .queueUrl(queue)
                .messageBody(json)
                .build();
        sqsClient.sendMessage(messageRequest);
        System.out.println(json);
    }

    /**
     * Generate random number from 1-1000
     * @return
     */
    private static int generateRandom() {
        final Random rand = new Random();
        return rand.nextInt(1000) + 1;
    }

    /**
     * Get current timestamp
     * @return
     */
    private static long getTimestamp() {
        final Date date = new Date();
        return date.getTime() / 1000;
    }
}
