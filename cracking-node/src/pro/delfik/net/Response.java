package pro.delfik.net;

import lombok.Data;

import java.util.Map;

@Data
public class Response {

	private final int code;
	private final String message;
	private final Map<String, String> headers;
	private final Map<String, String> cookies;
	private final byte[] body;
	private String[] args;

	public String getHeader(String name) {
		if (name == null) return headers.get(null);
		return headers.get(name.toLowerCase());
	}

	public Response args(String... args) {
		this.args = args;
		return this;
	}

}
