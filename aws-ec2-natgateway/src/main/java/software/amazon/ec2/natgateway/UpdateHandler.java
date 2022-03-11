package software.amazon.ec2.natgateway;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.*;

import java.util.List;
import java.util.Map;

public class UpdateHandler extends BaseHandlerStd {
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

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> updateTags(progress, proxy, proxyClient, model, callbackContext, request))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));

    }

    private ProgressEvent<ResourceModel, CallbackContext> updateTags(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<Ec2Client> proxyClient,
            final ResourceModel model,
            final CallbackContext context,
            final ResourceHandlerRequest<ResourceModel> request) {

        Map<String, String> previousTags = request.getPreviousResourceTags();
        Map<String, String> currentTags = request.getDesiredResourceTags();

        List<Tag> tagsToRemove = getTagsToRemove(previousTags, currentTags);
        List<Tag> tagsToAdd = getTagsToAdd(currentTags);

        return progress
                .then(_progress -> tagsToRemove.isEmpty()
                        ? ProgressEvent.progress(model, context)
                        : proxy.initiate("AWS-EC2-InternetGateway::Update::DeleteTags", proxyClient, model, context)
                        .translateToServiceRequest(apiRequest -> Translator.translateToDeleteTagsRequest(tagsToRemove,model.getNatGatewayId()))
                        .makeServiceCall((apiRequest, apiProxyClient) -> apiProxyClient.injectCredentialsAndInvokeV2(apiRequest, apiProxyClient.client()::deleteTags))
                        .handleError((apiRequest, exception, apiProxyClient, apiModel, apiContext) -> handleError(apiRequest, exception, apiProxyClient, apiModel, apiContext))
                        .progress())
                .then(_progress -> tagsToAdd.isEmpty()
                        ? ProgressEvent.progress(model, context)
                        : proxy.initiate("AWS-EC2-InternetGateway::Update::CreateTags", proxyClient, model, context)
                        .translateToServiceRequest(apiRequest -> Translator.translateToCreateTagsRequest(tagsToAdd,model.getNatGatewayId()))
                        .makeServiceCall((apiRequest, apiProxyClient) -> apiProxyClient.injectCredentialsAndInvokeV2(apiRequest, apiProxyClient.client()::createTags))
                        .handleError((apiRequest, exception, apiProxyClient, apiModel, apiContext) -> handleError(apiRequest, exception, apiProxyClient, apiModel, apiContext))
                        .progress());

    }
}
