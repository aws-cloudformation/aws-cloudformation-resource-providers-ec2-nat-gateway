package software.amazon.ec2.natgateway;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.ec2.model.NatGateway;
import software.amazon.awssdk.services.ec2.model.NatGatewayAddress;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class AbstractTestBase {
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final LoggerProxy logger;

  protected final String NAT_ID = "nat-0123456789abcdefg";
  protected final String ALT_NAT_ID = "nat-0abcdefg123456789";
  protected final String SUBNET_ID = "subnet-0123456789abcdefg";
  protected final String ALLOC_ID = "eipalloc-0123456789abcdefg";
  protected final String CONN_PUBLIC = "public";
  protected final String CONN_PRIVATE = "private";
  protected final String NEXT_TOKEN = "token-1";
  protected final Tag TAG_1 = Tag.builder().key("Key_1").value("Value_1").build();
  protected final Tag TAG_2 = Tag.builder().key("Key_2").value("Value_2").build();
  protected final List<Tag> TAGS = Collections.singletonList(TAG_1);

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    logger = new LoggerProxy();
  }
  static ProxyClient<Ec2Client> MOCK_PROXY(
    final AmazonWebServicesClientProxy proxy,
    final Ec2Client ec2Client) {
    return new ProxyClient<Ec2Client>() {
      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
      injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
        return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
      CompletableFuture<ResponseT>
      injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
      IterableT
      injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
        return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
      injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
      injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Ec2Client client() {
        return ec2Client;
      }
    };
  }

  protected ResourceHandlerRequest<ResourceModel> createResourceHandlerRequest() {
    final ResourceModel model = ResourceModel.builder()
            .natGatewayId(NAT_ID)
            .subnetId(SUBNET_ID)
            .connectivityType(CONN_PUBLIC)
            .tags(Translator.convertToNatTags(TAGS))
            .allocationId(ALLOC_ID)
            .build();
    return ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
  }

  protected NatGateway buildNatGatewayModel(String natId, String connType, String state) {
    NatGatewayAddress addr = connType.equals(CONN_PUBLIC) ? NatGatewayAddress.builder().allocationId(ALLOC_ID).build() : null;
    return NatGateway.builder()
            .natGatewayId(natId)
            .subnetId(SUBNET_ID)
            .connectivityType(connType)
            .tags(TAGS)
            .state(state)
            .natGatewayAddresses(addr)
            .build();
  }
}
