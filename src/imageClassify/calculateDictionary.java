package imageClassify;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.clustering.conversion.InputDriver;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.clustering.kmeans.RandomSeedGenerator;
import org.apache.mahout.common.AbstractJob;
import org.apache.mahout.common.ClassUtils;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.utils.clustering.ClusterDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class calculateDictionary extends AbstractJob {
  
  private static final Logger log = LoggerFactory.getLogger(calculateDictionary.class);
  
  private static final String DIRECTORY_CONTAINING_CONVERTED_INPUT = "data";
  
  private calculateDictionary() {
  }
  
 
  
  
  public static void main(String[] args) throws Exception {
      log.info("Running with only user-supplied arguments");
      ToolRunner.run(new Configuration(), new calculateDictionary(), args);
    
  }
  
  @Override
  public int run(String[] args) throws Exception {
	  	addInputOption();
	    addOutputOption();
	    addOption(DefaultOptionCreator.distanceMeasureOption().create());
	    addOption(DefaultOptionCreator.numClustersOption().create());
	    addOption(DefaultOptionCreator.convergenceOption().create());
	    addOption(DefaultOptionCreator.maxIterationsOption().create());
	    addOption(DefaultOptionCreator.overwriteOption().create());
	    addOption(DefaultOptionCreator.methodOption().create());
	    addOption(DefaultOptionCreator. clusteringOption().create());
	    
	    Map<String,List<String>> argMap = parseArguments(args);
	    if (argMap == null) {
	      return -1;
	    }
	    
	    Path input = getInputPath();
	    Path output = getOutputPath();
	    
	    String measureClass = getOption(DefaultOptionCreator.DISTANCE_MEASURE_OPTION);
	    if (measureClass == null) {
	      measureClass = EuclideanDistanceMeasure.class.getName();
	    }
	    
	    double convergenceDelta = Double.parseDouble(getOption(DefaultOptionCreator.CONVERGENCE_DELTA_OPTION));
	   
	    int maxIterations = Integer.parseInt(getOption(DefaultOptionCreator.MAX_ITERATIONS_OPTION));
	  
	    if (hasOption(DefaultOptionCreator.OVERWRITE_OPTION)) {
	      HadoopUtil.delete(getConf(), output);
	    }
	    
	    boolean clustering = false ;
	    if(hasOption(DefaultOptionCreator.CLUSTERING_OPTION))
	    	clustering = true;
	    
	 /*   boolean sequential =false;
	    if(hasOption(DefaultOptionCreator.METHOD_OPTION))
	    	sequential = true;*/
	   
	    DistanceMeasure measure = ClassUtils.instantiateAs(measureClass, DistanceMeasure.class);
	  
	    if (hasOption(DefaultOptionCreator.NUM_CLUSTERS_OPTION)) {
	      int k = Integer.parseInt(getOption(DefaultOptionCreator.NUM_CLUSTERS_OPTION));
	      run(getConf(), input, output, measure, k, convergenceDelta, maxIterations,clustering);
	      }
	    return 0;
  }
  
  /**
   * Run the kmeans clustering job on an input dataset using the given the number of clusters k and iteration
   * parameters. All output data will be written to the output directory, which will be initially deleted if it exists.
   * The clustered points will reside in the path <output>/clustered-points. By default, the job expects a file
   * containing equal length space delimited data that resides in a directory named "testdata", and writes output to a
   * directory named "output".
   * 
    * @param input
   *          the directory pathname for input points
   * @param clustersIn
   *          the directory pathname for initial & computed clusters
   * @param output
   *          the directory pathname for output points
   * @param convergenceDelta
   *          the convergence delta value
   * @param maxIterations
   *          the maximum number of iterations
   * @param runClustering
   *          true if points are to be clustered after iterations are completed
   * @param clusterClassificationThreshold
   *          Is a clustering strictness / outlier removal parameter. Its value should be between 0 and 1. Vectors
   *          having pdf below this value will not be clustered.
   * @param runSequential
   *          if true execute sequential algorithm
   */
  public static void run(Configuration conf, Path input, Path output, DistanceMeasure measure, int k,
      double convergenceDelta, int maxIterations,boolean clustering) throws Exception {
    Path directoryContainingConvertedInput = new Path(output, DIRECTORY_CONTAINING_CONVERTED_INPUT);
    log.info("Preparing Input");
    InputDriver.runJob(input, directoryContainingConvertedInput, "org.apache.mahout.math.RandomAccessSparseVector");
    log.info("Running random seed to get initial clusters");
    Path clusters = new Path(output, "random-seeds");
    clusters = RandomSeedGenerator.buildRandom(conf, directoryContainingConvertedInput, clusters, k, measure);
    log.info("Running KMeans with k = {}", k);
    KMeansDriver.run(conf, directoryContainingConvertedInput, clusters, output, convergenceDelta,
        maxIterations, clustering, 0.0, false);
    // run ClusterDumper
    Path outGlob = new Path(output, "clusters-*-final");
    Path clusteredPoints = new Path(output,"clusteredPoints");
    log.info("Dumping out clusters from clusters: {} and clusteredPoints: {}", outGlob, clusteredPoints);
    ClusterDumper clusterDumper = new ClusterDumper(outGlob, clusteredPoints);
    clusterDumper.printClusters(null);
  }
}
