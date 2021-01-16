package implario.vimeworld._2fa;

import implario.LoggerUtils;
import implario.humanize.Humanize;
import implario.humanize.TimeFormatter;
import implario.vk.event.events.messages.NewMessageEvent;
import implario.vk.event.longpoll.LongPollEventListener;
import implario.vk.event.longpoll.*;
import implario.vk.*;
import implario.vk.model.message.Message;
import implario.vk.GroupSession;
import implario.vk.event.longpoll.LongPollData;
import implario.vk.VkClient;
import implario.vk.model.message.OutcomingMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import implario.util.DataIO;
import implario.vimeworld._2fa.phase.*;
import implario.vimeworld._2fa.social.User;
import io.mikael.urlbuilder.UrlBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor (access = AccessLevel.PRIVATE)
public class App {

	@Getter
	private static App instance;

	private static final Logger _vkLogger = LoggerUtils.simpleLogger("VK");
	private static final Logger _mainLogger = LoggerUtils.simpleLogger("2FA");
	public static final TimeFormatter timeFormatter = TimeFormatter.builder().accuracy(1).build();

	private static String dbType = "json";

	public static Connection dbConn = null;

	public static App load() throws IOException {
		Yaml yaml = new Yaml();
		File configFile = new File("2fa.yml");
		Path configPath = configFile.toPath();

		File accs = new File("2fa-accounts.json");

		File perms = new File("2fa-permissions.json");

		if (!configFile.exists()) {
			DataIO.exportResource("/default-2fa.yml", configFile);
			_mainLogger.info("Создаю 2fa.yml, настроим же его скорее!");
			return null;
		}


		_mainLogger.info("Чтение конфигурационных файлов...");

		FileHandler handler = new FileHandler();
		_vkLogger.addHandler(handler);

		Map<String, Object> config = yaml.load(Files.newBufferedReader(configPath));


		dbType = (String) config.get("db-type");
		String[] data;
		String accountsContent = "";
		String permissionsContent = "";


		if (dbType.equals("json")) {
			if (!accs.exists()) {
				DataIO.exportResource("/2fa-accounts.json", accs);
			}
			if (!perms.exists()) {
				DataIO.exportResource("/2fa-permissions.json", perms);
			}
		} else if (dbType.equals("sqlite")) {
			dbConn = SqliteDB.connect();

			SqliteDB.migrate(dbConn);
		}




		String anticaptchaToken = (String) config.get("rucaptcha-token");

		String vimeworldCaptchaSecret = (String) config.get("vimeworld-captcha-secret");
		String userAgent = (String) config.get("user-agent");



		Map<String, Object> proxyConfig = (Map<String, Object>) config.get("proxy");
		String proxyType = proxyConfig == null ? null : (String) proxyConfig.get("type");
		String proxyHost = proxyConfig == null ? null : (String) proxyConfig.get("host");
		int proxyPort = proxyConfig == null ? 0 : (int) proxyConfig.get("port");

		String vkToken = (String) config.get("vk-token");
		int vkBotId = (int) config.get("vk-bot-id");
		int vkMainPeer = (int) config.get("vk-main-peer");

		HttpClient httpClient = HttpClient.newHttpClient();
		VkClient vkClient = new VkClient(httpClient);
		GroupSession session = vkClient.newGroupSession(vkBotId, vkToken);
		LongPollEventListener longPoll = session.createLongPoll(_vkLogger);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		App app = new App(gson, vkMainPeer, anticaptchaToken, vimeworldCaptchaSecret, userAgent,
				proxyType, proxyHost, proxyPort, session, longPoll,
				_mainLogger, _vkLogger);


		if (dbType.equals("json")) {
			accountsContent = Files.readString(new File("2fa-accounts.json").toPath(), StandardCharsets.UTF_8);
			permissionsContent = Files.readString(new File("2fa-permissions.json").toPath(), StandardCharsets.UTF_8);
		} else if (dbType.equals("sqlite")) {
			data = SqliteDB.selectAll(dbConn);
			accountsContent = data[0];
			permissionsContent = data[1];
		}

		if (accountsContent != null) {
			_mainLogger.info("Восстановление сохраненных аккаунтов...");
			Account[] accounts = app.getGson().fromJson(accountsContent, Account[].class);
			app.getAccounts().addAll(Arrays.asList(accounts));
			_mainLogger.info("Загружено " + accounts.length + " аккаунтов(та,т)" + (accounts.length == 1 ? "" : "s") + ".");
		}

		if (accountsContent != null) {
			User[] users = app.getGson().fromJson(permissionsContent, User[].class);
			for (User user : users) {
				app.getUserMap().put(user.getVkId(), user);

			}
			_vkLogger.info("Загружено " + app.getUserMap().size() + " VK пользователей(ля, лей)" + (users.length == 1 ? "" : "s") + ".");
		}
		app.saveAccounts();

		return app;

	}

