package software.amazon.ec2.natgateway;

import java.util.Map;
import java.util.stream.Collectors;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-ec2-natgateway.json");
    }

    @Override
    public Map<String, String> resourceDefinedTags(final ResourceModel model) {
        if (model.getTags() == null) {
            return null;
        } else {
            return model.getTags().stream().collect(Collectors.toMap(tag -> tag.getKey(), tag -> tag.getValue()));
        }
    }
}
