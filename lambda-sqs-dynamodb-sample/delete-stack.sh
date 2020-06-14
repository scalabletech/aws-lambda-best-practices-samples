#!/bin/bash
STACK_NAME=lambda-sqs-dynamodb-sample

aws cloudformation delete-stack --stack-name $STACK_NAME