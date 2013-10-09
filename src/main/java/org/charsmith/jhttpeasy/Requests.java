package org.charsmith.jhttpeasy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

/**
 * Just simple interface over HttpUrlConnection. Trying to create a reasonable
 * interface without any external dependencies besides core java.<br/>
 * <br/>
 * A get example: <br/>
 * <code>
 * String page = Requests.get('http://www.example.com/')
 * 						 .execute()
 * 						 .content();
 * </code> <br/>
 * A json post example: <br/>
 * <code>
 * String page = Requests.post('http://www.example.com/')
 * 						 .body("{\"json\":\"field\"}")
 * 						 .sendJson()
 * 						 .execute()
 * 						 .content();
 * </code> <br/>
 * Post form encoded example: <br/>
 * <code>
 * String page = Requests.post('http://www.example.com/')
 * 						 .data("field1", "value1")
 * 						 .data("field2", "value2")
 * 						 .execute()
 * 						 .content();
 * </code> <br/>
 * This won't prevent you from doing incompatible things like trying to send
 * json and form data at the same time. This behavior is currently undefined.
 */
public class Requests {
	public enum Method {
		DELETE, GET, HEAD, POST, PUT
	};

	public static class AllowAllHostNamesVerifier implements HostnameVerifier {
		public boolean verify(String urlHostName, SSLSession session) {
			return true;
		}
	}

	public static Requests delete(String url) throws MalformedURLException {
		return method(Method.DELETE, url);
	}

	public static Requests get(String url) throws MalformedURLException {
		return method(Method.GET, url);
	}

	public static Requests method(Method method, String url) throws MalformedURLException {
		Requests req = new Requests();
		req.method = method;
		req.url = url;
		return req;
	}

	public static Requests post(String url) throws MalformedURLException {
		return method(Method.POST, url);
	}

	public static Requests put(String url) throws MalformedURLException {
		return method(Method.PUT, url);
	}

	private byte[] body = null;
	private int connectionTimeout = 1000;
	private Map<String, String> data = new HashMap<String, String>();
	private boolean followRedirects = true;

	private Map<String, String> headers = new HashMap<String, String>();

	private Method method;

	private Map<String, String> params = new HashMap<String, String>();

	private int readTimeout = 1000;

	private String url;
	private SSLSocketFactory socketFactory;
	private boolean allowAllHostnames;

	public Requests allowAllHostnames() {
		allowAllHostnames = true;
		return this;
	}

	public Requests allowAllHostnames(boolean allowAllHostnames) {
		this.allowAllHostnames = allowAllHostnames;
		return this;
	}

	public Requests body(String body) {
		this.body = body.getBytes();
		return this;
	}

	public Requests body(byte[] body) {
		this.body = body;
		return this;
	}

	/**
	 * timeout while connecting. Defaults to 1000 milliseconds
	 */
	public Requests connectionTimeout(int milliseconds) {
		connectionTimeout = milliseconds;
		return this;
	}

	/**
	 * Adds values to the data to be sent. Use this for form encoded posts.
	 */
	public Requests data(Map<String, String> values) {
		data.putAll(values);
		return this;
	}

	/**
	 * Adds value to the data to be sent. Use this for form encoded posts.
	 */
	public Requests data(String name, String value) {
		data.put(name, value);
		return this;
	}

	/**
	 * Make the actual call and return response.
	 * 
	 * @return {@link Response}
	 * @throws IOException
	 *             Thrown if there was an error writing to the server.
	 * @throws MalformedURLException
	 *             Thrown if the url is invalid.
	 */
	public Response execute() throws IOException {
		URL fullUrl = new URL(url + getParams());

		HttpURLConnection conn = (HttpURLConnection) fullUrl.openConnection();
		if (conn instanceof HttpsURLConnection) {
			((HttpsURLConnection) conn).setSSLSocketFactory(socketFactory);
			if (allowAllHostnames) {
				((HttpsURLConnection) conn).setHostnameVerifier(new AllowAllHostNamesVerifier());
			}
		}
		for (Map.Entry<String, String> header : headers.entrySet()) {
			conn.addRequestProperty(header.getKey(), header.getValue());
		}
		conn.setRequestMethod(method.name());
		conn.setInstanceFollowRedirects(followRedirects);
		conn.setConnectTimeout(connectionTimeout);
		conn.setReadTimeout(readTimeout);
		if (data.size() > 0) {
			conn.setDoOutput(true);
			writeForm(conn);
		} else if (body != null) {
			conn.setDoOutput(true);
			writeBytes(conn, body);
		}

		InputStream is = null;
		int code = conn.getResponseCode();
		if (code / 200 > 1) {
			is = conn.getErrorStream();
		} else {
			is = conn.getInputStream();
		}
		return new Response(code, conn.getHeaderFields(), is);
	}

	private String getParams() throws UnsupportedEncodingException {
		StringBuilder filter = new StringBuilder();
		if (params.size() > 0) {
			filter.append('?');
		}
		boolean first = true;
		for (Map.Entry<String, String> p : params.entrySet()) {
			if (!first) {
				filter.append('&');
			}
			filter.append(URLEncoder.encode(p.getKey(), "UTF-8"));
			filter.append('=');
			filter.append(URLEncoder.encode(p.getValue(), "UTF-8"));
			first = false;
		}
		return filter.toString();
	}

	/**
	 * Whether the call should follow redirects of the server. Defaults to true.
	 */
	public Requests followRedirects(boolean value) {
		followRedirects = value;
		return this;
	}

	public Method getMethod() {
		return method;
	}

	public String getUrl() {
		return url;
	}

	/**
	 * Set a header in the call: name: value
	 */
	public Requests header(String name, String value) {
		headers.put(name, value);
		return this;
	}

	/**
	 * Adds multiple headers to a call
	 */
	public Requests headers(Map<String, String> values) {
		headers.putAll(values);
		return this;
	}

	/**
	 * Adds param to url -> url?name=value
	 */
	public Requests param(String name, String value) {
		params.put(name, value);
		return this;
	}

	/**
	 * Adds multiple params to url -> url?name1=value1&name2=value2&...
	 */
	public Requests params(Map<String, String> values) {
		params.putAll(values);
		return this;
	}

	/**
	 * timeout while reading. Default is 1000 milliseconds
	 */
	public Requests readTimeout(int milliseconds) {
		readTimeout = milliseconds;
		return this;
	}

	/**
	 * Short cut for setting content-type: application/json in header
	 */
	public Requests sendJson() {
		return header("Content-Type", "application/json");
	}

	protected void writeBytes(HttpURLConnection conn, byte[] outBytes) throws IOException {
		conn.setRequestProperty("Content-Length", Integer.toString(outBytes.length));
		conn.getOutputStream().write(outBytes);
		conn.getOutputStream().close();
	}

	protected void writeForm(HttpURLConnection conn) throws IOException {
		boolean first = true;
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		StringBuilder out = new StringBuilder();
		for (Map.Entry<String, String> d : data.entrySet()) {
			if (!first) {
				out.append('&');
			}
			out.append(URLEncoder.encode(d.getKey(), "UTF-8"));
			out.append('=');
			out.append(URLEncoder.encode(d.getValue(), "UTF-8"));
			first = false;
		}

		writeBytes(conn, out.toString().getBytes());
	}

	public Requests socketFactory(SSLSocketFactory socketFactory) {
		this.socketFactory = socketFactory;
		return this;
	}
}
