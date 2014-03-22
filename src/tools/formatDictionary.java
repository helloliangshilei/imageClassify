package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class formatDictionary {
	
	/*
	 * input:from local filesystem
	 * outpur: to hdfs
	 */
	
	public static void main(String[] args) throws IOException{
	
		   Configuration conf = new Configuration();
		    FileSystem hdfs = FileSystem.get(conf);
			 
		    File file = new File(args[0]);
		     BufferedReader reader = null;
		     reader = new BufferedReader(new FileReader(file));
		     String tempString = null;
		     
		     Path outPath = new Path(args[1]);
		     FSDataOutputStream outputStream = hdfs.create(outPath); 
		     BufferedWriter dicout = new BufferedWriter(new OutputStreamWriter(outputStream,"UTF-8"));
		     
		     while ((tempString = reader.readLine()) != null) {
		    	 	dicout.write(tempString.substring(tempString.indexOf("[")+1, tempString.indexOf("]")));
		    	 	dicout.newLine();
		        }
		    
		     dicout.flush();
		     dicout.close();
			 reader.close();
			 outputStream.close();

		
	}

}
