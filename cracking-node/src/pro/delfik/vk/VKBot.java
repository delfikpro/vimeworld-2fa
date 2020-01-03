package pro.delfik.vk;

import lombok.Data;
import pro.delfik.vk.groups.Groups;

@Data
public class VKBot {

	public static VKBot I = new VKBot("190422948", "e2f86f152ce88129531bc9d18bbf3fbccb68de7da850bfeb66a6bd9a5a170c9f1ab2b69fb269c6422b945");

	private final String group;
	private final String token;

	private LongPoll longPoll;

	private final Messages messages = new Messages(this);
	private final Groups groups = new Groups(this);

	public LongPoll getLongPoll() {
		if (longPoll != null) return longPoll;
		return longPoll = new LongPoll(this);
	}

	public Messages messages() {
		return this.messages;
	}

	public Groups groups() {
		return this.groups;
	}

}
