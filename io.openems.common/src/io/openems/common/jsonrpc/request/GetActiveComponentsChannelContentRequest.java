package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Represents a JSON-RPC Request for 'getActiveComponentsChannelContent'.
 * This will return all Channels with their contents of the currently configured Components.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getActiveComponentChannelContent",
 *   "params": {}
 * }
 * </pre>
 */
public class GetActiveComponentsChannelContentRequest extends JsonrpcRequest {

    public static final String METHOD = "getActiveComponentsChannelContent";

    /**
     * Create {@link GetActiveComponentsChannelContentRequest} from a template {@link JsonrpcRequest}.
     *
     * @param r the template {@link JsonrpcRequest}
     * @return the {@link GetActiveComponentsChannelContentRequest}
     * @throws OpenemsNamedException on parse error
     */
    public static GetActiveComponentsChannelContentRequest from(JsonrpcRequest r) throws OpenemsException {
        return new GetActiveComponentsChannelContentRequest(r);
    }

    private GetActiveComponentsChannelContentRequest(JsonrpcRequest request) {
        super(request, METHOD);
    }

    @Override
    public JsonObject getParams() {
        return new JsonObject();
    }

}