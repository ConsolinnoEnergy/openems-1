package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Represents a JSON-RPC Request for 'getActiveComponents'.
 * This will return all currently configured Components with their corresponding Configuration.
 * NOTE: This does not contain information about the Channels.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getActiveComponents",
 *   "params": {}
 * }
 * </pre>
 */
public class GetActiveComponentsRequest extends JsonrpcRequest {

    public static final String METHOD = "getActiveComponents";

    /**
     * Create {@link GetActiveComponentsRequest} from a template {@link JsonrpcRequest}.
     *
     * @param r the template {@link JsonrpcRequest}
     * @return the {@link GetActiveComponentsRequest}
     * @throws OpenemsNamedException on parse error
     */
    public static GetActiveComponentsRequest from(JsonrpcRequest r) throws OpenemsException {
        return new GetActiveComponentsRequest(r);
    }

    private GetActiveComponentsRequest(JsonrpcRequest request) {
        super(request, METHOD);
    }

    @Override
    public JsonObject getParams() {
        return new JsonObject();
    }

}