package software.amazon.ec2.natgateway;

import java.time.Duration;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
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

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<Ec2Client> proxyClient;

    @Mock
    Ec2Client sdkClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(Ec2Client.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        final DeleteHandler handler = new DeleteHandler();

        final DescribeNatGatewaysResponse describeNatGatewaysResponse1 = DescribeNatGatewaysResponse.builder()
                .natGateways(NatGateway.builder()
                        .natGatewayId("Id")
                        .state(NatGatewayState.AVAILABLE)
                        .build())
                .build();

        final DescribeNatGatewaysResponse describeNatGatewaysResponse2 = DescribeNatGatewaysResponse.builder()
                .natGateways(NatGateway.builder()
                        .natGatewayId("Id")
                        .state(NatGatewayState.DELETED)
                        .build())
                .build();

        final DeleteNatGatewayResponse deleteNatGatewayResponse = DeleteNatGatewayResponse.builder().build();

        AwsServiceException awsException = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode(BaseHandlerStd.INVALID_NAT_GATEWAY_ID_NOT_FOUND_ERROR_CODE).build())
                .build();

        when(proxyClient.client().
                deleteNatGateway(any(DeleteNatGatewayRequest.class))).
                thenReturn(deleteNatGatewayResponse);

        when(proxyClient.client()
                .describeNatGateways(any(DescribeNatGatewaysRequest.class)))
                .thenReturn(describeNatGatewaysResponse1)
                .thenReturn(describeNatGatewaysResponse2);

        final ResourceModel model = ResourceModel.builder()
                .natGatewayId("Id")
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
