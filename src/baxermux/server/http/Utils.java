package baxermux.server.http;

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.util.*;
import org.apache.http.conn.util.InetAddressUtils;

// http://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device

public class Utils
{

	// array中尋找特定組成內容
	// http://stackoverflow.com/questions/1507780/searching-for-a-sequence-of-bytes-in-a-binary-file-with-java
	/**
	 * Finds the first occurrence of the pattern in the text.
	 */
	public static int indexOf(byte[] data, byte[] pattern)
	{
		int[] failure = computeFailure(pattern);

		int j = 0;
		if (data.length == 0)
			return -1;

		for (int i = 0; i < data.length; i++)
		{
			while (j > 0 && pattern[j] != data[i])
			{
				j = failure[j - 1];
			}
			if (pattern[j] == data[i])
			{
				j++;
			}
			if (j == pattern.length)
			{
				return i - pattern.length + 1;
			}
		}
		return -1;
	}

	/**
	 * Computes the failure function using a boot-strapping process, where the pattern is matched against itself.
	 */
	private static int[] computeFailure(byte[] pattern)
	{
		int[] failure = new int[pattern.length];

		int j = 0;
		for (int i = 1; i < pattern.length; i++)
		{
			while (j > 0 && pattern[j] != pattern[i])
			{
				j = failure[j - 1];
			}
			if (pattern[j] == pattern[i])
			{
				j++;
			}
			failure[i] = j;
		}

		return failure;
	}

	// 拷貝檔案
	// http://herolin.twbbs.org/entry/java-copy-file-directory/
	public static void copyFile(String srFile, String dtFile)
	{
		try
		{
			FileChannel srcChannel = new FileInputStream(srFile).getChannel();
			FileChannel dstChannel = new FileOutputStream(dtFile).getChannel();
			dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
			srcChannel.close();
			dstChannel.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	// 遞迴拷貝整體目錄
	public static void copyDirectory(File source, File target)
	{
		File[] file = source.listFiles();
		for (int i = 0; i < file.length; i++)
		{
			if (file[i].isFile())
			{
				File sourceDemo = new File(source.getAbsolutePath() + "/" + file[i].getName());
				File destDemo = new File(target.getAbsolutePath() + "/" + file[i].getName());
				copyFile(sourceDemo.getAbsolutePath(), destDemo.getAbsolutePath());
			}
			if (file[i].isDirectory())
			{
				File sourceDemo = new File(source.getAbsolutePath() + "/" + file[i].getName());
				File destDemo = new File(target.getAbsolutePath() + "/" + file[i].getName());
				destDemo.mkdir();
				copyDirectory(sourceDemo, destDemo);
			}
		}
	}

	// 遞迴刪除整體目錄
	// http://www.java2s.com/Tutorial/Java/0180__File/Removeadirectoryandallofitscontents.htm
	public static boolean removeDirectory(File directory)
	{
		if (directory == null)
			return false;
		if (!directory.exists())
			return true;
		if (!directory.isDirectory())
			return false;

		String[] list = directory.list();
		if (list != null)
		{
			for (int i = 0; i < list.length; i++)
			{
				File entry = new File(directory, list[i]);
				if (entry.isDirectory())
				{
					if (!removeDirectory(entry))
						return false;
				}
				else
				{
					if (!entry.delete())
						return false;
				}
			}
		}
		return directory.delete();
	}

	public static String getIPAddress(boolean useIPv4)
	{
		try
		{
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces)
			{
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs)
				{
					if (!addr.isLoopbackAddress())
					{
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4)
						{
							if (isIPv4)
								return sAddr;
						}
						else
						{
							if (!isIPv4)
							{
								int delim = sAddr.indexOf('%'); // drop ip6 port suffix
								return delim < 0 ? sAddr : sAddr.substring(0, delim);
							}
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
		} // for now eat exceptions
		return "";
	}

	// 下面的暫時用不到 但是留著參考

	/**
	 * Convert byte array to hex string
	 * 
	 * @param bytes
	 * @return
	 */
	public static String bytesToHex(byte[] bytes)
	{
		StringBuilder sbuf = new StringBuilder();
		for (int idx = 0; idx < bytes.length; idx++)
		{
			int intVal = bytes[idx] & 0xff;
			if (intVal < 0x10)
				sbuf.append("0");
			sbuf.append(Integer.toHexString(intVal).toUpperCase());
		}
		return sbuf.toString();
	}

	/**
	 * Get utf8 byte array.
	 * 
	 * @param str
	 * @return array of NULL if error was found
	 */
	public static byte[] getUTF8Bytes(String str)
	{
		try
		{
			return str.getBytes("UTF-8");
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	/**
	 * Load UTF8withBOM or any ansi text file.
	 * 
	 * @param filename
	 * @return
	 * @throws java.io.IOException
	 */
	public static String loadFileAsString(String filename) throws java.io.IOException
	{
		final int BUFLEN = 1024;
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
			byte[] bytes = new byte[BUFLEN];
			boolean isUTF8 = false;
			int read, count = 0;
			while ((read = is.read(bytes)) != -1)
			{
				if (count == 0 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF)
				{
					isUTF8 = true;
					baos.write(bytes, 3, read - 3); // drop UTF8 bom marker
				}
				else
				{
					baos.write(bytes, 0, read);
				}
				count += read;
			}
			return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
		}
		finally
		{
			try
			{
				is.close();
			}
			catch (Exception ex)
			{
			}
		}
	}

	/**
	 * Returns MAC address of the given interface name.
	 * 
	 * @param interfaceName
	 *                    eth0, wlan0 or NULL=use first interface
	 * @return mac address or empty string
	 */
	public static String getMACAddress(String interfaceName)
	{
		try
		{
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces)
			{
				if (interfaceName != null)
				{
					if (!intf.getName().equalsIgnoreCase(interfaceName))
						continue;
				}
				byte[] mac = intf.getHardwareAddress();
				if (mac == null)
					return "";
				StringBuilder buf = new StringBuilder();
				for (int idx = 0; idx < mac.length; idx++)
					buf.append(String.format("%02X:", mac[idx]));
				if (buf.length() > 0)
					buf.deleteCharAt(buf.length() - 1);
				return buf.toString();
			}
		}
		catch (Exception ex)
		{
		} // for now eat exceptions
		return "";
		/*
		 * try { // this is so Linux hack return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim(); } catch (IOException ex) { return null; }
		 */
	}
}
