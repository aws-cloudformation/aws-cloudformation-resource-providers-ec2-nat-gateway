package software.amazon.ec2.natgateway;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.cloudformation.proxy.*;

public class ListHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<Ec2Client> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        final String nextToken;

        if(null != request.getNextToken())
         nextToken = request.getNextToken();

        return proxy.initiate("AWS-EC2-NatGateway::List", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToListRequest)
                .makeServiceCall((apiRequest, apiProxyClient)
                        -> apiProxyClient.injectCredentialsAndInvokeV2(apiRequest, apiProxyClient.client()::describeNatGateways))
                .handleError((apiRequest, exception, apiProxyClient, apiModel, apiContext)
                        -> handleError(apiRequest, exception, apiProxyClient, apiModel, apiContext))
                .done((apiRequest, apiResponse, apiClient, apiModel, apiContext)
                        -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(Translator.translateFromListResponse(apiResponse))
                        .status(OperationStatus.SUCCESS)
                        .nextToken(apiResponse.nextToken())
                        .build());
    }

}
