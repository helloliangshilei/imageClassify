package imageClassify;

import java.util.ArrayList;
import java.util.List;

import Jama.Matrix;

public class generateSiftFunction {
	
	private static final generateSiftFunction static_object = new generateSiftFunction();

	public static generateSiftFunction getInstance() {
		return static_object;
	}
	
	public Matrix gen_gauss(double sigma,int n){
		
		double[][] x = new double[5][5];
		double[][] y = new double[5][5];
		double sum =0;
		int i,j;
		for(i = 0; i<5 ;i++){
			x[i][0] = -2;
			y[0][i] = -2;
		}
		
		for(i=1;i<5;i++){
			for(j=0;j<5;j++){
				x[j][i] = x[j][i-1] + 1;
				y[i][j] = y[i-1][j] + 1;
			}
		}
		
		Matrix A =new Matrix(x);
		Matrix B =new Matrix(y);
		Matrix C =A.arrayTimes(A).plus(B.arrayTimes(B));
		
		Matrix arg =new Matrix(5,5);

		for(i=0;i<5;i++)
			for(j=0;j<5;j++)
			{
				arg.set(i, j, Math.exp(-C.get(i,j)/(sigma*sigma*2)));
				sum +=arg.get(i, j);
			}
		if(sum != 0){
			arg.timesEquals(1/sum);			
		}
		return arg;
	}
	
	//for 5*5 matrix
	public Matrix gradient(Matrix g){
		Matrix result =new Matrix(5,5);
		int i,j;
		for(i=0;i<5;i++){
			result.set(0, i, g.get(1,i)-g.get(0, i));
			result.set(4, i, g.get(4,i)-g.get(3, i));
		}
		for(i=2;i<5;i++)
		{
			for(j=0;j<5;j++){
				result.set(i-1, j,( g.get(i,j)-g.get(i-2, j))/2);
			}
		}
		return result;					
	}
	
	//for 5*5 matrix
	public Matrix gen_dgauss(double sigma,int n){
		Matrix A = gen_gauss(sigma,n);
		A = gradient(A);
		Matrix gy =new Matrix(5,5);
		int i,j;
		double sum = 0;
		for(i=0;i<5;i++)
			for(j=0;j<5;j++)
			{
				if(A.get(i,j)<0)
					sum +=-A.get(i,j);
				else
					sum +=A.get(i,j);
			}
		
		gy=A.times(2/sum);
		
		return gy;
	}
	
	public Matrix take4Format(Matrix t){
		int i,j;
		int t1=t.getRowDimension();
		int t2=t.getColumnDimension();
		for(i=0;i<t1;i++)
			for(j=0;j<t2;j++)
				t.set(i, j, (double)(Math.round(t.get(i, j)*10000)/10000.0));
		return t;
	}
	
	public Matrix rotate180(Matrix t){
		int i,j;
		int t1=t.getRowDimension();
		int t2=t.getColumnDimension();
		Matrix res = new Matrix(t1,t2);
		for(i=0;i<t1;i++)
			for(j=0;j<t2;j++)
				res.set(i, j, t.get(t1-i-1, t2-j-1));
		return res;
		
	}
	
