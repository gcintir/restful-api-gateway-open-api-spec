package com.awscloudprojects.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.logging.Logging;

public class CreateOrderLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>{
    private static final Logger logger = LoggerFactory.getLogger(CreateOrderLambda.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    @Logging(clearState = true)
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();
        apiGatewayProxyResponseEvent.setHeaders(
                Map.of(
                        "Access-Control-Allow-Origin", "*",
                        "Access-Control-Allow-Methods", "*",
                        "Access-Control-Allow-Headers", "*"));
        try {
            String requestBody = event.getBody();
            logger.info("Received input body: {}", requestBody);
            String orderId = UUID.randomUUID().toString();
            logger.info("Order created with id: {}", orderId);
            OrderData orderData = new OrderData(orderId, LocalDateTime.now().toString());
            apiGatewayProxyResponseEvent.setBody(objectMapper.writeValueAsString(orderData));
            apiGatewayProxyResponseEvent.setStatusCode(200);
        } catch (Exception e) {
            logger.error(e.getMessage());
            apiGatewayProxyResponseEvent.setBody(e.getMessage());
            apiGatewayProxyResponseEvent.setStatusCode(500);
        }
        return apiGatewayProxyResponseEvent;
    }
}
