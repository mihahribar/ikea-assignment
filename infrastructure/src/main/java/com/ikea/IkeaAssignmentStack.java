package com.ikea;

import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

public class IkeaAssignmentStack extends software.amazon.awscdk.Stack {
    public IkeaAssignmentStack(final software.constructs.Construct scope, final String id) {
        this(scope, id, null);
    }

    public IkeaAssignmentStack(final software.constructs.Construct scope, final String id, final software.amazon.awscdk.StackProps props) {
        super(scope, id, props);

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

        software.amazon.awscdk.BundlingOptions.Builder builderOptions = BundlingOptions.builder()
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

        Function generatorFunction = new Function(this, "generator", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../software/", AssetOptions.builder()
                        .bundling(builderOptions
                                .command(generatorPackagingInstructions)
                                .build())
                        .build()))
                .handler("ikea.App")
                .memorySize(512)
                .timeout(software.amazon.awscdk.Duration.seconds(10))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

        Function aggregatorFunction = new Function(this, "FunctionTwo", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../software/", AssetOptions.builder()
                        .bundling(builderOptions
                                .command(aggregatorPackagingInstructions)
                                .build())
                        .build()))
                .handler("ikea.App")
                .memorySize(512)
                .timeout(software.amazon.awscdk.Duration.seconds(10))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());
    }
}