	public Matrix conv2(Matrix x,Matrix y,Matrix I){
		int Im = I.getRowDimension();
		int In = I.getColumnDimension();
		int n = x.getRowDimension();
		
		
		Matrix tmp = new Matrix(n,n);
		tmp = x.times(y.transpose());
		tmp = take4Format(tmp);
		
		Matrix C1 = new Matrix(Im+n-1,In+n-1);
		Matrix res = new Matrix(Im,In);
		int i,j;
		for(i=0;i<Im;i++){
			for(j=0;j<In;j++){
				int r1 = i;
				int r2 =r1 +n-1;
				int c1 =j;
				int c2 = c1+n-1;
				C1.setMatrix(r1, r2, c1, c2,C1.getMatrix(r1, r2, c1, c2).plus(tmp.times(I.get(i, j))));
				
			}
		}
		res = C1.getMatrix((int)(Math.floor(n/2)), (int)(Math.floor(n/2)+Im-1), (int)(Math.floor(n/2)),(int)(Math.floor(n/2)+In-1));
		
		return res;
		
		
	}
	public Matrix filter2(Matrix g,Matrix I){
		I = take4Format(I);
		Matrix ss =new Matrix(5,5);
		ss=g.copy();
		ss=rotate180(ss);
		Matrix u = ss.svd().getU();
		Matrix s = ss.svd().getS();
		Matrix v = ss.svd().getV();
		
		Matrix hcol = new Matrix(5,1);
		Matrix hrow = new Matrix(5,1);
		for(int i=0;i<5;i++){
			hcol.set(i, 0, u.get(i,0)*Math.sqrt(s.get(0,0)));
			hrow.set(i, 0, v.get(i,0)*Math.sqrt(s.get(0,0)));
			
		}
		hcol=take4Format(hcol);
		hrow=take4Format(hrow);
		
		
		
		Matrix res = new Matrix(I.getRowDimension(),I.getColumnDimension());
		res = conv2(hcol,hrow,I);
		return res;
		
	}
	public Matrix find_sift_grid(Matrix I,Matrix gridX,Matrix gridY,int patchSize){
			
		int num_angles = 8;
		int num_bins = 4;
		int num_samples = num_bins * num_bins;
		int alpha = 9;
		int row = I.getRowDimension();//hgt
		int col = I.getColumnDimension();//wid
		int num_patches = gridX.getColumnDimension()*gridX.getRowDimension();
		
		Matrix sift_arr = new Matrix(num_patches,num_samples*num_angles);
		
		int i,j,a;
		double angle_step = 2 * Math.PI / num_angles;
		double[] angles =new double[8];
		angles[0] = 0;
		for(i = 1;i<8;i++)
			angles[i] = angles[i-1] + angle_step;
		
		Matrix gx = new Matrix(5,5);
		Matrix gy = new Matrix(5,5);
		gy = gen_dgauss(0.8, 5);
		gx = gy.transpose();
		gx = take4Format(gx);
		gy = take4Format(gy);
		
		Matrix I_X = new Matrix(row,col);
		Matrix I_Y = new Matrix(row,col);
		Matrix I_mag = new Matrix(row,col);
		Matrix I_theta = new Matrix(row,col);
		I_X = filter2(gx,I);
		I_Y = filter2(gy,I);
		I_mag = I_X.arrayTimes(I_X).plus(I_Y.arrayTimes(I_Y));
		for(i = 0;i<row;i++)
			for(j=0;j<col;j++){
				I_mag.set(i, j, Math.sqrt(I_mag.get(i, j)));
				I_theta.set(i, j, Math.atan2(I_Y.get(i, j), I_X.get(i,j)));
			}
		I_mag = take4Format(I_mag);
		I_theta = take4Format(I_theta);
		Matrix sample_x = new Matrix(1,16);
		Matrix sample_y = new Matrix(1,16);
		sample_x.set(0,0,-0.75);
		sample_y.set(0,0,-0.75);
		for(i=1;i<16;i++){
			if(i%4==0)
				sample_x.set(0,i,sample_x.get(0, i-1)+0.5);
			else
				sample_x.set(0,i,sample_x.get(0, i-1));
		}
		for(i=1;i<16;i++){
			if(i%4==0)
				sample_y.set(0,i,sample_y.get(0, 0));
			else
				sample_y.set(0,i,sample_y.get(0, i-1)+0.5);
		}
		
		
		List<Matrix> I_orientation =new ArrayList<Matrix>();
		for(a=0;a<num_angles;a++){
			Matrix tmp =new Matrix(row,col);
			for(i=0;i<row;i++)
				for(j=0;j<col;j++)
				{
					double t =Math.pow(Math.cos(I_theta.get(i, j)-angles[a]),alpha);
					if(t>0)
						tmp.set(i,j,t);
					else
						tmp.set(i, j, 0);
				}
			tmp=take4Format(tmp);
			I_orientation.add(tmp.arrayTimes(I_mag));
		}
		
		for(int aflag=0;aflag<num_patches;aflag++){
			int r = patchSize/2;
			int gridrow = gridX.getRowDimension();
			double cx = gridX.get(aflag%gridrow, aflag/gridrow)+r-0.5;
			double cy = gridY.get(aflag%gridrow, aflag/gridrow)+r-0.5;
			
			Matrix sample_x_t = new Matrix(1,16);
			Matrix sample_y_t = new Matrix(1,16);
			for(i=0;i<16;i++){
				sample_x_t.set(0, i, sample_x.times(r).get(0, i)+cx);
				sample_y_t.set(0, i, sample_y.times(r).get(0, i)+cy);
			}
			double sample_res = sample_y_t.get(0, 1)-sample_y_t.get(0, 0);
			
			int x_lo = (int)gridX.get(aflag%gridrow, aflag/gridrow);
			int x_hi = (int)gridX.get(aflag%gridrow, aflag/gridrow)+patchSize - 1;
			int y_lo = (int)gridY.get(aflag%gridrow, aflag/gridrow);
			int y_hi = (int)gridY.get(aflag%gridrow, aflag/gridrow)+patchSize - 1;
			Matrix sample_px =new Matrix(256,1);
			Matrix sample_py =new Matrix(256,1);
			Matrix sample_px_tmp =new Matrix(16,16);
			Matrix sample_py_tmp =new Matrix(16,16);
			
			for(i=0;i<16;i++)
				sample_px_tmp.set(i, 0, x_lo);
			for(i =1;i<16;i++)
				for(j=0;j<16;j++)
					sample_px_tmp.set(j, i, sample_px_tmp.get(j, i-1)+1);
			for(i=0;i<16;i++)
				sample_py_tmp.set(0, i, y_lo);
			for(i=1;i<16;i++)
				for(j=0;j<16;j++)
					sample_py_tmp.set(i, j, sample_py_tmp.get(i-1, j)+1);
			
			//double num_pix = sample_px_tmp.getColumnDimension()*sample_px_tmp.getRowDimension();
			for(i=0;i<256;i++){
				sample_px.set(i, 0, sample_px_tmp.get(i%16, i/16));
				sample_py.set(i, 0, sample_py_tmp.get(i%16, i/16));
			}
			
			Matrix dist_px = new Matrix(256,16);
			Matrix dist_py = new Matrix(256,16);
			for(i=0;i<16;i++)
				for(j=0;j<256;j++){
					dist_px.set(j, i, Math.abs(sample_px.get(j, 0)-sample_x_t.get(0, i)));
					dist_py.set(j, i, Math.abs(sample_py.get(j, 0)-sample_y_t.get(0, i)));
				}
				
			Matrix weights_x = new Matrix(256,16);	
			Matrix weights_y = new Matrix(256,16);
			Matrix weights = new Matrix(256,16);	
			weights_x = dist_px.times(1/sample_res);
			for(i=0;i<256;i++){
				for(j=0;j<16;j++){
					if(weights_x.get(i, j)<=1)
						weights_x.set(i, j, 1-weights_x.get(i, j));
					else
						weights_x.set(i, j, 0);
				}
			}
			weights_y = dist_py.times(1/sample_res);
			for(i=0;i<256;i++){
				for(j=0;j<16;j++){
					if(weights_y.get(i, j)<=1)
						weights_y.set(i, j, 1-weights_y.get(i, j));
					else
						weights_y.set(i, j, 0);
				}
			}
			
			weights = weights_x.arrayTimes(weights_y);	
			
			Matrix curr_sift = new Matrix(8,16);
			for(a=0;a<num_angles;a++){
				Matrix tmp =new Matrix(256,16);
				Matrix tmp_t = new Matrix(16,16);
				tmp_t = I_orientation.get(a).getMatrix(y_lo-1, y_hi-1, x_lo-1, x_hi-1);
				for(i=0;i<256;i++){
					tmp.set(i, 0, tmp_t.get(i%16, i/16));
				}
				for(i=1;i<num_samples;i++)
					for(j=0;j<256;j++)
						tmp.set(j, i, tmp.get(j, 0));
				tmp.arrayTimesEquals(weights);
				Matrix sum_tmp = new Matrix(1,16);
				for(i=0;i<16;i++)
					for(j=0;j<256;j++)
						sum_tmp.set(0, i, sum_tmp.get(0, i)+tmp.get(j, i));
				
				for(i=0;i<16;i++)
					curr_sift.set(a, i,sum_tmp.get(0, i));
			}
			Matrix curr_sift_tmp =new Matrix(128,1);
			for(i=0;i<16;i++)
				for(j=0;j<8;j++)
				{
					curr_sift_tmp.set(i*8+j, 0, curr_sift.get(j, i));
				}
				
			for(i=0;i<128;i++)
				sift_arr.set(aflag,i,curr_sift_tmp.get(i,0));	
				
			}
			
			return sift_arr;
		}
		
