package pro.delfik.net;

import lombok.ToString;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

@ToString
public class Request {

	private String address;
	private final Method method;
	private final Map<String, String> parameters = new HashMap<>();
	private final Map<String, String> headers = new HashMap<>();
	private final Collection<Cookie> cookies = new ArrayList<>();
	private final Map<String, String> body = new HashMap<>();

	public Request(String address, Method method) {
		this.address = address;
		this.method = method;
	}

	private HttpURLConnection prepare(Proxy proxy) {
		try {
			URL url = new URL(address + bakeParameters());
			HttpURLConnection con = (HttpURLConnection) url.openConnection(proxy);
			con.setInstanceFollowRedirects(false);
			con.setRequestMethod(method.name());
			//			for (Map.Entry<String, String> stringStringEntry : headers.entrySet()) System.out.println(stringStringEntry);
			headers.forEach(con::setRequestProperty);
			if (!cookies.isEmpty()) {
				StringBuilder b = new StringBuilder();
				for (Cookie cookie : cookies) b.append(cookie).append("; ");
				con.setRequestProperty("cookie", b.toString());
			}
			if (!body.isEmpty()) {
				con.setDoOutput(true);
				DataOutputStream out = new DataOutputStream(con.getOutputStream());
				for (Iterator<Map.Entry<String, String>> iterator = body.entrySet().iterator(); iterator.hasNext(); ) {
					Map.Entry<String, String> entry = iterator.next();
					//					System.out.println(entry.getKey() + "=" + entry.getValue());
					out.writeBytes(entry.getKey());
					out.writeBytes("=");
					out.writeBytes(entry.getValue());
					if (iterator.hasNext()) out.writeBytes("&");
				}
				out.flush();
				out.close();
			}

			return con;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Response execute(Proxy proxy) throws Exception {
		HttpURLConnection con = prepare(proxy);

		int code = con.getResponseCode();
		String message = con.getResponseMessage();
		Map<String, String> headers = new HashMap<>();
		Map<String, String> cookies = new HashMap<>();
		for (Map.Entry<String, List<String>> e : con.getHeaderFields().entrySet()) {
			String name = e.getKey() == null ? null : e.getKey().toLowerCase();
			List<String> values = e.getValue();

			if ("set-cookie".equals(name)) {
				for (String cookie : values) {
					String[] args = cookie.split("; ");
					String[] splitted = args[0].split("=");
					cookies.put(splitted[0], splitted[1]);
				}
				continue;
			}

			headers.put(name, values.get(0));
		}
		InputStream inputStream = con.getInputStream();
		byte[] body = NetUtil.readInputStreamFluix(inputStream);
		return new Response(code, message, headers, cookies, body);
	}

	public Request cookie(Cookie cookie) {
		cookies.add(cookie);
		return this;
	}

	public Request header(String header, String value) {
		headers.put(header, value);
		return this;
	}

	public Request body(String key, String value) {
		body.put(encode(key), encode(value));
		return this;
	}

	public Request address(String address) {
		this.address = address;
		return this;
	}

	private String bakeParameters() {
		if (parameters.isEmpty()) return "";
		StringBuilder sb = new StringBuilder(address.contains("?") ? "&" : "?");
		for (Iterator<Map.Entry<String, String>> iterator = parameters.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<String, String> entry = iterator.next();
			sb.append(encode(entry.getKey())).append("=").append(entry.getValue());
			if (iterator.hasNext()) sb.append("&");
		}
		return sb.toString();

	}

	private static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return s;
		}
	}


	public Map<String, String> getBody() {
		return body;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public String getAddress() {
		return address;
	}

	public Request param(String name, String value) {
		parameters.put(name, value);
		return this;
	}

	public Response executeSafe(Proxy proxy) {
		try {
			return execute(proxy);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
