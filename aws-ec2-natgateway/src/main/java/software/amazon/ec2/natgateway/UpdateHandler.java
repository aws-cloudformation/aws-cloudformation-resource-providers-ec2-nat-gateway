package software.amazon.ec2.natgateway;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsResponse;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> preUpdateCheck(progress, proxy, proxyClient, model, callbackContext))
                .then(progress -> updateTags(progress, proxy, proxyClient, model, callbackContext, request))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * This check verifies that the resource exists before updating. If the resource has been deleted, a ResourceNotFound
     * exception is thrown and the Update Handler fails.
     * @param progress          holds the current progress data
     * @param proxy             aws proxy used to inject credentials and to initiate the proxy chain for the call graph
     * @param proxyClient       aws ec2 client used to make request
     * @param model             Nat Gateway Resource Model
     * @param callbackContext   the callback context for the handler
     * @return ProgressEvent    indicates the state of the progress, whether successful, in progress, or failed
     */
    protected ProgressEvent<ResourceModel, CallbackContext> preUpdateCheck(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<Ec2Client> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext) {

        return ProgressEvent.progress(model, callbackContext)
                .then(newProgress ->
                        proxy.initiate("AWS-EC2-NatGateway::Update::PreUpdateCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToReadRequest)
                                .makeServiceCall((awsRequest, _proxyClient) -> readResource(awsRequest, proxyClient , logger))
                                .progress()
                );
    }

    /**
     * This will use the EC2 Tagging APIs to update the tags for the nat gateway resource by deleting the unwanted tags
     * and creating the desired ones.
     * @param progress          holds the current progress data
     * @param proxy             aws proxy used to inject credentials and to initiate the proxy chain for the call graph
     * @param proxyClient       aws ec2 client used to make request
     * @param model             Nat Gateway Resource Model
     * @param callbackContext   the callback context for the handler
     * @param request           Request made by the client
     * @return ProgressEvent    indicates the state of the progress, whether successful, in progress, or failed
     */
    protected ProgressEvent<ResourceModel, CallbackContext> updateTags(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<Ec2Client> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext,
            final ResourceHandlerRequest<ResourceModel> request) {

        final Map<String, String> oldTags = request.getPreviousResourceTags() == null ? Collections.emptyMap() : request.getPreviousResourceTags();
        final Map<String, String> newTags = request.getDesiredResourceTags() == null ? Collections.emptyMap() : request.getDesiredResourceTags();

        final Map<String, String> tagsToDelete = new HashMap<String, String>();
        tagsToDelete.putAll(oldTags);
        tagsToDelete.entrySet().removeAll(newTags.entrySet());

        final Map<String, String> tagsToCreate = new HashMap<String, String>();
        tagsToCreate.putAll(newTags);
        tagsToCreate.entrySet().removeAll(oldTags.entrySet());

        // Adds system tags when updating after importing a resource
        if (request.getPreviousSystemTags() == null && request.getSystemTags() != null) {
            tagsToCreate.putAll(request.getSystemTags());
        }

        // Removes system tags if update is rolled back after importing a resource
        if (request.getPreviousSystemTags() != null && request.getSystemTags() == null) {
            tagsToDelete.putAll(request.getPreviousResourceTags());
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(newProgress -> tagsToCreate.isEmpty() ? progress :
                        proxy.initiate("AWS-EC2-NatGateway::Update::CreateTags", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(createRequest -> Translator.translateToCreateTagsRequest(tagsToCreate, model))
                                .makeServiceCall((createRequest, client) -> createTags(createRequest, proxyClient, logger))
                                .progress())
                .then(newProgress -> tagsToDelete.isEmpty() ? progress :
                        proxy.initiate("AWS-EC2-NatGateway::Update::DeleteTags", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(deleteRequest -> Translator.translateToDeleteTagsRequest(tagsToDelete, model))
                                .makeServiceCall((deleteRequest, client) -> deleteTags(deleteRequest, proxyClient, logger))
                                .progress());
    }

    /**
     * Adds the Tags by calling the CreateTags EC2 API
     * @param createTagsRequest Request made by the client
     * @param proxyClient       aws ec2 client used to make request
     * @param logger            used to log
     * @return CreateTags response
     */
    protected CreateTagsResponse createTags(final CreateTagsRequest createTagsRequest,
                                            final ProxyClient<Ec2Client> proxyClient,
                                            final Logger logger) {
        CreateTagsResponse createTagsResponse;
        try {
            createTagsResponse = proxyClient.injectCredentialsAndInvokeV2(createTagsRequest, proxyClient.client()::createTags);
        } catch (final AwsServiceException e) {
            throw handleError(e);
        }
        logger.log(String.format("%s's tags have successfully been created. Update complete.", ResourceModel.TYPE_NAME));
        return createTagsResponse;
    }

    /**
     * Removes the Tags by calling the DeleteTags EC2 API
     * @param deleteTagsRequest Request made by the client
     * @param proxyClient       aws ec2 client used to make request
     * @param logger            used to log
     * @return DeleteTags response
     */
    protected DeleteTagsResponse deleteTags(final DeleteTagsRequest deleteTagsRequest,
                                            final ProxyClient<Ec2Client> proxyClient,
                                            final Logger logger) {
        DeleteTagsResponse deleteTagsResponse;
        try {
            deleteTagsResponse = proxyClient.injectCredentialsAndInvokeV2(deleteTagsRequest, proxyClient.client()::deleteTags);
        } catch (final AwsServiceException e) {
            throw handleError(e);
        }
        logger.log(String.format("%s's tags have successfully been deleted.", ResourceModel.TYPE_NAME));
        return deleteTagsResponse;
    }
}
