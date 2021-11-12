import java.io.IOException;
import java.util.Vector;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.midlet.MIDlet;

import cc.nnproject.json.AbstractJSON;
import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import models.ChannelModel;
import models.ILoader;
import models.VideoModel;
import ui.Settings;
import ui.VideoForm;

public class App extends MIDlet implements CommandListener, Constants {

	private static boolean started;
	
	public static int width;
	public static int height;
	
	// Settings
	public static String videoRes;
	public static String region;
	public static int watchMethod; // 0 - platform request 1 - mmapi player
	public static String downloadDir;
	public static String servergetlinks = getlinksphp;
	public static String serverhttp = streamphp;
	public static boolean videoPreviews;
	public static boolean searchChannels;
	public static boolean rememberSearch;
	public static boolean httpStream;
	public static int startScreen; // 0 - Trends 1 - Popular
	
	public static App midlet;
	
	private Form mainForm;
	private Form searchForm;
	public Settings settingsForm;
	//private TextField searchText;
	//private StringItem searchBtn;
	private VideoForm videoForm;
	private static PlayerCanvas playerCanv;

	private boolean asyncLoading = true;
	private Object lazyLoadLock = new Object();
	private LoaderThread t0;
	private LoaderThread t1;
	private LoaderThread t2;
	public Vector v0;
	private Vector v1;
	private Vector v2;

	protected void destroyApp(boolean b) {}

	protected void pauseApp() {}

	protected void startApp() {
		if(started) return;
		midlet = this;
		started = true;
		region = System.getProperty("user.country");
		if(region == null) {
			region = System.getProperty("microedition.locale");
			if(region == null) {
				region = "US";
			} else if(region.length() == 5) {
				region = region.substring(3, 5);
			} else if(region.length() > 2) {
				region = region.substring(0, 2);
			}
		} else if(region.length() > 2) {
			region = region.substring(0, 2);
		}
		region = region.toUpperCase();
		v0 = new Vector();
		if(startMemory != S40_MEM && asyncLoading) {
			v1 = new Vector();
			v2 = new Vector();
			t0 = new LoaderThread(5, lazyLoadLock, v0);
			t1 = new LoaderThread(5, lazyLoadLock, v1);
			t2 = new LoaderThread(5, lazyLoadLock, v2);
			t0.start();
			t1.start();
			t2.start();
		} else {
			t0 = new LoaderThread(5, lazyLoadLock, v0);
			t0.start();
		}
		testCanvas();
		initForm();
		Settings.loadConfig();
		if(startScreen == 0) {
			loadTrends();
		} else {
			loadPopular();
		}
	}

	private void testCanvas() {
		Canvas c = new TestCanvas();
		display(c);
		width = c.getWidth();
		height = c.getHeight();
	}

	private void initForm() {
		mainForm = new Form(NAME);
		
		/*
		searchText = new TextField("", "", 256, TextField.ANY);
		searchText.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_2);
		mainForm.append(searchText);
		searchBtn = new StringItem(null, "Поиск", StringItem.BUTTON);
		searchBtn.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_RIGHT | Item.LAYOUT_2);
		searchBtn.addCommand(searchCmd);
		searchBtn.setDefaultCommand(searchCmd);
		mainForm.append(searchBtn);
		*/
		mainForm.setCommandListener(this);
		mainForm.addCommand(searchCmd);
		mainForm.addCommand(idCmd);
		mainForm.addCommand(settingsCmd);
		mainForm.addCommand(exitCmd);
		display(mainForm);
	}
	
	public static byte[] hproxy(String s) throws IOException {
		if(s.startsWith("/")) return Util.get(inv + s.substring(1));
		return Util.get(hproxy + Util.url(s));
	}

	public static AbstractJSON invApi(String s) throws InvidiousException, IOException {
		if(!s.endsWith("?")) s = s.concat("&");
		s = s.concat("region=" + region);
		s = Util.getUtf(inv + "api/" + s);
		AbstractJSON res;
		try {
			res = JSON.getObject(s);
			if(((JSONObject) res).has("code")) {
				throw new InvidiousException((JSONObject) res, ((JSONObject) res).getString("code") + ": " + ((JSONObject) res).getNullableString("message"));
			}
			if(((JSONObject) res).has("error")) {
				throw new InvidiousException((JSONObject) res);
			}
		} catch (JSONException e) {
			if(!e.getMessage().equals("Not JSON object")) throw e;
			try {
				res = JSON.getArray(s);
			} catch (JSONException e2) {
				e2.printStackTrace();
				throw e;
			}
		}
		return res;
	}

