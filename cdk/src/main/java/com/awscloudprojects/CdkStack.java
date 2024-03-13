package com.awscloudprojects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.IResolvable;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.apigateway.ApiDefinition;
import software.amazon.awscdk.services.apigateway.ApiKeyOptions;
import software.amazon.awscdk.services.apigateway.CfnDocumentationVersion;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.IApiKey;
import software.amazon.awscdk.services.apigateway.InlineApiDefinition;
import software.amazon.awscdk.services.apigateway.SpecRestApi;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.apigateway.UsagePlan;
import software.amazon.awscdk.services.apigateway.UsagePlanPerApiStage;
import software.amazon.awscdk.services.apigateway.UsagePlanProps;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Permission;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.Asset;
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

        // overriding lambda function logicalId to reference it inside open api documentation
        CfnFunction apiCfnFunction = (CfnFunction) createOrderLambda.getNode().getDefaultChild();
        apiCfnFunction.overrideLogicalId("createOrderLambda");

        Permission lamdaAPIGatewayPermission = Permission.builder()
                .principal(new ServicePrincipal("apigateway.amazonaws.com"))
                .build();
        createOrderLambda.addPermission("GW Permission", lamdaAPIGatewayPermission);

        Asset openAPIAsset = Asset.Builder.create(this, "OpenAPIAsset")
                .path("../api/open-api.yaml").build();

        Map<String, String> transformMap = new HashMap<>();
        transformMap.put("Location", openAPIAsset.getS3ObjectUrl());
        IResolvable data = Fn.transform("AWS::Include", transformMap);
        InlineApiDefinition apiDefinition = ApiDefinition.fromInline(data);

        SpecRestApi restApi = SpecRestApi.Builder.create(this,
                        "restapi-gateway")
                .restApiName(id + "-restapi-gateway")
                .description("REST API Gateway")
                .deployOptions(StageOptions.builder()
                        .stageName("dev")
                        .documentationVersion("v1")
                        .build())
                .endpointTypes(List.of(EndpointType.REGIONAL))
                .apiDefinition(apiDefinition)
                .deploy(true)
                .build();

        IApiKey apiKey = restApi.addApiKey("api-key", ApiKeyOptions.builder()
                .apiKeyName(id + "-api-key")
                .build());

        UsagePlan usagePlan = restApi.addUsagePlan("api-usage-plan", UsagePlanProps.builder()
                .name(id + "-api-usage-plan")
                .build());

        usagePlan.addApiKey(apiKey);

        usagePlan.addApiStage(UsagePlanPerApiStage.builder()
                .api(restApi)
                .stage(restApi.getDeploymentStage())
                .build());

        CfnDocumentationVersion apiGatewayDocument =  CfnDocumentationVersion.Builder.create(this, "api-gateway-doc-version")
                .documentationVersion("v1")
                .description("api-gateway-doc-version")
                .restApiId(restApi.getRestApiId())
                .build();
        apiGatewayDocument.applyRemovalPolicy(RemovalPolicy.RETAIN_ON_UPDATE_OR_DELETE);

        /*
        Rest API creation with L2 constructs
        RestApi restApi = RestApi.Builder.create(this, "restapi-gateway")
                .restApiName(id + "-restapi-gateway")
                .endpointConfiguration(EndpointConfiguration.builder().types(
                        Collections.singletonList(EndpointType.REGIONAL)).build())
                .description("Test api gateway")
                .deployOptions(StageOptions.builder()
                        .stageName("dev")
                        .build())
                .build();

        restApi.getRoot().addResource("order").addMethod(HttpMethod.POST.name(),
                LambdaIntegration.Builder.create(createOrderLambda).build(),
                MethodOptions.builder().authorizationType(AuthorizationType.NONE).apiKeyRequired(true).build());
         */

    }
}
