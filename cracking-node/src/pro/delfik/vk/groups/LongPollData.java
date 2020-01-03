package pro.delfik.vk.groups;

import lombok.Data;

@Data
public class LongPollData {

	private final String key;
	private final String server;
	private final String ts;

}
