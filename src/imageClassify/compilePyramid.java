package imageClassify;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.LineRecordReader.LineReader;

import Jama.Matrix;

public class compilePyramid {
	  
	 
	 public static void main(String[] args) throws NumberFormatException, IOException{
		 
		 	compilePyramidFunction cpf =compilePyramidFunction.getInstance();
		 //	 int pyramidLevels = 3;
		 	 int binsHigh = 4;
		// 	 int num_bins = binsHigh/2;
			 Configuration conf = new Configuration();
			 FileSystem hdfs = FileSystem.get(conf);
			 // path of dictonary
			 Path inPath = new Path(args[0]);
			  
			 FSDataInputStream dis = hdfs.open(inPath);
			 LineReader in = new LineReader(dis,conf);  

			 Text one = new Text();
			 String[] str =new String[128];
			 int line = 0;
			 Matrix dictionary = new Matrix(300,128);	 
			 
			 while(in.readLine(one) > 0){
				str =  one.toString().split(",");
				 for(int i=0;i<str.length;i++)
					 dictionary.set(line, i, Double.valueOf(str[i]));
				 line++;	 
			 }
			 dis.close();
			 in.close();
			 
			 Path siftplace= new Path(args[1]);
			 FileStatus[] fileStatus = hdfs.listStatus(siftplace);  
			 double[] x;
			 double[] y;
			 int h,w,i,j,k;
			 Matrix sift;
			 Matrix d2;
			 Matrix index;
			 List<Matrix> pyramid_all = new ArrayList<Matrix>();
			 List<String> label_all = new ArrayList<String>();
			 for(FileStatus file : fileStatus){
				 System.out.println("Processing");
				 
				 List<Integer> texton_patch = new ArrayList<Integer>();
				 List<Matrix> pyramid_cell_1 = new ArrayList<Matrix>();
				 List<Matrix> pyramid_cell_2 = new ArrayList<Matrix>();
				 List<Matrix> pyramid_cell_3 = new ArrayList<Matrix>();
				 Matrix pyramid = new Matrix(1,6300);
				
				 String eachfile = args[1]+"/"+file.getPath().getName();
				 openFileForXYHWS xyhws = new openFileForXYHWS(eachfile,conf,hdfs);
				 x = xyhws.getX();
				 y = xyhws.getY();
				 h = xyhws.getH();
				 w = xyhws.getW();
				 sift = xyhws.getS();
				 label_all.add(xyhws.getL());
				 d2 = cpf.EuclideanDistance(sift, dictionary);
				 int d2row = d2.getRowDimension();
				 int d2col = d2.getColumnDimension();
				 index = new Matrix(1,d2row);
				 for(i=0;i<d2row;i++)
					 index.set(0, i, cpf.rowfindmin(d2.getMatrix(i, i, 0, d2col-1)));
				 for(i=1;i<=binsHigh;i++){
					 for(j=1;j<=binsHigh;j++)
					 {
						 int x_lo = (int)Math.floor(w/binsHigh*(i-1));
						 int x_hi = (int)Math.floor(w/binsHigh*i);
						 int y_lo = (int)Math.floor(h/binsHigh*(j-1));
						 int y_hi = (int)Math.floor(h/binsHigh*j);
						 for(k=0;k<d2row;k++)
						 {
							 if((x[k]>x_lo)&&(x[k]<=x_hi)&&(y[k]>y_lo)&&(y[k]<=y_hi))
								texton_patch.add((int)index.get(0,k)); 
						 }
						 pyramid_cell_1.add(cpf.hist(texton_patch).times(1.0/d2row));
						 
					 }
				 }
				 
				 pyramid_cell_2.add(pyramid_cell_1.get(0).plus(pyramid_cell_1.get(1).plus(pyramid_cell_1.get(4).plus(pyramid_cell_1.get(5)))));
				 pyramid_cell_2.add(pyramid_cell_1.get(2).plus(pyramid_cell_1.get(3).plus(pyramid_cell_1.get(6).plus(pyramid_cell_1.get(7)))));
				 pyramid_cell_2.add(pyramid_cell_1.get(8).plus(pyramid_cell_1.get(9).plus(pyramid_cell_1.get(12).plus(pyramid_cell_1.get(13)))));
				 pyramid_cell_2.add(pyramid_cell_1.get(10).plus(pyramid_cell_1.get(11).plus(pyramid_cell_1.get(14).plus(pyramid_cell_1.get(15)))));	 
				 pyramid_cell_3.add(pyramid_cell_2.get(0).plus(pyramid_cell_2.get(1).plus(pyramid_cell_2.get(2).plus(pyramid_cell_2.get(3)))));	
				 
				 int count =0;
				 for(i=0;i<21;i++){
					 if(i<16)
						 pyramid.setMatrix(0, 0, 0+count, 299+count, pyramid_cell_1.get(i).times(Math.pow(2, -1)));
					 else if((i>=16)&&(i<20))
						 pyramid.setMatrix(0, 0, 0+count, 299+count, pyramid_cell_2.get(i-16).times(Math.pow(2, -2)));
					 else
						 pyramid.setMatrix(0, 0, 0+count, 299+count, pyramid_cell_3.get(i-20).times(Math.pow(2, -2)));
					 count = count +300;
				 }

				 pyramid_all.add(pyramid);
			 }
			 
			 Path outPath = new Path(args[2]);
			 FSDataOutputStream outputStream = hdfs.create(outPath); 	
			 BufferedWriter pyramidout = new BufferedWriter(new OutputStreamWriter(outputStream,"UTF-8"));
			 int size = pyramid_all.size();
			 for(i=0;i<size;i++){
				 for(j=0;j<6300;j++){
					 pyramidout.write(Double.toString(pyramid_all.get(i).get(0, j))+" ");
				 }
				 pyramidout.write(label_all.get(i));
				 pyramidout.write('\n');
			 }
			 pyramidout.flush();
			 pyramidout.close();
			 outputStream.close();
	 
	}
	 

}
