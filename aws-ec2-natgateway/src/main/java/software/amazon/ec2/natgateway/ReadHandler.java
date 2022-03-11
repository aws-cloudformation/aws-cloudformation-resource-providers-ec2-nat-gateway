package software.amazon.ec2.natgateway;

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.NatGatewayState;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<Ec2Client> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        if (model == null || StringUtils.isNullOrEmpty(model.getNatGatewayId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.NotFound, "Nat Gateway ID cannot be empty");
        }

        return proxy.initiate("AWS-EC2-NatGateway::Read", proxyClient, model, callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall((awsRequest, client) -> {
                String state;
                DescribeNatGatewaysResponse response = proxy.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeNatGateways);
                state = response.natGateways().get(0).state().toString();
                if (state.equals(NatGatewayState.DELETED.toString())) {
                    throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getNatGatewayId());
                }
                else {
                    return response;
                }
            })
            .handleError((apiRequest, exception, apiProxyClient, apiModel, apiContext)
                    -> handleError(apiRequest, exception, apiProxyClient, apiModel, apiContext))
            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }
}

