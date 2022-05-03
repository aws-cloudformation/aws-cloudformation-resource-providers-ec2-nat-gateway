package software.amazon.ec2.natgateway;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.State;
import software.amazon.awssdk.services.ec2.model.NatGateway;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsResponse;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<Ec2Client> proxyClient;

    @Mock
    Ec2Client Ec2Client;

    private ResourceModel oldModel;
    private ResourceModel newModel;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        Ec2Client = mock(Ec2Client.class);
        proxyClient = MOCK_PROXY(proxy, Ec2Client);

    }

    @AfterEach
    public void tearDown() {
        verify(Ec2Client, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(Ec2Client);
    }

    public void buildResourceModels(List<Tag> oldTags, List<Tag> newTags) {
        oldModel = ResourceModel.builder()
                .natGatewayId(NAT_ID)
                .subnetId(SUBNET_ID)
                .connectivityType(CONN_PUBLIC)
                .tags(Translator.convertToNatTags(oldTags))
                .allocationId(ALLOC_ID)
                .build();

        newModel = ResourceModel.builder()
                .natGatewayId(NAT_ID)
                .subnetId(SUBNET_ID)
                .connectivityType(CONN_PUBLIC)
                .tags(Translator.convertToNatTags(newTags))
                .allocationId(ALLOC_ID)
                .build();
    }

    @Test
    public void handleRequestUpdateOnlyAddsTags() {
        final List<Tag> newTags = new ArrayList<>(TAGS);
        newTags.add(TAG_2);

        NatGateway natGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.AVAILABLE.toString());
        natGateway = natGateway.toBuilder().tags(newTags).build();

        final CreateTagsResponse createTagsResponse = CreateTagsResponse.builder().build();
        when(proxyClient.client().createTags(ArgumentMatchers.any(CreateTagsRequest.class))).thenReturn(createTagsResponse);

        final DescribeNatGatewaysResponse describeNatGatewaysResponse = DescribeNatGatewaysResponse.builder().natGateways(natGateway).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenReturn(describeNatGatewaysResponse);

        buildResourceModels(TAGS, newTags);

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(oldModel).desiredResourceState(newModel).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getTags().equals(newTags));
    }

    @Test
    public void handleRequestUpdateOnlyRemovesTags() {
        final List<Tag> newTags = new ArrayList<>();

        NatGateway natGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.AVAILABLE.toString());
        natGateway = natGateway.toBuilder().tags(newTags).build();

        final DeleteTagsResponse deleteTagsResponse = DeleteTagsResponse.builder().build();
        when(proxyClient.client().deleteTags(ArgumentMatchers.any(DeleteTagsRequest.class))).thenReturn(deleteTagsResponse);

        final DescribeNatGatewaysResponse describeNatGatewaysResponse = DescribeNatGatewaysResponse.builder().natGateways(natGateway).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenReturn(describeNatGatewaysResponse);

        buildResourceModels(TAGS, newTags);

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(oldModel).desiredResourceState(newModel).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getTags().equals(newTags));
    }

    @Test
    public void handleRequestUpdateAddsAndRemovesTags() {
        final List<Tag> newTags = new ArrayList<>();
        newTags.add(TAG_2);

        NatGateway natGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.AVAILABLE.toString());
        natGateway = natGateway.toBuilder().tags(newTags).build();

        final CreateTagsResponse createTagsResponse = CreateTagsResponse.builder().build();
        when(proxyClient.client().createTags(ArgumentMatchers.any(CreateTagsRequest.class))).thenReturn(createTagsResponse);

        final DeleteTagsResponse deleteTagsResponse = DeleteTagsResponse.builder().build();
        when(proxyClient.client().deleteTags(ArgumentMatchers.any(DeleteTagsRequest.class))).thenReturn(deleteTagsResponse);

        final DescribeNatGatewaysResponse describeNatGatewaysResponse = DescribeNatGatewaysResponse.builder().natGateways(natGateway).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenReturn(describeNatGatewaysResponse);

        buildResourceModels(TAGS, newTags);

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(oldModel).desiredResourceState(newModel).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getTags().equals(newTags));
    }

    @Test
    public void handleRequestUpdateDeletedNat() {
        final NatGateway natGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.DELETED.toString());

        final DescribeNatGatewaysResponse describeResponse = DescribeNatGatewaysResponse.builder().natGateways(Collections.singletonList(natGateway)).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenReturn(describeResponse);

        final UpdateHandler handler = new UpdateHandler();
        final ResourceHandlerRequest<ResourceModel> request = createResourceHandlerRequest();

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }

    @Test
    public void handleRequestInvalidTagKey() {
        final List<Tag> newTags = new ArrayList<>(TAGS);
        newTags.add(TAG_2);
        NatGateway natGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.AVAILABLE.toString());
        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("InvalidTagKey.Malformed").build();
        final AwsServiceException awsServiceException = AwsServiceException.builder().awsErrorDetails(awsErrorDetails).build();

        when(proxyClient.client().createTags(ArgumentMatchers.any(CreateTagsRequest.class)))
                .thenThrow(awsServiceException);

        final DescribeNatGatewaysResponse describeNatGatewaysResponse = DescribeNatGatewaysResponse.builder().natGateways(natGateway).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenReturn(describeNatGatewaysResponse);

        buildResourceModels(TAGS, newTags);

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(oldModel).desiredResourceState(newModel).build();

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }

    @Test
    public void handleRequestTagLimitExceeded() {
        final List<Tag> newTags = new ArrayList<>(TAGS);
        newTags.add(TAG_2);
        NatGateway natGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.AVAILABLE.toString());
        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("TagLimitExceeded").build();
        final AwsServiceException awsServiceException = AwsServiceException.builder().awsErrorDetails(awsErrorDetails).build();

        when(proxyClient.client().createTags(ArgumentMatchers.any(CreateTagsRequest.class)))
                .thenThrow(awsServiceException);

        final DescribeNatGatewaysResponse describeNatGatewaysResponse = DescribeNatGatewaysResponse.builder().natGateways(natGateway).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenReturn(describeNatGatewaysResponse);

        buildResourceModels(TAGS, newTags);

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(oldModel).desiredResourceState(newModel).build();

        Assertions.assertThrows(CfnServiceLimitExceededException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }

    @Test
    public void handleRequestUpdateNullTags() {
        NatGateway natGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.AVAILABLE.toString());
        natGateway = natGateway.toBuilder().tags(new ArrayList<>()).build();

        final DescribeNatGatewaysResponse describeNatGatewaysResponse = DescribeNatGatewaysResponse.builder().natGateways(natGateway).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenReturn(describeNatGatewaysResponse);

        buildResourceModels(null, null);

        final UpdateHandler handler = new UpdateHandler();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(oldModel).desiredResourceState(newModel).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getTags().equals(null));
    }
}
