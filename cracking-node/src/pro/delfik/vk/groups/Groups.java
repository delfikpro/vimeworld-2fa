package pro.delfik.vk.groups;

import org.json.JSONException;
import org.json.JSONObject;
import pro.delfik.net.Response;
import pro.delfik.vk.VKBot;
import pro.delfik.vk.VkModule;

import java.net.Proxy;

public class Groups extends VkModule {

	public Groups(VKBot bot) {
		super(bot, "groups");
	}

	public LongPollData getLongPollServer() {
		JSONObject json = execute(request("getLongPollServer")
				.param("group_id", getBot().getGroup())
			   );

		try {
			return new LongPollData(
					json.getString("key"),
					json.getString("server"),
					json.getString("ts")
			);
		} catch (JSONException ex) {
			System.out.println(json);
			throw ex;
		}

	}

}
