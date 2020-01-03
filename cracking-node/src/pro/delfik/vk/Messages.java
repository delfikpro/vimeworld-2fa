package pro.delfik.vk;

import pro.delfik.net.Response;

import java.net.Proxy;
import java.util.Random;

public class Messages extends VkModule {

	private final Random random = new Random();

	public Messages(VKBot bot) {
		super(bot, "messages");
	}

	public void send(int peer, String message) {
		Response response = request("send")
				.param("peer_id", String.valueOf(peer))
				.param("random_id", String.valueOf(random.nextLong()))
				.body("message", message).execute(Proxy.NO_PROXY);
//		System.out.println(response);
//		System.out.println(new String(response.getBody()));
	}

}
