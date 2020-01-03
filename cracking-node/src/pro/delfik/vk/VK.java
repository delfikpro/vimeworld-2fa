package pro.delfik.vk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VK {

	public static String query(String method) {
		return get("https://api.vk.com/method/" + method, "v=5.81&access_token=" + null);
	}

	public static String query(String method, String params) {
		return get("https://api.vk.com/method/" + method, "v=5.81&access_token=" + null + "&" + params);
	}


	public static void markAsRead(int from_id) {
		VK.query("messages.markAsRead", "peer_id=" + from_id);
	}

	public static String uploadPhoto(File f) {
		String id = "";
		String upload_url;

		try{
			String server = VK.query("photos.getMessagesUploadServer");

			JSONObject s = new JSONObject(server);
			upload_url = s.getJSONObject("response").getString("upload_url");

			String res = post_upload(upload_url, f);


			JSONObject j = new JSONObject(res);

			int _server = j.getInt("server");
			String _photo = j.getString("photo");
			String _hash = j.getString("hash");

			String params = "server=" + _server + "&photo=" + _photo + "&hash=" + _hash;

			res = VK.query("photos.saveMessagesPhoto", params);

			j = new JSONObject(res);
			JSONArray arr = j.getJSONArray("response");
			JSONObject g = arr.getJSONObject(0);
			id = g.getString("id");
		}catch (Exception e){
			e.printStackTrace();
		}

		return id;
	}

	public static String getUserName(int uid) {
		String data = query("users.get", "user_id=" + uid);
		String full_name;
		try{
			JSONObject obj = new JSONObject(data);
			JSONArray response = obj.getJSONArray("response");
			JSONObject _data = response.getJSONObject(0);
			String first_name = _data.getString("first_name");
			String last_name = _data.getString("last_name");

			full_name = first_name + " " + last_name;

		}catch (JSONException e){
			e.printStackTrace();
			full_name = "";
		}

		return full_name;
	}

	public static int getID(String link){
		String data = query("utils.resolveScreenName", "screen_name=" + link);
		try{
			JSONObject obj = new JSONObject(data);
			JSONObject response = obj.getJSONObject("response");
			return response.getInt("object_id");
		} catch (JSONException e){
			return -1;
		}
	}
	
	public static int getUserID(String arg) {
		String data = query("users.get", "user_ids=" + arg);
		try{
			JSONObject obj = new JSONObject(data);
			JSONArray response = obj.getJSONArray("response");
			JSONObject _data = response.getJSONObject(0);
			return _data.getInt("uid");
		} catch (JSONException e){
			return -1;
		}
	}
	
	public static class MultipartUtility {

		private final String boundary;
		private static final String LINE_FEED = "\r\n";
		private HttpURLConnection httpConn;
		private String charset;
		private OutputStream outputStream;
		private PrintWriter writer;

		public MultipartUtility(String var1, String var2) throws IOException {
			this.charset = var2;
			this.boundary = "===" + System.currentTimeMillis() + "===";
			URL var3 = new URL(var1);
			this.httpConn = (HttpURLConnection) var3.openConnection();
			this.httpConn.setUseCaches(false);
			this.httpConn.setDoOutput(true);
			this.httpConn.setDoInput(true);
			this.httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + this.boundary);
			this.httpConn.setRequestProperty("User-Agent", "CodeJava Agent");
			this.httpConn.setRequestProperty("Test", "Bonjour");
			this.outputStream = this.httpConn.getOutputStream();
			this.writer = new PrintWriter(new OutputStreamWriter(this.outputStream, var2), true);
		}

		public void addFormField(String var1, String var2) {
			this.writer.append("--" + this.boundary).append("\r\n");
			this.writer.append("Content-Disposition: form-data; name=\"" + var1 + "\"").append("\r\n");
			this.writer.append("Content-Type: text/plain; charset=" + this.charset).append("\r\n");
			this.writer.append("\r\n");
			this.writer.append(var2).append("\r\n");
			this.writer.flush();
		}

		public void addFilePart(String var1, File var2) throws IOException {
			String var3 = var2.getName();
			this.writer.append("--" + this.boundary).append("\r\n");
			this.writer.append("Content-Disposition: form-data; name=\"" + var1 + "\"; filename=\"" + var3 + "\"").append("\r\n");
			this.writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(var3)).append("\r\n");
			this.writer.append("Content-Transfer-Encoding: binary").append("\r\n");
			this.writer.append("\r\n");
			this.writer.flush();
			FileInputStream var4 = new FileInputStream(var2);
			byte[] var5 = new byte[4096];
			boolean var6 = true;

			int var7;
			while ((var7 = var4.read(var5)) != -1){
				this.outputStream.write(var5, 0, var7);
			}

			this.outputStream.flush();
			var4.close();
			this.writer.append("\r\n");
			this.writer.flush();
		}

		public void addHeaderField(String var1, String var2) {
			this.writer.append(var1 + ": " + var2).append("\r\n");
			this.writer.flush();
		}

		public List<String> finish() throws IOException {
			ArrayList var1 = new ArrayList();
			this.writer.append("\r\n").flush();
			this.writer.append("--" + this.boundary + "--").append("\r\n");
			this.writer.close();
			int var2 = this.httpConn.getResponseCode();
			if(var2 != 200){
				throw new IOException("Server returned non-OK status: " + var2);
			}else{
				BufferedReader var3 = new BufferedReader(new InputStreamReader(this.httpConn.getInputStream()));
				String var4 = null;

				while ((var4 = var3.readLine()) != null){
					var1.add(var4);
				}

				var3.close();
				this.httpConn.disconnect();
				return var1;
			}
		}
	}

	public static String post_upload(String var0, File var1) {
		String var2 = "";

		try{
			MultipartUtility var3 = new MultipartUtility(var0, "utf-8");
			var3.addFilePart("file", var1);
			List var4 = var3.finish();

			String var6;
			for (Iterator var5 = var4.iterator(); var5.hasNext(); var2 = var2 + var6){
				var6 = (String) var5.next();
			}
		}catch (IOException var7){
			var2 = "upload error";
			var7.printStackTrace();
		}

		return var2;
	}

	public static String get(String server, String args) {
		return get(server + "?" + args);
	}

	public static String get(String server){
		try{
			URL url = new URL(server);
			URLConnection connection = url.openConnection();
			connection.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String result = reader.readLine();
			reader.close();
			return result;
		}catch (Exception ex){
			ex.printStackTrace();
			return "fail";
		}
	}
}
