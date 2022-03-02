package software.amazon.ec2.natgateway;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsResponse;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsResponse;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.*;

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
                                .handleError((awsRequest, exception, client, resourceModel, context) -> {
                                    if (exception instanceof ResourceNotFoundException)
                                        return ProgressEvent.success(resourceModel, context);
                                    throw exception;
                                })
                                .progress()
                );
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updateTags(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<Ec2Client> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext,
            final ResourceHandlerRequest<ResourceModel> request) {

        final ResourceModel oldModel = request.getPreviousResourceState();
        final List<Tag> tagsToDelete = new ArrayList<Tag>(oldModel.getTags());
        tagsToDelete.removeAll(model.getTags());

        final List<Tag> tagsToCreate = new ArrayList<Tag>(model.getTags());
        tagsToCreate.removeAll(oldModel.getTags());

        return ProgressEvent.progress(model, callbackContext)
                .then(newProgress -> tagsToDelete.isEmpty() ? progress :
                        proxy.initiate("AWS-EC2-NatGateway::Update::DeleteTags", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(deleteRequest -> Translator.translateToDeleteTagsRequest(tagsToDelete, model))
                                .makeServiceCall((deleteRequest, client) -> deleteTags(deleteRequest, proxyClient, logger))
                                .progress())
                .then(newProgress -> tagsToCreate.isEmpty() ? progress :
                        proxy.initiate("AWS-EC2-NatGateway::Update::CreateTags", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(createRequest -> Translator.translateToCreateTagsRequest(tagsToCreate, model))
                                .makeServiceCall((createRequest, client) -> createTags(createRequest, proxyClient, logger))
                                .progress());
    }

    /**
     * Adds the Tags by calling the CreateTags EC2 API
     * @param createTagsRequest Request made by the client
     * @param proxyClient       aws ec2 client used to make request
     * @param logger            used to log
     * @return  CreateTags response
     */
    protected CreateTagsResponse createTags(final CreateTagsRequest createTagsRequest,
                                            final ProxyClient<Ec2Client> proxyClient,
                                            final Logger logger){
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
     * @return  DeleteTags response
     */
    protected DeleteTagsResponse deleteTags(final DeleteTagsRequest deleteTagsRequest,
                                            final ProxyClient<Ec2Client> proxyClient,
                                            final Logger logger){
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
