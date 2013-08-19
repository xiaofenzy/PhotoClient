package zy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import redis.clients.jedis.Jedis;

import common.RedisFactory;


public class PhotoClient {
	private int serverport;
	private String redisHost;							//redis服务器地址
	private int redisPort;								//端口号
	private String confPath = "conf.txt";			//配置文件
	private Socket s = null;						//与本地服务器连接的socket，用于写图片
	private InputStream is;
	private OutputStream os;					
	private Jedis jedis;
	/**
	 * 读取配置文件,进行必要初始化,并与服务器建立tcp连接
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
					if(ss[0].equals("serverport"))
						serverport = Integer.parseInt(ss[1]);
					if(ss[0].equals("redisHost"))
						redisHost = ss[1];
					if(ss[0].equals("redisPort"))
						redisPort = Integer.parseInt(ss[1]);
					
				}
			}
			br.close();
			//连接服务器
			
			jedis = RedisFactory.getNewInstance(redisHost, redisPort);
			
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
		if(!jedis.exists(md5))		//图片不存在
		{
			//只在第一次写的时候连接服务器
			if(s == null)
			{
				s = new Socket();
				try {
					s.connect(new InetSocketAddress("localhost",serverport));
					is = s.getInputStream();
//					isBr = new BufferedReader(new InputStreamReader(is));
					os = s.getOutputStream();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			try {
				writeLineToServer("store",os);
				writeLineToServer(set,os);
				writeLineToServer(md5,os);
				writeLineToServer(content.length+"",os);
				os.write(content);
				os.flush();
				
				int count = Integer.parseInt(readline(is));
				String info = new String(readBytes(count,is));
//				System.out.println("in client, store:"+info);
				return info;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			
		}
		else
		{
			System.out.println(md5+" exists");
			jedis.incr(md5+".ref");				
			return jedis.get(md5);
		}
	}
	
	
	public byte[] getPhoto(String md5)
	{
		String info = jedis.get(md5);
		if(info == null)
		{
			System.out.println("图片不存在");
			return null;
		}
		else {
			Socket getSocket = null;
			InputStream getis = null;
			OutputStream getos = null;
			try {
				String[] infos = info.split("#");
				getSocket = new Socket(); // 读取图片时所用的socket
				getSocket.connect(new InetSocketAddress(infos[2], serverport));
				getis = getSocket.getInputStream();
				getos = getSocket.getOutputStream();

				writeLineToServer("get", getos);
				writeLineToServer(md5, getos);

				int count = Integer.parseInt(readline(getis));
//				if (count == 0) {
//					System.out.println("图片不存在");
//					return null;
//				}
				// System.out.println(count);
				return readBytes(count, getis);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} finally {
				try {
					if(getis != null)
						getis.close();
					if(getos != null)
						getos.close();
					if(getSocket != null)
						getSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		
		
	}
	
	public byte[] searchPhoto(String info)
	{
		Socket searchSocket = null;
		InputStream searchis = null;
		OutputStream searchos = null;
		try {
			String[] infos = info.split("#");
			searchSocket = new Socket(); // 读取图片时所用的socket
			searchSocket.connect(new InetSocketAddress(infos[2], serverport));
			searchis = searchSocket.getInputStream();
			searchos = searchSocket.getOutputStream();

			writeLineToServer("search", searchos);
			writeLineToServer(info, searchos);

			int count = Integer.parseInt(readline(searchis));
//			if (count == 0) {
//				System.out.println("图片不存在");
//				return null;
//			}
			// System.out.println(count);
			return readBytes(count, searchis);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally {
			try {
				if(searchis != null)
					searchis.close();
				if(searchos != null)
					searchos.close();
				if(searchSocket != null)
					searchSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
	
	/**
	 * 向指定输出流输出一行字符串
	 * @param s
	 * @param ostream
	 * @throws IOException
	 */
	private void writeLineToServer(String s,OutputStream ostream) throws IOException {
		StringBuffer sb = new StringBuffer(s);
		sb.append("\n");			//\n的作用相当于分隔符
		ostream.write(sb.toString().getBytes());
	}
	
	/**
	 * 从指定的输入流中读取一行
	 * @param istream
	 * @return
	 */
	private String readline(InputStream istream)
	{
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		try {
			byte[] b = new byte[1];
			while(-1 != (istream.read(b)))
			{
				if(b[0] == '\n')
					break;
				else 
					baos.write(b);
			}
			if(baos.size() == 0)
				return null;
			return new String(baos.toByteArray());
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
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		//这里注意不能直接new byte[count],因为可能跟输入流的缓冲大小有关,如果count大于缓冲区大小,则缓冲区直接会被读空而返回,但实际上还有
		//想读的字节没有读出来.这时整个输入流位置指针就乱了,读不到自己想要的内容了.
		//所以要一点一点的读,给tcp滑动窗口进行滑动的时间
		byte[] buf = new byte[1024];			
		int n;
		try {
			while(count > buf.length)
			{
				n = istream.read(buf);
				baos.write(buf, 0, n);
				count -= n;
			}
			
			if(count>0)
			{
				buf = new byte[count];
				istream.read(buf);
				baos.write(buf);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return baos.toByteArray();
	}
	
	/**
	 * 关闭流、套接字和与redis的连接
	 * 只关闭写图片时与本地服务器交互的流和套接字，不关闭读图片时用到的流和套接字。
	 * 读图片用的套接字，读完就关掉了。写图片的套接字不关闭，一直保留，因为可以重用
	 */
	public void close()
	{
		try {
			jedis.quit();
			if(os != null)
				os.close();
			if(is != null)
				is.close();
			if(s != null)
				s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
