import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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


class Word {
    
    private String word;
    private String tag;
    private int count = 0;
    private Double tf = 0.00;
    
    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
    private Double tfidf = 0.00;
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

public class ICTCLAS {

    private ICTCLAS50 ict;
    private static final String[] TAGFILTER = new String[]{"v", "f"};
    private static final int NWEIGHT = 4;
    
    public ICTCLAS(String conf, String lib, String userDict) throws UnsupportedEncodingException {
        this(conf, lib);
        int nCount = this.ict.ICTCLAS_ImportUserDictFile(userDict.getBytes(), 0);
        this.ict.ICTCLAS_SaveTheUsrDic();//保存用户词典

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
    
    public static List<Word> getKeyWords (String resStr) {
        String[] strs = resStr.split(" ");
        Map<String, Word> wordCount = new HashMap<String, Word>();
        System.out.println(Arrays.toString(strs));
        Integer wordSize = 0;
        for(String str : strs) {
            String[] strinfo = str.split("/");
            if ((strinfo.length == 2) && (strinfo[0].length() > 1) && (Arrays.binarySearch(TAGFILTER, strinfo[1]) == -1)) {
                
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
        for(Word word : words) {
            Double tf = word.getCount() * word.getWord().length() / wordSize.doubleValue();
            if ("n".equals(word.getTag())){
                tf *= NWEIGHT;
            }
            word.setTf(tf);
        }
        
        Collections.sort(words, new Comparator<Word>() {
            @Override
            public int compare(Word o1, Word o2) {
                
                return -o1.getTf().compareTo(o2.getTf());
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
        Properties ps = new Properties();
        ps.load(new FileInputStream(conf + "/Configure.properties"));
        String confPath = ps.getProperty("conf_path");
        String libPath = ps.getProperty("lib_path");
        String userdict = ps.getProperty("user_dict");
        try {
            ICTCLAS ictclas = new ICTCLAS(confPath, libPath);
            List<Word> words = ictclas.getKeyWords(ictclas.Split(ICTCLAS.fileGetContents(filename)));
            for(Word word : words) {
                System.out.println(word.getWord() + "->" + word.getTag() + "->" + word.getCount() + "->" + word.getTf());
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
}

