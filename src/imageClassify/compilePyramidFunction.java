package imageClassify;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import Jama.Matrix;

public class compilePyramidFunction {
	
		private static final compilePyramidFunction static_object = new compilePyramidFunction();

		public static compilePyramidFunction getInstance() {
			return static_object;
		}
	
		//get dictionary in one file
		 public  void clusterFormat(String srcfile,String dstfile) throws IOException {
		        File file = new File(srcfile);
		        BufferedReader reader = null;
		        FileWriter fw = new FileWriter(dstfile);
		        BufferedWriter bw = new BufferedWriter(fw);
		        reader = new BufferedReader(new FileReader(file));
		        String tempString = null;
		        while ((tempString = reader.readLine()) != null) {
		            bw.write(tempString.substring(tempString.indexOf("[")+1, tempString.indexOf("]")));
		            bw.newLine();
		        }
		        bw.flush();    
		        bw.close();
		        reader.close();
		        fw.close();     
		    }
		 
		 public Matrix EuclideanDistance(Matrix x,Matrix y){
			 int xcol = x.getColumnDimension();
			 int xrow = x.getRowDimension();
			 int ycol = y.getColumnDimension();
			 int yrow = y.getRowDimension();
			 Matrix result = new Matrix(xrow,yrow);
			 Matrix tmp = new Matrix(1,xcol);
			 double sum;
			 int i,j,k;
			 for(i=0;i<xrow;i++)
				 for(j=0;j<yrow;j++)
				 {
					 sum = 0;
					 tmp=x.getMatrix(i, i, 0, xcol-1).minus(y.getMatrix(j, j, 0, ycol-1));
					 for(k=0;k<xcol;k++)
						 sum+=tmp.get(0, k)*tmp.get(0, k);
					 
					 result.set(i, j, Math.sqrt(sum));
				 }
			 return result;
		 }
		 
		 //find min value's index in each row
		 public double rowfindmin(Matrix row){
			 double min=row.get(0, 0);
			 int index = 0;
			 int len = row.getColumnDimension();
			 int i;
			 for(i=1;i<len;i++)
				 if(row.get(0, i)<min){
					 min = row.get(0, i);
					 index = i;
				 }
			 return index;
		 }
		 
		 //hist on dictionary of 300 words
		 public Matrix hist(List<Integer> li){
			 int len = li.size();
			 Matrix result = new Matrix(1,300);
			 int i=0;
			 for(i=0;i<len;i++)
				 result.set(0, (int)li.get(i), result.get(0, (int)li.get(i))+1);
			 return result;
				 
		 }
		 
		 

}
