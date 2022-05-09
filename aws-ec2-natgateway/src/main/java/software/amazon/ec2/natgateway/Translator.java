package software.amazon.ec2.natgateway;

import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateNatGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DeleteNatGatewayRequest;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.NatGateway;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Translator class is used for
 *  - api request construction (For Nat Gateway APIs and Tagging APIs)
 *  - object translation to/from ec2 sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a NAT Gateway resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateNatGatewayRequest translateToCreateRequest(final ResourceModel model, final ResourceHandlerRequest<ResourceModel> request, final String token) {
    return CreateNatGatewayRequest.builder()
            .clientToken(token)
            .subnetId(model.getSubnetId())
            .allocationId(model.getAllocationId())
            .connectivityType(model.getConnectivityType())
            .tagSpecifications(convertNatTagsToTagSpecification(request.getDesiredResourceTags(), request.getSystemTags()).orElse(null))
            .build();
  }

  /**
   * Request to read a NAT Gateway resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeNatGatewaysRequest translateToReadRequest(final ResourceModel model) {
    return DescribeNatGatewaysRequest.builder()
            .natGatewayIds(Collections.singletonList(model.getNatGatewayId()))
            .build();
  }

  /**
   * Translates NAT Gateway from the Describe response into a NAT Gateway resource model
   * @param natGateway given Nat Gateway received from a Describe response
   * @return model AWS::EC2::NatGateway resource model
   */
  static ResourceModel translateNatGatewayToResourceModel(final NatGateway natGateway) {
    return ResourceModel.builder()
            .natGatewayId(natGateway.natGatewayId())
            .subnetId(natGateway.subnetId())
            .connectivityType(natGateway.connectivityTypeAsString())
            .allocationId(natGateway.natGatewayAddresses().get(0).allocationId())
            .tags(convertToNatTags(natGateway.tags()).stream().filter(n -> !n.getKey().startsWith("aws:")).collect(Collectors.toList()))
            .build();
  }

  /**
   * Request to delete a NAT Gateway resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteNatGatewayRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteNatGatewayRequest.builder().natGatewayId(model.getNatGatewayId()).build();
  }

  /**
   * Request to create Tags for the NAT Gateway resource
   * @param tagsToCreate Tags from the NAT Gateway resource model to create and add to the resource
   * @param model resource model
   * @return awsRequest the aws service request to create the tags
   */
  static CreateTagsRequest translateToCreateTagsRequest(Map<String, String> tagsToCreate, final ResourceModel model) {
    return CreateTagsRequest.builder().tags(convertToSdkTags(tagsToCreate)).resources(model.getNatGatewayId()).build();
  }

  /**
   * Request to delete Tags for the NAT Gateway resource
   * @param tagsToDelete Tags from the NAT Gateway resource model to delete and remove from the resource
   * @param model resource model
   * @return awsRequest the aws service request to delete the tags
   */
  static DeleteTagsRequest translateToDeleteTagsRequest(Map<String, String> tagsToDelete, final ResourceModel model) {
    return DeleteTagsRequest.builder().tags(convertToSdkTags(tagsToDelete)).resources(model.getNatGatewayId()).build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static DescribeNatGatewaysRequest translateToListRequest(final String nextToken) {
    return DescribeNatGatewaysRequest.builder().nextToken(nextToken).build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param describeNatGatewaysResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final DescribeNatGatewaysResponse describeNatGatewaysResponse) {
    return streamOfOrEmpty(describeNatGatewaysResponse.natGateways())
        .map(resource -> ResourceModel.builder()
            .natGatewayId(resource.natGatewayId())
            .build())
        .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  /**
   * Converts the tags returned from a response to the Tag type from the Nat Gateway Resource Model.
   * @param tags list of tags
   * @return List of Nat Gateway Resource tags
   */
  static List<software.amazon.ec2.natgateway.Tag> convertToNatTags(final List<Tag> tags) {
    return Optional.ofNullable(tags).orElse(Collections.emptyList())
            .stream()
            .map(tag -> software.amazon.ec2.natgateway.Tag.builder()
                    .key(tag.key())
                    .value(tag.value())
                    .build())
            .collect(Collectors.toList());
  }

  /**
   * Converts the tags returned from the Nat Gateway Resource Model to the Tag type from the SDK.
   * @param tags List of Nat Gateway Resource tags
   * @return List of SDK tags
   */
  static List<Tag> convertToSdkTags(Map<String, String> tags) {
    return Optional.ofNullable(tags.entrySet()).orElse(Collections.emptySet())
            .stream()
            .map(tag -> Tag.builder()
                    .key(tag.getKey())
                    .value(tag.getValue())
                    .build())
            .collect(Collectors.toList());
  }

  /**
   * Converts the tags from the Nat Gateway Resource Model to the type TagSpecification to pass into requests.
   * @param modelTags list of Nat Gateway resource tags
   * @return List of TagSpecification tags
   */
  private static Optional<List<TagSpecification>> convertNatTagsToTagSpecification(Map<String, String> modelTags, Map<String, String> systemTags) {
    Map<String, String> allTags = new HashMap<String, String>();
    if (modelTags == null && systemTags == null) {
      return Optional.empty();
    }

    if (modelTags != null) {
      allTags.putAll(modelTags);
    }

    if (systemTags != null) {
      allTags.putAll(systemTags);
    }
    return Optional.of(Arrays.asList(TagSpecification.builder()
            .resourceType("natgateway")
            .tags(convertToSdkTags(allTags))
            .build()));
  }
}
