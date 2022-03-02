package software.amazon.ec2.natgateway;

import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.NatGateway;
import software.amazon.awssdk.services.ec2.model.State;
import software.amazon.awssdk.services.ec2.model.DeleteNatGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DeleteNatGatewayResponse;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysRequest;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<Ec2Client> proxyClient;

    @Mock
    Ec2Client Ec2Client;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        Ec2Client = mock(Ec2Client.class);
        proxyClient = MOCK_PROXY(proxy, Ec2Client);
    }

    @AfterEach
    public void tear_down() {
        verify(Ec2Client, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(Ec2Client);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final NatGateway availableNatGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.AVAILABLE.toString());
        final NatGateway deletedNatGateway = availableNatGateway.toBuilder().state(State.DELETED.toString()).build();

        final DescribeNatGatewaysResponse availableDescribeResponse = DescribeNatGatewaysResponse.builder()
                .natGateways(Collections.singletonList(availableNatGateway)).build();

        final DescribeNatGatewaysResponse deletedDescribeResponse = DescribeNatGatewaysResponse.builder()
                .natGateways(Collections.singletonList(deletedNatGateway)).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class)))
                .thenReturn(availableDescribeResponse, deletedDescribeResponse);

        final DeleteNatGatewayResponse deleteResponse = DeleteNatGatewayResponse.builder().natGatewayId(availableNatGateway.natGatewayId()).build();
        when(proxyClient.client().deleteNatGateway(ArgumentMatchers.any(DeleteNatGatewayRequest.class))).thenReturn(deleteResponse);

        final DeleteHandler handler = new DeleteHandler();

        final ResourceHandlerRequest<ResourceModel> request = createResourceHandlerRequest();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_StabilizationFailure() {
        final NatGateway natGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.FAILED.toString());
        final DeleteNatGatewayResponse deleteResponse = DeleteNatGatewayResponse.builder().natGatewayId(natGateway.natGatewayId()).build();
        when(proxyClient.client().deleteNatGateway(ArgumentMatchers.any(DeleteNatGatewayRequest.class))).thenReturn(deleteResponse);

        final DescribeNatGatewaysResponse describeResponse = DescribeNatGatewaysResponse.builder().natGateways(Collections.singletonList(natGateway)).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenReturn(describeResponse);

        final DeleteHandler handler = new DeleteHandler();
        final ResourceHandlerRequest<ResourceModel> request = createResourceHandlerRequest();

        Assertions.assertThrows(CfnGeneralServiceException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }

    @Test
    public void handleRequest_DeletingAlreadyDeletedNat() {
        final NatGateway natGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.DELETED.toString());

        final DescribeNatGatewaysResponse describeResponse = DescribeNatGatewaysResponse.builder().natGateways(Collections.singletonList(natGateway)).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenReturn(describeResponse);

        final DeleteHandler handler = new DeleteHandler();
        final ResourceHandlerRequest<ResourceModel> request = createResourceHandlerRequest();

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }

    @Test
    public void handleRequest_natGatewayNotFound(){
        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("NatGatewayNotFound").build();
        final AwsServiceException awsServiceException = AwsServiceException.builder().awsErrorDetails(awsErrorDetails).build();

        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenThrow(awsServiceException);

        final DeleteHandler handler = new DeleteHandler();

        final ResourceHandlerRequest<ResourceModel> request = createResourceHandlerRequest();

        Assertions.assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }

    @Test
    public void handleRequest_invalidNatGateway(){
        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("NatGatewayMalformed").build();
        final AwsServiceException awsServiceException = AwsServiceException.builder().awsErrorDetails(awsErrorDetails).build();

        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenThrow(awsServiceException);

        final DeleteHandler handler = new DeleteHandler();

        final ResourceHandlerRequest<ResourceModel> request = createResourceHandlerRequest();

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
    }
}