	public Matrix normalize_sift(Matrix sift){
		int row = sift.getRowDimension();
		int col = sift.getColumnDimension();
		Matrix tmp= new Matrix(row,1);
		int i,j;
		for(i=0;i<row;i++)
			for(j=0;j<col;j++){
				tmp.set(i,0,tmp.get(i,0)+Math.pow(sift.get(i, j), 2));
			}
		for(i=0;i<row;i++)
				tmp.set(i,0,Math.sqrt(tmp.get(i,0)));
			
		
		int count = 0;
		for(i=0;i<row;i++){
			if(tmp.get(i, 0)>1)
				count++;
		}
		Matrix normonize_ind =new Matrix(count,1);
		int cur=0;
		for(i=0;i<row;i++)
			if(tmp.get(i, 0)>1){
				normonize_ind.set(cur, 0, i);
				cur++;
			}
		
		Matrix sift_arr_norm = new Matrix(count,col);
		for(i=0;i<count;i++)
			sift_arr_norm.setMatrix(i, i, 0, col-1, sift.getMatrix((int)normonize_ind.get(i, 0), (int)normonize_ind.get(i, 0), 0, col-1));
		
		Matrix tmp_t =new Matrix(count,col);
		for(i=0;i<count;i++)
			tmp_t.set(i, 0, tmp.get((int)normonize_ind.get(i,0), 0));
		for(i=1;i<col;i++)
			for(j=0;j<count;j++)
				tmp_t.set(j, i, tmp_t.get(j, 0));
		
		sift_arr_norm.arrayRightDivideEquals(tmp_t);
		for(i=0;i<count;i++)
			for(j=0;j<col;j++)
			{
				if(sift_arr_norm.get(i, j)>0.2)
					sift_arr_norm.set(i, j, 0.2);
			}
		
		Matrix stmp = new Matrix(count,1);
		for(i=0;i<count;i++)
			for(j=0;j<col;j++){
				stmp.set(i,0,stmp.get(i,0)+Math.pow(sift_arr_norm.get(i, j), 2));
			}
		for(i=0;i<count;i++)
			stmp.set(i,0,Math.sqrt(stmp.get(i,0)));
		for(i=0;i<count;i++)
			tmp_t.set(i, 0, stmp.get(i, 0));
		for(i=1;i<col;i++)
			for(j=0;j<count;j++)
				tmp_t.set(j, i, tmp_t.get(j, 0));
		
		sift_arr_norm.arrayRightDivideEquals(tmp_t);
		for(i=0;i<count;i++)
		{
			int t1 =(int)normonize_ind.get(i,0);
			sift.setMatrix(t1, t1, 0, col-1, sift_arr_norm.getMatrix(i, i, 0, col-1));
		}
		
		return sift;
		
	}
		
