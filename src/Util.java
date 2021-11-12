import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class Util implements Constants {
	
	public static byte[] get(String url) throws IOException {
		if (url == null)
			throw new IllegalArgumentException("URL is null");
		/*boolean isLoader = Thread.currentThread() instanceof LoaderThread;
		LoaderThread il = null;
		if(isLoader) {
			il = (LoaderThread) Thread.currentThread();
		}*/
		ByteArrayOutputStream o = null;
		HttpConnection hc = null;
		DataInputStream i = null;
		try {
			hc = (HttpConnection) Connector.open(url);
			hc.setRequestMethod("GET");
			hc.setRequestProperty("User-Agent", userAgent);
			hc.setRequestProperty("Accept-Encoding", "identity");
			int r = hc.getResponseCode();
			if (r == 301 || r == 302) {
				String redir = hc.getHeaderField("Location");
				if (redir.startsWith("/")) {
					String tmp = url.substring(url.indexOf("//") + 2);
					String host = url.substring(0, url.indexOf("//")) + "//" + tmp.substring(0, tmp.indexOf("/"));
					redir = host + redir;
				}
				hc.close();
				hc = (HttpConnection) Connector.open(redir);
				hc.setRequestMethod("GET");
			}
			if(r >= 400) throw new IOException(r + " " + hc.getResponseMessage());
			i = hc.openDataInputStream();
			int s = 0;
			try {
				s = (int) hc.getLength();
			} catch (Exception e) {
				/*try {
					s = Integer.parseInt(hc.getHeaderField("Content-Length"));
				} catch (Exception e2) {
				}*/
			}
			if(s > 0) {
				byte[] b = new byte[s];
				i.readFully(b);
				return b;
			}
			s = 16384;
			byte[] b = new byte[s];
			o = new ByteArrayOutputStream();
			int c;
			while ((c = i.read(b)) != -1) {
				o.write(b, 0, c);
			}
			return o.toByteArray();
		} finally {
			try {
				if (i != null) i.close();
			} catch (IOException e) {
			}
			try {
				if (hc != null) hc.close();
			} catch (IOException e) {
			}
			try {
				if (o != null) o.close();
			} catch (IOException e) {
			}
		}
	}
	
	public static String getUtf(String url) throws IOException {
		return new String(get(url), "UTF-8");
	}
	
	public static String url(String url) {
		StringBuffer sb = new StringBuffer();
		char[] chars = url.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			int c = chars[i];
			if (65 <= c && c <= 90) {
				sb.append((char) c);
			} else if (97 <= c && c <= 122) {
				sb.append((char) c);
			} else if (48 <= c && c <= 57) {
				sb.append((char) c);
			} else if (c == 32) {
				sb.append("%20");
			} else if (c == 45 || c == 95 || c == 46 || c == 33 || c == 126 || c == 42 || c == 39 || c == 40
					|| c == 41) {
				sb.append((char) c);
			} else if (c <= 127) {
				sb.append(hex(c));
			} else if (c <= 2047) {
				sb.append(hex(0xC0 | c >> 6));
				sb.append(hex(0x80 | c & 0x3F));
			} else {
				sb.append(hex(0xE0 | c >> 12));
				sb.append(hex(0x80 | c >> 6 & 0x3F));
				sb.append(hex(0x80 | c & 0x3F));
			}
		}
		return sb.toString();
	}

	private static String hex(int i) {
		String s = Integer.toHexString(i);
		return "%" + (s.length() < 2 ? "0" : "") + s;
	}

}
