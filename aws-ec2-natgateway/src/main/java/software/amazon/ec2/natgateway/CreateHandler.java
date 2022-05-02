package software.amazon.ec2.natgateway;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateNatGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateNatGatewayResponse;
import software.amazon.awssdk.services.ec2.model.State;
import software.amazon.awssdk.services.ec2.model.NatGateway;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<Ec2Client> proxyClient,
        final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        final String clientToken = request.getClientRequestToken();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                 proxy.initiate("AWS-EC2-NatGateway::Create", proxyClient,progress.getResourceModel(),
                         progress.getCallbackContext())
                    .translateToServiceRequest(awsRequest -> Translator.translateToCreateRequest(model, clientToken))
                    .makeServiceCall((awsRequest, client) -> createResource(awsRequest, proxyClient, logger, model))
                    .stabilize(this::isCreateStabilized) // Only moves on when isCreateStabilized returns true
                    .progress()
                )
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Creates the NAT Gateway resource by calling the createNatGateway API
     * @param createNatGatewayRequest   Request made by the client
     * @param proxyClient               aws ec2 client used to make request
     * @param logger                    used to log
     * @param model                     Nat Gateway Resource Model
     * @return CreateNatGateway Response
     */
    protected CreateNatGatewayResponse createResource(
            final CreateNatGatewayRequest createNatGatewayRequest,
            final ProxyClient<Ec2Client> proxyClient,
            final Logger logger,
            final ResourceModel model) {
        CreateNatGatewayResponse createNatGatewayResponse;
        try {
            createNatGatewayResponse = proxyClient.injectCredentialsAndInvokeV2(createNatGatewayRequest,
                    proxyClient.client()::createNatGateway);
            model.setNatGatewayId(createNatGatewayResponse.natGateway().natGatewayId());
        } catch (final AwsServiceException e) {
            throw handleError(e);
        }

        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
        return createNatGatewayResponse;
    }

    /**
     * Verifies that the state of the Nat Gateway created has gone to AVAILABLE. If the state is FAILED, then it throws
     * an exception and fails the Resource Creation.
     * @param awsRequest        Request made by the client
     * @param awsResponse       Response from the request
     * @param proxyClient       aws ec2 client used to make request
     * @param model             Nat Gateway Resource Model
     * @param callbackContext   the callback context for the handler
     * @return boolean, true means stabilized and ends the stabilization process.
     */
    protected boolean isCreateStabilized(
            final AwsRequest awsRequest,
            final AwsResponse awsResponse,
            final ProxyClient<Ec2Client> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext) {

        try {
            final NatGateway natGateway =
                    proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model),
                            proxyClient.client()::describeNatGateways).natGateways().get(0);
            final String natId = natGateway.natGatewayId();
            final String state = natGateway.stateAsString();
            if (State.AVAILABLE.toString().equalsIgnoreCase(state)) {
                logger.log(String.format("%s %s has stabilized and is fully created.", ResourceModel.TYPE_NAME, natId));
                return true;
            } else if(State.FAILED.toString().equalsIgnoreCase(state)) {
                final String message = String.format("NatGateway %s is in state %s and hence failed to stabilize. " +
                        "Detailed failure message: %s", natId, state, natGateway.failureMessage());
                logger.log(message);
                throw new CfnGeneralServiceException(message);
            } else {
                return false;
            }
        } catch (final AwsServiceException e) {
            logger.log(String.format("DescribeNatGateways API call failed during stabilization with exception: %s",
                    e.getMessage()));
            throw handleError(e);
        }
    }
}
