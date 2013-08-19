package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import zy.PhotoClient;

public class CTest {
	private static String sourceRoot = "/home/zhaoyang/photo/";
	private static PhotoClient sp = new PhotoClient();
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CTest t = new CTest();
//		t.store();
		t.get();
//		t.get();
		sp.close();
	}
	private void get()
	{
//		String md5 = "psbx.png";
//		byte[] content = sp.getPhoto(md5);
		String info = "1#set4#zhaoyang-pc#photo/set4/block_1#0#41244";
		byte[] content = sp.searchPhoto(info);
		if(content == null)
			return;
		try {
			FileOutputStream fos = new FileOutputStream("b.png");
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
	
	public void store(){
		File sourceDir = new File(sourceRoot);
//		System.out.println(destDir.getName());
		FileInputStream fis; 
		int i = 0;
		for(File f : sourceDir.listFiles())
		{
			try {
				fis = new FileInputStream(f);
				byte[] content = new byte[fis.available()];
				fis.read(content);
				sp.storePhoto("set"+i,f.getName(),content);
				System.out.println(f.getName());
				i++;
//				System.out.println(content.length);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
//			getPhoto(f.getName());
		}
		
		
	}
}