	Feature generate(Matrix I){
		int patchSize = 16;
		int gridSpacing = 8;
		
		int row = I.getRowDimension();//hgt
		int col = I.getColumnDimension();//wid
		
		//make grid (coordinates of upper left patch corners)
		int offsetX =(int)Math.floor(((col-patchSize)%gridSpacing)/2)+1;
		int offsetY =(int)Math.floor(((row-patchSize)%gridSpacing)/2)+1;
		int gyvalue = (int)Math.floor((col-patchSize+1-offsetX)/gridSpacing)+1;//36
		int gxvalue = (int)Math.floor((row-patchSize+1-offsetY)/gridSpacing)+1;//24
		Matrix gridX = new Matrix(gxvalue,gyvalue);
		Matrix gridY = new Matrix(gxvalue,gyvalue);
		int i,j;
		for(i = 0; i<gxvalue ;i++)
			gridX.set(i, 0, offsetX);	
		for(i=1;i<gyvalue;i++)
			for(j=0;j<gxvalue;j++)
				gridX.set(j, i, gridX.get(j, i-1)+8);
		for(i=0;i<gyvalue;i++)
			gridY.set(0,i,offsetY);
		for(i=1;i<gxvalue;i++)
			for(j=0;j<gyvalue;j++)
				gridY.set(i, j, gridY.get(i-1,j)+8);
		
		Matrix sift =new Matrix(gxvalue*gyvalue,128);
		sift = find_sift_grid(I,gridX,gridY,patchSize);
		sift = normalize_sift(sift);
		double[] x =new double[gxvalue*gyvalue];
		double[] y =new double[gxvalue*gyvalue];
		int temp =gxvalue*gyvalue;
		for(i=0;i<temp;i++){
			x[i]=gridX.get(i%gxvalue, i/gxvalue)+patchSize/2-0.5;
			y[i]=gridY.get(i%gxvalue, i/gxvalue)+patchSize/2-0.5;
		}
		
		Feature feature = new Feature(sift,x,y,col,row,patchSize);
			
		return feature;
	}
	
	}


