import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ICTCLAS.I3S.AC.ICTCLAS50;
import redis.clients.jedis.*;

class Word {
    
    private String word;
    private String tag;
    private int count = 0;
    private Double tf = 0.00;
    private Double tfidf = 0.00;


    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
    public String getWord() {
        return word;
    }
    public void setWord(String word) {
        this.word = word;
    }
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public Double getTf() {
        return tf;
    }
    public void setTf(Double tf) {
        this.tf = tf;
    }
    public Double getTfidf() {
        return tfidf;
    }
    public void setTfidf(Double tfidf) {
        this.tfidf = tfidf;
    }
}

class Configure {
	private String redisIp;
	private int redisPort;
	private String redisKey;
	private int blogCount;
	private Double defaultScore;
	private String libPath;
	private String confPath;
	private String userDict;
	private Map<String, Integer> tagWeight = new HashMap<String, Integer>();
	
	public Configure(String conf) throws Exception {
		Properties ps = new Properties();
        ps.load(new FileInputStream(conf + "/Configure.properties"));
        this.confPath = ps.getProperty("conf_path");
        this.libPath = ps.getProperty("lib_path");
        this.userDict = ps.getProperty("user_dict");
		this.redisIp = ps.getProperty("redis_ip");
		this.redisPort = Integer.parseInt(ps.getProperty("redis_port"));
		this.redisKey = ps.getProperty("redis_key");
		this.blogCount = Integer.parseInt(ps.getProperty("blog_count"));
        this.defaultScore = Double.parseDouble(ps.getProperty("default_score"));
        
        String _stags_weight = ps.getProperty("tag_weight");
		String[] _atags_weight = _stags_weight.split(",");
		for (String _stag_weight : _atags_weight) {
			String[] kv = _stag_weight.split("->");
			this.tagWeight.put(kv[0], Integer.parseInt(kv[1]));
		}
	}
	
	
	public String getUserDict() {
		return userDict;
	}
	public void setUserDict(String userDict) {
		this.userDict = userDict;
	}
	public String getRedisIp() {
		return redisIp;
	}
	public void setRedisIp(String redisIp) {
		this.redisIp = redisIp;
	}
	public int getRedisPort() {
		return redisPort;
	}
	public void setRedisPort(int redisPort) {
		this.redisPort = redisPort;
	}
	public String getRedisKey() {
		return redisKey;
	}
	public void setRedisKey(String redisKey) {
		this.redisKey = redisKey;
	}
	public int getBlogCount() {
		return blogCount;
	}
	public void setBlogCount(int blogCount) {
		this.blogCount = blogCount;
	}
	public Double getDefaultScore() {
		return defaultScore;
	}
	public void setDefaultScore(Double defaultScore) {
		this.defaultScore = defaultScore;
	}
	public Map<String, Integer> getTagWeight() {
		return tagWeight;
	}
	public void setTagWeight(Map<String, Integer> tagWeight) {
		this.tagWeight = tagWeight;
	}
	public String getLibPath() {
		return libPath;
	}
	public void setLibPath(String libPath) {
		this.libPath = libPath;
	}
	public String getConfPath() {
		return confPath;
	}
	public void setConfPath(String confPath) {
		this.confPath = confPath;
	}
	
}


class RedisClient {
	private static volatile JedisPool pool;  
	//private static volatile Jedis jedis;
	private static Map<String, JedisPool> clientMap = new HashMap<String, JedisPool>();

	public static Jedis getInstance(String ip, int port) {
		pool = clientMap.get(ip +"_" + port);
		//double check lock
		if (pool == null) {
			synchronized (RedisClient.class) {
				if (pool == null) {
					pool = new JedisPool(new JedisPoolConfig(), ip, port);
					clientMap.put(ip +"_" + port, pool);
				}
			
			}
		}
		return pool.getResource();
	
	}
}


public class ICTCLAS {

    private ICTCLAS50 ict;
    
    public ICTCLAS(String conf, String lib, String userDict) throws UnsupportedEncodingException {
        this(conf, lib);
        int nCount = this.ict.ICTCLAS_ImportUserDictFile(userDict.getBytes(), 0);
        this.ict.ICTCLAS_SaveTheUsrDic();
    }
    
    public ICTCLAS(String conf, String lib) throws UnsupportedEncodingException {
        System.load(lib + "/libICTCLAS50.so");
        this.ict = new ICTCLAS50();
        this.ict.ICTCLAS_SetPOSmap(2);
        //System.out.println("ICTCLAS_Init");
        if (this.ict.ICTCLAS_Init(conf.getBytes()) == false)
        {
            System.out.println("Init Fail!");
        }

    }
    
    public String Split(String sInput) throws UnsupportedEncodingException {
        
        String str = "";
        byte nativeBytes[] = this.ict.ICTCLAS_ParagraphProcess(sInput.getBytes(), 0, 1);
        str = new String(nativeBytes);
            
        return str;
    }
    
    public static String fileGetContents(String filename) throws Exception {
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename))));
        String line = "";
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }
    
    public static List<Word> getKeyWords (String resStr, Configure configure) {
        String[] strs = resStr.split(" ");
        Map<String, Word> wordCount = new HashMap<String, Word>();
		Map<String, Integer> tagWeight = configure.getTagWeight();
        System.out.println(Arrays.toString(strs));
        Integer wordSize = 0;
        for(String str : strs) {
            String[] strinfo = str.split("/");
          //这里过滤掉垃圾词
            if ((strinfo.length == 2) && (strinfo[0].length() > 1) && (tagWeight.get(strinfo[1]) == null || tagWeight.get(strinfo[1]) != -1)) {
                
                Word word = wordCount.get(strinfo[0]);
                if (word == null) {
                    word = new Word();
                }
                word.setCount(word.getCount()+1);
                word.setWord(strinfo[0]);
                word.setTag(strinfo[1]);
                wordCount.put(strinfo[0], word);
                wordSize += strinfo[0].length();
            }
        }
        //wordSize = wordCount.size();       
 
        List<Word> words = new ArrayList<Word>(wordCount.values());
        Jedis jedis = RedisClient.getInstance(configure.getRedisIp(), configure.getRedisPort());
		jedis.select(1);
        Integer weight;
        for(Word word : words) {
        	//这里使用字数比而不是词数比，拉高大词的权重
            Double tf = word.getCount() * word.getWord().length() / wordSize.doubleValue();
            if ((weight = tagWeight.get(word.getTag())) != null){
                tf *= weight;
            }
			Double score = jedis.zscore(configure.getRedisKey(), word.getWord());
            Double idf = Math.log((score != null ? score : configure.getBlogCount()/configure.getDefaultScore()));
            word.setTf(tf);
            word.setTfidf(tf*idf);
        }
        
        Collections.sort(words, new Comparator<Word>() {
            @Override
            public int compare(Word o1, Word o2) {
                
                return -o1.getTfidf().compareTo(o2.getTfidf());
            }
        });
        return words;
        
    }
    
    public static void main(String[] args) throws Exception {
        
        if (args.length != 2 || "-help".equals(args[0])) {
            System.out.println("Usage: ict <conf> <filename>");
            System.exit(-1);
        }
        String filename = args[1];
        String conf = args[0];
        Configure configure = new Configure(conf);
        
        try {
            ICTCLAS ictclas = new ICTCLAS(configure.getConfPath(), configure.getLibPath());
            List<Word> words = ictclas.getKeyWords(ictclas.Split(ICTCLAS.fileGetContents(filename)), configure);
            for(Word word : words) {
                System.out.println(word.getWord() + "->" + word.getTag() + "->" + word.getCount() + "->" + word.getTfidf());
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
}

