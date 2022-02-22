package software.amazon.ec2.natgateway;

import com.google.common.collect.Lists;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateNatGatewayRequest translateToCreateRequest(final  ResourceModel model) {
    return CreateNatGatewayRequest.builder().subnetId(model.getSubnetId())
            .allocationId(model.getAllocationId())
            .connectivityType(model.getConnectivityType())
            .tagSpecifications(convertNatTagsToTagSpecification(model.getTags()).orElse(null))
            .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeNatGatewaysRequest translateToReadRequest(final ResourceModel model) {
    return DescribeNatGatewaysRequest.builder().natGatewayIds(Collections.singletonList(model.getId())).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param describeNatGatewaysResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final DescribeNatGatewaysResponse describeNatGatewaysResponse) {
    NatGateway natGateway = describeNatGatewaysResponse.natGateways().get(0);
    return ResourceModel.builder()
            .id(natGateway.natGatewayId())
            .subnetId(natGateway.subnetId())
            .connectivityType(natGateway.connectivityTypeAsString())
            .allocationId(natGateway.natGatewayAddresses().get(0).allocationId())
            .tags(convertToNatTags(natGateway.tags()))
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteNatGatewayRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteNatGatewayRequest.builder().natGatewayId(model.getId()).build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static AwsRequest translateToFirstUpdateRequest(final ResourceModel model) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L45-L50
    return awsRequest;
  }

  /**
   * Request to update some other properties that could not be provisioned through first update request
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static AwsRequest translateToSecondUpdateRequest(final ResourceModel model) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    return awsRequest;
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static AwsRequest translateToListRequest(final String nextToken) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L26-L31
    return awsRequest;
  }

  /** TODO: implement method
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L75-L82
    return streamOfOrEmpty(Lists.newArrayList())
        .map(resource -> ResourceModel.builder()
            // include only primary identifier
            .build())
        .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  /**
   * Request to add tags to a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static AwsRequest tagResourceRequest(final ResourceModel model, final Map<String, String> addedTags) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    return awsRequest;
  }

  /**
   * Request to add tags to a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static AwsRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    return awsRequest;
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
   * Converts the tags from the Nat Gateway Resource Model to the type TagSpecification to pass into requests.
   * @param tags list of Nat Gateway resource tags
   * @return List of TagSpecification tags
   */
  private static Optional<List<TagSpecification>> convertNatTagsToTagSpecification(final List<software.amazon.ec2.natgateway.Tag> modelTags) {
    if (modelTags == null) {
      return Optional.empty();
    }
    List<Tag> tags = Optional.of(modelTags).orElse(Collections.emptyList())
            .stream()
            .map(tag -> Tag.builder()
                    .key(tag.getKey())
                    .value(tag.getValue())
                    .build())
            .collect(Collectors.toList());
    return Optional.of(Arrays.asList(TagSpecification.builder()
            .resourceType("natgateway")
            .tags(tags)
            .build()));
  }
}
