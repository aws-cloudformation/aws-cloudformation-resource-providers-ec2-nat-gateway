package software.amazon.ec2.natgateway;

import com.google.common.collect.Lists;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.paginators.DescribeNatGatewaysIterable;

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
   * Convert SDK Tags List to Model Tags
   * @param sdkTags - SDK Tag List - from an SDK call
   * @return List of Model Tags
   */
  static List<Tag> convertSDKTagsToModelTags(final List<software.amazon.awssdk.services.ec2.model.Tag> sdkTags) {
    List<Tag> modelTags = new ArrayList<>();
    sdkTags.forEach((tag) -> { modelTags.add(Tag.builder().key(tag.key()).value(tag.value()).build());});
    return modelTags;
  }

  /**
   * Convert SDK Tags Map to Model Tags List. Returns a List of Tags.
   *
   */
  static List<Tag> convertSDKTagsToModelTags(final Map<String, String> tags) {
    List<Tag> modelTags = new ArrayList<Tag>();
    if (tags != null) {
      tags.forEach((tagKey, tagValue) -> modelTags.add(Tag.builder().key(tagKey).value(tagValue).build()));
    }

    return modelTags.isEmpty() ? null : modelTags;
  }



  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateNatGatewayRequest translateToCreateRequest(final ResourceModel model) {
    return CreateNatGatewayRequest.builder()
            .subnetId(model.getSubnetId())
            .allocationId(model.getAllocationId())
            .build();
  }

  static CreateTagsRequest translateToCreateTagsRequest(final List<Tag> tagsToAdd, final String natGatewayId) {
    return CreateTagsRequest.builder()
            .tags(convertModelTagstoSDKTags(tagsToAdd))
            .resources(natGatewayId)
            .build();
  }

  static List<software.amazon.awssdk.services.ec2.model.Tag> convertModelTagstoSDKTags(final List<Tag> modelTags) {
    List<software.amazon.awssdk.services.ec2.model.Tag> sdkTags = new ArrayList<>();
    modelTags.forEach((tag) -> { sdkTags.add(software.amazon.awssdk.services.ec2.model.Tag.builder().key(tag.getKey()).value(tag.getValue()).build());});
    return sdkTags;
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeNatGatewaysRequest translateToReadRequest(final ResourceModel model) {
    return DescribeNatGatewaysRequest.builder()
            .natGatewayIds(model.getNatGatewayId())
            .build();
  }

  static ResourceModel translateFromReadResponse(final DescribeNatGatewaysResponse response) {

      NatGateway nat = response.natGateways().get(0);
      String natAllocationId = null;

      if(nat.hasNatGatewayAddresses())
      natAllocationId = nat.natGatewayAddresses().get(0).allocationId();

      return ResourceModel.builder()
              .natGatewayId(nat.natGatewayId())
              .allocationId(natAllocationId)
              .connectivityType(nat.connectivityTypeAsString())
              .subnetId(nat.subnetId())
              .tags(convertSDKTagsToModelTags(nat.tags()))
        .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteNatGatewayRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteNatGatewayRequest.builder()
            .natGatewayId(model.getNatGatewayId())
            .build();
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

  static DeleteTagsRequest translateToDeleteTagsRequest(List<Tag> tagsToRemove, String natGatewayId) {
    return DeleteTagsRequest.builder()
            .tags(convertModelTagstoSDKTags(tagsToRemove))
            .resources(natGatewayId)
            .build();
  }

  static DescribeNatGatewaysRequest translateToListRequest(final ResourceModel model) {

    DescribeNatGatewaysRequest describeNatGatewaysRequest = DescribeNatGatewaysRequest.builder().build();
    return describeNatGatewaysRequest;
  }

  static List<ResourceModel> translateFromListResponse(final DescribeNatGatewaysResponse response) {

    return streamOfOrEmpty(response.natGateways())
            .map(item -> ResourceModel.builder()
                    .natGatewayId(item.natGatewayId())
                    .build())
            .collect(Collectors.toList());

  }
}
