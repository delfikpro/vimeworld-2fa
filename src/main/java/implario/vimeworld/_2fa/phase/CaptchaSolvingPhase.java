package implario.vimeworld._2fa.phase;

import clepto.vk.reverse.message.OutcomingMessage;
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

	public final URI methodUri = UrlBuilder.fromString("https://api.anti-captcha.com/getTaskResult").toUri();
	public final URI vimeControlPanelUri = UrlBuilder.fromString("https://cp.vimeworld.ru/").toUri();
	public final Pattern cookiePhpsessidPattern = Pattern.compile("PHPSESSID=([A-Za-z0-9_-]+)");
	private final HttpRequest.BodyPublisher requestBody;

	public CaptchaSolvingPhase(App app, Account account, int antiCaptchaTaskId) {
		super("captcha-bypass", app, account);
		JsonObject json = new JsonObject();
		json.addProperty("clientKey", app.getAntiCaptchaToken());
		json.addProperty("taskId", antiCaptchaTaskId);
		this.requestBody = HttpRequest.BodyPublishers.ofString(app.getGson().toJson(json));
	}

	@Override
	public void tick0() throws Exception {
		this.nextTickAfter(10000);
		HttpRequest request = HttpRequest.newBuilder(methodUri).POST(this.requestBody).build();
		HttpResponse<String> response = app.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		String body = response.body();
		app.getMainLogger().info(account + " captcha: " + body);
		JsonObject taskInfo = app.getGson().fromJson(body, JsonObject.class);
		if (taskInfo.has("errorId") && taskInfo.get("errorId").getAsInt() != 0) {
			app.getVkSession().sendMessage(new OutcomingMessage("Хаха гугловская капча упала"), app.getVkMainPeer());
			WaitingPhase phase = new WaitingPhase(this.app, this.account);
			account.setPhase(phase);
			phase.setApproved(true);
			phase.tick();
			return;
		}
		if (!taskInfo.get("status").getAsString().equals("ready")) return;
		String gRecaptchaResponse = taskInfo.get("solution").getAsJsonObject().get("gRecaptchaResponse").getAsString();

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
