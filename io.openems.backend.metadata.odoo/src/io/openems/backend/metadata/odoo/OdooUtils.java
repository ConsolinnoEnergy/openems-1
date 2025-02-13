package io.openems.backend.metadata.odoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

public class OdooUtils {

	private OdooUtils() {
	}

	public static final String DEFAULT_SERVER_DATE_FORMAT = "yyyy-MM-dd";
	public static final String DEFAULT_SERVER_TIME_FORMAT = "HH:mm:ss";
	public static final String DEFAULT_SERVER_DATETIME_FORMAT = DEFAULT_SERVER_DATE_FORMAT + " "
			+ DEFAULT_SERVER_TIME_FORMAT;

	public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter
			.ofPattern(DEFAULT_SERVER_DATETIME_FORMAT);

	/**
	 * Sends a JSON-RPC Request to an Odoo server.
	 * 
	 * @param url     the URL
	 * @param request the JSON-RPC Request
	 * @return the JSON-RPC Response (note: Odoo always sends success responses,
	 *         even on error...)
	 * @throws OpenemsNamedException on error
	 */
	public static JsonrpcResponseSuccess sendJsonrpcRequest(String url, JsonrpcRequest request)
			throws OpenemsNamedException {
		HttpURLConnection connection = null;
		try {
			// Open connection to Odoo
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(5000);// 5 secs
			connection.setReadTimeout(5000);// 5 secs
			connection.setRequestProperty("Accept-Charset", "US-ASCII");
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");

			// send JSON-RPC request
			try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream())) {
				out.write(request.toString());
				out.flush();
			}

