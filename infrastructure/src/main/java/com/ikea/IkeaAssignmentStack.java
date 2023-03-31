package com.ikea;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueProps;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

public class IkeaAssignmentStack extends Stack {
    static final String QUEUE = "ikea-queue";
    static final String LAMBDA = "ikea-lambda-aggregator";
    static final String DB = "ikea-db";

    public IkeaAssignmentStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public IkeaAssignmentStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // create DynamoDB table with destroy removal policy so it gets destroyed on stack destroy
        Table table = new Table(
                this,
                DB,
                TableProps.builder()
                        .tableName(DB)
                        .partitionKey(Attribute.builder()
                                .name("time_interval")
                                .type(AttributeType.STRING)
                                .build())
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .build()
        );

        // create SQS queue
        Queue queue = new Queue(
                this,
                QUEUE,
                QueueProps.builder()
                        .queueName(QUEUE)
                        .build()
        );

        // Java lambda packing instructions
        List<String> aggregatorPackagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd aggregator " +
                        "&& mvn clean install " +
                        "&& cp /asset-input/aggregator/target/aggregator.jar /asset-output/"
        );

        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(aggregatorPackagingInstructions)
                .image(Runtime.JAVA_11.getBundlingImage())
                .volumes(singletonList(
                        // Mount local .m2 repo to avoid the download of all the dependencies
                        // again inside the container
                        software.amazon.awscdk.DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(ARCHIVED);

        // create aggregator lambda
        Function aggregatorFunction = new Function(
                this,
                LAMBDA,
                FunctionProps.builder()
                        .runtime(Runtime.JAVA_11)
                        .code(Code.fromAsset("../software/", AssetOptions.builder()
                                .bundling(builderOptions.build())
                                .build()))
                        .handler("com.ikea.App")
                        .memorySize(1024)
                        .timeout(software.amazon.awscdk.Duration.seconds(30))
                        .logRetention(RetentionDays.ONE_WEEK)
                        .functionName(LAMBDA)
                        .environment(Map.of(
                                "DB_TABLE", table.getTableName()
                        ))
                        .build()
        );

        // link SQS as event source for lambda
        aggregatorFunction.addEventSource(
                SqsEventSource.Builder.create(queue)
                        .batchSize(100)
                        .maxBatchingWindow(Duration.seconds(30))
                        .build()
        );

        // grant table write permissions to lambda
        table.grantWriteData(aggregatorFunction);
    }
}
