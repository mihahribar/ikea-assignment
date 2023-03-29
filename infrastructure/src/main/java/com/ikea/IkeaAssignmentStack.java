package com.ikea;

import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awsconstructs.services.lambdasns.LambdaToSns;
import software.amazon.awsconstructs.services.lambdasns.LambdaToSnsProps;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

public class IkeaAssignmentStack extends Stack {

    static final String REGION = "eu-north-1";
    static final String SNS_TOPIC = "ikea-assignment";
    static final String STATE_MACHINE_EVERY_2_SECONDS = "2s-state-machine";
    static final String LAMBDA_GENERATOR = "lambda-generator";
    static final String LAMBDA_AGGREGATOR = "lambda-aggregator";
    static final String CRON_GENERATOR = "cron-generator";

    public IkeaAssignmentStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public IkeaAssignmentStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

//        // SNS topic
//        Topic snsTopic = new Topic(
//                this,
//                SNS_TOPIC,
//                TopicProps.builder()
//                        .topicName(SNS_TOPIC)
//                        .build()
//        );

        // lambda functions
        List<String> generatorPackagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd generator " +
                        "&& mvn clean install " +
                        "&& cp /asset-input/generator/target/generator.jar /asset-output/"
        );

        List<String> aggregatorPackagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd aggregator " +
                        "&& mvn clean install " +
                        "&& cp /asset-input/aggregator/target/aggregator.jar /asset-output/"
        );

        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(generatorPackagingInstructions)
                .image(Runtime.JAVA_11.getBundlingImage())
                .volumes(singletonList(
                        // Mount local .m2 repo to avoid download all the dependencies again inside the container
                        software.amazon.awscdk.DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(ARCHIVED);

        Function aggregatorFunction = new Function(
                this,
                LAMBDA_AGGREGATOR,
                FunctionProps.builder()
                        .runtime(Runtime.JAVA_11)
                        .code(Code.fromAsset("../software/", AssetOptions.builder()
                                .bundling(builderOptions
                                        .command(aggregatorPackagingInstructions)
                                        .build())
                                .build()))
                        .handler("com.ikea.App")
                        .memorySize(512)
                        .timeout(software.amazon.awscdk.Duration.seconds(10))
                        .logRetention(RetentionDays.ONE_WEEK)
                        .build()
        );

        // not sure if this is the best way to go about it
        LambdaToSns lambdaToSns = new LambdaToSns(
                this,
                SNS_TOPIC,
                LambdaToSnsProps.builder()
                        .existingLambdaObj(aggregatorFunction)
                        .build()
        );

        Function generatorFunction = new Function(
                this,
                LAMBDA_GENERATOR,
                FunctionProps.builder()
                        .runtime(Runtime.JAVA_11)
                        .code(Code.fromAsset("../software/", AssetOptions.builder()
                                .bundling(builderOptions
                                        .command(generatorPackagingInstructions)
                                        .build())
                                .build()))
                        .handler("com.ikea.App")
                        .memorySize(512)
                        .timeout(software.amazon.awscdk.Duration.seconds(10))
                        .logRetention(RetentionDays.ONE_WEEK)
                        .environment(Map.of(
                                "REGION", REGION,
                                "SNS_TOPIC", lambdaToSns.getSnsTopic().getTopicArn()))
                        .build()
        );

        // create a state machine with the step function to execute generator every 2s
//        StateMachine stateMachine = StateMachine.Builder.create(this, STATE_MACHINE_EVERY_2_SECONDS)
//                .definition(LambdaInvoke.Builder.create(this, "generator")
//                        .lambdaFunction(generatorFunction)
//                        .build())
//                .build();

        Rule rule = Rule.Builder.create(this, CRON_GENERATOR)
                .schedule(Schedule.expression("rate(1 minute)"))
                .enabled(true)
                .build();
        rule.addTarget(new LambdaFunction(generatorFunction, null));
    }
}
