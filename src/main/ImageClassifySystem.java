package main;

import tools.ClusterDumper;
import tools.CreateHipiImageBundle;
import tools.formatDictionary;
import imageClassify.*;
public class ImageClassifySystem {
	public static void main(String[] args) throws Exception{
	
		/********************************************First Part : Generate Sift**********************************************************/
		/*create hib from imageset
		 * *in : imageset path from localSystem
		 * out : out path on hdfs
		 */
		long startTime=System.currentTimeMillis();   //获取开始时间  
		System.out.println("First Step: CreateHipiImageBundle START!");
		String imageset_test = "/home/liang/hadoopworkspace/image_test";
		String hib_test = "classify/image_test_hib";
		String imageset_train = "/home/liang/hadoopworkspace/image_train";
		String hib_train = "classify/image_train_hib";
		String [] CreateHipiImageBundleArgs_test = new String[]{imageset_test,hib_test};
		String [] CreateHipiImageBundleArgs_train = new String[]{imageset_train,hib_train};
        CreateHipiImageBundle.main(CreateHipiImageBundleArgs_test);
        CreateHipiImageBundle.main(CreateHipiImageBundleArgs_train);
		System.out.println("First Step: CreateHipiImageBundle END!");
		
		/*
		 * generate sift of each image and gather sift togather
		 * in:hib from first step
		 * out:sift of each image && sifttogather
		 */
		System.out.println("Second Step: generateSift START!");
		String in_generateSift_test = hib_test;
		String out_generateSift_test = "classify/image_test_sift";
		String togathersift_test = "classify/image_test_sifttogather";
		String in_generateSift_train = hib_train;
		String out_generateSift_train = "classify/image_train_sift";
		String togathersift_train = "classify/image_train_sifttogather";
		String [] generateSiftArgs_test = new String[]{in_generateSift_test,out_generateSift_test,togathersift_test};
		String [] generateSiftArgs_train = new String[]{in_generateSift_train,out_generateSift_train,togathersift_train};
		generateSift.main(generateSiftArgs_test);
		generateSift.main(generateSiftArgs_train);
		System.out.println("Second Step: generateSift END!");
		
		/********************************************Second Part : Build Dictionary**********************************************************/
		/*
		 * calculate dictionary from sifttogather file with kmeans method
		 * in: togather sift file 
		 * out : the dictionary without formation
		 * cd : the convergence delta value (0.001)
		 *  k : the dictionary size(300)
		 *  x : the max iteration times
		 *  ow : overwrite
		 */
		System.out.println("Third Step: calculateDictionary START!");
		String in_dictionary = togathersift_train;
		String out_dictionary = "classify/dictionary_unformat";
		String cd = "0.001";
		String k = "300";
		String x = "1";
		String[] calculateDictionaryArgs = new String[]{"-i",in_dictionary,"-o",out_dictionary,"-cd",cd,"-k",k,"-x",x,"-ow"};
    	calculateDictionary.main(calculateDictionaryArgs);
		System.out.println("Third Step: calculateSift END!");
		
		/*
		 * dump class from dictionary from the third step
		 * in : dictionary file from hdfs
		 * out: dumpped dictionary in the local filesystem
		 */
		System.out.println("Fourth Step: clusterDumper START!");
		String in_dump = out_dictionary+"/clusters-"+x+"-final";
		String out_dump = "/home/liang/hadoopworkspace/classify/dictionary_dump";
		String[] ClusterDumperArgs = new String[]{"-i",in_dump,"-o",out_dump};
		ClusterDumper.main(ClusterDumperArgs);
		System.out.println("Fourth Step: clusterDumper END!");

		/*
		 * format dictionary to 300 lines,every num is splitted by space
		 * in : dictionary without format from local filesystem
		 * out: format dictionary to hdfs 
		 */
		System.out.println("Fifth Step: formatDictionary START!");
		String in_format = out_dump;
		String out_format = "classify/dictionary_format";
		String[] formatDictionaryArgs = new String[]{in_format,out_format};
		formatDictionary.main(formatDictionaryArgs);
		System.out.println("Fifth Step: formatDictionary END! ");

		/********************************************Third Part : Calculate Sift Pyramid**********************************************************/
		/*
		 * calculte pyramid sift of every image
		 * in: path of dictionary
		 * in: path of evey image's sift (out_generateSift+"/siftdescriptor")
		 * out: pyramid sift of 6300 in each image
		 */
		System.out.println("Sixth Step: compilePyramid START!");
		String in_dictionary_pyramid = out_format;
		String in_sift_pyramid_test = out_generateSift_test+"/siftdescriptor";
		String out_pyramid_test = "classify/pyramid_test";
		String in_sift_pyramid_train = out_generateSift_train+"/siftdescriptor";
		String out_pyramid_train = "classify/pyramid_train";
		String[] compilePyramidArgs_test = new String[]{in_dictionary_pyramid,in_sift_pyramid_test,out_pyramid_test};
		String[] compilePyramidArgs_train = new String[]{in_dictionary_pyramid,in_sift_pyramid_train,out_pyramid_train};
		compilePyramid.main(compilePyramidArgs_test);
		compilePyramid.main(compilePyramidArgs_train);
		System.out.println("Sixth Step: compilePyramid END!");
		
		/********************************************Fourth Part : Random Forest**********************************************************/
		/*
		 * describe the dataset
		 * (example : -p classify/pyramid_test -f classify/pyramid_test_describe -d 6300 N L)
		 * in:dataset to describe
		 * out describe file
		 */
		System.out.println("Seventh Step: Describe START!");
		String in_describe_train = out_pyramid_train;
		String out_describe_train = out_pyramid_train+"_describe";
		String[] describeArgs_train = new String[]{"-p",in_describe_train,"-f",out_describe_train,"-d","6300","N","L"};
		String in_describe_test = out_pyramid_test;
		String out_describe_test = out_pyramid_test+"_describe";
		String[] describeArgs_test = new String[]{"-p",in_describe_test,"-f",out_describe_test,"-d","6300","N","L"};
		Describe.main(describeArgs_train);
		Describe.main(describeArgs_test);
		System.out.println("Seventh Step: Describe END!");
		
		/*
		 * train the random forest
		 * (example: -d classify/pyramid_test -ds classify/pyramid_test_describe -p -t 100 -o classify/image-forest)
		 * in: dataset
		 * in: dataset describe
		 * out:forest model
		 */
		System.out.println("Eighth Step: BuildForest START!");
		String in_train_d = out_pyramid_train;
		String in_train_ds = out_describe_train;
		String out_train = "classify/image_forest";
		String[] BuildForestArgs =new String[]{"-d",in_train_d,"-ds",in_train_ds,"-p","-t","100","-o",out_train};
		BuildForest.main(BuildForestArgs);
		System.out.println("Eighth Step: BuildForest END!");
		
		/*
		 * test the random forest
		 * (example: -i classify/pyramid_test -ds classify/pyramid_test_describe -m classify/image-forest -a -mr -o classify/predictions)
		 * in : test dataset
		 * in: test dataset describe
		 * in: forest model
		 * out: prediction data
		 */
		System.out.println("Ninth Step: TestForest START!");
		String in_test_d = out_pyramid_test;
		String in_test_ds = out_describe_test;
		String in_test_forest = out_train;
		String out_test = "classify/predictions";
		String[] TestForestArgs = new String[]{"-i",in_test_d,"-ds",in_test_ds,"-m",in_test_forest,"-a","-mr","-o",out_test};
		TestForest.main(TestForestArgs);
		System.out.println("Ninth Step: TestForest END!");
		long endTime=System.currentTimeMillis(); //获取结束时间  
		System.out.println("程序运行时间： "+(endTime-startTime)/(1000.0*60)+"m"+(((endTime-startTime)/(1000.0))%60));  
		/********************************************END**********************************************************/
	}

}
