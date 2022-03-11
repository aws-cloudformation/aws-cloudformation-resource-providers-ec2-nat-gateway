package software.amazon.ec2.natgateway;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-ec2-natgateway.json");
    }

    @Override
    public Map<String, String> resourceDefinedTags(ResourceModel resourceModel) {
        return Optional.ofNullable(resourceModel.getTags()).orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(tag -> tag.getKey(), tag -> tag.getValue(), (value1, value2) -> value2));
    }

}
