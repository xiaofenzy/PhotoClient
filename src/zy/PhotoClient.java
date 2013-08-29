package zy;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import redis.clients.jedis.Jedis;
import common.ActionType;
import common.RedisFactory;


public class PhotoClient {
//	private int serverport;
	private String redisHost;							//redis服务器地址
	private int redisPort;								//端口号
	private String confPath = "conf.txt";			//配置文件
	private final File socketFile = new File(new File(System.
			getProperty("java.io.tmpdir")), "junixsocket-test.sock");	//用于构造junixsocket,这个文件必须客户端和服务端一样
	
	private AFUNIXSocket storeSocket;				//用于写请求的socket
	private DataInputStream storeis;
	private DataOutputStream storeos;
	
	private Hashtable<String,Socket> socketHash;				//缓存与服务端的tcp连接,服务端名称到连接的映射
	private Socket searchSocket;				//用于读请求的socket
	private DataInputStream searchis;
	private DataOutputStream searchos;
	private Jedis jedis;
	/**
	 * 读取配置文件,进行必要初始化,并与redis服务器建立连接
	 */
	public PhotoClient()
	{
		try {
			BufferedReader br = new BufferedReader(new FileReader(confPath));
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
					
				}
			}
			br.close();
			//连接服务器
			
			jedis = RedisFactory.getNewInstance(redisHost, redisPort);
			
			socketHash = new Hashtable<String,Socket>();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param set
	 * @param md5
	 * @param content
	 * @return		
	 */
	public String storePhoto(String set, String md5, byte[] content)
	{
		String info = jedis.get(md5);
		if(info == null)		//图片不存在
		{
			//只在第一次写的时候连接服务器
			if(storeSocket == null)
			{
				try {
					storeSocket = AFUNIXSocket.newInstance();
					storeSocket.connect(new AFUNIXSocketAddress(socketFile));
					storeos = new DataOutputStream(storeSocket.getOutputStream());
					storeis = new DataInputStream(storeSocket.getInputStream());
					
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			try {
				//action,set,md5,content的length写过去
				byte[] header = new byte[4];
				header[0] = ActionType.STORE;
				header[1] = (byte) set.getBytes().length;
				header[2] = (byte) md5.getBytes().length;
				storeos.write(header);
				storeos.writeInt(content.length);
				
				//set,md5,content的实际内容写过去
				storeos.write(set.getBytes());
				storeos.write(md5.getBytes());
				storeos.write(content);
				storeos.flush();
				int count = storeis.readInt();
				if(count == -1)
					return jedis.get(md5);
				String s = new String(readBytes(count,storeis));
				return s;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			
		}
		else
		{
			System.out.println(md5+" exists");
			jedis.incr("r."+md5);				
			return info;
		}
	}
	
	/**
	 * 读取图片
	 * @param md5
	 * @return		md5代表的图片内容,如果图片不存在则返回长度为0的byte数组
	 */
	public byte[] getPhoto(String md5)
	{
		String info = jedis.get(md5);
		if(info == null)
		{
			System.out.println("图片不存在");
			return new byte[0];
		}
		else {
			return searchPhoto(info);
		}
	}
	
	public byte[] searchPhoto(String info)
	{
		try {
			String[] infos = info.split("#");
			if(socketHash.containsKey(infos[2]))
				searchSocket = socketHash.get(infos[2]);
			else
			{				
				searchSocket = new Socket(); // 读取图片时所用的socket
				searchSocket.connect(new InetSocketAddress(infos[2], Integer.parseInt(infos[3])));
				socketHash.put(infos[2], searchSocket);
			}
			searchis =new DataInputStream(searchSocket.getInputStream());
			searchos =new DataOutputStream(searchSocket.getOutputStream());

			//action,info的length写过去
			byte[] header = new byte[4];
			header[0] = ActionType.SEARCH;
			header[1] = (byte) info.getBytes().length;
			searchos.write(header);
			
			//info的实际内容写过去
			searchos.write(info.getBytes());
			
			int count = searchis.readInt();
			return readBytes(count, searchis);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		
		}

	}
	
	/**
	 * 从输入流中读取count个字节
	 * @param count
	 * @return
	 */
	public byte[] readBytes(int count,InputStream istream)
	{
//		System.out.println("in client readBytes:"+count);
		byte[] buf = new byte[count];			
		int n = 0;
		try {
			while(count > n)
			{
				n += istream.read(buf,n,count-n);
//				System.out.println(n);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buf;
	}
	
	/**
	 * 关闭流、套接字和与redis的连接
	 * 用于读和写的套接字全部都关闭
	 */
	public void close()
	{
		try {
			jedis.quit();
			if(storeos != null)
				storeos.close();
			if(storeis != null)
				storeis.close();
			if(storeSocket != null)
				storeSocket.close();
			
			Enumeration<Socket> es = socketHash.elements();
			while(es.hasMoreElements())
			{
				Socket s = es.nextElement();
				s.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
