package pro.delfik.vk;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import pro.delfik.net.Method;
import pro.delfik.net.Request;
import pro.delfik.net.Response;

import java.io.ByteArrayInputStream;
import java.net.Proxy;

import static lombok.AccessLevel.PROTECTED;

@RequiredArgsConstructor(access = PROTECTED)
@Getter
public abstract class VkModule {

	private final VKBot bot;
	private final String sectionName;

	protected Request request(String method) {
		Request request = new Request("https://api.vk.com/method/" + sectionName + "." + method, Method.POST);
		request.body("v", "5.103");
		request.body("access_token", bot.getToken());
		return request;
	}

	protected JSONObject execute(Request request) {
		return execute(request, true);
	}

	protected JSONObject execute(Request request, boolean responseSubobject) {
		Response response = request.execute(Proxy.NO_PROXY);
		byte[] body = response.getBody();

		// Создавать строки очень медленно, поэтому будем читать массив байт
		ByteArrayInputStream stream = new ByteArrayInputStream(body);

		// Теперь этот массив в токены джсона
		JSONTokener tokener = new JSONTokener(stream);

		// И токены в сам объект
		JSONObject json = new JSONObject(tokener);

		if (!responseSubobject) return json;

		// А теперь респонс из этого объекта
		try {
			return json.getJSONObject("response");
		} catch (JSONException ex) {
			System.out.println(json);
			throw ex;
		}
	}

}
