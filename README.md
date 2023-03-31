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
* `cdk destroy`     destroy all stacks

To create stack used in the assignment run:

```
$ cdk deploy
```

The following infrastructure is created on AWS:
- DynamoDB table called `ikea-db`
- SQS queue called `ikea-queue`
- Lambda function called `ikea-lambda-aggregator`
- CloudWatch for logs with log group called `/aws/lambda/ikea-lambda-aggregator`

All this is defined in the `/infrastructure` folder.

To destroy the created stack run:

```
$ cdk destroy
```

### DynamoDB

<img width="1072" alt="Screenshot 2023-03-31 at 10 41 14" src="https://user-images.githubusercontent.com/148423/229074663-63d7409c-def4-4780-8030-2683d322b287.png">
<img width="1298" alt="Screenshot 2023-03-31 at 10 41 33" src="https://user-images.githubusercontent.com/148423/229074713-d4e559c9-75b4-4385-8b07-daf911d97963.png">

### Lambda connected to SQS

<img width="593" alt="Screenshot 2023-03-31 at 10 43 00" src="https://user-images.githubusercontent.com/148423/229074879-3256d6bc-263c-4e6b-b745-7fb23c17b8de.png">

### CloudWatch logs

<img width="1219" alt="Screenshot 2023-03-31 at 10 41 56" src="https://user-images.githubusercontent.com/148423/229074986-77cf3c21-7faa-43fc-8151-9d43f91eab5c.png">

## Software

In the `/software` folder you can find the `generator` (which generates a data point every 2 seconds) and the `aggregator` 
lambda function which is connected to the SQS queue and consumes messages in batches every 30s.

### Generator

To see how it works you must first run the generator (after you deploy the infrastructure). It generates a data point every 2 seconds and pushes it into SQS.

```
java -cp target/generator.jar com.ikea.App eu-north-1 https://sqs.eu-north-1.amazonaws.com/.../ikea-queue
```

If you are getting strange error you probably need to export the AWS access keys as the code is checking the local env
for credentials.

```
export AWS_SECRET_ACCESS_KEY=...
export AWS_ACCESS_KEY_ID=...
```

### Aggregator

Aggregator is periodically called with SQS messages. It then aggregates them and appends data points to the correct timeframe in the DynamoDB using conditional updates.

## Disclaimer

1. While the assignment asks for an aggregator that runs every 30s to aggregate and store data points,
I noticed this was very hard to achieve on AWS (inability to call cron on a sub minute resolution) 
and thus opted for what ended up being a more scalable solution in the end since the SQS and linked
Lambda can scale up almost indefinitely.
2. Logging part is also offloaded on CloudWatch where all system logs are automatically gathered and
displayed on an invocation resolution.
3. Code is very simple and was left as such for a couple of reasons:
   - goal was to create a very pragmatic and readable project
   - adding any services or dependency injection is partially handled by AWS and seemed like an overkill 