package iie.mm.client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ClientConf {
	private static String redisHost;
	private static int redisPort;
	private static int serverPort;
	
	static{
		try {
			BufferedReader br = new BufferedReader(new FileReader("conf.txt"));
			String line = "";
			while(true)
			{
				line = br.readLine();
				if(line == null)
					break;
				else if(!line.startsWith("#"))		
				{
					String[] ss = line.split("=");
					if(ss[0].equals("redisHost"))
						redisHost = ss[1];
					if(ss[0].equals("redisPort"))
						redisPort = Integer.parseInt(ss[1]);
					if (ss[0].equals("serverport"))
						serverPort = Integer.parseInt(ss[1]);
				}
			}
			br.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String getRedisHost() {
		return redisHost;
	}

	public static void setRedisHost(String redisHost) {
		ClientConf.redisHost = redisHost;
	}

	public static int getRedisPort() {
		return redisPort;
	}

	public static void setRedisPort(int redisPort) {
		ClientConf.redisPort = redisPort;
	}

	public static int getServerPort() {
		return serverPort;
	}

	public static void setServerPort(int serverPort) {
		ClientConf.serverPort = serverPort;
	}
	
	
}
