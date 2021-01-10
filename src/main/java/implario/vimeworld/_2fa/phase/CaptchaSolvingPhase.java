package implario.vimeworld._2fa.phase;

import implario.vk.model.message.OutcomingMessage;
import com.google.gson.JsonObject;
import implario.vimeworld._2fa.Account;
import implario.vimeworld._2fa.App;
import io.mikael.urlbuilder.UrlBuilder;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaptchaSolvingPhase extends Phase {

	public final URI methodUri;
	public final URI vimeControlPanelUri = UrlBuilder.fromString("https://cp.vimeworld.ru/").toUri();
	public final Pattern cookiePhpsessidPattern = Pattern.compile("PHPSESSID=([A-Za-z0-9_-]+)");

	public CaptchaSolvingPhase(App app, Account account, String antiCaptchaTaskId) {
		super("captcha-bypass", app, account);

		this.methodUri = UrlBuilder.fromString("https://rucaptcha.com/res.php?key=" + app.getAntiCaptchaToken() + "&json=1&id=" + antiCaptchaTaskId + "&action=get").toUri();
	}

	@Override
	public void tick0() throws Exception {
		this.nextTickAfter(10000);
		HttpRequest request = HttpRequest.newBuilder(methodUri).GET().build();
		HttpResponse<String> response = app.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		String body = response.body();
		app.getMainLogger().info(account + " captcha: " + body);
		JsonObject taskInfo = app.getGson().fromJson(body, JsonObject.class);

		if (taskInfo.get("request").getAsString().equals("CAPCHA_NOT_READY")) return;
		if (taskInfo.get("status").getAsInt() == 0 && !taskInfo.get("request").getAsString().equals("CAPCHA_NOT_READY")) {
			app.getVkSession().sendMessage(new OutcomingMessage("Хаха я словил ошибку " + taskInfo.get("request").getAsString() + " от Rucaptcha"), app.getVkMainPeer());
			FinishedPhase phase = new FinishedPhase(this.app, this.account, false);
			account.setPhase(phase);
			phase.tick();
			return;
		}

		String gRecaptchaResponse = taskInfo.get("request").getAsString();

		HttpResponse<String> vimeResponse = this.app.syncRequest(HttpRequest.newBuilder(vimeControlPanelUri));
		this.app.getMainLogger().info(vimeResponse.statusCode() + " " + vimeResponse);
		String phpsessid = null;
		for (String cookieStr : vimeResponse.headers().allValues("set-cookie")) {
			Matcher matcher = cookiePhpsessidPattern.matcher(cookieStr);
			if (!matcher.find()) continue;
			phpsessid = matcher.group(1);
		}
		if (phpsessid == null) throw new IllegalStateException("No PHPSESSID cookie from vimeworld control panel");
		this.account.setPhpsessid(phpsessid);
		this.app.saveAccounts();
		LoginPhase phase = new LoginPhase(this.app, this.account, gRecaptchaResponse);
		this.account.setPhase(phase);
		phase.tick();
	}

}
