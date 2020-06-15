#!/bin/bash
STACK_NAME=lambda-sqs-dynamodb-sample
BUCKET_NAME=$STACK_NAME-code-$(cat /dev/urandom | tr -dc 'a-z0-9' | fold -w 23cd | head -n 1)

echo Bucket with name "$BUCKET_NAME" will be created in AWS S3
echo "$BUCKET_NAME" > bucket-name.tmp

aws s3 mb s3://"$BUCKET_NAME"
