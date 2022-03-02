package software.amazon.ec2.natgateway;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DeleteNatGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DeleteNatGatewayResponse;
import software.amazon.awssdk.services.ec2.model.State;
import software.amazon.awssdk.services.ec2.model.NatGateway;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<Ec2Client> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                    proxy.initiate("AWS-EC2-NatGateway::Delete::PreDeletionCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                         .translateToServiceRequest(Translator::translateToReadRequest)
                         .makeServiceCall((awsRequest, _proxyClient) -> readResource(awsRequest, proxyClient , logger))
                         .handleError((awsRequest, exception, client, resourceModel, context) -> {
                             if (exception instanceof ResourceNotFoundException)
                                 return ProgressEvent.success(resourceModel, context);
                             throw exception;
                         })
                         .progress()
            )
            .then(progress ->
                proxy.initiate("AWS-EC2-NatGateway::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToDeleteRequest)
                    .makeServiceCall((awsRequest, client) -> deleteResource(awsRequest, proxyClient, logger))
                    .stabilize(this::isDeleteStabilized)
                    .progress()
            )
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }


    /**
     * Creates the NAT Gateway resource by calling the deleteNatGateway API
     * @param deleteNatGatewayRequest   Request made by the client
     * @param proxyClient               aws ec2 client used to make request
     * @param logger                    used to log
     * @return DeleteNatGateway Response
     */
    protected DeleteNatGatewayResponse deleteResource(
            final DeleteNatGatewayRequest deleteNatGatewayRequest,
            final ProxyClient<Ec2Client> proxyClient,
            final Logger logger) {
        DeleteNatGatewayResponse deleteNatGatewayResponse;
        try {
            deleteNatGatewayResponse = proxyClient.injectCredentialsAndInvokeV2(deleteNatGatewayRequest,
                    proxyClient.client()::deleteNatGateway);
        } catch (final AwsServiceException e) {
            throw handleError(e);
        }

        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
        return deleteNatGatewayResponse;
    }

    /**
     * Verifies that the state of the Nat Gateway created has gone to DELETED. If the state is FAILED, then it throws
     * an exception and fails the Resource Creation.
     * @param awsRequest        Request made by the client
     * @param awsResponse       Response from the request
     * @param proxyClient       aws ec2 client used to make request
     * @param model             Nat Gateway Resource Model
     * @param callbackContext   the callback context for the handler
     * @return boolean, true means stabilized and ends the stabilization process.
     */
    protected boolean isDeleteStabilized(
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
            if (state.equalsIgnoreCase(State.DELETED.toString())) {
                logger.log(String.format("%s %s has stabilized and is fully deleted.", ResourceModel.TYPE_NAME, natId));
                return true;
            } else if(state.equalsIgnoreCase(State.FAILED.toString())){
                final String message = String.format("NatGateway %s is in state %s and hence failed to stabilize. " +
                        "Detailed failure message: %s", natId, state, natGateway.failureMessage());
                logger.log(message);
                throw new CfnGeneralServiceException(message);
            } else {
                return false;
            }
        } catch (final AwsServiceException e) {
            logger.log(String.format("DescribeNatGateways API call failed during stablization with exception: %s",
                    e.getMessage()));
            throw handleError(e);
        }
    }
}
