package software.amazon.ec2.natgateway;

import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.NatGateway;
import software.amazon.awssdk.services.ec2.model.State;
import software.amazon.cloudformation.proxy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase{

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
        final NatGateway natGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.AVAILABLE.toString());
        final NatGateway alternateNatGateway = buildNatGatewayModel(ALT_NAT_ID, CONN_PUBLIC, State.AVAILABLE.toString());

        final DescribeNatGatewaysResponse describeResponse = DescribeNatGatewaysResponse.builder().
                natGateways(natGateway, alternateNatGateway).nextToken(NEXT_TOKEN).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenReturn(describeResponse);

        final ListHandler handler = new ListHandler();

        final ResourceModel model1 = ResourceModel.builder().id(NAT_ID).build();
        final ResourceModel model2 = ResourceModel.builder().id(ALT_NAT_ID).build();

        final ResourceHandlerRequest<ResourceModel> request = createResourceHandlerRequest();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels()).contains(model1, model2);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ListWithDeletedNat() {
        final NatGateway natGateway = buildNatGatewayModel(NAT_ID, CONN_PUBLIC, State.AVAILABLE.toString());
        final NatGateway alternateNatGateway = buildNatGatewayModel(ALT_NAT_ID, CONN_PUBLIC, State.DELETED.toString());

        final DescribeNatGatewaysResponse describeResponse = DescribeNatGatewaysResponse.builder().
                natGateways(natGateway, alternateNatGateway).nextToken(NEXT_TOKEN).build();
        when(proxyClient.client().describeNatGateways(ArgumentMatchers.any(DescribeNatGatewaysRequest.class))).thenReturn(describeResponse);

        final ListHandler handler = new ListHandler();

        final ResourceModel model1 = ResourceModel.builder().id(NAT_ID).build();
        final ResourceModel model2 = ResourceModel.builder().id(ALT_NAT_ID).build();

        final ResourceHandlerRequest<ResourceModel> request = createResourceHandlerRequest();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels()).contains(model1);
        assertThat(response.getResourceModels()).doesNotContain(model2);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
