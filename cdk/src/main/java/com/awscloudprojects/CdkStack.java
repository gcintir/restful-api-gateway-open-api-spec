package com.awscloudprojects;

import java.util.Collections;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.apigateway.AuthorizationType;
import software.amazon.awscdk.services.apigateway.EndpointConfiguration;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.HttpMethod;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;


public class CdkStack extends Stack {
    public CdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);


        Role executionRole = Role.Builder.create(this, id + "-CreateOrderLambdaRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .roleName(id + "-CreateOrderLambdaRole")
                .build();
        executionRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"));

        String functionName = id + "-CreateOrderLambda";
        Function createOrderLambda = Function.Builder.create(this, functionName)
                .runtime(Runtime.JAVA_21)
                .code(Code.fromAsset("../function/create-order-lambda/target/create-order-lambda.jar"))
                .handler("com.awscloudprojects.lambda.CreateOrderLambda::handleRequest")
                .functionName(functionName)
                .role(executionRole)
                .timeout(Duration.seconds(30L))
                .memorySize(512)
                .logGroup(LogGroup.Builder.create(this, functionName + "-logGroup")
                        .logGroupName("/aws/lambda/" + functionName)
                        .retention(RetentionDays.ONE_DAY)
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .build())
                .build();

        RestApi restApiGateway = RestApi.Builder.create(this, "restapi-gateway")
                .restApiName(id + "-restapi-gateway")
                .endpointConfiguration(EndpointConfiguration.builder().types(
                        Collections.singletonList(EndpointType.REGIONAL)).build())
                .description("Test api gateway")
                .deployOptions(StageOptions.builder()
                        .stageName("dev")
                        .build())
                .build();

        restApiGateway.getRoot().addResource("order").addMethod(HttpMethod.POST.name(),
                LambdaIntegration.Builder.create(createOrderLambda).build(),
                MethodOptions.builder().authorizationType(AuthorizationType.NONE).build());

    }
}
