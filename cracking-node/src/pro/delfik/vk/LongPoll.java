package pro.delfik.vk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pro.delfik.net.Method;
import pro.delfik.net.Request;
import pro.delfik.vk.groups.LongPollData;

public class LongPoll extends VkModule implements Runnable {

	protected String key;
	protected String server;
	protected String ts;
	public volatile long lastPeer;

	private Thread thread;

	public LongPoll(VKBot bot) {
		super(bot, null);
	}

	public void start() {
		(thread = new Thread(this)).start();
	}

	public void requestLongPollServer() {
		LongPollData data = getBot().groups().getLongPollServer();

		key = data.getKey();
		server = data.getServer();
		ts = data.getTs();
	}
	
	private volatile byte failed = 0;

	public void run() {
		requestLongPollServer();
		while (true) {

			Request request = new Request((server.startsWith("http") ? "" : "https://") + server, Method.GET);
			request.param("act", "a_check");
			request.param("key", key);
			request.param("ts", ts);
			request.param("wait", "25");
			request.param("mode", "2");



			try {
				JSONObject response = execute(request, false);
				System.out.println(response);
				String _ts = response.getString("ts");
				JSONArray updates = response.getJSONArray("updates");
				if (updates.length() != 0) processEvent(updates);
				ts = _ts;
				failed = 0;
			} catch (Exception ex) {
				if (failed > 10) throw new RuntimeException("Не удалось подключиться к LongPoll.");
				else {
					requestLongPollServer();
					failed++;
				}
			}
		}
	}


	private void processEvent(JSONArray array) {
		for (int i = 0; i <  array.length(); ++i) {
			try {
				JSONObject arrayItem = array.getJSONObject(i);
				String eventType = arrayItem.getString("type");
				JSONObject object = arrayItem.getJSONObject("object");
				
				
				switch (eventType) {
					case "message_new":
						JSONObject msg = object.getJSONObject("message");

						String text;
						int from_id;
						int peer_id = msg.getInt("peer_id");
						try {
							from_id = msg.getInt("user_id");
						} catch (Exception ex) {
							from_id = msg.getInt("from_id");
						}
						try {
							text = msg.getString("body");
						} catch (Exception ex) {
							text = msg.getString("text");
						}
						
						lastPeer = peer_id;
						
						text = text.replaceAll("\\[.*]", "");
//						String message = MessageHandler.handle(text, from_id, peer_id);
						if (text.length() > 0) getBot().messages().send(peer_id, text);
						break;
				}
				
			} catch (JSONException ignored) {}
		}
	}

}
