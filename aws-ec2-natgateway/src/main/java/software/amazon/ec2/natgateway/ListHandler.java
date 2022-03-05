package software.amazon.ec2.natgateway;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.NatGateway;
import software.amazon.awssdk.services.ec2.model.State;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<Ec2Client> proxyClient,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        return proxy.initiate("AWS-EC2-NatGateway::List", proxyClient, model, callbackContext)
                .translateToServiceRequest(awsRequest -> Translator.translateToListRequest(request.getNextToken()))
                .makeServiceCall((awsRequest, _proxyClient) -> listResource(awsRequest, proxyClient , logger))
                .done((awsRequest, awsResponse, client, clientModel, context) -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModels(Translator.translateFromListRequest(awsResponse))
                        .nextToken(awsResponse.nextToken())
                        .status(OperationStatus.SUCCESS)
                        .build());
    }


    /**
     * Lists all the NAT Gateway resources by calling the describeNatGateways API
     * @param describeNatGatewaysRequest   Request made by the client
     * @param proxyClient                  aws ec2 client used to make request
     * @param logger                       used to log
     * @return DescribeNatGateways Response
     */
    protected DescribeNatGatewaysResponse listResource(
            final DescribeNatGatewaysRequest describeNatGatewaysRequest,
            final ProxyClient<Ec2Client> proxyClient,
            final Logger logger) {
        DescribeNatGatewaysResponse describeNatGatewaysResponse;
        try {
            describeNatGatewaysResponse = proxyClient.injectCredentialsAndInvokeV2(describeNatGatewaysRequest,
                    proxyClient.client()::describeNatGateways);
            // The List Handler should only list non-deleted NAT Gateways
            List<NatGateway> natGatewayList = describeNatGatewaysResponse.natGateways().stream()
                    .filter(nat -> !(State.DELETED.toString().equalsIgnoreCase(nat.stateAsString())))
                    .collect(Collectors.toList());
            describeNatGatewaysResponse = DescribeNatGatewaysResponse.builder().natGateways(natGatewayList).build();
        } catch (final AwsServiceException e) {
            throw handleError(e);
        }
        logger.log(String.format("%s has successfully been listed.", ResourceModel.TYPE_NAME));
        return describeNatGatewaysResponse;
    }
}
