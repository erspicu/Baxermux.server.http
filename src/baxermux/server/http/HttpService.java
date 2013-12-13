package baxermux.server.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import java.util.*;
import java.lang.String;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

// local服務程式參考
// http://developer.android.com/reference/android/app/Service.html#LocalServiceSample
// http://www.techotopia.com/index.php/Android_Local_Bound_Services_%E2%80%93_A_Worked_Example

public class HttpService extends Service // <--龜毛的東西..
{

	private final IBinder mBinder = new LocalBinder();
	public boolean done = false;
	public String test_str = "from local service str";

	public class LocalBinder extends Binder
	{
		HttpService getService()
		{
			return HttpService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		Log.i("my", "service bind");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		Log.d("my", "service onUnbind");
		return super.onUnbind(intent);
	}

	@Override
	public void onCreate()
	{
		Log.i("my", "service creat");
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		Log.i("my", "service start  - http server start");
		(new connect_clinet()).start(); // 啟動http service
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.i("my", "service destroy");
		done = true;

		// 關閉server服務
		try
		{
			Server.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	// ------------------------------------------------
	public ServerSocket Server = null;

	// 先寫死 預設
	public String root_dir = "/storage/sdcard1/wwwroot"; // "/storage/removable/sdcard1/wwwroot";

	public class connect_clinet extends Thread
	{
		public int use_port = 8080;

		public void run()
		{

			Socket connected;

			try
			{
				Server = new ServerSocket();
			}
			catch (IOException e1)
			{
			}

			try
			{
				Server.setReuseAddress(true);
			}
			catch (SocketException e1)
			{
			}

			try
			{
				Server.bind(new InetSocketAddress(use_port));
			}
			catch (IOException e1)
			{
			}

			while (true && done != true)
			{
				try
				{
					connected = Server.accept();
					(new clinet_deal(connected)).start();
				}
				catch (IOException e)
				{
				}
			}

			test_str = "break...............";

			Log.i("my", "http server close");
		}
	}

	public class clinet_deal extends Thread
	{
		public Socket connectedClient = null;
		public String HeadersString = "";
		public String http_ver = "HTTP/1.1";
		public byte[] body;

		public void run()
		{

			String request_str = null;

			try
			{
				// 資料接收區 start
				int b_get = 0;

				try
				{
					b_get = connectedClient.getInputStream().read();
				}
				catch (IOException e)
				{
					connectedClient.close();
					return;
				}

				ByteArrayOutputStream b_list = new ByteArrayOutputStream();

				while (b_get != -1)
				{
					b_list.write((byte) (b_get));
					if (connectedClient.getInputStream().available() > 0)
					{
						try
						{
							b_get = connectedClient.getInputStream().read();

						}
						catch (IOException e)
						{
							connectedClient.close();
							return;

						}
					}
					else
					{
						request_str = new String(b_list.toByteArray(), "UTF-8");
						break;
					}

				}
				// 資料接收區 end

				// header分析區 start
				String firstline = "";

				try
				{
					firstline = request_str.substring(0, request_str.indexOf("\r\n"));
				}
				catch (Exception ex)
				{
					connectedClient.close();
					return;
				}
				String[] request_inf = firstline.split(" ");
				String request_method = request_inf[0];

				// http://www.ewdna.com/2008/11/urlencoder.html
				String request_target = URLDecoder.decode(request_inf[1], "UTF-8"); // 解碼

				String request_version = request_inf[2];

				// java的substring 跟 c#的 Substring API定義不太同
				String request_header_str = request_str.substring(firstline.length() + 2, request_str.indexOf("\r\n\r\n"));

				HashMap header_list = new HashMap();
				for (String i : request_header_str.split("\r\n"))
					header_list.put(i.substring(0, i.indexOf(": ")), i.substring(i.indexOf(": ") + 2));

				// header分析區 end

				// 開始進行不同的method處理 暫時先支援 GET method 與 PROPFIND

				// Java要使用 equals 不是 !=
				if (!request_method.equals("GET") && !request_method.equals("PROPFIND") && !request_method.equals("MKCOL") && !request_method.equals("DELETE")
						&& !request_method.equals("MOVE") && !request_method.equals("COPY")  && !request_method.equals("PUT")  )
				{
					connectedClient.close();
					return;
				}

				// 放行支援的method進行後續處理
				Log.i("my", "debug [" + request_target + "]");
				
				if (request_method.equals("PUT"))
				{
					//尚需實做,等補
					connectedClient.close();
					return;
				}
				
				if (request_method.equals("COPY"))
				{
					String des = (String) header_list.get("Destination");
					String from = root_dir +  request_target;
					String to = root_dir +  URLDecoder.decode(des.substring(("http://" + (String) header_list.get("Host")).length()), "UTF-8");
					
					File target_file = new File ( from);
					File to_file = new File ( to);
					
					if (  target_file.isDirectory()  )
					{
						Utils.copyDirectory(target_file, to_file );
					}
					else	
					{
						Utils.copyFile (target_file.getAbsolutePath(), to_file.getAbsolutePath() );
					}
										
					HeadersString = http_ver + " " + "201 Created" + "\r\n";
					HeadersString += "\r\n";
					connectedClient.getOutputStream().write(HeadersString.getBytes());
					connectedClient.close();
					return;
				}

				if (request_method.equals("MOVE"))
				{
					String des = (String) header_list.get("Destination");
					String from = root_dir +  request_target;
					String to = root_dir +  URLDecoder.decode(des.substring(("http://" + (String) header_list.get("Host")).length()), "UTF-8");
					
					File target_file = new File ( from);
					File to_file = new File ( to);
					
					target_file.renameTo(to_file);
					
					Log.i("my", from );
					Log.i("my" , to);
					
					HeadersString = http_ver + " " + "201 Created" + "\r\n";
					HeadersString += "\r\n";
					connectedClient.getOutputStream().write(HeadersString.getBytes());
					connectedClient.close();
					return;
				
				}

				if (request_method.equals("DELETE"))
				{
					File target_file = new File(root_dir + request_target);

					if (!target_file.exists())
					{
						HeadersString = http_ver + " " + "403 Forbidden" + "\r\n";
						HeadersString += "\r\n";
						connectedClient.getOutputStream().write(HeadersString.getBytes());
						connectedClient.close();
						return;
					}

					try
					{
						if (target_file.isDirectory())
							Utils.removeDirectory(target_file);
						else
							target_file.delete();

						HeadersString = http_ver + " " + "201 Created" + "\r\n";
					}
					catch (Exception e)
					{
						HeadersString = http_ver + " " + "403 Forbidden" + "\r\n";
					}
					HeadersString += "\r\n";
					connectedClient.getOutputStream().write(HeadersString.getBytes());
					connectedClient.close();
					return;

				}

				if (request_method.equals("MKCOL"))
				{
					File check_dir = new File(root_dir + request_target);

					Log.i("my", "mkcol debug :  " + root_dir + request_target);

					if (!check_dir.exists())
					{
						// 目錄不存在,建立
						HeadersString = http_ver + " " + "201 Created" + "\r\n";
						check_dir.mkdir();
					}
					else
					{
						// 禁行
						HeadersString = http_ver + " " + "403 Forbidden" + "\r\n";
					}
					HeadersString += "\r\n";
					connectedClient.getOutputStream().write(HeadersString.getBytes());
					connectedClient.close();
					return;
				}

				// 一堆 . . . . .
				File target_file = null;
				if (request_method.equals("PROPFIND"))
				{
					target_file = new File(root_dir + request_target);

					Log.i("my", "propfind");

					// 前置檢查
					// 需要實做,先假設對方傳入的路徑一定是正確的

					// 建立回傳xml資料結構體
					// http://www.mkyong.com/java/how-to-create-xml-file-in-java-dom
					DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder = null;
					try
					{
						docBuilder = docFactory.newDocumentBuilder();
					}
					catch (ParserConfigurationException e)
					{
						e.printStackTrace();
					}

					Document doc = docBuilder.newDocument();
					doc.setXmlStandalone(true);
					Element rootElement = doc.createElement("D:multistatus");
					Attr attr = doc.createAttribute("xmlns:D");
					attr.setValue("DAV:");

					rootElement.setAttributeNode(attr);
					doc.appendChild(rootElement);

					// 處理檔案流程
					int th = 0;

					for (File file_obj : target_file.listFiles())
					{

						if (!file_obj.isFile())
							continue;

						rootElement.appendChild(doc.createElement("D:response"));
						rootElement.getChildNodes().item(th).appendChild(doc.createElement("D:href"));

						rootElement.getChildNodes().item(th).getChildNodes().item(0)
								.setTextContent(request_target + URLEncoder.encode(file_obj.getName(), "utf8"));

						rootElement.getChildNodes().item(th).appendChild(doc.createElement("D:propstat"));
						rootElement.getChildNodes().item(th).getChildNodes().item(1).appendChild(doc.createElement("D:prop"));

						// 這個好像不是 BitKinex 必要運作屬性 so remove
						// Attr attr_prop = doc.createAttribute("xmlns:R");
						// attr_prop.setValue("http://www.example.com");

						// ((Element) rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0)).setAttributeNode(attr_prop);

						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0).appendChild(doc.createElement("D:status"));
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0).getChildNodes().item(0)
								.setTextContent("HTTP/1.1 200 OK");
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0)
								.appendChild(doc.createElement("D:creationdate"));
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0).getChildNodes().item(1)
								.setTextContent("2013-11-21T10:12:14:Z");// 先寫死
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0)
								.appendChild(doc.createElement("D:getlastmodified"));
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0).getChildNodes().item(2)
								.setTextContent("2013-11-21T10:12:14:Z");// 先寫死
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0)
								.appendChild(doc.createElement("D:resourcetype"));// index 3

						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0)
								.appendChild(doc.createElement("D:getcontentlength")); // index 4
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0).getChildNodes().item(4)
								.setTextContent(String.valueOf(file_obj.length()));

						th++;
					}

					// 處理目錄流程
					for (File file_obj : target_file.listFiles())
					{
						if (!file_obj.isDirectory())
							continue;

						rootElement.appendChild(doc.createElement("D:response"));
						rootElement.getChildNodes().item(th).appendChild(doc.createElement("D:href"));

						rootElement.getChildNodes().item(th).getChildNodes().item(0)
								.setTextContent(request_target + URLEncoder.encode(file_obj.getName(), "utf8") + "/");

						rootElement.getChildNodes().item(th).appendChild(doc.createElement("D:propstat"));
						rootElement.getChildNodes().item(th).getChildNodes().item(1).appendChild(doc.createElement("D:prop"));
						// ((Element) rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0)).setAttributeNode(attr_prop);
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0).appendChild(doc.createElement("D:status"));
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0).getChildNodes().item(0)
								.setTextContent("HTTP/1.1 200 OK");
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0)
								.appendChild(doc.createElement("D:creationdate"));
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0).getChildNodes().item(1)
								.setTextContent("2013-11-21T10:12:14:Z");// 先寫死
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0)
								.appendChild(doc.createElement("D:getlastmodified"));
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0).getChildNodes().item(2)
								.setTextContent("2013-11-21T10:12:14:Z");// 先寫死
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0)
								.appendChild(doc.createElement("D:resourcetype"));// index 3

						// for dir add
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0).getChildNodes().item(3)
								.appendChild(doc.createElement("D:collection"));

						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0)
								.appendChild(doc.createElement("D:getcontentlength")); // index 4
						rootElement.getChildNodes().item(th).getChildNodes().item(1).getChildNodes().item(0).getChildNodes().item(4)
								.setTextContent(String.valueOf(file_obj.length()));

						th++;

					}

					// http://stackoverflow.com/questions/4412848/xml-node-to-string-in-java
					String xmlbody_str = nodeToString(doc.getDocumentElement());

					Log.i("my", xmlbody_str);

					body = xmlbody_str.getBytes();
					HeadersString = http_ver + " " + "207 Multi-Status" + "\r\n";
					HeadersString += "Content-Type: application/xml; charset=\"utf-8\"" + "\r\n";
					HeadersString += "Content-Length: " + String.valueOf(body.length) + "\r\n";
					HeadersString += "\r\n";

					connectedClient.getOutputStream().write(HeadersString.getBytes());
					connectedClient.getOutputStream().write(body);
					connectedClient.close();
					return;

				}

				if (request_method.equals("GET"))
				{

					// 讀取首頁
					if (request_target.equals("/"))
					{

						String index_page = "";

						if (new File(root_dir + "/index.html").exists())
							index_page = root_dir + "/index.html";
						else if (new File(root_dir + "/index.htm").exists())
							index_page = root_dir + "/index.htm";
						else if (new File(root_dir + "/default.htm").exists())
							index_page = root_dir + "/default.htm";

						// 無首頁 日後再處理此狀況
						if (!index_page.equals(""))
						{
							target_file = new File(index_page);
							FileInputStream fin = new FileInputStream(target_file);
							body = new byte[(int) target_file.length()];
							fin.read(body);
							fin.close();
						}
						else
						{
							connectedClient.close();
							return;
							// 無首頁處理未實做
						}
					}
					else
					{
						target_file = new File(root_dir + request_target);

						if (target_file.exists())
						{
							Log.i("my", "there");
							// 處理檔案
							if (target_file.isFile())
							{
								FileInputStream fin = new FileInputStream(target_file);
								body = new byte[(int) target_file.length()];
								fin.read(body);
								fin.close();
							}
							else
							{
								// 處理目錄,尚未處理
								connectedClient.close();
								return;

							}

						}
						else
						{
							// 不存在的目錄或檔案,尚未處理
							connectedClient.close();
							return;
						}

					}
					HeadersString += http_ver + " 200 OK" + "\r\n";

					// ref http://stackoverflow.com/questions/3571223/how-do-i-get-the-file-extension-of-a-file-in-java
					String extension = "";
					int i = target_file.getName().lastIndexOf('.');
					int p = Math.max(target_file.getName().lastIndexOf('/'), target_file.getName().lastIndexOf('\\'));
					if (i > p)
						extension = target_file.getName().substring(i + 1).toLowerCase();

					if (extension.equals("htm") || extension.equals("html"))
						HeadersString += "Content-Type: text/html\n";
					else if (extension.equals("jpg"))
						HeadersString += "Content-type: image/jpeg";
					else
						HeadersString += "Content-type: application/octet-stream";

					Log.i("my", extension);

					HeadersString += "Connection: close\r\n";
					HeadersString += "\r\n";

					connectedClient.getOutputStream().write(HeadersString.getBytes());
					connectedClient.getOutputStream().write(body);
					connectedClient.close();
					return;
				}

			}
			catch (IOException e)
			{
			}

		}

		public clinet_deal(Socket client)
		{
			connectedClient = client;
		}

		private String nodeToString(Node node)
		{
			StringWriter sw = new StringWriter();
			try
			{
				Transformer t = TransformerFactory.newInstance().newTransformer();
				t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				t.setOutputProperty(OutputKeys.INDENT, "yes");
				t.transform(new DOMSource(node), new StreamResult(sw));
			}
			catch (TransformerException te)
			{
				System.out.println("nodeToString Transformer Exception");
			}
			return sw.toString();
		}

	}
	// ------------------------------------------------
}
