package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'GetDependenciesRequest'.
 * This will return all Import Packages from the Edge that aren't from the common Package.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getDependencies",
 *   "params": {
 *       "factoryPid":string
 *   }
 * }
 * </pre>
 */
public class GetDependenciesRequest extends JsonrpcRequest {
    private final String factoryPid;
    public static final String METHOD = "getDependencies";

    /**
     * Create {@link GetDependenciesRequest} from a template {@link JsonrpcRequest}.
     *
     * @param r the template {@link JsonrpcRequest}
     * @return the {@link GetActiveComponentsRequest}
     * @throws OpenemsNamedException on parse error
     */
    public static GetDependenciesRequest from(JsonrpcRequest r) throws OpenemsNamedException {
        JsonObject p = r.getParams();
        String factoryPid = JsonUtils.getAsString(p, "factoryPid");
        return new GetDependenciesRequest(r,factoryPid);
    }

    private GetDependenciesRequest(JsonrpcRequest request,String factoryPid) {
        super(request, METHOD);
        this.factoryPid = factoryPid;
    }

    /**
     * Gets the Factory-PID.
     *
     * @return Factory-PID
     */
    public String getFactoryPid() {
        return this.factoryPid;
    }

    @Override
    public JsonObject getParams() {
        return JsonUtils.buildJsonObject() //
                .addProperty("factoryPid", this.factoryPid) //
                .build();
    }

}