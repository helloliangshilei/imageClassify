package imageClassify;

import Jama.Matrix;

public class Feature {
	private Matrix sift;
	private double[] x;
	private double[] y;
	private int wit;
	private int hgt;
	private int patchSize;
	
	public Feature(Matrix sift,double[] x,double[] y, int wit,int hgt,int patchSize){
		this.sift=sift;
		this.x=x;
		this.y=y;
		this.wit=wit;
		this.hgt=hgt;
		this.patchSize=patchSize;
	}
	
	public Matrix getSift(){
		return this.sift;
	}
	
	public double[] getX(){
		return this.x;
	}
	
	public double[] getY(){
		return this.y;
	}
	
	public int getWit(){
		return this.wit;
	}
	
	public int getHgt(){
		return this.hgt;
	}
	
	public int patchSize(){
		return this.patchSize;
	}

}