	private final Gson gson;
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
	private final VkClient vkClient = new VkClient(httpClient);
	private final int vkMainPeer;
	private final String ruCaptchaToken;
	private final String vimeworldCaptchaSecret;
	private final String userAgent;
	private final String proxyType;
	private final String proxyAddress;
	private final int proxyPort;
	private final Set<Account> accounts = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private Set<Account> toRemove = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final GroupSession vkSession;
	private final LongPollEventListener longPoll;
	private final Logger mainLogger;
	private final Logger vkLogger;
	private final Map<Integer, User> userMap = new HashMap<>();
	private volatile boolean running;

	public void start() {
		instance = this;
		longPoll.registerHandler(NewMessageEvent.class, this::handleVkMessage);
		longPoll.start();
		for (Account account : accounts) {
			account.setPhase(account.getPhpsessid() == null ?
							new WaitingPhase(this, account) :
							new GuessPhase(this, account)
							);
		}

		this.running = true;

		while (this.running) {

			for (Account account : new ArrayList<>(accounts)) {
				Phase phase = account.getPhase();
				long time = System.currentTimeMillis();
				if (phase.getNextTickTime() >= time) continue;
				phase.tick();
			}
//
			//			try {
			//				List<Account> captcha = accounts.stream().filter(account -> account.getPhase() instanceof CaptchaSolvingPhase).collect(Collectors.toList());
			//				if (captcha.isEmpty() || captcha.size() < 2) {
			//					accounts.stream().filter(account -> account.getPhase() instanceof WaitingPhase).findFirst().ifPresent(acc -> {
			//						((WaitingPhase) acc.getPhase()).setApproved(true);
			//						vkSession.sendMessage(new OutcomingMessage(acc.getTitle(true) + " получил автоматическое подтверждение"), vkMainPeer);
			//					});
			//				}
			//			} catch (Exception ex) {
			//				mainLogger.log(Level.SEVERE, "Error while auto-approving:", ex);
			//			}

			accounts.removeAll(toRemove);
			toRemove = Collections.newSetFromMap(new ConcurrentHashMap<>());

			this.saveAccounts();

			try {Thread.sleep(10);} catch (InterruptedException e) {}

		}

	}

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy (HH:mm)");

