package software.amazon.ec2.natgateway;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<Ec2Client> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return proxy.initiate("AWS-EC2-NatGateway::Read", proxyClient, request.getDesiredResourceState(),
                callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall((awsRequest, _proxyClient) -> readResource(awsRequest, proxyClient , logger))
            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }

    /**
     * Reads the NAT Gateway resource by calling the describeNatGateways API
     * @param describeNatGatewaysRequest   Request made by the client
     * @param proxyClient               aws ec2 client used to make request
     * @param logger                    used to log
     * @return DescribeNatGateways Response
     */
    protected DescribeNatGatewaysResponse readResource(
            final DescribeNatGatewaysRequest describeNatGatewaysRequest,
            final ProxyClient<Ec2Client> proxyClient,
            final Logger logger) {
        DescribeNatGatewaysResponse describeNatGatewaysResponse;
        try {
            describeNatGatewaysResponse = proxyClient.injectCredentialsAndInvokeV2(describeNatGatewaysRequest,
                    proxyClient.client()::describeNatGateways);
        } catch (final AwsServiceException e) {
            throw handleError(e);
        }
        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return describeNatGatewaysResponse;
    }
}
