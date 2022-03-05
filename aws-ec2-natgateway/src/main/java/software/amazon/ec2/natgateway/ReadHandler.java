package software.amazon.ec2.natgateway;

import software.amazon.awssdk.services.ec2.Ec2Client;
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
            .done(natGateway -> ProgressEvent.defaultSuccessHandler(Translator.translateNatGatewayToResourceModel(natGateway)));
    }
}
