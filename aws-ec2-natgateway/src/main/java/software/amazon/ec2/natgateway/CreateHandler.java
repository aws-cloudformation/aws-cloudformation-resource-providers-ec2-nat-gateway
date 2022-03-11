package software.amazon.ec2.natgateway;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.ec2.model.CreateNatGatewayResponse;
import software.amazon.cloudformation.proxy.*;

import java.util.*;


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

        if(!StringUtils.isBlank(model.getNatGatewayId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Nat Gateway ID is a read only property");
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> createNatGateway(proxy, proxyClient, model, callbackContext, request))
                .then(progress -> createTags(proxy, proxyClient, model, callbackContext, request))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));


    }

    private ProgressEvent<ResourceModel, CallbackContext> createTags(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<Ec2Client> proxyClient,
            final ResourceModel model,
            final CallbackContext context,
            final ResourceHandlerRequest<ResourceModel> request) {

        Map<String, String> mergedTags = new HashMap<String, String>();
        mergedTags.putAll(request.getDesiredResourceTags());
        mergedTags.putAll(Optional.ofNullable(request.getSystemTags()).orElse(Collections.emptyMap()));

        List<Tag> tagsToAdd = getTagsToAdd(mergedTags);
        logger.log("tagsToAdd variable");
        logger.log(tagsToAdd.toString());

        return proxy.initiate("AWS-EC2-NatGateway::CreateTags", proxyClient, model, context)
                .translateToServiceRequest(apiModel
                        -> Translator.translateToCreateTagsRequest(tagsToAdd, apiModel.getNatGatewayId()))
                .makeServiceCall((apiRequest, apiProxyClient) ->
                        {
                        logger.log(apiRequest.toString());
                        return apiProxyClient.injectCredentialsAndInvokeV2(apiRequest, apiProxyClient.client()::createTags);
                        })
                .handleError((apiRequest, exception, apiProxyClient, apiModel, apiContext)
                        -> handleError(apiRequest, exception, apiProxyClient, apiModel, apiContext))
                .progress();

    }


    private ProgressEvent<ResourceModel, CallbackContext> createNatGateway(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<Ec2Client> proxyClient,
            final ResourceModel model,
            final CallbackContext context,
            final ResourceHandlerRequest<ResourceModel> request) {


        return proxy.initiate("AWS-EC2-NatGateway::Create", proxyClient, model, context)
                .translateToServiceRequest(apiModel
                        -> Translator.translateToCreateRequest(apiModel))
                .makeServiceCall((apiRequest, apiProxyClient) ->
                        apiProxyClient.injectCredentialsAndInvokeV2(apiRequest, apiProxyClient.client()::createNatGateway))
                .stabilize((apiRequest, apiResponse, apiProxyClient, apiModel, callbackContext)
                        -> stabilizeCreate(apiProxyClient, apiModel, apiResponse))
                .handleError((apiRequest, exception, apiProxyClient, apiModel, apiContext)
                        -> handleError(apiRequest, exception, apiProxyClient, apiModel, apiContext))
                .done((apiRequest, apiResponse, apiProxyClient, apiModel, callbackContext) -> {
                    model.setNatGatewayId(apiResponse.natGateway().natGatewayId());
                    return ProgressEvent.progress(apiModel, callbackContext);
                });


    }

    private Boolean stabilizeCreate(
            final ProxyClient<Ec2Client> apiProxyClient,
            final ResourceModel apiModel,
            final CreateNatGatewayResponse apiResponse) {

        try {
            apiModel.setNatGatewayId(apiResponse.natGateway().natGatewayId());
            apiProxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(apiModel), apiProxyClient.client()::describeNatGateways);
            logger.log(MESSAGE_STABILIZED);
            return true;
        } catch (AwsServiceException e) {
            if (INVALID_NAT_GATEWAY_ID_NOT_FOUND_ERROR_CODE.equals(e.awsErrorDetails().errorCode())) {
                logger.log(MESSAGE_DID_NOT_STABILIZE);
                return false;
            }
            if ("RequestLimitExceeded".equals(e.awsErrorDetails().errorCode()))
                return false;

            throw e;
        }





    }

}


