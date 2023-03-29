package com.ikea;

import com.amazonaws.services.lambda.runtime.RequestHandler;

public class App implements RequestHandler<java.util.Map<String, String>, Void>
{
    public Void handleRequest(java.util.Map<String, String> stringStringMap, com.amazonaws.services.lambda.runtime.Context context) {
        final com.amazonaws.services.lambda.runtime.LambdaLogger logger = context.getLogger();
        logger.log("Aggregate data");
        return null;
    }
}
