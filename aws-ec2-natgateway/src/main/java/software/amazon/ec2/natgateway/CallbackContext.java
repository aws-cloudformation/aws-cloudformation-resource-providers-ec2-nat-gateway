package software.amazon.ec2.natgateway;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
/**
 * CallbackContext is an extension of StdCallbackContext used to provide the given requests or responses that occur during
 * a Handler's progress. This is now empty but could be updated to include variables for the call graph to retain if
 * necessary.
 */
public class CallbackContext extends StdCallbackContext {
}
