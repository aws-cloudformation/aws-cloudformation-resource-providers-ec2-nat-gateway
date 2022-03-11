package software.amazon.ec2.natgateway;

import com.google.common.collect.Maps;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DeleteNatGatewayRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Ec2Request;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.*;

import java.util.*;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

  protected static final String MESSAGE_STABILIZED = "Nat Gateway operation has stabilized";
  protected static final String MESSAGE_DID_NOT_STABILIZE = "Nat Gateway delete operation did not stabilize";
  protected static final String INVALID_NAT_GATEWAY_ID_NOT_FOUND_ERROR_CODE = "InvalidNatGatewayID.NotFound";
  protected static final String INVALID_NAT_GATEWAY_ID_MALFORMED = "InvalidNatGatewayId.Malformed";
  protected static final String LIMITS_EXCEEDED = "NatGatewayLimitExceeded";

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


  protected ProgressEvent<ResourceModel, CallbackContext> handleError(
          final Ec2Request apiRequest,
          Exception e,
          ProxyClient<Ec2Client> apiProxyClient,
          ResourceModel apiModel,
          CallbackContext callbackContext) {


    BaseHandlerException ex = null;

    if (INVALID_NAT_GATEWAY_ID_NOT_FOUND_ERROR_CODE.equals(getErrorCode(e))) {
      ex = new CfnNotFoundException(e);
    } else if (INVALID_NAT_GATEWAY_ID_MALFORMED.equals(getErrorCode(e))) {
      ex = new CfnInvalidRequestException(e);
    } else if (LIMITS_EXCEEDED.equals(getErrorCode(e))) {
      ex = new CfnServiceLimitExceededException(e);
    } else if (e instanceof Ec2Exception) {
      ex = new CfnGeneralServiceException(e);
    }
    return ProgressEvent.failed(apiModel, callbackContext, ex.getErrorCode(),ex.getMessage());
  }

  protected static String getErrorCode(Exception e) {
    if (e instanceof AwsServiceException) {
      return ((AwsServiceException) e).awsErrorDetails().errorCode();
    }
    return e.getMessage();

  }

  protected static List<Tag> getTagsToAdd(final Map<String, String> currentTags) {
    return Optional.ofNullable(Translator.convertSDKTagsToModelTags(currentTags)).orElse(Collections.emptyList());
  }

  protected static List<Tag> getTagsToRemove(final Map<String, String> previousTags, final Map<String, String> currentTags) {
    if (previousTags == null && currentTags == null) {
      return new ArrayList<Tag>();
    }
    return Optional.ofNullable(Translator.convertSDKTagsToModelTags(Maps.difference(previousTags, currentTags).entriesOnlyOnLeft())).orElse(Collections.emptyList());
  }



}
