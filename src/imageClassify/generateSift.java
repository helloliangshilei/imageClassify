package imageClassify;

import hipi.image.FloatImage;
import hipi.image.ImageHeader;
import hipi.imagebundle.mapreduce.ImageBundleInputFormat;
import hipi.util.ByteUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;



import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import Jama.Matrix;

public class generateSift extends Configured implements Tool {

	public static class generateSiftMapper extends Mapper<ImageHeader, FloatImage, IntWritable, Text> {
		
		private Path path;
		private FileSystem fileSystem;
		private Configuration conf;
		public void setup(Context jc) throws IOException
		{
			conf = jc.getConfiguration();
			fileSystem = FileSystem.get(conf);
			path = new Path( conf.get("generateSift.outdir"));
			fileSystem.mkdirs(path);
		}

		@Override
		public void map(ImageHeader key, FloatImage value, Context context)
		throws IOException, InterruptedException {
			
			if (value != null) {
				FloatImage gray = value.convert(FloatImage.RGB2GRAY);
				int imageWidth = gray.getWidth();
				int imageHeight = gray.getHeight();
				
				//get grayimage matrix
				float[] imgdata = gray.getData();
				int i,j;
				Matrix img =new Matrix(imageHeight,imageWidth);
				for(i=0;i<imageHeight;i++)
					for(j=0;j<imageWidth;j++)
						img.set(i, j, imgdata[i*imageWidth+j]);
				
				//generate sift deccriptors
				generateSiftFunction s =generateSiftFunction.getInstance();
				Feature feature = s.generate(img);
				int row = feature.getHgt();
				int col = feature.getWit();
				int patchSize = 16;
				int gridSpacing = 8;
				int offsetX =(int)Math.floor(((col-patchSize)%gridSpacing)/2)+1;
				int offsetY =(int)Math.floor(((row-patchSize)%gridSpacing)/2)+1;
				int gyvalue = (int)Math.floor((col-patchSize+1-offsetX)/gridSpacing)+1;//36
				int gxvalue = (int)Math.floor((row-patchSize+1-offsetY)/gridSpacing)+1;//24
				Matrix sift =new Matrix(gxvalue*gyvalue,128);
				sift = feature.getSift();
				double []x =feature.getX();
				double []y =feature.getY();
			
			    //store sift x y hight width patchSize to the local filesystem
				String fileName = key.fileName;
				Path outpath = new Path(path + "/siftdescriptor/" + fileName);
				while(fileSystem.exists(outpath)){
					String temp = outpath.toString();
					outpath = new Path(temp + "1"); 
				}
				
				FSDataOutputStream siftStream = fileSystem.create(new Path(outpath+"/sift"));
				FSDataOutputStream xywhStream = fileSystem.create(new Path(outpath+"/x"));
				FSDataOutputStream labelStream = fileSystem.create(new Path(outpath+"/label"));
				FSDataOutputStream tsiftStream = fileSystem.create(new Path(conf.get("generateSift.togathersiftdir")+"/"+fileName));
			    BufferedWriter siftout = new BufferedWriter(new OutputStreamWriter(siftStream,"UTF-8"));
			    BufferedWriter xyhwout = new BufferedWriter(new OutputStreamWriter(xywhStream,"UTF-8"));
			    BufferedWriter label = new BufferedWriter(new OutputStreamWriter(labelStream,"UTF-8"));
			    BufferedWriter tsift = new BufferedWriter(new OutputStreamWriter(tsiftStream,"UTF-8"));
	 
			    for(i=0;i<gxvalue*gyvalue;i++){
			    	for(j=0;j<128;j++)
			    	{
			    		siftout.write(Double.toString(sift.get(i, j))+' ');
			    		tsift.write(Double.toString(sift.get(i, j))+' ');
			    	}
			    	siftout.write('\n');
			    	tsift.write('\n');
			    }
			    
			    for(i=0;i<x.length;i++)
			    	xyhwout.write(Double.toString(x[i])+' ');
			    xyhwout.write('\n');
			    for(i=0;i<y.length;i++)
			    	xyhwout.write(Double.toString(y[i])+' ');
			    xyhwout.write('\n');
			    xyhwout.write(String.valueOf(feature.getHgt()));
			    xyhwout.write('\n');
			    xyhwout.write(String.valueOf(feature.getWit()));
			    
			    switch(fileName.split("_")[0])
			    {
			    case "phoning":
			    	label.write(String.valueOf(1));
			    	break;
			    case "playingguitar":
			    	label.write(String.valueOf(2));
			    	break;
			    case "ridingbike":
			    	label.write(String.valueOf(3));
			    	break;
			    case "ridinghorse":
			    	label.write(String.valueOf(4));
			    	break;
			    case "running":
			    	label.write(String.valueOf(5));
			    	break;
			    case "shooting":
			    	label.write(String.valueOf(6));
			    	break;
			    default:
			    	break;
			    }
			    
			    	
			    
			    siftout.flush();
			    siftout.close();
			    xyhwout.flush();
			    xyhwout.close();
			    label.flush();
			    label.close();
			    tsift.flush();
			    tsift.close();
			    siftStream.close();
			    xywhStream.close();
			    labelStream.close();
			    tsiftStream.close();
				
							
				
				String hexHash = ByteUtils.asHex(ByteUtils.FloatArraytoByteArray(value.getData()));
				String camera = key.getEXIFInformation("Model");
				String output = imageWidth + "x" + imageHeight + "\t(" + hexHash + ")\t	" + camera;
								
				context.write(new IntWritable(1), new Text(output));
			}
		}

	}
	
	public static class generateSiftReducer extends Reducer<IntWritable, Text, IntWritable, Text> {
		public void reduce(IntWritable key, Iterable<Text> values, Context context) 
		throws IOException, InterruptedException {
			for (Text value : values) {
				context.write(key, value);
			}
		}
	}

	public int run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		if (args.length < 3) {
			System.out.println("Usage: generateSift <input hib> <outputdir> <togathersiftdir>");
			System.exit(0);
		}
		String inputPath = args[0];
		String outputPath = args[1];
		String togathersiftPath = args[2];
		
		conf.setStrings("generateSift.outdir", outputPath);
		conf.setStrings("generateSift.togathersiftdir", togathersiftPath);

		Job job = new Job(conf, "generateSift");
		job.setJarByClass(generateSift.class);
		job.setMapperClass(generateSiftMapper.class);
		job.setReducerClass(generateSiftReducer.class);

		// Set formats
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		job.setInputFormatClass(ImageBundleInputFormat.class);

		// Set out/in paths
		removeDir(outputPath, conf);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		FileInputFormat.setInputPaths(job, new Path(inputPath));	

		job.setNumReduceTasks(1);
		job.waitForCompletion(true);
	//	System.exit(job.waitForCompletion(true) ? 0 : 1);
		return 0;

	}

	public static void main(String[] args) throws Exception {
		int exitCode = ToolRunner.run(new generateSift(), args);
		//System.exit(exitCode);
	}

	public static void removeDir(String path, Configuration conf) throws IOException {
		Path output_path = new Path(path);

		FileSystem fs = FileSystem.get(conf);

		if (fs.exists(output_path)) {
			fs.delete(output_path, true);
		}
	}
}

