package implario.vimeworld._2fa.phase;

import com.google.gson.JsonObject;
import implario.vimeworld._2fa.Account;
import implario.vimeworld._2fa.App;
import io.mikael.urlbuilder.UrlBuilder;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WaitingPhase extends Phase {

	private final URI methodUri = UrlBuilder.fromString("https://api.anti-captcha.com/createTask").toUri();
	private final HttpRequest.BodyPublisher requestBody;

	@Setter
	private volatile boolean approved;

	public WaitingPhase(App app, Account account) {
		super("waiting", app, account);
		JsonObject wrapper = new JsonObject();
		wrapper.addProperty("clientKey", app.getAntiCaptchaToken());
		JsonObject json = new JsonObject();
		json.addProperty("type", "NoCaptchaTask" + (app.getProxyAddress() == null ? "Proxyless" : ""));
		json.addProperty("websiteURL", "https://cp.vimeworld.ru/login");
		json.addProperty("websiteKey", app.getVimeworldCaptchaSecret());
		json.addProperty("userAgent", app.getUserAgent());
		if (app.getProxyType() != null) {
			json.addProperty("proxyType", app.getProxyType().toLowerCase());
			json.addProperty("proxyAddress", app.getProxyAddress());
			json.addProperty("proxyPort", app.getProxyPort());
		}
		wrapper.add("task", json);
		requestBody = HttpRequest.BodyPublishers.ofString(app.getGson().toJson(wrapper));
	}

	@Override
	public void tick0() throws Exception {
		if (!this.approved) return;
		HttpRequest request = HttpRequest.newBuilder(methodUri).POST(requestBody).build();
		HttpResponse<String> response = app.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		JsonObject object = app.getGson().fromJson(response.body(), JsonObject.class);
		System.out.println(response.body());
		int taskId = object.get("taskId").getAsInt();
		account.setPhase(new CaptchaSolvingPhase(app, account, taskId).nextTickAfter(10_000));
	}

}
