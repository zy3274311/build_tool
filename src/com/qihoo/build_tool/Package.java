package com.qihoo.package_tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Package {

	private static String projectBasePath;//要打包的项目根目录
	
	private static String signApk = "Daily-release.apk";//这里的文件名必须是准确的项目名！
	private static String copyApkPath;//保存打包apk的根目录
	private static String reNameApk = "kandian_";//重命名的项目名称前缀
	
	public static void main(String args[]) {
		if(args==null||args.length<2){
			System.out.println("参数错误");
			return;
		}
		
		projectBasePath = args[0];
		copyApkPath = args[1];
		
		String update_cmd = "android update project -n Daily -t android-21 -p "+projectBasePath;
		runCmd(projectBasePath, update_cmd);
		
		String[] channelList = getChannels();
		if(channelList==null||channelList.length==0){
			System.out.println("渠道名称配置错误，请检查工程目录下的config文件");
			return;
		}
		
		long startTime = 0L;
		long endTime = 0L;
		long totalTime = 0L;
		try {
			startTime = System.currentTimeMillis();

			for(int i =0;i<channelList.length;i++){
				String channel_vlaue = channelList[i];
				System.out.println();
				System.out.println();
				System.out.println("---------"+channel_vlaue+"打包开始----------");
				
//				System.out.println("正在修改渠道名称："+channel_vlaue);
				updateManifest(channel_vlaue);
				runCmd(projectBasePath, "ant clean");
				runCmd(projectBasePath, "ant release", true);
				
				// 打完包后执行重命名加拷贝操作
				File file = new File(projectBasePath + File.separator + "bin" + File.separator + signApk);// bin目录下签名的apk文件
				
				File renameFile = new File(copyApkPath + File.separator + reNameApk+ channel_vlaue + ".apk");
				
				if(renameFile.exists()){
					renameFile.delete();
				}
				boolean renametag = file.renameTo(renameFile);
				if(renametag){
					System.out.println("---------"+channel_vlaue+"打包成功----------");
				}else{
					System.out.println("---------"+channel_vlaue+"打包失败----------");
				}
				
			}
			//清理临时打包文件
			runCmd(projectBasePath, "ant clean");
			
			endTime = System.currentTimeMillis();
			totalTime = endTime - startTime;
			
			System.out.println();
			System.out.println();
			System.out.println("耗费时间为:" + getBeapartDate(totalTime));

		} catch (Exception e) {
			e.printStackTrace();
			
			System.out.println();
			System.out.println();
			System.out.println("---------批量自动化打包中发生异常----------");
			endTime = System.currentTimeMillis();
			totalTime = endTime - startTime;
			System.out.println("耗费时间为:" + getBeapartDate(totalTime));
		}
	
	}

	/**
	 * 获取频道号
	 * @return
	 */
	private static String[] getChannels() {
		try {
			String configPath = "config";
			File file = new File(projectBasePath, configPath);
			FileInputStream fileInputStream = new FileInputStream(file);
			byte[] data = StreamTool.readStream(fileInputStream);
			String channel_all = new String(data);
			String[] channelList = channel_all.split(",");
//			System.out.println("渠道号："+channel_all);
			return channelList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据所秒数,计算相差的时间并以**时**分**秒返回
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private static String getBeapartDate(long m) {
		m = m / 1000;
		String beapartdate = "";
		int nDay = (int) m / (24 * 60 * 60);
		int nHour = (int) (m - nDay * 24 * 60 * 60) / (60 * 60);
		int nMinute = (int) (m - nDay * 24 * 60 * 60 - nHour * 60 * 60) / 60;
		int nSecond = (int) m - nDay * 24 * 60 * 60 - nHour * 60 * 60 - nMinute* 60;
		beapartdate = nDay + "天" + nHour + "小时" + nMinute + "分" + nSecond + "秒";
		return beapartdate;
	}

	/**
	 * 修改频道号
	 */
	private static void updateManifest(String value){
		try {
			String filePath = projectBasePath + File.separator+ "AndroidManifest.xml";
			FileInputStream fileInputStream = new FileInputStream(filePath);
			byte[] data =StreamTool.readStream(fileInputStream);
			String content = new String(data,"utf-8");
			String regex = "<meta-data android:name=\"CHANNEL_VALUE\" android:value=\".*\" />";
			String replacement = "<meta-data android:name=\"CHANNEL_VALUE\" android:value=\""+value+"\" />";
			content = content.replaceFirst(regex, replacement);
			
			FileOutputStream fileOutputStream = new FileOutputStream(filePath);
			fileOutputStream.write(content.getBytes("utf-8"));
			fileOutputStream.close();
			return;
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	private static void runCmd(String dir, String cmd){
		runCmd(dir, cmd, false);
	}
	
	private static void runCmd(String dir, String cmd, boolean hasProgressLog){
		//检查windows系统环境
//		String osName = System.getProperty("os.name" );
//		System.out.println(osName);
		
		try {
			String[] cmdarray = new String[3];
			cmdarray[0] = "cmd.exe" ;
			cmdarray[1] = "/C" ;
			cmdarray[2] = cmd;
			
			Process process = Runtime.getRuntime().exec(cmdarray, null, new File(dir));
			InputStream inStream = process.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inStream);
			BufferedReader reader = new BufferedReader(inputStreamReader);
			
			int progress = 0;
			int count = 1015;
			int i = 0;
			String log = progress+"%...";
			if(hasProgressLog){
				System.out.print("已进行"+log);
			}
			while(true){
                String line=reader.readLine();
//                System.out.println(line);
                if(null==line){
                	if(hasProgressLog){
	                	int len = log.length();
	                	for(int j=0;j<len;j++){
	                		System.out.print("\b");
	                	}
                		System.out.print("100%...");
                		System.out.println();
                	}
                	break;
                }
                
                if(hasProgressLog){
                	i++;
                	int p = i*100/count;
                	if(p>100){
                		p = 100;
                	}
                	if(p!=progress){
                		progress = p;
                		
                		int len = log.length();
                		for(int j=0;j<len;j++){
                			System.out.print("\b");
                		}
                		log = progress+"%...";
                		System.out.print(log);
                	}
                }
            }
            
            
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	


}