	private void loadTrends() {
		try {
			mainForm.setTitle(NAME + " - Trends");
			JSONArray j = (JSONArray) invApi("v1/trending?");
			for(int i = 0; i < j.size(); i++) {
				VideoModel v = new VideoModel(j.getObject(i));
				if(videoPreviews) addAsyncLoad(v);
				mainForm.append(v.makeImageItemForList());
				if(i >= TRENDS_LIMIT) break;
			}
			notifyAsyncTasks();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void loadPopular() {
		try {
			mainForm.setTitle(NAME + " - Popular");
			JSONArray j = (JSONArray) invApi("v1/popular?");
			for(int i = 0; i < j.size(); i++) {
				VideoModel v = new VideoModel(j.getObject(i));
				if(videoPreviews) addAsyncLoad(v);
				mainForm.append(v.makeImageItemForList());
				if(i >= TRENDS_LIMIT) break;
			}
			notifyAsyncTasks();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void search(String q) {
		searchForm = new Form(NAME + " - Search query");
		searchForm.setCommandListener(this);
		searchForm.addCommand(backCmd);
		display(searchForm);
		try {
			JSONArray j = (JSONArray) invApi("v1/search?q=" + Util.url(q) + (searchChannels ? "&type=all" : ""));
			for(int i = 0; i < j.size(); i++) {
				JSONObject o = j.getObject(i);
				String type = o.getNullableString("type");
				if(type == null) continue;
				if(type.equals("video")) {
					VideoModel v = new VideoModel(o);
					v.setFromSearch();
					if(videoPreviews) addAsyncLoad(v);
					searchForm.append(v.makeImageItemForList());
				}
				if(searchChannels && type.equals("channel")) {
					ChannelModel c = new ChannelModel(o);
					searchForm.append(c.makeItemForList());
				}
				if(i >= SEARCH_LIMIT) break;
			}
			notifyAsyncTasks();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void openVideo(String id) {
		final String https = "https://";
		final String ytshort = "youtu.be/";
		final String www = "www.";
		final String watch = "youtube.com/watch?v=";
		if(id.startsWith(https)) id = id.substring(https.length());
		if(id.startsWith(ytshort)) id = id.substring(ytshort.length());
		if(id.startsWith(www)) id = id.substring(www.length());
		if(id.startsWith(watch)) id = id.substring(watch.length());
		try {
			openVideo(new VideoModel(id).extend());
		} catch (Exception e) {
			msg(e.toString());
		}
	}

	static JSONObject getVideoInfo(String id, String res) throws JSONException, IOException {
		JSONArray j = JSON.getArray(Util.getUtf(servergetlinks + "?url=" + Util.url("https://www.youtube.com/watch?v="+id)));
		if(j.size() == 0) {
			throw new RuntimeException("failed to get link for video: " + id);
		}
		JSONObject _144 = null;
		JSONObject _360 = null;
		JSONObject _720 = null;
		JSONObject other = null;
		for(int i = 0; i < j.size(); i++) {
			JSONObject o = j.getObject(i);
			String q = o.getString("qualityLabel");
			if(q.startsWith("720p")) {
				_720 = o;
			} else if(q.startsWith("360p")) {
				_360 = o;
			} else if(q.startsWith("144p")) {
				_144 = o;
			} else {
				other = o;
			}
		}
		JSONObject o = null;
		if(res == null) {
			if(_360 != null) {
				o = _360;
			} else if(other != null) {
				o = other;
			} else if(_144 != null) {
				o = _144;
			} 
		} else if(res.equals("144p")) {
			if(_144 != null) {
				o = _144;
			} else if(_360 != null) {
				o = _360;
			} else if(other != null) {
				o = other;
			}
		} else if(res.equals("360p")) {
			if(_360 != null) {
				o = _360;
			} else if(other != null) {
				o = other;
			} else if(_144 != null) {
				o = _144;
			} 
		} else if(res.equals("720p")) {
			if(_720 != null) {
				o = _720;
			} else if(_360 != null) {
				o = _360;
			} else if(other != null) {
				o = other;
			} else if(_144 != null) {
				o = _144;
			} 
		}
		return o;
	}

	public static String getVideoLink(String id, String res) throws JSONException, IOException {
		JSONObject o = getVideoInfo(id, res);
		String s = o.getString("url");
		if(httpStream) {
			s = serverhttp + "?url=" + Util.url(s);
		}
		return s;
	}

	private void openVideo(VideoModel v) {
		videoForm = new VideoForm(v);
		display(videoForm);
		videoForm.queueLoad();
	}
	
	public static void download(final String id) {
		Downloader d = new Downloader(id, videoRes, midlet.videoForm, downloadDir);
		d.start();
	}
	
	public static void watch(String id) {
		// TODO other variants
		try {
			String url = getVideoLink(id, videoRes);
			switch(watchMethod) {
			case 0: {
				platReq(url);
				break;
			}
			case 1: {
				Player p = Manager.createPlayer(url);
				playerCanv = new PlayerCanvas(p);
				display(playerCanv);
				playerCanv.init();
				break;
			}
			}
		} catch (Exception e) {
			e.printStackTrace();
			msg(e.toString());
		}
	}
	
	public static void platReq(String s) throws ConnectionNotFoundException {
		midlet.platformRequest(s);
	}
	
	void addAsyncLoad(ILoader v) {
		synchronized(lazyLoadLock) {
			if(v1 == null) {
				v0.addElement(v);
			} else {
				int s0 = v0.size();
				int s1 = v1.size();
				int s2 = v2.size();
				if(s0 < s1) {
					v0.addElement(v);
				} else if(s1 < s2) {
					v1.addElement(v);
				} else {
					v2.addElement(v);
				}
			}
		}
	}
	
	void notifyAsyncTasks() {
		synchronized(lazyLoadLock) {
			lazyLoadLock.notifyAll();
		}
	}

	void stopDoingAsyncTasks() {
		synchronized(lazyLoadLock) {
			if(t0 != null) t0.pleaseInterrupt();
			if(t1 != null) t1.pleaseInterrupt();
			if(t2 != null) t2.pleaseInterrupt();
			v0.removeAllElements();
		}
	}

	public static void msg(String s) {
		Alert a = new Alert("", s, null, null);
		a.setTimeout(-2);
		display(a);
	}
	
	public static void display(Displayable d) {
		if(d == null) d = midlet.mainForm;
		Display.getDisplay(midlet).setCurrent(d);
	}

	public static void open(VideoModel v) {
		if(v.isFromSearch() && !rememberSearch) {
			midlet.disposeSearchForm();
		}
		midlet.openVideo(v);
	}

	public void commandAction(Command c, Displayable d) {
		if(c == exitCmd) {
			notifyDestroyed();
		}
		if(c == settingsCmd) {
			if(settingsForm == null) {
				settingsForm = new Settings();
			}
			display(settingsForm);
			settingsForm.show();
		}
		if(c == searchCmd && d instanceof Form) {
			TextBox t = new TextBox("", "", 256, TextField.ANY);
			t.setCommandListener(this);
			t.setTitle("Search");
			t.addCommand(searchOkCmd);
			t.addCommand(cancelCmd);
			display(t);
		}
		if(c == idCmd && d instanceof Form) {
			TextBox t = new TextBox("", "", 256, TextField.ANY);
			t.setCommandListener(this);
			t.setTitle("Video URL or ID");
			t.addCommand(goCmd);
			t.addCommand(cancelCmd);
			display(t);
		}
		/*if(c == browserCmd) {
			try {
				platReq(getVideoInfo(video.getVideoId(), videoRes).getString("url"));
			} catch (Exception e) {
				e.printStackTrace();
				msg(e.toString());
			}
		}*/
		if(c == cancelCmd && d instanceof TextBox) {
			display(mainForm);
		}
		if(c == backCmd && d == searchForm) {
			display(mainForm);
			disposeSearchForm();
		}
		if(c == goCmd && d instanceof TextBox) {
			openVideo(((TextBox) d).getString());
		}
		if(c == searchOkCmd && d instanceof TextBox) {
			search(((TextBox) d).getString());
		}
	}


	public void disposeVideoForm() {
		videoForm.dispose();
		videoForm = null;
	}

	private void disposeSearchForm() {
		searchForm = null;
	}

	public static void pageOpen(VideoForm vf) {
		App app = App.midlet;
		app.stopDoingAsyncTasks();
		app.addAsyncLoad(vf);
		app.notifyAsyncTasks();
	}

	public static void back(VideoForm vf) {
		if(vf.getVideo().isFromSearch() && midlet.searchForm != null) {
			App.display(midlet.searchForm);
		} else {
			App.display(midlet.mainForm);
		}
	}

}
