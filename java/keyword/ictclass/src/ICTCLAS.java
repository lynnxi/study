import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.impl.JsonWriteContext;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;
import org.codehaus.jackson.util.CharTypes;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import ICTCLAS.I3S.AC.ICTCLAS50;


class Blog {

	private String blogid;
	private String blog_pubdate;
	private String classid;
	private String body;
	private List<Word> keyWords;

	public String getClassid() {
		return classid;
	}

	public void setClassid(String classid) {
		this.classid = classid;
	}

	public String getBlog_pubdate(){
		return blog_pubdate;
	}

	public void setBlog_pubdate(String date) {
		this.blog_pubdate = date;
	}

	public List<Word> getKeyWords() {
		return keyWords;	
	}

	public void setKeyWords(List<Word> keyWords) {
		this.keyWords = keyWords;
	}

	public String getBlogid() {
		return blogid;
	}

	public void setBlogid(String blogid) {
		this.blogid = blogid;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

}

class Word {
    
    private String word;
    private String attr;
    private int count = 0;
    private Double tf = 0.00;
    private Double tfidf = 0.00;

    public String getAttr() {
    	return attr;
    }

    public void setAttr(String attr) {
    	this.attr = attr;
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
	private int miniSize;
	private int maxSize;
	private long sleepTime;
	private String redisIp;
	private int redisPort;
	private String redisKey;
	private int blogCount;
	private Double defaultScore;
	private String libPath;
	private String confPath;
	private String userDict;
	private int serverPort;
	private String endOfData;
	private Map<String, Double> attrWeight = new HashMap<String, Double>();
	
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
        this.miniSize = Integer.parseInt(ps.getProperty("mini_size"));
        this.maxSize = Integer.parseInt(ps.getProperty("max_size"));
        this.sleepTime = Integer.parseInt(ps.getProperty("sleep_time"));
        this.serverPort = Integer.parseInt(ps.getProperty("server_port"));
        this.endOfData = ps.getProperty("end_of_data");
        
        String _stags_weight = ps.getProperty("attr_weight");
		String[] _atags_weight = _stags_weight.split(",");
		for (String _stag_weight : _atags_weight) {
			String[] kv = _stag_weight.split("->");
			this.attrWeight.put(kv[0], Double.parseDouble(kv[1]));
		}
	}
	
	
	
	public String getEndOfData() {
		return endOfData;
	}

	public void setEndOfData(String endOfData) {
		this.endOfData = endOfData;
	}

