package test;

import iie.mm.client.PhotoClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CTest {
	private String sourceRoot = "/home/zhaoyang/photo/";
	private PhotoClient sp = new PhotoClient();
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		CTest t = new CTest();
//		if(args.length>0)
//		{
//			
//			if(args[0].equals("store"))
//				t.store2();
//			if(args[0].equals("get"))
//				t.get(args[1],args[2]);
//		}else
//			t.get();
		for(int i = 0;i<5;i++)
		new Thread(new StoreThread(""+i)).start();
//		t.get();
		
//		System.in.read();
//		sp.close();
	}
	private void get(String set,String md5)
	{
		byte[] content = sp.getPhoto(set,md5);
		System.out.println("content length read from server:"+content.length);
	}
	private void get()
	{
		for(int j = 0;j<5;j++)
		for(int i = 0;i<5;i++)
		{
			byte[] content = sp.getPhoto("loc",j+""+i);
			if(content.length == 0)
				continue;
			try {
				FileOutputStream fos = new FileOutputStream((j+""+i));
				System.out.println("content length read from server:"+content.length);
				fos.write(content);
				fos.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	public void store2()
	{
		int i = 0,j=0;
		for(;i<5;i++)
		{
			byte[] content = new byte[40];
			String s =sp.storePhoto("set"+i,j+"b",content);
//			System.out.println("in client store"+s);
			j++;
		}
	}
	public void store(String set){
		PhotoClient sp1 = new PhotoClient();
		File sourceDir = new File(sourceRoot);
//		System.out.println(destDir.getName());
		FileInputStream fis; 
		int i = 0,j=0;
		
		for(File f : sourceDir.listFiles())
		{
			
			try {
				fis = new FileInputStream(f);
				byte[] content = new byte[fis.available()];
				fis.read(content);
				String s =sp1.storePhoto("loc",j+set,content);
//				System.out.println("in client store"+s);
				j++;
//				System.out.println(f.getName());
//				System.out.println(content.length);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
	static class StoreThread implements Runnable
	{
		private String set;
		
		public StoreThread(String set)
		{
			this.set = set;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			new CTest().store(set);
		}
		
	}
}
