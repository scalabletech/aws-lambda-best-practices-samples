#!/bin/bash
STACK_NAME=lambda-trigger-destinations-sample
BUCKET_NAME=$(aws cloudformation describe-stack-resource --stack-name $STACK_NAME --logical-resource-id InputBucket --query 'StackResourceDetail.PhysicalResourceId' --output text)

aws s3 cp images/lambda-trigger-destinations-sample.png s3://$BUCKET_NAME/