# Ikea Assignment

## Infrastructure

This code assignment was created using CDK development with Java.

The `cdk.json` file tells the CDK Toolkit how to execute your app.

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

## Useful commands

* `mvn package`     compile and run tests
* `cdk ls`          list all stacks in the app
* `cdk synth`       emits the synthesized CloudFormation template
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk docs`        open CDK documentation

```
$ cdk deploy
```

The following infrastructure is created on AWS:
- DynamoDB table called `ikea-db`
- SQS queue called `ikea-queue`
- Lambda function called `ikea-lambda-aggregator`

All this is defined in the `/infrastructure` folder.

## Software

In the `/software` folder you can find the `generator` (which generates a data point every 2 seconds) and the `aggregator` 
lambda function which is connected to the SQS queue and consumes messages in batches every 30s.

To see how it works you must first run the generator (after you deploy the infrastructure):

```
java -cp target/generator.jar com.ikea.App eu-north-1 https://sqs.eu-north-1.amazonaws.com/945640281759/ikea-queue
```

If you are getting strange error you probably need to export the AWS access keys as the code is checking the local env
for credentials.

```
export AWS_SECRET_ACCESS_KEY=...
export AWS_ACCESS_KEY_ID=...
```

## Cleanup

After you are done make sure to destroy the infrastructure.

```
$ cdk destroy
```