			// read JSON-RPC response
			StringBuilder sb = new StringBuilder();
			String line = null;
			try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
			}

			JsonrpcResponse response = JsonrpcResponse.from(sb.toString());
			if (response instanceof JsonrpcResponseSuccess) {
				return (JsonrpcResponseSuccess) response;
			} else {
				throw OpenemsError.GENERIC.exception(response);
			}

		} catch (IOException e) {
			e.printStackTrace();
			throw OpenemsError.GENERIC.exception(e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static Object executeKw(String url, Object[] params) throws XmlRpcException, MalformedURLException {
		final XmlRpcClient client = new XmlRpcClient();
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setEnabledForExtensions(true);
		config.setServerURL(new URL(String.format("%s/xmlrpc/2/object", url)));
		config.setConnectionTimeout(10_000 /* 10 seconds */);
		config.setReplyTimeout(60_000 /* 60 seconds */);
		client.setConfig(config);
		return client.execute("execute_kw", params);
	}

	/**
	 * Executes a search on Odoo.
	 * 
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model to query (e.g. 'res.partner')
	 * @param domains     Odoo domain filters
	 * @return Odoo object ids
	 * @throws OpenemsException on error
	 */
	protected static int[] search(OdooCredentials credentials, String model, Domain... domains)
			throws OpenemsException {
		// Add domain filter
		Object[] domain = new Object[domains.length];
		for (int i = 0; i < domains.length; i++) {
			Domain filter = domains[i];
			domain[i] = new Object[] { filter.field, filter.operator, filter.value };
		}
		Object[] paramsDomain = new Object[] { domain };
		// Create request params
		HashMap<Object, Object> paramsRules = new HashMap<Object, Object>();
		String action = "search";
		Object[] params = new Object[] { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(),
				model, action, paramsDomain, paramsRules };
		try {
			// Execute XML request
			Object[] resultObjs = (Object[]) executeKw(credentials.getUrl(), params);
			// Parse results
			int[] results = new int[resultObjs.length];
			for (int i = 0; i < resultObjs.length; i++) {
				results[i] = (int) resultObjs[i];
			}
			return results;
		} catch (Throwable e) {
			throw new OpenemsException("Unable to search from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Reads a record from Odoo.
	 * 
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model to query (e.g. 'res.partner')
	 * @param id          id of model to read
	 * @param fields      fields that should be read
	 * @return the record as a Map
	 * @throws OpenemsException on error
	 */
	protected static Map<String, Object> readOne(OdooCredentials credentials, String model, int id, Field... fields)
			throws OpenemsException {
		// Create request params
		// Add ids
		Object[] paramsIds = new Object[1];
		paramsIds[0] = id;
		// Add fields
		String[] fieldStrings = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			fieldStrings[i] = fields[i].n();
		}
		Map<String, String[]> paramsFields = new HashMap<>();
		paramsFields.put("fields", fieldStrings);
		// Create request params
		Object[] params = new Object[] { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(),
				model, "read", paramsIds, paramsFields };
		try {
			// Execute XML request
			Object[] resultObjs = (Object[]) executeKw(credentials.getUrl(), params);
			// Parse results
			for (int i = 0; i < resultObjs.length;) {
				@SuppressWarnings("unchecked")
				Map<String, Object> result = (Map<String, Object>) resultObjs[i];
				return result;
			}
			throw new OpenemsException("No matching entry found for id [" + id + "]");
		} catch (Throwable e) {
			throw new OpenemsException("Unable to read from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Executes a Search and read on Odoo.
	 * 
	 * @see <a href=
	 *      "https://www.odoo.com/documentation/10.0/api_integration.html">Odoo API
	 *      Integration</a>
	 * 
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model to query (e.g. 'res.partner')
	 * @param domains     Odoo domain filters
	 * @param fields      the Fields
	 * @return Odoo object ids
	 * @throws OpenemsException on error
	 */
	// TODO this method is not yet functional
	protected static Map<String, Object>[] searchAndRead(OdooCredentials credentials, String model, Domain[] domains,
			Field[] fields) throws OpenemsException {
		// Add domain filter
		Object[] domain = new Object[domains.length];
		for (int i = 0; i < domains.length; i++) {
			Domain filter = domains[i];
			domain[i] = new Object[] { filter.field, filter.operator, filter.value };
		}
		Object[] paramsDomain = new Object[] { domain };
		// Add fields
		String[] fieldStrings = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			fieldStrings[i] = fields[i].n();
		}
		Map<String, String[]> paramsFields = new HashMap<>();
		paramsFields.put("fields", fieldStrings);
		// Create request params
		String action = "search_read";
		Object[] params = new Object[] { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(),
				model, action, paramsDomain, paramsFields };
		try {
			// Execute XML request
			executeKw(credentials.getUrl(), params);
			// Object[] resultObjs = (Object[]) executeKw(url, params);
			// Parse results
			// int[] results = new int[resultObjs.length];
			// for (int i = 0; i < resultObjs.length; i++) {
			// results[i] = (int) resultObjs[i];
			// }
			return null;
		} catch (Throwable e) {
			throw new OpenemsException("Unable to search and read from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Reads multiple records from Odoo.
	 * 
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model to query (e.g. 'res.partner')
	 * @param ids         ids of model to read
	 * @param fields      fields that should be read
	 * @return the records as a Map array
	 * @throws OpenemsException on error
	 */
	protected static Map<String, Object>[] readMany(OdooCredentials credentials, String model, Integer[] ids,
			Field... fields) throws OpenemsException {
		// Create request params
		String action = "read";
		// Add ids
		// Object[] paramsIds = Arrays.stream(ids).mapToObj(id -> (Integer)
		// id).toArray();
		// Object[] paramsIds = new Object[2];
		// paramsIds[0] = ids[0];
		// paramsIds[1] = ids[1];
		// Add fields
		String[] fieldStrings = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			fieldStrings[i] = fields[i].n();
		}
		// Map<String, String[]> paramsFields = new HashMap<>();
		// paramsFields.put("fields", fieldStrings);
		// Create request params
		Object[] params = new Object[] { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(),
				model, action, new Object[] { ids, fieldStrings } };
		try {
			// Execute XML request
			Object[] resultObjs = (Object[]) executeKw(credentials.getUrl(), params);
			// Parse results
			@SuppressWarnings("unchecked")
			Map<String, Object>[] results = (Map<String, Object>[]) new Map[resultObjs.length];
			for (int i = 0; i < resultObjs.length; i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> result = (Map<String, Object>) resultObjs[i];
				results[i] = result;
			}
			return results;
		} catch (Throwable e) {
			throw new OpenemsException("Unable to read from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Search-Reads multiple records from Odoo.
	 * 
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model to query (e.g. 'res.partner')
	 * @param fields      fields that should be read
	 * @param domains     filter domains
	 * @return the records as a Map array
	 * @throws OpenemsException on error
	 */
	protected static Map<String, Object>[] searchRead(OdooCredentials credentials, String model, Field[] fields,
			Domain... domains) throws OpenemsException {
		// Create request params
		// Add domain filter
		Object[] domain = new Object[domains.length];
		for (int i = 0; i < domains.length; i++) {
			Domain filter = domains[i];
			domain[i] = new Object[] { filter.field, filter.operator, filter.value };
		}
		Object[] paramsDomain = new Object[] { domain };
		// Add fields
		String[] fieldStrings = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			fieldStrings[i] = fields[i].toString();
		}
		Map<String, String[]> paramsFields = new HashMap<>();
		paramsFields.put("fields", fieldStrings);
		// Create request params
		Object[] params = new Object[] { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(),
				model, "search_read", paramsDomain, paramsFields };
		try {
			// Execute XML request
			Object[] resultObjs = (Object[]) executeKw(credentials.getUrl(), params);
			// Parse results
			@SuppressWarnings("unchecked")
			Map<String, Object>[] results = (Map<String, Object>[]) new Map[resultObjs.length];
			for (int i = 0; i < resultObjs.length; i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> result = (Map<String, Object>) resultObjs[i];
				results[0] = result;
			}
			return results;
		} catch (Throwable e) {
			throw new OpenemsException("Unable to read from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Adds a message in Odoo Chatter ('mail.thread').
	 * 
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model (e.g. 'res.partner')
	 * @param id          id of model
	 * @param message     the message
	 * @throws OpenemsException on error
	 */
	protected static void addChatterMessage(OdooCredentials credentials, String model, int id, String message)
			throws OpenemsException {
		// Create request params
		Object[] params = new Object[] { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(),
				model, "message_post", new Object[] { id, message } };
		try {
			// Execute XML request
			Object resultObj = executeKw(credentials.getUrl(), params);
			if (resultObj == null) {
				throw new OpenemsException("Returned Null");
			}
		} catch (Throwable e) {
			throw new OpenemsException("Unable to write to Odoo: " + e.getMessage());
		}
	}

	/**
	 * Update a record in Odoo.
	 * 
	 * @param credentials the Odoo credentials
	 * @param model       the Odoo model
	 * @param ids         ids of model to update
	 * @param fieldValues fields and values that should be written
	 * @throws OpenemsException on error
	 */
	protected static void write(OdooCredentials credentials, String model, Integer[] ids, FieldValue<?>... fieldValues)
			throws OpenemsException {
		// // for debugging:
		// StringBuilder b = new StringBuilder("Odoo Write: " + model + "; ");
		// for (int id : ids) {
		// b.append(id + ",");
		// }
		// b.append(";");
		// for (FieldValue fieldValue : fieldValues) {
		// b.append(fieldValue.getField().n() + ",");
		// }
		// System.out.println(b.toString());

		// Create request params
		String action = "write";
		// Add fieldValues
		Map<String, Object> paramsFieldValues = new HashMap<>();
		for (FieldValue<?> fieldValue : fieldValues) {
			paramsFieldValues.put(fieldValue.getField().n(), fieldValue.getValue());
		}
		// Create request params
		Object[] params = new Object[] { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(),
				model, action, new Object[] { ids, paramsFieldValues } };
		try {
			// Execute XML request
			Boolean resultObj = (Boolean) executeKw(credentials.getUrl(), params);
			if (!resultObj) {
				throw new OpenemsException("Returned False.");
			}
		} catch (Throwable e) {
			throw new OpenemsException("Unable to write to Odoo: " + e.getMessage());
		}
	}

	/**
	 * Return the Object type-safe as a String; or otherwise as an empty String.
	 * 
	 * @param object the value as object
	 * @return the value as String
	 */
	protected static String getAsString(Object object) {
		if (object != null && object instanceof String) {
			return (String) object;
		} else {
			return "";
		}
	}

	/**
	 * Return the Object type-safe as a Integer; or otherwise null.
	 * 
	 * @param object the value as object
	 * @return the value as Integer
	 */
	protected static Integer getAsInteger(Object object) {
		if (object != null && object instanceof Integer) {
			return (Integer) object;
		} else {
			return null;
		}
	}
}
