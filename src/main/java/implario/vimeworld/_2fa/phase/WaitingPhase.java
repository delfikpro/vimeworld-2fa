package implario.vimeworld._2fa.phase;

import com.google.gson.JsonObject;
import implario.vimeworld._2fa.Account;
import implario.vimeworld._2fa.App;
import implario.vk.model.message.OutcomingMessage;
import io.mikael.urlbuilder.UrlBuilder;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WaitingPhase extends Phase {

	private final URI methodUri;

	@Setter
	private volatile boolean approved;

	public WaitingPhase(App app, Account account) {
		super("waiting", app, account);

		methodUri = UrlBuilder.fromString("https://rucaptcha.com/in.php?key=" + app.getAntiCaptchaToken() + "&method=userrecaptcha&json=1&userAgent=" + app.getUserAgent() + "&googlekey=" + app.getVimeworldCaptchaSecret() + "&pageurl=https://cp.vimeworld.ru/login").toUri();
	}

	@Override
	public void tick0() throws Exception {
		if (!this.approved) return;
		HttpRequest request = HttpRequest.newBuilder(methodUri).GET().build();
		HttpResponse<String> response = app.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		JsonObject object = app.getGson().fromJson(response.body(), JsonObject.class);
		System.out.println(response.body());
		if (object.get("status").getAsInt() == 0) {
			app.getVkSession().sendMessage(new OutcomingMessage("Хаха я словил ошибку " + object.get("request").getAsString() + " от Rucaptcha"), app.getVkMainPeer());
			FinishedPhase phase = new FinishedPhase(this.app, this.account, false);
			account.setPhase(phase);
			phase.tick();
			return;
		}
		String taskId = object.get("request").getAsString();
		account.setPhase(new CaptchaSolvingPhase(app, account, taskId).nextTickAfter(10_000));
	}

}
