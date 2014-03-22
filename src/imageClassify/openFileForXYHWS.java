package imageClassify;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.LineRecordReader.LineReader;

import Jama.Matrix;


public class openFileForXYHWS {
	private double[] x;
	private double[] y;
	private int height;
	private int width;
	private Matrix sift;
	private String label;
	
	public openFileForXYHWS(String file,Configuration conf,FileSystem fs) throws IOException{
		
		List<String> xString = new ArrayList<String>(); 
		List<String> yString = new ArrayList<String>(); 
		int i;	
		
	
		Path xyhwfile = new Path(file+"/x");
		FSDataInputStream xyhwis = fs.open(xyhwfile);
		LineReader xyhwin = new LineReader(xyhwis,conf);  
		
		Path sfile = new Path(file+"/sift");
		FSDataInputStream siftis = fs.open(sfile);
		LineReader siftin = new LineReader(siftis,conf);  
		
		Path labelfile = new Path(file+"/label");
		FSDataInputStream labelis = fs.open(labelfile);
		LineReader labelin = new LineReader(labelis,conf);  
		

		Text line = new Text();

		//read x[]
		if(xyhwin.readLine(line) > 0){
			for(String str:line.toString().split(" "))
			{
				xString.add(str);
			}

		}
		int xsize =xString.size();
		x = new double[xsize];
		for(i=0;i<xsize;i++){
			x[i]=Double.valueOf(xString.get(i));
		}
		
		//read y[]
		if(xyhwin.readLine(line) > 0){
			for(String str:line.toString().split(" "))
			{
				yString.add(str);
			}

		}
		int ysize =yString.size();
		y = new double[ysize];
		for(i=0;i<yString.size();i++){
			y[i]=Double.valueOf(yString.get(i));
		}
		
		//read height and width
		if(xyhwin.readLine(line) > 0){
			height = Integer.parseInt(line.toString());
		}
		if(xyhwin.readLine(line) > 0){
			width = Integer.parseInt(line.toString());
		}
			 
			 //read sift
		int flag =0;
		sift = new Matrix(xsize,128);   
	    while(siftin.readLine(line)>0){
	    	String[] tmp = line.toString().split(" ");
	    	int col = tmp.length;
	    	for(i=0;i<col;i++)
				sift.set(flag, i, Double.valueOf(tmp[i]));
			flag++;
	    }
	    
	    //read label 
	    if(labelin.readLine(line) > 0){
			label  = line.toString();
		}
	    
			 xyhwis.close();
			 xyhwin.close();
			 siftis.close();
			 siftin.close();
			 labelis.close();
			 labelin.close();
			 
	}
	
	public double[] getX(){
		return x;
	}
	
	public double[] getY(){
		return y;
	}
	
	public int getH(){
		return height;
	}
	
	public int getW(){
		return width;
	}
		
	public Matrix getS(){
		return sift;
	}
	
	public String getL(){
		return label;
	}

}
