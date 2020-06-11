#!/bin/bash
STACK_NAME=lambda-trigger-destinations-sample

aws cloudformation delete-stack --stack-name $STACK_NAME