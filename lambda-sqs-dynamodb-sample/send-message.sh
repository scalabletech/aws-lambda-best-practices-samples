#!/bin/bash
STACK_NAME=lambda-sqs-dynamodb-sample
QUEUE_URL=$(aws cloudformation describe-stack-resource --stack-name $STACK_NAME --logical-resource-id SourceQueue --query 'StackResourceDetail.PhysicalResourceId' --output text)

aws sqs send-message --queue-url $QUEUE_URL --message-body file://json/message-one-item.json