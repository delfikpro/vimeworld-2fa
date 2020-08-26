package implario.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * the great legacy.
 * @author 6oogle
 */
public class DataIO {

	public static List<String> read(String path) {
		String in = readFile(path);
		if (in == null || in.length() == 0) return null;
		String[] split = in.split("\n");
		List<String> list = new ArrayList<>(split.length);
		Collections.addAll(list, split);
		return list;
	}

	public static Map<String, String> readConfig(String path) {
		Map<String, String> map = new HashMap<>();
		List<String> read = read(path);
		if (read == null) return new HashMap<>();
		for (String input : read) {
			String[] split = input.split(": ");
			if (split.length == 2) map.put(split[0].trim(), split[1].trim());
		}
		return map;
	}

	public static void write(String path, List<String> write) {
		StringBuilder buffer = new StringBuilder();
		for (String line : write) {
			buffer.append(line);
			buffer.append('\n');
		}
		writeFile(path, buffer.toString());
	}

	public static void writeConfig(String path, Map<String, String> write) {
		StringBuilder buffer = new StringBuilder();

		for (Map.Entry<String, String> entry : write.entrySet()) {
			buffer.append(entry.getKey());
			buffer.append('=');
			buffer.append(entry.getValue());
			buffer.append('\n');
		}

		writeFile(path, buffer.toString());
	}

	public static void exportResource(String resourceName, File dst) throws IOException {
		InputStream stream = null;
		OutputStream resStreamOut = null;
		try {
			stream = DataIO.class.getResourceAsStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
			if(stream == null) {
				throw new RuntimeException("Cannot get resource \"" + resourceName + "\" from Jar file.");
			}

			int readBytes;
			byte[] buffer = new byte[4096];
			resStreamOut = new FileOutputStream(dst);
			while ((readBytes = stream.read(buffer)) > 0) {
				resStreamOut.write(buffer, 0, readBytes);
			}
		} finally {
			stream.close();
			resStreamOut.close();
		}

	}

	public static void writeFile(String path, String write) {
		File file = getFile(path);
		if (!file.exists()) create(path);

		BufferedWriter out = null;

		try {
			out = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8));
			out.write(write);
		} catch (IOException var5) {
			var5.printStackTrace();
		}

		close(out);
	}

	public static String readFile(String path) {
		File file = getFile(path);
		if (!contains(path)) return null;
		BufferedReader in = null;
		StringBuilder sb = new StringBuilder((int) file.length());
		try {
			in = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
			while (true) {
				int read = in.read();
				if (read == -1) break;
				if (read == 13) continue;
				sb.append((char) read);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		close(in);
		return sb.toString();
	}

	public static void remove(String path) {
		getFile(path).delete();
	}

	public static void create(String path) {
		File file = getFile(path);
		file.getParentFile().mkdirs();

		try {
			file.createNewFile();
		} catch (IOException var3) {
			var3.printStackTrace();
		}

	}

	public static boolean contains(File file) {
		return file.exists();
	}

	public static boolean contains(String path) {
		return contains(getFile(path));
	}

	public static File[] getAll(String path) {
		return new File(path).listFiles();
	}

	public static File getFile(String path) {
		File f;
		if (path.contains(".")) f = new File(path);
		else f = new File(path.toLowerCase() + ".txt");
		if (!f.exists()) try {
			f.createNewFile();
		} catch (IOException ignored) {}
		return f;
	}

	private static void close(Reader in) {
		if (in == null) return;
		try {
			in.close();
		} catch (IOException var2) {
			var2.printStackTrace();
		}
	}

	private static void close(Writer out) {
		if (out == null) return;
		try {
			out.flush();
			out.close();
		} catch (IOException var2) {
			var2.printStackTrace();
		}
	}

}
