package software.amazon.ec2.natgateway;

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

import com.amazonaws.event.request.Progress;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DeleteNatGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Request;
import software.amazon.awssdk.services.ec2.model.NatGatewayState;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.OperationStatus;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<Ec2Client> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(firstProgress -> new ReadHandler().handleRequest(proxy, request, firstProgress.getCallbackContext(), proxyClient, logger))
                .onSuccess(progress -> deleteNatGateway(proxy, proxyClient, model, callbackContext, request));

    }

    private ProgressEvent<ResourceModel, CallbackContext> deleteNatGateway(AmazonWebServicesClientProxy proxy, ProxyClient<Ec2Client> proxyClient, ResourceModel model, CallbackContext context, ResourceHandlerRequest<ResourceModel> request) {

        return proxy.initiate("AWS-EC2-NatGateway::Delete", proxyClient, model, context)
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((apiRequest, apiProxyClient) ->
                        apiProxyClient.injectCredentialsAndInvokeV2(apiRequest, apiProxyClient.client()::deleteNatGateway))
                .stabilize((apiRequest, apiResponse, apiProxyClient, apiModel, callbackContext)
                        -> stabilizeDelete(apiProxyClient, apiModel))
                .handleError((apiRequest, exception, apiProxyClient, apiModel, apiContext)
                        -> handleError(apiRequest, exception, apiProxyClient, apiModel, apiContext))
                .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext> builder().status(OperationStatus.SUCCESS).build());

    }

    private Boolean stabilizeDelete(ProxyClient<Ec2Client> proxyClient, ResourceModel model) {

        try {

            String state;
            DescribeNatGatewaysResponse response = proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::describeNatGateways);
            state = response.natGateways().get(0).state().toString();
            logger.log(MESSAGE_DID_NOT_STABILIZE);
            return state.equals(NatGatewayState.DELETED.toString());

        } catch (AwsServiceException e) {
            if (INVALID_NAT_GATEWAY_ID_NOT_FOUND_ERROR_CODE.equals(e.awsErrorDetails().errorCode())
                    || INVALID_NAT_GATEWAY_ID_MALFORMED.equals(e.awsErrorDetails().errorCode())) {
                logger.log(MESSAGE_STABILIZED);
                return true;
            }
            if ("RequestLimitExceeded".equals(e.awsErrorDetails().errorCode()))
                    return false;


            throw e;
        }
    }
}
