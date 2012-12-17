package querysort;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Partitioner;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class QuerySort extends Configured implements Tool{
	/**
	   * A partitioner that splits text keys into roughly equal partitions
	   * in a global sorted order.
	   */
	  static class TotalOrderPartitioner implements Partitioner<LongWritable,Text>{
	    private LongWritable[] splitPoints;


	    /**
	     * Read the cut points from the given sequence file.
	     * @param fs the file system
	     * @param p the path to read
	     * @param job the job config
	     * @return the strings to split the partitions on
	     * @throws IOException
	     */
	    private static LongWritable[] readPartitions(FileSystem fs, Path p, 
	                                         JobConf job) throws IOException {
	      SequenceFile.Reader reader = new SequenceFile.Reader(fs, p, job);
	      List<LongWritable> parts = new ArrayList<LongWritable>();
	      LongWritable key = new LongWritable();
	      NullWritable value = NullWritable.get();
	      while (reader.next(key, value)) {
	        parts.add(key);
	        key = new LongWritable();
	      }
	      reader.close();
	      return parts.toArray(new LongWritable[parts.size()]);  
	    }


	    public void configure(JobConf job) {
	      try {
	        FileSystem fs = FileSystem.getLocal(job);
	        Path partFile = new Path(QuerySortInputFormat.PARTITION_FILENAME);
	        splitPoints = readPartitions(fs, partFile, job);
	      } catch (IOException ie) {
	        throw new IllegalArgumentException("can't read paritions file", ie);
	      }
	    }

	    public int findPartition(LongWritable key) {
	    	int low = 0, high=splitPoints.length-1;
	    	int mid;
	    	int partition = 0;
	    	long _key = key.get();
	    	long _val;
	    	
	    	while (low <= high) {
	    		mid = (low + high) / 2;
	    		_val = splitPoints[mid].get();
	    		
	    		if (low == high) {
	    			partition = _key <= _val ? mid : mid + 1;
	    			break;
	    		} else if (_key == _val) {
	    			partition = mid;
	    			break;
	    		} else if (_key < _val) {
	    			high = mid - 1;
	    		} else if (_key > _val) {
	    			low = mid + 1;
	    		}
	    	}
	    	return partition;
	    }
	    
	    public TotalOrderPartitioner() {
	    }

	    public int getPartition(LongWritable key, Text value, int numPartitions) {
	      return this.findPartition(key);
	    }
	    
	  }
	  public int run(String[] args) throws Exception {
		    JobConf job = (JobConf) getConf();
		    Path inputDir = new Path(args[1]);
		    inputDir = inputDir.makeQualified(inputDir.getFileSystem(job));
		    Path partitionFile = new Path(inputDir, QuerySortInputFormat.PARTITION_FILENAME);
		    URI partitionUri = new URI(partitionFile.toString() +
		                               "#" + QuerySortInputFormat.PARTITION_FILENAME);
		    QuerySortInputFormat.setInputPaths(job, new Path(args[1]));
		    FileOutputFormat.setOutputPath(job, new Path(args[2]));
		    job.setJobName("TeraSort");
		    job.setJarByClass(QuerySort.class);
		    job.setOutputKeyClass(LongWritable.class);
		    job.setOutputValueClass(Text.class);
		    job.setInputFormat(QuerySortInputFormat.class);
		    job.setOutputFormat(QueryOutputFormat.class);
		    job.setPartitionerClass(TotalOrderPartitioner.class);
		    job.setOutputKeyComparatorClass(LongWritable.DecreasingComparator.class);
		    job.setNumReduceTasks(args.length >= 4 ? Integer.parseInt(args[3]) : 1);
		    
		    QuerySortInputFormat.writePartitionFile(job, partitionFile);
		    System.out.println(partitionFile.toString());
		    DistributedCache.addCacheFile(partitionUri, job);
		    DistributedCache.createSymlink(job);
		    job.setInt("dfs.replication", 1);
		    QueryOutputFormat.setFinalSync(job, true);
		    JobClient.runJob(job);
		    return 0;
	  }
	  
	  public static void main(String[] args) throws Exception {
//		TotalOrderPartitioner top = new TotalOrderPartitioner();
//		top.splitPoints = new LongWritable[]{new LongWritable(3), 
//											new LongWritable(5), 
//											new LongWritable(8), 
//											new LongWritable(9), 
//											new LongWritable(15), 
//											new LongWritable(19)};
//		int i = top.findPartition(new LongWritable(15));
//		System.out.println(i);
		QueryCount.run(args);
	    int res = ToolRunner.run(new JobConf(), new QuerySort(), args);
	    System.exit(res);
	  }
	  
}
