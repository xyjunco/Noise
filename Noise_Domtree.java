import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;

public class Noise_Domtree {
	
	public static String UTF8( String text ) throws UnsupportedEncodingException {
		byte[] byteUTF8 = text.getBytes();
		String utf8 = new String(byteUTF8, "utf-8");
		return utf8;
	}
	
	public static String trans2dom( String text ) throws  Exception {
		Document doc = Jsoup.parse(text);
		Elements p_tags = doc.select("p");
		String result = "";
		for( Element p_tag : p_tags ) {
			result += p_tag.text() + "\r\n";
		}
		return result;
	}
	
	//以回车符分regex，将正文进行段落分割
	public static String[] dividePara( String text )
	{
		String symbol = "\n";
		java.util.regex.Pattern p = java.util.regex.Pattern.compile(symbol);
		String[] graph = p.split(text);
		return graph;
	}
	
	public static String judgeNoise( String[] para, int count, Jedis redis ) {
		String text = "";
		for( String p : para ) {
			String key = Integer.toString(p.hashCode());
			if( redis.exists(key) ) {
				redis.incrBy(key, 1);
				if( Integer.parseInt(redis.get(key)) <= (int)(1+count/50) ) {
					System.out.println( Integer.parseInt(redis.get(key)) + "  " + (int)(1+count/50)  );
					text += p;
				}
				else {
					System.out.println(p);
					continue;
				}
			}
			else {
				redis.set(key, "1");
				text += p;
			}
			//System.out.println(para[para.length-1].hashCode());
		}
		
		return text;
	}
	
	public static void main(String[] args) throws Exception {
		int count = 0;
		File path = new File("/home/junco/work-web/noise/hash/HTML/");
		Collection<File> file = FileUtils.listFiles(path, new String[]{"html"}, true);
		Jedis redis = new Jedis( "localhost", 6379 );
		Iterator<File> it = file.iterator();
		while( it.hasNext() ) {
			File f = it.next();
			String filename = f.toString();
			String[] name = filename.split("[,\\/]");
			String[] name_pre = name[name.length-1].split("[.]");
			
			String text = FileUtils.readFileToString(f);
			text = UTF8(text);
			File newpath = new File( path+"/result/"+ name_pre[0] + ".txt");
			//将text格式化，生成DOM树
			String domtree = trans2dom(text);
			
			//将正文进行分割
			String[] para = dividePara(domtree);
			
			//利用redis存储每段正文的hash值，进行噪声判断
			FileUtils.writeStringToFile( newpath, judgeNoise(para, count, redis));
			count++;
			System.out.println(count);
		}
		redis.flushAll();
	}
}
