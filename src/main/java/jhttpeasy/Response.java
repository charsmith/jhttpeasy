package jhttpeasy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * A class that holds the response for {@link Requests#execute()}
 */
public class Response {
	private int code;
	private Map<String, List<String>> headers;
	private InputStream inputStream;
	private String content;
	
	protected Response(int code, Map<String, List<String>> headers, InputStream inputStream) {
		this.code = code;
		this.headers = headers;
		this.inputStream = inputStream;
	}
	
	/**
	 * @return the integer code from the response. (200, 404, etc.)
	 */
	public int code() {
		return code;
	}
	
	/**
	 * @return The map of headers returned from the server.
	 */
	public Map<String, List<String>> headers() {
		return headers;
	}
	
	/**
	 * @return The first header for the key.  Or null if the header doesn't exist.
	 */
	public String header(String key) {
		if (!headers.containsKey(key)) {
			return null;
		}
		return headers.get(key).get(0);
	}

	/**
	 * @return The list of headers for a key or null if the header doesn't exist.
	 */
	public List<String> headerList(String key) {
		if (!headers.containsKey(key)) {
			return null;
		}
		return headers.get(key);
	}
	
	/**
	 * @return The input stream from the response
	 * @see {@link HttpURLConnection#getInputStream()}
	 */
	public InputStream contentStream() {
		return inputStream;
	}
	
	/**
	 * @return A {@link BufferedInputStream} wrapping the response's {@link HttpURLConnection#getInputStream()}
	 */
	public InputStream bufferedContent() {
		return new BufferedInputStream(contentStream());
	}
	
	/**
	 * @return A String holding the response from the server.
	 * @throws IOException Thrown if there is an error reading from the server.
	 */
	public String content() throws IOException {
		if (content == null && inputStream != null) {
			byte[] b = new byte[65536];
			StringBuilder sb = new StringBuilder();
			int len = 0;
			while (-1 != (len = inputStream.read(b))) {
				sb.append(new String(b, 0, len));
			}
			inputStream.close();
			content = sb.toString();
		}
		return content;
	}

}
