#!/bin/bash
mvn clean package
STACK_NAME=lambda-sqs-dynamodb-sample
CODE_BUCKET=$(cat bucket-name.tmp)
TEMPLATE=template.yml
TEMPLATE_OUT=template-out.yml

aws cloudformation package --template-file $TEMPLATE --s3-bucket "$CODE_BUCKET" --output-template-file $TEMPLATE_OUT
aws cloudformation deploy --template-file $TEMPLATE_OUT --stack-name $STACK_NAME --capabilities CAPABILITY_NAMED_IAM