	private void handleVkMessage(NewMessageEvent event) {
		try {

			Message message = event.getMessage();
			String text = message.getText();
			if (text == null || text.isEmpty()) return;
			User user = this.getUser(message.getFromId());
			String[] args = text.split(" ");
			int peerId = message.getPeerId();
			if (args[0].equals("/sail")) {
				if (args.length < 3) {
					this.vkSession.sendMessage(new OutcomingMessage("Чтобы зарегистрировать своё судно на регату - " +
							"используй команду /sail [Имя судна] [Кодовое слово]"), peerId);
					return;
				}
				URI uri = UrlBuilder.fromString("https://api.vime.world/user/name/" + args[1]).toUri();
				httpClient.sendAsync(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString())
						.thenAccept(response -> {
							JsonObject[] users = this.gson.fromJson(response.body(), JsonObject[].class);
							if (users.length == 0) {
								vkSession.sendMessage(new OutcomingMessage("Лодочка под названием " + args[1] + " не найдена во всемирном реестре лодочек.\n" +
										"Быть может ты опечатался, о юный моряк?"), peerId);
								return;
							}
							Optional<Account> existing = accounts.stream().filter(a -> a.getUsername().equalsIgnoreCase(args[1])).findAny();
							String answer;
							if (existing.isPresent()) {
								Account account = existing.get();
								if (account.getOwnerVkId() == message.getFromId()) {
									account.setPassword(args[2]);
									answer = "Кодовое слово лодочки " + account + " обновлено.";
								} else {
									answer = "Это не твоя лодочка :<";
								}
							} else {
								Account account = new Account(users[0].get("id").getAsInt(), users[0].get("username").getAsString(),
										message.getFromId(), System.currentTimeMillis());
								account.setPassword(args[2]);
								account.setPhase(new WaitingPhase(this, account));
								accounts.add(account);
								answer = "Ты зарегистрировал лодочку и стал матросом. Попроси капитана/лоцмана поднять якорь и рвись навстречу волнам!";
							}
							this.saveAccounts();
							vkSession.sendMessage(new OutcomingMessage(answer), peerId);
						});
				return;
			}
			if (args[0].equals("/remove")) {
				if (args.length < 2) {
					this.vkSession.sendMessage(new OutcomingMessage("Использование: /remove [Лодочка]"), peerId);
					return;
				}
				Account account = this.getAccount(args[1]);
				if (!user.getRank().isAtLeast(User.Rank.CAPTAIN) && account.getOwnerVkId() != message.getFromId()) {
					this.vkSession.sendMessage(new OutcomingMessage("Удалять чужие аккаунты может только капитан."), peerId);
					return;
				}
				this.toRemove.add(account);
				this.saveAccounts();
				this.vkSession.sendMessage(new OutcomingMessage("Аккаунт " + args[1] + " удалён."), peerId);
			}
			if (args[0].equals("/repair")) {
				if (args.length < 2) {
					this.vkSession.sendMessage(new OutcomingMessage("Использование: /repair [лодочка]"), peerId);
					return;
				}
				Account account = this.getAccount(args[1]);
				if (account == null || account.getOwnerVkId() != message.getFromId()) {
					this.vkSession.sendMessage(new OutcomingMessage("В списке ваших лодочек не найдено ни одной, которую звали бы " + args[1]), peerId);
					return;
				}
				if (account.getPhase() instanceof RepairPhase) {
					this.vkSession.sendMessage(new OutcomingMessage("Вы починили лодочку " + account), peerId);
					account.setPhase(new WaitingPhase(this, account));
				} else {
					this.vkSession.sendMessage(new OutcomingMessage("Лодочка " + account + " не нуждается в ремонте"), peerId);
				}
				return;
			}
			if (args[0].equals("/try")) {
				if (!user.getRank().isAtLeast(User.Rank.CAPTAIN)) {
					this.vkSession.sendMessage(new OutcomingMessage("Доступ к штурвалу есть только у капитана"), peerId);
					return;
				}
				Account account = this.getAccount(args[1]);
				String code = args[2];
				if (!(account.getPhase() instanceof GuessPhase)) {
					this.vkSession.sendMessage(new OutcomingMessage("Штурвал не работает во фремя фазы " + account.getPhase().getName()), peerId);
					return;
				}
				((GuessPhase) account.getPhase()).setCurrentCode(code);
				this.vkSession.sendMessage(new OutcomingMessage("Штурвал " + account + " повёрнут в направлении " + code), peerId);
				return;
			}
			if (args[0].equals("/peer")) {
				this.vkSession.sendMessage(new OutcomingMessage("peer-id этого диалога - " + peerId), peerId);
				return;
			}
			if (args[0].equals("/boats")) {
				Collection<Account> displayed;
				if (args.length > 1) {
					if (!user.getRank().isAtLeast(User.Rank.CAPTAIN)) {
						vkSession.sendMessage(new OutcomingMessage("Пробивать чужие лодочки по правительственным базам может только капитан"), peerId);
						return;
					}
					if (args[1].equals("*")) displayed = accounts;
					else try {
						int ownerId = Integer.parseInt(args[1].replaceAll("[^0-9]+", ""));
						displayed = accounts.stream().filter(a -> a.getOwnerVkId() == ownerId).collect(Collectors.toList());
					} catch (NumberFormatException ex) {
						vkSession.sendMessage(new OutcomingMessage("Использование: /boats [id в вк]"), peerId);
						return;
					}
				} else displayed = accounts.stream().filter(a -> a.getOwnerVkId() == user.getVkId()).collect(Collectors.toList());

				String msg;
				if (displayed.isEmpty()) {
					msg = "Лодочки не найдены.";
				} else {
					msg = displayed.stream().sorted(Comparator.comparing((Account account) -> account.getPhase().getName()).thenComparingLong(Account::getCreatedTimeStamp))
							.map(account -> {
										int tried = (int) account.getTriedCodes();
										String mileage = tried >= 1000 ? (tried - tried % 100) / 1000.0 + " км" : tried + " м";
										return account.getPhase().getName() + " " + account.getTitle(peerId != message.getFromId()) +
												"  " + mileage + " за " +
												timeFormatter.format((Duration.ofMillis(System.currentTimeMillis() - account.getCreatedTimeStamp()))) + "\n";
									}
								).collect(Collectors.joining());
				}

				vkSession.sendMessage(new OutcomingMessage(msg).disableMentions(), peerId);
				return;
			}
			if (args[0].equals("/stats")) {
				String response = accounts.stream()
						.collect(Collectors.groupingBy(acc -> acc.getPhase().getName()))
						.entrySet().stream()
						.map(e -> e.getKey() + ": " + (peerId == message.getFromId() && user.getRank().isAtLeast(User.Rank.CAPTAIN) ?
								e.getValue().stream()
										.map(account -> account.getTitle(false))
										.collect(Collectors.joining(" | ")) :
								e.getValue().size() + " лодоч" + Humanize.plurals("ка", "ки", "ек", e.getValue().size())))
						.collect(Collectors.joining("\n"));

				this.vkSession.sendMessage(new OutcomingMessage(response), peerId);
			}
			if (args[0].equals("/approve")) {
				if (!user.getRank().isAtLeast(User.Rank.PILOT)) {
					this.vkSession.sendMessage(new OutcomingMessage("Отдать приказ на поднятие якоря может только лоцман или капитан."), peerId);
					return;
				}
				if (args.length < 2) {
					this.vkSession.sendMessage(new OutcomingMessage("Использование: /approve [Никнейм]"), peerId);
				}
				for (Account account : accounts) {
					if (account.getUsername().equalsIgnoreCase(args[1])) {
						String result;
						if (account.getPhase() instanceof WaitingPhase) {
							((WaitingPhase) account.getPhase()).setApproved(true);
							result = "Аккаунт " + account + " успешно подтверждён.";
						} else result = "Аккаунт " + account + " находится в фазе " + account.getPhase().getName() + " и не нуждается в подтверждении";
						this.vkSession.sendMessage(new OutcomingMessage(result), peerId);
					}
					if (!user.getRank().isAtLeast(User.Rank.CAPTAIN)) continue;
					if ((args[1].equals("*") || args[1].startsWith("id") && args[1].substring(2).equals(String.valueOf(account.getOwnerVkId())))
							&& account.getPhase() instanceof WaitingPhase) {
						((WaitingPhase) account.getPhase()).setApproved(true);
						this.vkSession.sendMessage(new OutcomingMessage("Выдано подтверждение для аккаунта " + account), peerId);
					}
				}
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public Account getAccount(String username) {
		for (Account account : this.accounts) {
			if (username.equalsIgnoreCase(account.getUsername())) return account;
		}
		return null;
	}

	public User getUser(int vkId) {
		if (this.userMap.containsKey(vkId)) return this.userMap.get(vkId);
		implario.vk.model.User[] result = this.vkSession.getUser(vkId).join();
		implario.vk.model.User vkUser = result[0];
		User user = new User(vkId, vkUser.getFirstName() + " " + vkUser.getLastName(), User.Rank.SAILOR);
		this.userMap.put(vkId, user);
		this.saveAccounts();
		return user;
	}

	public void saveAccounts() {
		try {
			if (dbType.equals("json")) {
				String writeAccounts = this.gson.toJson(this.accounts);
				//			this.mainLogger.info(writeAccounts);
				Files.write(new File("2fa-accounts.json").toPath(), writeAccounts.getBytes(StandardCharsets.UTF_8));
				String writePermissions = this.gson.toJson(this.userMap.values());
				//			this.mainLogger.info(writePermissions);
				Files.write(new File("2fa-permissions.json").toPath(), writePermissions.getBytes(StandardCharsets.UTF_8));
			} else if (dbType.equals("sqlite")) {
				String writeAccounts = this.gson.toJson(this.accounts);
				String writePermissions = this.gson.toJson(this.userMap.values());

				SqliteDB.saveData(dbConn, writeAccounts, writePermissions);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public HttpResponse<String> syncRequest(HttpRequest.Builder builder) throws IOException, InterruptedException {
		return this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	public HttpRequest.Builder applyVimeWorldData(HttpRequest.Builder builder, Account account) {

		try {Thread.sleep(250);} catch (InterruptedException ignored) {}
		builder.header("content-type", "application/x-www-form-urlencoded");
		builder.header("user-agent", this.userAgent);
		if (account.getPhpsessid() != null) builder.header("cookie", "PHPSESSID=" + account.getPhpsessid());

		return builder;
	}

}
