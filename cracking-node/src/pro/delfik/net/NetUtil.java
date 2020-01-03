package pro.delfik.net;

import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@UtilityClass
public class NetUtil {

	public final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3";
	public final String USERAGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36";
	public final String CONTENTTYPE = "application/x-www-form-urlencoded";

	public void readInputStream(InputStream in, byte[] array) throws IOException {
		int i = 0;
		while (i != array.length)
			i += in.read(array, i, array.length - i);
	}

	public byte[] readInputStreamFluix(InputStream in) throws IOException {

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[1024];
		while ((nRead = in.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();
		return buffer.toByteArray();
	}

	public Map<String, String> map(String... strings) {
		if (strings.length % 2 != 0) throw new IllegalArgumentException("args amount must be even");
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < strings.length; i += 2)
			map.put(strings[i], strings[i + 1]);

		return map;
	}

	private String comp;
	public String getComp() {
		if (comp != null) return comp;
		try {
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			return comp = addr.getHostName();
		} catch (UnknownHostException ex) {
			System.out.println("Hostname can not be resolved");
			return "???";
		}
	}

	public int byteSearch(byte[] outer, byte[] inner) {
		for (int i = 0; i < outer.length - inner.length + 1; ++i) {
			boolean found = true;
			for (int j = 0; j < inner.length; ++j) {
				if (outer[i + j] != inner[j]) {
					found = false;
					break;
				}
			}
			if (found) return i;
		}
		return -1;
	}

}