	public int getServerPort() {
		return serverPort;
	}
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
	public int getMiniSize() {
		return miniSize;
	}
	public void setMiniSize(int miniSize) {
		this.miniSize = miniSize;
	}
	public int getMaxSize() {
		return maxSize;
	}
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}
	public long getSleepTime() {
		return sleepTime;
	}
	public void setSleepTime(long sleepTime) {
		this.sleepTime = sleepTime;
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

	public Map<String, Double> getAttrWeight() {
		return attrWeight;
	}


	public void setAttrWeight(Map<String, Double> attrWeight) {
		this.attrWeight = attrWeight;
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

	public static void close(Jedis jedis) {
		pool.returnResource(jedis);
	}

}


class StringUnicodeSerializer extends JsonSerializer<String> {  
	  
    private final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();  
    private final int[] ESCAPE_CODES = CharTypes.get7BitOutputEscapes();  
  
    private void writeUnicodeEscape(JsonGenerator gen, char c) throws IOException {  
        gen.writeRaw('\\');  
        gen.writeRaw('u');  
        gen.writeRaw(HEX_CHARS[(c >> 12) & 0xF]);  
        gen.writeRaw(HEX_CHARS[(c >> 8) & 0xF]);  
        gen.writeRaw(HEX_CHARS[(c >> 4) & 0xF]);  
        gen.writeRaw(HEX_CHARS[c & 0xF]);  
    }  
  
    private void writeShortEscape(JsonGenerator gen, char c) throws IOException {  
        gen.writeRaw('\\');  
        gen.writeRaw(c);  
    }  
  
    @Override  
    public void serialize(String str, JsonGenerator gen,  
            SerializerProvider provider) throws IOException,  
            JsonProcessingException {  
        int status = ((JsonWriteContext) gen.getOutputContext()).writeValue();  
        switch (status) {  
          case JsonWriteContext.STATUS_OK_AFTER_COLON:  
            gen.writeRaw(':');  
            break;  
          case JsonWriteContext.STATUS_OK_AFTER_COMMA:  
            gen.writeRaw(',');  
            break;  
          case JsonWriteContext.STATUS_EXPECT_NAME:  
            throw new JsonGenerationException("Can not write string value here");  
        }  
        gen.writeRaw('"');//写入JSON中字符串的开头引号  
        for (char c : str.toCharArray()) {  
          if (c >= 0x80){  
              writeUnicodeEscape(gen, c); // 为所有非ASCII字符生成转义的unicode字符  
          }else {  
            // 为ASCII字符中前128个字符使用转义的unicode字符  
            int code = (c < ESCAPE_CODES.length ? ESCAPE_CODES[c] : 0);  
            if (code == 0){  
                gen.writeRaw(c); // 此处不用转义  
            }else if (code < 0){  
                writeUnicodeEscape(gen, (char) (-code - 1)); // 通用转义字符  
            }else {  
                writeShortEscape(gen, (char) code); // 短转义字符 (\n \t ...)  
            }  
          }  
        }  
        gen.writeRaw('"');//写入JSON中字符串的结束引号  
    }  
  
} 

class Server1 {
    private ThreadPoolExecutor threadsPool;
    /*缓冲区大小*/  
    private Configure configure;
	
    private ServerSocket ss;
    
    public Server1(Configure configure) throws IOException {  
        threadsPool = ThreadPool.getThreadsPool(configure.getMiniSize(), configure.getMaxSize(), configure.getSleepTime());
        this.configure = configure;
        
        ss = new ServerSocket(configure.getServerPort());
        this.listen();
    } 
    
    public static void printLog (String msg) {
    	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	System.err.println(dateFormat.format(new Date()) + " : " + msg);
    }
    
	private void listen(){
		
		while(true)
		{
			try {
				final Socket s = ss.accept();
				printLog("session accepted");
				threadsPool.execute(new Runnable() {
					
					@Override
					public void run() {
						try {
							ICTCLAS.getKeyWordsBat(s.getInputStream(), s.getOutputStream(), configure);
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							try {s.close();} catch (IOException e) {e.printStackTrace();}
						}
						printLog("session done");
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

class Server2 {
	
	class Response {
		private byte[] content;
		private int point;
		private int remain;
		
		public Response(byte[] content) {
			this.content = content;
			point = 0;
			remain = content.length;
		}
		
		public byte[] getContent() {
			return content;
		}
		public void setContent(byte[] content) {
			this.content = content;
			point = 0;
			remain = content.length;
		}
		public int getPoint() {
			return point;
		}
		public void setPoint(int point) {
			this.point = point;
			this.remain = content.length - point;
		}

		public int getRemain() {
			return remain;
		}

		public void setRemain(int remain) {
			this.remain = remain;
		}
		
	}
	
	class Request {
		private String data;
		private int flag;
		
		public Request(String data) {
			this.data = data;
			this.flag = 0;
		}
		
		public String addData (String data) {
			return this.data += data;
		}
		public String getData() {
			return data;
		}
		public void setData(String data) {
			this.data = data;
		}
		public int getFlag() {
			return flag;
		}
		public void setFlag(int flag) {
			this.flag = flag;
		}
	}
	
	
	class Context {
		private Request req;
		private Response res;
		private boolean handle;
		
		public Context() {
			req = new Request("");
			res = new Response("".getBytes());
		}
		
		
		public boolean isHandle() {
			return handle;
		}

		public void setHandle(boolean handle) {
			this.handle = handle;
		}

		public Request getReq() {
			return req;
		}
		public void setReq(Request req) {
			this.req = req;
		}
		public Response getRes() {
			return res;
		}
		public void setRes(Response res) {
			this.res = res;
		}
	}
	
    private Selector selector;  
    private ThreadPoolExecutor threadsPool;
    private final int port;
    private final String EOD; 
    /*缓冲区大小*/  
    private final static int BLOCK = 4096;
    private Configure configure;
    
    
    private static Map<SocketChannel, Context> chanelContext = new ConcurrentHashMap<SocketChannel, Context>();
    
    public Server2(Configure configure) throws IOException {  
        selector = Selector.open();  
        threadsPool = ThreadPool.getThreadsPool(configure.getMiniSize(), configure.getMaxSize(), configure.getSleepTime());
        port = configure.getServerPort();
        EOD = configure.getEndOfData();
        this.configure = configure;
        this.listen();
        this.select();
    }  
    
    // 监听  
    private void listen() throws IOException {  
    	threadsPool.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
	        		// 打开服务器套接字通道  
					ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
					serverSocketChannel.socket().bind(new InetSocketAddress(port));
					SocketChannel client = null;
    	        	while (true) {
						System.out.println("now pool size : " + threadsPool.getPoolSize());
						System.out.println("now active thread : " + threadsPool.getActiveCount());
						client = serverSocketChannel.accept();
						System.out.println("accept connect");
						chanelContext.put(client, new Context());
			            // 配置为非阻塞  
			            client.configureBlocking(false);  
			            // 注册到selector，等待数据
			            client.register(selector.wakeup(), SelectionKey.OP_READ);   
						System.out.println("regist connect");
    	        	}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
    }  
  
    private void select() throws IOException {
        while (true) {  
            // 选择一组键，并且相应的通道已经打开  
            selector.select(1);  
            // 返回此选择器的已选择键集。  
            Set<SelectionKey> selectionKeys = selector.selectedKeys();  
            Iterator<SelectionKey> iterator = selectionKeys.iterator();  
            while (iterator.hasNext()) {          
                final SelectionKey selectionKey = iterator.next();  
                iterator.remove();
                if (!chanelContext.get(selectionKey.channel()).isHandle()) {
                    threadsPool.execute(new Runnable() {
    					@Override
    					public void run() {
							try {
								handleKey(selectionKey);
							} catch (Exception e) {
								e.printStackTrace();
							}
    					}
    				});
                    chanelContext.get(selectionKey.channel()).setHandle(true);
                }
            }  
        } 
    }
    
    // 处理请求  
    private void handleKey(SelectionKey selectionKey) throws Exception {  
  
        /*接受数据缓冲区*/  
        ByteBuffer sendbuffer = ByteBuffer.allocate(BLOCK);  
        /*发送数据缓冲区*/  
        ByteBuffer receivebuffer = ByteBuffer.allocate(BLOCK);  
        SocketChannel client = (SocketChannel) selectionKey.channel();
        Context context = chanelContext.get(client);
        
        String receiveText;  
        int readCount = 0;
       if (selectionKey.isReadable()) {  
            // 返回为之创建此键的通道。  
            //System.out.println(client.hashCode());
            //将缓冲区清空以备下次读取  
            receivebuffer.clear();  
            //读取服务器发送来的数据到缓冲区中  
            readCount = client.read(receivebuffer);   
            if (readCount > 0) {  
                receiveText = new String(receivebuffer.array(), 0, receivebuffer.position());
				//System.out.println("read data : " + receiveText);
				System.out.println("read data : ");
				//System.out.println(Arrays.toString(EOD.getBytes()));
				//System.out.println(Arrays.toString(receiveText.getBytes()));
				//System.out.println(EOD);
				//System.out.println(EOD.equals(receiveText));
                Request req = context.getReq();
                req.addData(receiveText);
                if (receiveText.endsWith(EOD)){
                	
					System.out.println("receive done : ");
					
                	ByteArrayOutputStream out = new ByteArrayOutputStream();
                	ByteArrayInputStream in = new ByteArrayInputStream(req.getData().getBytes());
					//System.out.println("deal with : " + req.getData());
					System.out.println("deal with : ");
					try{
	                	ICTCLAS.getKeyWordsBat(in, out, configure);
	                	context.getRes().setContent(out.toByteArray());
					} finally {
						in.close();
						out.close();
					}
					//System.out.println("res : " + new String(context.getRes().getContent()));
                	client.register(selector, SelectionKey.OP_WRITE);
                }
            }  
        } else if (selectionKey.isWritable()) {  
            //将缓冲区清空以备下次写入  
            sendbuffer.clear();  
            // 返回为之创建此键的通道。  
            client = (SocketChannel) selectionKey.channel();  
            Response res = context.getRes();  
            //向缓冲区中输入数据  
            sendbuffer.put(res.getContent(), res.getPoint(), (res.getRemain() > BLOCK) ? BLOCK : res.getRemain());  
             //将缓冲区各标志复位,因为向里面put了数据标志被改变要想从中读取数据发向服务器,就要复位  
            sendbuffer.flip();  
            //输出到通道  
            res.setPoint(res.getPoint() + client.write(sendbuffer));  
			System.out.println("write data : ");
            if (res.getRemain() == 0) {
            	sendbuffer.clear();
            	sendbuffer.put(EOD.getBytes());
            	sendbuffer.flip();
            	client.write(sendbuffer);
            	client.close();
            	chanelContext.remove(client);
				System.out.println("session done : ");
            }
        }
       context.setHandle(false);
	   System.out.println("handle done : ");
    }  
}

class ThreadPool {
	
	private static volatile ThreadPoolExecutor threadsPool;
	
	public static ThreadPoolExecutor getThreadsPool (int corePoolSize, int maximumPoolSize, final long sleepTime) {
    	if (threadsPool == null) {
    		synchronized (ThreadPoolExecutor.class) {
				if (threadsPool == null) {
					//threadsPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100));
					threadsPool = (ThreadPoolExecutor)Executors.newCachedThreadPool();
					threadsPool.setCorePoolSize(corePoolSize);
					threadsPool.setMaximumPoolSize(maximumPoolSize);
					threadsPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
						
						@Override
						public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
							try {
								Thread.sleep(sleepTime);
								executor.execute(r);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
						}
					});
				}
			}
    	}
    	return threadsPool;
	}
}

public class ICTCLAS {

    private ICTCLAS50 ict;
	private	String regex = "[a-zA-z]+://([^/]*)[^\\s]*";
	private	Pattern pattern = Pattern.compile(regex);
    private static volatile ICTCLAS instance;
    private static volatile ObjectMapper mapper;   
	
    public static ObjectMapper getObjectMapper () {
    	if (mapper == null) {
    		synchronized (ObjectMapper.class) {
				if (mapper == null) {
					mapper = new ObjectMapper();
					//使Jackson JSON支持Unicode编码非ASCII字符  
				    CustomSerializerFactory serializerFactory= new CustomSerializerFactory();  
				    serializerFactory.addSpecificMapping(String.class, new StringUnicodeSerializer());  
				    mapper.setSerializerFactory(serializerFactory);  
				}
			}
    	}
    	return mapper;
    	
    }
    
    public static ICTCLAS getInstance(String conf, String lib) throws Exception {
    	if (instance == null) {
    		synchronized (ICTCLAS.class) {
				if (instance == null) {
					instance = new ICTCLAS(conf, lib);
				}
			}
    	}
    	return instance;
    }
	
	
	
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
    
    public String split(String sInput) throws UnsupportedEncodingException {
        
		//Matcher m = pattern.matcher(sInput);
		//String domain = "";
		//String url = "";
		//while(m.find()) {
			//url = m.group();
			//domain = m.group(1).replaceAll("\\.", "-");
			//sInput = sInput.replaceAll(url, domain);
			//sInput = m.replaceAll("$1");
			//sInput = m.replaceAll("xurl");
		
		//}
		//System.out.println(sInput);
        return new String(this.ict.ICTCLAS_ParagraphProcess(pattern.matcher(sInput).replaceAll("xurl").getBytes(), 0, 1));
            
    }
   
	public static void getKeyWordsBat(InputStream in, OutputStream out, Configure configure) throws Exception {
		
		//ThreadPoolExecutor threadPool = ICTCLAS.getThreadsPool(configure.getMiniSize(), configure.getMaxSize(), configure.getSleepTime());
		ICTCLAS ictclas = ICTCLAS.getInstance(configure.getConfPath(), configure.getLibPath());
		ObjectMapper mapper = ICTCLAS.getObjectMapper();
		String EOD = configure.getEndOfData();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		PrintWriter pw = new PrintWriter(out, true);
		String line = "";
		Blog blog;
		
		//List<Word> words;
		while ((line = br.readLine()) != null) {
			try {
				//System.out.println(line);
				//if ("".equals(line)) {
				//	continue;
				//}
				if ("".equals(line) || EOD.equals(line)) {
					Server1.printLog("read EOD");
					pw.print(EOD);
					pw.flush();
					Server1.printLog("write EOD");
					break;
				}

				blog = mapper.readValue(line, Blog.class);
				blog.setKeyWords(ICTCLAS.getKeyWords(ictclas.split(URLDecoder.decode(blog.getBody(), "utf-8")), configure));

				//words = getKeyWords(ictclas.split(URLDecoder.decode(blog.getBody(), "utf-8")), configure);
				//for(Word word : words) {
				//	System.out.println(URLDecoder.decode(word.getWord()) + "->" + word.getTag() + "->" + word.getCount() + "->" + word.getTfidf());
				//}
				//break;
				blog.setBody("-");
				pw.println(mapper.writeValueAsString(blog));
				Server1.printLog("write line");
			} catch (JsonParseException e) {
				pw.println("json parse error");
				Server1.printLog(line);
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		pw.flush();
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
    
    public static List<Word> getKeyWords(String resStr, Configure configure) throws Exception {
        String[] strs = resStr.split(" ");
        Map<String, Word> wordCount = new HashMap<String, Word>();
		Map<String, Double> attrWeight = configure.getAttrWeight();
        //System.out.println(Arrays.toString(strs));
        Double wordSize = 0.00;
		Double weight = 0.00;
        for(String str : strs) {
            String[] strinfo = str.trim().split("/");
          //这里过滤掉垃圾词
            if ((strinfo.length == 2) && (strinfo[0].length() > 1) && ((weight = (attrWeight.get(strinfo[1]) == null ? 1 : attrWeight.get(strinfo[1]))) != -1)) {
               	//System.out.println(strinfo[0].length() + "-> " + strinfo[0] + "=>" + "->" + strinfo[1]); 
                Word word = wordCount.get(strinfo[0]);
                if (word == null) {
                    word = new Word();
                }
                word.setCount(word.getCount()+1);
                word.setWord(strinfo[0]);
                word.setAttr(strinfo[1]);
                wordCount.put(strinfo[0], word);
                wordSize += (strinfo[0].length() * weight);
            }
        }
        //wordSize = wordCount.size();       
 
        List<Word> words = new ArrayList<Word>(wordCount.values());
        
        Jedis jedis = RedisClient.getInstance(configure.getRedisIp(), configure.getRedisPort());
		jedis.select(1);
        for(Word word : words) {
        	//这里使用字数比而不是词数比，拉高大词的权重
            Double tf = word.getCount() * word.getWord().length() / wordSize;
            if ((weight = attrWeight.get(word.getAttr())) != null){
                tf *= weight;
            }
			Double score = jedis.zscore(configure.getRedisKey(), word.getWord());
            Double idf = Math.log((score != null ? configure.getBlogCount()/score : configure.getBlogCount()/configure.getDefaultScore()));
            word.setTf(tf);
            word.setTfidf(tf*idf);
        }
		RedisClient.close(jedis);
        Collections.sort(words, new Comparator<Word>() {
            @Override
            public int compare(Word o1, Word o2) {
                
                return -o1.getTfidf().compareTo(o2.getTfidf());
            }
        });
		//return words;
        return words.subList(0, (words.size() > 50 ? 50 : words.size()));
        
    }
    
    public static void main(String[] args) throws Exception {
        
        if (args.length != 1 || "-help".equals(args[0])) {
            //System.out.println("Usage: ict <conf> <filepath>");
            System.out.println("Usage: ict <conf>");
            System.exit(-1);
        }
        //File file = new File(args[1]);
        final Configure configure = new Configure(args[0]);
       
		//ICTCLAS.getKeyWordsBat(file, configure);

        /*File[] files = file.listFiles();
        
        for (final File _file : files) {
        	new Thread() {
        		public void run() {
        			try {
						ICTCLAS.getKeyWordsBat(new FileInputStream(_file), System.out, configure);
					} catch (Exception e) {
						e.printStackTrace();
					}
        		};
			}.start(); 
        } */
        
        new Server1(configure);
        
		//ICTCLAS ictclas = new ICTCLAS(configure.getConfPath(), configure.getLibPath());
        //List<Word> words = ictclas.getKeyWords(ictclas.split(ICTCLAS.fileGetContents(filename)), configure);
        //ObjectMapper mapper = new ObjectMapper();
		//System.out.println(mapper.writeValueAsString(words));
		
		//mapper.writeValue(System.out, words);
		//System.out.println("-----------------------");
        //for(Word word : words) {
        //    System.out.println(word.getWord() + "->" + word.getTag() + "->" + word.getCount() + "->" + word.getTfidf());
		//}
        
    }
    
}

