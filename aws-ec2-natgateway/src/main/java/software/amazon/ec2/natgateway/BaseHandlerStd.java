package software.amazon.ec2.natgateway;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.State;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final Logger logger) {
    return handleRequest(
      proxy,
      request,
      callbackContext != null ? callbackContext : new CallbackContext(),
      proxy.newProxy(ClientBuilder::getClient),
      logger
    );
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final ProxyClient<Ec2Client> proxyClient,
    final Logger logger);


  /**
   * Translates the exceptions to CloudFormation exceptions based on the EC2 error codes.
   * @param e the aws exception thrown
   * @return BaseHandlerException, a CloudFormation exception
   */
  protected BaseHandlerException handleError(final AwsServiceException e){
    switch(e.awsErrorDetails().errorCode()){
      case "InvalidParameter":
      case "MissingParameter":
      case "InvalidSubnet":
      case "InvalidSubnetID.Malformed":
      case "NatGatewayMalformed":
      case "InvalidTagKey.Malformed":
      case "InvalidID":
      case "InvalidPaginationToken":
      case "InvalidAvailabilityZone":
      case "InvalidElasticIpID.Malformed": return new CfnInvalidRequestException(e);
      case "InvalidNatGatewayID.NotFound":
      case "NatGatewayNotFound":
      case "InvalidSubnetID.NotFound":
      case "InvalidElasticIpID.NotFound": return new CfnNotFoundException(e);
      case "FilterLimitExceeded":
      case "TagLimitExceeded":
      case "NatGatewayLimitExceeded": return new CfnServiceLimitExceededException(e);
      case "UnauthorizedOperation": return new CfnAccessDeniedException(e);
      case "InternalError":
      case "InternalFailure":
      case "ServiceUnavailable": return new CfnServiceInternalErrorException(e);
      default: return new CfnGeneralServiceException(e);
    }
  }

  /**
   * Reads the NAT Gateway resource by calling the describeNatGateways API
   * @param describeNatGatewaysRequest   Request made by the client
   * @param proxyClient                  aws ec2 client used to make request
   * @param logger                       used to log
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
      // The Read Handler should only return a non-deleted NAT Gateway
      if(describeNatGatewaysResponse.natGateways().get(0).stateAsString().equalsIgnoreCase(State.DELETED.toString())){
        throw new ResourceNotFoundException(new Throwable("Nat Gateway is deleted."));
      }
    } catch (final AwsServiceException e) {
      throw handleError(e);
    }
    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
    return describeNatGatewaysResponse;
  }
}
