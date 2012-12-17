/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package querysort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.LineRecordReader;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;

/**
 * An input format that reads the first 10 characters of each line as the key
 * and the rest of the line as the value. Both key and value are represented
 * as Text.
 */
public class QuerySortInputFormat extends FileInputFormat<LongWritable,Text> {

  static final String PARTITION_FILENAME = "_partition.lst";
  static final String SAMPLE_SIZE = "terasort.partitions.sample";
  private static JobConf lastConf = null;
  private static InputSplit[] lastResult = null;

  static class TextSampler implements IndexedSortable {
    private ArrayList<LongWritable> records = new ArrayList<LongWritable>();

    public int compare(int i, int j) {
      LongWritable left = records.get(i);
      LongWritable right = records.get(j);
      return left.compareTo(right);
    }

    public void swap(int i, int j) {
      LongWritable left = records.get(i);
      LongWritable right = records.get(j);
      records.set(j, left);
      records.set(i, right);
    }

    public void addKey(LongWritable key) {
      records.add(key);
    }

    
    /**
     * Find the split points for a given sample. The sample keys are sorted
     * and down sampled to find even split points for the partitions. The
     * returned keys should be the start of their respective partitions.
     * @param numPartitions the desired number of partitions
     * @return an array of size numPartitions - 1 that holds the split points
     */
    LongWritable[] createPartitions(int numPartitions) {
      int numRecords = records.size();
      System.out.println("Making " + numPartitions + " from " + numRecords + 
                         " records");
      if (numPartitions > numRecords) {
        throw new IllegalArgumentException
          ("Requested more partitions than input keys (" + numPartitions +
           " > " + numRecords + ")");
      }
      new QuickSort().sort(this, 0, records.size());
      float stepSize = numRecords / (float) numPartitions;
      System.out.println("Step size is " + stepSize);
      LongWritable[] result = new LongWritable[numPartitions-1];
      for(int i=1; i < numPartitions; ++i) {
        result[i-1] = records.get(Math.round(stepSize * i));
      }
      return result;
    }
  }
  
  /**
   * Use the input splits to take samples of the input and generate sample
   * keys. By default reads 100,000 keys from 10 locations in the input, sorts
   * them and picks N-1 keys to generate N equally sized partitions.
   * @param conf the job to sample
   * @param partFile where to write the output file to
   * @throws IOException if something goes wrong
   */
  public static void writePartitionFile(JobConf conf, 
                                        Path partFile) throws IOException {
	QuerySortInputFormat inFormat = new QuerySortInputFormat();
    TextSampler sampler = new TextSampler();
    LongWritable key = new LongWritable();
    Text value = new Text();
    int partitions = conf.getNumReduceTasks();
    long sampleSize = conf.getLong(SAMPLE_SIZE, 100000);
    InputSplit[] splits = inFormat.getSplits(conf, conf.getNumMapTasks());
    int samples = Math.min(10, splits.length);
    long recordsPerSample = sampleSize / samples;
    int sampleStep = splits.length / samples;
    long records = 0;
    // take N samples from different parts of the input
    for(int i=0; i < samples; ++i) {
      RecordReader<LongWritable,Text> reader = 
        inFormat.getRecordReader(splits[sampleStep * i], conf, null);
      while (reader.next(key, value)) {
        sampler.addKey(new LongWritable(key.get()));
        records += 1;
        if ((i+1) * recordsPerSample <= records) {
          break;
        }
      }
    }
    FileSystem outFs = partFile.getFileSystem(conf);
    if (outFs.exists(partFile)) {
      outFs.delete(partFile, false);
    }
    SequenceFile.Writer writer = 
      SequenceFile.createWriter(outFs, conf, partFile, LongWritable.class, 
                                NullWritable.class);
    NullWritable nullValue = NullWritable.get();
    for(LongWritable split : sampler.createPartitions(partitions)) {
      writer.append(split, nullValue);
    }
    writer.close();
  }

  static class TeraRecordReader implements RecordReader<LongWritable,Text> {
    private LineRecordReader in;
    private LongWritable junk = new LongWritable();
    private Text line = new Text();
    private static int KEY_LENGTH = 10;

    public TeraRecordReader(Configuration job, 
                            FileSplit split) throws IOException {
      in = new LineRecordReader(job, split);
    }

    public void close() throws IOException {
      in.close();
    }

    public LongWritable createKey() {
      return new LongWritable();
    }

    public Text createValue() {
      return new Text();
    }

    public long getPos() throws IOException {
      return in.getPos();
    }

    public float getProgress() throws IOException {
      return in.getProgress();
    }

    public boolean next(LongWritable key, Text value) throws IOException {
      if (in.next(junk, line)) {
    	String[] kv = line.toString().split("\t");
    	try {
	    	key.set(Long.valueOf(kv[1]));
	    	value.set(kv[0]);
    	} catch (Exception e) {
    		System.out.println(Arrays.toString(kv));
    	}
        return true;
      } else {
        return false;
      }
    }
  }
  

  @Override
  public RecordReader<LongWritable, Text> 
      getRecordReader(InputSplit split,
                      JobConf job, 
                      Reporter reporter) throws IOException {
    return new TeraRecordReader(job, (FileSplit) split);
  }

  @Override
  public InputSplit[] getSplits(JobConf conf, int splits) throws IOException {
    if (conf == lastConf) {
      return lastResult;
    }
    lastConf = conf;
    lastResult = super.getSplits(conf, splits);
    return lastResult;
  }
}
