package software.amazon.ec2.natgateway;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
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
   * @return BaseHandlerException, a cloudformation exception
   */
  protected BaseHandlerException handleError(final AwsServiceException e){
    switch(e.awsErrorDetails().errorCode()){
      case "InvalidParameter":
      case "MissingParameter":
      case "InvalidSubnet":
      case "InvalidSubnetID.Malformed":
      case "NatGatewayMalformed":
      case "InvalidElasticIpID.Malformed": return new CfnInvalidRequestException(e);
      case "InvalidNatGatewayID.NotFound":
      case "NatGatewayNotFound":
      case "InvalidSubnetID.NotFound":
      case "InvalidElasticIpID.NotFound": return new CfnNotFoundException(e);
      case "NatGatewayLimitExceeded": return new CfnServiceLimitExceededException(e);
      case "UnauthorizedOperation": return new CfnAccessDeniedException(e);
      default: return new CfnGeneralServiceException(e);
    }
  }
}
